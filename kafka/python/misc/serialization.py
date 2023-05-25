from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import (AvroDeserializer,
                                                  AvroSerializer)


class AvroSchema:
    def __init__(
        self,
        schema_registry_url: str,
        *,
        schema="",
        schema_file=""
    ) -> None:
        self._registry_client = SchemaRegistryClient({
            "url": schema_registry_url,
        })

        if schema:
            self._schema = schema
        elif schema_file:
            self._schema = self._from_file(schema_file)

    @staticmethod
    def _from_file(schema_file: str) -> str:
        with open(schema_file) as fp:
            return fp.read()

    def create_serializer(self) -> AvroSerializer:
        return AvroSerializer(self._registry_client, self._schema)

    def create_deserializer(self) -> AvroDeserializer:
        return AvroDeserializer(self._registry_client, self._schema)
