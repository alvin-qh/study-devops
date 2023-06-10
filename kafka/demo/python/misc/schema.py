from abc import ABC, abstractmethod
from ast import Dict
from ctypes import ArgumentError
from typing import Any, Callable, Optional

from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import (AvroDeserializer,
                                                  AvroSerializer, Deserializer,
                                                  Serializer)
from confluent_kafka.schema_registry.json_schema import (JSONDeserializer,
                                                         JSONSerializer)
from confluent_kafka.serialization import SerializationContext

# 定义从对象到字典的函数类型
ToDictFn = Callable[[Any, SerializationContext], Dict]

# 定义从字典到对象的函数类型
FromDictFn = Callable[[Dict, SerializationContext], Any]


class Schema(ABC):
    """
    表示 Schema 类型的接口类
    """

    def __init__(
        self,
        schema_registry_url: str,
        *,
        schema="",
        schema_file=""
    ) -> None:
        """
        实例化 Schema 对象

        Args:
        - `schema_registry_url` (`str`): Schema Registry 服务地址
        - `schema` (`str`, optional): Schema 描述字符串. Defaults to "".
        - `schema_file` (`str`, optional): 保存 Schema 描述的文件路径名. Defaults to "".
        """
        # 实例化 Schema Registry 客户端对象
        self._registry_client = SchemaRegistryClient({
            "url": schema_registry_url,  # Schema Registry 服务地址
        })

        # 如果传递了 schema 参数, 则保存 Schema 描述字符串, 否则从所给文件中读取 Schema
        # 描述字符串
        if schema:
            self._schema = schema
        elif schema_file:
            self._schema = self._from_file(schema_file)
        else:
            raise ArgumentError("Invalid arguments")

    @staticmethod
    def _from_file(schema_file: str) -> str:
        """
        从给定的文件中读取 Schema 描述字符串

        Args:
        - `schema_file` (`str`): 文件路径

        Returns:
        - `str`: Schema 描述字符串
        """
        with open(schema_file) as fp:
            return fp.read()

    @abstractmethod
    def create_serializer(self, to_dict: Optional[ToDictFn] = None) -> Serializer:
        """
        创建序列化器对象

        注意, 如果序列化的对象不是 `Dict` 类型, 则需要传递 `to_dict` 参数来进行转换

        Args:
        - `to_dict` (`Optional[ToDictFunc]`): 将参数对象转为 Dict 对象的函数

        Returns:
        - `Serializer`: 序列化器对象
        """

    @abstractmethod
    def create_deserializer(self, from_dict: Optional[FromDictFn] = None) -> Deserializer:
        """
        创建反序列化器对象

        注意, 默认情况下, 反序列化结果为 `Dict` 对象, 如果需要返回其它类型对象,
        则需要传递 `to_obj` 参数来将 `Dict` 对象转为目标类型

        Args:
        - `to_obj` (`Optional[ToObjFunc]`): 将 Dict 对象转为目标对象的函数

        Returns:
        - `Deserializer`: 反序列化器对象
        """


class AvroSchema(Schema):
    """
    表示符合 Avro 协议的 Schema 类型
    """

    def create_serializer(self, to_dict: Optional[ToDictFn] = None) -> Serializer:
        return AvroSerializer(
            schema_registry_client=self._registry_client,
            schema_str=self._schema,
            to_dict=to_dict,
        )

    def create_deserializer(self, from_dict: Optional[FromDictFn] = None) -> Deserializer:
        return AvroDeserializer(
            schema_registry_client=self._registry_client,
            schema_str=self._schema,
            from_dict=from_dict)


class JsonSchema(Schema):
    """
    表示符合 Json 协议的 Schema 类型
    """

    def create_serializer(self, to_dict: Optional[ToDictFn] = None) -> Serializer:
        return JSONSerializer(
            schema_registry_client=self._registry_client,
            schema_str=self._schema,
            to_dict=to_dict,
        )

    def create_deserializer(self, from_dict: Optional[FromDictFn] = None) -> Deserializer:
        return JSONDeserializer(
            schema_registry_client=self._registry_client,
            schema_str=self._schema,
            from_dict=from_dict)
