#!/usr/bin/env sh

docker build -t study/gradlew:5.2.1 .
docker image prune -f
