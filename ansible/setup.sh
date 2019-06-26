#!/usr/bin/env bash

cp ~/.ssh/id_rsa .
python -m venv .venv --prompt ansible && source .venv/bin/activate
pip install -r requirements.txt
