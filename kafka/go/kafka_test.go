package main

import (
	"kafka/misc"
	"testing"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	"github.com/stretchr/testify/assert"
)

func TestTopic(t *testing.T) {
	topicName := "go-test__simple-topic"

	result, err := misc.CreateTopic(topicName)
	assert.NoError(t, err)

	assert.Equal(t, result.Topic, topicName)
	assert.True(t, misc.IsTopicCreated(result))
}

func TestProducerProduce(t *testing.T) {
	topicName := "go-test__simple-topic"
	misc.CreateTopic(topicName)

	producer, err := misc.CreateProducer(nil)
	assert.NoError(t, err)

	// key:= []byte("")

	ch := make(chan kafka.Event)
	msg := kafka.Message{
		TopicPartition: kafka.TopicPartition{
			Topic:     &topicName,
			Partition: kafka.PartitionAny,
		},
		// Key: ,
	}
	producer.Produce()
}
