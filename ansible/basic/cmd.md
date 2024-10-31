# Ansible

- [Ansible](#ansible)
  - [1. 基本设置](#1-基本设置)
    - [1.1. 配置 Ansible](#11-配置-ansible)
    - [1.2. 配置 Inventory](#12-配置-inventory)
    - [1.3. 配置 ssh](#13-配置-ssh)
  - [2. Ansible 命令行模式](#2-ansible-命令行模式)
    - [2.1. ping 模块](#21-ping-模块)
      - [2.1.1. 执行最基本的 `ping` 模块](#211-执行最基本的-ping-模块)
      - [2.1.2. 指定远程用户](#212-指定远程用户)
      - [2.1.3. 以 `sudo` 权限执行](#213-以-sudo-权限执行)
      - [2.1.4. 指定 `sudo` 密码](#214-指定-sudo-密码)
      - [2.1.5. 以 `root` 用户执行](#215-以-root-用户执行)
      - [2.1.6. 通过 `expect` 脚本自动输入密码](#216-通过-expect-脚本自动输入密码)
    - [2.2. 执行远程命令](#22-执行远程命令)
      - [2.2.1. 执行一个远程命令](#221-执行一个远程命令)
      - [2.2.2. 执行命令失败（权限不足）](#222-执行命令失败权限不足)
      - [2.2.3. 通过 `become` 提权](#223-通过-become-提权)

## 1. 基本设置

### 1.1. 配置 Ansible

> 参考 [`ansible.cfg`](./ansible.cfg) 文件

在 `ansible.cfg` 文件中, 主要需要指定远程服务器的配置（`Inventory` 配置文件）以及所使用的 ssh 配置

### 1.2. 配置 Inventory

> 参考 [`inventory`](./conf/inventory) 文件

在 `inventory` 配置文件中, 主要需配置要连接的远程主机以及远程访问该主机所需的参数, 例如:

```ini
[web_servers]
web01
web02
web03

[web_servers:vars]
ansible_ssh_user=<username>
```

假设 Web 服务部署需要 3 台机器（web01~03）, 且这 3 台机器需要完全一致的部署流程, 则可以将这三台主机统一设置为一个远程主机, 统一管理

### 1.3. 配置 ssh

> 参考 [`ssh_config`](./conf/ssh_config) 文件

`inventory` 配置中的主机, 都需要在 ssh 配置中定义, 可以使用 `~/.ssh` 中的 `config` 文件进行定义, 也可以在 `ansible.cfg` 文件中通过 `ssh_args` 配置来指定额外的 ssh 配置文件和密钥文件

## 2. Ansible 命令行模式

Ansible 命令行模式, 即通过 `ansible` 命令执行 Ansible 内置的模块或远程服务器的 shell 命令

执行内置模块

```bash
ansible <主机名> -m <模块名> [参数]
```

执行命令行

```bash
ansible <主机名> -a "<命令行>" [参数]
```

### 2.1. ping 模块

`ping` **模块**用于测试本地机器和远程机器的 ssh 连通性

#### 2.1.1. 执行最基本的 `ping` 模块

```bash
ansible group_debian1 -m ping

debian1 | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
```

#### 2.1.2. 指定远程用户

```bash
ansible group_debian1 -m ping --user=alvin --ask-become-pass

BECOME password:
debian1 | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
```

- `--user` 指定远程用户
- `--ask-become-pass` 要求输入密码

#### 2.1.3. 以 `sudo` 权限执行

```bash
ansible group_debian1 -m ping -b --ask-become-pass

BECOME password:
debian1 | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
```

- `-b` 以 `become` 方式执行
- `--ask-become-pass` 要求输入密码

`become` 方式即通过 `sudo` 方式提升权限来执行模块

#### 2.1.4. 指定 `sudo` 密码

```bash
ansible group_debian1 -m ping -b --extra-vars "ansible_become_pass=<password>"

debian1 | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
```

- `--extra-vars` 设置扩展参数, `ansible_become_pass` 表示设置 `sudo` 用户密码

#### 2.1.5. 以 `root` 用户执行

```bash
ansible group_debian1 -m ping -b --become-method=su --extra-vars "ansible_become_pass=<password>"

debian1 | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
```

- `--become-method` 切换用户的命令, 默认为 `sudo`。`su` 表示切换到 `root` 用户

#### 2.1.6. 通过 `expect` 脚本自动输入密码

```bash
expect -c '
  set timeout -1;
  spawn ansible group_debian1 -m ping -b --ask-become-pass
  expect {
      "BECOME*password*" {send "<password>\r"}
  }
  expect eof;'

spawn ansible group_debian1 -m ping -b --ask-become-pass
BECOME password:
debian1 | SUCCESS => {
    "changed": false,
    "ping": "pong"
}
```

### 2.2. 执行远程命令

#### 2.2.1. 执行一个远程命令

```bash
ansible group_debian1 -a "echo Hello World"

debian1 | CHANGED | rc=0 >>
Hello World
```

#### 2.2.2. 执行命令失败（权限不足）

```bash
ansible group_debian1 -a "ifconfig"

debian1 | FAILED | rc=2 >>
[Errno 2] No such file or directory: b'ifconfig': b'ifconfig'
```

#### 2.2.3. 通过 `become` 提权

```bash
ansible group_debian1 -b -a "ifconfig" --extra-vars "ansible_become_pass=<password>"

debian1 | CHANGED | rc=0 >>
eth0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 172.25.186.215  netmask 255.255.240.0  broadcast 172.25.191.255
        inet6 fe80::215:5dff:fe83:1f06  prefixlen 64  scopeid 0x20<link>
        ether 00:15:5d:83:1f:06  txqueuelen 1000  (Ethernet)
        RX packets 616  bytes 93019 (90.8 KiB)
        RX errors 0  dropped 0  overruns 0  frame 0
        TX packets 146  bytes 16597 (16.2 KiB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0

...
```
