#!/usr/bin/env bash

# close index before
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X POST 'http://localhost:9200/study/_close';

curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X PUT 'http://localhost:9200/study/_settings?pretty' -d '@json/index-settings.json';

# reopen index after
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X POST 'http://localhost:9200/study/_open';

curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X POST 'http://localhost:9200/study/books/_mapping?pretty' -d '@json/mapping-setting.json';