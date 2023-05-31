// 测试 Kafka 操作
package main

import (
	"context"
	"fmt"
	"kafka/misc"
	"os"
	"testing"
	"time"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/stretchr/testify/assert"
)

// 测试创建主题
func TestTopic(t *testing.T) {
	topicName := "go-test__simple-topic"

	// 创建主题, 使用默认的分区和副本数
	result, err := misc.CreateTopic(topicName)
	assert.NoError(t, err)

	// 确认主题创建成功
	assert.Equal(t, result.Topic, topicName)
	assert.True(t, misc.IsTopicCreated(result))
}

// 测试生产者发送消息
func TestProducerProduce(t *testing.T) {
	topicName := "go-test__simple-topic"
	groupId := "py-test__simple-group"

	// 创建主题
	misc.CreateTopic(topicName)

	// 创建生产者对象
	producer, err := misc.CreateProducer(nil)
	assert.NoError(t, err)

	// 在函数结束前关闭生产者对象
	defer producer.Close()

	// 生成一个消息对象
	msg, err := misc.GenerateMessage(topicName)
	assert.NoError(t, err)

	// 创建 kafka.Event 类型的频道对象, 用于监听消息发送成功事件
	ch := make(chan kafka.Event)

	// 发送生成的消息
	err = producer.Produce(msg, ch)
	assert.NoError(t, err)

	rMsg, err := misc.WaitProduceResult(ch)
	assert.NoError(t, err)
	assert.Equal(t, *rMsg.TopicPartition.Topic, topicName)
	assert.GreaterOrEqual(t, rMsg.TopicPartition.Partition, int32(0))
	assert.Equal(t, rMsg.Key, msg.Key)
	assert.Equal(t, rMsg.Value, msg.Value)

	// 创建生产者对象
	consumer, err := misc.CreateConsumer(topicName, &kafka.ConfigMap{
		"group.id":             groupId,    // 消费者分组 ID
		"enable.auto.commit":   false,      // 禁止自动提交偏移量
		"auto.offset.reset":    "earliest", // 从服务端记录的最早的偏移量开始读取
		"enable.partition.eof": true,       // 允许读取分区 EOF 消息
	})
	assert.NoError(t, err)

	// 在函数结束前关闭消费者对象
	defer consumer.Close()

	// 获取最新的一条消息
	msgs, err := misc.PollLastNMessage(consumer, 1, true)
	assert.NoError(t, err)

	// 确认获取的消息即为发送的消息
	assert.Len(t, msgs, 1)
	assert.Equal(t, *msgs[0].TopicPartition.Topic, topicName)
	assert.Equal(t, msgs[0].Key, msg.Key)
	assert.Equal(t, msgs[0].Value, msg.Value)
}

