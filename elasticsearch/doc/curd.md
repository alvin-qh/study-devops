# ES 增删查改

- [ES 增删查改](#es-增删查改)
  - [1. 文档](#1-文档)
    - [1.1. 创建文档](#11-创建文档)
  - [2. 更新文档](#2-更新文档)
    - [2.1. 按照 ID 更新文档](#21-按照-id-更新文档)
    - [2.2. 根据查询条件更新文档](#22-根据查询条件更新文档)
  - [3. 删除文档](#3-删除文档)
    - [3.1. 根据 ID 删除文档](#31-根据-id-删除文档)
    - [3.2. 根据查询条件删除文档](#32-根据查询条件删除文档)
  - [4. 查询](#4-查询)
    - [4.1. 根据 ID 获取文档](#41-根据-id-获取文档)
    - [4.2. 同时获取多个文档](#42-同时获取多个文档)
    - [4.3. 条件查询](#43-条件查询)
    - [4.4. 定义获取内容](#44-定义获取内容)
    - [4.5. 查询分析](#45-查询分析)
      - [4.5.1. 查询结果分析](#451-查询结果分析)
      - [4.5.2. 查询情况分析](#452-查询情况分析)
      - [4.5.3. 字段能力查询](#453-字段能力查询)
      - [4.5.4. 查询分片情况](#454-查询分片情况)
      - [4.5.5. Rank 评估](#455-rank-评估)
      - [4.5.6. 查询验证](#456-查询验证)
    - [4.6. Lucene 语法查询](#46-lucene-语法查询)

## 1. 文档

### 1.1. 创建文档

创建文档即是对一个 document 结构进行索引 (index) 操作

[`PUT /<index>/_doc/<_id>`, `POST /<index>/_doc/`, `PUT /index/_create/<_id>`, `POST /<index>/_create/<_id>`](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html)

索引文档的请求方法有如上四种，其中 `POST /<index>/_doc/` 会自动生成 `_id` 属性

创建 `STUDENT 001`

```json
POST /person/_doc/001
{
    "name": "Alvin",
    "gender": "M",
    "birthday": "1981-03-17",
    "role": "STUDENT",
    "department": {
        "college": "Computer science",
        "program": "Network engineering"
    },
    "note": "Top Outstanding student"
}
```

创建 `TEACHER 002`

```json
POST /person/_doc/002
{
    "name": "Emma",
    "gender": "F",
    "birthday": "1985-03-29",
    "role": "TEACHER",
    "department": {
        "college": "Computer science",
        "program": "Network engineering"
    },
    "note": "Top Outstanding teacher"
}
```

## 2. 更新文档

`PUT /<index>/<type>/<id>` 用于更新索引 `index` 的 `type` 下 id 为 `id` 的文档

### 2.1. 按照 ID 更新文档

整个文档更新。整文档更新的语法和创建文档的语法一致，用新文档覆盖旧文档

```json
PUT /person/_doc/001
{
    "name": "Alvin",
    "gender": "M",
    "birthday": "1981-03-17",
    "role": "STUDENT",
    "department": {
        "college": "Computer science",
        "program": "Software engineering"
    },
    "note": "Top Outstanding student"
}
```

更新部分文档

[`POST /<index>/_update/<_id>`](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html)

可以只更新文档的部分字段

```json
POST /person/_update/001
{
    "doc": {
        "department": {
            "college": "Computer science",
            "program": "Networking engineering"
        }
    }
}
```

- `doc` 字段表示要更新的那部分文档

通过脚本更新文档

[`POST /<index>/_update/<_id>`](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update.html)

```json
POST /person/_update/001
{
    "script": {
        "source": "if (ctx._source['gender'] == params.expectedGender) { ctx._source['gender'] = params.updatedGender; }",
        "lang": "painless",
        "params": {
            "expectedGender": "M"
        }
    }
}
```

上述操作中，比较文档的 `gender` 字段，并在满足条件时进行修改

- `ctx` 表示脚本的上下文对象，`ctx._source` 指的是要操作的文档

更新脚本中，可以通过 `remove` 函数删除某个现有字段

```json
POST /person/_update/001
{
    "script": {
        "source": "if (ctx._source['role'] == params.expectedRole) { ctx._source['department'].remove('program'); }",
        "lang": "painless",
        "params": {
            "expectedRole": "STUDENT"
        }
    }
}
```

- `remove` 函数表示删除文档的某个字段

可以通过更新，删除掉指定文档

```json
POST /person/_update/001
{
    "script": {
        "source": "if (ctx._source['role'] == params.expectedRole) { ctx.op = 'delete'; } else { ctx.op = 'none'; }",
        "lang": "painless",
        "params": {
            "expectedRole": "STUDENT"
        }
    }
}
```

- `ctx.op` 表示后续的操作，`delete` 表示后续进行删除操作，`none` 表示后续无操作

### 2.2. 根据查询条件更新文档

[`POST /<index>/_update_by_query?conflicts=proceed`](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-update-by-query.html)

```json
POST /person/_update_by_query?conflicts=proceed
{
    "script": {
        "source": "ctx._source['gender'] = 'M'; ctx._source['department']['program'] = params.program;",
        "params": {
            "program": "Software engineering"
        },
        "lang":"painless"
    },
    "query": {
        "term": {
            "role": "TEACHER"
        }
    }
}
```

- `conflicts`: `conflicts=proceed` 通过简单的版本计数方式处理冲突，不返回失败，除此外还有 `conflicts=abort` 表示发生冲突后返回失败。默认值为 `abort`

## 3. 删除文档

### 3.1. 根据 ID 删除文档

[`DELETE /<index>/_doc/<_id>`](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete.html)

删除指定 id 为 `002` 的文档

```json
DELETE /person/_doc/002?routing=shard-1
```

- `routing` 如果已知文档存储的分片，则可以直接将删除请求路由到指定分片上，减少服务端操作；但如果分片指定错误，则删除失败

### 3.2. 根据查询条件删除文档

[`POST /<index>/_delete_by_query`](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-delete-by-query.html)

删除匹配条件的所有文档

```json
POST /person/_delete_by_query
{
    "query": {
        "term": {
            "department.college": "Computer science"
        }
    }
}
```

## 4. 查询

### 4.1. 根据 ID 获取文档

[`GET /<index>/_doc/<_id>, HEAD <index>/_doc/<_id>, GET <index>/_source/<_id>, HEAD <index>/_source/<_id>`](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html)

根据 ID 获取文档

```json
GET /person/_doc/002
```

仅判断文档是否存在（效率较高）

```json
HEAD /person/_doc/002
```

仅获取文档中的 `_source` 部分，不需要其余 metadata 信息

```json
GET /person/_source/002
```

### 4.2. 同时获取多个文档

[`GET /_mget`, `GET /<index>/_mget`](https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html)

查询不同索引下的多个文档

```json
GET /_mget
{
  "docs": [
    {
        "_index": "person",
        "_id": "001"
    },
    {
        "_index": "person",
        "_id": "002"
    }
  ]
}
```

所有的查询项都会返回结果，即是否查询成功

查询指定索引下的多个文档

```json
GET /person/_mget
{
  "docs": [
    {
        "_id": "001"
    },
    {
        "_id": "002"
    }
  ]
}
```

### 4.3. 条件查询

条件查询是通过 `_search` API 和 QueryDSL 进行查询，有关 QueryDSL 参见相关文档

[`GET /<index>/_search`](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-search.html)

查询指定索引下的全部文档

```json
GET /person/_search
```

相当于不使用任何查询条件进行文档检索，结果是指定索引下的所有文档

查询符合条件的结果

```json
GET /person/_search
{
    "query": {
        "term": {
            "name": "Alvin"
        }
    }
}
```

查询结果进行分页

```json
GET /person/_search?from=0&size=10
{
    "query": {
        "match": {
            "note": "Top"
        }
    },
    "sort": {
        "_id": {
            "order": "desc"
        }
    }
}
```

上面的请求查询 `note` 字段包含 "Top" 字符串的所有结果，结果按 `_id` 倒序，并进行分页

- `from` 起始索引
- `size` 一页大小

### 4.4. 定义获取内容

通过 `_source` 字段可以定义查询结果的内容

```json
POST /person/_search
{
    "_source": [
        "name",
        "type",
        "department.program"
    ],
    "query": {
        "match": {
            "ALL": {
                "query": "STUDENT"
            }
        }
    }
}
```

这样一来，在返回结果的 `_source` 字段中，只包含 `name`, `type` 和 `department` 中的 `program` 字段

### 4.5. 查询分析

#### 4.5.1. 查询结果分析

查询结果分析用来体现一个文档被搜索（或无法被搜索）的原因

[`GET /<index>/_explain/<id>, POST /<index>/_explain/<id>`](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-explain.html#search-explain)

```JSON
GET /person/_explain/001
{
    "query" : {
        "match": {
            "note" : "Top"
        }
    }
}
```

#### 4.5.2. 查询情况分析

查询情况分析主要用来分析查询的各个环节耗时情况

```json
GET /person/_search?from=0&size=10
{
    "profile": true,
    "query": {
        "match": {
            "note": "Top"
        }
    },
    "sort": {
        "_id": {
            "order": "desc"
        }
    }
}
```

- `profile` 为 `true` 表示查询结果中包含查询情况分析报告

#### 4.5.3. 字段能力查询

查询指定索引下，文档各个字段可被查询的能力

```json
GET /person/_field_caps?fields=name,department.college
```

返回结果中包括字段类型，以及可被检索的方式，例如 `person` 索引的 `name` 字段分析结果如下：

```json
{
    "fields" : {
        "name" : {
            "keyword" : {
                "type" : "keyword",
                "metadata_field" : false,
                "searchable" : true,
                "aggregatable" : true
            }
        }
        ...
    }
}
```

#### 4.5.4. 查询分片情况

用于查询某个索引的分片情况

```json
GET /person/_search_shards
```

#### 4.5.5. Rank 评估

对查询结果的评分进行评估，了解各种指标下的评估情况

[`GET /<index>/_rank_eval`](https://www.elastic.co/guide/en/elasticsearch/reference/current/search-rank-eval.html)

[各类指标说明](https://en.wikipedia.org/wiki/Evaluation_measures_(information_retrieval)#Precision)

`_rank_eval` 分两部分，`requests` 以及 `metric`。`requests` 表示要评估的查询和数据，`metric` 表示使用的指标

需要评估的查询和数据可以如下，后续的评估都会用到该 `requests`

```json
"requests": [
    {
        "id": "rank_result",
        "request": {
            "query": {
                "match": {
                    "note": "Top Outstand"
                }
            }
        },
        "ratings": [
            {
                "_index": "person",
                "_id": "001",
                "rating": 0
            },
            {
                "_index": "person",
                "_id": "002",
                "rating": 1
            }
        ]
    }
]
```

- `id` 表示一个标识，区分多个 `requests` 项目，用于在评估结果中进行查找
- `request` 待评估的查询
- `ratings` 需要评估的数据，评估结果即这部分数据在整个查询结果中的情况

K 精度指标 (P@K)：衡量前 k 个查询结果中相关结果的比例。是 Precision 指标的一种形式。例如：P@10 的值为 `0.6` 表示 10 个匹配结果项中有 6 个和所给的 `ratings` 相关

```json
GET /person/_rank_eval
{
    "requests": @requests,
    "metric": {
        "precision": {
            "k": 10,
            "relevant_rating_threshold": 1,
            "ignore_unlabeled": false
        }
    }
}
```

- `k` 检索结果最大数量，默认为 `10`
- `relevant_rating_threshold` 文档被评估为"相关"的阈值，默认为 `1`
- `ignore_unlabeled` 如果为 `true`，则未标记文档会被忽略，默认为 `false`

K 召回指标 (R@K)：衡量前 k 个搜索项中相关结果的总数。结果为前 k 个结果中相关文档相对于所有可能的相关结果的比例。例如：10（R@10）值为 `0.5` 的召回意味着 `8` 个搜索结果中有 `4` 个和所给的 `ratings` 相关

```json
GET /person/_rank_eval
{
    "requests": @requests,
    "metric": {
        "recall": {
            "k": 10,
            "relevant_rating_threshold": 1
        }
    }
}
```

平均倒数等级：该指标是计算第一个相关文档的排名的倒数。例如，在位置 `3` 中找到第一个相关结果意味着倒数**秩**为 `1/3`。每个查询的倒数秩所有查询中取平均值，以提供平均倒数秩

```json
GET /person/_rank_eval
{
    "requests": @requests,
    "metric": {
        "mean_reciprocal_rank": {
            "k": 10,
            "relevant_rating_threshold": 1
        }
    }
}
```

折扣累积收益 (DCG) ：与上述两个指标相比，折扣累积收益同时考虑了搜索结果的排名和评级。假设高度相关的文档在结果列表顶部显示时对用户更有用。因此，DCG 公式会降低搜索排名较低的文档的高评级对整体 DCG 指标的贡献。

```json
GET /person/_rank_eval
{
    "requests": @requests,
    "metric": {
        "dcg": {
            "k": 10,
            "normalize": false
        }
    }
}
```

#### 4.5.6. 查询验证

不进行实际的查询，只是分析请求的查询 DSL 是否正确

```json
GET /person/_validate/query
{
    "query": {
        "match": {
            "note": "Top"
        }
    }
}
```

返回结果中会包含 query 是否正确

### 4.6. Lucene 语法查询

Elasticsearch 支持使用 Lucene 表达式进行查询，具体方式就是在 URL 后通过 `q` 参数设置 Lucene 表达式

> 参见 [Lucene 查询](http://www.lucenetutorial.com/lucene-query-syntax.html)

查询字段值

```json
GET /person/_search?q=role:STUDENT
```

模糊匹配

```json
GET /person/_search?q=department.program:Soft*
```

- `Soft*` 表示匹配所有以 `Soft` 开头的结果

```json
GET /person/_search?q=note:*Out*
```

- `*Out*` 表示匹配所有包含 `Out` 字符串的结果

逻辑运算符

通过 `or` 和 `and` 连接多个表达式

```json
GET /person/_search?q=(role:STUDENT or role:TEACHER) AND note:Top*
```

注意：如果使用 http 请求发送查询，`q` 参数需进行 URL 编码

非运算符

```json
GET /person/_search?q=-role:STUDENT
```

- `-` 放在逻辑表达式之前，表示"非"
