# 定义存储消息的元组
from collections import namedtuple
from uuid import uuid4

# 表示消息的命名元组类型
# key 表示消息的 Key
# value 表示消息的 Value
MessageData = namedtuple("MessageData", ["key", "value"])


class MessageGenerator:
    """
    消息生成器类型
    """

    def __init__(self, key_prefix="key-", value_prefix="value-") -> None:
        """
        构造器, 指定一条消息的 Key 前缀和 Value 前缀

        Args:
        - `key_prefix` (`str`, optional): 消息 Key 的前缀. Defaults to `"key-"`.
        - `value_prefix` (`str`, optional): 消息 Value 的前缀. Defaults to `"value-"`.
        """
        self._key_prefix = key_prefix
        self._value_prefix = value_prefix

    def generate(self) -> MessageData:
        """
        生成一条消息, Key 和 Value 为指定的前缀 + 随机字符串组成

        Returns:
        - `MessageData`: 消息对象
        """
        return MessageData(
            f"{self._key_prefix}{uuid4()}".encode(),
            f"{self._value_prefix}{uuid4()}".encode(),
        )

    def generate_with_fixed_key(self) -> MessageData:
        """
        生成一条 Key 固定, Value 随机的消息

        Returns:
        - `MessageData`: 消息对象
        """
        return MessageData(
            f"{self._key_prefix}fixed".encode(),
            f"{self._value_prefix}{uuid4()}".encode(),
        )
