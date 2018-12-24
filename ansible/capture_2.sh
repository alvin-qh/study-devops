#!/usr/bin/env bash

source .virtualenv/bin/activate
ansible -i inventories/common vm -a "/bin/echo 'Hello'"
ansible -i inventories/common vm -a "/bin/ls -alh /"
ansible -i inventories/common vm -a "/bin/pwd"  -b --become-user root --ask-become-pass