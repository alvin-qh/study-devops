#!/usr/bin/env bash

# 利用ansible执行ping命令，测试远程主机是否可以连通
# ansible <主机名> -m ping
# 主机名在'inventory'文件中定义，参见'hosts'文件
# 也可以使用'-i'参数指定'inventory'文件, 例如: ansible vm -i ./hosts -m ping
# 另外，参见 ansible.cfg 文件[ssh_connection]节，指定了如何使用ssh命令

# 以缺省用户执行命令
# 缺省用户在'inventory'文件中定义，参见'hosts'文件'ansible_ssh_user'配置
.venv/bin/ansible vm -m ping


# 利用sudo方式执行ping命令
expect -c "
set timeout -1;
spawn .venv/bin/ansible vm -m ping -b --ask-become-pass
expect {
    "SUDO*password*" {send "kkmouse"\r}
}
expect eof;"