# 正则化器

- [正则化器](#正则化器)
  - [1. 自定义正则化器](#1-自定义正则化器)

正则化器与分析器（Analyzer）类似，除了它们只能处理单个词汇。因此，正则化器不包含分词器，仅接受可用的字符过滤器（Character Filter）和令牌过滤器（Token Filter）的子集。仅允许按字符运行的过滤器

例如，将允许使用小写过滤器（`Lowercase`），但不允许使用词干过滤器（`Stemmer`），因为词干过滤器需要从整体上考虑关键字

目前可化在正则化使用的过滤器列表如下：

- `arabic_normalization`
- `asciifolding`
- `bengali_normalization`
- `cjk_width`
- `decimal_digit`
- `elision`
- `german_normalization`
- `hindi_normalization`
- `indic_normalization`
- `lowercase`
- `persian_normalization`
- `scandinavian_folding`
- `serbian_normalization`, `sorani_normalization`, `uppercase`

## 1. 自定义正则化器

自定义规范化器可包含 [字符过滤器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-charfilters.html) 列表和 [词汇过滤器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-tokenfilters.html) 列表

创建索引

```json
PUT /normalizer-quote
{
    "settings": {
        "analysis": {
            "normalizer": {
                "quote_normalizer": {
                    "type": "custom",
                    "char_filter": [
                        "quote_filter"
                    ],
                    "filter": [
                        "lowercase",
                        "asciifolding"
                    ]
                }
            },
            "char_filter": {
                "quote_filter": {
                    "type": "mapping",
                    "mappings": [
                        "« => \"",
                        "» => \""
                    ]
                }
            }
        }
    },
    "mappings": {
        "properties": {
            "name": {
                "type": "keyword",
                "normalizer": "quote_normalizer"
            }
        }
    }
}
```

存储数据

```json
PUT /normalizer-quote/_doc/1
{
    "name": "Alvin «Q»"
}
```

查询数据：使用小写字母和 `"` 可以匹配到数据

```json
GET /normalizer-quote/_search
{
    "query": {
        "term": {
            "name": "Alvin \"Q\""
        }
    }
}
```
