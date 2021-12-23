# Ad-Hoc

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

`yum` 模块具备如下参数

- `allow_downgrade` 是否允许降级已安装的软件，允许软件降级可能会导致一些以来问题。默认值为 `no`
- `autoremove` 是否允许删除软件时自动删除依赖，默认值为 `no`
- `bugfix` 是否只安装用来修复 bug 的软件包，默认值为 `no`
- `conf_file` 指定远程主机的 `yum.conf` 配置文件，一般使用默认配置文件
- `disable_excludes` 禁用 `yum.conf` 配置文件中定义的“排除项”。如果设置为 `all`，禁用所有排除项；如果设置为 `main`，禁用以 `[main]` 定义的排除项；如果设置为 `repoid`，则禁用给定 "repo id" 定义的排除项
- `disable_gpg_check` 是否禁用安装软件包签名的 GPG 检查，默认值为 `no`
- `disable_plugin` 指定在安装/更新软件包操作中禁用的插件，仅此次命令生效，默认值为 `no`
- `disablerepo` 在安装/更新软件时禁用的软件源，多个值使用 `,` 分隔。仅在本次命令中有效
- `download_dir` 当 `download_only` 生效时，指定软件包下载路径
- `download_only` 是否只下载不安装，默认值为 `no`
- `enable_plugin` 在安装/更新软件时启用的插件，仅在本次命令中有效，默认值为 `yes`
- `enablerepo` 在安装/更新时，设置启用的软件源，仅在本次命令中有效
- `exclude`: 当 `state=present` 或 `state=latest` 时，设置排除的软件包名称
- `install_repoquery`: `no` or `yes`(default). 如果软件源查询不可用，则安装 `yum-utils` 包。如果系统注册到 RHN 或 RHN，则允许查询所有分配的软件源频道。`list` 参数也依赖该参数
- `install_weak_deps` 自动安装由“弱依赖关系”关联的其它包，默认值 `yes`
  - 该功能只在后端为 `yum4` 时生效
- `installroot` 默认为 "/"，指定一个路径替代原有的安装路径
- `list` 列举软件包，等效于 `yum list --show-duplicates <package>` 命令。除了列举软件包外，还可以对“已安装”，“待更新”，“可用”和“软件源”进行列举。该参数与 `name` 参数互斥
- `lock_timeout` 等待 yum 文件锁释放的实际，默认值为 `30`
- `name`: 要安装/更新/卸载的软件包名称，可以带有版本号（如：`name-1.0`）。如果版本号低于已安装版本，则 `allow_downgrade` 设置需开启。如果使用 `state=latest`，则可设置 `name=*` 表示更新所有软件
  - 别名为 `pkg`
- `releasever` 指定要按照软件包的替代版本
- `security`: 是否仅安装标记为与“安全相关”的更新，需要 `state=latest`，默认值为 `no`
- `skip_broken`: 跳过依赖项已损坏的包，默认值为 `no`
- `state` 取值为：安装（`present` 或 `installed`），卸载（`absent` 或 `removed`），更新（`latest`）
- `update_cache` 强制在安装前检查软件源缓存并更新，默认值为 `no`
  - 别名为 `expire-cache`
- `update_only` 只更新软件包而不安装不存在的包，默认值为 `no`
- `use_backend` 选择使用的 yum 后端，可以为 `auto`（默认值）, `yum`, `yum4` 或者 `dnf`。默认情况下会根据 `ansible_pkg_mgr` 的设置选择后端
- `validate_certs`: 是否验证软件源的证书，只对 `https` 软件源有效。默认值为 `yes`

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

