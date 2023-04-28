# 使用 Docker 容器

- [使用 Docker 容器](#使用-docker-容器)
  - [1. 几个问题](#1-几个问题)
    - [问题 1](#问题-1)
    - [问题 2](#问题-2)
  - [2. 单实例模式](#2-单实例模式)
    - [2.1. 通过环境变量定义管理员密码](#21-通过环境变量定义管理员密码)
    - [2.2. 映射配置文件](#22-映射配置文件)
    - [2.3. 创建健康检查用户](#23-创建健康检查用户)
  - [3. 主从模式](#3-主从模式)
    - [3.1. 数据库配置](#31-数据库配置)
    - [3.2. 令 Slave 连接到 Master](#32-令-slave-连接到-master)
      - [3.2.1. 在 Master 上进行如下操作](#321-在-master-上进行如下操作)
      - [3.2.2. 在 Slave 上进行如下操作](#322-在-slave-上进行如下操作)
      - [3.2.3. 结果验证](#323-结果验证)
        - [3.2.3.1. 在 Master 上执行](#3231-在-master-上执行)
        - [3.2.3.2. 在 Slave 上执行](#3232-在-slave-上执行)
    - [3.3. 主从故障恢复](#33-主从故障恢复)
  - [4. 相关 Docker 命令](#4-相关-docker-命令)
    - [4.1. 删除卷文件](#41-删除卷文件)
      - [4.1.1. 删除主库卷文件](#411-删除主库卷文件)
      - [4.1.2. 删除从库卷文件](#412-删除从库卷文件)
      - [4.1.3. 删除所有卷文件](#413-删除所有卷文件)
    - [4.2. 访问数据库](#42-访问数据库)
      - [4.2.1. 访问主库](#421-访问主库)
      - [4.2.2. 访问从库](#422-访问从库)

## 1. 几个问题

### 问题 1

`percona/percona-server` 镜像创建的容器中 `/var/log/mysql` 目录定义的用户是 `root:root`, 但容器用户是 `mysql`, 导致容器启动后, 无法在 `/var/log/mysql` 中创建和写入文件

所以, 目前的解决方案是, 将容器的 `/var/log/mysql` 路径映射到宿主机指定路径上, 并将此路径权限设置为 `777`, 例如:

```bash
chmod 777 ./standalone/log
```

### 问题 2

在 MySQL 中创建用户所能访问主机时, 使用容器的 Hostname 无法生效, 需要使用 `<容器标示>.<网络名>`, 例如要创建一个只允许容器标示 `percona-slave` 访问的用户 `replica` 时, 需要这样设置

```sql
CREATE USER 'replica'@'percona-slave.cluster-ms_percona_network' IDENTIFIED WITH mysql_native_password BY 'replica';
```

`percona-slave` 为容器标识, `cluster-ms_percona_network` 为网络名称, 可以通过 `docker network ls` 命令查询到, 也可以通过进入另一个容器中, 通过 `ping` 命令探查 (例如: `ping percona-slave`)

## 2. 单实例模式

单实例模式的容器配置文件为 [standalone/docker-compose.yml](../docker/standalone/docker-compose.yml) 文件

### 2.1. 通过环境变量定义管理员密码

MySQL 的初始管理员密码可以通过容器的环境变量 `MYSQL_ROOT_PASSWORD` 来设置

可以通过 `MYSQL_DATABASE` 来设置登录 MySQL 后的默认数据库

可以通过 `MYSQL_USER` 和 `MYSQL_PASSWORD` 设置一个非 root 的新用户, 如果同时设置了 `MYSQL_DATABASE` 环境变量, 则会同时对这个数据库设置所有权限

可以在容器配置文件中, 通过 `environment` 属性设置容器环境变量, 如:

```yml
environment:
  - MYSQL_ROOT_PASSWORD=***
  - MYSQL_USER=user
  - MYSQL_PASSWORD=***
  - MYSQL_DATABASE=demo
```

也可以将环境变量保存在一个文件中 (例如: [env/percona.env](../docker/env/percona.env) 文件), 并在容器配置文件中, 通过 `env_file` 属性指定环境变量文件的路径, 如:

```yml
env_file:
  - ../env/percona.env
  ...
```

### 2.2. 映射配置文件

如果需要修改默认配置, 需要将自定义的配置文件 (例如 [standalone/conf/default.cnf](../docker/standalone/conf/default.cnf) 文件) 映射到容器的 `/etc/my.cnf.d` 路径下, 在容器配置文件中, 通过 `volumes` 属性指定文件映射

```yml
volumes:
  - ./conf/default.cnf:/etc/my.cnf.d/default.cnf:ro
```

### 2.3. 创建健康检查用户

```sql
CREATE USER 'health'@'localhost' IDENTIFIED WITH mysql_native_password BY '';
```

可以利用容器的 `/docker-entrypoint-initdb.d/` 路径执行初始化 SQL 脚本, 即将 [standalone/sql/initialize.sql](../docker/standalone/sql/initialize.sql) 文件映射到 `/docker-entrypoint-initdb.d/` 路径下, 通过容器配置文件的 `volumes` 属性配置如下:

```yml
volumes:
  - ./sql/initialize.sql:/docker-entrypoint-initdb.d/initialize.sql:ro
  ...
```

之后就可以进行健康检查, 即通过容器配置文件的 `healthcheck` 属性配置如下:

```yml
healthcheck:
  test:
    [
      "CMD-SHELL",
      "mysqladmin ping -uhealth | grep -q 'alive'"
    ]
  interval: 1m
  timeout: 30s
  retries: 5
  start_period: 30s
```

## 3. 主从模式

主从模式由一个 Master 实例和多个 Slave 实例组成, Slave 通过 Master 的 `bin-log` 读取数据增量变化, 将变化进行同步, 从而达到"主从复制"的目的

所有所有的 `DDL` (数据定义语句) 和 `DML` (数据操作语句) 必须在 Master 上执行, `DQL` (数据查询语句) 可以在 Master 和 Slave 上, 从而实现"读写分离", 至于 `DCL` (数据控制语句) 则根据需要, 在 Master 和 Slave 上分别执行

### 3.1. 数据库配置

主从模式应包含单例模式下容器的所有配置, 包括:

- 主从模式使用的容器配置文件 [cluster-ms/docker-compose.yml](../docker/cluster-ms/docker-compose.yml), 包括 Master 节点 (`percona-master`) 和 Slave 节点 (`percona-slave`);
- Master 节点将 [cluster-ms/conf/master.cnf](../docker/cluster-ms/conf/master.cnf) 文件作为数据库配置, 必须映射到容器的 `/docker-entrypoint-initdb.d` 路径下;
- Master 节点将 [cluster-ms/sql/master_setup.sql](../docker/cluster-ms/sql/master_setup.sql) 文件作为初始化 SQL 脚本, 用于创建健康检查和**主从同步**两个账号和权限, 必须映射到容器的 `/docker-entrypoint-initdb.d` 路径下;
- Slave 节点将 [cluster-ms/conf/slave.cnf](../docker/cluster-ms/conf/slave.cnf) 文件作为数据库配置, 映射到各自容器的 `/etc/mysql.cnf.d` 路径下;
- Slave 节点将 [cluster-ms/sql/slave_setup.sql](../docker/cluster-ms/sql/slave_setup.sql) 文件作为初始化 SQL 脚本, 用于创建健康检查账号和权限;
- Master 和 Slave 节点均将日志写入 `cluster-ms/log` 路径下, 通过各自配置文件设定日志文件的名称以加以区分;

### 3.2. 令 Slave 连接到 Master

#### 3.2.1. 在 Master 上进行如下操作

修改 Master 配置文件 ([cluster-ms/conf/master.cnf](../docker/cluster-ms/conf/master.cnf)), 增加如下部分:

```ini
server-id = 1 # 设置节点 id, 不能重复
log-bin = mysql-bin   # 开启 binlog
binlog_format = ROW   # binlog 格式

gtid_mode = on        # 为 binlog 启动 gtid
enforce_gtid_consistency = on

log_replica_updates = 1 # 从库 binlog 才会记录主库同步的操作日志
skip_replica_start = 1  # 跳过 slave 复制线程

relay-log = relay-bin

sync_binlog = 1000  # 设置同步频率
innodb_flush_log_at_trx_commit = 2  # 设置刷新时机
```

重启服务, 查看 `bin-log` 同步进度

```sql
SHOW MASTER STATUS\G

*************************** 1. row ***************************
             File: mysql-bin.000003
         Position: 196
     Binlog_Do_DB:
 Binlog_Ignore_DB:
Executed_Gtid_Set: e5176efe-e2b1-11ed-92d7-0242c0a89002:1-4
```

输出的 `File: mysql-bin.000003` 和 `Position: 196` 即 Master 上 `bin-log` 的当前位置

#### 3.2.2. 在 Slave 上进行如下操作

修改 Slave 配置文件 ([cluster-ms/conf/slave.cnf](../docker/cluster-ms/conf/slave.cnf)), 增加如下部分:

```ini
server-id = 2 # 设置节点 id, 不能重复

gtid_mode = on # 为 binlog 启动 gtid
enforce_gtid_consistency = on

read_only = on # 启动只读模式

sync_binlog = 1000  # 设置同步频率
innodb_flush_log_at_trx_commit = 2  # 设置刷新时机
```

重启服务, 设置 Slave 连接到 Master 上, 需要注意, `MASTER_LOG_FILE`, `MASTER_LOG_POS` 两个属性值需要根据上一步返回的结果进行调整

```sql
STOP SLAVE;

CHANGE MASTER TO
    MASTER_HOST='percona_master',
    MASTER_USER='replica',
    MASTER_PASSWORD='replica',
    MASTER_LOG_FILE='mysql-bin.000003',
    MASTER_LOG_POS=196;

START SLAVE;
```

注意, `replica` 这个用户是在 Master 节点通过初始化 SQL 脚本 [cluster-ms/sql/master_setup.sql](../docker/cluster-ms/sql/master_setup.sql) 生成的, 该脚本必须映射到容器的 `/docker-entrypoint-initdb.d` 路径下, 如果无法进行同步, 请检查这一步是否正确

查看设置结果, 若类似如下结果, 则表示 Slave 已经和 Master 建立关系

```sql
SHOW SLAVE STATUS\G

*************************** 1. row ***************************
               Slave_IO_State: Waiting for source to send event
                  Master_Host: percona_master
                  Master_User: replica
                  Master_Port: 3306
                Connect_Retry: 60
              Master_Log_File: mysql-bin.000003
          Read_Master_Log_Pos: 196
...
```

#### 3.2.3. 结果验证

可以执行一些 SQL 验证一下结果

##### 3.2.3.1. 在 Master 上执行

```sql
CREATE DATABASE IF NOT EXISTS `replicate_test`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE replicate_test;

CREATE TABLE IF NOT EXISTS `test` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `value` VARCHAR(200) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARSET = utf8mb4;

INSERT INTO `test` (`value`) VALUES ('test-value');
```

##### 3.2.3.2. 在 Slave 上执行

```sql
USE `replicate_test`;

SELECT * FROM `test`;
```

如果可以返回 Master 上插入的结果, 表示主从复制已经成功

### 3.3. 主从故障恢复

排除故障后, 在 Slave 上执行如下语句:

```sql
START SLAVE;
SHOW SLAVE STATUS\G;
```

如果上述语句执行失败, 则需要重置同步

```sql
STOP SLAVE;
RESET SLAVE;
START SLAVE;
```

## 4. 相关 Docker 命令

### 4.1. 删除卷文件

#### 4.1.1. 删除主库卷文件

```bash
docker volume rm -f cluster-ms_percona_master_data
```

#### 4.1.2. 删除从库卷文件

```bash
docker volume rm -f cluster-ms_percona_slave_data
```

#### 4.1.3. 删除所有卷文件

```bash
docker volume rm -f $(docker volume ls -q | grep cluster-ms)
```

### 4.2. 访问数据库

#### 4.2.1. 访问主库

```bash
docker exec -it percona-master mysql -u root -p
```

#### 4.2.2. 访问从库

```bash
docker exec -it percona-slave mysql -u root -p
```
