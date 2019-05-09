#!/usr/bin/env bash

curl -X GET http://localhost:9200/study/_search?pretty              \
     -H 'Cache-Control: no-cache' 									\
     -H 'Content-Type: application/json'                            \
     -d '{
            "query": {
                "match": {
                    "intro": {
                        "query": "java"
                    }
                }
            }
         }';

curl -X GET http://localhost:9200/study/_search?pretty&q=intro:java \
     -H 'Cache-Control: no-cache' 									\
     -H 'Content-Type: application/json' 