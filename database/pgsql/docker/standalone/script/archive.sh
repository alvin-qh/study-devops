#!/usr/bin/env bash

set -ex;

arch_dir="/var/lib/postgresql/archive";

# if [[ ! -f "$arch_dir" ]]; then
#   mkdir -p "$arch_dir"
# fi

# 将归档文件复制到指定位置
test ! -f "$arch_dir/$1" && cp --preserve=timestamps $2 "$arch_dir/$1";

# 删除 7 天以前的归档文件
find "$arch_dir" -type f -mtime +7 -exec rm -f {} \;

# 将当前文件挂载到容器的 '/var/lib/postgresql/data/archive.sh' 文件上
# 在 'postgres.conf' 配置文件中, 通过如下配置使用:
# archive_command = 'bash /var/lib/postgresql/data/archive.sh %f %p'
