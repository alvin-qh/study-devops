#!/usr/bin/env bash

curl -X POST http://localhost:9200/study/students/alvin?pretty	\
	 -H 'Cache-Control: no-cache' 								\
     -H 'Content-Type: application/json'						\
	 -d '{
	 	"name": "Alvin",
	 	"class": "Class1",
	 	"major": "化学"
	 }'
