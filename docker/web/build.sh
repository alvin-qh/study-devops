#!/usr/bin/env sh

docker build -t study/web:1.0 .
docker image prune -f