import conf
import pykafka as ka

client: ka.KafkaClient = None


def setup_function():
    """
    测试前置函数
    """
    global client

    # 获取 Kafka Client 连接
    client = ka.KafkaClient(
        hosts=conf.HOSTS,
    )


def teardown_function():
    pass


def test_connection():
    assert client is not None
