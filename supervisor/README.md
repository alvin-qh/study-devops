# Supervisor

[Offical](http://supervisord.org/)

## 1. Install

### 1.1. Install

```bash
$ pip install supervisor
```

### 1.2. Startup

```bash
$ supervisord -c <config file>
```

## 2. Setup

### 2.1. Create config file (Optional)

```bash
$ sudo echo_supervisord_conf > /etc/supervisord.conf
```
### 2.2. Edit config file

[Offical Document](http://supervisord.org/configuration.html)

#### 2.2.1. Enable webservice:

```ini
[inet_http_server]
port=127.0.0.1:9001        ; ip_address:port specifier, *:port for all iface
username=user              ; default is no username (open server)
password=123               ; default is no password (open server)
```

#### 2.2.2. Include sub config files:

```ini
[include]
files = /etc/supervisor/*.conf	; sub config files 
```

#### 2.2.3. Config for process

For example, a process named `show-time`, config in `/etc/supervisor/show-time.conf` file

```ini
[program:show-time]
directory = .		 ; process startup path
command = sh ./work  ; startup command
autostart = true
startsecs = 5        ; 5 seconds after start-up is considered a success
autorestart = true   ; auto restart if error caused
startretries = 3     ; retry times
user = alvin         ; user to startup process
redirect_stderr = true  ; redirect stderr into stdout (false is default)
stdout_logfile_maxbytes = 20MB  ; log size for stdout (50MB for default)
stdout_logfile_backups = 20     ; max backups for stdout log
stdout_logfile = logs/show-time.log 	; log file for stdout
stopasgroup = true
killasgroup = true
environment = KEY1="value1",KEY2="value2"	; environment variables
...
```

#### 2.2.4. Config process group

```ini
[group:work-group]      ; group name
programs=name1,name2  	; processes in this group
priority=999            ; the relative start priority (default 999)
...
```
> All path is relative from `supervisord` path

#### 2.2.5. Use environment variable

```ini
[program:example]
command=/usr/bin/example --loglevel=%(ENV_LOGLEVEL)s
```

> `%(ENV_X)s` means environment vriable `X`

#### 2.2.6. Startup multi-process

```ini
[program:web]
command = python /opt/www/site/serv.py 80%(process_num)02d
process_name = %(program_name)s_%(process_num)02d
numprocs=4
numprocs_start=1
...
```

> `%(program_name)s`: name of processes<br>
> `%(process_num)02d`: number of process

## 3. Usage

### 3.1. Start service

Start by specific config file

```bash
$ supervisord -c /etc/supervisord.conf
```

`-c` parameter is used to specify config file, by default, the config file would be found by order `$CWD/supervisord.conf, $CWD/etc/supervisord.conf, /etc/supervisord.conf` 

Check `supervisord` service is startup

```bash
$ ps aux | grep supervisord
```

### 3.2. Shutdown supervisor

```bash
$ supervisorctl shutdown
```

### 3.3. Manage process

#### 3.3.1. by commands

```bash
# Show status of all processes
$ supervisorctl status

# Start, stop and restart process
$ supervisorctl start show-time
$ supervisorctl stop show-time
$ supervisorctl restart show-time

# Start, stop and restart process group
$ supervisorctl start work-group
$ supervisorctl stop work-group
$ supervisorctl restart work-group

# Start, stop and restart all process
$ supervisorctl start all
$ supervisorctl stop all
$ supervisorctl restart all

# Reload process
$ supervisorctl reload

# Refresh process with config file
$ supervisorctl update
# Note: Reload or update will not automatically restart the process which When stopped with stop
```

#### 3.3.2. Interactive

Run `supervisorctl` without any arguments, enter the interactive mode：

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