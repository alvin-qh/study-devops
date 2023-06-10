// 创建 Kafka 生产者对象
package misc

import (
	"errors"

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

// # 创建生产者对象
//
// 参数:
//   - `extConf`: 扩展配置项, 应包含除 `"bootstrap.servers"` 外的其它消费者配置项
//
// 返回生产者对象和错误信息
func CreateProducer(extConf *kafka.ConfigMap) (*kafka.Producer, error) {
	// 合并默认配置项, 创建生产者对象并返回
	return kafka.NewProducer(NewConfig(extConf))
}

// # 等待生产者发送消息结果
//
// 参数:
//   - `ch`: 生产者发送消息时使用的频道
//
// 返回已发送消息对象和错误信息
func WaitProduceResult(ch chan kafka.Event) (*kafka.Message, error) {
	// 等待生产者完成消息发送
	ev := <-ch
	switch e := ev.(type) {
	case *kafka.Message:
		return e, nil
	case error:
		return nil, e
	default:
		return nil, errors.New("invalid event type")
	}
}
