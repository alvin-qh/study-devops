# KRaft 集群容器化

- [KRaft 集群容器化](#kraft-集群容器化)
  - [1. 容器配置](#1-容器配置)
    - [1.1. 设置内外网](#11-设置内外网)
    - [1.2. 设置对外发布地址](#12-设置对外发布地址)
    - [1.3. 配置清单](#13-配置清单)
  - [2. 测试集群](#2-测试集群)
    - [2.1. 查看集群元数据日志文件](#21-查看集群元数据日志文件)
    - [2.2. 测试集群](#22-测试集群)
  - [3. 集群容器监控](#3-集群容器监控)

使用容器配置 KRaft 集群更加简单方便, 因为容器的启动脚本会自动进行日志目录的格式化, 所以只要做好配置启动容器即可组成集群

## 1. 容器配置

使用 KRaft 容器配置集群和在物理机上进行配置基本一致, 需要注意的主要是以下几点:

1. 对于通过 `bitnami/kafka` 镜像创建的容器, 仍推荐使用环境变量替代配置文件, 这样一方面可以减少一次文件映射, 避免映射文件的权限问题, 另一方面也较为方便;
2. 容器的网络只对容器内部开放, 即容器间通过内部网络控制器组成了 **内网**, 而宿主机则是通过映射容器的端口进行访问, 相当于 **外网**, 所以如果有通过宿主机访问容器中 Kafka 实例的需求, 则 Kafka 要具备多套网络侦听, 分别对应内网和外网的情况;

关于 KRaft 集群的基本配置, 可以参考 [KRaft 集群](../../doc/kraft.md) 章节

### 1.1. 设置内外网

1. 设置 `KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP` 环境变量:

   `KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP` 环境变量即 `listener.security.protocol.map` 配置项, 用于设置监听器和协议的映射关系

   为了区分内外网, 默认的监听器定义不够用, 需要额外定义 `INTERNAL` 和 `EXTERNAL` 两个监听器

   ```ini
   KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT
   ```

   - `INTERNAL`, 表示用于内网监听的监听器, 映射到 `PLAINTEXT`;
   - `EXTERNAL`, 表示用于外网监听的监听器, 映射到 `PLAINTEXT`;
   - `CONTROLLER`, 表示用于控制器监听的监听器, 映射到 `PLAINTEXT`;

   之后, 所有使用监听器的地方就必须使用 `KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP` 中定义的监听器名称, 包括:

   - `KAFKA_CFG_INTER_BROKER_LISTENER_NAME=INTERNAL`, 指定 Broker 之间的内网连接使用 `INTERNAL` 监听器;
   - `KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER`, 指定控制器之间的内网连接使用 `CONTROLLER` 监听器;

2. 设置 `KAFKA_CFG_LISTENERS` 环境变量:

   `KAFKA_CFG_LISTENERS` 环境变量即 `listeners` 配置项, 用于设置当前节点的监听地址

   这里需要增加一个外网监听地址, 本例中设置为 `0.0.0.0:19092`, 即:

   ```ini
   KAFKA_CFG_LISTENERS=INTERNAL://:9092,EXTERNAL://:19092,CONTROLLER://:9093
   ```

   - 这里的 `EXTERNAL` 即表示外网监听协议. 因为 `listeners` 设置中不能设置多个重复的监听器, 所以需要在 `listener.security.protocol.map` 设置设置足够的监听器

### 1.2. 设置对外发布地址

设置好了 Kafka 的监听器和监听地址后, 在容器使用中还会发生一个问题, 即: 如果需要从宿主机连接使用容器中的 Kafka, 同步到客户端 (生产者, 消费者) 的 Broker 地址实际上是容器内网使用的地址, 并不能从容器宿主机进行访问

为了解决上述问题, 在上一步还额外配置了一个 `EXTERNAL://:19092` 监听地址, 即监听 `0.0.0.0:19092` 地址, 将该端口和宿主机端口映射后, 将映射后的宿主机地址作为对外地址发布

设置 `KAFKA_CFG_ADVERTISED_LISTENERS` 环境变量即 `advertised.listeners` 配置项, 用于设置实际发布到客户端的监听地址, 即"外网"监听地址

```ini
KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL://<容器主机名>:9092,EXTERNAL://<宿主机主机名>:19092
```

注意, `advertised.listeners` 配置项必须包含 `inter.broker.listener.name` 设置的监听器, 所以必须同时包含 `INTERNAL` 和 `EXTERNAL` 这两个监听器, 前者满足 `inter.broker.listener.name` 配置项的要求, 相当于发布了"内网"监听地址, 后者相当于发布了"外网"监听地址

这里的 `EXTERNAL://<宿主机主机名>:19092` 实际描述的是从当前容器的宿主机进行访问的地址, 用于同步到客户端 (生产者, 消费者) 进行连接的地址

### 1.3. 配置清单

公共配置 (即三个容器节点公用的配置) 环境变量定义在 [env/kf.env](./env/kf.env) 文件中

每个容器节点需要单独配置控制器 ID 和发布的监听地址, 包括:

- `kf01` 节点:

  ```yml
  ports:
    - 19092:19092
  environment:
    KAFKA_CFG_NODE_ID=1
    KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL://kf01:9092,EXTERNAL://localhost:19092
  ```

- `kf02` 节点:

  ```yml
  ports:
    - 19093:19092
  environment:
    KAFKA_CFG_NODE_ID=2
    KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL://kf02:9092,EXTERNAL://localhost:19093
  ```

- `kf03` 节点:

  ```yml
  ports:
    - 19094:19092
  environment:
    KAFKA_CFG_NODE_ID=3
    KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL://kf03:9092,EXTERNAL://localhost:19094
  ```

上面的配置参考 [docker-compose.yml](./docker-compose.yml) 配置文件

本例中, 客户端 (生产者, 消费者) 被设定与 Kafka 容器在同一台宿主机, 根据 `KAFKA_CFG_ADVERTISED_LISTENERS` 环境变量的配置, 客户端会使用 `localhost:19092,localhost:19093,localhost:19094` 这三个地址进行连接

## 2. 测试集群

按 [docker-compose.yml](./docker-compose.yml) 配置启动的集群为例, 启动容器并对其进行测试

```bash
docker-compose up -d
```

### 2.1. 查看集群元数据日志文件

KRaft 模式下, 原先保存在 Zookeeper 上的数据会全部转移到一个内部的 `@metadata` 主题上, 可以通过查看这个主题的日志分片来确认集群状态

登录控制集群的一个节点, 进入日志存储目录 (由 `log.dirs` 配置指定), 执行如下命令, 进入元数据交互会话:

```bash
docker exec -it kf01 kafka-metadata-shell.sh \
    --snapshot /bitnami/kafka/data/__cluster_metadata-0/00000000000000000000.log

>> ls /
brokers  features  local  metadataQuorum

>> ls brokers/
1  2  3

>> cat metadataQuorum/offset
4800
```

可以看到, 整个集群具有 `3` 个 Broker 节点

### 2.2. 测试集群

1. 尝试创建一个主题

   尝试创建一个主题, 包括 `3` 个分区, 每分区 `2` 个副本

   ```bash
   docker exec -it kf01 kafka-topics.sh --create \
       --partitions 3 \
       --replication-factor 2 \
       --topic test-topic \
       --if-not-exists \
       --bootstrap-server kf01:9092,kf02:9020,kf03:9020
   ```

2. 查看创建的主题情况

   ```bash
   docker exec -it kf01 kafka-topics.sh --describe \
       --topic test-topic \
       --bootstrap-server kf01:9092,kf02:9020,kf03:9020
   ```

3. 向该主题发送一条消息

   ```bash
   docker exec -it kf01 kafka-console-producer.sh \
       --topic test-topic \
       --property 'parse.key=true' \
       --property 'key.separator=:' \
       --bootstrap-server kf01:9092,kf02:9020,kf03:9020

   >1:Hello
   ```

   - `--property 'parse.key=true'` 和 `--property 'key.separator=:'` 分别表示消息需要 Key, 且 Key 和 Value 使用 `:` 符号分隔;

4. 从指定主题接收消息

   ```bash
   docker exec -it kf01 kafka-console-consumer.sh \
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

## 3. 集群容器监控

通过 `bitnami/kafka-exporter` 镜像可以启动 Kafka Exporter 容器, 对同一个集群中的 Kafka 节点进行监控

Kafka Exporter 的配置使用具体参考 [使用 Kafka Exporter](../../doc/monitor.md#22-使用-kafka-exporter) 章节
