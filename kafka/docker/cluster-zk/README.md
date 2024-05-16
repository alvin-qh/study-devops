# Kafka 传统集群容器化配置

- [Kafka 传统集群容器化配置](#kafka-传统集群容器化配置)
  - [1. Zookeeper 集群配置](#1-zookeeper-集群配置)
  - [2. Kafka 集群配置](#2-kafka-集群配置)
    - [2.1. 关闭 KRaft 模式](#21-关闭-kraft-模式)
    - [2.2. 设定 Zookeeper 集群地址](#22-设定-zookeeper-集群地址)
    - [2.3. 设置内外网](#23-设置内外网)
    - [2.4. 设置对外发布地址](#24-设置对外发布地址)
    - [2.5. 配置清单](#25-配置清单)
  - [3. 测试集群](#3-测试集群)
    - [3.1. 查看集群元数据](#31-查看集群元数据)
    - [3.2. 查看集群元数据](#32-查看集群元数据)
  - [4. 集群容器监控](#4-集群容器监控)

这里所谓的 Zookeeper 集群, 指的是利用 Zookeeper 记录控制器元数据的 Kafka 集群, 这是一种传统的 Kafka 集群搭建方式, 新版本的 Kafka 可以使用 KRaft 协议控制器, 不再依赖于 Zookeeper

KRaft 集群的容器化参考 [KRaft 集群容器化](../cluster-kraft/README.md) 章节

所以整个集群的搭建分两个部分: 1. Zookeeper 集群搭建; 2. Kafka 集群搭建, 本章节重点在于 Kafka 集群, Zookeeper 集群的设置与使用不作详细说明

## 1. Zookeeper 集群配置

Zookeeper 的集群最少需要 `3` 个节点, 少于 `3` 个节点会在集群进行仲裁的时候出现"脑裂"现象

本例中使用了 Zookeeper 的官方镜像 `zookeeper` 并启动 `3` 个容器组成集群, 供 Kafka 连接使用

官方 Zookeeper 镜像容器也是通过 **环境变量** 来取代部分配置项, 以达到简化配置的作用, 环境变量的命名规范为: `ZOO_<配置项>`, 且配置项的驼峰命名要改为下划线命名且所有字母大写 (例如: `tickTime` 对应的环境变量为 `ZOO_TICK_TIME`), 一些必要的环境变量包括:

1. `ZOO_MY_ID`, 定义某个节点的唯一 ID, 取值 `1~255`, 该 ID 会写入节点的 `data/myid` 文件, 对于 `ZOO_MY_ID=1` 的节点, 则 `data/myid` 文件内容如下:

   ```bash
   cat data/myid

   1
   ```

   `data` 目录由配置文件的 `dataDir` 配置项指定, 再官方镜像中被固定为 `/data` 路径

2. `ZOO_SERVERS`, 其值为 `servers.<n>` 配置项 (`n` 为各个服务节点的 `myid`), 用于指定集群中的所有主机地址, 多个配置通过 **空格** 分隔, 例如:

   ```ini
   ZOO_SERVERS=server.1=zk01:2888:3888;2181 server.2=zk02:2888:3888;2181 server.3=zk03:2888:3888;2181
   ```

   对应的配置项为:

   ```ini
   server.1=zk01:2888:3888;2181
   server.2=zk02:2888:3888;2181
   server.3=zk03:2888:3888;2181
   ```

更多配置项参考容器中的 `/conf/zoo.cfg` 配置文件, 本章节不再详细赘述

## 2. Kafka 集群配置

完成 Zookeeper 集群配置后, 即可进行 Kafka 集群配置, 基本的容器配置参考 [单实例容器](../standalone/README.md) 章节, 需要注意的主要是以下几点:

1. 对于通过 `bitnami/kafka` 镜像创建的容器, 推荐使用环境变量替代配置文件, 这样一方面可以减少一次文件映射, 避免映射文件的权限问题, 另一方面也较为方便;
2. 容器的网络只对容器内部开放, 即容器间通过内部网络控制器组成了 **内网**, 而宿主机则是通过映射容器的端口进行访问, 相当于 **外网**, 所以如果有通过宿主机访问容器中 Kafka 实例的需求, 则 Kafka 要具备多套网络侦听, 分别对应内网和外网的情况;

### 2.1. 关闭 KRaft 模式

由于本例中使用的 `bitnami/kafka` 镜像默认开启 KRaft 模式, 所以为了使用 Zookeeper, 需要将 `KAFKA_ENABLE_KRAFT` 环境变量设置为关闭

```ini
KAFKA_ENABLE_KRAFT=no
```

### 2.2. 设定 Zookeeper 集群地址

下一步需要为 Kafka 指定 Zookeeper 服务的地址, 为了高可用性, 尽量指定足够多的 Zookeeper 服务节点地址, Zookeeper 集群地址通过 `KAFKA_CFG_ZOOKEEPER_CONNECT` 环境变量指定, 对应 Kafka 的 `zookeeper.connect` 配置项:

```ini
KAFKA_CFG_ZOOKEEPER_CONNECT=zk01:2181,zk02:2181,zk03:2181/kafka
```

注意, 配置中的最后一个 Zookeeper 节点地址设置了 `chroot`, 表示所有和 Kafka 相关的数据都写入 `/kafka` 路径下, 这是一个良好的习惯, 用于将 Kafka 数据和其它使用 Zookeeper 的服务隔离开

### 2.3. 设置内外网

1. 设置 `KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP` 环境变量:

   `KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP` 环境变量用于设置监听器和协议的映射关系

   为了区分内外网, 默认的监听器定义不够用, 需要额外定义 `INTERNAL` 和 `EXTERNAL` 两个监听器

   ```ini
   KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT
   ```

   - `INTERNAL`, 表示用于内网监听的监听器, 映射到 `PLAINTEXT`;
   - `EXTERNAL`, 表示用于外网监听的监听器, 映射到 `PLAINTEXT`;

   之后, 所有使用监听器的地方就必须使用 `KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP` 中定义的监听器名称, 包括:

   - `KAFKA_CFG_INTER_BROKER_LISTENER_NAME=INTERNAL`, 指定 Broker 之间的内网连接使用 `INTERNAL` 监听器;

2. 设置 `KAFKA_CFG_LISTENERS` 环境变量:

   `KAFKA_CFG_LISTENERS` 环境变量用于设置当前节点的监听地址

   这里需要增加一个外网监听地址, 本例中设置为 `0.0.0.0:19092`, 即:

   ```ini
   KAFKA_CFG_LISTENERS=INTERNAL://:9092,EXTERNAL://:19092
   ```

   - 这里的 `EXTERNAL` 即表示外网监听协议. 因为 `KAFKA_CFG_LISTENERS` 环境变量中不能设置多个重复的监听器, 所以需要在 `KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP` 环境变量设置设置足够的监听器

### 2.4. 设置对外发布地址

设置好了 Kafka 的监听器和监听地址后, 在容器使用中还会发生一个问题, 即: 如果需要从宿主机连接使用容器中的 Kafka, 同步到客户端 (生产者, 消费者) 的 Broker 地址实际上是容器内网使用的地址, 并不能从容器宿主机进行访问

为了解决上述问题, 在上一步还额外配置了一个 `EXTERNAL://:19092` 监听地址, 即监听 `0.0.0.0:19092` 地址, 将该端口和宿主机端口映射后, 将映射后的宿主机地址作为对外地址发布

设置 `KAFKA_CFG_ADVERTISED_LISTENERS` 环境变量, 用于设置实际发布到客户端的监听地址, 即"外网"监听地址

```ini
KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL://<容器主机名>:9092,EXTERNAL://<宿主机主机名>:19092
```

注意, `KAFKA_CFG_ADVERTISED_LISTENERS` 环境变量必须包含 `KAFKA_CFG_INTER_BROKER_LISTENER_NAME` 环境变量设置的监听器, 所以必须同时包含 `INTERNAL` 和 `EXTERNAL` 这两个监听器, 前者满足 `KAFKA_CFG_INTER_BROKER_LISTENER_NAME` 环境变量的要求, 相当于发布了"内网"监听地址, 后者相当于发布了"外网"监听地址

这里的 `EXTERNAL://<宿主机主机名>:19092` 实际描述的是从当前容器的宿主机进行访问的地址, 用于同步到客户端 (生产者, 消费者) 进行连接的地址

### 2.5. 配置清单

公共配置 (即三个容器节点公用的配置) 环境变量定义在 [env/kf.env](./env/kf.env) 文件中

每个容器节点需要单独配置控制器 ID 和发布的监听地址, 包括:

- `kf01` 节点:

  ```yml
  env_file:
    - ./env/kf.env
  ports:
    - 19092:19092
  environment:
    - KAFKA_CFG_BROKER_ID=1
    - KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL://kf01:9092,EXTERNAL://localhost:19092
  ```

- `kf02` 节点:

  ```yml
  env_file:
    - ./env/kf.env
  ports:
    - 19093:19092
  environment:
    - KAFKA_CFG_BROKER_ID=2
    - KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL://kf02:9092,EXTERNAL://localhost:19093
  ```

- `kf03` 节点:

  ```yml
  env_file:
    - ./env/kf.env
  ports:
    - 19094:19092
  environment:
    - KAFKA_CFG_BROKER_ID=3
    - KAFKA_CFG_ADVERTISED_LISTENERS=INTERNAL://kf03:9092,EXTERNAL://localhost:19094
  ```

上面的配置参考 [docker-compose.yml](./docker-compose.yml) 配置文件

本例中, 客户端 (生产者, 消费者) 被设定与 Kafka 容器在同一台宿主机, 根据 `KAFKA_CFG_ADVERTISED_LISTENERS` 环境变量的配置, 客户端会使用 `localhost:19092,localhost:19093,localhost:19094` 这三个地址进行连接

## 3. 测试集群

按 [docker-compose.yml](./docker-compose.yml) 配置启动的集群为例, 启动容器并对其进行测试

```bash
docker-compose up -d
```

### 3.1. 查看集群元数据

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

### 3.2. 查看集群元数据

本例中, 集群的元数据存在于 Zookeeper 中, 所以需要到 Zookeeper 中进行查看

注意, 本例中使用了 Zookeeper 的 `chroot`, Kafka 相关的数据位于 Zookeeper 的 `/kafka` 路径下

选择任意 Zookeeper 容器, 通过如下命令进入 Zookeeper Shell

```bash
docker exec -it zk01 zkCli.sh
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

## 4. 集群容器监控

通过 `bitnami/kafka-exporter` 镜像可以启动 Kafka Exporter 容器, 对同一个集群中的 Kafka 节点进行监控

Kafka Exporter 的配置使用具体参考 [使用 Kafka Exporter](../../doc/monitor.md#22-使用-kafka-exporter) 章节
