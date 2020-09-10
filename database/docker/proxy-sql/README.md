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

#### 查看 Slave 状态

```sql
mysql> show slave status\G;
*************************** 1. row ***************************
               Slave_IO_State: Waiting for master to send event
                  Master_Host: percona-master
                  Master_User: repl
                  Master_Port: 3306
                Connect_Retry: 60
              Master_Log_File: mysql-bin.000003
          Read_Master_Log_Pos: 1201
               Relay_Log_File: relay-bin.000005
                Relay_Log_Pos: 1414
        Relay_Master_Log_File: mysql-bin.000003
             Slave_IO_Running: Yes
            Slave_SQL_Running: Yes
              Replicate_Do_DB: mysql_proxy
          Replicate_Ignore_DB:
           Replicate_Do_Table:
       Replicate_Ignore_Table:
      Replicate_Wild_Do_Table:
  Replicate_Wild_Ignore_Table:
                   Last_Errno: 0
                   Last_Error:
                 Skip_Counter: 0
          Exec_Master_Log_Pos: 1201
              Relay_Log_Space: 1781
              Until_Condition: None
               Until_Log_File:
                Until_Log_Pos: 0
           Master_SSL_Allowed: No
           Master_SSL_CA_File:
           Master_SSL_CA_Path:
              Master_SSL_Cert:
            Master_SSL_Cipher:
               Master_SSL_Key:
        Seconds_Behind_Master: 0
Master_SSL_Verify_Server_Cert: No
                Last_IO_Errno: 0
                Last_IO_Error:
               Last_SQL_Errno: 0
               Last_SQL_Error:
  Replicate_Ignore_Server_Ids:
             Master_Server_Id: 1
                  Master_UUID: 98c5182f-ed96-11ea-a3d8-0242c0a89002
             Master_Info_File: /var/lib/mysql/master.info
                    SQL_Delay: 0
          SQL_Remaining_Delay: NULL
      Slave_SQL_Running_State: Slave has read all relay log; waiting for more updates
           Master_Retry_Count: 86400
                  Master_Bind:
      Last_IO_Error_Timestamp:
     Last_SQL_Error_Timestamp:
               Master_SSL_Crl:
           Master_SSL_Crlpath:
           Retrieved_Gtid_Set:
            Executed_Gtid_Set:
                Auto_Position: 0
         Replicate_Rewrite_DB:
                 Channel_Name:
           Master_TLS_Version:
1 row in set (0.00 sec)
```
