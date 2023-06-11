# Ansible

## 1. 设置开发环境

### 1.1. 安装 Linux 虚拟机

需要 3 台 Debian 虚拟机

- 三台虚拟机的内网 IP 设置为: 192.168.100.[2, 3, 4]
- 设定相同的用户名和密码
- 关闭防火墙

### 1.2. 配置 ssh

#### 1.2.1. 创建密钥

在本机上执行如下命令, 生成密钥:

```bash
ssh-keygen -t rsa -P '' -C <username>
```

#### 1.2.2. 将生成的公钥文件拷贝到虚拟机

```bash
ssh-copy-id -i ~/.ssh/id_rsa.pub <username>@192.168.100.2
ssh-copy-id -i ~/.ssh/id_rsa.pub <username>@192.168.100.3
ssh-copy-id -i ~/.ssh/id_rsa.pub <username>@192.168.100.4
```

#### 1.2.3. 配置 ssh config 文件

编辑本机 `~/.ssh/config` 文件:

```plain
Host vsrv*
    IdentityFile ~/.ssh/id_rsa
    User alvin
    Port 22

Host vsrv01
    HostName 192.168.100.2

Host vsrv02
    HostName 192.168.100.3

Host vsrv03
    HostName 192.168.100.4
```

### 1.2. 安装 Python

在本机安装 Python 开发环境

#### 1.2.1. 安装 PyENV

##### 下载并安装 PyENV 工具链

```bash
curl -L https://github.com/pyenv/pyenv-installer/raw/master/bin/pyenv-installer | bash
```

##### 设置环境变量

修改 `~/.bashrc` (或 `~/.zshrc` 以及 `~/.bash_profile` 文件), 增加如下内容:

```bash
export PATH="~/.pyenv/bin:$PATH"
eval "$(pyenv init -)"
eval "$(pyenv virtualenv-init -)"
```

#### 1.2.2. 安装 Python

##### 列出可按照的 Python 版本

```bash
pyenv install --list
```

##### 安装所需版本的 Python 解释器

```bash
pyenv install <version>
```

## 2. 执行前准备

### 2.1. 创建 ssh 的 control path 目录

```bash
mkdir .cp
```
