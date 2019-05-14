#!/usr/bin/env bash

curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/study/_search?pretty' -d '@json/query-simple.json';

curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/study/_search?pretty&q=intro:java';