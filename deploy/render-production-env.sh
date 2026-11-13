#!/bin/bash
set -euo pipefail

production_region="${AWS_REGION:-us-east-1}"
production_parameter_root="/the-commons/production"
production_env_file="${1:-.env.production}"

read_parameter() {
    aws ssm get-parameter \
        --name "${production_parameter_root}/$1" \
        --with-decryption \
        --query 'Parameter.Value' \
        --output text \
        --region "$production_region"
}

app_domain="$(read_parameter app-domain)"
database_username="$(read_parameter database-username)"
database_password="$(read_parameter database-password)"
google_client_id="$(read_parameter google-client-id)"
google_client_secret="$(read_parameter google-client-secret)"

# Missing optional feature settings keep NLP disabled on existing deployments.
read_optional_parameter() {
    local value
    value=$(aws ssm get-parameters --names "${production_parameter_root}/$1" \
        --query 'Parameters[0].Value' --output text --region "$production_region")
    if [ "$value" = None ]; then printf '%s' "$2"; else printf '%s' "$value"; fi
}
nlp_enabled="$(read_optional_parameter nlp-enabled false)"
bedrock_region="$(read_optional_parameter bedrock-region "$production_region")"
bedrock_model_id="$(read_optional_parameter bedrock-model-id '')"

umask 077
{
    printf 'APP_DOMAIN=%s\n' "$app_domain"
    printf 'DATABASE_USERNAME=%s\n' "$database_username"
    printf 'DATABASE_PASSWORD=%s\n' "$database_password"
    printf 'GOOGLE_CLIENT_ID=%s\n' "$google_client_id"
    printf 'GOOGLE_CLIENT_SECRET=%s\n' "$google_client_secret"
    printf 'NLP_ENABLED=%s\n' "$nlp_enabled"
    printf 'BEDROCK_REGION=%s\n' "$bedrock_region"
    printf 'BEDROCK_MODEL_ID=%s\n' "$bedrock_model_id"
} > "$production_env_file"

printf 'Created %s with permissions 600.\n' "$production_env_file"
