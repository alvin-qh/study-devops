#!/usr/bin/env bash

curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X POST 'http://localhost:9200/study/_analyze?pretty' -d '@json/analyze-string.json';
