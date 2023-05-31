package misc

import (
	"context"
	"fmt"

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

// # 创建消费者对象
//
// 本函数用于创建消费者对象, 并订阅指定的主题
//
// 参数:
//   - `topic`: 要订阅的主题名称
//   - `extConf`: 扩展配置项, 应包含除 `"bootstrap.servers"` 外的其它消费者配置项
//
// 返回消费者对象以及错误对象
func CreateConsumer(topic string, extConf *kafka.ConfigMap) (*kafka.Consumer, error) {
	// 创建消费者对象
	consumer, err := kafka.NewConsumer(NewConfig(extConf))
	if err != nil {
		return nil, err
	}

	// 订阅给定主题
	err = consumer.Subscribe(topic, nil)
	if err != nil {
		return nil, err
	}

	return consumer, nil
}

// # 通过指定生产者发送指定消费者的当前偏移量
//
// 在指定生产者启动事务的前提下, 事务中的消费者不能直接提交偏移量, 而应该将偏移量作为事务的一部分, 由生产者提交给集群管理器
//
// 参数:
//   - `ctx`: 异步上下文对象, 可以设置超时时间, 或者为 `context.TODO()` 设置无限等待
//   - `producer`: 启动事务的生产者对象
//   - `consumer`: 要提交偏移量的消费者对象
//
// 返回错误对象
func SendConsumerOffsetsToTransaction(ctx context.Context, producer *kafka.Producer, consumer *kafka.Consumer) error {
	// 获取消费者的所有主题分区
	tp, err := consumer.Assignment()
	if err != nil {
		return err
	}

	// 获取消费者所有主题分区的当前偏移量
	tp, err = consumer.Position(tp)
	if err != nil {
		return err
	}

	// 获取 input 消费者的群组元数据
	meta, err := consumer.GetConsumerGroupMetadata()
	if err != nil {
		return err
	}

	// 通过 output 生产者将 input 消费者的偏移量发送到 Kafka 群组
	return producer.SendOffsetsToTransaction(ctx, tp, meta)
}

// # 读取指定主题的最后 `N` 条消息
//
// 根据给定的消费者对象和消息条数 `N`, 读取该消费者订阅主题的最后 N 条消息, 返回实际数量 (`<=N`) 的消息集合
//
// 参数:
//   - `consumer`: 消费者对象
//   - `n`: 要获取的最新消息的条数
//   - `commit`: 接收完消息后是否提交偏移量
//
// 返回消息对象集合即错误对象
func PollLastNMessage(consumer *kafka.Consumer, n int, commit bool) ([]*kafka.Message, error) {
	// 创建消息切片集合
	msgs := make([]*kafka.Message, 0)

	// 用于存放到达末尾的主题分区的字典对象, Key 为主题分区组合
	eofs := make(map[string]bool)

	// 循环, 不断获取消息对象
	for stop := false; !stop; {
		// 获取一条消息, 返回结果
		ev := consumer.Poll(5000)

		// 根据结果的类型进行处理
		switch e := ev.(type) {
		case *kafka.Message: // 消息结果为 kafka.Message 类型, 表示成功获取一条消息
			// 判断消息的主题分区是否正确
			if e.TopicPartition.Error != nil {
				return nil, e.TopicPartition.Error
			}
			// 保证返回的消息数量为 N
			if len(msgs) == n {
				msgs = msgs[1:]
			}

			// 将本次取得的消息进行保存
			msgs = append(msgs, e)

			// 从 EOFs 字典中删除有消息的主题分区
			delete(eofs, fmt.Sprintf("%v|%v", *e.TopicPartition.Topic, e.TopicPartition.Partition))
		case error: // 消息结果为 error 类型, 表示发生错误
			return nil, e
		case kafka.PartitionEOF: // 消息结果为 kafka.PartitionEOF 类型, 表示一个分区到达结尾
			// 获取消费者订阅的主题和分区集合
			tps, err := consumer.Assignment()
			if err != nil {
				return nil, err
			}

			// 将以达到结尾的主题和分区存入 EOFs 字典中
			eofs[fmt.Sprintf("%v|%v", *e.Topic, e.Partition)] = true

			// 如果所有分区都到达结尾, 则退出循环
			if len(eofs) == len(tps) {
				stop = true
			}
		}
	}

	// 提交偏移量
	if commit {
		_, err := consumer.Commit()
		if err != nil {
			return nil, err
		}
	}

	return msgs, nil
}
