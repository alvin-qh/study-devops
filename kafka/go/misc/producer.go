// 创建 Kafka 生产者对象
package misc

import (
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
	return kafka.NewProducer(newConfig(extConf))
}
