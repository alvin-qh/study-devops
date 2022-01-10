# 生产者配置

- `metadata.broker.list=kafka1:9092,kafka2:9092,kafka3:9092` 指定节点列表

- `partitioner.class=kafka.producer.DefaultPartitioner` 指定分区处理类。默认 `kafka.producer.DefaultPartitioner`

- `compression.codec=0` 是否压缩。`0` 代表不压缩，`1` 代表用 gzip 压缩，`2` 代表用 snappy 压缩

- `serializer.class=kafka.serializer.DefaultEncoder` 指定序列化处理类

- `compressed.topics=` 如果要压缩消息，这里指定哪些 topic 要压缩消息，默认是 空，表示不压缩

- `request.required.acks=0` 设置发送数据是否需要服务端的反馈，有三个值 `0`，`1`，`-1`
  - `0` producer 不会等待 broker 发送 ack
  - `1` 当 leader 接收到消息后发送 ack
  - `-1` 当所有的 follower 都同步消息成功后发送 ack

- `request.timeout.ms=10000` 在向 producer 发送 ack 之前，broker 均需等待的最大时间

- `producer.type=async` `sync` 同步（默认），`async` 异步。异步可以提高发送吞吐量

- `queue.buffering.max.ms=5000` 在 `async` 模式下，当 message 缓存超时后，将会批量发送给broker。默认 5000ms

- `queue.buffering.max.messages=20000` 在 `async` 模式下，producer 端允许 buffer 的最大消息量

- `batch.num.messages=500` 在 `async` 模式下，指定每次批量发送的数据量，默认 `200`

- `queue.enqueue.timeout.ms=-1` 当消息在 producer 端沉积的条数达到 `queue.buffering.max.messages` 阻塞一定时间后，队列仍然没有 enqueue（即 producer 仍然没有发送出任何消息），此时producer可以继续阻塞，或者将消息抛弃
  - `-1` 无阻塞超时限制，消息不会被抛弃
  - `0` 立即清空队列，消息被抛弃
