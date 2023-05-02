# ProxySQL 集群

- [ProxySQL 集群](#proxysql-集群)
  - [1. 概述](#1-概述)
  - [2. Monitoring 组件](#2-monitoring-组件)
    - [2.1. Admin 变量](#21-admin-变量)
      - [2.1.1. 定义要同步内容的相关变量](#211-定义要同步内容的相关变量)
      - [2.1.2. 定义凭证的相关变量](#212-定义凭证的相关变量)
      - [2.1.3. 定义检查间隔/频率的相关变量](#213-定义检查间隔频率的相关变量)
      - [2.1.4. 同步到磁盘相关的变量](#214-同步到磁盘相关的变量)
      - [2.1.5. 是否要远程同步的变量](#215-是否要远程同步的变量)
    - [2.2. 配置相关的表](#22-配置相关的表)
      - [2.2.1. proxysql\_servers 表](#221-proxysql_servers-表)
      - [2.2.2. runtime\_checksums\_values 表](#222-runtime_checksums_values-表)
    - [2.3. 新命令](#23-新命令)
    - [2.4. 统计表](#24-统计表)
      - [2.4.1. stats\_proxysql\_servers\_checksums 表](#241-stats_proxysql_servers_checksums-表)
      - [2.4.2. stats\_proxysql\_servers\_metrics 表](#242-stats_proxysql_servers_metrics-表)
      - [2.4.3. stats\_proxysql\_servers\_status 表](#243-stats_proxysql_servers_status-表)
    - [2.5. 关于带宽](#25-关于带宽)
  - [3. Re-configuration](#3-re-configuration)
  - [4. Roadmap](#4-roadmap)
  - [5. Q \& A](#5-q--a)

## 1. 概述

ProxySQL 是一个去中心化的代理中间件, 通常将和应用程序部署在同一台服务器 (节点) 上, 这种方式可以无限的扩展节点, 且配置较为方便 (例如通过 Ansible/Chef/Puppet/Salt 等远程配置工具), 或者通过服务发现 (如: Consul/ZooKeeper 等工具)

但上述方式也有一些缺点

- 需要依赖外部软件 (远程部署工具, 服务发现工具等);
- 无法得到 ProxySQL 原生支持 (因为引入了外部工具);
- 收敛时间不可预知, 系统规模会逐步扩大;
- 无法通过网络隔离进行信息防护;

所以需要了解 ProxySQL 的集群化部署, 已解决上述问题

ProxySQL 从 v1.4.x 版本开始支持集群, 下面介绍如何基于 ProxySQL 进行集群部署

ProxySQL 为集群解决方案提供了两个主要组件

- Monitoring (监控组件)
- Re-configuration (重配置组件)

这两个组件由以下 4 个 ProxySQL Admin 配置表提供支持

- `global_variables` (自 v2.1.x 起开始包含)
- `mysql_query_rules`
- `mysql_servers`
- `mysql_users`
- `proxysql_servers`

## 2. Monitoring 组件

为了支持集群方案, ProxySQL 添加了一些新的表, 命令和变量, 包括:

### 2.1. Admin 变量

ProxySQL 为集群方案添加了一组 Admin 变量, 包括:

#### 2.1.1. 定义要同步内容的相关变量

- `admin-checksum_mysql_query_rules`,
- `admin-checksum_mysql_servers`,
- `admin-checksum_mysql_users`, 这些变量类型都为布尔类型. 当设置为 `true` (默认值) 时, ProxySQL 在每次执行 `LOAD MYSQL <QUERY RULES | SERVERS | USERS> TO RUNTIME` 时都会生成一个新的配置校验和. 如果设置为 `false`, 则不会生成校验和, 新的配置内容不会传播出去, 也不会从其它节点同步配置到本机;

注意: 如果 ProxySQL 中定义的用户数量很大 (百万级别), 则应该设置 `admin-checksum_mysql_users=false`, 否则会很慢

#### 2.1.2. 定义凭证的相关变量

- `admin-cluster_username` 和 `admin-cluster_password`, 表示该用户凭据用于监控其它 ProxySQL 实例. 需要注意, 这个用户密码必须是 `admin-admin_credentials` 中已经存在的, 否则将会连接失败. 如果没有定义集群凭据, ProxySQL 集群将不会做任何检查;

#### 2.1.3. 定义检查间隔/频率的相关变量

- `admin-cluster_check_interval_ms`, 定义校验和 (Checksum) 检查的时间间隔。默认值 `1000` (即 `1` 秒). 可设置的最小值`10`, 最大值 `300000`;
- `admin-cluster_check_status_frequency`, 该变量定义做了多少次校验和检查后, 就执行一次状态 (Status) 检查. 默认值 `10`. 可设置的最小为 `0`, 最大值为 `10000`;

#### 2.1.4. 同步到磁盘相关的变量

在远程同步配置之后, 通常最好的做法是立即将新的更改保存到磁盘. 这样重启时, 更改的配置不会丢失

- `admin-cluster_mysql_query_rules_save_to_disk`,
- `admin-cluster_mysql_servers_save_to_disk`,
- `admin-cluster_mysql_users_save_to_disk`,
- `admin-cluster_proxysql_servers_save_to_disk`, 这些变量都是布尔类型. 当设置为 `true` (默认值) 时, 在远程同步并加载到 RUNTIME 后, 新的 `mysql_query_rules`, `mysql_servers`, `mysql_users`, `proxysql_servers` 配置会持久化到磁盘中

#### 2.1.5. 是否要远程同步的变量

由于某些原因, 可能多个 ProxySQL 实例会在同一时间进行重新配置

例如, 每个 ProxySQL 实例都在监控 MySQL 的 Replication, 且自动探测到 MySQL 的故障转移, 在一个极短的时间内 (可能小于 `1` 秒), 这些 ProxySQL 实例可能会自动调整新的配置, 而无需通过其它 ProxySQL 实例来同步新配置

类似的还有, 当所有 ProxySQL 实例都探测到了和某实例的临时的网络问题, 或者某个 MySQL 节点比较慢 (Replication Lag), 这些 ProxySQL 实例都会自动地避开这些节点. 这时各 ProxySQL 实例也无需从其它节点处同步配置, 而是同时自动完成新的配置

基于此，可以配置 ProxySQL 集群, 让各 ProxySQL 实例暂时无需从其它实例处同步某些配置, 而是等待一定次数的检查之后, 再触发远程同步. 但是, 如果本地和远程节点的这些变量阈值不同, 则还是会触发远程同步

- `admin-cluster_mysql_query_rules_diffs_before_sync`,
- `admin-cluster_mysql_servers_diffs_before_sync`,
- `admin-cluster_mysql_users_diffs_before_sync`,
- `admin-cluster_proxysql_servers_diffs_before_sync`, 这些变量分别定义经过多少次的"无法匹配"检查之后, `触发mysql_query_rules`, `mysql_servers`, `mysql_users`, `proxysql_servers` 配置的远程同步. 默认值 `3` 次, 可配置的最小值为 `0`, 表示永不远程同步, 最大值为 `1000`;

> 注: 例如，各实例监控 `mysql_servers` 配置并做校验码检查, 如果某实例和本地配置不同, 当多次检测到都不同时, 将根据 `LOAD TO RUNTIME` 的时间戳决定是否要从远端将 `mysql_servers` 同步到本地

### 2.2. 配置相关的表

#### 2.2.1. proxysql_servers 表

`proxysql_servers` 表定义了 ProxySQL 集群中各 ProxySQL 实例列表. 该表的定义语句如下:

```sql
CREATE TABLE proxysql_servers (
  hostname VARCHAR NOT NULL,
  port INT NOT NULL DEFAULT 6032,
  weight INT CHECK (weight >= 0) NOT NULL DEFAULT 0,
  comment VARCHAR NOT NULL DEFAULT '',
  PRIMARY KEY (hostname, port)
)
```

- `hostname`, ProxySQL 实例的主机名或 IP 地址;
- `port`, ProxySQL 实例的端口 (这个端口是 ProxySQL 示例的 Admin 管理端口);
- `weight`, 定义集群中各 ProxySQL 的权重值;
- `comment`, 注释

`proxysql_servers` 的配置项也可以从配置文件中加载, 例如:

```ini
proxysql_servers=
(
  {
    hostname="172.16.0.101"
    port=6032
    weight=0
    comment="proxysql1"
  },
  {
    hostname="172.16.0.102"
    port=6032
    weight=0
    comment="proxysql2"
  }
)
```

#### 2.2.2. runtime_checksums_values 表

`runtime_checksums_values` 表不基于 `main` 库中的 `runtime_` 表 (即没有 `checksums_values` 表)

该表用于记录执行 `LOAD TO RUNTIME` 命令时的一些信息, 定义语句如下:

```sql
CREATE TABLE runtime_checksums_values (
  name VARCHAR NOT NULL,
  version INT NOT NULL,
  epoch INT NOT NULL,
  checksum VARCHAR NOT NULL,
  PRIMARY KEY (name)
)
```

- `name`, 表示模块的名称;
- `version`, 表示执行了多少次 `LOAD TO RUNTIME` 操作，包括所有隐式和显式执行的 (某些事件会导致 ProxySQL 内部自动执行 `LOAD TO RUNTIME` 命令);
- `epoch`, 表示最近一次执行 `LOAD TO RUNTIME` 的时间戳;
- `checksum`, 表示执行 `LOAD TO RUNTIME` 时生成的配置校验和 (Checksum);

下面是该表的一个实例

```sql
SELECT * FROM `runtime_checksums_values`;

+-------------------+---------+------------+--------------------+
| name              | version | epoch      | checksum           |
+-------------------+---------+------------+--------------------+
| admin_variables   | 0       | 0          |                    |
| mysql_query_rules | 5       | 1503442167 | 0xD3BD702F8E759B1E |
| mysql_servers     | 1       | 1503440533 | 0x6F8CEF0F4BD6456E |
| mysql_users       | 1       | 1503440533 | 0xF8BDF26C65A70AC5 |
| mysql_variables   | 0       | 0          |                    |
| proxysql_servers  | 2       | 1503442214 | 0x89768E27E4931C87 |
+-------------------+---------+------------+--------------------+
```

注意, 目前只有 4 种模块的配置会生成对应的校验码

- `LOAD MYSQL QUERY RULES TO RUNTIME`, 当 `admin-checksum_mysql_query_rules=true` 时生成一个新的 `mysql_query_rules` 配置校验码;
- `LOAD MYSQL SERVERS TO RUNTIME`, 当 `admin-checksum_mysql_servers=true` 时生成一个新的 `mysql_servers` 配置校验码;
- `LOAD MYSQL USERS TO RUNTIME`, 当 `admin-checksum_mysql_users=true` 时生成一个新的 `mysql_users` 配置校验码;
- `LOAD PROXYSQL SERVERS TO RUNTIME`, 总是会生成一个新的 `proxysql_servers` 配置校验码;
- `LOAD ADMIN VARIABLES TO RUNTIME`, 不生成校验码;
- `LOAD MYSQL VARIABLES TO RUNTIME`, 不生产校验码;

### 2.3. 新命令

- `LOAD PROXYSQL SERVERS FROM MEMORY`/`LOAD PROXYSQL SERVERS TO RUNTIME`, 将 `proxysql_servers` 配置从 MEMORY 中加载到 RUNTIME;
- `SAVE PROXYSQL SERVERS TO MEMORY`/`SAVE PROXYSQL SERVERS FROM RUNTIME`, 将 `proxysql_servers` 配置从 RUNTIME 复制到 MEMORY
- `LOAD PROXYSQL SERVERS TO MEMORY`/`LOAD PROXYSQL SERVERS FROM DISK`, 将 `proxysql_servers` 配置从DISK 读取到 MEMORY;
- `LOAD PROXYSQL SERVERS FROM CONFIG`, 从配置文件中读取 `proxysql_servers` 配置;
- `SAVE PROXYSQL SERVERS FROM MEMORY`/`SAVE PROXYSQL SERVERS TO DISK`, 将 `proxysql_servers` 配置从 MEMORY 持久化到 DISK;

### 2.4. 统计表

ProxySQL 为集群功能新增了 3 张统计表, 均位于 `stats` 库中

#### 2.4.1. stats_proxysql_servers_checksums 表

该表统计 ProxySQL 实例的校验和 (Checksum) 信息, 以及校验相关的状态, 定义如下:

```sql
SHOW CREATE TABLE `stats`.`stats_proxysql_servers_checksums`\G

*************************** 1. row ***************************
       table: stats_proxysql_servers_checksums
Create Table: CREATE TABLE stats_proxysql_servers_checksums (
  hostname VARCHAR NOT NULL,
  port INT NOT NULL DEFAULT 6032,
  name VARCHAR NOT NULL,
  version INT NOT NULL,
  epoch INT NOT NULL,
  checksum VARCHAR NOT NULL,
  changed_at INT NOT NULL,
  updated_at INT NOT NULL,
  diff_check INT NOT NULL,
  PRIMARY KEY (hostname, port, name)
)
```

- `hostname`, 表示 ProxySQL 实例的主机名;
- `port`, 表示 ProxySQL 实例的端口号;
- `name`, 表示对端 `runtime_checksums_values` 中报告的模块名称;
- `version`, 表示对端 `runtime_checksum_values` 中报告的校验和 (Checksum) 的版本;
  注意, ProxySQL 实例刚启动时 `version=1`, ProxySQL 实例将永远不会从 `version=1` 的实例处同步配置数据, 因为一个刚刚启动的 ProxyQL 实例不太可能是真实配置的来源, 这可以防止新的连接节点破坏当前集群配置;
- `epoch`, 表示对端 `runtime_checksums_values` 中报告的校验和 (Checksum) 的时间戳值;
- `checksum`, 表示对端 `runtime_checksums_values` 中报告的校验和 (Checksum) 的值;
- `changed_at`, 表示探测到校验和 (Checksum) 发生变化的时间戳;
- `updated_at`, 表示最近一次更新该类配置的时间戳;
- `diff_check`, 表示一个计数器, 用于记录探测到的对端和本地校验和 (Checksum) 值已有多少次不同, 需要等待达到阈值后, 才会触发重新配置
  - 前文已经说明, 在多个 ProxySQL 实例同时或极短时间内同时更改配置时, 可以让 ProxySQL 等待多次探测之后再决定是否从远端同步配置, 这个字段正是用于记录探测到的配置不同次数;
  - 如果 `diff_checks` 的值不断增加却仍未触发同步操作, 这意味着对端不是可信任的同步源, 例如对端的 `version=1`;
  - 另一方面, 如果某对端节点不和 ProxySQL 集群中的其它实例进行配置同步, 这意味着集群没有可信任的同步源. 这种情况可能是因为集群中所有实例启动时的配置都不一样, 它们无法自动判断哪个配置才是正确的. 可以在某个节点上执行 `LOAD TO RUNTIME`, 使该节点被选举为该类配置的可信任同步源

#### 2.4.2. stats_proxysql_servers_metrics 表

该表用于在执行 `SHOW MYSQL STATUS` 语句时, 显示一些已检索到的指标, 该表定义如下:

```sql
SHOW CREATE TABLE `stats`.`stats_proxysql_servers_metrics`\G

*************************** 1. row ***************************
       table: stats_proxysql_servers_metrics
Create Table: CREATE TABLE stats_proxysql_servers_metrics (
  hostname VARCHAR NOT NULL,
  port INT NOT NULL DEFAULT 6032,
  weight INT CHECK (weight >= 0) NOT NULL DEFAULT 0,
  comment VARCHAR NOT NULL DEFAULT '',
  response_time_ms INT NOT NULL,
  Uptime_s INT NOT NULL,
  last_check_ms INT NOT NULL,
  Queries INT NOT NULL,
  Client_Connections_connected INT NOT NULL,
  Client_Connections_created INT NOT NULL,
  PRIMARY KEY (hostname, port)
)
```

- `hostname`, 表示 ProxySQL 实例主机名 (或 IP 地址);
- `port`, 表示 ProxySQL 实例的端口号;
- `weight`, 该字段值等同于 `proxysql_servers.weight` 字段;
- `comment`, 该字段等同于 `proxysql_servers.comment` 字段;
- `response_time_ms`, 表示执行 `SHOW MYSQL STATUS` 的响应时长, 单位为 `ms`;
- `Uptime_s`, 表示 ProxySQL 实例已启动的时间, 单位为 `s`;
- `last_check_ms`, 表示最近一次执行检查距离当前的时间, 单位为 `ms`;
- `Queries`, 表示当前 ProxySQL 节点已执行查询的数量;
- `Client_Connections_connected`, 表示已连接到当前 ProxySQL 节点的客户端连接数;
- `Client_Connections_created`, 表示客户端到当前 ProxySQL 节点已经创建的连接数;

注意, 当前这些状态只为 Debug 目的, 但未来可能会作为远程实例的健康指标

#### 2.4.3. stats_proxysql_servers_status 表

该表暂未启用

### 2.5. 关于带宽

在上述描述的架构模式中, 每个 ProxySQL 节点都监控集群中其它所有节点, 这是一个完整的点对点网络

为了减少集群中网络带宽的使用, 各节点不会一次性交换所有的校验和 (Checksum) 列表, 而是交换一个由各节点上所有版本和所有校验和值结合生成的全局校验和 (每个节点都有一个全局校验和, 而不是所有节点共有一个全局校验和). 当全局校验和改变, 将检索该全局校验和对应的校验和列表

通过该技术, 200 个节点的 ProxySQL 集群中, 如果每个节点的监控时间间隔为 1000ms, 每个节点的进/出流量只需 50KB 的带宽

## 3. Re-configuration

由于每个 ProxySQL 节点都监控集群中的其它实例, 它们可以快速探测到某个实例的配置是否发生改变

如果某实例的配置发生改变, 其它实例会检查这个配置和自身的配置是否相同, 因为其它节点的配置可能和本节点的配置同时 (或在极短时间差范围) 发生了改变

如果比较结果不同:

- 如果本节点 `version=1`, 则从 `version>1` 的节点处找出 `epoch` 最大值的节点, 并从该节点拉取配置应用到本地;
- 如果本节点 `version>1`, 则开始对探测到的不同配置进行计数;
- 当探测到不同配置的次数超过 `cluster_name_diffs_before_sync` 字段值, 且 `cluster_name_diffs_before_sync` 字段值大于 `0` 时, 找出 `version>1` 且 `epoch` 值最大的节点, 并从该节点拉取配置禁用应用

同步配置的过程如下:

- 用于健康检查的连接, 也用来执行一系列类似于 `SELECT _list_of_columns FROM runtime_module` 的 `SELECT` 语句, 例如:

  ```sql
  SELECT `hostgroup_id`, `hostname`, `port`, `status`, `weight`, `compression`, `max_connections`, `max_replication_lag`, `use_ssl`, `max_latency_ms`, `comment`
  FROM `runtime_mysql_servers`;

  SELECT `writer_hostgroup`, `reader_hostgroup`, `comment`
  FROM `runtime_mysql_replication_hostgroups`;
  ```

- 删除本地配置, 例如:

  ```sql
  DELETE FROM `mysql_servers`;
  DELETE FROM `mysql_replication_hostgroups`;
  ```

## 4. Roadmap

以下是未来可能要实现的 Cluster 不完整特性列表. 这些特性目前都还未实现, 且实现后有可能会与此处描述的有所区别

- 支持 Master 选举: ProxySQL 内部将使用 Master 关键字替代 Leader;
- 只有 Master 节点是可写/可配置的;
- 实现类似于 MySQL 复制的功能: 从 Master 复制到 Slave. 这将允许实时推送配置内容, 而非现在使用的主动 Pull 机制;
- 实现类似于 MySQL 复制的功能: 从 Master 复制到候选 Master;
- 实现类似于 MySQL 复制的功能: 从候选 Master 复制到 Slave;
- 将候选 Master 定义为法定票数节点, Slave 不参与投票;

## 5. Q & A

**如果不同节点在同一时刻加载了不同配置会如何, 最后一个才生效吗?**

目前还未实现 Master 和 Master 选举的机制, 这意味着多个节点上可能会潜在地同时执行 Load 命令 (就像是多个 Master 一样), 每个实例都会基于时间戳来检测配置冲突, 然后再触发自动重新配置

如果所有节点在同一时刻加载的是相同的配置, 则不会出现问题

如果所有节点在不同时刻加载了不同的配置, 则最后一个配置生效

如果所有节点在同一时刻加载了不同配置, 这些不同配置会正常进行传播, 直到出现冲突, 然后回滚

庆幸的是, 每个 ProxySQL 节点都知道其它每个节点的校验和, 因此很容易监控并探测到不同的配置

**谁负责向所有节点写入配置**

目前, ProxySQL 集群使用拉取 (Pull) 机制, 因此当探测到节点自身需要重新配置时, 会从拥有最新配置的节点处拉取配置到本地并应用

**你想如何实现选举? Raft 协议吗**

关于选举正在实现计划中, 但应该不会采用 Raft 共识协议

ProxySQL 使用表来存储配置信息, 使用 MySQL 协议来执行对端健康检查, 配置信息的查询请求, 以及使用 MySQL 协议实现心跳等等. 所以对于 ProxySQL 来说, MySQL 协议本身相比 Raft 协议要更多才多艺

**某些原因下, 如果某个节点无法从远端抓取新的配置会发生什么**

配置更改是异步传播的, 因此, 某个 ProxySQL 节点可能暂时会无法获取新的配置, 例如网络问题. 但是, 当该实例探测到新的配置时, 它会自动去抓取新配置

**跨 DC 的 ProxySQL 集群是否实现? 最佳实践是怎样的? 每个 DC 一个 ProxySQL 集群吗**

ProxySQL 集群没有边界限制, 因此一个 ProxySQL 集群可以跨多个 DC, 一个 DC 内也可以有多个 ProxySQL 集群. 这依赖于实际应用场景

唯一的限制是, 每个 ProxySQL 实例只能属于单个 ProxySQL 集群

ProxySQL 集群没有名称, 为了确保 ProxySQL 实例不会加入到错误的集群中, 可以让每个 ProxySQL 集群采用不同的集群认证凭据

**如何引导启动一个 ProxySQL 集群**

只需让 `proxysql_servers` 表中多于一个节点即可

**ProxySQL 集群中的其它节点如何知道有新节点**

无法自动知道, 这是为了防止新节点破坏集群

一个新节点在加入集群时, 会立刻从集群中拉取配置, 但不会将自己作为可信任的配置源通告出去

要让其它节点知道有一个新的节点, 只需向这些节点的 `proxysql_servers` 中加入新的节点信息, 然后执行 `LOAD PROXYSQL SERVERS TO RUNTIME` 即可
