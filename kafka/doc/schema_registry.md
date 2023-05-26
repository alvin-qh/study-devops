# Schema Registry

- [Schema Registry](#schema-registry)
  - [1. Schema 注册表](#1-schema-注册表)
  - [2. 使用 Confluent Schema Registry](#2-使用-confluent-schema-registry)
    - [2.1. 安装](#21-安装)
    - [2.2. 通过容器](#22-通过容器)
    - [2.3. 配置](#23-配置)
      - [2.3.1. 配置文件](#231-配置文件)
      - [2.3.2. 容器配置](#232-容器配置)
    - [2.4. 使用 Schema Registry 服务](#24-使用-schema-registry-服务)
    - [2.5. RESTful API](#25-restful-api)
      - [2.5.1. 查看 Project](#251-查看-project)
      - [2.5.2. 创建 Project](#252-创建-project)
      - [2.5.3. 删除 Schema 和 Project](#253-删除-schema-和-project)
      - [2.5.4. Schema 兼容性](#254-schema-兼容性)

## 1. Schema 注册表

Kafka 的客户端 (生产者, 消费者) 发送的数据都为字节串 (即 `byte[]` 类型), 所以理论上 Kafka 可以发送和接收任何类型的数据, 例如:

- 编码为 UTF-8 的字符串数据;
- 序列化为字节串的 Java 对象;
- C/C++ 的结构体;

等等, 只需要生产者对消息数据进行序列化, 且消费者可以正确的对消息数据进行反序列化, 即双方通过一致的"数据格式协议"来处理数据即可. 但从应用未来发展的角度上, 仍希望定义的"数据格式协议"可以做到跨平台和跨语言

一种方式是使用 **结构化对象描述文本** (例如 XML 和 JSON) 来描述一个对象, 这样发送方只要能够将对象描述为字符串, 接收方可以从字符串反向解析出原始对象, 就可以达到通讯的目的, 且结构化对象描述文本本身就是平台和语义无关的对象描述协议

但结构化对象描述文本也有其弊端, 首先就是会影响传输的吞吐量, 因为这种对象描述方式势必在结构中会引入大量的非数据内容, 导致有效数据传输的能力有限; 另外, 将对象序列化为文本并从文本进行反序列化的效率也较低; 最后, 如果对象的描述发生了变化, 结构化对象描述难以向前兼容

## 2. 使用 Confluent Schema Registry

Confluent Schema Registry (参见 [官方文档](https://docs.confluent.io/platform/current/schema-registry/index.html)) 是官方提供的 Schema 注册服务, 可以和 Kafka 无缝进行集成

### 2.1. 安装

官方提供了两种安装方式: 1. 安装 Confluent Platform 套件, 其中包含了 Schema Registry; 2. 从 [GitHub](https://github.com/confluentinc/schema-registry) 上下载源码进行编译

编译需要安装 Maven, 通过如下两种方式进行编译和打包:

方式一: 标准编译

```bash
mvn package -DskipTests
```

编译完毕后, 可以在 Schema Registry 源码目录下的 `package-schema-registry/target/kafka-schema-registry-package-$VERSION-package` 目录下看到编译结果

另外, avro/json/protobuf 对象转化工具位于 `package-kafka-serde-tools/target/kafka-serde-tools-package-$VERSION-package` 目录下

方式二: 编译为独立 Jar 文件

```bash
mvn package -P standalone -DskipTests
```

### 2.2. 通过容器

官方并未提供 Schema Registry 的容器镜像, 本例中使用 `bitnami/schema-registry` 镜像, 参考 [docker/cluster-kraft/docker-compose.yml](../docker/cluster-kraft/docker-compose.yml) 文件

### 2.3. 配置

#### 2.3.1. 配置文件

Schema Registry 提供一组 RESTful API 来对 Schema 进行增删改查, 其后端数据就存储在 Kafka 中默认为 `_schemas` 的主题中, 所以多个 Schema Registry 可以轻易的根据同一个 Kafka 集群组成集群

所以, 需要为 Schema Registry 服务指定 Kafka 集群的地址, 需要修改 `<schema-registry-install-path>/etc/schema-registry/schema-registry.properties` 配置文件, 其基本配置如下:

- `listeners`, 指定 Schema Registry 的监听地址, 默认为 `0.0.0.0:8081`, 本例中改为 `0.0.0.0:18081`;
- `kafkastore.topic`, 指定 Schema Registry 存储信息的主题名称, 默认为 `_schemas`;
- `kafkastore.topic.replication.factor`, 指定 Schema Registry 存储信息主题的副本数, 默认为 `3`;
- `kafkastore.security.protocol`, 指定 Schema Registry 和 Kafka 连接的安全协议, 默认为 `PLAINTEXT`, 即明文连接;
- `kafkastore.bootstrap.servers`, 指定 Kafka 集群的 Broker 地址, 最好指定多个以体现高可用;

其余连接使用默认即可, 完整的配置列表参考 [官方文档](https://docs.confluent.io/platform/current/schema-registry/installation/config.html#schemaregistry-config)

#### 2.3.2. 容器配置

使用 Docker 容器时, 可以无需映射配置文件, 而是通过环境变量来传递配置项, 对于 `bitnami/schema-registry` 镜像, 环境变量的格式为 `SCHEMA_REGISTRY_<配置项> = 配置值`, 注意 `=` 左边要全为大写字母, 例如:

```ini
SCHEMA_REGISTRY_LISTENERS=18081
```

相当于配置项的 `listeners=18081`

这里注意, `bitnami/schema-registry` 中 `SCHEMA_REGISTRY_KAFKA_BROKERS` 会映射到配置项 `kafkastore.bootstrap.servers` 上, 而 `SCHEMA_REGISTRY_KAFKA_BOOTSTRAP_SERVERS` 则不起作用, 具体参考 [docker/cluster-kraft/env/registry.env](../docker/cluster-kraft/env/registry.env) 文件

### 2.4. 使用 Schema Registry 服务

一般情况下, Kafka 对 Schema Registry 服务的操作是透明的

当生产者在发送 avro 格式消息 (或者 JSON 格式消息) 时, 若消息的 Schema 未注册, 则消费者会将消息的 Schema 通过 Schema Registry 服务进行注册

生产者在接收消息时, 会收到该消息的 Schema 注册 ID, 通过该 ID 即可从 Schema Registry 服务获取 Schema 内容

生产者和消费者都会对 Schema 进行缓存, 防止无谓的访问 Schema Registry 服务, 所以 Schema Registry 服务的压力不会太大

这里面的关键点是, 生产者和消费者都需要直到 Schema Registry 服务的访问地址

Kafka 生产者会再 Schema Registry 服务上为每个消息 Schema 创建一个或两个 Subject, 里面按照版本存放注册的 Schema, 包括:

- 如果消息的 Key 需要注册 Schema, 则会以 `<topic-name>-key` 为格式创建 Subject, 并在其中注册 Key 的 Schema;
- 如果消息的 Value 需要注册 Schema, 则会以 `<topic-name>-value` 为格式创建 Subject, 并在其中注册 Value 的 Schema;

> 一般情况下, Key 都为简单的字符串, 所以只需要为 Value 注册 Schema 即可

### 2.5. RESTful API

虽然 Kafka 对 Schema Registry 服务的操作是自动化的, 无需开发人员干预, 但有时也需要知道 Schema Registry 服务注册了哪些 Schema, 并且对其进行手动的增删改查, 这就需要通过 Schema Registry 服务提供的一组 RESTful API 来进行

假设 Schema Register 服务的地址为 `localhost:18081`, Project 名称为 `schema-value`, 注册的 Schema 内容如下:

```json
{
  "schema": {
    "type": "record",
    "name": "Customer",
    "namespace": "kafka.customer.avro",
    "fields": [
      {
        "name": "id",
        "type": "int"
      },
      {
        "name": "name",
        "type":"string"
      },
      {
        "name": "email",
        "type": ["null", "string"],
        "default": null
      }
    ]
  }
}
```

#### 2.5.1. 查看 Project

1. 列举 Project 列表

   ```bash
   curl -X GET http://localhost:18081/subjects \
      -H "Content-Type: application/vnd.schemaregistry.v1+json"

   ["schema-value"]
   ```

2. 查询指定 Subject 下 Schema 的版本号列表

   ```bash
   curl -X GET http://localhost:18081/subjects/schema-value/versions \
      -H "Content-Type: application/vnd.schemaregistry.v1+json"

   [1]
   ```

3. 查询指定 Subject 下指定版本号的 Schema 内容

   ```bash
   curl -X GET http://localhost:18081/subjects/schema-value/versions/1 \
      -H "Content-Type: application/vnd.schemaregistry.v1+json"


   {"subject":"schema-value","version":1,"id":1,"schema":"{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"kafka.customer.avro\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"},{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"email\",\"type\":[\"null\",\"string\"],\"default\":null}]}"}
   ```

4. 查询指定 Subject 下最新版本的 Schema 内容

   ```bash
   curl -X GET http://localhost:18081/subjects/schema-value/versions/latest \
      -H "Content-Type: application/vnd.schemaregistry.v1+json"


   {"subject":"schema-value","version":1,"id":1,"schema":"{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"kafka.customer.avro\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"},{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"email\",\"type\":[\"null\",\"string\"],\"default\":null}]}"}
   ```

5. 查询指定 ID 的 Schema 内容

   ```bash
   curl -X GET http://localhost:18081/schemas/ids/1 \
      -H "Content-Type: application/vnd.schemaregistry.v1+json"


   {"schema":"{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"kafka.customer.avro\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"},{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"email\",\"type\":[\"null\",\"string\"],\"default\":null}]}"}
   ```

#### 2.5.2. 创建 Project

1. 在指定的 Project 下创建 Schema

   ```bash
   SCHEMA=$(cat schema/customer.avro | sed 's/\"/\\"/g' | sed ':a;N;$!ba;s/\r//g' | sed ':a;N;$!ba;s/\n//g')

   curl -X POST http://localhost:18081/subjects/schema-value/versions \
      -H "Content-Type: application/vnd.schemaregistry.v1+json" \
      -d "{\"schema\": \"$SCHEMA\"}"

   {"id":1}
   ```

   该 API 调用之后, 返回的是注册的 Schema 在当前 Project 中的版本号

   本例为了简单起见, 引用了 [schema/customer.avro](../schema/customer.avro) 文件, 里面存放了本例所用的 Schema 的 JSON 描述

   当前 API 需要将 Schema 的 JSON 描述以字符串形式放在请求 JSON 的 `schema` 字段中, 所以上述脚本的第一行就是从文件读取 Schema JSON, 并将所有的 `"` 进行转义 `\"`, 并将 Schema JSON 中的 `\r` 和 `\n` 字符删除

   如果再同一个 Project 下多次创建 Schema, 如果每次 Schema 内容不同, 则会创建不同版本的记录, 返回不同的版本编号

   > 说明: 因为 `sed` 命令本身是以行为单位进行处理的, 所以通过该命令无法直接替换 `\r` 或 `\n` 字符; 可以通过 `sed ':a;N;$!ba;s/\r//g'` 来完成替换 (其中 `:a` 表示设置一个标记, `N` 表示读取一行到模式区 (`Pattern Space`) 中, `$!ba` 表示如果不是最后一行 (即 `$!`) 则跳回到前一个标记 `:a` 再次执行, 直到最后一行读取完毕, 这样就将所有的行读取到模式区, 然后再进行后面的替换操作)

2. 检查 Schema 是否被注册

   ```bash
   SCHEMA=$(cat schema/customer.avro | sed 's/\"/\\"/g' | sed ':a;N;$!ba;s/\r//g' | sed ':a;N;$!ba;s/\n//g')

   curl -X POST http://localhost:18081/subjects/schema-value \
      -H "Content-Type: application/vnd.schemaregistry.v1+json" \
      -d "{\"schema\": \"$SCHEMA\"}"

   {"subject":"schema-value","version":3,"id":1,"schema":"{\"type\":\"record\",\"name\":\"Customer\",\"namespace\":\"kafka.customer.avro\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"},{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"email\",\"type\":[\"null\",\"string\"],\"default\":null}]}"}
   ```

   如果指定的 Subject 下已经注册同样的 Schema, 则返回其版本号和 ID; 如果未注册过, 则返回错误信息

#### 2.5.3. 删除 Schema 和 Project

1. 删除指定版本的 Schema

   ```bash
   curl -X DELETE http://localhost:18081/subjects/schema-value/versions/2 \
      -H "Content-Type: application/vnd.schemaregistry.v1+json"

   2
   ```

2. 删除整个 Project

   ```bash
   curl -X DELETE http://localhost:18081/subjects/schema-value \
      -H "Content-Type: application/vnd.schemaregistry.v1+json"

   [1, 2]
   ```

#### 2.5.4. Schema 兼容性

1. 查看一个 Schema 内容和指定 Subject 下的 Schema 是否兼容

   ```bash
   SCHEMA=$(cat schema/customer.avro | sed 's/\"/\\"/g' | sed ':a;N;$!ba;s/\r//g' | sed ':a;N;$!ba;s/\n//g')

   curl -X POST http://localhost:18081/compatibility/subjects/schema-value/versions/latest \
      -H "Content-Type: application/vnd.schemaregistry.v1+json" \
      -d "{\"schema\": \"$SCHEMA\"}"

   {"is_compatible":true}
   ```

   这一步操作是将请求中的 Schema 和服务端指定 Subject 下的某个版本进行比较, 不会更新服务端的 Schema 内容

   URL 最后一部分是 Schema 在 Project 下的版本号, `latest` 表示最新版本, 即用给定的 Schema 和服务端最新版本进行比较

2. 查看 Schema 兼容性配置

   ```bash
   curl -X GET http://localhost:18081/config

   {"compatibilityLevel":"BACKWARD"}
   ```

   返回服务端配置文件中定义的兼容性等级, 参考 [官方文档](https://docs.confluent.io/platform/current/schema-registry/installation/config.html#schemaregistry-config)

   > `compatibilityLevel` 为 `BACKWARD` 表示"向前兼容", 即通过新的 Schema 仍可以解析通过上一版 Schema 产生的消息内容

3. 修改全局 Schema 兼容性

   如果通过配置文件修改 Schema 兼容性, 则需要重启服务, 此时可以通过 RESTful API 进行动态修改

   ```bash
   curl -X PUT http://localhost:18081/config \
      -H "Content-Type: application/vnd.schemaregistry.v1+json" \
      -d '{"compatibility": "NONE"}'

   {"compatibility":"NONE"}
   ```

   注意, 除非新版本 Schema 确实无法和上一版本兼容, 且不兼容的 Message 可以舍弃, 否则不能轻易修改兼容性配置

4. 修改指定 Subject 下的 Schema 兼容性

   如果通过配置文件修改 Schema 兼容性, 则需要重启服务, 此时可以通过 RESTful API 进行动态修改

   ```bash
   curl -X PUT http://localhost:18081/config/schema-value \
      -H "Content-Type: application/vnd.schemaregistry.v1+json" \
      -d '{"compatibility": "NONE"}'

   {"compatibility":"NONE"}
   ```
