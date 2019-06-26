#!/usr/bin/env bash

python -m venv .venv --prompt ansible && source .venv/bin/activate
pip install -r requirements.txt
