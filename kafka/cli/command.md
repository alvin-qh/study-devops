# Command

- [Command](#command)
  - [1. 主题](#1-主题)
    - [1.1. 创建主题](#11-创建主题)
    - [1.2. 修改主题](#12-修改主题)
      - [1.2.1. 增加主题分区](#121-增加主题分区)
      - [1.2.2. 修改主题的配置](#122-修改主题的配置)
    - [1.3. 列举所有主题](#13-列举所有主题)
    - [1.4. 获取主题详情](#14-获取主题详情)
      - [1.4.1. 获取指定主题详情](#141-获取指定主题详情)
      - [1.4.2. 获取所有主题详情](#142-获取所有主题详情)
      - [1.4.3. 只获取主题的特殊配置](#143-只获取主题的特殊配置)
    - [1.5. 获取主题在相关分区的偏移量](#15-获取主题在相关分区的偏移量)
    - [1.6. 删除主题](#16-删除主题)
    - [1.7. 获取未正确同步的主题副本](#17-获取未正确同步的主题副本)
  - [2. 生产者](#2-生产者)
    - [2.1. 开启生产者 Shell](#21-开启生产者-shell)
    - [2.2. 发送文件中的内容](#22-发送文件中的内容)
    - [2.3. 发送 Key/Value 键值对](#23-发送-keyvalue-键值对)
  - [3. 消费者](#3-消费者)
    - [3.1. 启动消费者 Shell](#31-启动消费者-shell)
    - [3.2. 以 Key/Value 解析消费信息](#32-以-keyvalue-解析消费信息)
    - [3.3. 从头显示消费信息](#33-从头显示消费信息)
  - [4. 集群和 Brokers](#4-集群和-brokers)
    - [4.1. 获取集群 ID](#41-获取集群-id)
    - [4.2. 获取 Brokers 列表](#42-获取-brokers-列表)
    - [4.3. 查看 Broker 详情](#43-查看-broker-详情)
    - [4.4. 查看所有主题列表](#44-查看所有主题列表)
    - [4.5. 查看某个主题信息](#45-查看某个主题信息)
  - [附录](#附录)
    - [附1. 可动态修改的主题参数](#附1-可动态修改的主题参数)

对容器中命令执行，集群为 `localhost:9092,zk02:9092`

```bash
docker exec -it kf01 <command>
```

直接调用命令，在 Kafka 路径下，集群为 `localhost:9092,localhost:9093`

```bash
bin/<command>
```

## 1. 主题

### 1.1. 创建主题

创建一个 `5` 分区，`2` 副本的主题

```bash
kafka-topics.sh --create \
    --partitions 5 \
    --replication-factor 2 \
    --topic test-topic \
    --if-not-exists \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

- `--create` 创建主题
- `--partitions` 分区数，需要大于等于 Consumer 的数量
- `--replication` 副本数，最好和 broker 数量相同
- `--topic` 主题名称
- `--bootstrap-server` Kafka 集群
- `--if-not-exists` 如果主题已存在，忽略命令且不返回错误

创建一个覆盖 Kafka 配置的主题

```bash
kafka-topics.sh --create \
    --partitions 5 \
    --replication-factor 2 \
    --topic test-topic \
    --if-not-exists \
    --config retention.ms=10000 \
    --config segment.ms=10000 \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

- `--config` 要覆盖的配置项。参见 [附1. 可动态修改的主题参数](#附1-可动态修改的主题参数)

### 1.2. 修改主题

#### 1.2.1. 增加主题分区

将主题的分区扩展到 `10`

```bash
kafka-topics.sh --alter \
    --partitions 10 \
    --topic test-topic \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

- `--alter` 修改主题

#### 1.2.2. 修改主题的配置

将主题的回收时间设置为 `3` 天

```bash
kafka-configs.sh --alter \
    --entity-type topics \
    --entity-name test-topic \
    --add-config retention.ms=259200000 \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

- `--alter` 表示要修改配置
- `--entity-type topics` 表示要修改主题的配置
- `--entity-name test-topic` 表示要修改配置的主题名称
- `--add-config` 要增加（或修改）的配置项和值，可修改的配置项包括 [附1. 可动态修改的主题参数](#附1-可动态修改的主题参数)

删除主题的某个配置，恢复默认配置（或配置文件定义配置）

```bash
kafka-configs.sh --alter \
    --entity-type topics \
    --entity-name test-topic \
    --delete-config retention.ms \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

- `--delete-config` 要删除的配置项，可删除的配置项包括 [附1. 可动态修改的主题参数](#附1-可动态修改的主题参数)

### 1.3. 列举所有主题

```bash
kafka-topics.sh --list \
    --exclude-internal \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

- `--exclude-internal` 不包含内部主题（即 Kafka 自建主题）

### 1.4. 获取主题详情

#### 1.4.1. 获取指定主题详情

获取 test-topic 主题详情

```bash
kafka-topics.sh --describe \
    --topic test-topic \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092

Topic: test-topic    TopicId: q6MQnYbpTN6t77UYQ703rQ PartitionCount: 10      ReplicationFactor: 2    Configs: segment.bytes=1073741824,file.delete.delay.ms=60000,retention.bytes=536870912
        Topic: test-topic    Partition: 0    Leader: 2       Replicas: 2,1   Isr: 2,1
        Topic: test-topic    Partition: 1    Leader: 1       Replicas: 1,2   Isr: 1,2
        Topic: test-topic    Partition: 2    Leader: 2       Replicas: 2,1   Isr: 2,1
        Topic: test-topic    Partition: 3    Leader: 1       Replicas: 1,2   Isr: 1,2
        Topic: test-topic    Partition: 4    Leader: 2       Replicas: 2,1   Isr: 2,1
```

#### 1.4.2. 获取所有主题详情

```bash
kafka-topics.sh --describe \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

#### 1.4.3. 只获取主题的特殊配置

所谓特殊配置，就是通过 [1.2.2. 修改主题的配置](#122-修改主题的配置) 的方法修改的主题配置

```bash
kafka-topics.sh kafka-topics.sh --describe \
    --topic test-topic \
    --topics-with-overrides \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092

Topic: test-topic    TopicId: q6MQnYbpTN6t77UYQ703rQ PartitionCount: 5      ReplicationFactor: 2    Configs: segment.bytes=1073741824,file.delete.delay.ms=60000,retention.bytes=536870912
```

- `--topics-with-overrides` 显示被覆盖的配置项

### 1.5. 获取主题在相关分区的偏移量

查看 test-topic 主题的分区偏移量

```bash
kafka-run-class.sh kafka.tools.GetOffsetShell \
    --topic test-topic \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092

test-topic:0:2
test-topic:1:0
test-topic:2:1
test-topic:3:0
test-topic:4:0
```

- `--kafka-run-class.sh` 执行某个指定的 Java 类，本例中 `GetOffsetShell` 类表示在命令行中获取偏移量

### 1.6. 删除主题

正常情况下，Kafka 不推荐删除主题，通过设置 `retention.ms=1000` 配置项清空其内容即可

删除 test-topic 主题

```bash
    kafka-topics.sh --delete \
        --topic test-topic \
        --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

- `--delete` 删除主题

### 1.7. 获取未正确同步的主题副本

```bash
kafka-topics.sh --describe \
    --under-replicated-partitions \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

## 2. 生产者

### 2.1. 开启生产者 Shell

开启生产者 Shell，在命令提示符后输入数据

```bash
kafka-console-producer.sh \
    --topic test-topic \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

### 2.2. 发送文件中的内容

可以通过 `<` 管道操作符，将文件内容送入 Kafka

```bash
kafka-console-producer.sh \
    --topic test-topic \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092 < data.txt
```

这个命令无法通过 `docker exec` 在容器中直接使用

### 2.3. 发送 Key/Value 键值对

```bash
kafka-console-producer.sh \
    --topic test-topic \
    --property parse.key=true \
    --property key.separator=: \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

- `--property parse.key=true` 解析 key/value 模式
- `--property key.separator=:` 以 `:` 字符分隔 key 和 value

## 3. 消费者

### 3.1. 启动消费者 Shell

开启消费者控制台，生产者发送的内容会直接显示出来

```bash
kafka-console-consumer.sh \
    --topic test-topic --group g1 \
    --bootstrap-server localhost:9092,kf02:9092,kf03:9092
```

- `--group` 消费组编号

### 3.2. 以 Key/Value 解析消费信息

显示 key 和 value，并显示时间戳

```bash
kafka-console-consumer.sh \
    --topic test-topic \
    --group g1 \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.timestamp=true \
    --property print.key=true \
    --property print.value=true \
    --bootstrap-server localhost:9092,zk02:909
```

- `--formatter` 显示消费内容的格式，`kafka.tools.DefaultMessageFormatter` 表示默认格式
- `--property` 显示消费内容的设置
  - `print.timestamp=true` 显示时间戳
  - `print.key=true` 显示 key 值
  - `print.value=true` 显示 value 值

### 3.3. 从头显示消费信息

显示还未被删除的所有消息记录

```bash
kafka-console-consumer.sh \
    --topic test-topic --group g1 \
    --from-beginning \
    --bootstrap-server localhost:9092,kf02:9092
```

- `from-beginning` 从头获取消息记录

## 4. 集群和 Brokers

Kafka 将集群，Brokers 和主题的信息存储在 Kafka 中，以达到集群中各个节点统一协调的目的，所以这部分内容均需要通过访问 Zookeeper 达成

### 4.1. 获取集群 ID

获取集群 id。Kafka 的集群信息存储在 Zookeeper 的 `/cluster/id` 路径下

```json
bash zkCli.sh get /cluster/id

>
{
    "version": "1",
    "id": "Hfg-hYBiTSCfZhjg_d8LOw"
}
```

### 4.2. 获取 Brokers 列表

查看所有的 broker id 列表

```json
bash zkCli.sh ls /brokers/ids

>
[1, 2]
```

### 4.3. 查看 Broker 详情

显示 broker id 为 `1` 的 broker 详细信息

```json
bash zkCli.sh get /brokers/ids/1

>
{
    "listener_security_protocol_map": {
        "PLAINTEXT":"PLAINTEXT"
    },
    "endpoints": [
        "PLAINTEXT://kf01:9092" // 该 broker 的协议和地址
    ],
    "jmx_port": -1,
    "features": {
    },
    "host": "kf01", // 该 broker 的主机名
    "timestamp": "1641465975746",
    "port": 9092,
    "version": 5
}
```

### 4.4. 查看所有主题列表

显示所有的主题列表

```json
bash zkCli.sh ls /brokers/topics

>
[__consumer_offsets, test-topic]
```

一般情况下，不会通过 Zookeeper 查看主题列表，Kafka 本身提供了相关的 API

### 4.5. 查看某个主题信息

查看主题 `test-topic` 的详情

```json
bash zkCli.sh get /brokers/topics/test-topic

>
{
    "removing_replicas": { // 已删除的副本信息
    },
    "partitions": {   // 分区信息
        "2": [2, 1],
        "1": [1, 2],
        "4": [2, 1],
        "0": [2, 1],
        "3": [1, 2]
    },
    "topic_id": "q6MQnYbpTN6t77UYQ703rQ",
    "adding_replicas": { // 新增的副本信息
    },
    "version": 3
}
```

## 附录

### 附1. 可动态修改的主题参数

- 配置:
  - `cleanup.policy`
  - `compression.type`
  - `delete.retention.ms`
  - `file.delete.delay.ms`
  - `flush.messages`
  - `flush.ms`
  - `follower.replication.throttled.`
- 副本：
  - `index.interval.bytes`
  - `leader.replication.throttled.replicas`
  - `local.retention.bytes`
  - `local.retention.ms`
  - `max.compaction.lag.ms`
  - `max.message.bytes`
  - `message.downconversion.enable`
  - `message.format.version`
  - `message.timestamp.difference.max.ms`
  - `message.timestamp.type`
  - `min.cleanable.dirty.ratio`
  - `min.compaction.lag.ms`
  - `min.insync.replicas`
  - `preallocate`
  - `remote.storage.enable`
  - `retention.bytes`
  - `retention.ms`
  - `segment.bytes`
  - `segment.index.bytes`
  - `segment.jitter.ms`
  - `segment.ms`
  - `unclean.leader.election.enable`
