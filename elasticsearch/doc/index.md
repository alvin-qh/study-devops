# 索引

- [索引](#索引)
  - [1. 操作索引](#1-操作索引)
    - [1.1. 创建索引](#11-创建索引)
    - [1.2. 查询索引](#12-查询索引)
    - [1.3. 删除索引](#13-删除索引)
    - [1.4. 关闭和打开索引](#14-关闭和打开索引)
  - [2. 操作索引 mapping](#2-操作索引-mapping)
    - [2.1. 更新 mapping](#21-更新-mapping)

> [文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices.html)

## 1. 操作索引

### 1.1. 创建索引

[`PUT /<index>`](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-create-index.html)

创建 person 索引

```json
PUT /person
```

创建 person 索引并定义文档字段

```json
PUT /person
{
    "settings": {
        "number_of_replicas": 0,
        "number_of_shards": 1
    },
    "mappings": {
        "properties": {
            "name": {
                "type": "keyword"
            },
            "gender": {
                "type": "keyword"
            },
            "birthday": {
                "type": "date"
            },
            "role": {
                "type": "keyword"
            },
            "department": {
                "properties": {
                    "college": {
                        "type": "keyword"
                    },
                    "program": {
                        "type": "keyword"
                    }
                }
            },
            "note": {
                "type": "text"
            }
        }
    }
}
```

- `settings`: 索引的设置
  - `number_of_replicas` 数据副本数，根据 `集群个数 - 1` 设置，一台机器时设置为 `0`
  - `number_of_shards` 数据分片数，默认为 `5`
- `mappings` 数据和字段的对应关系
  - `properties` 每种类型的数据都有 `properties` 来说明其字段

### 1.2. 查询索引

[`GET /<index>`](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-get-index.html)

获取 person 索引

```json
GET /person
```

### 1.3. 删除索引

注意，删除索引会同时删除相关的文档

[`DELETE /<index>`](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-delete-index.html)

删除 person 索引

```json
DELETE /person
```

### 1.4. 关闭和打开索引

关闭索引可以让索引暂时失效，之后可以重新打开该索引

[`POST /<index>/_close`](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-close.html)
[`POST /<index>/_open`](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-open-close.html)

关闭 person 索引

```json
POST /person/_close
```

重新打开 person 索引

```json
POST /person/_open
```

## 2. 操作索引 mapping

如果创建索引时同时创建了 mapping，或者需要给现有索引添加 mapping，则需要对 mapping 进行操作

### 2.1. 更新 mapping

[`PUT /<index>/_mapping`](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html)