// 测试事务
//
// # Kafka 事务可以保证生产者发送的消息, 无论写入多少个分区, 都能在事务提交后同时生效, 在事务取消 (回滚) 后同时失效
//
// 通过事务可以完成 "consume-transform-produce" 模式, 即 "消费-转化-再生产". 这种模式可以从一个"输入主题"读取消息,
// 对消息进行加工后, 写入到输出主题, 供另一个消费者消费
//
// 几个要点包括:
//
// 1. 启动事务的生产者要设置 `transactional.id` 配置项, 指定事务的 ID. 如果多个生产者使用同一个
// `transactional.id`, 则先加入集群的生产者会抛出异常; 所以, 当一个生产者出现故障离开集群, 则新开启的具备相同
// `transactional.id` 的生产者将完全将其取代, 之前的事务会提交或取消;
//
// 2. 生产者设置 `transactional.id` 后, `enable.idempotence`, `retries` 以及 `acks` 这几个设置项会自动设置
// 为 `true`, `max` 以及 `all`, 分别表示: 开启"精确一致性", 无限重试以及需要所有副本的响应, 这是为了保障一条消息一
// 定会被成功写入, 所以设置了 `transactional.id` 后, 可以不设置这几个设置项, 但如果设置错误, 则会抛出异常;
//
// 3. 消费者和事务的关系不大, 并不会因为有了事务就能保证消费者读取所有消息, 但消费者的 `isolation.level` 配置项可以
// 指定事务的隔离级别, 包括: `read_committed` 和 `read_uncommitted`, 前者表示事务提交前, 消费者无法消费事务中发
// 送的消息;
//
// 4. 在事务中不能由消费者提交偏移量, 因为这种方式并不能将偏移量提交给所有节点, 而必须通过生产者的
// `send_offsets_to_transaction` 方法将消费偏移量提交给事务控制器
func TestTransaction(t *testing.T) {
	inputTopicName := "go-test__ctp-input-topic"
	outputTopicName := "go-test__ctp-output-topic"
	inputGroupId := "go-test__ctp-input-group"
	outputGroupId := "go-test__ctp-output-group"

	// 创建输入数据主题, 即 input 主题
	misc.CreateTopic(inputTopicName)

	// 创建输出数据主题, 即 output 主题
	misc.CreateTopic(outputTopicName)

	// 创建向 input 主题写入消息的生产者
	inputProducer, err := misc.CreateProducer(nil)
	assert.NoError(t, err)

	// 在函数结束前关闭 input 生产者对象
	defer inputProducer.Close()

	// 创建发送到 input 分区的消息对象
	inputMsg, err := misc.GenerateMessage(inputTopicName)
	assert.NoError(t, err)

	// 为 input 生产者创建频道
	inputCh := make(chan kafka.Event)

	// 发送 input 消息
	err = inputProducer.Produce(inputMsg, inputCh)
	assert.NoError(t, err)

	// 等待 input 消息发送完毕
	_, err = misc.WaitProduceResult(inputCh)
	assert.NoError(t, err)

	// 获取当前及其的主机名作为事务 ID
	hostname, err := os.Hostname()
	assert.NoError(t, err)

	// 创建 output 生产者
	outputProducer, err := misc.CreateProducer(&kafka.ConfigMap{
		"transactional.id": hostname,
		// "enable.idempotence": true,  // 开启精确一次性, 使用事务时必须设置
		// "acks":               "all", // 要求所有分区副本回执, 使用事务时必须设置
	})
	assert.NoError(t, err)

	// 在函数结束前关闭 output 生产者
	defer outputProducer.Close()

	// 创建 input 消费者, 订阅 input 主题
	inputConsumer, err := misc.CreateConsumer(inputTopicName, &kafka.ConfigMap{
		"group.id":             inputGroupId, // 消费者分组 ID
		"enable.auto.commit":   false,        // 禁止自动提交偏移量
		"auto.offset.reset":    "earliest",   // 从服务端记录的最早的偏移量开始读取
		"enable.partition.eof": true,         // 允许读取分区 EOF 消息
	})
	assert.NoError(t, err)

	// 在函数结束前关闭 input 消费者
	defer inputConsumer.Close()

	// 为 output 生产者初始化事务
	err = outputProducer.InitTransactions(context.TODO())
	assert.NoError(t, err)

	// 在 output 生产者上启动事务
	err = outputProducer.BeginTransaction()
	assert.NoError(t, err)

	// 从 input 主题中获取最新的一条消息
	inputMsgs, err := misc.PollLastNMessage(inputConsumer, 1, false)
	if err != nil {
		// 出现错误, 回滚事务
		outputProducer.AbortTransaction(context.TODO())
		assert.Fail(t, err.Error())
	}
	assert.Len(t, inputMsgs, 1)

	// 确认从 input 消费者读取的消息和通过 input 生产者发送的消息一致
	assert.Equal(t, *(inputMsgs[0].TopicPartition.Topic), inputTopicName)
	assert.Equal(t, inputMsgs[0].Key, inputMsg.Key)
	assert.Equal(t, inputMsgs[0].Value, inputMsg.Value)

	// 对 input 消费者读取的消息进行转换处理
	outputMsg := kafka.Message{
		TopicPartition: kafka.TopicPartition{
			Topic:     &outputTopicName,
			Partition: kafka.PartitionAny,
		},
		Key:   []byte(fmt.Sprintf("%v-transformed", string(inputMsgs[0].Key))),
		Value: []byte(fmt.Sprintf("%v-transformed", string(inputMsgs[0].Value))),
	}

	// 为 output 生产者创建频道
	outputCh := make(chan kafka.Event)

	// 将处理后的消息通过 output 生产者发送到 output 主题上
	if err = outputProducer.Produce(&outputMsg, outputCh); err != nil {
		// 出现错误, 回滚事务
		outputProducer.AbortTransaction(context.TODO())
		assert.Fail(t, err.Error())
	}

	// 等待 output 生产者完成消息发送
	_, err = misc.WaitProduceResult(outputCh)
	assert.NoError(t, err)

	// 在事务中提交 input 消费者的偏移量
	err = misc.SendConsumerOffsetsToTransaction(
		context.TODO(),
		outputProducer,
		inputConsumer,
	)
	assert.NoError(t, err)

	// 提交事务
	err = outputProducer.CommitTransaction(context.TODO())
	assert.NoError(t, err)

	// 创建 output 消费者, 订阅 output 主题
	outputConsumer, err := misc.CreateConsumer(outputTopicName, &kafka.ConfigMap{
		"group.id":             outputGroupId,    // 消费者分组 ID
		"enable.auto.commit":   false,            // 禁止自动提交偏移量
		"auto.offset.reset":    "earliest",       // 从服务端记录的最早的偏移量开始读取
		"enable.partition.eof": true,             // 允许读取分区 EOF 消息
		"isolation.level":      "read_committed", // 设置事务隔离级别
	})
	assert.NoError(t, err)

	// 在函数结束前关闭 output 消费者
	defer outputConsumer.Close()

	// 通过 output 消费者从 output 主题中读取最新的 1 条消息
	outputMsgs, err := misc.PollLastNMessage(outputConsumer, 1, true)
	assert.NoError(t, err)

	// 确认消息为从 output 生产者发送的消息
	assert.Len(t, outputMsgs, 1)
	assert.Equal(t, *(outputMsgs[0].TopicPartition.Topic), outputTopicName)
	assert.Equal(t, outputMsgs[0].Key, []byte(fmt.Sprintf("%v-transformed", string(inputMsg.Key))))
	assert.Equal(t, outputMsgs[0].Value, []byte(fmt.Sprintf("%v-transformed", string(inputMsg.Value))))
}

