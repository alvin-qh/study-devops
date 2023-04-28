# 安装和配置 Percona

- [安装和配置 Percona](#安装和配置-percona)
  - [1. 安装 Percona](#1-安装-percona)
    - [1.1. Debian/Ubuntu](#11-debianubuntu)
    - [1.2. RHEL/CentOS](#12-rhelcentos)
  - [2. 配置](#2-配置)
    - [2.1. 配置文件位置](#21-配置文件位置)
    - [2.2. 字符集设置](#22-字符集设置)
    - [2.3. 日志配置](#23-日志配置)
    - [2.4. 网络配置](#24-网络配置)
    - [2.5. 内存配置](#25-内存配置)
    - [2.6. 认证插件配置](#26-认证插件配置)
    - [2.7. SQL 模式配置](#27-sql-模式配置)
    - [2.8. 线程池配置](#28-线程池配置)
    - [2.9. 其它配置](#29-其它配置)

## 1. 安装 Percona

> 参见[官方文档](https://docs.percona.com/percona-server/8.0/installation.html)查看最新的安装方法

### 1.1. Debian/Ubuntu

1. 下载 Percona 安装器 (`percona-release`) 并安装

    ```bash
    curl -O https://repo.percona.com/apt/percona-release_latest.generic_all.deb
    sudo apt install gnupg2 lsb-release ./percona-release_latest.generic_all.deb
    ```

2. 通过安装器 (`percona-release`) 安装数据库

    ```bash
    sudo percona-release setup ps80
    sudo apt install percona-server-server
    ```

### 1.2. RHEL/CentOS

1. 下载 Percona 安装器 (`percona-release`) 并安装

    ```bash
    sudo yum install https://repo.percona.com/yum/percona-release-latest.noarch.rpm
    ```

2. 通过安装器 (`percona-release`) 安装数据库

    ```bash
    sudo percona-release setup ps-80
    sudo yum install percona-server-server
    ```

## 2. 配置

### 2.1. 配置文件位置

Percona 的配置文件会按优先级自上而下从如下几个位置加载

- `~/.my.cnf`
- `/etc/my.cnf`
- `/etc/mysql/conf.d/*.cnf`

### 2.2. 字符集设置

```ini
[client]
default-character-set = utf8mb4

[mysql]
default-character-set = utf8mb4

[mysqld]
character-set-client-handshake = FALSE
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
init_connect = 'SET NAMES utf8mb4'
```

### 2.3. 日志配置

```ini
[mysqld]
general_log = ON
general_log_file = /var/log/mysql/mysqld.log   # 通用查询日志

slow_query_log = OFF
slow_query_log_file = /var/log/mysql/slow_query.log # 慢查询日志
long_query_time = 10.000000
```

### 2.4. 网络配置

```ini
[mysqld]
bind-address = 0.0.0.0
port = 3306
socket = /tmp/mysql.sock

connect_timeout = 10
net_write_timeout = 28800
net_read_timeout = 28800
interactive_timeout = 100
wait_timeout = 100

max_allowed_packet = 16M
```

其中:

- 合适的最大连接数设置遵循如下公式:

  ```plain
  (最大使用连接 / 最大连接数) * 100% >= 0.1%`
  ```

- 获取最大连接:

  ```sql
  SHOW VARIABLES LIKE 'max_connections';

  +-----------------+-------+
  | Variable_name   | Value |
  +-----------------+-------+
  | max_connections | 151   |
  +-----------------+-------+
  1 row in set (0.00 sec)
  ```

- 获取正在使用的连接数:

  ```sql
  SHOW GLOBAL STATUS LIKE 'max_used_connections';

  +----------------------+-------+
  | Variable_name        | Value |
  +----------------------+-------+
  | Max_used_connections | 1     |
  +----------------------+-------+
  1 row in set (0.00 sec)
  ```

- 临时修改最大连接数:

  ```sql
  SET GLOBAL max_connections = 1024;
  ```

- 永久设置最大连接数:

    ```ini
    [mysqld]
    max_connections = 1024
    ```

### 2.5. 内存配置

```ini
[mysqld]
table_cache = 1024                    # 512 ~ 1024
query_cache_size = 64M
query_cache_type = 1
query_cache_limit = 1M

innodb_additional_mem_pool_size = 4M  # 2M by default
innodb_flush_log_at_trx_commit = 0
innodb_log_buffer_size = 2M           # 1M by default
tmp_table_size = 64M                  # 16M by default, 64 ~ 256 is OK

key_buffer = 256M                     # 218M by default
join_buffer = 16M                     # 1M by default
record_buffer = 1M

read_buffer_size = 4M                 # 64K by default
sort_buffer_size = 32M                # 256K by default
myisam_sort_buffer_size = 64M
read_rnd_buffer_size = 16M            # 256K by default
```

### 2.6. 认证插件配置

```ini
[mysqld]
default_authentication_plugin = mysql_native_password
```

### 2.7. SQL 模式配置

```ini
[mysqld]
sql_mode = STRICT_TRANS_TABLES,ONLY_FULL_GROUP_BY,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION
```

### 2.8. 线程池配置

- 获取线程池大小

  ```sql
  SHOW VARIABLES LIKE 'thread_pool_size';
  ```

- 设置线程池配置

  ```ini
  [mysqld]
  skip-locking
  thread_handling = one-thread-per-connection (or pool-of-threads)
  thread_pool_size = 100
  thread_pool_stall_limit = 20      # 200ms
  thread_cache_size = 120           # 60 by default
  thread_concurrency = 8            # as total cpu count
  ```

### 2.9. 其它配置

```ini
[safe_mysqld]
err-log = /var/log/mysqld.log
open_files_limit = 8192

[mysqldump]
quick
max_allowed_packet = 16M

[mysql]
no-auto-rehash
#safe-updates

[isamchk]
key_buffer = 64M
sort_buffer = 64M
read_buffer = 16M
write_buffer = 16M

[myisamchk]
key_buffer = 64M
sort_buffer = 64M
read_buffer = 16M
write_buffer = 16M

[mysqlhotcopy]
interactive-timeout
```
