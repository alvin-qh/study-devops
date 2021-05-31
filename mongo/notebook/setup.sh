#!/usr/bin/env bash

if [[ ! -f "./.python-version" ]]; then
  echo "> Use python version 3.8.5..."
  pyenv local 3.8.5
fi

if [[ ! -d ".venv" ]]; then
  echo "> Create virtualenv..."
  python -m venv .venv --prompt="study-devops-mongo-notebook"

  . .venv/bin/activate

  echo "> Install jupyter lab requirement packages..."
  pip install -r ../../notebook-requirements.txt
  pip install -r requirements.txt
fi

echo "Jupyter lab environment was setup."
