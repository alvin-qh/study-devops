#!/usr/bin/env bash

curl -X GET http://localhost:9200/study/_search?pretty              \
     -H 'Cache-Control: no-cache' 									\
     -H 'Content-Type: application/json'                            \
     -d '{
            "query": {
                "match": {
                    "major": {
                        "query": "化学"
                    }
                }
            }
         }'