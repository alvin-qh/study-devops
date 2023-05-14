# Broker 配置

- [Broker 配置](#broker-配置)
  - [1. 常规配置参数](#1-常规配置参数)
  - [2. 主题相关配置](#2-主题相关配置)
  - [3. 进阶配置](#3-进阶配置)
    - [3.1. IO 吞吐量配置](#31-io-吞吐量配置)
    - [3.2. 日志分片存储配置](#32-日志分片存储配置)
    - [3.4. Leader 和 Replicas 相关配置](#34-leader-和-replicas-相关配置)
    - [3.5. 消息偏移量相关配置](#35-消息偏移量相关配置)
    - [3.6. Zookeeper 参数配置](#36-zookeeper-参数配置)

Broker 的配置在 Kafka 按照路径的 `config` 目录下, `server.properties` 文件中, 参考官方 [Kafka 配置](https://svn.apache.org/repos/asf/kafka/site/083/configuration.html)

## 1. 常规配置参数

1. `node.id` 或 `broker.id`, 定义 Broker 的唯一 ID:

   - `node.id` 用于在 KRaft 集群模式下指定 Broker 的唯一 ID;
   - `broker.id` 用于在 Zookeeper 集群模式下指定 Broker 的唯一 ID;
   - 注意, 一个集群内的 Broker ID 必须唯一

2. `listeners`, 用于设定 Broker 的监听地址:

   - 监听地址格式为 `<protocol>://<hostname>:<port>`, 默认为 `PLAINTEXT://:9092`, 表示使用明文传输协议, 监听本地 `0.0.0.0:9092`;
   - 如需要多个监听可以使用 `,` 分隔, 例如: `PLAINTEXT://:9092,SSL://:9091`;

3. `zookeeper.connect`, 设置 Zookeeper 的链接地址:

   - 如果使用 Zookeeper 作为 Kafka 集群元数据存储, 则需要通过该配置设置 Zookeeper 服务端的地址, 如果 Zookeeper 为集群模式, 则需要通过 `,` 为分隔符连接多个 Zookeeper 服务端地址;
   - 设置格式为 `<hostname1>:<port>/<path>,<hostname2>:<port>/<path>, ...`, 其中:
     - `hostname` 表示 Zookeeper 的主机名或 IP 地址;
     - `port` 表示 Zookeeper 的地址 (默认为 `2181`);
     - `path` 表示连接 Zookeeper 的 `chroot` 路径 (应用程序根路径), 这样可以在 Zookeeper 上对不同应用进行隔离;

4. `log.dirs`, Kafka 保存消息日志的路径, 可以指定多个并用 `,` 分隔:

    - 如果设置的路径为多个, 则 Broker 会根据"最少使用"原则, 把同一个分区的消息日志保存在同一个日志路径下, 且 Broker 会向分区数量最少的日志路径新增分区日志;

5. `num.recovery.threads.per.data.dir`, 设置每个日志目录对应的线程数, 例如 `log.dirs` 参数指定了 `3` 个路径, 且 `num.recovery.threads.per.data.dir` 参数设置为 `8`, 则一共需要 `24` 个线程:

    - Kafka 使用线程池来处理消息日志, 包括如下 `3` 中情形:
      - 当服务器正常启动时, 用于打开每个分区的日志分片;
      - 当服务器发生崩溃并重启时, 用于检查和截断每个分区的日志;
      - 当服务器正常关闭时, 用于关闭打开的日志;
    - 因为这些线程只在 Broker 启动和关闭时使用, 所以可以设置也给较大值, 通过并行操作加快操作速度, 特别是对包含大量分区的 Broker

6. `auto.create.topics.enable`, 是否允许自动创建主题:

    - 默认情况下, Kafka 会在如下几种情形中自动创建主题:
      - 当一个生产者开始向主题写入消息时;
      - 当一个消费者开始从主题读取消息时;
      - 当客户端像主题发送获取元数据的请求时;
    - 如果设置 `auto.create.topics.enable` 为 `false`, 则不允许 Kafka 自动创建主题, 如果访问不存在的主题, 会返回错误. 需要通过 Kafka API 显式创建所需的主题;

7. `auto.leader.rebalance.enable`, 是否允许自动进行 Rebalance 操作:

    - 如果启用了此选项, 则或有一个后台线程定期 (通过 `leader.imbalance.check.interval.seconds` 配置指定) 检查各分区的分布情况;
    - 如果各个 Broker 的主题不均衡成都超出指定的百分比 (通过 `leader.imbalance.per.broker.percentage` 配置指定), 则会自动启动一次分区 Leader 再平衡;

8. `delete.topic.enable`, 禁止主题被随意删除;

## 2. 主题相关配置

Kafka为 新创建的主题提供了很多默认的配置参数, 可以通过管理工具为每个主题单独配置一些参数 (比如分区数和数据保留策略), 还可以将服务器提供的默认配置作为基准, 应用于集群内的大部分主题

1. `num.partitions`, 定义新创建的主题包含的分区数:

    - 当创建主题时, 该设置值作为分区的默认分区数;
    - 一个主题的分区可以增加, 但不能减少;
    - Kafka 集群通过分区对主题进行横向扩展;
    - 基本的思路是:
      - 分区数可以设置为集群 Broker 的数量 (或者是其倍数), 这样一般来说可以获得当前系统的最佳吞吐量;
      - 也可以通过增加主题数量来实现消息的负载均衡;

    > 分区数量选择依据:
    >
    > - 考虑主题的吞吐量 (`100KB/s` 还是 `1GB/s`);
    > - 单个分区的最大吞吐量 (要同时考虑生产者和消费者两端, 且通常消费者的效率要低于生产者);
    > - 要根据未来的预期使用量 (而不是当前使用量) 来估算吞吐量;
    > - 避免使用太多分区, 这会增加 "元数据" 和 Leader 选举的时间;
    > - 是否需要对分区进行镜像, 需额外考虑镜像的吞吐量. 另外, 分区过大也会影响镜像的效率;
    > - 了解虚拟机或磁盘是否有 IOPS (每秒输入输出) 限制, 另外分区数量太大也会导致 IOPS 增加;

2. `default.replication.factor`, 设定新建主题的默认复制系数:

    - 复制系数的建议值是一个至少比 `min.insync.replicas` 大 `1` 的数值;
    - 如果硬件比较富裕, 也可以设置为比 `min.insync.replicas` 大 `2` 的值, 简称 `RF++`, 可以更好的防止停机 (允许同时发生一次计划内停机和一次计划外停机), 这意味着每个分区至少有 `3` 个副本;
    - 如果发生了网络中断或者磁盘故障等原因, 需要保证至少有一个分区可用;

3. `log.retention.{hours,minutes,ms}`, 设定消息在服务器上保留的最长时间, 时间单位为"小时", "分钟"和"毫秒":

    - Kafka 通常根据配置的时间长短来决定数据可以被保留多久;
    - 通过配置 `log.retention.hours` 参数来配置时间, 默认为168小时, 也就是1周;
    - 推荐使用 `log.retention.ms`, 因为如果指定了不止一个参数, 那么 Kafka 会优先使用具有最小单位值的那个;

    > 注意: 根据时间保留数据是通过检查日志分片文件的最后修改时间来实现的:
    >
    > - 一般来说, 最后修改时间就是日志分片的关闭时间, 也就是文件中最后一条消息的时间戳;
    > - 如果使用管理工具在服务器间移动分区, 那么最后修改时间就不再准确, 这种误差可能会导致这些分区过多地保留数据;

4. `log.retention.bytes`, 设定消息在服务器上保留的大小:

    - 通过参数 `log.retention.bytes` 来指定一个 byte 数, 作为每个分区可以保留消息数据的最大值大小 (即如果该参数为 `1GB`, 共有 `8` 个分区, 意味着总共有 `8GB` 的消息数据会保留在服务器上);
    - 如果一个主题增加了分区, 则意味着该主题保留的数据量也会随之增加;
    - 如果这个值被设置为 `-1`, 那么分区就可以无限期地保留数据;

    > 注意: 如果同时指定了 `log.retention.bytes` 和 `log.retention.ms` (或另一个按时间保留的参数), 那么只要任意一个条件得到满足, 消息就会被删除
    >
    > - 假设 `log.retention.ms` 被设置为 `86400000` (也就是 `1` 天), `log.retention.bytes` 被设置为 `1000000000` (也就是 `1GB`), 如果消息字节总数不到一天就超过了 `1GB`, 那么旧数据就会被删除;
    > - 相反, 如果消息字节总数小于 `1GB`, 那么一天之后这些消息也会被删除, 尽管分区的数据总量小于 `1GB`;
    > - 建议只选择其中的一种保留策略, 或者两种都不选择, 以防发生意外的数据丢失;
    > - 对于复杂的场景, 也可以两种都使用, 但一定要妥善设计, 综合各个方面的考虑;

5. `log.segment.bytes`, 定义消息分片的最大长度:

    - 当日志分片大小达到 `log.segment.bytes` 指定的上限 (默认是 `1GB`) 时, 会创建一个新的日志分片取代当前的日志分片;
    - 一旦日志分片被关闭, 就开始进入过期倒计时;
    - 这个参数的值越小, 关闭和分配新文件就会越频繁, 从而降低整体的磁盘写入效率;
    - 如果主题的消息量不是很大, 则必须妥善设置这个参数, 例如:
      - 如果一个主题每天只接收 `100MB` 的消息, 且 `log.segment.bytes` 使用了默认设置, 那么填满一个日志分片将需要 `10` 天;
      - 日志分片被关闭之前消息是不会过期的, 所以如果 `log.retention.ms` 被设为 `604800000` (1周), 那么日志分片最多需要 `17` 天才会过期 (即关闭日志分片需要 `10` 天, 还需要再保留数据 `7` 天, 要等到日志分片的最后一条消息过期才能将其删除);

    > 注意, 日志分片的大小也会影响使用时间戳获取偏移量的行为:
    >
    > - 当使用时间戳获取日志偏移量时, Kafka 会查找在指定时间戳写入的日志分片文件, 也就是创建时间在指定时间戳之前且最后修改时间在指定时间戳之后的文件;
    > - 然后 Kafka 会返回这个日志分片开头的偏移量 (也就是文件名);

6. `log.roll.{hours,ms}`, 指定多长时间后日志分片可以被关闭, 单位可以为"小时" 以及毫秒:

    - `log.segment.bytes` 和 `log.roll.ms` 并不互斥, 日志分片会在大小或时间达到上限时被关闭, 取决于哪个条件先被满足;
    - 在默认情况下, `log.roll.ms` 没有设定值, 所以使用 `log.roll.hours` 设定的默认值 `168`小时, 即 `7` 天;

    > 注意, 基于时间的日志分片对磁盘性能的影响:
    >
    > - 在使用基于时间的日志分片时, 需要考虑并行关闭多个日志分片对磁盘性能的影响, 如果多个分区的日志分片一直未达到大小的上限, 就会出现这种情况;
    > - 这是因为 Broker 在启动时会计算日志分片的过期时间, 一旦满足条件, 就会并行关闭它们, 尽管它们的数据量可能很少;

7. `min.insync.replicas`, 指定最小同步确认数:

    - 为提升集群的持久性, 可以将 `min.insync.replicas` 设置为 `2`, 确保至少有两个副本跟生产者保持"同步";
    - 生产者需要配合将 `ack` 设置为 `all`, 这样就可以确保至少有两个副本 (Leader 和另一个 Replica) 确认写入成功，从而防止在以下情况下丢失数据:
      - 如果 Leader 确认写入, 然后发生停机, 所有权被转移到一个副本, 但这个副本没有写入成功;
      - 如果没有这些配置, 则生产者会认为已经写入成功, 但实际上消息丢失了;
    - 这样做是有副作用的, 需要额外的开销, 所以效率会有所降低;
    - 因此, 对于能够容忍偶尔消息丢失的高吞吐量集群不建议修改这个参数的默认值;

8. `message.max.bytes`, 指定单条消息的大小上限:

    - 默认值是 `1000000` (`1MB`);
    - 如果生产者尝试发送超过这个大小的消息, 会收到错误信息, 且该消息不会被处理;
    - 这个参数指的是压缩后的消息大小, 即消息的实际大小可以远大于 `message.max.bytes`, 只要压缩后小于这个值即可;
    - 这个参数对性能有显著的影响: 值越大, 负责处理网络连接和请求的线程用在处理请求上的时间就越长, 它还会增加磁盘写入块的大小, 从而影响 IO 吞吐量;

    > 注意, 消费者客户端要做对应的调整:
    >
    > - 消费者客户端设置的 `fetch.max.bytes` 要与服务器端设置的消息大小保持一致;
    > - 如果这个参数的值比 `message.max.bytes` 小, 那么消费者就无法读取比较大的消息, 进而造成阻塞, 无法继续处理消息;
    > - 在配置 Broker 的 `replica.fetch.max.bytes` 参数时也遵循同样的原则

## 3. 进阶配置

### 3.1. IO 吞吐量配置

除了前面介绍的常用配置外, 和 IO, 线程和 Socket 相关的配置还包含以下参数, 这些参数非必要无需修改

1. `num.network.threads`, 指定 Broker 处理消息的最大线程数, 默认值为 `4`;

2. `num.io.threads`, 指定 Broker 处理磁盘 IO 的线程数, 该数值应该大于你的硬盘数, 默认值为 `8`;

3. `background.threads`, 指定后台任务处理的线程数, 主要用于一些后台异步任务, 例如删除过期文件等, 默认值为 `4`;

4. `queued.max.requests`, 指定消息请求队列最大容量, 若是等待 IO 的请求超过这个数值, 那么会停止接受生产者消息, 默认值为 `500`;

5. `socket.send.buffer.bytes`, 指定 Socket 的发送缓冲区, 相当于 Socket 的调优参数 `SO_SNDBUFF`, 默认值为 `100 * 1024` (相当于 `100KB`);

6. `socket.receive.buffer.bytes` 指定 Socket 的接受缓冲区, 相当于 Socket 的调优参数 `SO_RCVBUFF`, 默认值为 `100 * 1024` (相当于 `100KB`);

7. `socket.request.max.bytes` 指定 Socket 请求的最大数值. 为防止服务端 OOM, `message.max.bytes` 的值必然要小于该配置的值, 默认值为 `100 * 1024 * 1024` (即 `10MB`);

### 3.2. 日志分片存储配置

除了前面介绍的常用配置外, 和日志, 日志分片相关的配置还包括以下参数, 这些参数非必要无需修改

1. `log.cleanup.policy`, 指定日志清理策略, 可选值包括 `delete` (删除) 和 `compact` (压缩), 默认为 `delete`;

2. `log.retention.check.interval.ms`, 指定对日志分片是否符合删除要求的检查周期时间, 默认值为 `5` 分钟;

3. `log.cleaner.enable`, 指定是否开启日志清理, 默认值为 `true`;

4. `log.cleaner.threads`, 指定日志压缩运行的线程数, 默认值为 `2`;

5. `log.cleaner.io.max.bytes.per.second`, 指定过期日志每秒清理的大小, 默认为 `None`, 表示不限制;

6. `log.cleaner.dedupe.buffer.size`, 指定日志清理去重时分配的缓存空间, 默认值为 `500 * 1024 * 1024` 字节, 越大越好;

7. `log.cleaner.io.buffer.size`, 指定日志清理时所需的 IO 块大小, 默认为 `512 * 1024` 字节;

8. `log.cleaner.backoff.ms`, 指定检查是否有日志需要清理的间隔时间, 默认 `15000` 毫秒;

9. `log.cleaner.min.cleanable.ratio`, 指定清理被压缩日志的比率, 默认为 `0.5`, 即压缩比超过 `50%` 的日志将不被清理, 这样提高了日志清理的效率, 但会导致存储空间的浪费;

10. `log.cleaner.delete.retention.ms`, 指定对于压缩的日志保留的最长时间, 也是客户端消费消息的最长时间, 同 `log.retention.minutes` 的区别在于一个控制未压缩数据, 一个控制压缩后的数据, 默认为 `1` 天. 会被 Topic 创建时的指定参数覆盖;

11. `log.index.size.max.bytes`, 指定每个日志分片索引文件可以存储 Offset (消息偏移量) 索引的大小 (单位为字节):

    - 会被 Topic 创建时的指定参数覆盖;
    - 这个设置会影响每个日志分片索引文件建立时的大小 (稀疏文件), 在日志填满后再截断到实际大小;
    - 如果日志分片索引文件的索引量达到该限制, 则无论日志是否被填满, 都会建立新的日志分片文件;
    - 默认值为 `10 * 1024 * 1024` 字节;

12. `log.index.interval.bytes`, 当执行一个获取消息操作后, 需要一定的空间来扫描最近的 Offset 的大小, 设置越大代表扫描速度越快, 但是也更耗内存, 默认值为 `4096`;

13. `log.flush.interval.messages`, 指定日志文件同步到磁盘之前累积的消息条数:

    - 磁盘 IO 操作是一个慢操作, 但又是达成"数据可靠性"的必要手段, 所以此参数的设置需要在"数据可靠性"与"性能"之间做必要的权衡;
    - 如果此值过大, 将会导致每次文件同步的时间较长 (IO 阻塞); 如果此值过小, 将会导致文件同步的次数较多, 同样会影响 IO 的性能;
    - 大部分时候, 应该使用副本来保证数据的可靠性;

14. `log.flush.scheduler.interval.ms`, 指定检查日志消息是否需要同步到硬盘的时间间隔;

15. `log.flush.interval.ms`, 指定两次日志文件同步的时间间隔, 将和 `log.flush.interval.messages` 配置共同起作用, 任意一个条件满足都会将日志同步到磁盘;

16. `log.delete.delay.ms`, 指定文件在索引中清除后保留的时间;

17. `log.flush.offset.checkpoint.interval.ms`, 检查日志最后一个同步点的频率, 用于进行数据恢复;

### 3.4. Leader 和 Replicas 相关配置

除了前面介绍的常用配置外, 和每个分区 Leader 及其 Replicas 相关的还有如下配置, 一般情况下无需修改

1. `controller.socket.timeout.ms`, 指定一个分区中控制节点 (Leader) 和受控节点 (Follower) 之间 Socket 通信超时时间, 默认值 `30000` 毫秒;

2. `controller.message.queue.size`, 指定分区中控制节点 (Leader) 用于处理 Socket 请求的队列 (`controller-to-broker-channels`) 的长度, 默认不限制长度;

3. `replica.lag.time.max.ms`, 指定在分区中, 记录副本响应 Leader 的最长等待时间, 一旦超过这个时间, 就会将该 Follower 从同步列表 `ISR` (In-Sync Replicas) 中删除, 并不再进行管理;

4. `replica.lag.max.messages`, 指定 Follower 中的副本消息的消息索引落后 Leader 中消息的最大值:

   - 通常，在 Replica 与 Leader 通讯时, 因为网络延迟或者链接断开, 总会导致 Replica 中消息同步滞后;
   - 如果 Replica 落后与 Leader 太多 (超出此阈值), 将会认为此 Replica 已经失效;
   - 如果消息滞后太多, Leader 将认为此 Follower 节点的网络延迟较大或者消息吞吐能力有限, 将会把其上的副本内容转移到其它节点上;
   - 在 Broker 数量较少, 或者网络不佳的环境中, 可以提高此值以避免太多的 Replica 失效;

5. `replica.socket.timeout.ms`, 指定 Follower 在获取副本时, 与 Leader 之间的 Socket 通讯超时时间, 默认值 `30000` 毫秒;

6. `replica.socket.receive.buffer.bytes`, 指定在进行副本消息传递时 Socket 的缓存大小, 默认 `64 * 1024` 字节;

7. `replica.fetch.max.bytes`, 指定副本复制时, 每次获取消息的最大字节数, 默认 `1024 * 1024` 字节;

8. `replica.fetch.wait.max.ms`, 指定副本复制时, 每次获取消息的最大的等待时间, 默认 `500` 毫秒;

9. `replica.fetch.min.bytes`, 指定副本复制时, 每次获取消息的最小字节数, 如果此时 Leader 中的数据小于此设置字节数, 则会进入等待, 直到 Leader 发送大于此设置字节数的数据, 默认值为 `1` 字节;

10. `num.replica.fetchers`, 指定用于复制副本的线程数, 增加这个值会提升 Follower 节点的 IO 并发度, 默认值为 `1`;

11. `replica.high.watermark.checkpoint.interval.ms`, 指定每个副本将 HW (High Watermark) 记录到磁盘 (用于数据恢复) 的频率, 默认 `5000` 毫秒;

12. `controlled.shutdown.enable`, 指定是否允许控制节点关闭 Broker, 如果允许, 则 Broker 会在关闭前将其上的所有 Leader 转移到其它 Broker 上, 这样可以减少关机过程中的服务不可用窗口期, 默认值为 `true`;

13. `controlled.shutdown.max.retries`, 指定控制节点再关闭 Broker 的过程中最大的重试次数, 如果超过重试次数后仍无法完成关闭, 则会进行 unclear 方式关闭, 默认为 `3`;

14. `controlled.shutdown.retry.backoff.ms`, 指定控制节点每次尝试关闭 Broker 的间隔时间, 默认为 `5000` 毫秒;

15. `leader.imbalance.per.broker.percentage`, 指定一个分区中, 每个 Broker 记录消息所允许的不均衡百分比, 超过这个值后, Leader 将会对所有 Broker 进行重新均衡消息的操作;

16. `leader.imbalance.check.interval.seconds`, 指定 Leader 去检查各个 Broker 消息不均衡情况的时间间隔, 默认 `300` 秒;

17. `offset.metadata.max.bytes`, 指定允许客户端保存其偏移量的最大元数据量, 默认值 `4096` 字节;

18. `max.connections.per.ip`, 指定 Broker 的一个 IP 地址允许的最大连接数, 默认值 `Int.MaxValue`;

19. `max.connections.per.ip.overrides`, 指定针对指定 IP (或主机名) 设置最大连接数, 可以配置多个, 用逗号隔开, 例如: `127.0.0.1:200,192.168.0.121:100`, 该配置会覆盖 `max.connections.per.ip` 的配置值;

20. `connections.max.idle.ms`, 连接的空闲超时时间, 如果也给连接空闲时间超过该配置值, 则关闭此连接, 默认值 `600000` 毫秒;

21. `log.roll.jitter.{hours,ms}`, 指定对日志分片滚动操作的时间抖动值, 时间单位为"小时"或"毫秒", 默认值为 `0`:

    - 如果设置了以时间来滚动日志分片, 即: `log.roll.{ms,hours}` 配置项, 则在指定的时间点上, 所有正在使用的日志分片都会进行滚动操作 (关闭当前分片, 建立新分片), 这会对系统 IO 带来比较大的压力;
    - 该设置值就是将日志滚动时间点做随机的提前和延后, 即抖动, 从而保证系统 IO 的平顺性;

22. `num.recovery.threads.per.data.dir`, 指定启动时用于日志恢复以及关闭时用于将数据写入磁盘的的线程数, 该数值是针对每个数据目录的 (日志目录数量由 `log.dirs` 配置项指定), 默认值 `1`;

23. `unclean.leader.election.enable`, 默认值 `true`, 指定是否允许不在 ISR 中的副本在没有 Leader 可用的情况下选举为 Leader (即可用的 Broker 都已经宕机), 尽管这样做可能会导致数据丢失. 默认值为 `true`;

### 3.5. 消息偏移量相关配置

1. `offsets.topic.num.partitions`, 指定一个主题允许的能够提交偏移量的分区数, 默认值 `50`;

   > 该配置项是少数几个不能在 Kafka 运行过程中修改的参数, 所以最好设置足够大的值, 例如 `100-200`;

2. `offsets.topic.retention.minutes`, 设定一个时间值 (单位分钟), 保留时间超过此值的偏移量将被标记为删除 (实际的删除工作会在清理日志分片的时候进行), 默认值 `1440`;

3. `offsets.retention.check.interval.ms`, 检查失效偏移量的时间间隔, 默认 `600000` 毫秒;

4. `offsets.topic.replication.factor`, 指定主题的偏移量提交的复制因子 (最大值), 以确保更高的可用性. 如果 Broker 的数量小于复制因子, 则按照 Broker 数量使用较小的复制因子. 默认 `3`;

5. `offsets.topic.segment.bytes`, 默认 `104857600`, 主题日志分片的最大大小. 如果该主题使用日志压缩, 则该值可以设置的较小, 以确保更快的压缩

6. `offsets.load.buffer.size`, 指定偏移量加载缓存的大小, 默认值 `5242880` 字节:
   - 当一个消费组中某个 Broker 称为偏移量管理者 (例如一个 Broker 变更为某个主题分区的 Leader), 即会发生偏移量加载操作, 将偏移量加载到偏移量管理器中;
   - 偏移量管理器是按批次加载偏移量的, 该设置值定义了这个批次的大小;

7. `offsets.commit.required.acks`, 偏移量提交之前要进行确认的次数, 默认值为 `-1`;

8. `offsets.commit.timeout.ms`, 偏移量提交延迟时间, 默认值 `5000` 毫秒:

   - 偏移量在整体提交前, 要确认副本已经接收到要提交的偏移量值;
   - 如果在该配置指定的超时时间内为完成副本偏移量的提交, 则偏移量提交失败;

9. `inter.broker.protocol.version`, Kafka 内部通信协议的版本, 默认值 `0.8.3`;

10. `offsets.retention.minutes`, 指定保留偏移量的最长时间:

    - 当一个消费者群组失去了所有成员, Kafka 会根据此设置保存各分区消息偏移量;
    - 在指定时间内, 如果消费者重新加入群组, 则再平衡后, 各消费者会根据偏移量保存结果继续获取之后的消息;
    - 超出指定时间后, 即使消费者重新加入群组, 也会如同一个新消费群组, 没有之前读取的偏移量记录;

### 3.6. Zookeeper 参数配置

1. `zookeeper.session.timeout.ms`, 表示 Zookeeper 的最大超时时间, 即心跳的间隔. 若是在超时时间内没有响应, 则认为 Zookeeper 已经宕机, 这个值不宜过大, 默认为 `6000` 毫秒;

2. `zookeeper.connection.timeout.ms`, 表示 Zookeeper 的连接超时时间, 默认 `6000` 毫秒;

3. `zookeeper.sync.time.ms`, 表示 Zookeeper 集群中 Leader 和 Follower 之间的同步时间, 默认 `2000` 毫秒;
