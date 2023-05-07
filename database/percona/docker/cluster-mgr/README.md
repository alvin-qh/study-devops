# MySQL Group Replication

- [MySQL Group Replication](#mysql-group-replication)
  - [1. 介绍](#1-介绍)
  - [2. 安装和配置](#2-安装和配置)
    - [2.1. 集群配置](#21-集群配置)
      - [2.1.1. 安装数据库节点](#211-安装数据库节点)
      - [2.1.2. 配置单主模式](#212-配置单主模式)
      - [2.1.3. 配置多主模式](#213-配置多主模式)
      - [2.1.4. 模式切换](#214-模式切换)
        - [2.1.4.1. 单主模式切换为多主模式](#2141-单主模式切换为多主模式)
        - [2.1.4.2. 多主模式切换为单主模式](#2142-多主模式切换为单主模式)
  - [3. 使用容器](#3-使用容器)
    - [3.1. 配置说明](#31-配置说明)
    - [3.2. 节点配置](#32-节点配置)
    - [3.2. 重启集群](#32-重启集群)
  - [4. 故障恢复](#4-故障恢复)
  - [5. 加入负载均衡](#5-加入负载均衡)

## 1. 介绍

类似 Percona XtraDB Cluster 集群, MySQL Group Replication (简称 MGR) 是 MySQL 原生的高可用性, 高一致性集群, 具备如下特点:

1. 高一致性: 基于原生复制和 Paxos 协议的组复制技术;
2. 高容错性: 有自动检测机制, 当出现宕机后, 会自动剔除问题节点, 在 $2N + 1$ 个节点集群中, 集群 只要 $N + 1$ 个节点还存活着, 数据库就能稳定的对外提供服务;
3. 高扩展性: 可以在线增加和移除节点;
4. 高灵活性: 可以在单主模式和多主模式自由切换;

注意, 和 PXC 集群类似, MGR 集群也有如下要求:

1. 只能使用 `InnoDB` 数据库引擎, 不支持其它不具备事务的数据库引擎;
2. 不支持 XA 事务 (分布式事务), 因为提交随时可能被回滚;
3. 集群最少需要 3 个节点;
4. 集群模式下, 要避免在集群模式下使用 `ALTER TABLE ... IMPORT/EXPORT` 负载, 如果未在所有节点上执行, 会导致节点不一致;
5. 所有的表必须都有一个主键, 这样可以保证每个节点上相同的行具有相同的顺序;

## 2. 安装和配置

### 2.1. 集群配置

#### 2.1.1. 安装数据库节点

MGR 集群最少需要 `3` 个节点, 即需要启动 `3` 个 MySQL 实例, 并记录每个实例的 IP 地址 (或主机名)

整个配置过程如下:

1. 参考 [单实例](../standalone/README.md) 配置方法配置每个节点;
2. 参考 [主从模式](../cluster-ms/README.md) 集群配置方法, 主要包括:
   - 设置服务 ID (`server-id`);
   - 启用 GTID 模式 (`gtid_mode`);
   - 无需设置任何只读配置 (`read_only` 和 `super_read_only`);

#### 2.1.2. 配置单主模式

单主模式即集群中只有一个主节点, 只有该节点可以进行写操作, 其它节点只能进行读操作, 相当于一个高一致性的"主从"模式, 如果主节点失效, 则可以在集群中其余的从节点中, 任意选择一个提升为主节点

1. 连接各个数据库实例, 执行如下 SQL 命令

    安装 `group_replication` 插件

    ```sql
    INSTALL PLUGIN group_replication SONAME 'group_replication.so';
    ```

    执行如下命令创建同步用户

    ```sql
    SET sql_log_bin = 0;

    CREATE USER 'repl'@'%' IDENTIFIED WITH mysql_native_password BY 'repl';
    GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'repl'@'%';

    FLUSH PRIVILEGES;

    SET sql_log_bin = 1;
    ```

    注意, 要通过 `sql_log_bin` 变量, 保证创建用户的 SQL 不会记录到 Binlog 中 (因为每个节点都要创建整个用户, 同步这部分语句会导致数据冲突)

2. 修改各个节点的配置文件

    ```ini
    loose-group_replication_group_name = '38f34157-cbe8-4623-a7bd-054cc5c2de0b'
    loose-group_replication_start_on_boot = OFF
    loose-group_replication_local_address = '<当前节点 IP:10061>'
    loose-group_replication_group_seeds = '<当前节点 IP:10061>,<其它节点 IP:10061>,...'
    loose-group_replication_bootstrap_group = OFF
    loose-group_replication_ip_whitelist = '<当前节点 IP/24>,<其它节点 IP/24>,...'
    ```

    注意, 一定要先安装插件才能修改配置, 否则这些配置项会因为没有对应插件导致节点启动失败

    `loose-group_replication_group_name` 配置项是一个 UUID, 注意格式必须符合 UUID 的要求

3. 启动主节点

    连接主节点 (集群中任选一个即可), 执行如下指令

    开启主节点模式开关, 开启集群初始化开关

    ```sql
    SET GLOBAL group_replication_single_primary_mode = ON;
    SET GLOBAL group_replication_bootstrap_group = ON;
    ```

    `group_replication_single_primary_mode` 变量表示集群为"单主模式", 该变量的默认值为 `ON`, 所以也可以不设置该变量, 使用默认值即可

    设置 Master 连接, 初始化集群

    ```sql
    CHANGE MASTER TO MASTER_USER='repl', MASTER_PASSWORD='repl' FOR CHANNEL 'group_replication_recovery';
    START GROUP_REPLICATION;
    ```

    稍等片刻, 返回成功表示主节点已启动

    关闭集群初始化开关, 这样故障转移后, 可以在集群中重新设置主节点

    ```sql
    SET GLOBAL group_replication_bootstrap_group = OFF;
    ```

    可以查看节点状态

    ```sql
    SELECT * FROM `performance_schema`.`replication_group_members`;

    +---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
    | CHANNEL_NAME              | MEMBER_ID                            | MEMBER_HOST | MEMBER_PORT | MEMBER_STATE | MEMBER_ROLE | MEMBER_VERSION |
    +---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
    | group_replication_applier | c1a60f79-f719-11ea-97db-000c29cc2388 | mysql       |        3306 | ONLINE       | PRIMARY     | 8.0.21         |
    +---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
    ```

    `MEMBER_ID` 是集群分配给当前节点的 ID, 是一个 UUID

4. 启动其它节点

    逐一在其它节点执行如下 SQL 指令

    ```sql
    CHANGE MASTER TO MASTER_USER='repl', MASTER_PASSWORD='repl' FOR CHANNEL 'group_replication_recovery';
    START GROUP_REPLICATION;
    ```

    稍等片刻, 返回成功后节点即加入集群, 通过如下语句在任意节点可以查看集群状态

    ```sql
    SELECT * FROM `performance_schema`.`replication_group_members`;

    +---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
    | CHANNEL_NAME              | MEMBER_ID                            | MEMBER_HOST | MEMBER_PORT | MEMBER_STATE | MEMBER_ROLE | MEMBER_VERSION |
    +---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
    | group_replication_applier | 188f118c-f71b-11ea-899b-000c29387845 | mysql       |        3306 | ONLINE       | SECONDARY   | 8.0.21         |
    | group_replication_applier | 577b5cdb-f71a-11ea-9f03-000c29231183 | mysql       |        3306 | ONLINE       | SECONDARY   | 8.0.21         |
    | group_replication_applier | c1a60f79-f719-11ea-97db-000c29cc2388 | mysql       |        3306 | ONLINE       | PRIMARY     | 8.0.21         |
    +---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
    ```

#### 2.1.3. 配置多主模式

多主模式即集群中每个节点都可以为主节点, 所有节点均可读写, 类似于 Percona XtraDB Cluster 集群, 任意节点故障, 整个集群不会受到影响

整个配置过程和 [配置单主模式](#212-配置单主模式) 基本一致, 只需将 `group_replication_single_primary_mode` 变量关闭, 并打开 `group_replication_enforce_update_everywhere_checks` 变量即可, 即在各个节点执行:

```sql
SET GLOBAL group_replication_single_primary_mode = OFF;
SET GLOBAL group_replication_enforce_update_everywhere_checks = ON;
```

`group_replication_enforce_update_everywhere_checks=ON` 变量表示集群为"多主模式"

在任意节点上执行如下 SQL 指令, 设置 Master 连接, 初始化集群

设置完毕后, 在任意节点执行如下查询, 查看集群状态

```sql
SELECT * FROM `performance_schema`.`replication_group_members`;

+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
| CHANNEL_NAME              | MEMBER_ID                            | MEMBER_HOST | MEMBER_PORT | MEMBER_STATE | MEMBER_ROLE | MEMBER_VERSION |
+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
| group_replication_applier | 188f118c-f71b-11ea-899b-000c29387845 | mysql       |        3306 | ONLINE       | PRIMARY     | 8.0.21         |
| group_replication_applier | 577b5cdb-f71a-11ea-9f03-000c29231183 | mysql       |        3306 | ONLINE       | PRIMARY     | 8.0.21         |
| group_replication_applier | c1a60f79-f719-11ea-97db-000c29cc2388 | mysql       |        3306 | ONLINE       | PRIMARY     | 8.0.21         |
+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
```

#### 2.1.4. 模式切换

##### 2.1.4.1. 单主模式切换为多主模式

在所有节点上执行如下 SQL 指令

```sql
STOP GROUP_REPLICATION;

SET GLOBAL group_replication_single_primary_mode = OFF;
SET GLOBAL group_replication_enforce_update_everywhere_checks = ON;
```

即先解散集群, 把各个节点切换为"多主模式"

任意找一个节点, 执行如下 SQL 指令, 启动第一个主节点, 初始化集群

```sql
SET GLOBAL group_replication_bootstrap_group = ON;
START GROUP_REPLICATION;

SET GLOBAL group_replication_bootstrap_group = OFF;
```

注意, 集群初始化完毕后, 仍需通过 `group_replication_bootstrap_group` 变量关闭集群初始化开关

在其它节点执行如下 SQL 指令

```sql
START GROUP_REPLICATION;
```

在任意节点执行如下查询, 查看集群状态

```sql
SELECT * FROM `performance_schema`.`replication_group_members`;

+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
| CHANNEL_NAME              | MEMBER_ID                            | MEMBER_HOST | MEMBER_PORT | MEMBER_STATE | MEMBER_ROLE | MEMBER_VERSION |
+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
| group_replication_applier | 188f118c-f71b-11ea-899b-000c29387845 | mysql       |        3306 | ONLINE       | PRIMARY     | 8.0.21         |
| group_replication_applier | 577b5cdb-f71a-11ea-9f03-000c29231183 | mysql       |        3306 | ONLINE       | PRIMARY     | 8.0.21         |
| group_replication_applier | c1a60f79-f719-11ea-97db-000c29cc2388 | mysql       |        3306 | ONLINE       | PRIMARY     | 8.0.21         |
+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
```

##### 2.1.4.2. 多主模式切换为单主模式

在所有节点上执行如下 SQL 指令

```sql
STOP GROUP_REPLICATION;

SET GLOBAL group_replication_enforce_update_everywhere_checks = OFF;
SET GLOBAL group_replication_single_primary_mode = ON;
```

即先解散集群, 关闭各节点的"多主模式"

任意找一个节点, 执行如下 SQL 指令, 将这个节点设置为主节点

```sql
SET GLOBAL group_replication_bootstrap_group = ON;
START GROUP_REPLICATION;

SET GLOBAL group_replication_bootstrap_group = OFF;
```

注意, 集群初始化完毕后, 仍需通过 `group_replication_bootstrap_group` 变量关闭集群初始化开关

在其它节点执行如下 SQL 指令

```sql
START GROUP_REPLICATION;
```

在任意节点执行如下查询, 查看集群状态

```sql
SELECT * FROM `performance_schema`.`replication_group_members`;

+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
| CHANNEL_NAME              | MEMBER_ID                            | MEMBER_HOST | MEMBER_PORT | MEMBER_STATE | MEMBER_ROLE | MEMBER_VERSION |
+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
| group_replication_applier | 188f118c-f71b-11ea-899b-000c29387845 | mysql       |        3306 | ONLINE       | SECONDARY   | 8.0.21         |
| group_replication_applier | 577b5cdb-f71a-11ea-9f03-000c29231183 | mysql       |        3306 | ONLINE       | SECONDARY   | 8.0.21         |
| group_replication_applier | c1a60f79-f719-11ea-97db-000c29cc2388 | mysql       |        3306 | ONLINE       | PRIMARY     | 8.0.21         |
+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
```

## 3. 使用容器

### 3.1. 配置说明

官方的 Percona (或其它 MySQL) 镜像中并未安装 `group_replication` 插件, 需先安装插件. 所以无法直接通过容器映射配置文件的方式启动容器化集群, 对于此问题, 有两种解决方案

1. 先成功启动所有容器, 安装插件, 修改配置文件后重启所有容器;
2. 启动容器后, 在所有节点上通过 SQL 指令完成集群配置, 初始化以及加入集群等操作;

本例采用方法 2 进行演示, 该方法也适用于非容器环境

参考 [单实例](../standalone/README.md) 以及 [主从模式](../cluster-ms/README.md) 的相关章节的容器配置配置各个节点, 并启动容器

注意: 各个节点避免 `read_only` 和 `super_read_only` 的配置

启动所有容器 (参见 [docker-compose.yml](./docker-compose.yml) 中对集群中 3 个节点的配置)

在第一次启动容器时, 安装插件, 安装插件的脚本参考 [sql/initialize.sql](./sql/initialize.sql)

### 3.2. 节点配置

以多主模式为例 (单主模式参考 [配置单主模式](#212-配置单主模式)) 一章

- 在所有节点执行如下 SQL 脚本

  ```sql
  SET GLOBAL group_replication_group_name = '38f34157-cbe8-4623-a7bd-054cc5c2de0b';
  SET GLOBAL group_replication_group_seeds ='mgr_01:10061,mgr_02:10061,mgr_03:10061';

  SET GLOBAL group_replication_single_primary_mode = OFF;
  SET GLOBAL group_replication_enforce_update_everywhere_checks = ON;
  ```

- 在主节点 (`mgr-01`) 执行 SQL 脚本

  ```sql
  SET GLOBAL group_replication_local_address = 'mgr_01:10061';
  SET GLOBAL group_replication_bootstrap_group = ON;

  CHANGE MASTER TO
    MASTER_USER='repl',
    MASTER_PASSWORD='repl'
    FOR CHANNEL 'group_replication_recovery';

  RESET MASTER;

  START GROUP_REPLICATION;

  SET GLOBAL group_replication_bootstrap_group = OFF;
  ```

- 在节点 2 (`mgr-02`) 执行 SQL 脚本

  ```sql
  SET GLOBAL group_replication_local_address = 'mgr_02:10061';

  CHANGE MASTER TO
    MASTER_USER='repl',
    MASTER_PASSWORD='repl'
    FOR CHANNEL 'group_replication_recovery';

  RESET MASTER;

  START GROUP_REPLICATION;
  ```

- 在节点 3 (`mgr-03`) 执行 SQL 脚本

  ```sql
  SET GLOBAL group_replication_local_address = 'mgr_03:10061';

  CHANGE MASTER TO
    MASTER_USER='repl',
    MASTER_PASSWORD='repl'
    FOR CHANNEL 'group_replication_recovery';

  RESET MASTER;

  START GROUP_REPLICATION;
  ```

注意, `RESET MASTER` 用于重设 Binlog 的起始位置, 这样就可以避免各节点上已有数据的同步, 同时避免这部分数据产生的冲突 (比如各个节点上存在的 `repl` 用户), 这个指令必须在第一次建立集群时使用, 且各个节点上无实际业务数据

在任意节点上执行如下 SQL 查询, 查看集群状态

```sql
SELECT * FROM `performance_schema`.`replication_group_members`;

+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
| CHANNEL_NAME              | MEMBER_ID                            | MEMBER_HOST | MEMBER_PORT | MEMBER_STATE | MEMBER_ROLE | MEMBER_VERSION |
+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
| group_replication_applier | 690cd5f0-ecd5-11ed-a1ea-0242ac170003 | mgr_01      |        3306 | ONLINE       | PRIMARY     | 8.0.26         |
| group_replication_applier | 68f5f398-ecd5-11ed-a22b-0242ac170002 | mgr_02      |        3306 | ONLINE       | PRIMARY     | 8.0.26         |
| group_replication_applier | 691a1eb6-ecd5-11ed-a1a6-0242ac170004 | mgr_03      |        3306 | ONLINE       | PRIMARY     | 8.0.26         |
+---------------------------+--------------------------------------+-------------+-------------+--------------+-------------+----------------+
```

### 3.2. 重启集群

多主集群重启后, 需要在每个节点进行如下操作:

1. 在所有节点执行如下操作

    ```sql
    SET GLOBAL group_replication_group_name = '38f34157-cbe8-4623-a7bd-054cc5c2de0b';
    SET GLOBAL group_replication_group_seeds ='mgr_01:10061,mgr_02:10061,mgr_03:10061';

    SET GLOBAL group_replication_single_primary_mode = OFF;
    SET GLOBAL group_replication_enforce_update_everywhere_checks = ON;
    ```

    重设集群变量, 如果使用配置文件, 这一步可以忽略

2. 第一个节点操作如下

    ```sql
    SET GLOBAL group_replication_local_address = 'mgr_01:10061';
    SET GLOBAL group_replication_bootstrap_group = ON;

    START GROUP_REPLICATION;
    SET GLOBAL group_replication_bootstrap_group = OFF;
    ```

3. 其它节点操作如下

    ```sql
    SET GLOBAL group_replication_local_address = '<mgr_02:10061>/<mgr_03:10061>';

    START GROUP_REPLICATION;
    ```

## 4. 故障恢复

故障节点可以随时从集群里摘除, 修复之后重新加入集群即可, 对于使用变量进行配置的情况, 进行如下操作即可:

以 `mgr-01` 节点故障为例

1. 停止节点

    ```sql
    STOP GROUP_REPLICATION;
    ```

2. 重启节点, 并加入集群

    ```sql
    SET GLOBAL group_replication_group_name = '38f34157-cbe8-4623-a7bd-054cc5c2de0b';
    SET GLOBAL group_replication_start_on_boot = OFF;
    SET GLOBAL group_replication_group_seeds ='mgr_01:10061,mgr_02:10061,mgr_03:10061';
    SET GLOBAL group_replication_bootstrap_group = OFF;
    SET GLOBAL group_replication_single_primary_mode = OFF;
    SET GLOBAL group_replication_enforce_update_everywhere_checks = ON;

    SET GLOBAL group_replication_local_address = 'mgr_01:10061';

    START GROUP_REPLICATION;
    ```

## 5. 加入负载均衡

通过 HAProxy 进行负载均衡, 参考 [HAProxy](../cluster-xtradb/README.md#234-haproxy) 章节
