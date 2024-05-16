# Nginx

## 1. Setup environment

### 1.1. Install docker

- Install docker

```bash
$ sudo apt install docker-ce docker-ce-cli containerd.io
```

- Add current user into docker group

```bash
$ sudo usermod -aG docker $(whoami)
```

## 2. Install and setup

### 2.1. macOS

#### 2.1.1. Install

```bash
$ brew install nginx
```

Nginx will be installed in '`/usr/local/Cellar/nginx/<version>`' and make link to '`/usr/local/opt/nginx`'

Default docroot is: `/usr/local/var/www`
Default config file is: `/usr/local/etc/nginx/nginx.conf`

#### 2.1.2. Setup

Start as service

```bash
$ brew services start nginx
```

Start with default base path and config

```bash
$ nginx
```

Start with path and config

```bash
$ nginx -p <base path> -c <config file>
```

### 2.2. Debian

Example of `Debian jessie`

#### 2.2.1. Install

Setup deb source:

Edit `/etc/apt/sources.list` file and  add the flowing content:

```paint
deb http://nginx.org/packages/debian/ jessie nginx
deb-src http://nginx.org/packages/debian/ jessie nginx
```

Import the key of nginx

```bash
$ wget http://nginx.org/keys/nginx_signing.key
$ apt-key add nginx_signing.key
```

Install nginx

```bash
$ apt-get update
$ apt-get install nginx
```

The default config file is: `/etc/nginx/nginx.conf`
The executable file is: `/usr/sbin/nginx`
The PID file is: `/var/run/nginx.pid`

#### 2.2.2. Setup

Startup with default base path and config:

```bash
$ /etc/init.d/nginx start
```

Startup by user define path and config:

```bash
$ /usr/sbin/nginx -p <base path> -c <config file>
```
