#!/usr/bin/env bash

curl -X GET http://localhost:9200/study/_search?pretty              \
     -H 'Cache-Control: no-cache' 									\
     -H 'Content-Type: application/json'                            \
     -d '{
            "query": {
                "bool": {
                    "must": [
                        {
                            "term": {
                                "author": "Bruce Eckel"
                            }
                        }
                    ],
                    "filter": [
                        {
                            "range": {
                                "publication_date": {
                                    "gte": "2007-01-01"
                                }
                            }
                        }
                    ],
                    "must_not": [
                        {
                            "match": {
                                "intro": "C++"
                            }
                        }
                    ],
                    "should": [
                        {
                            "match": {
                                "intro": "java"
                            }
                        }
                    ]
                }
            }
         }';