#!/bin/bash
set -euxo pipefail

dnf update -y
dnf install -y curl docker git

systemctl enable --now docker
usermod -aG docker ec2-user

install -d /usr/local/lib/docker/cli-plugins
curl --fail --location --silent --show-error \
    https://github.com/docker/compose/releases/download/v5.6.0/docker-compose-linux-aarch64 \
    --output /tmp/docker-compose-linux-aarch64
curl --fail --location --silent --show-error \
    https://github.com/docker/compose/releases/download/v5.6.0/docker-compose-linux-aarch64.sha256 \
    --output /tmp/docker-compose.sha256
(
    cd /tmp
    sha256sum --check /tmp/docker-compose.sha256
)
install -m 0755 /tmp/docker-compose-linux-aarch64 \
    /usr/local/lib/docker/cli-plugins/docker-compose

curl --fail --location --silent --show-error \
    https://github.com/docker/buildx/releases/download/v0.38.0/buildx-v0.38.0.linux-arm64 \
    --output /tmp/docker-buildx-linux-arm64
echo "38e890e1a162bfdbf32fbe91404991dc4fc28e55b0de901b34374a061bab4d2c  /tmp/docker-buildx-linux-arm64" \
    | sha256sum --check -
install -m 0755 /tmp/docker-buildx-linux-arm64 \
    /usr/local/lib/docker/cli-plugins/docker-buildx

mkdir -p /opt/the-commons
chown ec2-user:ec2-user /opt/the-commons
