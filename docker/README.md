# Docker 基本使用

## 1. 安装 Docker

> [参考按照文档](https://mirror.tuna.tsinghua.edu.cn/help/docker-ce/)

### 1.1. 通过 apt 安装

1. 卸载旧的 Docker 组件

    ```bash
    sudo apt remove docker docker-engine docker.io
    ```

2. 安装依赖软件包

    ```bash
    sudo apt install apt-transport-https ca-certificates curl gnupg2 software-properties-common
    ```

3. 在 Debian 系统中

   1. 添加信任的 GPG 公钥

      ```bash
      curl -fsSL https://download.docker.com/linux/debian/gpg | sudo apt-key add -
      ```

   2. 添加软件源

      - AMD64

      ```bash
      sudo add-apt-repository \
        "deb [arch=amd64] https://mirrors.tuna.tsinghua.edu.cn/docker-ce/linux/debian \
        $(lsb_release -cs) \
        stable"
      ```

      - ARM64

      ```bash
      echo "deb [arch=armhf] https://mirrors.tuna.tsinghua.edu.cn/docker-ce/linux/debian \
        $(lsb_release -cs) stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list
      ```

4. 在 Ubuntu 系统中

   1. 添加信任的 GPG 公钥

      ```bash
      curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
      ```

   2. 添加软件源

      - AMD64

      ```bash
      sudo add-apt-repository \
        "deb [arch=amd64] https://mirrors.tuna.tsinghua.edu.cn/docker-ce/linux/ubuntu \
        $(lsb_release -cs) \
        stable"
      ```

      - ARM64

      ```bash
      echo "deb [arch=armhf] https://mirrors.tuna.tsinghua.edu.cn/docker-ce/linux/ubuntu \
        $(lsb_release -cs) stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list
      ```

5. 安装 Docker

   ```bash
   sudo apt update
   sudo apt install docker-ce
   ```

### 1.2. 通过 yum 安装

1. 卸载旧的 Docker 组件

    ```bash
    sudo yum remove docker docker-common docker-selinux docker-engine
    ```

2. 安装依赖软件包

    ```bash
    sudo yum install -y yum-utils device-mapper-persistent-data lvm2
    ```

3. 在 CentOS 系统中

   1. 下载 repo 文件

      ```bash
      wget -O /etc/yum.repos.d/docker-ce.repo https://download.docker.com/linux/centos/docker-ce.repo
      ```

   2. 替换软件源镜像地址

      ```bash
      sudo sed -i 's+download.docker.com+mirrors.tuna.tsinghua.edu.cn/docker-ce+' /etc/yum.repos.d/docker-ce.repo
      ```

4. 在 Fedora 系统中

   1. 下载 repo 文件

      ```bash
      wget -O /etc/yum.repos.d/docker-ce.repo https://download.docker.com/linux/fedora/docker-ce.repo
      ```

   2. 替换软件源镜像地址

      ```bash
      sudo sed -i 's+download.docker.com+mirrors.tuna.tsinghua.edu.cn/docker-ce+' /etc/yum.repos.d/docker-ce.repo
      ```

5. 安装 Docker

   ```bash
   sudo yum makecache fast
   sudo yum install docker-ce
   ```

### 1.3. 将当前用户加入 Docker 用户组

可以避免使用 root 权限使用 docker

```bash
sudo usermod -aG docker $(whoami)
```

## 2. 安装 docker-compose

### 2.1. 通过 apt 安装

```bash
sudo apt install docker-compose
```

### 2.2. 通过 yum 安装

```bash
yum install -y epel-release
yum install -y docker-compose
```

## 3. 设置 docker-hub 镜像地址 (以阿里云镜像为例)

### 3.1. 注册阿里云账号

到 <https://homenew.console.aliyun.com/> 注册账号

### 3.2. 创建镜像仓库

- 在 <https://cr.console.aliyun.com/> 创建新的实例 (如果需要私有镜像仓库)
- 在 <https://cr.console.aliyun.com/cn-hangzhou/instances/mirrors> 复制镜像地址 (如果仅使用 docker-hub 镜像)

### 3.3. 设置 docker-hub 镜像地址

编辑 `/etc/docker/daemon.json` 文件, 添加如下内容:

```json
{
  "registry-mirrors": [
    "https://*******.mirror.aliyuncs.com"
  ]
}
```

## 4. 安装 python

### 4.1. 安装 pyenv

- 下载并安装 pyenv

  ```bash
  curl -L https://github.com/pyenv/pyenv-installer/raw/master/bin/pyenv-installer | bash
  ```

- 设置环境变量: 修改 `~/.bashrc` (或 `~/.zshrc` 或 `~/.bash_profile`)文件, 添加如下内容:

  ```bash
  export PATH="~/.pyenv/bin:$PATH"
  eval "$(pyenv init -)"
  eval "$(pyenv virtualenv-init -)"
  ```

### 4.2. 安装指定版本 python

```bash
pyenv install <version>
```

### 4.3. 使用指定版本 python

在当前项目目录中

```bash
pyenv local <version>
```
