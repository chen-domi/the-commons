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

umask 077
{
    printf 'APP_DOMAIN=%s\n' "$app_domain"
    printf 'DATABASE_USERNAME=%s\n' "$database_username"
    printf 'DATABASE_PASSWORD=%s\n' "$database_password"
    printf 'GOOGLE_CLIENT_ID=%s\n' "$google_client_id"
    printf 'GOOGLE_CLIENT_SECRET=%s\n' "$google_client_secret"
} > "$production_env_file"

printf 'Created %s with permissions 600.\n' "$production_env_file"
