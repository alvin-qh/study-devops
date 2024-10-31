# 使用配置文件

- [使用配置文件](#使用配置文件)
  - [1. 配置文件使用时机](#1-配置文件使用时机)
  - [2. 配置文件说明](#2-配置文件说明)
    - [2.1. admin\_variables 以及 mysql\_variables 段](#21-admin_variables-以及-mysql_variables-段)
    - [2.2. mysql\_users 段](#22-mysql_users-段)
    - [2.3. mysql\_servers 段](#23-mysql_servers-段)
    - [2.4. mysql\_replication\_hostgroups 段](#24-mysql_replication_hostgroups-段)
    - [2.5. mysql\_group\_replication\_hostgroups 段](#25-mysql_group_replication_hostgroups-段)
    - [2.6. mysql\_query\_rules 段](#26-mysql_query_rules-段)
    - [2.7 scheduler 节](#27-scheduler-节)

## 1. 配置文件使用时机

ProxySQL 配置文件除了配置服务的基本属性 (服务地址/端口号, 数据存储位置等) 外, 也可以配置"用户", "查询规则", "集群" 等属性

可以说, 所有通过 ProxySQL Admin 进行的设置, 都可以通过配置文件达成

一般情况下, 不推荐使用 ProxySQL 配置, 原因如下:

- 查询规则是需要根据业务不断调整的, 通过 ProxySQL Admin 调整的规则只需要加载到 RUNTIME 即可, 而无需重启服务;
- ProxySQL 在启动时, 会将 CONFIG 和 DISK 中的配置均进行加载, 且 DISK 中的配置优先级更高, 所以配置文件中的内容往往不代表 ProxySQL 的真实设置, 仍需要通过 ProxySQL Admin 查询 RUNTIME 的实际情况

但在开发环境, 由于数据会经常性的被清除, 且需要将设置同步给不同的开发人员, 所以可以通过配置文件固化所需的配置, 以减少开发环境的配置复杂度

## 2. 配置文件说明

配置文件的格式为

```ini
section =
{
  property=value
}
```

或者

```ini
section =
(
  {
    property1=value1
    property2=value2
    ...
  },
  {
    property1=value1
    property2=value2
    ...
  },
  ...
)
```

后者表示配置内容有多条 (即是一个"数组"), 每个 `{}` 中表示一项

其中, `section` 包括:

- [admin_variables](#21-admin_variables-以及-mysql_variables-段), 包含 ProxySQL 本身的配置项;
- [mysql_variables](#21-admin_variables-以及-mysql_variables-段), 包含 ProxySQL 访问后端 MySQL 实例的配置项;
- [mysql_users](#22-mysql_users-段), 包含 ProxySQL 连接后端 MySQL 实例的用户的配置项;
- [mysql_servers](#23-mysql_servers-段), 包含 ProxySQL 连接后端 MySQL 实例的配置项;
- [mysql_replication_hostgroups](#24-mysql_replication_hostgroups-段), 管理后端 MySQL 实例组配置项, 用于后端数据库为"主从", "异步复制" 以及 "半同步复制"集群的方式;
- [mysql_group_replication_hostgroups](#25-mysql_group_replication_hostgroups-段), 管理后端 MySQL 实例组配置项, 用于后端数据库为"MGR", "Galera" 以及 "PXC" 集群的方式;
- [mysql_query_rules](#26-mysql_query_rules-段), 配置 SQL 语句规则, 包括 SQL 路由, 查询缓冲等;
- [scheduler](#27-scheduler-节), 配置 ProxySQL 定时任务的配置项;

所有的 `section` 都会对应 ProxySQL Admin 中的一张配置表, 所以大部分配置都可以同时通过配置文件和 ProxySQL Admin 来进行

唯一例外的是 `datadir` 配置项, 它不属于任何 `section`, 用于指定 ProxySQL Admin 配置表的存储路径

### 2.1. admin_variables 以及 mysql_variables 段

`admin_variables` 以及 `mysql_variables` 段中包含了 ProxySQL 本身的配置项, 对应 ProxySQL Admin 的 `main.global_variables` 表, 是 ProxySQL 运行时必要的变量

`admin_variables` 中的配置项在 `main.global_variables` 表中, 以 `admin-` 为前缀的记录表示; `mysql_variables` 中的配置项在 `main.global_variables` 表中, 以 `mysql-` 为前缀的记录表示

另外, `admin_variables` 和 `mysql_variables` 中的配置项也可以通过 `SET <admin/mysql>-<variable-name>` 指令来修改, 通过 `SELECT @@<admin/mysql>-<variable-name>` 来查询, 例如:

```sql
SET admin-refresh_interval = 3000;
Query OK, 1 row affected (0.01 sec)

SELECT @@admin-refresh_interval;
+--------------------------+
| @@admin-refresh_interval |
+--------------------------+
| 3000                     |
+--------------------------+
1 row in set (0.01 sec)
```

相当于执行

```sql
UPDATE `global_variables`
SET `variable_value` = 3000
WHERE `variable_name` = 'admin-refresh_interval';
Query OK, 1 row affected (0.00 sec)

SELECT `variable_value`
FROM `global_variables`
WHERE `variable_name` = 'admin-refresh_interval';
+----------------+
| variable_value |
+----------------+
| 3000           |
+----------------+
1 row in set (0.01 sec)
```

查询 ProxySQL admin `main` 库中的表数据时, 可以不加 `main.` 前缀, 因为 `main` 库中存储的均为 ProxySQL 运行时所需的关键配置和数据

这部分内容可以参考 [官方文档](https://proxysql.com/documentation/global-variables/), 内有详细的配置项 (或变量) 说明

特别说明一下, 上述的 `main.global_variables` 表或对应的变量指的是 MEMORY 层中的设置, 并不会直接生效, 修改完毕后仍要加载到 RUNTIME 层后方能生效

```sql
LOAD ADMIN VARIABLES TO RUNTIME;
LOAD MYSQL VARIABLES TO RUNTIME;
```

上述操作会将数据从 `main.global_variables` 复制到 `main.runtime_global_variables` 表中

### 2.2. mysql_users 段

`mysql_users` 中包含了 ProxySQL 连接后端 MySQL 实例的用户的配置项, 对应的是 `main.mysql_user` 表的记录, 即:

```sql
SHOW CREATE TABLE `mysql_users`\G

*************************** 1. row ***************************
       table: mysql_users
Create Table: CREATE TABLE mysql_users (
  username VARCHAR NOT NULL,
  password VARCHAR,
  active INT CHECK (active IN (0,1)) NOT NULL DEFAULT 1,
  use_ssl INT CHECK (use_ssl IN (0,1)) NOT NULL DEFAULT 0,
  default_hostgroup INT NOT NULL DEFAULT 0,
  default_schema VARCHAR,
  schema_locked INT CHECK (schema_locked IN (0,1)) NOT NULL DEFAULT 0,
  transaction_persistent INT CHECK (transaction_persistent IN (0,1)) NOT NULL DEFAULT 1,
  fast_forward INT CHECK (fast_forward IN (0,1)) NOT NULL DEFAULT 0,
  backend INT CHECK (backend IN (0,1)) NOT NULL DEFAULT 1,
  frontend INT CHECK (frontend IN (0,1)) NOT NULL DEFAULT 1,
  max_connections INT CHECK (max_connections >=0) NOT NULL DEFAULT 10000,
  attributes VARCHAR CHECK (JSON_VALID(attributes) OR attributes = '') NOT NULL DEFAULT '',
  comment VARCHAR NOT NULL DEFAULT '',
  PRIMARY KEY (username, backend),
  UNIQUE (username, frontend)
)
```

- `username`, `password`, 用于连接到 mysqld 或 ProxySQL 实例的凭据, 密码可以是明文形式, 也可以是散列式密码. 另请参 [阅密码管理](./passwords.md);
- `active`, `active=0` 的用户存储在 DISK 或 MEMORY 层中, 但永远不会加载到 RUNTIME 层中;
- `use_ssl`, 如果设置为 `1`, 则强制用户使用 SSL 证书进行身份验证. 另请参阅 [SSL 支持](./ssl.md);
- `default_hostgroup`, 该用户分配的默认后端数据库组, 当该用户发送的 SQL 语句未命中任何规则时, 会路由到这个数据库组;
- `default_schema`, 默认情况下该用户连接所更改的 Schema 名称;
- `schema_locked`, 尚不支持;
- `transaction_persistent`, 如果设置为 `1`, 则该用户启动的事务都会位于 `default_hostgroup` 指定的数据库组中;
- `fast_forward`, 如果设置为 `1`, 它将绕过查询处理层 (SQL 重写, 缓存), 并按原样将查询传递到后端服务器;
- `frontend`, 如果设置为 `1`, 则该用户的用户名和密码同时也可为 ProxySQL 进行身份验证;
- `backend`, 如果设置为 `1`, 则该用户的用户名和密码同时也可为后端 MySQL 进行身份验证;
- `max_connections`, 定义该用户到 ProxySQL 允许发起的最大连接数;
- `attributes`, 暂不支持;
- `comment`, 注释

`mysql_users` 中的每组配置对应 `main.mysql_users` 表中的一条记录, 其配置项和 `main.mysql_users` 表的字段一一对应, 例如:

```ini
mysql_users=
(
  {
    username="user1"
    password="user1pass"
    default_hostgroup=100
    active=1
  },
  {
    username="user2"
    password="user2pass"
    default_hostgroup=101
    active=1
  }
)
```

相当于 `main.mysql_users` 表里的两条记录 (未涉及的字段全部取默认值)

注意, 对于 `main.mysql_users` 表的修改是针对 MEMORY 层的, 需要加载到 RUNTIME 层才能生效, 即:

```sql
LOAD MYSQL USERS TO RUNTIME;
```

相当于将 `main.mysql_users` 表中的数据复制到 `main.runtime_mysql_users` 表

更详细的 `mysql_users` 配置说明请参见 [官方文档](https://proxysql.com/documentation/main-runtime/#mysql_users)

### 2.3. mysql_servers 段

`mysql_servers` 中包含 ProxySQL 连接后端 MySQL 实例的配置项, 对应的是 `main.mysql_servers` 表中的记录, 即:

```sql
SHOW CREATE TABLE `mysql_servers`\G

*************************** 1. row ***************************
       table: mysql_servers
Create Table: CREATE TABLE mysql_servers (
  hostgroup_id INT CHECK (hostgroup_id>=0) NOT NULL DEFAULT 0,
  hostname VARCHAR NOT NULL,
  port INT CHECK (port >= 0 AND port <= 65535) NOT NULL DEFAULT 3306,
  gtid_port INT CHECK ((gtid_port <> port OR gtid_port=0) AND gtid_port >= 0 AND gtid_port <= 65535) NOT NULL DEFAULT 0,
  status VARCHAR CHECK (UPPER(status) IN ('ONLINE','SHUNNED','OFFLINE_SOFT','OFFLINE_HARD')) NOT NULL DEFAULT 'ONLINE',
  weight INT CHECK (weight >= 0 AND weight <=10000000) NOT NULL DEFAULT 1,
  compression INT CHECK (compression IN(0,1)) NOT NULL DEFAULT 0,
  max_connections INT CHECK (max_connections >=0) NOT NULL DEFAULT 1000,
  max_replication_lag INT CHECK (max_replication_lag >= 0 AND max_replication_lag <= 126144000) NOT NULL DEFAULT 0,
  use_ssl INT CHECK (use_ssl IN(0,1)) NOT NULL DEFAULT 0,
  max_latency_ms INT UNSIGNED CHECK (max_latency_ms>=0) NOT NULL DEFAULT 0,
  comment VARCHAR NOT NULL DEFAULT '',
  PRIMARY KEY (hostgroup_id, hostname, port)
)
```

- `hostgroup_id`, 当前后端数据库所在的组 ID, 每个组可以包含多个数据库实例, 所以该字段可以重复;
- `hostname`, `port`, 访问后端数据库实例的主机名 (IP 地址) 和端口号, 如果端口号为 `0`, 则 `hostname` 表示 Unix 套接字文件 (`.sock` 文件);
- `gtid_port`, 用于 ProxySQL Binlog Reader 侦听后端服务器 GTID 的端口号;
- `status`, 用于设置该后端数据库实例的状态, 注意: 这是个配置值, 而不是实际的后端数据库状态值
  - `ONLINE`, 表示该后端数据库在线, 可以使用;
  - `SHUNNED`, 表示该后端数据库因为发生了太多错误而暂停使用;
  - `OFFLINE_SOFT`, 表示该后端数据库需要离线, 不在向其路由新的查询, 将正在执行的查询处理完毕后下线 (优雅离线);
  - `OFFLINE_HARD`, 表示该后端数据库需要立即离线, 正在处理的查询全部立即终止, 向客户端返回错误;
- `weight`, 值越大, 该后端数据库在所在数据库组中被访问的几率就越大 (以随机加权作为负载均衡算法);
- `compression`, 如果值为 `1`, 则与该后端数据库的新连接将使用数据压缩, 现有连接不受影响;
- `max_connections`, ProxySQL 到该后端数据库的最大连接数, 如果超出该连接数, 即使 `weight` 设置的再大, 也不会建立新的连接. 如果 ProxySQL 的"连接多路复用"功能可用, 则此值可以设置的很小;
- `max_replication_lag`, 如果大于 `0`, ProxySQL 将定期监控后端数据库的复制滞后, 如果超过该配置表示的阈值, 将暂时避开该数据库节点, 直到复制进度赶上;
- `use_ssl`, 如果设置为 `1`, 则与后端的连接将使用 SSL;
- `max_latency_ms`, ProxySQL 对该后端数据库 ping 监控的间隔时间;
- `comment`, 注释

其配置和使用方法和 `mysql_users` 完全类似, 具体配置说明请参见 [官方文档](https://proxysql.com/documentation/main-runtime/#mysql_servers)

### 2.4. mysql_replication_hostgroups 段

`mysql_replication_hostgroups` 中包含了管理后端 MySQL 实例组配置项, 用于后端数据库为"主从", "异步复制" 以及 "半同步复制"集群的方式的配置, 对应的是 `main.mysql_replication_hostgroups` 表中的记录, 即:

```sql
SHOW CREATE TABLE `mysql_replication_hostgroups`\G

*************************** 1. row ***************************
       table: mysql_replication_hostgroups
Create Table: CREATE TABLE mysql_replication_hostgroups (
  writer_hostgroup INT CHECK (writer_hostgroup>=0) NOT NULL PRIMARY KEY,
  reader_hostgroup INT NOT NULL CHECK (reader_hostgroup<>writer_hostgroup AND reader_hostgroup>=0),
  check_type VARCHAR CHECK (LOWER(check_type) IN ('read_only','innodb_read_only','super_read_only','read_only|innodb_read_only','read_only&innodb_read_only')) NOT NULL DEFAULT 'read_only',
  comment VARCHAR NOT NULL DEFAULT '', UNIQUE (reader_hostgroup)
)
```

- `writer_hostgroup`, 配置"写"数据库实例组. 另外, 返回 `read_only=0` 的数据库实例会分配到该组;
- `reader_hostgroup`, 配置"读"数据库实例组. 通过规则将"读"请求发送到此组, 或者账号为只读权限的请求也应该发送到此组, 另外, 返回 `read_only=1` 的数据库实例会分配到此组;
- `check_type`, 用于后端数据库检查的 MySQL 变量, 默认为 `read_only` 变量;
- `comment`, 注释

其配置和使用方法和 `mysql_users` 完全类似, 具体配置说明请参见 [官方文档](https://proxysql.com/documentation/main-runtime/#mysql_replication_hostgroups)

### 2.5. mysql_group_replication_hostgroups 段

`mysql_group_replication_hostgroups` 中包含了管理后端 MySQL 实例组配置项, 用于后端数据库为"MGR", "Galera" 以及 "PXC" 集群方式的配置, 对应的是 `main.mysql_group_replication_hostgroups` 表中的记录, 即:

```sql
SHOW CREATE TABLE `mysql_group_replication_hostgroups`\G

*************************** 1. row ***************************
       table: mysql_group_replication_hostgroups
Create Table: CREATE TABLE mysql_group_replication_hostgroups (
  writer_hostgroup INT CHECK (writer_hostgroup>=0) NOT NULL PRIMARY KEY,
  backup_writer_hostgroup INT CHECK (backup_writer_hostgroup>=0 AND backup_writer_hostgroup<>writer_hostgroup) NOT NULL,
  reader_hostgroup INT NOT NULL CHECK (reader_hostgroup<>writer_hostgroup AND backup_writer_hostgroup<>reader_hostgroup AND reader_hostgroup>0),
  offline_hostgroup INT NOT NULL CHECK (offline_hostgroup<>writer_hostgroup AND offline_hostgroup<>reader_hostgroup AND backup_writer_hostgroup<>offline_hostgroup AND offline_hostgroup>=0),
  active INT CHECK (active IN (0,1)) NOT NULL DEFAULT 1,
  max_writers INT NOT NULL CHECK (max_writers >= 0) DEFAULT 1,
  writer_is_also_reader INT CHECK (writer_is_also_reader IN (0,1,2)) NOT NULL DEFAULT 0,
  max_transactions_behind INT CHECK (max_transactions_behind>=0) NOT NULL DEFAULT 0,
  comment VARCHAR,
  UNIQUE (reader_hostgroup),
  UNIQUE (offline_hostgroup),
  UNIQUE (backup_writer_hostgroup)
)
```

- `writer_hostgroup`, 配置"写"数据库实例组. 另外, 返回 `read_only=0` 的数据库实例会分配到该组;
- `backup_writer_hostgroup`, 如果后端数据库集群有多个"写"节点 (`read_only=0`), 则超出 `max_writers` 值的数据库实例会放在该组;
- `reader_hostgroup`, 配置"读"数据库实例组. 通过规则将"读"请求发送到此组, 或者账号为只读权限的请求也应该发送到此组, 另外, 返回 `read_only=1` 的数据库实例会分配到此组;
- `offline_hostgroup`, 当监控到后端数据库实例状况不佳时, 需要暂时离线时, 会将其放到这个组;
- `active`, `1` 表示当前配置生效;
- `max_writers`, `writer_hostgroup` 组中允许的数据库实例最大数量, 超出部分会放入 `backup_writer_hostgroup` 组中;
- `writer_is_also_reader`, `0` 表示无效, `1` 表示"写"组的数据库实例也同时用于读操作, `2` 表示只有在 `backup_writer_hostgroup` 组的数据库实例可以用于读操作;
- `max_transactions_behind`, 在避开此节点以防止读取陈旧数据前, 允许执行的事务的数量 (该值可以从后端数据库的 `sys.gr_member_routing_candidate_status` 表的 `transactions_behind` 字段查询到);
- `comment`, 注释

其配置和使用方法和 `mysql_users` 完全类似, 具体配置说明请参见 [官方文档](https://proxysql.com/documentation/main-runtime/#mysql_group_replication_hostgroups)

### 2.6. mysql_query_rules 段

`mysql_query_rules` 中包含了 SQL 语句规则, 包括匹配规则, SQL 路由, 查询缓冲等的配置项, 对应的是 `main.mysql_query_rules` 表中的记录, 即:

```sql
SHOW CREATE TABLE `mysql_query_rules`\G

*************************** 1. row ***************************
       table: mysql_query_rules
Create Table: CREATE TABLE mysql_query_rules (
  rule_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  active INT CHECK (active IN (0,1)) NOT NULL DEFAULT 0,
  username VARCHAR,
  schemaname VARCHAR,
  flagIN INT CHECK (flagIN >= 0) NOT NULL DEFAULT 0,
  client_addr VARCHAR,
  proxy_addr VARCHAR,
  proxy_port INT CHECK (proxy_port >= 0 AND proxy_port <= 65535),
  digest VARCHAR,
  match_digest VARCHAR,
  match_pattern VARCHAR,
  negate_match_pattern INT CHECK (negate_match_pattern IN (0,1)) NOT NULL DEFAULT 0,
  re_modifiers VARCHAR DEFAULT 'CASELESS',
  flagOUT INT CHECK (flagOUT >= 0),
  replace_pattern VARCHAR CHECK(CASE WHEN replace_pattern IS NULL THEN 1 WHEN replace_pattern IS NOT NULL AND match_pattern IS NOT NULL THEN 1 ELSE 0 END),
  destination_hostgroup INT DEFAULT NULL,
  cache_ttl INT CHECK(cache_ttl > 0),
  cache_empty_result INT CHECK (cache_empty_result IN (0,1)) DEFAULT NULL,
  cache_timeout INT CHECK(cache_timeout >= 0),
  reconnect INT CHECK (reconnect IN (0,1)) DEFAULT NULL,
  timeout INT UNSIGNED CHECK (timeout >= 0),
  retries INT CHECK (retries>=0 AND retries <=1000),
  delay INT UNSIGNED CHECK (delay >=0),
  next_query_flagIN INT UNSIGNED,
  mirror_flagOUT INT UNSIGNED,
  mirror_hostgroup INT UNSIGNED,
  error_msg VARCHAR,
  OK_msg VARCHAR,
  sticky_conn INT CHECK (sticky_conn IN (0,1)),
  multiplex INT CHECK (multiplex IN (0,1,2)),
  gtid_from_hostgroup INT UNSIGNED,
  log INT CHECK (log IN (0,1)),
  apply INT CHECK(apply IN (0,1)) NOT NULL DEFAULT 0,
  attributes VARCHAR CHECK (JSON_VALID(attributes) OR attributes = '') NOT NULL DEFAULT '',
  comment VARCHAR
)
```

该表字段参考 [mysql_query_rules 表详解](./query-rule.md#11-mysql_query_rules-表详解) 一节内容

其配置和使用方法和 `mysql_users` 完全类似, 具体配置说明请参见 [官方文档](https://proxysql.com/documentation/main-runtime/#mysql_query_rules)

### 2.7 scheduler 节

`scheduler` 中包含了 ProxySQL 定时任务的配置项, 对应的是 `main.scheduler` 表中的记录, 即:

```sql
SHOW CREATE TABLE `scheduler`\G

*************************** 1. row ***************************
       table: scheduler
Create Table: CREATE TABLE scheduler (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  active INT CHECK (active IN (0,1)) NOT NULL DEFAULT 1,
  interval_ms INTEGER CHECK (interval_ms>=100 AND interval_ms<=100000000) NOT NULL,
  filename VARCHAR NOT NULL,
  arg1 VARCHAR,
  arg2 VARCHAR,
  arg3 VARCHAR,
  arg4 VARCHAR,
  arg5 VARCHAR,
  comment VARCHAR NOT NULL DEFAULT ''
)
```

- `active`, `1` 表示该定时任务生效;
- `interval_ms`, 表示定时任务每次重复执行的间隔;
- `filename`, 表示定时任务要执行的可执行文件名, 可以为 sh 脚本或可执行文件;
- `arg1` ~ `arg5`, 送入到 `filename` 执行的参数值;
- `comment`, 注释;

其配置和使用方法和 `mysql_users` 完全类似, 具体配置说明请参见 [官方文档](https://proxysql.com/documentation/main-runtime/#scheduler)
