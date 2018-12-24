#!/usr/bin/env bash

source .virtualenv/bin/activate

# run command on remote server
ansible -i inventories/common vm -a "echo Hello World"

# run command with sudo permission
ansible -i inventories/common vm --become --become-user root --ask-become-pass -a "ls -alh /"
ansible -i inventories/common vm --become --become-user root --ask-become-pass -a "touch aaa"

# connect ssh with different users (not defined in `~/.ssh/config`)
ansible -i inventories/common vm --user root --ask-pass -a "pwd"