# Master-Slave Replication

## 结构

- `docker-compose.yml`：docker-compose 配置文件
- `master.cnf`：主数据库配置文件
- `log-master`：主数据库日志文件
- `slave.cnf`：从数据库配置文件
- `log-slave`：从数据库日志文件

## 设置

### Master 数据库

#### 创建同步数据库

```sql
mysql> CREATE DATABASE `mysql_proxy` DEFAULT CHARACTER SET utf8mb4;
```

> 创建的库（`mysql_proxy`）应该和`master.cnf`文件中的`binlog-do-db`配置相匹配

#### 创建同步账号

```sql
mysql> CREATE USER 'repl'@'%' IDENTIFIED BY '123456';   # 创建用户
mysql> GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';    # 授权该用户可复制给 Slave
mysql> flush privileges;
```

#### 查看 Master 状态

```sql
mysql> show master status;
+------------------+----------+--------------+--------------------------+-------------------+
| File             | Position | Binlog_Do_DB | Binlog_Ignore_DB         | Executed_Gtid_Set |
+------------------+----------+--------------+--------------------------+-------------------+
| mysql-bin.000002 |      155 | test         | mysql,information_schema |                   |
+------------------+----------+--------------+--------------------------+-------------------+
```

### Slave 数据库

#### 创建同步数据库

```sql
mysql> CREATE DATABASE `mysql_proxy` DEFAULT CHARACTER SET utf8mb4;
```

> 创建的库（`mysql_proxy`）应该和`slave.cnf`文件中的`replicate-do-db`配置相匹配

####  启动、停止和重置 Slave

- 配置 Slave

```sql
mysql> change master to
    -> master_host='percona-master',
    -> master_port=3306,
    -> master_user='repl',
    -> master_password='123456',
    -> master_log_file='mysql-bin.000002',  # 这里的 master_log_file 和 master_log_pos 就是配置主数据库查询到的 File 和 Position
    -> master_log_pos=155;
```

- 启动 Slave

```sql
mysql> start slave;
```

- 停止 Slave

```sql
mysql> stop slave;
```

- 重置 Slave

```sql
mysql> reset slave;
```
