#!/usr/bin/env bash

# The _all field allows you to search for values in documents without knowing which field contains the value. 
# This makes it a useful option when getting started with a new dataset. For instance
curl -X GET http://localhost:9200/study?pretty              \
     -H 'Cache-Control: no-cache' 						    \
     -H 'Content-Type: application/json'                    \
     -d '{
            "mapping": {
                "books": {
                    "_all": {
                        "enabled": true,
                        "store": true
                    }
                }
            }
         }';

curl -X GET http://localhost:9200/_all?pretty              \
     -H 'Cache-Control: no-cache' 						   \
     -H 'Content-Type: application/json';

curl -X GET http://localhost:9200/study/_search?pretty      \
     -H 'Cache-Control: no-cache' 							\
     -H 'Content-Type: application/json'                    \
     -d '{
            "query": {
                "query_string": {
                    "query": "java"
                }
            }
         }';

curl -X GET http://localhost:9200/study/_search?pretty      \
     -H 'Cache-Control: no-cache' 							\
     -H 'Content-Type: application/json'                    \
     -d '{
            "query": {
                "query_string": {
                    "query": "2007-06-01"
                }
            }
         }';