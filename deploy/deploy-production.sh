#!/bin/bash
# Run locally with your existing AWS and GitHub logins. No credentials go to EC2.
set -euo pipefail

deployment_repo="${GITHUB_REPOSITORY:-domchenn/the-commons}"
deployment_region="${AWS_REGION:-us-east-1}"
deployment_instance="${EC2_INSTANCE_ID:-i-0f207923a0c860c38}"
deployment_profile="${AWS_PROFILE:-the-commons}"
deployment_id=''
deployment_finished=false
aws_cli=(aws --profile "$deployment_profile" --region "$deployment_region")

for tool in aws gh python3 curl; do
    command -v "$tool" >/dev/null || { printf 'Required tool missing: %s\n' "$tool" >&2; exit 1; }
done

"${aws_cli[@]}" sts get-caller-identity --query Account --output text >/dev/null
deployment_sha=$(gh api "repos/$deployment_repo/commits/main" --jq '.sha')
[[ "$deployment_sha" =~ ^[0-9a-f]{40}$ ]] || { echo 'Invalid main commit SHA' >&2; exit 1; }

# Require both CI jobs on this exact commit. Never use an unchecked branch tip.
gh api "repos/$deployment_repo/commits/$deployment_sha/check-runs" --paginate --slurp \
    | python3 -c '
import json, sys
runs = [run for page in json.load(sys.stdin) for run in page["check_runs"]]
for name in ("test", "build"):
    matches = [r for r in runs if r["name"] == name]
    if not matches or any(r["status"] != "completed" or r["conclusion"] != "success" for r in matches):
        sys.exit("Both backend test and frontend build CI must pass on main before deployment")
'

deployment_domain=$("${aws_cli[@]}" ssm get-parameter \
    --name /the-commons/production/app-domain --query Parameter.Value --output text)
[[ "$deployment_domain" =~ ^[a-zA-Z0-9][a-zA-Z0-9.-]+$ ]] || { echo 'Invalid production hostname' >&2; exit 1; }
deployment_url="https://$deployment_domain"

post_status() {
    gh api --method POST "repos/$deployment_repo/deployments/$deployment_id/statuses" \
        -f state="$1" -f environment_url="$deployment_url" -f description="$2" \
        -F auto_inactive=false >/dev/null
}

finish() {
    deployment_exit=$?
    trap - EXIT
    if [ -n "$deployment_id" ] && [ "$deployment_finished" != true ]; then
        post_status failure 'AWS deployment or health verification failed; inspect the local output and SSM command.' || true
    fi
    exit "$deployment_exit"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

# Reuse the existing Production environment, but retain all Vercel history.
deployment_id=$(printf '{"ref":"%s","environment":"Production","auto_merge":false,"required_contexts":[],"production_environment":true,"description":"AWS EC2 deployment"}' "$deployment_sha" \
    | gh api --method POST "repos/$deployment_repo/deployments" --input - --jq '.id')
[[ "$deployment_id" =~ ^[0-9]+$ ]] || { echo 'GitHub did not create a deployment' >&2; exit 1; }
post_status in_progress 'Deploying checked main commit to AWS EC2 via Systems Manager.'
printf 'Deploying %s to %s (GitHub deployment %s)\n' "$deployment_sha" "$deployment_url" "$deployment_id"

# The server uses its own IAM role for Parameter Store, never the local CLI token.
# A lock prevents simultaneous invocations from modifying the same deployment.
remote_commands=(
    'set -euo pipefail'
    'exec 9>/opt/the-commons/deployment.lock'
    'flock -n 9 || { echo "Another deployment is running" >&2; exit 1; }'
    'cd /opt/the-commons/app'
    'test -z "$(git status --porcelain --untracked-files=no)" || { echo "Tracked server files have local changes" >&2; exit 1; }'
    'git fetch origin main'
    "git merge-base --is-ancestor $deployment_sha FETCH_HEAD"
    "git checkout --detach $deployment_sha"
    './deploy/render-production-env.sh .env.production'
    'docker compose --env-file .env.production -f docker-compose.production.yml config --quiet'
    'docker compose --env-file .env.production -f docker-compose.production.yml build backend'
    'docker compose --env-file .env.production -f docker-compose.production.yml build frontend'
    'docker compose --env-file .env.production -f docker-compose.production.yml pull database gateway'
    'docker compose --env-file .env.production -f docker-compose.production.yml up -d --wait --wait-timeout 180'
    "test \"\$(git rev-parse HEAD)\" = $deployment_sha"
    'docker compose --env-file .env.production -f docker-compose.production.yml ps'
)
deployment_parameters=$(python3 -c 'import json, sys; print(json.dumps({"commands": ["\n".join(sys.argv[1:])], "executionTimeout": ["3600"]}))' "${remote_commands[@]}")
deployment_command=$("${aws_cli[@]}" ssm send-command --instance-ids "$deployment_instance" \
    --document-name AWS-RunShellScript --timeout-seconds 3600 \
    --parameters "$deployment_parameters" --query Command.CommandId --output text)
printf 'SSM command: %s\n' "$deployment_command"

deployment_deadline=$((SECONDS + 3900))
while [ "$SECONDS" -lt "$deployment_deadline" ]; do
    # SSM command results are eventually consistent immediately after submission.
    deployment_status=$("${aws_cli[@]}" ssm get-command-invocation \
        --command-id "$deployment_command" --instance-id "$deployment_instance" \
        --query Status --output text 2>/dev/null) || deployment_status=Pending
    case "$deployment_status" in
        Success) break ;;
        Pending|InProgress|Delayed) sleep 5 ;;
        *)
            printf 'SSM deployment failed: %s\n' "$deployment_status" >&2
            "${aws_cli[@]}" ssm get-command-invocation --command-id "$deployment_command" \
                --instance-id "$deployment_instance" --query '{Output:StandardOutputContent,Error:StandardErrorContent}'
            exit 1 ;;
    esac
done
[ "$deployment_status" = Success ] || { echo 'Deployment timed out' >&2; exit 1; }

# The homepage alone can succeed while the backend is down. Verify both.
deployment_healthy=false
for attempt in {1..24}; do
    homepage_status=$(curl --max-time 10 --silent --output /dev/null --write-out '%{http_code}' "$deployment_url/") || homepage_status=000
    api_status=$(curl --max-time 10 --silent --output /dev/null --write-out '%{http_code}' "$deployment_url/api/auth/me") || api_status=000
    if [ "$homepage_status" = 200 ] && [ "$api_status" = 401 ]; then
        deployment_healthy=true
        break
    fi
    sleep 5
done
[ "$deployment_healthy" = true ] || { echo 'Public HTTPS/API health verification failed' >&2; exit 1; }
post_status success 'AWS EC2 deployment completed; public HTTPS and protected backend API verified.'
deployment_finished=true
printf 'Production deployment verified: %s\n' "$deployment_url"
