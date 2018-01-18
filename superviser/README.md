Supervisor
===

[官网](http://supervisord.org/)

## 安装

```sh
$ pip install supervisor
```



## 配置

### 创建配置文件

```bash
$ echo_supervisord_conf > /etc/supervisord.conf
```

配置文件可以创建在任意位置

### 编辑配置文件

开启WEB服务，例如：

```ini
[inet_http_server]
port=127.0.0.1:9001        ; ip_address:port specifier, *:port for all iface
username=user              ; default is no username (open server)
password=123               ; default is no password (open server)
```

设置子配置文件，例如：

```ini
[include]
files = /etc/supervisor/*.conf	; 包含子配置文件 
```

在子配置文件中，配置进程，例如配置名称为`show-time`的进程：

```ini
[program:show-time]
directory = .		 ; 程序的启动目录
command = sh ./work  ; 启动命令
autostart = true
startsecs = 5        ; 启动 5 秒后没有异常退出，就当作已经正常启动了
autorestart = true   ; 程序异常退出后自动重启
startretries = 3     ; 启动失败自动重试次数，默认是 3
user = alvin         ; 用哪个用户启动
redirect_stderr = true  ; 把 stderr 重定向到 stdout，默认 false
stdout_logfile_maxbytes = 20MB  ; stdout 日志文件大小，默认 50MB
stdout_logfile_backups = 20     ; stdout 日志文件备份数
stdout_logfile = logs/show-time.log 	; stdout 日志文件，需要注意当指定目录不存在时无法正常启动，所以需要手动创建目录（supervisord 会自动创建日志文件）
stopasgroup = true
killasgroup = true
```

配置进程组

```ini
[group:work-group]
programs=name1,name2  	; 这里的进程名是上文 [program:name] 定义的 name
priority=999            ; the relative start priority (default 999)
```



## 基本使用

### 启动服务

通过`supervisord`可以直接启动

```bash
$ supervisord -c /etc/supervisord.conf
```

`-c` 参数用于指定配置文件的位置，如果省略`-c`参数，则按照`$CWD/supervisord.conf, $CWD/etc/supervisord.conf, /etc/supervisord.conf`自动搜索配置文件。

如果`supervisord`服务已经启动，则可通过如下命令检查：

```bash
$ ps aux | grep supervisord
```

### 停止服务

```bash
$ supervisorctl shutdown
```

### 管理进程

#### 命令式

```bash
# 查询各进程运行状态
$ supervisorctl status

# 启、停、重启业务进程, show-time 为进程名,即[program:show-time]里配置的值
$ supervisorctl start show-time
$ supervisorctl stop show-time
$ supervisorctl restart show-time

#重启所有属于名为 work-group 这个分组的进程
$ supervisorctl start work-group
$ supervisorctl stop work-group
$ supervisorctl restart work-group

#启、停、重启全部进程(不会载入最新的配置文件)
$ supervisorctl start all
$ supervisorctl stop all
$ supervisorctl restart all

#重新加载配置文件.停止原有进程并按新的配置启动所有进程
$ supervisorctl reload

#根据最新的配置文件,启动新配置或有改动的进程,配置没有改动的进程不会受影响而重启。
$ supervisorctl update
#注意:显示用stop停止掉的进程，用reload或者update都不会自动重启
```

#### 交互式

通过不带参数`supervisorctl`命令可以进入到交互模式下，完成操作：

```bash
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



