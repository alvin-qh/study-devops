# 消费者配置

- `group.id=group1` consumer 归属的组 id，broker 是根据 `group.id` 来判断是队列模式还是发布订阅模式，非常重要

- `consumer.id=1` 消费者的 id，若是没有设置的话，会自增

- `client.id=group1` 一个用于跟踪调查的 id，最好同 `group.id` 相同

- `zookeeper.connect=localhost:2182` 对于 zookeeper 集群的指定，可以是多个，例如：`hostname1:port1,hostname2:port2,hostname3:port3`。必须和 broker 使用同样的 zookeeper 配置

- `bootstrap.servers` 指定 Kafka 集群的地址。对于新版 Kafka，用以取代 `zookeeper.connect` 配置

- `zookeeper.session.timeout.ms=6000` zookeeper 的心跳超时时间，查过这个时间就认为是 dead 消费者

- `zookeeper.connection.timeout.ms=6000` zookeeper 的等待连接时间

- `zookeeper.sync.time.ms=2000` zookeeper 的 follower 同 leader 的同步时间

- `auto.offset.reset=largest` 当 zookeeper 中没有初始的 offset 时候的处理方式
  - `smallest` 重置为最小值
  - `largest` 重置为最大值

- `socket.timeout.ms=30*1000` socket 的超时时间，实际的超时时间是 `max.fetch.wait + socket.timeout.ms`

- `socket.receive.buffer.bytes=64*1024` socket 接收缓存空间大小

- `fetch.message.max.bytes=1024*1024` 从每个分区获取的消息大小限制

- `auto.commit.enable=true` 是否在消费消息后将 offset 同步到 zookeeper，当 consumer 失败后就能从 zookeeper 获取最新的 offset

- `auto.commit.interval.ms=60*1000` 自动提交的时间间隔

- `queued.max.message.chunks=10` 用来处理消费消息的块，每个块可以等同于 `fetch.message.max.bytes` 中数值

- `rebalance.max.retries=4` 当有新的 consumer 加入到 group 时,将会 rebalance，此后将会有partitions 的消费端迁移到新的 consumer 上，如果一个 consumer 获得了某个 partition 的消费权限，那么它将会向 zookeeper 注册 **Partition Owner registry** 节点信息，但是有可能此时旧的 consumer 尚没有释放此节点。此值用于控制注册节点的重试次数

- `rebalance.backoff.ms=2000` 每次再平衡的时间间隔

- `refresh.leader.backoff.ms` 每次重新选举 leader 的时间

- `fetch.min.bytes=1` server 发送到消费端的最小数据，若是不满足这个数值则会等待，知道满足数值要求

- `fetch.wait.max.ms=100` 若是不满足最小大小（`fetch.min.bytes`）的话，等待消费端请求的最长等待时间

- `consumer.timeout.ms=-1` 指定时间内没有消息到达就抛出异常，一般不需要修改
