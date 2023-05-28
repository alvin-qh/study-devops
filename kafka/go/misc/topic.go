package misc

import (
	"context"

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

// `CreateTopic` 函数的可选参数项结构体
type createOption struct {
	NumPartition      int // 分区数
	ReplicationFactor int // 分区副本数
}

// `CreateTopic` 函数的可选参数类型, 为函数类型, 参数为 createOption 结构体, 用于设置结构体字段值
type CreateOption = func(o *createOption)

// # 设置分区数参数项
//
// 本函数用于改变 `createOption` 结构体的 `NumPartition` 字段值
//
// 参数:
//   - `numPartition`: 主题分区数
//
// 返回 `CreateOption` 函数
func WithNumPartition(numPartition int) CreateOption {
	return func(o *createOption) {
		o.NumPartition = numPartition
	}
}

// # 设置分区副本数参数项
//
// 本函数用于改变 `createOption` 结构体的 `ReplicationFactor` 字段值
//
// 参数:
//   - `replicationFactor`: 主题分区副本数
//
// 返回 CreateOption 函数,
func WithReplicationFactor(replicationFactor int) CreateOption {
	return func(o *createOption) {
		o.ReplicationFactor = replicationFactor
	}
}

// # 创建主题
//
// 本函数用于创建指定主题
//
// 参数:
//   - `topicName`: 主题名称
//   - `opts`: 附加 `CreateOption` 类型可选参数
//
// 返回主题创建结果和错误对象
func CreateTopic(topicName string, opts ...CreateOption) (*kafka.TopicResult, error) {
	// 创建 AdminClient 对象
	adminClient, err := kafka.NewAdminClient(newConfig(nil))
	if err != nil {
		return nil, err
	}

	// 在结束前关闭 AdminClient 对象
	defer adminClient.Close()

	// 设置默认参数选项
	o := createOption{
		NumPartition:      3,
		ReplicationFactor: 2,
	}

	// 遍历可选参数, 覆盖默认参数选项
	for _, opt := range opts {
		opt(&o)
	}

	// 创建主题切片集合, 包含要创建的主题项
	topics := []kafka.TopicSpecification{
		{
			Topic:             topicName,
			NumPartitions:     o.NumPartition,
			ReplicationFactor: o.ReplicationFactor,
		},
	}

	// 创建主题
	result, err := adminClient.CreateTopics(context.Background(), topics)
	return &result[0], err
}

// # 判断主题创建结果是否为成功或主题已存在
//
// 判断返回的 kafka.TopicResult 对象的错误码是否为"无错误"(表示主题创建成功)或"主题已存在"
//
// 参数:
//   - `result`: `CreateTopic` 函数值, 表示主题创建的结果
//
// 返回 `true` 表示创建成功
func IsTopicCreated(result *kafka.TopicResult) bool {
	// 获取主题创建结果的错误码
	errCode := result.Error.Code()
	// 返回错误码是否表示"成功"或"主题已存在"
	return errCode == kafka.ErrNoError || errCode == kafka.ErrTopicAlreadyExists
}
