#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
docker compose up -d mysql redis rabbit
cd ../backend
mvn spring-boot:run
