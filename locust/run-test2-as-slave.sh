#!/usr/bin/env bash

locust -f ./test2.py -H http://localhost:3000 --slave --master-host=127.0.0.1