#!/usr/bin/env bash

curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X PUT 'http://localhost:9200/study?pretty' -d '@json/create-index.json';
