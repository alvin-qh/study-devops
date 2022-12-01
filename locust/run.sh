#!/usr/bin/env bash

gunicorn -w 4 -k gevent -b 0.0.0.0:3000 --log-level=debug app.app:app
