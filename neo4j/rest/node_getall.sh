#!/usr/bin/env bash

curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -u 'neo4j:neo4j_'  \
     -X POST 'http://localhost:7474/db/data/transaction/commit' -d '@statements/node_getall.json';