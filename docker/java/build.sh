#!/usr/bin/env sh

docker build -t study/java:1.0 .
docker image prune -f
