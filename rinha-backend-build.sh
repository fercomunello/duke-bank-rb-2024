#!/usr/bin/env bash
cd "$(dirname "$0")"

./mvnw clean package -Dnative -Dquarkus.native.additional-build-args=-J--enable-preview -DskipTests
docker build -f src/main/docker/Dockerfile.native-micro -t fercomunello/rinha-backend-duke-bank-2024q1:latest .