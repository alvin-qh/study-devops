# Ad-Hoc

- [Ad-Hoc](#ad-hoc)
  - [1. 执行 Ad-Hoc 命令](#1-执行-ad-hoc-命令)
    - [1.1. 列出服务器列表](#11-列出服务器列表)
    - [1.2. 使用模块](#12-使用模块)
  - [2. 各类模块](#2-各类模块)
    - [2.1. Ping 模块](#21-ping-模块)
    - [2.2. Raw 模块](#22-raw-模块)
      - [执行远程命令行](#执行远程命令行)
      - [增加扩展参数](#增加扩展参数)
    - [2.3. Shell 模块](#23-shell-模块)
      - [执行 shell 命令](#执行-shell-命令)
      - [使用附加参数执行 shell 命令](#使用附加参数执行-shell-命令)
    - [2.4. 软件安装](#24-软件安装)
      - [2.4.1. Yum 模块](#241-yum-模块)
        - [安装并升级软件包](#安装并升级软件包)
        - [设置软件仓库](#设置软件仓库)
      - [2.4.2. Apt 模块](#242-apt-模块)
        - [安装并升级软件包](#安装并升级软件包-1)
    - [2.5. Template 模块](#25-template-模块)
      - [使用模板输出文件](#使用模板输出文件)
    - [2.6. Copy 模块](#26-copy-模块)
      - [复制文件](#复制文件)
      - [复制文件和目录](#复制文件和目录)
      - [复制目录](#复制目录)
      - [复制内容](#复制内容)

Ansible 具备一系列内置模块，用于执行各类远程任务，例如文件复制、软件安装等

```bash
$ ansible <pattern> <options>
```

- `pattern` 即 `inventory` 中定义的远程服务器
- `options` 即一系列命令行参数，包括：
  - `-a <MODULE_ARGS>`, `--args <MODULE_ARGS>` 设置 Ansible 模块的参数
  - `-e <EXTRA_VARS>`, `--extra-vars <EXTRA_VARS>` 设置扩展参数，必须为 `key=value` 格式。也可以将参数存储在 `YAML/JSON` 文件中，并用 `@filename` 表示参数文件
  - `-m <MODULE_NAME>`, `--module-name <MODULE_NAME>` 设置要执行的模块名称，缺省为 `command` 模块
  - `-f <FORKS>`, `--forks <FORKS>` 表示用多少个进程执行远程命令，缺省为 `5`
  - `-i <INVENTORY>`, `--inventory <INVENTORY>` 设置 `inventory` 文件的位置
  - `-u <REMOTE_USER>`, `--user <REMOTE_USER>` 设置远程服务器的用户名，覆盖 `inventory` 文件中设置的默认用户名（建议使用 `--become-user` 参数）
  - `-k`, `--ask-pass` 要求输入密码（建议使用 `--ask-become-pass` 参数）
  - `-b`, `--become` 使用 `--become-user` 参数指定的用户登录远程服务器
  - `--become-method <BECOME_METHOD>` 切换远程用户身份的方式，例如：`sudo` 或 `su`
  - `--become-user <BECOME_USER>` 指定要切换的用户身份，默认为 `root`
  - `-K`, `--ask-become-pass` 切换用户身份后需要输入密码
  - `--private-key <PRIVATE_KEY_FILE>`, `--key-file <PRIVATE_KEY_FILE>` 设置远程服务器的私钥文件，默认为 `~/.ssh/id_rsa`
  - `--list-hosts` 输出 `pattern` 包含的远程服务器列表
  - `--vault-id <VAULT_IDS>` 指定保存密码的文件
  - `--vault-password-file <VAULT_PASSWORD_FILES>`, `--vault-pass-file <VAULT_PASSWORD_FILES>` 指定保存密码的文件（推荐使用 `--vault-id` 参数）
  - `-l <SUBSET>`, `--limit <SUBSET>` 对 `pattern` 设置的服务器进行限制，例如：`--limit=192.168.0.10,192.168.0.11` 或者 `-l 192.168.0.10,192.168.0.11`

## 1. 执行 Ad-Hoc 命令

### 1.1. 列出服务器列表

```bash
$ ansible group_debian --list

  hosts (3):
    debian1
    debian2
    debian3
```

### 1.2. 使用模块

使用 Ansible 内置模块

```bash
$ ansible <pattern> -m <module> -a "<args>"
```

例如，使用 `shell` 模块执行远程命令行：

```bash
$ ansible group_debian1 -m shell -a "ps aux"  # 列出远程服务器的进程
```

## 2. 各类模块

### 2.1. Ping 模块

> [ping](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/ping_module.html)

检查各主机的 ssh 连通性

```bash
$ ansible group_debian1 -m ping
```

### 2.2. Raw 模块

> [raw](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/raw_module.html)

#### 执行远程命令行

```bash
$ ansible group_debian1 -m raw -a "ps -aux"
```

- `-a` 表示传递给 raw 模块的参数，这里即为 shell 命令字符串

#### 增加扩展参数

```bash
$ ansible group_debian1 -m raw \
    -a "ls -al executable=/bin/bash"
```

- `-a` 参数中增加 `executable` 参数，表示使用哪个命令解释器执行 shell 命令

### 2.3. Shell 模块

> [shell](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/shell_module.html)

在远程主机上执行 shell 命令，该模块会对一些命令做处理，以符合不同主机操作系统的要求，可选的附加参数包括：

- `chdir` 进入指定路径执行 shell 命令
- `cmd` 在 shell 命令执行完毕后执行的命令
- `creates` 一个文件名，如果该文件存在，则 shell 命令不被执行
- `executable` 指定执行 shell 命令的命令解释器
- `free_form` 以“自由形式”执行命令
- `removes`: 一个文件名，如果该文件不存在，则 shell 命令不被执行
- `stdin` 设置远程主机的标准输入
- `stdin_add_newline` 取值 `no` 或 `yes`（默认值），即在输入内容中增加换行
- `warn`: 取值 `no` 或 `yes`（默认值），允许显示警告信息

#### 执行 shell 命令

执行 `ls` 命令

```bash
$ ansible group_debian1 -m shell \
    -a "ls -al"
```

#### 使用附加参数执行 shell 命令

产生 `ok.txt` 文件，如果文件已存在则不执行命令

```bash
$ ansible group_debian1 -m shell \
    -a "echo 'OK' > ok.txt creates=ok.txt"
```

### 2.4. 软件安装

#### 2.4.1. Yum 模块

> [yum](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/yum_module.html)
> [yum_repository](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/yum_repository_module.html)

##### 安装并升级软件包

安装 `epel-release` 软件包

```bash
$ ansible group_centos1 -m yum \
    -a "name=epel-release state=present use_backend=dnf" \
    -e "@become.yml" \
    --vault-id=vault-id
```

- `state=present` 表示安装软件包

安装 `htop` 软件包

```bash
$ ansible group_centos1 -m yum \
    -a "name=htop state=present update_cache=yes use_backend=dnf" \
    -e "@become.yml" \
    --vault-id=vault-id
```

- `update_cache=yes` 表示执行 `yum update` 命令更新软件源缓存

卸载 `htop` 软件包

```bash
$ ansible group_centos1 -m yum \
    -a "name=htop state=absent autoremove=yes use_backend=dnf" \
    -e "@become.yml" \
    --vault-id=vault-id
```

- `state=absent` 表示卸载软件包

更新软件包

```bash
$ ansible group_centos1 -m yum \
    -a "name=* state=latest use_backend=dnf" \
    -e "@become.yml" \
    --vault-id=vault-id
```

- `name=*` 表示更新所有软件包

##### 设置软件仓库

以 `docker` 安装为例，首先卸载旧的 `docker` 软件包，包括：`docker`, `docker-engine` 和 `docker.io`

```bash
$ ansible group_centos1 -m yum \
    -a "name={{ names }} autoremove=yes state=absent use_backend=dnf" \
    -e "{names: ['docker', 'docker-engine', 'docker.io']}" \
    -e "@become.yml" \
    --vault-id=vault-id
```

安装 `docker` 相关依赖软件包

```bash
$ ansible group_centos1 -m yum \
    -a "name={{ names }} state=present use_backend=dnf" \
    -e "@become.yml" \
    -e "{names: ['yum-utils', 'device-mapper-persistent-data', 'lvm2']}" \
    --vault-id=vault-id
```

设置 `docker` 软件仓库，下载 `docker` 的 `repo` 文件

```bash
$ ansible group_centos1 -m get_url \
    -a "url=https://download.docker.com/linux/centos/docker-ce.repo dest=/etc/yum.repos.d/docker-ce.repo timeout=20" \
    -e "@become.yml" \
    --vault-id=vault-id
```

将软件源的地址替换为国内镜像地址

```bash
$ ansible group_centos1 -m raw \
    -a "sed -i 's+download.docker.com+mirrors.tuna.tsinghua.edu.cn/docker-ce+' /etc/yum.repos.d/docker-ce.repo" \
    -e "@become.yml" \
    --vault-id=vault-id
```

安装 `docker`

```bash
$ ansible group_centos1 -m yum \
    -a "name=docker-ce update_cache=yes state=present use_backend=dnf" \
    -e "@become.yml"  \
    --vault-id=vault-id
```

#### 2.4.2. Apt 模块

> [apt](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/apt_module.html)
> [apt_key](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/apt_key_module.html)
> [apt_repository](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/apt_repository_module.html)

##### 安装并升级软件包

安装 `htop` 软件

```bash
$ ansible group_debian1 -m apt \
    -a "name=htop state=present install_recommends=true update_cache=yes" \
    -e "@become.yml" \
    --vault-id=vault-id
```

卸载 `htop` 软件包

```bash
$ ansible group_debian1 -m apt \
    -a "name=htop state=absent purge=yes" \
    -e "@become.yml" \
    --vault-id=vault-id;

# 卸载无效的依赖包
$ ansible group_debian1 -m apt \
    -a "autoremove=yes purge=yes" \
    -e "@become.yml" \
    --vault-id=vault-id;
```

更新软件包

```bash
# apt update
$ ansible group_debian1 -m apt \
    -a "update_cache=yes" \
    -e "@become.yml" \
    --vault-id=vault-id;

# apt upgrade
$ ansible group_centos1 -m yum \
    -a "name=* state=latest update_cache=yes" \
    -e "@become.yml" \
    --vault-id=vault-id
```

- `name=*` 表示更新所有软件包

### 2.5. Template 模块

[template](https://docs.ansible.com/ansible/latest/collections/ansible/builtin/template_module.html)

通过“模板”在目标主机上输出文件

#### 使用模板输出文件

```bash
$ ansible group_debian1 -m template \
    -a "src=./test_template.txt dest=~/test_template.txt \
        owner=alvin mode=u=rw,g=r,o=r backup=yes" \
    -e "dynamic_name=Alvin"
```

- `src` 本机的模板文件名
- `dest` 远程主机的目标文件名
- `owner` 设置文件在远程主机的所属用户
- `mode` 设置文件在远程主机的属性
- `backup` 是否备份旧文件

### 2.6. Copy 模块

将文件从本机复制到远程主机

#### 复制文件

```bash
$ ansible group_debian1 -m copy \
    -a "src=./ansible.cfg dest=~/ force=yes"
```

- `src` 本机文件路径
- `dest` 远程主机目标文件路径
- `force` 是否强制拷贝（覆盖目标文件）

#### 复制文件和目录

```bash
$ ansible group_debian1 -m copy \
    -a "src=./conf/ dest=~/target force=yes"
```

#### 复制目录

```bash
$ ansible group_debian1 -m copy \
    -a "src=./conf dest=~/target force=yes"
```

#### 复制内容

```bash
$ ansible group_debian1 -m copy \
    -a "content='Hello World' dest=~/target.txt force=yes"
```

- `content` 要复制的字符串，需要使用“引号”包围

