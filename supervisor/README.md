# Supervisor

- [Supervisor](#supervisor)
  - [1. 安装](#1-安装)
    - [1.1. 安装 supervisor](#11-安装-supervisor)
    - [1.2. 启动 supervisor](#12-启动-supervisor)
  - [2. 设置 supervisor](#2-设置-supervisor)
    - [2.1. 生成初始配置文件（可选）](#21-生成初始配置文件可选)
    - [2.2. 编辑配置文件](#22-编辑配置文件)
      - [2.2.1. 启用 Web 服务](#221-启用-web-服务)
      - [2.2.2. 设置包含的子配置文件](#222-设置包含的子配置文件)
      - [2.2.3. 配置启动进程](#223-配置启动进程)
      - [2.2.4. 配置进程组](#224-配置进程组)
      - [2.2.5. 使用环境变量](#225-使用环境变量)
      - [2.2.6. 启动多进程](#226-启动多进程)
  - [3. 使用方法](#3-使用方法)
    - [3.1. 启动进程](#31-启动进程)
    - [3.2. 关闭进程](#32-关闭进程)
    - [3.3. 管理进程](#33-管理进程)
      - [3.3.1. 通过命令行管理进程](#331-通过命令行管理进程)
      - [3.3.2. 交互式 Shell](#332-交互式-shell)

> [官网地址](http://supervisord.org/)

## 1. 安装

### 1.1. 安装 supervisor

```bash
$ pip install supervisor
```

### 1.2. 启动 supervisor

`supervisor` 默认的主配置文件为 `supervisord.conf`，会从以下默认位置查找配置

- `../etc/supervisord.conf` 相对于执行进程
- `../supervisord.conf` 相对于执行进程
- `$CMD/supervisord.conf` `$CMD` 表示命令行启动路径
- `$CMD/etc/supervisord.conf` `$CMD` 表示命令行启动路径
- `/etc/supervisord.conf` 绝对路径
- `/etc/supervisor/supervisord.conf` 绝对路径

除此外，可以使用 `-c` 参数指定配置文件

```bash
$ supervisord [-c <config file>]
```

## 2. 设置 supervisor

### 2.1. 生成初始配置文件（可选）

```bash
$ sudo echo_supervisord_conf > /etc/supervisord.conf
```

在 `/etc/` 目录下生成默认的 `supervisord.conf` 配置文件

### 2.2. 编辑配置文件

> 查看 [官方文档](http://supervisord.org/configuration.html)

#### 2.2.1. 启用 Web 服务

可以启动一个 web 服务，用来查看 supervisor 管理的进程情况

```ini
[inet_http_server]
port=127.0.0.1:9001        ; 监听的地址和端口, *:port 表示监听任意 iface 地址
username=user              ; 登录用户名，默认无需用户名
password=123               ; 登陆密码，默认无需密码
```

#### 2.2.2. 设置包含的子配置文件

```ini
[include]
files = /etc/supervisor/*.conf  ; 包含子进程配置的路径
```

#### 2.2.3. 配置启动进程

可以在 `/etc/supervisor/` 下为要监控的进程创建一个配置文件，例如：`show-time.conf`

```ini
[program:show-time]
directory = .        ; 进程启动路径
command = sh work    ; 启动进程的命令行
autostart = true     ; 是否自动启动
startsecs = 5        ; 启动时间，5 秒内完成启动则视为成功
autorestart = true   ; 失败后是否自动重启
startretries = 3     ; 重启失败的重试次数
user = alvin         ; 启动进程的系统用户
redirect_stderr = true  ; 将 stderr 输出转入到 stdout
stdout_logfile_maxbytes = 20MB  ; 每个 log 文件的最大容量
stdout_logfile_backups = 20     ; 最大备份文件数量
stdout_logfile = logs/show-time.log ; log 存储的文件名
stopasgroup = true   ; 可以按组停止
killasgroup = true   ; 可以按组 kill 进程
environment = KEY1="value1",KEY2="value2" ; 进程环境变量
...
```

注意：log 存储路径须事先建立，否则会报告错误

#### 2.2.4. 配置进程组

进程组可以批量管理进程，这对进程间有依赖性的进程组管理提供变例

```ini
[group:work-group]      ; 进程分组名
programs=name1,name2    ; 组内进程名，即上节中 program:<name> 的名称
priority=999            ; 优先级
...
```

#### 2.2.5. 使用环境变量

```ini
[program:example]
command=/usr/bin/example --loglevel=%(ENV_LOGLEVEL)s
```

- `%(ENV_X)s` 表示环境变量 `X`

#### 2.2.6. 启动多进程

按照指定的数量一次性启动多个进程

```ini
[program:web]
command = python /opt/www/site/serv.py 80%(process_num)02d ; 启动进程的命令行
process_name = %(program_name)s_%(process_num)02d ; 进程名称
numprocs = 4 ; 启动进程数量
numprocs_start = 1 ; 进程编号起始值
...
```

- `%(program_name)s`: 进程名称
- `%(process_num)02d`: 进程编号

## 3. 使用方法

### 3.1. 启动进程

指定配置文件启动 supervisor

```bash
$ supervisord -c /etc/supervisord.conf
```

查看 supervisor 进程是否正常启动

```bash
$ ps aux | grep supervisord
```

### 3.2. 关闭进程

```bash
$ supervisorctl shutdown
```

### 3.3. 管理进程

#### 3.3.1. 通过命令行管理进程

```bash
# 显示所有进程状态
$ supervisorctl status

# 启动、停止、重启进程
$ supervisorctl start show-time
$ supervisorctl stop show-time
$ supervisorctl restart show-time

# 启动、停止、重启进程组
$ supervisorctl start work-group
$ supervisorctl stop work-group
$ supervisorctl restart work-group

# 启动、停止、重启所有进程
$ supervisorctl start all
$ supervisorctl stop all
$ supervisorctl restart all

# 重新加载进程
$ supervisorctl reload

# 更新进程配置
$ supervisorctl update
```

注意：如果一个进程已经停止，并且没有 `autorestart` 配置，则 `reload` 和 `update` 也不会启动这些进程

#### 3.3.2. 交互式 Shell

执行不带参数的 `supervisorctl`，既可以进入交互式界面，输入对应的命令完成操作即可

```bash
supervisor> help
default commands (type help <topic>):
=====================================
add    exit      open  reload  restart   start   tail
avail  fg        pid   remove  shutdown  status  update
clear  maintail  quit  reread  signal    stop    version

supervisor> stop show-time
show-time: stopped

supervisor> start show-time
show-time: started

supervisor> status
show-time                       RUNNING   pid 1258, uptime 0:00:04

supervisor> restart show-time
show-time: stopped
show-time: started
```
