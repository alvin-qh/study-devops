// 创建 Kafka 生产者对象
package misc

import (
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

// 创建生产者对象
//
// 本函数用于创建生产者对象, 需传入除 "bootstrap.servers" 外的其它生产者配置项
func CreateProducer(extConf *kafka.ConfigMap) (*kafka.Producer, error) {
	// 合并默认配置项, 创建生产者对象并返回
	return kafka.NewProducer(newConfig(extConf))
}
