# 消费者

- [消费者](#消费者)
  - [1. 消费者相关概念](#1-消费者相关概念)
    - [1.1. 消费者和消费者组](#11-消费者和消费者组)
    - [1.2. 分区再平衡](#12-分区再平衡)
      - [1.2.1. 主动再平衡](#121-主动再平衡)
      - [1.2.2. 协作再平衡](#122-协作再平衡)
    - [1.3. 固定群组成员](#13-固定群组成员)
  - [2. 创建消费者](#2-创建消费者)
    - [2.1. 设置必要属性](#21-设置必要属性)
    - [2.2. 订阅主题](#22-订阅主题)
    - [2.3. 启动轮询](#23-启动轮询)
    - [2.4. 设置其它属性](#24-设置其它属性)
  - [3. 提交和偏移量](#3-提交和偏移量)
    - [3.1. 自动提交](#31-自动提交)
    - [3.2. 提交当前偏移量](#32-提交当前偏移量)
    - [3.3. 异步提交](#33-异步提交)
    - [3.4. 同步和异步组合提交](#34-同步和异步组合提交)
    - [3.5. 提交特定偏移量](#35-提交特定偏移量)
  - [4. 再平衡监听器](#4-再平衡监听器)
  - [5. 从特定偏移量位置读取记录](#5-从特定偏移量位置读取记录)
  - [6. 优雅的关闭消费者](#6-优雅的关闭消费者)
  - [7. 反序列化器](#7-反序列化器)

## 1. 消费者相关概念

**消费者** (Consumer) 即从 Kafka 读取消息的角色, 即生产者向 Kafka 写入消息, 由消费者从 Kafka 读取消息

### 1.1. 消费者和消费者组

Kafka 消费者从属于 **消费者组** (Consumer Group), 一个群组里的消费者订阅的是同一个主题的消息, 且主题的每个分区会分配给唯一的一个消费者. 即: 每个分区只能有一个消费者消费 (不可能将一个分区交给多个消费者), 但一个消费者可以同时消费多个分区

一般来说, 消费要比生产的速度慢, 所以消费者群组是对消费者进行横向扩展的方法

假设主题 `T1` 包含四个分区: `分区0 ~ 分区3`, 一个消费者组 `G1`, 则 `G1` 包含不同数量的消费者情况为:

1. 单消费者

   ![*](./../assets/consumer-group-one-consumer.png)

   此时, 单独的一个消费者会消费主题中所有分区的消息

2. 多消费者

   ![*](./../assets/consumer-group-two-consumer.png)

   此时, 四个分区会均匀的分配给两个消费者, 其中 `C1` 消费 `分区0` 和 `分区2`, `C2` 消费 `分区1` 和 `分区3`

3. 每个分区一个消费者

   ![*](./../assets/consumer-group-four-consumer.png)

   如果能为每个分区配备一个消费者, 则可以达到最佳的吞吐量

4. 消费者多于分区

   ![*](./../assets/consumer-group-five-consumer.png)

   如果消费者的数量多于分区的数量, 则多余的消费者将无法收到任何分区的消息, 因为一个分区不得被多个消费者消费, 所以设置多余分区数量的消费者是无意义的

可以增加多个消费者组, 例如为上面的例子增加消费者组 `G2` 也订阅主题 `T1` 的消息, 则 `G1` 和 `G2` 都会收到主题 `T1` 的全部消息, 即:

![*](../assets/multi-consumer-group.png)

增加消费者组和消费者, 不会影响 Kafka 的性能

### 1.2. 分区再平衡

分区再平衡 (Rebalance) 指的是当一个主题相关的消费者群组中的消费者发生变化时 (加入新的消费者或关闭已有的消费者), 则分区会进行 **再平衡**

消费者会向被指定为群组协调器的 Broker (不同消费者群组的协调器可能不同) 发送心跳, 以此来保持群组成员关系和对分区的所有权关系

心跳是由消费者的一个后台线程发送的, 只要消费者能够以正常的时间间隔发送心跳, Kafka 集群就会认为该消费者还"存活"; 如果消费者在足够长的一段时间内没有发送心跳, 群组协调器会认为它已经"死亡", 进而触发再平衡

在消费者被判定为"死亡"的这几秒时间里, "死亡"的消费者不会读取分区里的消息, 在关闭消费者后, 协调器会立即触发一次再平衡, 尽量降低处理延迟

根据消费者群组所使用的分区分配策略的不同, 分区再平衡分为两种情况

#### 1.2.1. 主动再平衡

在进行主动再平衡期间, 所有消费者都会停止读取消息, 放弃分区所有权并重新加入消费者群组, 并获得重新分配到的分区

主动再平衡会导致整个消费者群组在一个很短的时间窗口内不可用, 这个时间窗口的长短取决于消费者群组的大小和几个配置参数

![*](./../assets/active-rebalance.png)

可以看到, 主动再平衡包含两个不同的阶段:

- 第一个阶段, 所有消费者都放弃分区所有权;
- 第二个阶段, 消费者重新加入群组, 获得重新分配到的分区, 并继续读取消息;

#### 1.2.2. 协作再平衡

协作再平衡, 也称为增量再平衡, 通常是指将一个消费者的部分分区重新分配给另一个消费者，其他消费者则继续读取没有被重新分配的分区

这种再平衡包含两个或多个阶段:

- 在第一个阶段, 消费者群组首领会通知所有消费者, 它们将失去部分分区的所有权, 然后消费者会停止读取这些分区, 并放弃对它们的所有权;
- 在第二个阶段, 消费者群组首领会将这些没有所有权的分区分配给其他消费者;

虽然这种增量再平衡可能需要进行几次迭代, 直到达到稳定状态, 但它避免了主动再平衡中出现的停顿窗口期, 这对大型消费者群组来说尤为重要, 因为它们的再平衡可能需要很长时间

![*](../assets/cooperation-rebalance.png)

### 1.3. 固定群组成员

默认情况下, 消费者的群组成员身份标识是临时的:

- 当一个消费者离开群组时, 分配给它的分区所有权将被撤销；
- 当该消费者重新加入时, 将通过再平衡协议为其分配一个新的成员 ID 和新分区;

可以给消费者分配一个唯一的 `group.instance.id`, 让它成为群组的固定成员:

- 当消费者第一次以固定成员身份加入群组时, 群组协调器会按照分区分配策略给它分配一部分分区;
- 当该消费者被关闭时, 不会自动离开群组, 即它仍然是群组的成员, 直到会话超时;
- 当这个消费者重新加入群组时, 它会继续持有之前的身份, 并分配到之前所持有的分区;
- 群组协调器缓存了每个成员的分区分配信息, 只需要将缓存中的信息发送给重新加入的固定成员, 不需要进行再平衡;

注意: 如果两个消费者使用相同的 `group.instance.id` 并加入同一个群组, 则后加入的消费者会收到错误, 告诉它具有相同 ID 的消费者已存在

注意: 设置了 `group.instance.id` 属性的固定消费组成员再关闭后, 不会立即离开该组 (只是暂时不再接收消息), 分配给其的分区也不会被组中的其它消费者读取 (因为不会发生再平衡), 固定成员关闭后, 真正离开消费者组的时间由消费者的 `session.timeout.ms` 属性决定:

- 可以将 `session.timeout.ms` 属性的值设置大一些, 给被关闭的消费者留有足够重新加入群组的时间, 这样该群组不会发生再平衡, 避免整体暂停的情况; 当消费者重新加入群组时, 会立即开始读取其原本关联的分区, 该分区的读取进度会稍稍落后一些, 但很快就会被赶上;
- 但也不能将 `session.timeout.ms` 属性的值设置的过大, 这样被关闭的消费者一旦出现无法恢复的情况, 则意味着很长一段时间内, 其对应的分区都无法被读取, 会出现较大的滞后;
- 正确的做法是估算一般情况下消费者故障恢复的时间, 以此来作为 `session.timeout.ms` 属性的上限值, 超过该事件, 应该让消费组进行再平衡, 将无人读取的分区分配给其它消费者处理;

## 2. 创建消费者

通过 `KafkaConsumer` 类型可以创建消费者对象, 同创建生产者对象类似, 创建消费者对象同样是通过保存一组消费者属性的 `Properties` 对象来进行

### 2.1. 设置必要属性

创建消费者需要设置一些属性, 其中必要的属性包括:

1. `bootstrap.servers`, 指定要连接的 Kafka 集群的地址, 这个属性的含义和生产者的 `bootstrap.servers` 属性一致, 参见生产者的 [设置必要属性](./producer.md#21-设置必要属性) 章节;

2. `group.id`, 指定要加入的消费组 ID, 这个值非必填, 缺省表示加入 **默认组** (即单个消费者自成一组), 一般情况下应该提供该属性值;

3. `key.deserializer`, 指定用于对读取的消息键进行反序列化的类型, 对应生产者的 `key.serializer` 属性, 作用相反, 参见生产者的 [设置必要属性](./producer.md#21-设置必要属性) 章节, 其中介绍的所有 Serializer 类型, 都对应有一个 Deserializer 类型用于消费者;

4. `value.deserializer`, 指定用于对读取的消息值进行反序列化的类型, 对应生产者的 `value.serializer` 属性, 作用相反, 参见生产者的 [设置必要属性](./producer.md#21-设置必要属性) 章节, 其中介绍的所有 Serializer 类型, 都对应有一个 Deserializer 类型用于消费者;

### 2.2. 订阅主题

创建 `KafkaConsumer` 类型对象后, 即可让其订阅主题

一个消费者可以订阅多个主题, 所以订阅的主题是一个 `List` 集合, 即:

```java
consumer.subscribe(List.of("Topic1", "Topic2", ...));
```

另一种订阅多个主题的方式是通过正则表达式, 即符合该正则表达式的主题都会自动被该消费者订阅, 即便该主题是后于消费者加入 Kafka 集群的, 例如:

```java
consumer.subscribe(Pattern.compile("Topic\\d+"));
```

这就表示, 所有符合 `"Topic" + 数字` 模式的主题, 都会被这个消费者订阅

但一般情况下不建议一个消费者订阅一个以上的主题, 这违背了 Kafka 对消息进行负载均衡的初衷, 例外情况包括:

1. 数据同步程序 (ETL), 即所有发布的数据源主题, 都应该被该消费者订阅并处理;
2. 测试程序;

> 注意: 利用正则表达式订阅主题会带来以下的一些副作用:
>
> 1. 消费者需要不定期的向 Broker 请求所有订阅的分区和主题列表, 依次来检查是否有新增主题需要进行订阅, 所以如果集群中存在大量的分区和主题, 这会给系统带来额外的网络开销;
> 2. 为了能够通过正则表达式订阅主题, 对应消费者必须掌握集群的全部主题元数据, 需要能够获取整个集群元数据的权限;

### 2.3. 启动轮询

**轮询** 是 Kafka 消费者的核心 API, 用于从服务端请求数据

一般而言, 轮询方式的代码模式如下:

```java
var timeout = Duration.ofMillis(1000);

while (true) {
   var records = consumer.poll(timeout);
   for (var record : records) {
      // ...
   }
}
```

- 通过一个无限循环, 通过在其中不断调用 `poll` 方法, 从服务端获取消息记录;
- 轮询必须持续发生, 否则群控制器会认为该消费者已经"死亡", 会将其对应分区移交给其它消费者;
- `timeout` 参数表示当服务端没有消息时, 消费者端最长的等待时间;
- `poll` 方法返回的是一个集合对象, 表示两次 `poll` 方法调用之间服务端收到的所有消息对象;

轮询的过程:

1. 找到 `GroupCoordinator` (消费组控制器), 并加入指定的消费组, 对应匹配的分区;
2. 如果触发了再平衡, 则在轮询过程中会执行群组再平衡;
3. 如果超过 `max.poll.interval.ms` 时间后仍未再次调用 `poll` 方法, 则认为该消费者已经"死亡"并被移出消费组, 所以轮询过程中要避免任何阻塞操作;

使用轮询要注意线程安全, 一般情况下, 每个消费者要使用独立的线程进行轮询, 轮询结果可以放到线程池中异步执行

### 2.4. 设置其它属性

除了即便属性外, 其它消费者属性包括:

1. `fetch.min.bytes`, 指定了消费者从服务器获取记录的最小字节数, 默认为 `1` 字节; 当对延迟要求不高时, 增大这个值会增加吞吐量, 即当服务端接受的数据足够大时, 才会一次性的发送给消费者客户端;

2. `fetch.max.wait.ms`, 指定服务端在没有足够数据发送到客户端时, 最长的等待时间, 即: 如果消费者设置了最小消息大小 `fetch.min.bytes` 为 `1MB`, 则当消费者获取信息时, 如果服务端的消息大小超过 `1MB`, 则立即返回给消费者, 否则等待 `fetch.max.wait.ms` 时间后返回;

3. `fetch.max.bytes`, 指定了消费者一次性从服务器获取记录的最大字节数, 默认为 `50MB`:

   - 消费者会将服务端返回的数据放在内存中, 所以 `fetch.max.bytes` 参数约束了消费者存放数据的内存大小;
   - 如果服务端发送的一个批次数据大小超出消费者 `fetch.max.bytes` 的大小, 则意味着消费者需要多次获取才能完整的获得服务端这一批次的消息;
   - 要限定消息大小 (即超出限定的消息不予处理), 则需要在服务端的 Broker 设置中设置 `message.max.bytes` 属性值;

4. `max.poll.records`, 指定了消费者一次可以从服务端获取消息的条数, 即控制 `poll` 方法返回集合的最大长度;

5. `max.partition.fetch.bytes`, 指定了消费者每次 `poll` 操作从服务器各个分区返回数据的最大字节数, 默认为 `1MB`. 一般情况下不使用此属性 (因为分区可以动态变化, 这个参数导致消费者具体消耗的内存数不好估算), 而通过 `fetch.min.bytes` 属性来整体限制消费者使用的内存;

6. `session.timeout.ms` 和 `heartbeat.interval.ms`, 前者指定了消费者和服务端的会话超时时间, 默认 `10` 秒, 后者指定了消费者向服务端报告心跳的时间间隔:

   - 所谓会话超时时间, 即指消费者多久不和服务端通讯会被认定为"死亡";
   - 一般需要同时设置这两个属性, 且通常前者是后者的 $\frac{1}{3}$, 即如果 `session.timeout.ms` 为 `3` 秒, 则 `heartbeat.interval.ms` 应该为 `1` 秒;
   - 将 `session.timeout.ms` 设置的较小, 可以以最短时间检测到消费者故障, 但同时也会导致不必要的再平衡;

7. `max.poll.interval.ms`, 指定执行 `poll` 操作的最大允许时间间隔, 默认为 `5` 分钟:

   - 服务端除了通过设置心跳间隔时间和会话超时时间来保证消费者本身的活性外, 检测 `poll` 操作调用间隔也是一个检测消费者是否存活的重要依据;
   - 消费者在处理消息时, 一旦阻塞了调用 `poll` 操作的线程, 也会导致无法获取到后续的新消息;
   - 精确预判每次 `poll` 调用时间间隔比较困难, 所以一般需要为 `max.poll.interval.ms` 属性设置一个较长的时间, 让服务端即不会导致因为消息处理时间稍长而产生误杀, 也不会因为 `poll` 线程被阻塞而迟迟无法激活再平衡;

8. `default.api.timeout.ms`, 指定消费者在调用除 `poll` 方法外的其它 API 时的超时时间;

9. `auto.offset.reset`, 指定消费者在读取一个偏移量 (Offset) 无效或不存在的分区时, 默认的行为:

    - 如果一个消费者长时间离线, 会导致其对应分区的偏移量记录过期或删除, 此时需要消费者给出策略;
    - `latest` 表示如果偏移量记录失效, 则从最新的一条记录 (消息) 开始读取;
    - `earliest` 表示从该分区有记录以来最早的一条数据 (消息) 开始读取;

10. `enable.auto.commit`, 指定是否允许消费者自动提交偏移量, 默认为 `true`:

    - 自动提交偏移量意味着只要 `poll` 操作成功, 则消费者会将这部分记录的偏移量提交给服务端, 表示这部分消息已读, 下次就会读取之后偏移量的数据;
    - 如果该属性设置为 `true`, 则可以通过 `auto.commit.interval.ms` 属性值设置自动提交的频率;
    - 可以将该设置改为 `false`, 在消息处理成功之后, 手动提交该消息的偏移量, 这样可以尽量的保证消息丢失, 因为一个 `poll` 操作流程中, 如果抛出了异常导致未提交其偏移量, 则下次 `poll` 操作仍能拿回这条消息, 不会因为消息处理失败导致消息丢失;

11. `partition.assignment.strategy`, 指定分区器将分区和消费者进行匹配的策略. 例如: 消费者 `C1` 和 `C2` 同时订阅了两个主题 `T1` 和 `T2`, 且每个主题有 `3` 个分区, 则:

    - `range`, 即将主题的若干连续分区分配给消费者. 即消费者 `C1` 可能会分配到这两个主题的 `0` 和 `1` 分区, 消费者 `C2` 分配这两个主题的 `1` 分区. 且如果消费者是奇数个 (无法平均分配分区), 则先加入的消费者会比后加入的分配到更多的分区;
    - `roundRobin`, 即将主题逐个平均的分配给每个消费者, 即消费者 `C1` 会分配到 `T1` 主题的 `0` 和 `2` 分区以及 `T2` 主题的 `1` 分区, 而消费者 `C2` 会分配到 `T1` 主题的 `1` 分区和 `T2` 主题的 `0` 和 `2` 分区. 轮询方式会将分区分配的比较"均匀";
    - `sticky`, 即带有"黏性"的分区分配策略, 其目的为: 1. 尽可能均衡的分配分区; 2. 在进行再平衡时尽可能的保留原先分区和消费者的对应关系, 减少一个分区转移消费者的开销. 对于此策略, 如果所有消费者都订阅了相同的主题, 则其初始的分区分配和轮询方式一样均衡, 再平衡时能最大程度的减少要转移分区的数量; 如果消费者订阅了不同的主题, 则其分配比例比轮询分配的均衡性更好;
    - `cooperative sticky`, 类似于 `sticky` 策略, 但具备协作 (增量式) 再平衡, 参考 [协作再平衡](#122-协作再平衡) 章节;

12. `client.id`, 指定一个客户端标识, 可以是任意字符串 (具备唯一性), 主要用于 Broker 对客户端的识别 (例如在日志, 指标和配额中);

13. `client.rack`, 指定消费者获取消息的分区:

    - 默认情况下, 消费者会从每个分区的 Leader 副本中获取消息;
    - 如果消息需要同步到其它集群, 则可以固定一个专门用于读的分区, 以分散读的压力;
    - 通过在消费者端设置 `client.rack` 属性, 并在 Broker 端将 `replica.selector.class` 属性设置为 `org.apache.kafka.common.replica.RackAwareReplicaSelector`, 来约束消费者获取消息的分区副本;

14. `group.instance.id`, 指定消费者群组的固定名称, 可以是任意字符串 (具备唯一性), 具体参考 [固定群组成员](#13-固定群组成员) 章节;

15. `receive.buffer.bytes` 和 `send.buffer.bytes`, 分别用于指定消费者 TCP 发送和接收数据的缓冲区, 即 Socket 的 `SO_SNDBUF` 和 `SO_RCVBUF` 设置, 默认值为 `-1`, 即采用操作系统的默认值;

16. 偏移量保存时间: 参考 [消息偏移量相关配置](./broker.md#35-消息偏移量相关配置) 章节 `offsets.retention.minutes` 属性设置;

## 3. 提交和偏移量

消费者每次调用 `poll` 方法时, 总会返回还没有被任何消费者读取过的消息记录, 这意味着每一条消息都可以追踪到消费它的消费者, 即偏移量

服务端的每条数据都有一个"偏移量", 服务端也记录了当前各个分区的当前偏移量, 更新这些偏移量记录的操作即为提交偏移量

注意, 消费者并不会为每一条消息提交偏移量, 而是提交一批数据中最后一条消息的偏移量, 并假设此偏移量之前的消息记录都已经处理成功

消费者提交偏移量的步骤为:

1. 消费者会向一个叫做 `__consumer_offset` 的主题发送消息, 消息里包含每个分区的偏移量;
2. 如果消费者一直处于运行状态, 那么服务端存储的偏移量并无实际作用, 但一旦消费者离开群组, 新的消费者加入时, 就需要从服务端重新同步相关分区的偏移量; 另外, 消费者群组再平衡时, 也需要为每个消费者重新同步服务端的偏移量;

注意: 如果最后一次提交的偏移量小于客户端处理的最后一条消息的偏移量, 那么位于这两个偏移量之间的消息会被重复处理, 即:

![*](../assets/different-offset-1.png)

另外, 如果最后一次提交的偏移量大于客户端处理的最后一条消息的偏移量, 那么位于这两个偏移量之间的消息会丢失, 即:

![*](../assets/different-offset-2.png)

所以, 如果管理偏移量就对客户端应用程序有很大影响, 所以 Kafka Consumer API 提供了多种提交偏移量的方式:

### 3.1. 自动提交

自动提交是最简单的提交方式, 即消费者自动提交偏移量

若消费者的 `enable.auto.commit` 属性被设置为 `true`, 则表示消费者将自动提交偏移量, 自动提交的时间间隔由 `auto.commit.interval.ms` 属性来设置, 默认为 `5` 秒, 即每 `5` 秒就会将消费者最后一次轮询得到的消息中, 最后一条消息的偏移量提交给服务端, 参考 [设置其它属性](#24-设置其它属性) 章节中对这两个属性的描述

自动提交偏移量会导致如下副作用:

1. 如果消费者崩溃, 导致消费群组再平衡, 则崩溃消费者的 `auto.commit.interval.ms` 之间的消息会重复接收, 减少 `auto.commit.interval.ms` 值会减少这类重复消息的数量, 但无法完全避免;
2. 如果消费者端对消息是异步处理的, 则提交偏移量时, 有可能对应的消息尚未被处理, 此时消费者崩溃, 会导致这部分消息丢失;

### 3.2. 提交当前偏移量

将 `enable.auto.commit` 设置为 `false`, 即可对偏移量进行手动提交, 此时消费者将不再自动提交偏移量, 全部交给消费者程序来处理

通过 `commitSync` 方法可以手动提交偏移量, 这个 API 会提交 `poll` 返回的最新偏移量, 提交成功后立即返回, 所以:

- 如果在处理完所有消息记录前就调用了 `commitSync` 方法, 则一旦消费者崩溃, 会有消息丢失的风险;
- 如果消费者在处理信息记录时崩溃, 但 `commitSync` 方法还未被调用, 则会有消息重复的可能需;

注意, `commitSync` 方法在提交失败时会自动进行重试

```java
var timeout = Duration.ofMillis(1000);

while (true) {
    var records = consumer.poll(timeout);
    for (var record : records) {
        System.out.printf("topic = %s, partition = %d, offset = %d, key = %s, value = %s%n",
            record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }

    try {
        consumer.commitSync();
    } catch (CommitFailedException e) {
        throw ...;
    }
}
```

### 3.3. 异步提交

手动提交偏移量会阻塞当前线程, 直到从 Broker 返回了提交偏移量的响应, 这会对消费者的吞吐量产生影响

顾名思义, 异步提交偏移量无需等待 Broker 的响应, 响应将会以异步方式通过回调来进行通知

```java
var timeout = Duration.ofMillis(1000);

while (true) {
    var records = consumer.poll(timeout);
    for (var record : records) {
        System.out.println("topic = %s, partition = %d, offset = %d, key = %s, value = %s",
            record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }

    consumer.commitAsync((offsets, e) -> {
        if (e != null) {
            log.error(...);
        }
    });
}
```

注意, 异步提交不会进行重试, 发生错误即立即失败, 如需重试, 则需要手动重新调用 `commitAsync` 方法, 可以维护一个全局单调递增的序列号变量 (例如一个 `AtomicLong` 变量), 每次调用 `commitAsync` 方法前增加序列号, 并将当前序列号值传递到回调; 回调返回异常时, 先查看回调保存的序列号和全局序列号是否相等, 如果相等, 则有必要进行重试, 否则即表示已经开始处理新批次的消息, 无需重复提交偏移量

### 3.4. 同步和异步组合提交

一般情况下, 偏移量偶尔提交失败并不会有太大的影响, 因为随后会提交更靠后的偏移量, 从而覆盖掉之前的失败提交, 但对于消费者关闭 (或者消费群组再平衡) 前的最后一次提交, 则应该保障其成功:

- 通过异步和同步的组合来确保消费者关闭前成功提交偏移量;
- 通过再平衡监听器来确保再平衡前成功提交偏移量;

下面的代码演示如何结合同步和异步方式确保消费者关闭前正确提交偏移量

```java
var timeout = Duration.ofMillis(1000);

try {
    while (!closed) {
        var records = consumer.poll(timeout);

        for (var record : records) {
            System.out.printf("topic = %s, partition = %d, offset = %d, key = %s, value = %s%n",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
        }

        // 每次消息处理完毕, 进行一次异步消息提交, 如果失败也无需重试
        consumer.commitAsync();
    }

    // 当 closed 为 true 时, 表示消费者关闭, 此时进行一次同步的偏移量提交, 确保提交成功
    consumer.commitSync();
} catch (Exception e) {
    log.error(...);
} finally {
    consumer.close();
}
```

### 3.5. 提交特定偏移量

消费者 API 允许在调用 `commitSync` 和 `commitAsync` 方法时传给它们想要提交的分区和偏移量:

- 假设消费者正在处理一个消息批次, 刚处理好来自主题 `customers` 的分区 `3` 的消息, 其偏移量是 `5000`, 那么就可以调用`commitSync` 来提交这个分区的偏移量 `5001`;
- 因为一个消费者可能不止读取一个分区, 所以需要跟踪所有分区的偏移量, 因此通过这种方式提交偏移量会让代码变得复杂;

下面是提交特定偏移量的范例代码:

首先定义一个 `Map` 对象用于存储不同分区的偏移量, 以及一个 `count` 变量用于记录接收到的消息数量:

```java
private Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
private long count = 0;
```

接着利用上面定义的变量进行特定偏移量的提交

```java
var timeout = Duration.ofMillis(1000);

try {
    while (!closed) {
        var records = consumer.poll(timeout);

        for (var record : records) {
            System.out.printf("topic = %s, partition = %d, offset = %d, key = %s, value = %s%n",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());

            // 以当前消息的主题和分区为 key, 将偏移量的下一个位置存入 Map
            currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1, null));

            if (count++ % 1000 == 0) {
                // 提交各主体各分区的偏移量 (本次不进行回调, 所以第二个参数为 null)
                consumer.commitAsync(currentOffsets, null);
            }
        }
    }

    consumer.commitSync();
} catch (Exception e) {
    log.error(...);
} finally {
    consumer.close();
}
```

所以, `commitSync` 和 `commitAsync` 方法可以通过提交一个 `Map<TopicPartition, OffsetAndMetadata>` 类型的对象来提交所谓的 **特定偏移量**

## 4. 再平衡监听器

消费者 API 中提供了对消费组发生再平衡时进行处理的监听器

通过调用消费者的 `subscribe` 方法, 并传入一个 `ConsumerRebalanceListener` 接口对象, 即刻启动对消费组再平衡的监听, `ConsumerRebalanceListener` 有三个需要实现的方法:

- `onPartitionsAssigned(Collection<TopicPartition> partitions)`, 该回调会在消费者群组重新分配分区之后以及消费者开始读取消息之前被调用, 该方法用于加载与分区相关的状态信息, 设置正确的偏移量等; 该方法必须在 `max.poll.timeout.ms` 设定的时间内执行完毕, 以防止消费者超时导致无法加入群组;
- `onPartitionsRevoked(Collection<TopicPartition> partitions)`, 该方法会在消费者放弃对分区的所有权 (包括消费组再平衡和消费者被关闭) 时被调用. 对于主动再平衡 (参考 [主动再平衡](#121-主动再平衡) 章节), 则会在再平衡开始之前且消费者停止读取消息之后被调用; 对于协作再平衡 (参考 [协作再平衡](#122-协作再平衡) 章节), 则会在再平衡结束时调用, 且只涉及当前消费者放弃的那部分分区. 可以在该方法中提交偏移量, 即 [同步和异步组合提交](#34-同步和异步组合提交) 章节中提到的消费者应该在再平衡前正确提交偏移量;
- `onPartitionsLost(TopicPartition partitions)`, 该方法只有在一个消费者相关的 **原生分区** (即一开始就分配给消费者的分区, 而非之后通过再平衡分配的分区), 通过协作再平衡重新分配给其它消费者时被调用, 通过回调告诉程序哪些原生分区不再属于当前消费者; 如果不实现该方法, 则 `onPartitionsRevoked` 会被调用;

所以对于协作再平衡, `ConsumerRebalanceListener` 的行为有如下特殊情况:

1. `onPartitionsAssigned` 方法在每次进行再平衡时都会被调用, 以此来告诉消费者发生了再平衡. 如果没有新的分区分配给消费者, 那么它的参数就是一个空集合;
2. `onPartitionsRevoked` 会在进行正常的再平衡并且有消费者放弃分区所有权时被调用. 如果它被调用, 那么参数就不会是空集合;
3. `onPartitionsLost` 会在进行意外的再平衡并且参数集合中的分区已经有新的所有者的情况下被调用;
4. 如果这 `3` 个方法都被实现, 那么就可以保证在一个正常的再平衡过程中, 分区的新所有者监听的 `onPartitionsAssigned` 方法会在之前的分区所有者的 `onPartitionsRevoked` 被调用完毕并放弃了所有权之后被调用

下面演示了如何消费者失去分区所有权前提交一次偏移量

首先定义公共变量, 用于记录当前消费者所有主题和分区的偏移量

```java
private Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
```

接着实现 `ConsumerRebalanceListener`, 接口的 `onPartitionsRevoked` 方法, 在其中提交偏移量

```java
class HandleRebalance implements ConsumerRebalanceListener {
    // 分配到新分区时, 无需任何动作, 直接通过轮询读取新分区消息即可
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
    }

    // 当发生再平衡前, 提交当前消费者的偏移量
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        System.out.printf(
            "Lost partitions in rebalance. Committing current offsets %s%n", currentOffsets);
        consumer.commitSync(currentOffsets);
    }
}
```

最后, 在启动轮询前执行监听

```java
try {
    // 在轮询启动前对再平衡启动监听
    consumer.subscribe(topics, new HandleRebalance());

    // 在循环中进行轮询
    while (!closed) {
        var records = consumer.poll();

        for (var record : records) {
            System.out.printf("topic = %s, partition = %d, offset = %d, key = %s, value = %s%n",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());

            // 将每条消息的偏移量写入 Map 对象
            currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1, null));
        }

        // 提交这批消息的偏移量
        consumer.commitAsync(currentOffsets, null);
    }
} catch (WakeupException e) {
    // ignore
} catch (Exception e) {
    log.error(...);
} finally {
    consumer.close();
}
```

## 5. 从特定偏移量位置读取记录

如果需要从分区的起始位置读取所有的消息, 或者直接跳到分区的末尾读取新消息, 则 Kafka API 分别提供了两个方法:

- `seekToBeginning(Collection<TopicPartition> partitions)`, 将指定主题分区的偏移量设置到起始位置, 即从指定分区的消息日志的头部开始重新读取所有消息;
- `seekToEnd(Collection<TopicPartition> partitions)`, 将指定主题分区的偏移量设置到结束位置, 即从最新的消息开始读取, 忽略之前的所有消息;

Kafka 还提供了用于查找特定偏移量的 API, 以进行类似如下的操作:

- 对时间敏感的应用程序在处理速度滞后的情况下可以向前跳过几条消息;
- 如果消费者写入的文件丢失了, 则它可以重置偏移量, 回到某个位置进行数据恢复;

例如将分区的当前偏移量定位到指定的记录生成时间点上

```java
// 计算当前实际向前一个小时的时间戳 (秒数)
var oneHourEarlier = Instant.now().atZone(ZoneId.systemDefault()).minusHours(1).toEpochSeconds();

// 创建一个 Map 对象, Key 为 TopicPartition 对象, Value 为一小时前的时间戳
var partitionTimestampMap = consumer.assignment().stream().collect(
    Collectors.toMap(tp -> tp, tp -> oneHourEarlier));

// 查找时间戳在一小时前的所有主题分区的偏移量值
var offsetMap = consumer.offsetsForTimes(partitionTimestampMap);

// 遍历找到的主题分区, 将其偏移量移动到查询到的值上
for (var entry : offsetMap.entrySet()) {
    consumer.seek(entry.getKey(), entry.getValue().offset());
}
```

## 6. 优雅的关闭消费者

所谓优雅的关闭消费者, 就是要保证消费者结束的时候一定不能正在执行 `poll` 方法

所以, 可以通过等待上一轮 `poll` 结束 (或超时), 处理完这一批消息后, 退出循环, 执行消费者对象的 `close` 方法, 退出程序;

如果消费者 `poll` 方法的超时设置的比较久, 来不及等待其退出, 则可以在另一个线程中调用消费者对象的 `wakeup` 方法, 该方法会导致正在调用的 `poll` 方法立即抛出 `WakeupException`, 从而结束调用, 借此机会退出循环, 执行消费者对象的 `close` 方法, 退出程序;

## 7. 反序列化器
