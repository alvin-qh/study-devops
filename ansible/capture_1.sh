#!/usr/bin/env bash

source .virtualenv/bin/activate

# Test if ansible host (or group) is connected
expect -c "
set timeout -1;
spawn ansible -i inventories/common test_server -u vm -m ping -b --become-user alvin --ask-become-pass
expect {
    "SUDO*password:" {send "kkmouse"\r}
}
expect eof;"

ansible -i inventories/common test_server -u alvin -m ping