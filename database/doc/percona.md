# Percona

## 1. 安装

### 1.1. Debian 系

- 获取安装包

```bash
wget https://repo.percona.com/apt/percona-release_latest.$(lsb_release -sc)_all.deb
```

- 安装包

```bash
sudo dpkg -i percona-release_latest.$(lsb_release -sc)_all.deb
```

安装完毕后，会在软件源中添加 `/etc/apt/sources.list.d/percona-release.list` 文件

- 安装服务端软件

```bash
sudo apt update
sudo apt install percona-server-server-5.7
```

- 启动 percona

```bash
service start mysql
```
