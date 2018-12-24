#!/usr/bin/env bash

virtualenv -p `which python3` .virtualenv && source .virtualenv/bin/activate
pip install -r requirement.txt