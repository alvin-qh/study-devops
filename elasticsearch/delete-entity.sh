#!/usr/bin/env bash

curl -X DELETE http://localhost:9200/study/students/alvin?pretty	\
	 -H 'Cache-Control: no-cache' 									\
     -H 'Content-Type: application/json'
