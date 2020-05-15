# How to use SSH

## 1. 启动SSH服务

### 1.1. Linux下

- 安装SSH命令（以Debian为例）：

```bash
# 安装SSH服务
$ sudo apt-get install openssh-server

# 安装SSH命令
$ sudo apt-get install ssh
```

- 启动/停止SSH server：

```bash
$ /etc/init.d/ssh start     # 启动服务
$ /etc/init.d/ssh stop      # 停止服务
$ /etc/init.d/ssh restart   # 重启服务
```

- 设置开机自启（以Ubuntu为例）：

```bash
# 编辑/etc/rc.local文件，在exit 0语句前加入

/etc/init.d/ssh start
```

### 1.2. Mac下

1. 打开"System Preferences" , 打开"Sharing"设置面板
2. 勾选"Remote Login"前的复选框，使其生效

![启用ssh](images/enable-sftp-server-mac-os-x.jpg "启用ssh")


## 2. 连接 ssh 主机

```bash
$ ssh -p <port> <user>@<host or ip> -y
```

- `-p` 参数用于指定端口，默认为`22`端口  
- `-y` 参数表示对于系统提出的问题一律回答`yes`


例如: 

```bash
$ ssh alvin@192.168.1.133
$ ssh -p 10022 alvin@web-qa -y
```

## 3. 利用密钥使用SSH


### 3.1. 生成密钥

```bash
$ ssh-keygen -t rsa -P '' -C <string>
```
  
- `-t` 参数指定了密钥算法，包括：dsa和rsa  
- `-P` 参数用于指定生成密钥的密码，''表示不需要密码（空）  
- `-C` 密钥注释，在公钥的尾部可以看到

> 如果取消`-P`参数，则需要回答系统提出的若干问题方能产生密钥


默认密钥存储在`~/.ssh`目录下，包括: 
- `id_rsa`: 私钥文件
- `id_rsa.pub`: 公钥文件


### 3.2. 指定存储密钥的文件

```bash
$ ssh-keygen -t rsa -f <key file name> -P ''
```

- `-f` 指定密钥文件名称

在当前目录下产生密钥文件，包括:
- `<key file>`: 私钥文件
- `<key file>.pub`: 公钥文件


### 3.3. 将公钥文件加入SSH主机

#### 3.3.1. 手动处理

- Client 端

```bash
$ scp ~/.ssh/id_rsa.pub <name>@<host>:/home/<user>/id_rsa.pub 
```

- Server 端

```bash
$ cat ~/id_rsa.pub >> ~/.ssh/authorized_keys

# 如果有权限问题
$ chmod 700 ~/.ssh/
$ chomd 600 ~/.ssh/authorized_keys
```

### 3.4. 执行SSH命令

```bash
$ ssh -i ~/.ssh/<key file> -p 10022 <user>@<host> -y
```

- `-i` 指定所需的私钥文件


## 4. SSH Tunnel

### 4.1. Local port forwarding

通过远程主机A（ssh可连接），将本机的指定端口m映射到远程主机B（可通过A放达）的指定端口n上。

本机访问本地指定端口，相当于访问远程主机的指定端口。

```bash
$ ssh -fgNL <local port>:<remote host>:<remote port> <user>@<bastion host>
```

- `-L` 表示本地端口重定向到远端端口    
- `-f` 后台运行   
- `-N` 不连接远程终端   
- `-g` 允许远端服务器连接本地转发端口（双向通讯）

范例：

```bash
# 建立本地10090端口，通过bastion主机，到web01主机90端口的连接
$ ssh -NL 10090:web01:90 alvin@bastion
```

### 4.2. Remote port forwarding

将一台远程主机A的指定端口a，经由本机，映射到另一台远程主机B的指定端口b上。

A主机访问指定端口a，相当于访问B主机的指定端口b。

```bash
$ ssh -fgNR 13306:<remote 1>:3306 <user>@<remote 2>
```

- `-R` 表示远端端口重定向到指定端口
- `-f` 后台运行
- `-N` 不连接远程终端
- `-g` 允许远端服务器连接本地转发端口（双向通讯）

范例：

```bash
# 建立web-qa主机10080端口到本地主机80端口的连接
$ ssh -NR 10080:localhost:80 alvin@web-qa
```

## 4.3. Proxy

可以通过SSH主机来作为本地主机的代理，让数据经由SSH主机和外部服务器进行交互

```bash
$ ssh -qTfnN -D <port> <user>@<host>
# 相当于 ssh -qTfnN -D localhost:10330 alvin@proxy
```

- `-D` 动态端口转发，相当于将所有端口的数据通过指定端口进行转发  
- `-f` 后台运行   
- `-N` 不执行命令（不加就必须执行条远端命令）   
- `-n` 输出重定向到 `/dev/null`（不输出） 
- `-T` 禁止分配伪终端设备    
- `-q` 静默模式   

此时，将`localhost:10030`设置为代理服务器，即可访问网络

```bash
$ ssh -qTfnN -D web01:10330 alvin@proxy
# 此时，将 lucy:10030 设置为代理服务器，即可访问网络
```

## 5. SSH Config

## 6. Troubleshooting

### 6.1. sshd: no hostkeys available -- exiting.

```bash
$ sudo ssh-keygen -t dsa -f /etc/ssh/ssh_host_dsa_key
$ sudo ssh-keygen -t rsa -f /etc/ssh/ssh_host_rsa_key
$ sudo chmod 600 /etc/ssh
```
