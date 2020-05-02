How to use SSH
===

## 启动SSH服务

### Linux下

安装SSH命令（以Debian为例）：

```bash
# 安装SSH服务
$ sudo apt-get install openssh-server

# 安装SSH命令
$ sudo apt-get install ssh
```

启动/停止SSH server：

```bash
$ /etc/init.d/ssh start     # 启动服务
$ /etc/init.d/ssh stop      # 停止服务
$ /etc/init.d/ssh restart   # 重启服务
```

设置开机自启（以Ubuntu为例）：

```bash
# 编辑/etc/rc.local文件，在exit 0语句前加入

/etc/init.d/ssh start
```

### Mac下

1. 通过""菜单打开"System Preferences" , 打开"Sharing"设置面板
2. 勾选"Remote Login"前的复选框，使其生效

![启用ssh](images/enable-sftp-server-mac-os-x.jpg "启用ssh")


## 连接ssh主机

```bash
$ ssh -p 端口号 用户名@地址/主机名 -y
```

-p 参数用于指定端口，默认为22端口  
-y 参数表示对于系统提出的问题一律回答"yes"


例如: 

```bash
$ ssh alvin@192.168.1.133
$ ssh -p 10022 alvin@web-qa -y
```

## 利用密钥使用SSH


### 生成密钥

```bash
$ ssh-keygen -t rsa -P '' -C alvin
```
  
-t 参数指定了密钥算法，包括：dsa和rsa  
-P 参数用于指定生成密钥的密码，''表示不需要密码（空）  
-C 密钥注释，在公钥的尾部可以看到

> 如果取消`-P`参数，则需要回答系统提出的若干问题方能产生密钥


默认密钥存储在~/.ssh目录下，包括: 
* id_rsa: 私钥文件
* id_rsa.pub: 公钥文件


#### 指定存储密钥的文件

```bash
$ ssh-keygen -t rsa -f alvin-key-file -P ''
```

-f 指定密钥文件名称

在当前目录下产生密钥文件，包括:
* alvin-key-file: 私钥文件
* alvin-key-file.pub: 公钥文件


### 将公钥文件加入SSH主机

#### 通过scp命令将公钥文件传输到SSH主机

```bash
$ scp ~/.ssh/id_rsa.pub alvin@web-qa:/home/admin/tmp/id_rsa.pub 
```

#### 在SSH主机端，将公钥文件内容追加到`authorized_keys`文件末尾

```bash
$ cat ~/tmp/id_rsa.pub >> ~/.ssh/authorized_keys
```

> ~/.ssh目录需要700的权限：`chmod 700 ~/.ssh/`   
> authorized_keys文件需要有600的执行权限: `chmod 600 ~/.ssh/authorized_keys`


### 执行SSH命令

```bash
$ ssh -i ~/.ssh/alvin-key-file -p 10022 alvin@web-qa -y
```

-i 指定所需的私钥文件


## SSH Tunnel

### Local port forwarding

通过ssh主机，将本地主机某个端口映射到远程主机的指定端口。

本地主机访问本地指定端口，相当于访问远程主机的指定端口。

```bash
# 建立本地13306端口，经由bastion主机到db-server主机3306端口的连接
$ ssh -fgNL 13306:db-server:3306 alvin@bastion
```

-L 表示本地端口重定向到远端端口    
-f 后台运行   
-N 不执行命令（不加就必须执行条远端命令）   
-g 允许远端服务器连接本地转发端口（双向通讯）

其它范例：

```bash
# 建立本地10090端口，到bastion主机90端口的连接
$ ssh -NL 10090:bastion:90 alvin@bastion
```

### Remote port forwarding

将SSH主机的指定端口，通过本地主机，映射到远程主机的指定端口。

SSH主机访问指定端口，相当于访问远程主机的指定端口

```bash
# 建立lucy主机13306端口，经由本地主机，到db-server主机3306端口的连接
$ ssh -fgNR 13306:db-server:3306 alvin@lucy
```

-R 表示远端端口重定向到指定端口
-f 后台运行
-N 不执行命令（不加就必须执行条远端命令）
-g 允许远端服务器连接本地转发端口（双向通讯）

其它范例：

```bash
# 建立web-qa主机10080端口到本地主机80端口的连接
$ ssh -NR 10080:localhost:80 alvin@web-qa
```

## Proxy

可以通过SSH主机来作为本地主机的代理，让数据经由SSH主机和外部服务器进行交互

```bash
# 通过本地主机10330端口连接proxy主机，本地所有流量通过proxy主机转发
$ ssh -qTfnN -D 10330 alvin@proxy
# 相当于 ssh -qTfnN -D localhost:10330 alvin@proxy
```

-D 动态端口转发，相当于将所有端口的数据通过指定端口进行转发  
-f 后台运行   
-N 不执行命令（不加就必须执行条远端命令）   
-n 输出重定向到 /dev/null（不输出） 
-T 禁止分配伪终端设备    
-q 静默模式   

此时，将localhost:10030设置为代理服务器，即可访问网络

```bash
$ ssh -qTfnN -D lucy:10330 alvin@proxy
```

此时，将lucy:10030设置为代理服务器，即可访问网络


## SSH Config