// # 移动消费者偏移量
//
// 移动消费者偏移量可以重新获取到"之前"的消息, 几个注意点:
//  1. 创建消费者时, `auto.offset.reset` 配置项要设置为 `earliest`;
//  2. 消费者的 `seek` 方法参数为 `TopicPartition` 对象, 即偏移量的移动是针对指定主题和分区的;
//  3. 偏移量提交并不包含发生移动的偏移量;
func TestSeekConsumerOffset(t *testing.T) {
	topicName := "py-test__seek-topic"
	groupId := "py-test__seek-group"

	// 创建主题
	_, err := misc.CreateTopic(topicName)
	assert.NoError(t, err)

	// 创建生产者对象
	producer, err := misc.CreateProducer(nil)
	assert.NoError(t, err)

	// 在函数结束前关闭生产者对象
	defer producer.Close()

	// 创建一条消息对象
	msg, err := misc.GenerateMessage(topicName)
	assert.NoError(t, err)

	// 创建生产者频道
	ch := make(chan kafka.Event)

	// 生产者发送消息
	err = producer.Produce(msg, ch)
	assert.NoError(t, err)

	// 等待生产者发送消息成功
	_, err = misc.WaitProduceResult(ch)
	assert.NoError(t, err)

	// 创建消费者
	consumer, err := misc.CreateConsumer(topicName, &kafka.ConfigMap{
		"group.id":             groupId,    // 消费者分组 ID
		"enable.auto.commit":   false,      // 禁止自动提交偏移量
		"auto.offset.reset":    "earliest", // 从服务端记录的最早的偏移量开始读取
		"enable.partition.eof": true,       // 允许读取分区 EOF 消息
	})
	assert.NoError(t, err)

	// 在函数结束前关闭消费者对象
	defer consumer.Close()

	// 通过消费者获取最新一条消息
	msgs, err := misc.PollLastNMessage(consumer, 1, true)
	assert.NoError(t, err)
	assert.Len(t, msgs, 1)

	// 确认消费者获取的消息为生产者发送的消息
	assert.Equal(t, *(msgs[0].TopicPartition.Topic), topicName)
	assert.Equal(t, msgs[0].Key, msg.Key)
	assert.Equal(t, msgs[0].Value, msg.Value)

	// 将消费者偏移量移动到前一条消息
	err = consumer.Seek(msgs[0].TopicPartition, 5000)
	assert.NoError(t, err)

	// 令消费者再次获取消息, 注意, 此次获取消息不能在提交偏移量
	rMsg, err := consumer.ReadMessage(5 * time.Second)
	assert.NoError(t, err)

	// 确认消费者获取到了上次获取过的消息
	assert.Equal(t, *(rMsg.TopicPartition.Topic), topicName)
	assert.Equal(t, rMsg.Key, msg.Key)
	assert.Equal(t, rMsg.Value, msg.Value)
}

