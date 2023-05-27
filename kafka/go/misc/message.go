// 消息相关函数
package misc

import (
	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/google/uuid"
)

// buildMessage 函数的可选参数项结构体
type messageOption struct {
	KeyPrefix   string // 消息 Key 前缀字符串
	ValuePrefix string // 消息 Value 前缀字符串
	Partition   int32  // 消息发送的目标分区
}

// buildMessage 函数的可选参数类型, 为函数类型, 参数为 messageOption 结构体, 用于设置结构体字段值
type MessageOption = func(*messageOption)

// 设置消息 Key 前缀字符串
//
// 本函数返回 MessageOption 函数, 用于改变 messageOption 结构体的 KeyPrefix 字段值
func WithKeyPrefix(keyPrefix string) MessageOption {
	return func(opt *messageOption) {
		opt.KeyPrefix = keyPrefix
	}
}

// 设置消息 Value 前缀字符串
//
// 本函数返回 MessageOption 函数, 用于改变 messageOption 结构体的 ValuePrefix 字段值
func WithValuePrefix(valuePrefix string) MessageOption {
	return func(opt *messageOption) {
		opt.ValuePrefix = valuePrefix
	}
}

// 设置消息分区值
//
// 本函数返回 MessageOption 函数, 用于改变 messageOption 结构体的 Partition 字段值
func WithPartition(partition int32) MessageOption {
	return func(opt *messageOption) {
		opt.Partition = partition
	}
}

// 构建消息对象
//
// 本函数用于创建一个 kafka.Message 对象, 即要发送的消息, 需要设置消息的 "主题", "Key" 和 "Value" 值
func buildMessage(topic string, key string, value string, opts []MessageOption) (*kafka.Message, error) {
	// 设置默认参数选项
	o := messageOption{
		KeyPrefix:   "key-",
		ValuePrefix: "value-",
		Partition:   kafka.PartitionAny, // 设置默认消息分区是由 Kafka 确定
	}

	// 遍历可选参数, 覆盖默认参数选项
	for _, opt := range opts {
		opt(&o)
	}

	// 创建消息结构体对象并返回, 注意, 消息的 Key 和 Value 均需要转换为"字节切片"对象
	return &kafka.Message{
		TopicPartition: kafka.TopicPartition{ // 设置消息的主题和分区
			Topic:     &topic,
			Partition: o.Partition,
		},
		Key:   []byte(o.KeyPrefix + key),     // 设置消息的 Key, 为指定的前缀 + 给定的 Key 值
		Value: []byte(o.ValuePrefix + value), // 设置消息的 Value, 为指定的前缀 + 给定的 Value 值
	}, nil
}

// 创建一个消息对象
//
// 创建消息对象需指定"消息主题", 该方法会自动生成一个 Key 和 Value 值为随机的消息对象
func GenerateMessage(topic string, opts ...MessageOption) (*kafka.Message, error) {
	// 创建 UUID 对象, 用于消息的 Key
	uidKey, err := uuid.NewUUID()
	if err != nil {
		return nil, err
	}

	// 创建 UUID 对象, 用于消息的 Value
	uidValue, err := uuid.NewUUID()
	if err != nil {
		return nil, err
	}

	// 创建消息对象并返回
	return buildMessage(topic, uidKey.String(), uidValue.String(), opts)
}

// 创建一个 Key 为固定值的消息对象
//
// 固定 Key 值是为了让消息发送到固定的分区
//
// 创建消息对象需指定"消息主题", 该方法会自动生成一个 Key 值固定, Value 值随机的消息对象
func GenerateMessageWithFixedKey(topic string, opts ...MessageOption) (*kafka.Message, error) {
	// 生成一个 UUID 对象, 用于消息的 Value
	uidValue, err := uuid.NewUUID()
	if err != nil {
		return nil, err
	}

	// 创建消息对象并返回
	return buildMessage(topic, "fixed", uidValue.String(), opts)
}
