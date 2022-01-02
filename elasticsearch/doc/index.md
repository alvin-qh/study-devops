# 索引

- [索引](#索引)
  - [1. 操作索引](#1-操作索引)
    - [1.1. 创建索引](#11-创建索引)
    - [1.2. 查询索引](#12-查询索引)
    - [1.3. 删除索引](#13-删除索引)
    - [1.4. 关闭和打开索引](#14-关闭和打开索引)
  - [2. 更新索引](#2-更新索引)
    - [2.1. 更新索引 settings](#21-更新索引-settings)
    - [2.2. 更新索引 mapping](#22-更新索引-mapping)
  - [3. 重建索引](#3-重建索引)

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
                "type": "keyword",
                "copy_to": "ALL"
            },
            "gender": {
                "type": "keyword",
                "copy_to": "ALL"
            },
            "birthday": {
                "type": "date",
                "copy_to": "ALL"
            },
            "role": {
                "type": "keyword",
                "copy_to": "ALL"
            },
            "department": {
                "properties": {
                    "college": {
                        "type": "keyword",
                        "copy_to": "ALL"
                    },
                    "program": {
                        "type": "keyword",
                        "copy_to": "ALL"
                    }
                }
            },
            "note": {
                "type": "text",
                "copy_to": "ALL"
            },
            "ALL": {
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
  - `copy_to` 将字段值复制到另一个字段中去，该字段不会作为 document 的字段，但可以被查询，例如：

    ```json
    GET /person/_search
    {
        "query": {
            "match": {
                "ALL": "STUDENT"
            }
        }
    }
    ```

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
POST /person/_close?wait_for_active_shards=0
```

- `wait_for_active_shards` 等待活动分片的数量。`0` 表示直到所有分片都为不活动状态

重新打开 person 索引

```json
POST /person/_open
```

## 2. 更新索引

**注意：** 更新索引前必须关闭该索引，更新完毕后打开即可

### 2.1. 更新索引 settings

如果需要给现有索引添加或更新 settings，则需通过 `_settings` API 进行操作

settings 结构中包括了索引的各类设置，更新时需要一并完整更新

```json
PUT /person/_settings
{
    "number_of_replicas": 2,
    "similarity": {
        ...
    },
    "analysis": {
        ...
    }
}
```

可以修改索引设置定义中的 副本数，相似性模块（`similarity`），分析器（`analysis`）。其中，分析器又是由 分词器（`tokenizer`），词过滤器（`filter`） 和 字符过滤器（`char_filter`）组成

**注意**：分片数（`number_of_shards`）无法直接修改，需要对索引进行 重建（`reindex`）操作

例如一个用于中文搜索的索引定义，使用了 `SmartCn` 和 `ICU` 分析器插件如下：

```json
PUT /person/_settings
{
    "similarity": {
        "default": {
            "type": "BM25"
        },
        "arrays": {
            "type": "BM25",
            "b": "0.3",
            "k1": "1.2"
        }
    },
    "analysis": {
        "filter": {
            "lowercase": {
                "type": "lowercase"
            },
            "icu_normalizer": {
                "name": "nfkc_cf",
                "type": "icu_normalizer"
            },
            "smartcn_stop": {
                "type": "stop",
                "stopwords": [
                    "，",
                    ",",
                    ".",
                    "。"
                ]
            }
        },
        "char_filter": {
            "word_break_helper_filter": {
                "type": "mapping",
                "mappings": [
                    "_=>\\u0020",
                    ".=>\\u0020",
                    "(=>\\u0020",
                    ")=>\\u0020",
                    ":=>\\u0020"
                ]
            }
        },
        "analyzer": {
            "plain_search_analyzer": {
                "type": "custom",
                "tokenizer": "icu_tokenizer",
                "filter": [
                    "lowercase",
                    "smartcn_stop",
                    "icu_normalizer"
                ],
                "char_filter": [
                    "word_break_helper_filter"
                ]
            }
        }
    }
}
```

### 2.2. 更新索引 mapping

如果需要给现有索引添加或更新 mapping，则需通过 `_mapping` API 进行操作

[`PUT /<index>/_mapping`](https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-put-mapping.html)

```json
PUT /person/_mapping
{
    "properties": {
        "name": {
            "type": "keyword",
            "copy_to": "ALL"
        },
        ...,
        "note": {
            "type": "text",
            "index_options": "offsets",
            "similarity": "BM25",
            "analyzer": "plain_search_analyzer",
            "position_increment_gap": 10,
            "fields": {
                "word_count": { // 通过指定分析其，对 note 字段的分词数量进行统计
                    "type": "token_count",
                    "store": true,
                    "analyzer": "plain_search_analyzer"
                }
            },
            "copy_to": "ALL"
        }
    }
}
```

## 3. 重建索引

一些时候，需要对某个索引进行重建。例如：索引重命名，索引重分片，集群转移等。

[`POST /_reindex`](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-reindex.html)

重建一个新索引

```json
PUT /person_new
{
    "settings": {
        "number_of_replicas": 2,
        "number_of_shards": 1,
        "similarity": {
            "default": {
                "type": "BM25"
            },
            "arrays": {
                "type": "BM25",
                "b": "0.3",
                "k1": "1.2"
            }
        },
        "analysis": {
            "filter": {
                "lowercase": {
                    "type": "lowercase"
                },
                "icu_normalizer": {
                    "name": "nfkc_cf",
                    "type": "icu_normalizer"
                },
                "smartcn_stop": {
                    "type": "stop",
                    "stopwords": [
                        "，",
                        ",",
                        ".",
                        "。"
                    ]
                }
            },
            "char_filter": {
                "word_break_helper_filter": {
                    "type": "mapping",
                    "mappings": [
                        "_=>\\u0020",
                        ".=>\\u0020",
                        "(=>\\u0020",
                        ")=>\\u0020",
                        ":=>\\u0020"
                    ]
                }
            },
            "analyzer": {
                "plain_search_analyzer": {
                    "type": "custom",
                    "tokenizer": "icu_tokenizer",
                    "filter": [
                        "lowercase",
                        "smartcn_stop",
                        "icu_normalizer"
                    ],
                    "char_filter": [
                        "word_break_helper_filter"
                    ]
                }
            }
        }
    },
    "mappings": {
        "properties": {
            "name": {
                "type": "keyword",
                "copy_to": "ALL"
            },
            "gender": {
                "type": "keyword",
                "copy_to": "ALL"
            },
            "birthday": {
                "type": "date",
                "copy_to": "ALL"
            },
            "role": {
                "type": "keyword",
                "copy_to": "ALL"
            },
            "department": {
                "properties": {
                    "college": {
                        "type": "keyword",
                        "copy_to": "ALL"
                    },
                    "program": {
                        "type": "keyword",
                        "copy_to": "ALL"
                    }
                }
            },
            "note": {
                "type": "text",
                "index_options": "offsets",
                "similarity": "BM25",
                "analyzer": "plain_search_analyzer",
                "position_increment_gap": 10,
                "fields": {
                    "word_count": {
                        "type": "token_count",
                        "store": true,
                        "analyzer": "plain_search_analyzer"
                    }
                },
                "copy_to": "ALL"
            },
            "ALL": {
                "type": "text",
                "index_options": "offsets",
                "similarity": "BM25",
                "analyzer": "plain_search_analyzer",
                "position_increment_gap": 10
            }
        }
    }
}
```

将原索引重建到新索引

```json
POST _reindex
{
    "source": {
        "index": "person"
    },
    "dest": {
        "index": "person_new"
    }
}
```

查看新索引状态，等待重建结束

```json
GET /_cat/indices/person_new?v
```

删除旧索引

```json
DELETE /person
```

使用新索引

```json
GET /person_new/_search
{
    "query": {
        "match": {
            "ALL": "STUDENT"
        }
    }
}
```
