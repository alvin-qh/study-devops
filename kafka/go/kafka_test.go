// 测试 Kafka 操作
package main

import (
	"kafka/misc"
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
	msgs, err := misc.PollLastNMessage(consumer, 1)
	assert.NoError(t, err)

	// 确认获取的消息即为发送的消息
	assert.Len(t, msgs, 1)
	assert.Equal(t, *msgs[0].TopicPartition.Topic, topicName)
	assert.Equal(t, msgs[0].Key, msg.Key)
	assert.Equal(t, msgs[0].Value, msg.Value)
}