// # 群组固定成员
//
// 默认情况下, 群组成员是动态的, 即一个群组成员离开或加入群组, 都会导致消费组再平衡, 即重新为每个消费者重新分配分区,
// 这个过程会导致长时间等待
//
// 通过消费者的 `group.instance.id` 配置项可以指定一个消费者在群组中的"固定"标识, 当一个消费者离开群组, 稍后又以
// 相同的 `group.instance.id` 加入回群组时, 群组无需进行再平衡, 而会直接把原本该消费者负责的分区重新分配给该消费者
//
// 通过 `session.timeout.ms` 参数可以指定群组的固定成员离开多久后, 群组会进行再平衡, 将该分区交给其它消费者处理,
// 另外 `heartbeat.interval.ms` 则是消费者发送心跳包的时间间隔, 必须小于 `session.timeout.ms` 设置的时间
//
// 该测试将 `session.timeout.ms` 设置为 3 分钟, 所以观察 Kafka 日志, 在 3 分钟内, 执行该测试, 不会发生消费者离
// 开或加入群组的操作, 同时也不会发生群组再平衡
func TestGroupRegularMember(t *testing.T) {
	topicName := "py-test__regular-member-topic"
	groupId := "py-test__regular-member-group"

	// 创建主题
	_, err := misc.CreateTopic(topicName)
	assert.NoError(t, err)

	// 创建生产者对象
	producer, err := misc.CreateProducer(nil)
	assert.NoError(t, err)

	// 在函数结束前关闭生产者对象
	defer producer.Close()

	// 创建一条消息对象
	msg, err := misc.GenerateMessage(topicName)
	assert.NoError(t, err)

	// 创建生产者频道
	ch := make(chan kafka.Event)

	// 生产者发送消息
	err = producer.Produce(msg, ch)
	assert.NoError(t, err)

	// 等待生产者发送消息成功
	_, err = misc.WaitProduceResult(ch)
	assert.NoError(t, err)

	// 创建消费者, 设置固定群组 ID
	consumer, err := misc.CreateConsumer(topicName, &kafka.ConfigMap{
		"group.id":              groupId,                                // 消费者分组 ID
		"group.instance.id":     "0bb6da93-7f59-4ff4-aea2-0e8348252d4f", // 固定群组 ID
		"session.timeout.ms":    180000,                                 // 会话超时时间
		"heartbeat.interval.ms": 18000,                                  // 心跳发送时间间隔
		"enable.auto.commit":    false,                                  // 禁止自动提交偏移量
		"auto.offset.reset":     "earliest",                             // 从服务端记录的最早的偏移量开始读取
		"enable.partition.eof":  true,                                   // 允许读取分区 EOF 消息
	})
	assert.NoError(t, err)

	// 在函数结束前关闭消费者对象
	defer consumer.Close()

	// 通过消费者获取最新一条消息
	msgs, err := misc.PollLastNMessage(consumer, 1, true)
	assert.NoError(t, err)
	assert.Len(t, msgs, 1)

	// 确认消费者获取的消息为生产者发送的消息
	assert.Equal(t, *(msgs[0].TopicPartition.Topic), topicName)
	assert.Equal(t, msgs[0].Key, msg.Key)
	assert.Equal(t, msgs[0].Value, msg.Value)
}

