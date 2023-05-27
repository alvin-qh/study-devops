// 创建 Kafka 客户端对象的配置项
// 创建 AdminClient, Producer 以及 Consumer 等客户端对象时, 需要提供配置项
package misc

import "github.com/confluentinc/confluent-kafka-go/kafka"

// 公共配置项
//
// 公共配置项指的是所有 Kafka 客户端对象在创建时都需要包含的配置项,
//
// 配置项包含在 kafka.ConfigMap 类型的对象中, 该类型相当于一个 map[string]interface{} 的字典类型
//
// 默认配置项主要包括:
//
// - "bootstrap.servers", 指定 Kafka Broker 集群的地址
var commonConfig = kafka.ConfigMap{
	"bootstrap.servers": "localhost:19092,localhost:19093,localhost:19094",
}

// 复制配置项
//
// 本函数的作用是将一个 kafka.ConfigMap 类型对象进行复制, 得到一个内容相同的新对象
func copy(cm *kafka.ConfigMap) *kafka.ConfigMap {
	// 创建新对象
	newCm := make(kafka.ConfigMap)

	// 将参数对象的键值对复制到新对象中
	for k, v := range *cm {
		newCm[k] = v
	}
	return &newCm
}

// 合并两个 kafka.ConfigMap 对象
//
// 本函数的作用是将两个 kafka.ConfigMap 对象进行合并, 即将 right 表示的对象内容复制到 left 对象中, 并返回 left 对象指针
//
// 注意: 如果 left 中包含和 right 相同的 Key, 则 left 中该键值对会被覆盖
func merge(left *kafka.ConfigMap, right *kafka.ConfigMap) *kafka.ConfigMap {
	if right != nil {
		// 遍历 right 对象的键值对
		for k, v := range *right {
			// 将 right 对象的键值对存入 left 对象中
			(*left)[k] = v
		}
	}
	return left
}

// 获取一个新的 kafka.ConfigMap 对象
//
// 本函数用于产生一个全新的 kafka.ConfigMap 对象, 并将 extConf 参数中的键值对存入其中
func newConfig(extConf *kafka.ConfigMap) *kafka.ConfigMap {
	return merge(copy(&commonConfig), extConf)
}
