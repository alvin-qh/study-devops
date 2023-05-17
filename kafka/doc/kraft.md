# KRaft 集群

- [KRaft 集群](#kraft-集群)
  - [1. KRaft 和控制器](#1-kraft-和控制器)
    - [1.1. Zookeeper 的问题](#11-zookeeper-的问题)
    - [1.2. KRaft 的作用](#12-kraft-的作用)
  - [2. 配置 KRaft 集群](#2-配置-kraft-集群)
    - [2.1. 控制器集群配置](#21-控制器集群配置)
    - [2.2. Broker 集群配置](#22-broker-集群配置)
    - [2.3. 混合模式集群配置](#23-混合模式集群配置)
  - [3. 启动集群](#3-启动集群)
    - [3.1. 格式化日志存储](#31-格式化日志存储)
    - [3.2. 启动服务节点](#32-启动服务节点)
  - [4. 测试集群](#4-测试集群)
    - [4.1. 查看集群元数据日志文件](#41-查看集群元数据日志文件)
    - [4.2. 测试集群](#42-测试集群)

## 1. KRaft 和控制器

### 1.1. Zookeeper 的问题

在 3.0 版本以前, Kafka 一直使用 Zookeeper 来作为控制器的协同服务, 它起到了两个重要作用:

- 用于选举控制器;
- 用于保存集群元数据 (Broker, 配置, 主题, 分区和副本);

Zookeeper 作为协同服务导致了一些长期存在的问题:

- 集群元数据存储在 Zookeeper 中, 是以异步方式发送到 Broker, 同时, Zookeeper 接收元数据更新也是异步方式, 这就有几率导致 Broker, 控制器 和 Zookeeper 直接的数据不一致;
- 控制器在重启时需要从 Zookeeper 读取所有 Broker 和分区的元数据, 再将这些数据发送给所有 Broker, 这方面 Zookeeper 一直是性能瓶颈, 随着 Broker 数量不断增加, 重启控制器的速度会越来越慢;
- 元数据所有权的架构设计不够好, 操作元数据的途径不统一, 包括: 通过控制器, 通过 Broker 和直接修改 Zookeeper 三种, 关系比较混乱;
- 需要同时维护 Zookeeper 集群和 Kafka 集群, 为运维增加了工作量;

所以, 在 Kafka 3.0 版本之后, 增加了 KRaft 控制器, 自此 Kafka 可以脱离 Zookeeper 独立运行

### 1.2. KRaft 的作用

新的控制器节点形成了一个 KRaft 仲裁, 管理着元数据事件日志, 该日志包含了集群元数据的每一个变更, 即将之前通过 Zookeeper 保存的元数据 (例如 Broker, 配置, 主题, ISR, 分区和副本等) 都将保存在这个日志中

通过控制器的 KRaft 算法, 可以在不依赖 Zookeeper 的情况下进行首领选举. 首领节点为 **主控制器**, 负责处理所有来自 Broker 的 RPC 调用

**从控制器** 会时刻从主控制器同步数据, 作为主控制器的热备, 当主控制器故障时, 会将控制权转移到某个从控制器, 并将该从控制器升级为主控制器

其它 Broker 会通过新加入的 MetadataFetch API 从主控制器获取集群状态的更新 (Pull 模式而非 Push 模式), Broker 会跟踪元数据日志的数据偏移量, 所以每次只需要获取最新数据即可 (类似于消费者从 Broker 通过轮询获取消息), 而非 Zookeeper 那样必须获取完整的数据, 这样就解决控制器重启时可以快速向集群发布所有 Broker 和分区元数据

所以 KRaft 控制器已经完成了 Zookeeper 的所有工作, 包括控制器节点的选举和集群元数据的存储和分发, 而且无需引入任何三方服务, 而且使用的方式是 Kafka 已经非常成熟的日志和消息模式

除过 KRaft 替换 Zookeeper 外, Kafka 集群的其它方面和之前基本保持一致, 参考 [Kafka 集群](./cluster.md) 章节

## 2. 配置 KRaft 集群

通过 KRaft 配置集群有两种模式:

- 独立控制器模式, 即将一部分 Kafka 节点单独用作"控制器", 不参与消息处理, 另一部分服务器作为 Broker 进行消息处理;
- 混合模式, 即所有 Kafka 节点都身兼两职, 即作为控制器节点, 同时也作为 Broker 参与消息处理;

前者用于服务器资源充裕的情况, 单独的控制器集群可以有效的降低控制节点的延迟, 且控制节点的故障不会影响到消息订阅分发的的过程, 但无论哪种模式, 都是通过 `config/server.properties` 配置文件进行的

集群配置的核心就是每个服务节点的网络配置, 包括 **控制器集群配置** 和 **节点监听器配置** 两种配置模式

### 2.1. 控制器集群配置

1. `process.roles`, 用于指定当前节点的身份, 可以为 `broker` 或 `controller`, 对于独立控制器集群的模式, 则控制器节点填 `controller`, Broker 节点填 `broker`;

2. `node.id`, 用于指定控制器的节点 ID, 用于标识控制器节点, 且在集群内不能重复;

3. `controller.quorum.voters`, 用于指定控制器集群的节点集合, 格式为 `<node.id>@<host:port>,...`, 即指定控制器集群内所有节点的 ID, 主机名和端口号:

   - 对于控制器节点, 通过该配置可以知道其它控制器节点的信息;
   - 对于 Broker 节点, 则可知道控制器集群的各个节点的信息;

4. `controller.listener.names`, 用于指定控制器使用的监听器名称, 需要从 `listener.security.protocol.map` 配置中选择一个;

5. `listeners`, 用于指定侦听器, 控制器节点必须包括具备用于监听其它控制器通信的监听器;

整个控制器集群的主要配置项如下 (假设配置 `3` 个节点的控制器集群, 主机名分别为 `kfc01`, `kfc02` 和 `kfc03`):

```ini
process.roles=controller
node.id=1 # 或 2, 3
controller.quorum.voters=1@kfc01:9093,2@kfc02:9093,3@kfc03:9093
controller.listener.names=CONTROLLER
listeners=CONTROLLER://:9093
```

> `process.roles` 是 KRaft 模式的关键, 如果不配置此项, 则 Kafka 会认为使用 Zookeeper 作为控制器协同服务

### 2.2. Broker 集群配置

KRaft 模式下, Broker 的配置和 Zookeeper 模式下基本一致, 不同之处包括:

1. `broker.id`, 表示 Broker 节点的标识, 必须在集群中唯一, 在 KRaft 模式下, 这个配置不是必须的;

2. `controller.quorum.voters`, 表示控制器集群中各个节点, 参考 [控制器集群配置](#21-控制器集群配置) 章节中 `controller.quorum.voters` 配置项说明;

3. `inter.broker.listener.name`, 表示 Broker 集群内部互相通信的监听器名称;

4. `advertised.listeners`, 如果需要从外网访问 Broker 节点, 则需要通过该配置设置每个节点的外网访问;

整个 Broker 集群的主要配置项如下 (假设配置 `3` 个节点的 Broker 集群, 主机名分别为 `kfb01`, `kfb02` 和 `kfb03`):

```ini
process.roles=broker
broker.id=1 # 或 2, 3
controller.quorum.voters=1@kfc01:9093,2@kfc02:9093,3@kfc03:9093
inter.broker.listener.name=PLAINTEXT
listeners=PLAINTEXT://:9092
advertised.listeners=PLAINTEXT://:9092
```

### 2.3. 混合模式集群配置

混合模式即一个节点同时具备"控制器"和 "Broker" 的功能, 这种模式在服务器资源紧张的情况下很有帮助, 但每个节点都会同时处理集群元数据以及业务消息数据, 对整个系统的高可用性以及数据吞吐量会有影响

整个混合模式节点配置如下 (假设一共有 `3` 个节点, 分别为 `kf01`, `kf02` 和 `kf03`):

```ini
process.roles=controller,broker
node.id=1   # 或 2, 3
broker.id=1 # 或 2, 3
controller.quorum.voters=1@kf01:9093,2@kf02:9093,3@kf03:9093
controller.listener.names=CONTROLLER
inter.broker.listener.name=PLAINTEXT
listeners=PLAINTEXT://:9092,CONTROLLER://:9093
advertised.listeners=PLAINTEXT://:9092
```

## 3. 启动集群

### 3.1. 格式化日志存储

每个节点都通过 `log.dirs` 配置指定了日志分片存储的路径, 如果要使用 KRaft 模式 (或从 Zookeeper 模式切换到 KRaft 模式), 则需要先对日志存储进行格式化, 步骤如下:

1. 生成集群唯一 ID, 作为整个集群共享的一个 ID 值:

   ```bash
   kafka-storage.sh random-uuid
   ```

   该命令会返回一个唯一 ID (假设为 `BqeKWXR-QCGwzec3IpAmFQ`)

2. 格式化日志目录

   通过上一步返回的 ID 值, 在每个节点上执行如下命令对日志目录进行格式化

   ```bash
   kafka-storage.sh format -t BqeKWXR-QCGwzec3IpAmFQ -c ./config/server.properties
   ```

操作完毕后, 可以到日志目录中查看 `meta.properties` 文件, 查看格式化结果

```bash
cat meta.properties

#
# <Created time>
node.id=1
version=1
cluster.id=BqeKWXR-QCGwzec3IpAmFQ
```

### 3.2. 启动服务节点

完成日志目录格式化后, 即可启动各个服务节点:

- 对于独立控制器模式, 需要先启动所有控制器节点, 再启动剩余的 Broker 节点;
- 对于混合模式, 则直接启动所有的节点即可, 集群内部会自动进行协调;

```bash
kafka-server-start.sh ./config/server.properties
```

## 4. 测试集群

假设以混合模式搭建集群, 三个节点分别为 `kf01`, `kf02` 和 `kf03`, 且:

- 控制器集群节点为 `1@kf01:9093,2@kf02:9093,3@kf03:9093`;
- Broker 集群节点为 `kf01:9092,kf02:9092,kf03:9092`;

按 [混合模式集群配置](#23-混合模式集群配置) 配置各个节点, 格式化日志目录并启动集群

> 如果集群中没有 DNS 服务, 则需要在各个节点的 `/etc/hosts` 文件中增加每个节点 IP 和主机名 (域名) 的对应关系

### 4.1. 查看集群元数据日志文件

KRaft 模式下, 原先保存在 Zookeeper 上的数据会全部转移到一个内部的 `@metadata` 主题上, 可以通过查看这个主题的日志分片来确认集群状态

登录控制集群的一个节点, 进入日志存储目录 (由 `log.dirs` 配置指定), 执行如下命令, 进入元数据交互会话:

```bash
kafka-metadata-shell.sh --snapshot ./__cluster_metadata-0/00000000000000000000.log

>> ls /
brokers  features  local  metadataQuorum

>> ls brokers/
1  2  3

>> cat metadataQuorum/offset
4800
```

可以看到, 整个集群具有 `3` 个 Broker 节点

### 4.2. 测试集群

1. 尝试创建一个主题

   尝试创建一个主题, 包括 `3` 个分区, 每分区 `2` 个副本

   ```bash
   kafka-topics.sh --create \
     --partitions 3 \
     --replication-factor 2 \
     --topic test-topic \
     --if-not-exists \
     --bootstrap-server localhost:9092,kf02:9020,kf03:9020
   ```

2. 查看创建的主题情况

   ```bash
   kafka-topics.sh --describe \
       --topic test-topic \
       --bootstrap-server localhost:9092,kf02:9020,kf03:9020
   ```

3. 向该主题发送一条消息

   ```bash
   kafka-console-producer.sh \
       --topic test-topic \
       --property 'parse.key=true' \
       --property 'key.separator=:' \
       --bootstrap-server localhost:9092,kf02:9020,kf03:9020

   >1:Hello
   ```

   - `--property 'parse.key=true'` 和 `--property 'key.separator=:'` 分别表示消息需要 Key, 且 Key 和 Value 使用 `:` 符号分隔;

4. 从指定主题接收消息

   ```bash
   kafka-console-consumer.sh \
       --topic test-topic \
       --group g1 \
       --from-beginning \
       --formatter kafka.tools.DefaultMessageFormatter \
       --property 'print.timestamp=true' \
       --property 'print.key=true' \
       --property 'print.value=true' \
       --bootstrap-server localhost:9092,kf02:9020,kf03:9020
   ```

   - `--from-beginning` 表示从上次读取提交的 offset 位置开始继续读取; 不设置此项则从日志分片的最新位置开始读取 (即只读取之后写入的新消息);
