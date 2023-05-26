package misc

import "github.com/confluentinc/confluent-kafka-go/kafka"

var config = kafka.ConfigMap{
	"bootstrap.servers": "localhost:19092,localhost:19093,localhost:19094",
}

func newConfig() *kafka.ConfigMap {
	newConf := make(kafka.ConfigMap)

	for k, v := range config {
		newConf[k] = v
	}
	return &newConf
}
