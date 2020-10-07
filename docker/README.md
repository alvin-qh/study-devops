# Use Docker

## 1. Install docker

> [See also](https://mirror.tuna.tsinghua.edu.cn/help/docker-ce/)

### 1.1. Use apt

1. Uninstall installed docker
    ```bash
    $ sudo apt remove docker docker-engine docker.io
    ```
2. Install dependency packages
    ```bash
    $ sudo apt install apt-transport-https ca-certificates curl gnupg2 software-properties-common
    ```
3. In Debain
   1. Trust GPG public-key
      ```bash
      $ curl -fsSL https://download.docker.com/linux/debian/gpg | sudo apt-key add -
      ```
   2. Add repository
      - AMD64
      ```bash
      $ sudo add-apt-repository \
        "deb [arch=amd64] https://mirrors.tuna.tsinghua.edu.cn/docker-ce/linux/debian \
        $(lsb_release -cs) \
        stable"
      ```
      - ARM64
      ```bash
      $ echo "deb [arch=armhf] https://mirrors.tuna.tsinghua.edu.cn/docker-ce/linux/debian \
        $(lsb_release -cs) stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list
      ```
4. In Ubuntu
   1. Trust GPG public-key
      ```bash
      $ curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
      ```
   2. Add repository
      - AMD64
      ```bash
      $ sudo add-apt-repository \
        "deb [arch=amd64] https://mirrors.tuna.tsinghua.edu.cn/docker-ce/linux/ubuntu \
        $(lsb_release -cs) \
        stable"
      ```
      - ARM64
      ```bash
      $ echo "deb [arch=armhf] https://mirrors.tuna.tsinghua.edu.cn/docker-ce/linux/ubuntu \
        $(lsb_release -cs) stable" | \
        sudo tee /etc/apt/sources.list.d/docker.list
      ```
5. Install
   ```bash
   $ sudo apt update
   $ sudo apt install docker-ce
   ```

### 1.2. Use yum

1. Uninstall installed docker
    ```bash
    $ sudo yum remove docker docker-common docker-selinux docker-engine
    ```
2. Install dependency packages
    ```bash
    $ sudo yum install -y yum-utils device-mapper-persistent-data lvm2
    ```
3. In CentOS
   1. Download repo file
      ```bash
      $ wget -O /etc/yum.repos.d/docker-ce.repo https://download.docker.com/linux/centos/docker-ce.repo
      ```
   2. Replace repository address
      ```bash
      $ sudo sed -i 's+download.docker.com+mirrors.tuna.tsinghua.edu.cn/docker-ce+' /etc/yum.repos.d/docker-ce.repo
      ```
4. In Fedora
   1. Download repo file
      ```bash
      $ wget -O /etc/yum.repos.d/docker-ce.repo https://download.docker.com/linux/fedora/docker-ce.repo
      ```
   2. Replace repository address
      ```bash
      $ sudo sed -i 's+download.docker.com+mirrors.tuna.tsinghua.edu.cn/docker-ce+' /etc/yum.repos.d/docker-ce.repo
      ```
5. Install
   ```bash
   $ sudo yum makecache fast
   $ sudo yum install docker-ce
   ```

### 1.3. Add current user into docker group

```bash
$ sudo usermod -aG docker $(whoami)
```

## 2. Install docker-compose

### 2.1. Use apt

```bash
$ sudo apt install docker-compose
```

### 2.2. Use yum
```bash
$ yum install -y epel-release
$ yum install -y docker-compose
```

## 3. Setup docker-hub mirror (Aliyun mirror)

### 3.1. Register Aliyun account

Goto https://homenew.console.aliyun.com/ and register account

### 3.2. Create docker mirror service

- Goto https://cr.console.aliyun.com/, create new instance (If need private docker repository)
- Goto https://cr.console.aliyun.com/cn-hangzhou/instances/mirrors copy docker-hub mirror url (If use docker-hub mirror)

## 4. Install python

### 4.1. Install pyenv

- Download and install pyenv
  ```bash
  $ curl -L https://github.com/pyenv/pyenv-installer/raw/master/bin/pyenv-installer | bash
  ```

- Set shell enviroment: modify `~/.bashrc` (or `~/.zshrc` or `~/.bash_profile`), and add the following content
  ```bash
  export PATH="~/.pyenv/bin:$PATH"
  eval "$(pyenv init -)"
  eval "$(pyenv virtualenv-init -)"
  ```

### 4.2. Install special version python

```bash
$ pyenv install 3.8.5
```

### 4.3. Use python

In notebook folder:
```bash
$ pyenv local 3.8.5
```

[Use jupyter notebook](./notebook/README.md)
