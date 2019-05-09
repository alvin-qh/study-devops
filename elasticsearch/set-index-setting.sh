#!/usr/bin/env bash

# close index before
curl -X POST http://localhost:9200/study/_close;

curl -X PUT http://localhost:9200/study/_settings?pretty            \
     -H 'Cache-Control: no-cache' 									\
     -H 'Content-Type: application/json'                            \
     -d '{
            "similarity": {
                "default": {
                    "type": "BM25"
                },
                "arrays": {
                    "type": "BM25",
                    "b": "0.3",
                    "k1": "1.2"
                }
            },
            "analysis": {
                "filter": {
                    "lowercase": {
                        "type": "lowercase"
                    },
                    "smartcn_stop": {
                        "type": "stop",
                        "stopwords": [
                            "，", ",", ".", "。"
                        ]
                    }
                },
                "char_filter": {
                    "tsconvert": {
                        "convert_type": "t2s",
                        "type": "stconvert",
                        "keep_both": "false",
                        "delimiter": "#"
                    },
                    "word_break_helper_source_text": {
                        "type": "mapping",
                        "mappings": [
                            "_=>\\u0020",
                            ".=>\\u0020",
                            "(=>\\u0020",
                            ")=>\\u0020",
                            ":=>\\u0020"
                        ]
                    }
                }
            }
        }';

# reopen index after
curl -X POST http://localhost:9200/study/_open;

curl -X PUT http://localhost:9200/study/books/_mapping?pretty       \
     -H 'Cache-Control: no-cache' 						            \
     -H 'Content-Type: application/json'                            \
     -d '{
            "properties": {
                "publication_date": {
                    "type": "date"
                },
                "author": {
                    "type": "keyword"
                }
            }
        }';