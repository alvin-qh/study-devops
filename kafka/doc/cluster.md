# Kafka 传统集群配置

- [Kafka 传统集群配置](#kafka-传统集群配置)
  - [1. 配置 Zookeeper 集群](#1-配置-zookeeper-集群)
    - [1.1. 安装 Zookeeper](#11-安装-zookeeper)
    - [1.2. 配置 Zookeeper](#12-配置-zookeeper)
  - [2. 配置 Kafka](#2-配置-kafka)
    - [2.1. 配置 Zookeeper 连接](#21-配置-zookeeper-连接)
    - [2.2. 配置 Kafka 监听器](#22-配置-kafka-监听器)
    - [2.3. 启动 Kafka](#23-启动-kafka)
  - [3. 集群测试](#3-集群测试)
    - [3.1. 测试集群](#31-测试集群)
    - [3.2. 查看集群元数据](#32-查看集群元数据)

早期的 Kafka 需要配合 Zookeeper 集群一起使用, Zookeeper 存储着 Kafka 的控制器数据以及元数据, 本章节主要来介绍如何基于 Zookeeper 集群配置 Kafka 集群

本章节主要介绍 Kafka 集群的配置, 不会包含过多的 Zookeeper 相关内容

## 1. 配置 Zookeeper 集群

Zookeeper 集群需要最少 `3` 个节点 (即要么是单实例, 要么至少要启动 `3` 个节点), 偶数节点的 Zookeeper 集群容易导致"脑裂"现象

### 1.1. 安装 Zookeeper

和 Kafka 一样, Zookeeper 也是通过 Java 编写, 所以只需主机上安装 JDK (或 JRE), 之后从 [官网](https://zookeeper.apache.org/releases.html) 下载软件包, 解压后即可使用

Zookeeper 的配置项位于 `<zookeeper>/config/zoo.cfg` 文件中, 官方提供了 `<zookeeper>/config/zoo_sample.cfg` 参考示例

### 1.2. 配置 Zookeeper

在本例中, Zookeeper 主要要配置如下配置项:

1. `dataDir`, 用于指定 Zookeeper 数据文件的存储路径;

2. `dataLogDir`, 用于指定 Zookeeper 数据日志文件的存储路径;

3. `autopurge.snapRetainCount`, 用于指定 Zookeeper 的副本存储数量, 本例中启动 `3` 个 Zookeeper 节点, 所以这里设置为 `3`;

4. `server.<n>`, 用于设置集群中的服务器地址和监听端口号, 本例设置 `3` 个节点如下:

   ```ini
   server.1=zk01:2888:3888;2181
   server.2=zk02:2888:3888;2181
   server.3=zk03:2888:3888;2181
   ```

   其中:

   - `n` 为每个节点 `myid` 设置, 即节点 ID, 可以为 `1~255` 之间的任意整数, 但不能重复, 下面会详细说明;
   - 每个节点需配置 3 个端口号, 包括: 客户端监听端口号 (默认 `2888`); 集群内部连接监听端口号 (默认 `3888`); 集群管理监听端口号 (默认 `2181`)

5. `4lw.commands.whitelist`, 用于设置远程管理命令白名单, 只有名单上的命令可以通过 `2181` 端口进行远程调用

   所谓 `4lw` 也称为"四字命令", 即所有的命令均有 4 个字母组成, 完整的命令列表参考 [官方说明](https://zookeeper.apache.org/doc/current/zookeeperAdmin.html#sc_4lw)

   本例中使用 `stat`, `mntr` 以及 `ruok` 这四个命令, 主要是供 `zookeeper_exporter` 监控使用

修改完配置文件后, 需要在 Zookeeper 的数据存储目录 (由 `dataDir` 配置指定) 下创建一个 `myid` 文件, 文件内容仅包含一个数字, 该数字要和 `server.<n>` 配置项的 `n` 一一对应, 所以每个节点都要具备该文件

完成如上配置后, 即可启动 Zookeeper 集群, 在每个节点上执行

```bash
<zookeeper>/bin/zkServer.sh start
```

## 2. 配置 Kafka

准备好 Zookeeper 后, 即可进一步配置 Kafka 集群, 由于本例中 Kafka 也有三个节点, 所以可以在三台主机上各部署一个 Zookeeper 节点和一个 Kafka 节点

Kafka 的集群配置需要通过 `config/server.properties` 配置文件进行

注意, 在新版的 Kafka 中, 如果要配合 Zookeeper 集群使用, 则一定不能配置 `process.roles` 配置项, 否则会启动 KRaft 模式, 从而不去连接 Zookeeper

### 2.1. 配置 Zookeeper 连接

`zookeeper.connect` 配置项用来设置 Kafka 到 Zookeeper 的连接, 为了保证高可用性, 该配置项会设置两个以上的 Zookeeper 节点地址, 本例中配置如下:

```ini
zookeeper.connect=zk01:2181,zk02:2181,zk03:2181/kafka
```

表示连接到 `3` 个 Zookeeper 节点, 均为 `2181` 端口

最后一个连接地址指定了一个 `chroot` 路径, 即 `zk03:2181/kafka` (注意, `chroot` 只能加载最后一个地址上), 表示 Kafka 会将元数据写入 Zookeeper 的 `/kafka` 路径下, 这样可以隔离 Kafka 数据和同时使用 Zookeeper 的其它应用的数据

### 2.2. 配置 Kafka 监听器

Kafka 通过监听器对其它 Broker 和客户端 (生产者, 消费者) 的连接进行监听, 配置监听器需要设置如下配置项:

1. `broker.id`, 即每个 Broker 的唯一 ID, 每个 Kafka 节点都要配置且不能重复;

2. `listener.security.protocol.map`, 用于设置监听器以及监听器和网络协议的对应关系, 例如:

   ```ini
   listener.security.protocol.map=PLAINTEXT:PLAINTEXT,SSL:SSL,SASL_PLAINTEXT:SASL_PLAINTEXT,SASL_SSL:SASL_SSL
   ```

   表示配置了 `PLAINTEXT`, `SSL`, `SASL_PLAINTEXT` 和 `SASL_SSL` 这四个监听器, 格式为 `<监听器名称>:<网络协议`

   监听器的名称可以任意, 但必须对应到 `PLAINTEXT`, `SSL`, `SASL_PLAINTEXT` 和 `SASL_SSL` 这四个协议上

   本例中为了简单, 为每个网络协议定义了同名的监听器

3. `listeners`, 用于指定当前节点的监听地址和端口号, 格式为 `<监听器名称>://<监听地址>:<端口号>`, 例如:

   ```ini
   listeners=PLAINTEXT://:9092
   ```

   监听地址可以有多个, 用 `,` 隔开即可, 但要注意, 每个监听器名称只能设置一个监听地址, 且监听器必须在 `listener.security.protocol.map` 配置项中定义

### 2.3. 启动 Kafka

完成上述配置后, 即可在各个节点上启动 Kafka 实例, 即:

```bash
kafka-server-start.sh ./config/server.properties
```

## 3. 集群测试

为了明确起见, 假设 Zookeeper 和 Kafka 均部署在单独的主机上, 其中: Zookeeper 包括 3 个节点, 分别为 `zk01`, `zk02`, `zk03`; Kafka 包含 3 个节点分别为 `kf01`, `kf02` 和 `kf03`

按照上述方法对各节点进行配置并启动

> 如果集群中没有 DNS 服务, 则需要在各个节点的 `/etc/hosts` 文件中增加每个节点 IP 和主机名 (域名) 的对应关系

### 3.1. 测试集群

1. 尝试创建一个主题

   尝试创建一个主题, 包括 `3` 个分区, 每分区 `2` 个副本

   ```bash
   kafka-topics.sh --create \
     --partitions 3 \
     --replication-factor 2 \
     --topic test-topic \
     --if-not-exists \
     --bootstrap-server kf01:9092,kf02:9020,kf03:9020
   ```

2. 查看创建的主题情况

   ```bash
   kafka-topics.sh --describe \
       --topic test-topic \
       --bootstrap-server kf01:9092,kf02:9020,kf03:9020
   ```

3. 向该主题发送一条消息

   ```bash
   kafka-console-producer.sh \
       --topic test-topic \
       --property 'parse.key=true' \
       --property 'key.separator=:' \
       --bootstrap-server kf01:9092,kf02:9020,kf03:9020

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
       --bootstrap-server kf01:9092,kf02:9020,kf03:9020
   ```

   - `--from-beginning` 表示从上次读取提交的 offset 位置开始继续读取; 不设置此项则从日志分片的最新位置开始读取 (即只读取之后写入的新消息);

### 3.2. 查看集群元数据

本例中, 集群的元数据存在于 Zookeeper 中, 所以需要到 Zookeeper 中进行查看

注意, 本例中使用了 Zookeeper 的 `chroot`, Kafka 相关的数据位于 Zookeeper 的 `/kafka` 路径下

在任意 Zookeeper 主机上, 通过如下命令进入 Zookeeper Shell

```bash
<zookeeper>/bin/zkCli.sh
```

看到命令提示符后, 输入如下命令获取 Kafka 元数据

1. 获取集群 ID

   ```bash
   get /kafka/cluster/id

   {"version":"1","id":"KRj3FZZ1QnOYG9zYBrmnUQ"}
   ```

2. 查看所有的 Broker 列表

   ```bash
   ls /kafka/brokers/ids

   [1, 2, 3]
   ```

3. 查看 Broker 详情

   显示 Broker ID 为 `1` 的详细信息

   ```bash
   get /kafka/brokers/ids/1

   {"listener_security_protocol_map":{"INTERNAL":"PLAINTEXT","EXTERNAL":"PLAINTEXT"},"endpoints":["INTERNAL://kf01:9092","EXTERNAL://localhost:19092"],"jmx_port":-1,"features":{},"host":"kf01","timestamp":"1684599876292","port":9092,"version":5}
   ```

4. 查看所有主题列表

   ```bash
   ls /kafka/brokers/topics

   [__consumer_offsets, test-topic]
   ```

   - `__consumer_offsets` 主题是 Kafka 内部使用的主题, 用来记录消费者提交的偏移量

5. 查看某个主题信息

   ```bash
   get /kafka/brokers/topics/test-topic

   {"removing_replicas":{},"partitions":{"2":[2,1],"1":[1,3],"0":[3,2]},"topic_id":"RSZ--odYRlSOPGRgukKnAQ","adding_replicas":{},"version":3}
   ```

一般情况下，不会通过 Zookeeper 查看主题列表，Kafka 本身提供了相关的 API
