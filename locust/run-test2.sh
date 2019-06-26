#!/usr/bin/env bash

locust -f ./test2.py -H http://localhost:3000 -P 8001