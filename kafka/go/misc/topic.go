package misc

import (
	"context"

	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func CreateTopic(topicName string, options ...CreateOption) (*kafka.TopicResult, error) {
	args := createOption{
		NumPartition:      3,
		ReplicationFactor: 2,
	}
	for _, opt := range options {
		opt.apply(&args)
	}

	adminClient, err := kafka.NewAdminClient(newConfig())
	if err != nil {
		return nil, err
	}

	defer adminClient.Close()

	topics := []kafka.TopicSpecification{
		{
			Topic:             topicName,
			NumPartitions:     args.NumPartition,
			ReplicationFactor: args.ReplicationFactor,
		},
	}

	ctx := context.Background()
	result, err := adminClient.CreateTopics(ctx, topics)

	return &result[0], err
}

func IsTopicCreated(result *kafka.TopicResult) bool {
	errCode := result.Error.Code()
	return errCode == kafka.ErrNoError || errCode == kafka.ErrTopicAlreadyExists
}
