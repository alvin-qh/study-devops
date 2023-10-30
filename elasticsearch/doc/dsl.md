# QueryDSL

- [QueryDSL](#querydsl)
  - [1. 查询 DSL](#1-查询-dsl)
    - [1.1. `match` 查询](#11-match-查询)
    - [1.2. `multi_match` 查询](#12-multi_match-查询)
    - [1.3. `bool` 查询](#13-bool-查询)

> 查看 [文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)

较为复杂的查询均是通过 QueryDSL 来描述，QueryDSL 由一组运算符和查询条件，通过 JSON 结构组合而成

## 1. 查询 DSL

### 1.1. `match` 查询

[`Match` query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-match-query.html)

字段查询：`match` 运算符可以对 `text` 类型的字段进行全文检索（其它类型无法全文检索）

```json
GET /person/_search
{
    "query": {
        "match": {
            "role": "STUDENT"
        }
    }
}
```

- `role` 是 `keyword` 类型，无法全文检索，只能等值比较

```json
GET /person/_search
{
    "query": {
        "match": {
            "note": "Top"
        }
    }
}
```

- `note` 是 `text` 类型，支持全文检索

```json
GET /person/_search
{
    "query": {
        "match": {
            "note": {
                "query": "Outstanding",
                "fuzziness": "AUTO",
                "max_expansions": 50,
                "prefix_length": 0,
                "fuzzy_transpositions": true,
                "lenient": true,
                "operator": "OR",
                "minimum_should_match": 1,
                "zero_terms_query": "none"
            }
        }
    }
}
```

- `fuzziness` 模糊查询策略，默认为 `AUTO`。也可以为一个数字或 `AUTO:3,6`。数字表示一个"编辑距离"，即：需要对一个字符串进行多少次编辑才能和另一个字符串匹配，参见 [说明](https://en.wikipedia.org/wiki/Levenshtein_distance)
- `max_expansions` 在模糊查询过程中，设置变化发生的次数，默认为 `50`。这个值如果设置过高，则会导致查询结果匹配不佳
- `prefix_length` 设置字符串匹配时，不能变化的前置字符个数，默认为 `0`，表示整个字符串都可以在变化后进行匹配
- `fuzzy_transpositions` 字符串比较时是否允许交互相邻的两个字符进行比较，默认为 `true`
- `lenient` 设置是否宽容匹配，默认为 `false`。如果为 `true`，则部分格式化错误（例如以文本类型匹配数值类型时）将被忽略
- `operator` 多个字符串进行匹配时，匹配结果取多个单词同时匹配（`AND`）还是个别匹配（`OR`），默认为 `OR`
- `minimum_should_match` 最小匹配约束，即所给的查询关键词必须匹配目标文本的最小次数，可以为正负数字（如 `1`，`-2`），正负百分比（如 `20%`, `-30%`）等，参见 [说明](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-minimum-should-match.html)
- `zero_terms_query` 当语法分析器删除所有 token 后（例如使用了 stop 过滤器），返回的结果。`none` 不返回结果，`all` 返回所有结果（类似于 `match_all` 操作），默认为 `none`

### 1.2. `multi_match` 查询

[`Multi-match` query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-multi-match-query.html#type-cross-fields)

多字段查询：可以同时从多个字段匹配检索字符串

```json
GET /person/_search
{
    "query": {
        "multi_match": {
          "query": "STUDENT Top",
          "type": "best_fields",
          "fields" : [
                "role",
                "note"
            ]
        }
    }
}
```

- `type` 表示查询结果评分的类型，包括：
  - `best_fields` 以最佳匹配的字段进行评分
  - `most_fields` 以所有匹配到的字段进行评分
  - `cross_fields` 将具有相同分析器的字段合成一个"大字段"后进行查询

### 1.3. `bool` 查询

[`Bool` query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-match-bool-prefix-query.html)

通过 `bool` 运算符可以组合多个查询

```json
GET /person/_search
{
    "query": {
        "bool": {
            "must": [
                {
                    "term": {
                        "role": "STUDENT"
                    }
                }
            ],
            "filter": [
                {
                    "range": {
                        "birthday": {
                            "gte": "1980-01-01"
                        }
                    }
                }
            ],
            "must_not": [
                {
                    "term": {
                        "gender": "F"
                    }
                }
            ],
            "should": [
                {
                    "match": {
                        "note": "Top"
                    }
                }
            ]
        }
    }
}
```
