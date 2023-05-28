// 测试 Kafka 操作
package main

import (
	"context"
	"fmt"
	"kafka/misc"
	"os"
	"testing"

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

	// 从频道中获取消息发送结果
	ev := <-ch
	// 根据发送结果的类型分别处理
	switch e := ev.(type) {
	case *kafka.Message:
		// 结果为 *kafka.Message 类型时, 确认发送消息
		assert.Equal(t, *e.TopicPartition.Topic, topicName)
		assert.GreaterOrEqual(t, e.TopicPartition.Partition, int32(0))
		assert.Equal(t, e.Key, msg.Key)
		assert.Equal(t, e.Value, msg.Value)
	case error:
		// 结果为错误对象
		assert.Fail(t, e.Error())
	}

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

	// 将消息发送到 input 主题
	err = inputProducer.Produce(inputMsg, inputCh)
	assert.NoError(t, err)

	// 等待 input 生产者完成消息发送
	ev := <-inputCh
	switch e := ev.(type) {
	case error:
		assert.Fail(t, e.Error())
	}

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

	// 为 output 生产者创建频道
	outputCh := make(chan kafka.Event, len(inputMsgs))

	// 遍历从 input 消费者获取的消息, 进行 Transfer 后通过 output 生产者发送到 output 主题中
	for _, im := range inputMsgs {
		// 确认从 input 消费者读取的消息和通过 input 生产者发送的消息一致
		assert.Equal(t, *im.TopicPartition.Topic, inputTopicName)
		assert.Equal(t, im.Key, inputMsg.Key)
		assert.Equal(t, im.Value, inputMsg.Value)

		// 对 input 消费者读取的消息进行转换处理
		outputMsg := kafka.Message{
			TopicPartition: kafka.TopicPartition{
				Topic:     &outputTopicName,
				Partition: kafka.PartitionAny,
			},
			Key:   []byte(fmt.Sprintf("%v-transformed", string(im.Key))),
			Value: []byte(fmt.Sprintf("%v-transformed", string(im.Value))),
		}

		// 将处理后的消息通过 output 生产者发送到 output 主题上
		if err = outputProducer.Produce(&outputMsg, outputCh); err != nil {
			// 出现错误, 回滚事务
			outputProducer.AbortTransaction(context.TODO())
			assert.Fail(t, err.Error())
		}
	}

	// 等待 output 生产者完成消息发送
	ev = <-outputCh
	switch e := ev.(type) {
	case error:
		assert.Fail(t, e.Error())
	}

	// 获取 input 消费者的所有主题分区
	tp, err := inputConsumer.Assignment()
	assert.NoError(t, err)

	// 获取 input 消费者所有主题分区的当前偏移量
	tp, err = inputConsumer.Position(tp)
	assert.NoError(t, err)

	// 获取 input 消费者的群组元数据
	meta, err := inputConsumer.GetConsumerGroupMetadata()
	assert.NoError(t, err)

	// 通过 output 生产者将 input 消费者的偏移量发送到 Kafka 群组
	err = outputProducer.SendOffsetsToTransaction(context.TODO(), tp, meta)
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
	assert.Equal(t, *outputMsgs[0].TopicPartition.Topic, outputTopicName)
	assert.Equal(t, outputMsgs[0].Key, []byte(fmt.Sprintf("%v-transformed", string(inputMsg.Key))))
	assert.Equal(t, outputMsgs[0].Value, []byte(fmt.Sprintf("%v-transformed", string(inputMsg.Value))))
}
