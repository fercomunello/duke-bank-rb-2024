#!/usr/bin/env bash
cd "$(dirname "$0")"

./mvnw clean package -DskipTests
docker build -f src/main/docker/Dockerfile.jvm -t fercomunello/rinha-backend-duke-bank-2024q1:latest .