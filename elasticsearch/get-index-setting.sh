#!/usr/bin/env bash

curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/study/_settings?pretty';
