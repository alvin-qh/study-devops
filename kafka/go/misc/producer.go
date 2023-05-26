package misc

import (
	"github.com/confluentinc/confluent-kafka-go/kafka"
)

func CreateProducer(extConf *kafka.ConfigMap) (*kafka.Producer, error) {
	conf := newConfig()
	if extConf != nil {
		for k, v := range *extConf {
			(*conf)[k] = v
		}
	}

	return kafka.NewProducer(conf)
}
