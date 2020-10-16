# Ansible

## 1. Setup environment

### 1.1. Install linux Virtual Machines

3 Debain VM need be installed:

- The VM IPs are: 192.168.100.[2, 3, 4]
- The user with sudoers name is: `alvin`, password is: `kkmoue`

### 1.2. Config ssh

#### 1.2.1. Create keygen

```bash
$ ssh-keygen -t rsa -P '' -C alvin
```

#### 1.2.2. Copy pubkey to VM

```bash
$ ssh-copy-id -i ~/.ssh/id_rsa.pub alvin@192.168.100.2
$ ssh-copy-id -i ~/.ssh/id_rsa.pub alvin@192.168.100.3
$ ssh-copy-id -i ~/.ssh/id_rsa.pub alvin@192.168.100.4
```

#### 1.2.3. Config ssh shortcuts

Edit `~/.ssh/config` file:

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

### 1.2. Install python

#### 1.2.1. Install pyenv

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

#### 1.2.2. Install python

```bash
$ pyenv install 3.7.5
```

#### 1.2.3. Use python

In notebook folder:

```bash
$ pyenv local 3.7.5
```

## 2. Setup notebook

[Use jupyter notebook](./notebook/README.md)
