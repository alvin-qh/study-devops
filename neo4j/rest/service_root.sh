#!/usr/bin/env bash

curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:7474';

curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -u 'neo4j:neo4j_'  \
     -X GET 'http://localhost:7474/db/data/';