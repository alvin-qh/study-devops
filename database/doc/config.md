# MySQL 配置

## 1. 配置文件位置

- `~/.my.ini` or `~/.my.cnf'
- `/etc/my.ini` or `/etc/my.cnf`
- `/etc/mysql/conf.d/my.cnf` (percona server)

## 2. my.ini 配置文件

MySQL 的配置文件为 `my.ini` 或者 `my.cnf`.

### 2.1. 字符集设置

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

### 2.2. 日志配置

```ini
[mysqld]
general_log = ON
general_log_file = /var/log/mysql/mysqld.log   # 通用查询日志

slow_query_log = OFF
slow_query_log_file = /var/log/mysql/slow_query.log # 慢查询日志
long_query_time = 10.000000
```

### 2.3. 网络配置

```ini
[mysqld]
bind-address = 127.0.0.1
port = 3306
socket = /tmp/mysql.sock

connect_timeout = 10
net_write_timeout = 28800
net_read_timeout = 28800
interactive_timeout = 100
wait_timeout = 100

max_allowed_packet = 16M
```

合适的最大连接数设置遵循如下公式：

```plain
(最大使用连接 / 最大连接数) * 100% >= 0.1%
```

其中：

获取最大连接

```sql
mysql> SHOW VARIABLES LIKE 'max_connections';

+-----------------+-------+
| Variable_name   | Value |
+-----------------+-------+
| max_connections | 151   |
+-----------------+-------+
1 row in set (0.00 sec)
```

获取正在使用的连接数

```sql
mysql> SHOW GLOBAL STATUS LIKE 'max_used_connections';

+----------------------+-------+
| Variable_name        | Value |
+----------------------+-------+
| Max_used_connections | 1     |
+----------------------+-------+
1 row in set (0.00 sec)
```

临时设置最大连接数

```sql
mysql> SET GLOBAL max_connections = 1024;
```

通过配置文件设置最大连接数

```ini
[mysqld]
max_connections = 1024
```

### 2.4. 内存配置

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

### 2.5. 认证插件配置

```ini
[mysqld]
default_authentication_plugin = mysql_native_password
```

### 2.6. SQL 模式配置

```ini
[mysqld]
sql_mode = STRICT_TRANS_TABLES,ONLY_FULL_GROUP_BY,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION
```

### 2.7. 线程池配置

- 获取线程池大小

```sql
mysql> SHOW VARIABLES LIKE 'thread_pool_size';
```

- 设置线程池配置

```ini
[mysqld]
skip-locking
thread_handling = one-thread-per-connection | pool-of-threads
thread_pool_size = 100
thread_pool_stall_limit = 20      # 200ms
thread_cache_size = 120           # 60 by default
thread_concurrency = 8            # as total cpu count
```

### 2.8. 其它配置

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
