# ProxySQL 线程及连接池设置

- [ProxySQL 线程及连接池设置](#proxysql-线程及连接池设置)
  - [1. ProxySQL 线程](#1-proxysql-线程)
    - [1.1. 主线程](#11-主线程)
    - [1.2. Admin Thread](#12-admin-thread)
    - [1.3. MySQL Workers](#13-mysql-workers)
    - [1.4. MySQL Auxiliary Threads](#14-mysql-auxiliary-threads)
    - [1.5. Monitor 模块相关的线程](#15-monitor-模块相关的线程)
    - [1.6. Query Cache Purge Thread](#16-query-cache-purge-thread)
    - [1.7. 其它线程](#17-其它线程)
  - [2. ProxySQL 线程池](#2-proxysql-线程池)
  - [3. ProxySQL 连接池](#3-proxysql-连接池)
    - [3.1. 快速连接到后端 MySQL 节点](#31-快速连接到后端-mysql-节点)
      - [3.1.1. 相关变量](#311-相关变量)
    - [3.2. Monitor 模块的连接池](#32-monitor-模块的连接池)

## 1. ProxySQL 线程

ProxySQL 由多个模块组成, 是一个多线程的 daemon 类程序, 每个模块都有一个或多个线程去执行任务

例如以下是刚启动 ProxySQL 时的进程情况: 一个 Main 进程, 一个主线程以及 21 个子线程

```bash
pstree | grep proxy
  |-proxysql---proxysql---21*[{proxysql}]
```

### 1.1. 主线程

这其实是一个进程, 该进程只负责引导和启动核心模块以及启动其它核心线程

### 1.2. Admin Thread

该线程负责以下几件事:

- 初始化并引导启动 Admin 接口
- 从磁盘数据库或配置文件中加载配置, 为 ProxySQL 的运行提供环境
- 启动一个监听者, 让其负责监听并接收 Admin 接口的新连接, 并为每个该类型连接创建一个新线程

所以每连接一次 Admin 接口就会新生成一个线程, 每次退出 Admin 接口结束一个线程

### 1.3. MySQL Workers

`mysql-threads` 线程负责处理 SQL 语句转发, 包括所有来自客户端的连接以及所有到后端服务器节点的连接, 即: 用少量线程处理任意多数量的连接

MySQL Workers 线程在相同的端口上进行监听, 当新客户端发起连接请求, 其中一个 MySQL Worker 线程将成功接受该连接, 并创建一个 MySQL 会话 (Session): 客户端和会话绑定在该 Worker 线程上, 直到连接断开. 换句话说, 在断开连接之前, 某客户端的所有连接总是被同一个 Worker 线程处理

默认情况下, MySQL Worker 的线程数量为 `4`

```sql
SELECT @@mysql-threads;

+-----------------+
| @@mysql-threads |
+-----------------+
| 4               |
+-----------------+
```

配置值 `mysql-threads` 修改后必须重启 ProxySQL 才能生效, 这是少有的需要重启生效的 ProxySQL 配置之一 (另一个是 `mysql-stacksize`). 例如, 修改为 `8` 个 Worker 线程

```sql
SET mysql-threads = 8;

-- 将修改结果持久化到 DISK 层
SAVE MYSQL VARIABLES TO DISK;
```

重启前查看设置结果

```sql
SELECT *
FROM `runtime_global_variables`
WHERE `variable_name` = 'mysql-threads';

+---------------+----------------+
| variable_name | variable_value |
+---------------+----------------+
| mysql-threads | 4              |
+---------------+----------------+
```

重启后查看设置结果

```sql
select * from runtime_global_variables
 where variable_name='mysql-threads';
+---------------+----------------+
| variable_name | variable_value |
+---------------+----------------+
| mysql-threads | 8              |
+---------------+----------------+
```

### 1.4. MySQL Auxiliary Threads

这类线程即就是空闲线程 (Idle Threads)

如果 ProxySQL 使用 `--idle-threads` 选项启动, 每个 Worker 线程都会伴随启动一个 Auxiliary 线程

每个 Worker 线程以及它的 Auxiliary 线程一起工作:

- Worker 线程处理活动的连接, 并将所有的空闲连接派遣到 Auxiliary 线程上;
- Auxiliary 线程只要等待到了发生在空闲连接上的一个事件 (或超时), 就会将连接还给 Worker 线程

当活动连接数量远少于空闲连接数量时, 强烈建议使用 `--idle-threads` 选型, 这使得 ProxySQL 可以处理数十万个连接

### 1.5. Monitor 模块相关的线程

Monitor 模块有自己的线程管理系统以及自己的线程池

正常情况下, Monitor 模块有以下几个线程:

- 一个 Master 线程 (主线程), 负责生成和协调其它 Monitor 相关的线程
- 一个负责监控 Connection 的线程
- 一个负责监控 Ping 的线程
- 一个负责监控 Read Only 的线程
- 一个负责监控 Replication Lag 的线程
- 一个负责提供 Monitor Worker 的线程池, 上面每个监控线程都是任务的生产者, Worker 线程从任务队列消费一个任务并执行该任务. 该线程池初始时默认为 Mysql Thread 的两倍

线程池负责执行所有的检查任务, 并通过以上各调度线程来监控调度情况. 线程池会基于监控队列中待检查的数量多少而自动增长和收缩

基于检查的结果, 会使用相同的线程对结果进行处理, 例如避开一个节点或重新配置一个主机组等

### 1.6. Query Cache Purge Thread

该线程是需要时才生成的, 它扮演的是垃圾收集器, 回收查询缓存

通过垃圾收集器, 可保证在客户端等待响应的过程中**绝不会回收缓存**

### 1.7. 其它线程

在 ProxySQL 运行过程中, 偶尔会派生临时线程, 这些临时线程是为了向后端发送 `KILL` 语句, 以便杀掉后端服务器上对查询长时间无响应的查询线程

此外, `ilbmariadbclient` 库还会使用一些后台线程, 这些线程是为了和后端 MySQL Server 进行一些特定的异步交互任务

还有一些模块, 例如内置的 HTTP Server, 正处于实验阶段的 Cluster, ClickHouse Server, SQLite3 Server, 如果启用了这些功能, 则会按需创建它们对应的线程

## 2. ProxySQL 线程池

在 ProxySQL 中, 有两个地方使用了线程池:

- 快速建立和后端 MySQL 的连接, ProxySQL 为了尽快和后端My SQL 建立新的 TCP 连接, 使用了一个线程池来等待 `accept()` 函数返回新连接
- Monitor 模块, 为了尽快执行各监控线程生产的监控任务, Monitor 模块提供了一个 Monitor Worker 线程池, 可以快速从任务队列中消费任务

需要注意的是, 正常情况下 MySQL Worker 线程是最繁忙, 最消耗CPU资源的部分, 但在一个极其繁忙的环境下, Monitor 模块需要监控的连接数过多, 消耗的 CPU 也是不可忽视的

## 3. ProxySQL 连接池

ProxySQL 同样有两个连接池, 和线程池部分是对应的

### 3.1. 快速连接到后端 MySQL 节点

线程池是为了快速和后端建立新的 TCP 连接, 而这里的连接池是为了快速和后端建立连接

ProxySQL 使用一个连接池来存放一定数量的"之前已经和某后端建立连接, 但当前是空闲连接"的连接. 当需要向这些连接对应的后端发送新的数据包时, 可以快速地取回连接, 因为这些连接早已经被打开

当应用程序发送了一个 MySQL 请求给 ProxySQL 时, ProxySQL 首先解析要路由到哪个后端, 如果连接池中已经有和该后端的连接, 将重用该连接, 否则将创建一个新的和后端的连接

当处理完客户端的请求后, 连接会还回主机组管理器 (HostGroup Manager). 如果主机组管理器判断了该连接是可以被安全共享的, 且连接池未满, 则该连接会放进连接池

放进连接池的连接都是空闲连接, 正在使用的连接是不可能进入连接池的. ProxySQL 会定期发送 `ping` 消息来维持空闲连接. 如果某连接从上一次 `ping` 之后, 如果还没有被使用, 则该连接被定义为空闲连接. 对于空闲连接 `ping` 的时间间隔由变量 `mysql-ping_interval_server_msec` 控制

但是不是所有的未使用的连接都会放进连接池. 该变量用来控制某后端的空闲连接和最大总连接数的百分比. 对于每个 `hostgroup/backend`, 主机组管理器只会保持连接池中的最大连接数为 `mysql-free_connections_pct * mysql_servers.max_connections / 100`. 池中的每个空闲连接都通过间断性的 `ping` 来维持它的打开状态

当一个连接放回连接池时, 会计算这个连接之后还能处理多少个语句, 当处理的语句数量达到该阈值后, 将关闭该连接 (`v1.4.3` 之前) 或者重置该连接 (`v1.4.4` 之后)

#### 3.1.1. 相关变量

- `mysql-ping_interval_server_msec`, ProxySQL 为了维持和后端的空闲连接, 每隔一段时间发送一次 `ping`, 该变量指定发起ping的时间间隔, 默认值为 `10000ms`

- `mysql-ping_timeout_server`, ProxySQL 为了维持和后端的空闲连接, 每隔一段时间发送一次 `ping`. 该变量指定 `ping` 得到回复的超时时间, 默认值为 `200ms`

- `mysql-connection_max_age_ms`, 当该变量设置的值大于 `0` 时 (单位 `ms`), 如果某个空闲连接 (当前没有任何会话使用) 的空闲时长超过了这里设置的值, 则这个连接会关闭. 默认值为 `0`, 表示不会因为存活时间而关闭空闲连接

### 3.2. Monitor 模块的连接池

Monitor 有它自己的连接池. 当连接池中空闲连接的空闲时长达到了 `3 * mysql-monitor_ping_interval` (单位 `ms`) 后, 该空闲连接将自动被 Purge

变量 `mysql-monitor_ping_interval` 的默认值为 `1min` (`60000ms`), 所以 Monitor 连接池中的空闲连接默认最长维持`3min`
