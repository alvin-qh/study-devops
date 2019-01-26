#!/usr/bin/env bash

source .virtualenv/bin/activate

# 利用ansible执行ping命令，测试远程主机是否可以连通
# ansible <主机名> -a "shell命令"
# 主机名在'inventory'文件中定义，参见'hosts'文件
# 也可以使用'-i'参数指定'inventory'文件, 例如: ansible vm -i ./hosts -a "shell命令"
# 另外，参见 ansible.cfg 文件[ssh_connection]节，指定了如何使用ssh命令

# 以缺省用户执行命令
# 缺省用户在'inventory'文件中定义，参见'hosts'文件'ansible_ssh_user'配置
ansible vm -a "echo Hello World"

# 以缺省用户'sudo'方式执行命令
#	-a 				要执行的命令行
# 	-b --become 	表示要切换用户
# 	--become-method 切换用户的方式, 使用'sudo'命令
ansible vm -b --become-method=sudo --ask-become-pass -a "ifconfig" 

# 以root用户执行命令
# 	--become-method 	切换用户的方式, 使用'su'命令
# 	--become-user 		要切换的用户名
# 	--ask-become-pass 	是否需要输入密码
# 	在命令行最后加上'warn=no'可以禁用命令行警告, 所有对目标服务器产生改动的操作都会产生警告
# 也可以在'ansible.cfg'中设置'command_warnings = False'达到同样的效果
ansible vm -b --become-method=su --become-user root --ask-become-pass -a "touch /aaa warn=no"
ansible vm -a "ls -alh /aaa"

ansible vm -b --become-method=su --become-user root --ask-become-pass -a "rm /aaa warn=no"
ansible vm -a "ls -alh /aaa"