// # 独立消费者
//
// 所谓独立消费者, 即不直接进行主题订阅, 而是为该消费者直接分配确定的主题和分区, 有如下特点:
//  1. 固定消费者直接从指定主题的分区获取消息, 该组不再进行再平衡操作;
//  2. 和固定消费者同组的消费者均必须为固定消费者 (不能发生主题订阅), 且各自关联的主题分区不能交叉;
func TestAssignSpecifiedPartition(t *testing.T) {
	topicName := "py-test__assign-topic"
	groupId := "py-test__assign-group"

	// 创建主题
	_, err := misc.CreateTopic(topicName)
	assert.NoError(t, err)

	// 创建生产者对象
	producer, err := misc.CreateProducer(nil)
	assert.NoError(t, err)

	// 在函数结束前关闭生产者对象
	defer producer.Close()

	// 创建生产者频道
	ch := make(chan kafka.Event)

	// 记录已发送消息的字典
	partitionMsgs := make(map[int32]*kafka.Message)

	// 向各个分区发送消息
	for partition := int32(0); partition < 3; partition++ {
		msg, err := misc.GenerateMessage(topicName, misc.WithPartition(partition))
		assert.NoError(t, err)

		err = producer.Produce(msg, ch)
		assert.NoError(t, err)

		// 等待生产者发送消息成功
		_, err = misc.WaitProduceResult(ch)
		assert.NoError(t, err)

		// 记录已发送消息
		partitionMsgs[partition] = msg
	}

	// 创建消费者对象, 注意, 这里不能对主题进行订阅
	consumer, err := kafka.NewConsumer(misc.NewConfig(&kafka.ConfigMap{
		"group.id":             groupId,    // 消费者分组 ID
		"enable.auto.commit":   false,      // 禁止自动提交偏移量
		"auto.offset.reset":    "earliest", // 从服务端记录的最早的偏移量开始读取
		"enable.partition.eof": true,       // 允许读取分区 EOF 消息
	}))
	assert.NoError(t, err)

	// 在函数结束前关闭消费者对象
	defer consumer.Close()

	// 为消费者分配指定主题的 1, 2 两个分区, 0 分区不消费
	// 注意: Offset 要设置为 kafka.OffsetStored, 表示使用服务端存储的偏移量
	err = consumer.Assign([]kafka.TopicPartition{
		{
			Topic:     &topicName,
			Partition: 1,
			Offset:    kafka.OffsetStored,
		},
		{
			Topic:     &topicName,
			Partition: 2,
			Offset:    kafka.OffsetStored,
		},
	})
	assert.NoError(t, err)

	// 读取最新的 3 条消息
	msgs, err := misc.PollLastNMessage(consumer, 3, true)
	assert.NoError(t, err)

	// 确认只能读取到 2 条消息, 因为消费者只分配了两个分区
	assert.Len(t, msgs, 2)

	// 确认获取的消息不包含 0 分区消息
	assert.NotContains(t, [...]int32{msgs[0].TopicPartition.Partition, msgs[1].TopicPartition.Partition}, 0)

	// 确认接收的消息即发送的消息
	pm := partitionMsgs[msgs[0].TopicPartition.Partition]
	assert.Equal(t, *(msgs[0].TopicPartition.Topic), topicName)
	assert.Equal(t, msgs[0].Key, pm.Key)
	assert.Equal(t, msgs[0].Value, pm.Value)

	pm = partitionMsgs[msgs[1].TopicPartition.Partition]
	assert.Equal(t, *(msgs[1].TopicPartition.Topic), topicName)
	assert.Equal(t, msgs[1].Key, pm.Key)
	assert.Equal(t, msgs[1].Value, pm.Value)
}
