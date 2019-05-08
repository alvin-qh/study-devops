#!/usr/bin/env bash

curl -X GET http://localhost:9200/study/_settings?pretty            \
     -H 'Cache-Control: no-cache' 									\
     -H 'Content-Type: application/json'