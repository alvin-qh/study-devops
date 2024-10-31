# Vagrant

- [Vagrant](#vagrant)
  - [1. VagrantBox](#1-vagrantbox)
    - [1.1. 添加镜像](#11-添加镜像)
      - [1.1.1. 从官方仓库安装镜像](#111-从官方仓库安装镜像)
      - [1.1.2. 从下载镜像文件并安装](#112-从下载镜像文件并安装)
    - [1.2. 查看已安装的 VagrantBox](#12-查看已安装的-vagrantbox)
    - [1.3. 删除已安装的 VagrantBox](#13-删除已安装的-vagrantbox)
  - [2. 使用 Vagrant](#2-使用-vagrant)
    - [2.1. 初始化](#21-初始化)
    - [2.2. 使用虚拟机](#22-使用虚拟机)
      - [2.2.1. 启动虚拟机](#221-启动虚拟机)
      - [2.2.2. 显示 `ssh-config` 配置](#222-显示-ssh-config-配置)
      - [2.2.3. ssh 到虚拟机终端](#223-ssh-到虚拟机终端)
      - [2.2.4. 挂起和恢复](#224-挂起和恢复)
      - [2.2.5. 关闭虚拟机](#225-关闭虚拟机)
      - [2.2.6. 重新加载 `VagrantFile`](#226-重新加载-vagrantfile)
      - [2.2.7. 验证 `Vagrantfile`](#227-验证-vagrantfile)
      - [2.2.8. 查看虚拟机状态](#228-查看虚拟机状态)
    - [2.3. 打包新的 vagrantbox 文件](#23-打包新的-vagrantbox-文件)

Vagrant 是一个虚拟机管理工具，可以将各类虚拟机的使用脚本化，可以快速地构建单虚拟机或虚拟集群，用于进行软件测试，架构测试

## 1. VagrantBox

VagrantBox 是 Vagrant 管理虚拟镜像的组件

### 1.1. 添加镜像

#### 1.1.1. 从官方仓库安装镜像

```bash
$ vagrant box add debian/buster64 --name debian_buster

==> box: Loading metadata for box 'debian/buster64'
    box: URL: https://vagrantcloud.com/debian/buster64
This box can work with multiple providers! The providers that it
can work with are listed below. Please review the list and choose
the provider you will be working with.

1) libvirt
2) virtualbox
```

这里选择 2，使用 Virtualbox 作为 backend

```bash
Enter your choice: 2
==> box: Adding box 'debian/buster64' (v10.4.0) for provider: virtualbox
    box: Downloading: https://vagrantcloud.com/debian/boxes/buster64/versions/10.4.0/providers/virtualbox.box
==> box: Box download is resuming from prior download progress
...
```

至此 debian/buster64 虚拟镜像安装完毕

#### 1.1.2. 从下载镜像文件并安装

从官方仓库下载镜像，这里仍以 Virtualbox 为 backend，所以需下载 Virtualbox 镜像。
[下载链接](https://vagrantcloud.com/debian/boxes/buster64/versions/10.4.0/providers/virtualbox.box)

下载完毕后，即可将下载的镜像添加到 VagrantBox 中

```bash
$ vagrant box add ./virtualbox.box --name debian_buster
```

### 1.2. 查看已安装的 VagrantBox

```bash
$ vagrant box list -i
```

### 1.3. 删除已安装的 VagrantBox

```bash
$ vagrant box remove debian_buster
```

## 2. 使用 Vagrant

通过 `VagrantFile` 脚本文件来定义和启动虚拟机

### 2.1. 初始化

生成 `VagrantFile` 脚本文件

```bash
$ vagrant init debian_buster
```

在 `debian_buster` 目录下生成 `VagrantFile` 文件

### 2.2. 使用虚拟机

#### 2.2.1. 启动虚拟机

```bash
$ vagrant up [--provision]
```

- `--provision` 是否在启动虚拟机后执行 `config.vm.provision` 块，一般用来初始化虚拟机

#### 2.2.2. 显示 `ssh-config` 配置

```bash
$ vagrant ssh-config
```

#### 2.2.3. ssh 到虚拟机终端

```bash
$ vagrant ssh debian1
```

#### 2.2.4. 挂起和恢复

```bash
$ vagrant suspend
$ vagrant resume
```

#### 2.2.5. 关闭虚拟机

```bash
$ vagrant halt
```

#### 2.2.6. 重新加载 `VagrantFile`

```bash
$ vagrant reload
```

#### 2.2.7. 验证 `Vagrantfile`

```bash
$ vagrant validate
```

#### 2.2.8. 查看虚拟机状态

```bash
$ vagrant status
```

### 2.3. 打包新的 vagrantbox 文件

```bash
$ vagrant package --base debian1
```
