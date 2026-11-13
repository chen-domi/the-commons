# Production deployment

Run this from the repository root on your computer, after the intended changes
are merged and both GitHub CI jobs pass on `main`:

```bash
aws login --profile the-commons --region us-east-1
gh auth status
bash deploy/deploy-production.sh
```

This is a manual deployment, not automatic CD. It deploys the exact checked
`main` commit to the existing EC2 instance through Systems Manager. The server
fetches secrets using its own IAM role. No GitHub or local AWS credentials are
copied to the server. Database volumes are retained.

GitHub's existing **Production** environment receives an in-progress record,
then success only after the build/start command succeeds, HTTPS returns 200,
and the protected authentication API returns 401 to an anonymous request.
Failures are recorded as failures. The status describes that deployment run;
it is not continuous uptime monitoring or a complete authenticated smoke test.

The server refuses concurrent deployments and refuses to overwrite locally
modified tracked source files. Backend and frontend builds run sequentially
to reduce peak memory use. Failed deployments do not automatically roll back;
inspect the printed SSM command ID and container logs before trying again.

Defaults can be overridden with `GITHUB_REPOSITORY`, `AWS_PROFILE`, `AWS_REGION`,
and `EC2_INSTANCE_ID`. The public hostname comes from Parameter Store.

## Retiring Vercel

In the old Vercel project's **Settings → Git**, disconnect this repository.
Do not uninstall the Vercel app for other projects or delete deployment history.
The AWS reporter reuses **Production** with the AWS URL rather than changing old
failed Vercel records into successes. Historical Vercel failures remain visible
in the deployment timeline.
