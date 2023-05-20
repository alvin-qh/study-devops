# 单实例容器

- [单实例容器](#单实例容器)
  - [1. 设置容器](#1-设置容器)
    - [1.1. 下载镜像](#11-下载镜像)
    - [1.2. 启动容器](#12-启动容器)
    - [1.3. 测试容器](#13-测试容器)
  - [2. 配置](#2-配置)
    - [2.1. 通过配置文件](#21-通过配置文件)
    - [2.2. 通过环境变量](#22-通过环境变量)
  - [3. 使用 Docker Compose](#3-使用-docker-compose)
  - [4. 容器监控](#4-容器监控)

如果只是做测试使用, 大部分情况下一个 Kafka 实例就完全足够, 可以通过 Docker 容器的方式很方便的在当前宿主机启动 Kafka 实例

## 1. 设置容器

### 1.1. 下载镜像

推荐使用 `bitnami/kafka` 镜像, 目前的最新版本 tag 为 `3.4`, 对应 Kafka 的最新版本

```bash
docker pull bitnami/kafka:latest
```

### 1.2. 启动容器

镜像下载完毕后, 即可通过 docker 命令启动容器

```bash
docker run -d --rm \
    --name kafka \
    -e KAFKA_ENABLE_KRAFT=yes \
    -e ALLOW_PLAINTEXT_LISTENER=yes \
    -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
    bitnami/kafka
```

可以查看启动的日志

```bash
docker logs -f kafka
```

### 1.3. 测试容器

1. 创建主题

   尝试创建一个主题, 包括 `2` 个分区, `1` 个副本 (只有一个 Broker 所以只能具备一个副本)

   ```bash
   docker exec -it kafka kafka-topics.sh --create \
       --partitions 2 \
       --replication-factor 1 \
       --topic test-topic \
       --if-not-exists \
       --config 'retention.ms=10000' \
       --config 'segment.ms=10000' \
       --bootstrap-server localhost:9092
   ```

2. 查看创建的主题情况

   ```bash
   docker exec -it kafka kafka-topics.sh --describe \
       --topic test-topic \
       --bootstrap-server localhost:9092
   ```

3. 向该主题发送一条消息

   ```bash
   docker exec -it kafka kafka-console-producer.sh \
       --topic test-topic \
       --property 'parse.key=true' \
       --property 'key.separator=:' \
       --bootstrap-server localhost:9092

   >1:Hello
   ```

   - `--property 'parse.key=true'` 和 `--property 'key.separator=:'` 分别表示消息需要 Key, 且 Key 和 Value 使用 `:` 符号分隔;

4. 从指定主题接收消息

   ```bash
   docker exec -it kafka kafka-console-consumer.sh \
       --topic test-topic \
       --group g1 \
       --from-beginning \
       --formatter kafka.tools.DefaultMessageFormatter \
       --property 'print.timestamp=true' \
       --property 'print.key=true' \
       --property 'print.value=true' \
       --bootstrap-server localhost:9092
   ```

   - `--from-beginning` 表示从上次读取提交的 offset 位置开始继续读取; 不设置此项则从日志分片的最新位置开始读取 (即只读取之后写入的新消息);

## 2. 配置

### 2.1. 通过配置文件

容器中 Kafka 的配置和非容器环境下基本一致

在 `bitnami/kafka` 镜像容器中, 配置文件位于容器的 `/opt/bitnami/kafka/config/kraft` 路径下 (由于本例中, 容器中的 Kafka 均是以 KRaft 模式启动, 如果不使用 Kraft 模式, 则配置文件的位置为 `/opt/bitnami/kafka/config` 路径), 可以将其中部分映射到宿主机的指定文件, 从而达到修改配置的目的, 例如:

```bash
docker run -d --rm \
    --name kafka \
    -v ./conf/server.properties:/opt/bitnami/kafka/config/kraft/server.properties \
    bitnami/kafka
```

注意: 由于容器中的用户和宿主机用户不同, 所以需要将需要映射的配置文件权限改为 `666`, 否则无法在容器内部读取, 即:

```bash
chmod 666 conf/server.properties
```

### 2.2. 通过环境变量

更简易的一种方式是通过设置环境变量, 即将配置文件中的参数进行如下修改:

- 所有字母转为大写;
- 将 `.` 分隔符改为 `_` 分隔符;
- 加上 `KAFKA_CFG_` 前缀;

例如 `server.properties` 配置文件中的 `advertised.listeners` 配置项, 修改为环境变量即为 `KAFKA_CFG_ADVERTISED_LISTENERS`, 通过 Docker 的 `-e` 参数设置即可, 即:

```bash
docker run -d --rm \
    --name kafka \
    -e KAFKA_ENABLE_KRAFT=yes \
    -e ALLOW_PLAINTEXT_LISTENER=yes \
    -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
    -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://:9092 \
    bitnami/kafka
```

注意, 不以 `KAFKA_CFG_` 为前缀的环境变量是容器本身使用的环境变量, 并非 Kafka 配置参数, 例如 `KAFKA_ENABLE_KRAFT` 环境变量是用于容器启动脚本选择配置文件的表示, 如果为 `yes`, 表示启动 Kraft 模式, 则使用 `/opt/bitnami/kafka/config/kraft/server.properties` 配置文件, 否则会使用 `/opt/bitnami/kafka/config/server.properties` 配置文件

## 3. 使用 Docker Compose

通过 [docker-compose.yml](./docker-compose.yml) 可以更方便的启动容器

```bash
docker-compose up -d
```

## 4. 容器监控

通过 `bitnami/kafka-exporter` 镜像可以启动 Kafka Exporter 容器, 对同一网络下的 Kafka 容器进行监控

Kafka Exporter 的配置使用具体参考 [使用 Kafka Exporter](../../doc/monitor.md#22-使用-kafka-exporter) 章节
