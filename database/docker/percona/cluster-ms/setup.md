# 数据库初始化

## 1. Master

```bash
docker exec -it percona-master mysql -u root -p -e "
    SHOW VARIABLES LIKE 'server_id';
    CREATE USER 'replica'@'percona-slave.cluster-ms_percona_network' IDENTIFIED BY 'replica~123';
    GRANT REPLICATION SLAVE ON *.* TO 'replica'@'percona-slave.cluster-ms_percona_network';
    SHOW MASTER STATUS\G;
"
```

## 2. Slave

```bash
docker exec -it percona-slave mysql -u root -p -e "
    SHOW VARIABLES LIKE 'server_id';
    STOP SLAVE;
    CHANGE MASTER TO
    MASTER_HOST='percona-master.cluster-ms_percona_network',
    MASTER_USER='replica',
    MASTER_PASSWORD='replica~123',
    MASTER_LOG_FILE='mysql-bin.000003',
    MASTER_LOG_POS=745;
    START SLAVE;
    SHOW SLAVE STATUS\G;
"
```

## 3. 测试

### 3.1. Master

```bash
docker exec -it percona-master mysql -u root -p -e "
    CREATE DATABASE IF NOT EXISTS ``replicate_test``
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
    USE replicate_test;
    CREATE TABLE IF NOT EXISTS ``test`` (
      ``id`` BIGINT NOT NULL AUTO_INCREMENT,
      ``value`` VARCHAR(200) NOT NULL,
      PRIMARY KEY (``id``)
    ) ENGINE = InnoDB CHARSET = utf8mb4;
    INSERT INTO ``test`` (``value``) VALUES ('test-value');
"
```

### 3.2. Slave

```bash
docker exec -it percona-slave mysql -u root -p -e "
    USE ``replicate_test``;
    SELECT * FROM ``test``;
"
```

## 4. 断连后恢复

```sh
docker exec -it percona-slave mysql -u root -p
```

重启从节点

```sql
START SLAVE;
```

查看同步状态

```sql
SHOW SLAVE STATUS \G;
*************************** 1. row ***************************
               Slave_IO_State: Waiting for source to send event
                  Master_Host: percona-master.cluster-ms_percona_network
                  Master_User: replica
                  Master_Port: 3306
                Connect_Retry: 60
              Master_Log_File: mysql-bin.000005
          Read_Master_Log_Pos: 196
               Relay_Log_File: 11469037e9b7-relay-bin.000007
                Relay_Log_Pos: 411
        Relay_Master_Log_File: mysql-bin.000005
             Slave_IO_Running: Yes
            Slave_SQL_Running: Yes
              Replicate_Do_DB:
          Replicate_Ignore_DB:
           Replicate_Do_Table:
       Replicate_Ignore_Table:
      Replicate_Wild_Do_Table:
  Replicate_Wild_Ignore_Table:
                   Last_Errno: 0
                   Last_Error:
                 Skip_Counter: 0
          Exec_Master_Log_Pos: 196
              Relay_Log_Space: 2075
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
                  Master_UUID: e6ba8bbc-731e-11ed-a74a-0242c0a8a003
             Master_Info_File: mysql.slave_master_info
                    SQL_Delay: 0
          SQL_Remaining_Delay: NULL
      Slave_SQL_Running_State: Replica has read all relay log; waiting for more updates
           Master_Retry_Count: 86400
                  Master_Bind:
      Last_IO_Error_Timestamp:
     Last_SQL_Error_Timestamp:
               Master_SSL_Crl:
           Master_SSL_Crlpath:
           Retrieved_Gtid_Set: e6ba8bbc-731e-11ed-a74a-0242c0a8a003:3-13
            Executed_Gtid_Set: e6ba8bbc-731e-11ed-a74a-0242c0a8a003:3-13
                Auto_Position: 0
         Replicate_Rewrite_DB:
                 Channel_Name:
           Master_TLS_Version:
       Master_public_key_path:
        Get_master_public_key: 0
            Network_Namespace:
1 row in set, 0 warning (0.01 sec)
```

如果重启失败, 可以尝试重置从节点

```sql
STOP SLAVE;
RESET SLAVE;
START SLAVE;
```
