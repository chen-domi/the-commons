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

mkdir -p /opt/the-commons
chown ec2-user:ec2-user /opt/the-commons
