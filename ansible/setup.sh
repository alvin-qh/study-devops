#!/usr/bin/env bash

virtualenv -p `which python3` --always-copy .venv && source .venv/bin/activate
pip install -r requirement.txt