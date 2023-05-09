# KRaft 集群

- [KRaft 集群](#kraft-集群)
  - [1. 配置](#1-配置)

新版本的 Kafka 可以脱离 Zookeeper 独立运行, 即: 新版本的 Kafka 无需通过 Zookeeper 进行服务发现和偏移量查询. 摆脱了 Zookeeper 之后, Kafka 的配置得到了简化, 部署架构进行精简, 且避免了因 Zookeeper 并发性能不足带来的损耗

## 1. 配置
