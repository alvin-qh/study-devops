# cat 命令

- [cat 命令](#cat-命令)
  - [1. 公共参数](#1-公共参数)
    - [1.1. 表格形态](#11-表格形态)
    - [1.2. JSON 形态](#12-json-形态)
    - [1.3. 帮助信息](#13-帮助信息)
    - [1.4. 命令一览](#14-命令一览)
  - [2. 常用 cat 命令](#2-常用-cat-命令)
    - [2.1. 显示索引状态](#21-显示索引状态)
    - [2.2. 显示分片状态](#22-显示分片状态)
    - [2.3. 集群健康检查](#23-集群健康检查)
    - [2.4. 主节点状态](#24-主节点状态)
    - [4.2. 所有节点状态](#42-所有节点状态)
    - [4.3. 节点属性](#43-节点属性)
    - [4.4. 节点恢复状态](#44-节点恢复状态)
    - [4.5. 节点线程池状态](#45-节点线程池状态)
    - [4.5. 插件状态](#45-插件状态)
    - [4.6. 存储区信息](#46-存储区信息)
    - [4.6. 快照信息](#46-快照信息)

有些数据（特别是 log，profile 或者 properties）不便于使用 json 展示，则可以使用 cat 接口对其进行格式化。

cat 格式化后的数据以 Table 格式呈现，可以设置其 Column，Sort 方式，更为直观

> 查看 [文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat.html)

## 1. 公共参数

### 1.1. 表格形态

- `h=a,b,c` 要显示的 a, b, c 列
- `bytes=b|mb|gb` 字节显示单位，默认为自动
- `s=a:asc,b:desc` 排序选项
- `v=true` 或 `v` 显示表头

### 1.2. JSON 形态

- `format=json` 以 json 形态显示结果
- `pretty=true` 或 `pretty` 对 json 进行格式化

### 1.3. 帮助信息

所有的 cat 命令均可以通过 `_cat/<command>/help` 获取帮助信息，包括可使用的字段和字段的描述

### 1.4. 命令一览

- [`cat aliases`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-alias.html) 返回一个集群下索引的别名
- [`cat allocation`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-allocation.html) 返回每个数据节点的分片情况和磁盘使用情况快照
- [`cat anomaly detectors`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-anomaly-detectors.html) 返回异常 jobs 的配置和信息
- [`cat count`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-count.html) 返回一个数据流，索引或者集群内的文档数量
- [`cat data frame analytics`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-dfanalytics.html) 返回关于数据帧分析任务的配置和信息
- [`cat datafeeds`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-datafeeds.html) 返回数据处理任务的配置和使用信息
- [`cat fielddata`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-fielddata.html) 返回每个集群节点上用于字段数据缓存的堆内存总数
- [`cat health`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-health.html) 返回集群的监控状态信息，功能与 [集群健康 API](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-health.html) 类似
- [`cat indices`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-indices.html) 返回集群索引（包括数据流的支持指数）的高阶信息
- [`cat master`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-master.html) 返回主节点信息，包括 ID，IP 和名称
- [`cat nodeattrs`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodeattrs.html) 返回自定义节点属性信息
- [`cat nodes`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html) 返回集群节点信息
- [`cat pending tasks`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-pending-tasks.html) 返回尚未执行的集群级别变更处理，类似 [集群等待任务 API](https://www.elastic.co/guide/en/elasticsearch/reference/current/cluster-pending.html)
- [`cat plugins`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-plugins.html) 获取每个集群节点上安装的插件列表
- [`cat recovery`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-recovery.html) 返回每个索引数据分片正在进行（或已完结的）恢复操作信息，类似于 [索引恢复 API](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-recovery.html)
- [`cat repositories`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-repositories.html) 返回集群的 [快照存储](https://www.elastic.co/guide/en/elasticsearch/reference/current/snapshots-repositories.html) 信息
- [`cat segments`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-segments.html) 返回有关索引分片中 Lucene 段的低级信息，类似于 [索引段 API](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-segments.html)
- [`cat shards`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-shards.html) 返回索引的数据分片信息
- [`cat snapshots`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-snapshots.html)  返回一个或多个数据仓库中的数据快照信息
- [`cat task management`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-tasks.html) 返回集群中正在执行的任务信息，类似于 [任务 API](https://www.elastic.co/guide/en/elasticsearch/reference/current/tasks.html)
- [`cat templates`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-templates.html) 返回集群中存储的“索引模板”信息
- [`cat thread pool`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-thread-pool.html) 返回每个集群节点的线程池信息，包括内置线程池和用户自定义线程池
- [`cat trained model`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-trained-model.html) 返回推理训练模型的配置和使用信息
- [`cat transforms`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-transforms.html) 返回转换器的配置和使用信息

## 2. 常用 cat 命令

### 2.1. 显示索引状态

查看所有索引状态

[`GET /_cat/indices?v&s=docs.count:desc`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-indices.html)

查看指定索引状态

[`GET /_cat/indices/person?v&s=docs.count:desc`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-indices.html)

返回信息包括

- `health` 健康状态，值为 `red`, `green` 和 `yellow`
- `status` 索引状态，值为 `open` 和 `close`
- `index` 索引名称
- `uuid` 索引唯一 ID
- `pri` 主分片数
- `rep` 副本数
- `docs.count` 文档总数
- `docs.deleted` 已删除文档数
- `store.size` 存储大小
- `pri.store.size` 主分片存储大小

### 2.2. 显示分片状态

查看全部索引的分片

[`GET /_cat/shards?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-shards.html)

查看指定索引的分片

[`GET /_cat/shards/person?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-shards.html)

返回信息包括

- `index` 索引名称
- `shard` 分片编号
- `prirep` 存储类型，主分片或是副本
- `state` 状态，`INITIALIZING`, `RELOCATING`, `STARTED` 和 `UNASSIGNED`
- `docs` 分片内的文档数
- `store` 存储占用空间
- `ip` 节点 IP 地址
- `node` 节点名称

### 2.3. 集群健康检查

[`GET /_cat/health?v&ts=false`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-health.html)

返回信息包括

- `epoch` Unix 时间戳
- `timestamp` 时间戳
- `cluster` 集群名称
- `status` 集群状态，为 `red`, `green` 和 `yellow`
- `node.total` 集群节点总数
- `node.data` 节点数据集总数
- `shards` 包含分片总数
- `pri` 主数据分片总数
- `relo` 重新加载的分片总数
- `init` 初始化中的分片总数
- `unassign` 未分配的分片总数
- `pending_tasks` 挂起的任务总数
- `max_task_wait_time` 总共任务等待时间
- `active_shards_percent` 活跃的分片百分比

### 2.4. 主节点状态

[`GET /_cat/master?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html)

返回信息包括

- `id` 节点 id
- `host` 节点 ip host
- `ip` 节点 ip 地址
- `node` 节点名称

### 4.2. 所有节点状态

[`GET /_cat/nodes?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodes.html)

返回信息包括

- `ip` 节点 ip 地址
- `heap.percent` 对内存使用百分比
- `ram.percent` 物理内存使用百分比
- `cpu` CPU 使用百分比
- `load_1m` 1 分钟内负责情况
- `load_5m` 5 分钟内负责情况
- `load_15m` 15 分钟内负责情况
- `node.role` 节点角色列表，角色包括：
  - `c` (cold node)
  - `d` (data node)
  - `f` (frozen node)
  - `h` (hot node)
  - `i` (ingest node)
  - `l` (machine learning node)
  - `m` (master-eligible node)
  - `r` (remote cluster client node)
  - `s` (content node)
  - `t` (transform node)
  - `v` (voting-only node)
  - `w` (warm node)
  - `-` (coordinating node only)
- `master` 是否选举的主节点，`*` 被选举的主节点；`-` 未被选举的主节点

### 4.3. 节点属性

[`GET /_cat/nodeattrs?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-nodeattrs.html)

返回信息包括

- `node` 节点名称
- `host` 节点主机名
- `ip` 节点 ip 地址
- `attr` 节点属性名
- `value` 节点属性值

### 4.4. 节点恢复状态

查看全部索引的恢复状态

[`GET _cat/recovery?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-recovery.html)

查看指定索引的恢复状态

[`GET _cat/recovery/<index>?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-recovery.html)

返回的结果主要体现恢复的进度

### 4.5. 节点线程池状态

查看全部线程池状态

[`GET /_cat/thread_pool`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-thread-pool.html)

查看指定线程池状态（例如查看搜索线程池状态）

[`GET /_cat/thread_pool/search`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-thread-pool.html)

返回信息包括

- `node_name` 节点名称
- `name` 线程池名称
- `active` 活动线程数
- `query` 队列任务数
- `rejected` 被拒绝执行的任务数

### 4.5. 插件状态

如果安装了插件，则可以查看插件的名称和版本号

[`GET _cat/plugins?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-plugins.html)

### 4.6. 存储区信息

存储区用于存储索引的快照，可以查看所有存储区信息

[`GET _cat/repositories?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-repositories.html)

### 4.6. 快照信息

查看所有快照信息

[`GET _cat/snapshots?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-snapshots.html)

查看指定存储区下的快照信息

[`GET _cat/snapshots/<repo name>?v`](https://www.elastic.co/guide/en/elasticsearch/reference/current/cat-snapshots.html)

结果返回每个快照所在的存储区，创建时间和备份情况
