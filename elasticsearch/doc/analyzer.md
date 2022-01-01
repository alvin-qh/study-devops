# 文本分析

文本分析器用来对索引的文本进行分析处理，以便其在检索的时候更符合语法、语义，从而有更高的召回率

需要为每个索引设置分析器，并为索引的字段设置分析器，这样才能再写入数据时对数据进行正确的分析

## 1. 内置分析器

[内置分析器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-analyzers.html)

### 1.1. 标准分析器

[Standard analyzer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-standard-analyzer.html#analysis-standard-analyzer)

是默认的文本分析器，默认文本分析器基于机械分词器和停止词集合进行分词，可以在大部分语系下正常工作

```json
PUT /analyzer-standard
{
    "settings": {
        "analysis": {
            "analyzer": {
                "test_standard_analyzer": {
                    "type": "standard",
                    "max_token_length": 5,
                    "stopwords": "_english_"
                }
            }
        }
    }
}

POST /analyzer-standard/_analyze
{
    "analyzer": "test_standard_analyzer",
    "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

- `max_token_length` 最大词长度，默认为 `255`
- `stopwords` 停止词集合，默认为 `"_english_"`
- `stopwords_path` 停止词集合文件路径

### 1.2. 简单分析器

[Simple analyzer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-simple-analyzer.html)

简单分析器，只是根据文本中的“非字母”字符（如数字，空格等）将文本进行分词

```json
PUT /analyzer-simple
{
    "settings": {
        "analysis": {
                "analyzer": {
                "test_simple_analyzer": {
                    "type": "simple"
                }
            }
        }
    }
}

POST /analyzer-simple/_analyze
{
    "analyzer": "test_simple_analyzer",
    "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

### 1.3. 空白分析器

[Whitespace analyzer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-whitespace-analyzer.html)

空白分析器通过空格等“空白”字符，对文本进行

```json
PUT /analyzer-whitespace
{
    "settings": {
        "analysis": {
            "analyzer": {
                "test_whitespace_analyzer": {
                    "type": "whitespace"
                }
            }
        }
    }
}

POST /analyzer-whitespace/_analyze
{
    "analyzer": "test_whitespace_analyzer",
    "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}

```

### 1.4. 停止词分析器

[Stop analyzer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-stop-analyzer.html)

和 `simple` 分析器类似，相当于在 `simple` 分析器基础上添加了对停止词集合的支持，默认的停止词集合为 `"_english_"`

```json
PUT /analyzer-stop
{
    "settings": {
        "analysis": {
            "analyzer": {
                "test_stop_analyzer": {
                    "type": "stop",
                    "stopwords": "_english_"
                }
            }
        }
    }
}

POST /analyzer-stop/_analyze
{
    "analyzer": "test_stop_analyzer",
    "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

- `stopwords` 停止词集合，默认为 `"_english_"`
- `stopwords_path` 停止词集合文件路径

### 1.5. 关键词分析器

[Keyword analyzer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-keyword-analyzer.html)

关键词分析器（`keyword`）是 `keyword` 类型字段使用的分析器，使用 `noop` 分析器，其实就是不进行分词

对于非字符（`\W`）以及非下划线（`_`）的情况进行分词，即正则为 `\W|_`

```json
PUT /analyzer-keyword
{
    "settings": {
        "analysis": {
            "analyzer": {
                "test_keyword_analyzer": {
                    "type": "keyword"
                }
            }
        }
    }
}

POST /analyzer-keyword/_analyze
{
    "analyzer": "test_keyword_analyzer",
    "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

- `pattern` 用于分词的正则表达式，以满足该正则的部分为分词点
- `lowercase` 分词结果是否转为小写，默认为 `true`
- `flags` 正则标志，和 Java `java.util.regex.Pattern` 类定义的标志一致。参考 [Java Doc](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html)
- `stopwords` 停止词集合，默认为 `"_english_"`
- `stopwords_path` 停止词集合文件路径

### 1.6. 正则分析器

[Pattern analyzer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-pattern-analyzer.html)

正则分析器，通过正则表达式匹配结果进行分词

例如：对于非字符（`\W`）以及非下划线（`_`）的情况进行分词，即正则为 `\W|_`

```json
PUT /analyzer-pattern
{
    "settings": {
        "analysis": {
            "analyzer": {
                "test_pattern_analyzer": {
                    "type": "pattern",
                    "pattern": "\\W|_",
                    "lowercase": true,
                    "flags": "COMMENTS"
                }
            }
        }
    }
}

POST /analyzer-pattern/_analyze
{
    "analyzer": "test_pattern_analyzer",
    "text": "alvin-qh@alvin_study.com"
}
```

### 1.7. 指纹分析器

[Fingerprint analyzer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-fingerprint-analyzer.html)

指纹分析器实现了一种 [指纹算法](https://github.com/OpenRefine/OpenRefine/wiki/Clustering-In-Depth#fingerprint)，OpenRefine 项目使用该算法来辅助聚类。

输入文本会被转换为小写，通过规范化以移除扩展字符、进行排序、去重并连接成单个结果。如果配置了停用词集合，则相关停用词也将被删除。输出结果是输入内容重新分词，排序后的结果，可以作为文本相似度比较的依据

```json
PUT /analyzer-fingerprint
{
    "settings": {
        "analysis": {
            "analyzer": {
                "test_fingerprint_analyzer": {
                    "type": "fingerprint",
                    "separator": " ",
                    "max_output_size": 150
                }
            }
        }
    }
}

POST /analyzer-fingerprint/_analyze
{
    "analyzer": "test_fingerprint_analyzer",
    "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

### 1.8. 语言相关分析器

[Language analyzer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-lang-analyzer.html)

Elasticsearch 内置了多种语言的分析器，可以针对某种特定语言进行分词，包括：
`arabic`, `armenian`, `basque`, `bengali`, `brazilian`, `bulgarian`, `catalan`, `cjk`, `czech`, `danish`, `dutch`, `english`, `estonian`, `finnish`, `french`, `galician`, `german`, `greek`, `hindi`, `hungarian`, `indonesian`, `irish`, `italian`, `latvian`, `lithuanian`, `norwegian`, `persian`, `portuguese`, `romanian`, `russian`, `sorani`, `spanish`, `swedish`, `turkish`, `thai`

对于东亚地区，主要使用 `cjk`（Chinese, Japanese and Korean）分析器，分析方法就是按照单字依次进行分词。例如 `ABCD` 四个字的分词结果为 `AB`，`BC`，`CD`，

```json
PUT /analyzer-language
{
    "settings": {
        "analysis": {
            "analyzer": {
                "test_language_analyzer": {
                    "type": "cjk",
                    "stopwords": "_ckj_"
                }
            }
        }
    }
}

POST /analyzer-language/_analyze
{
    "analyzer": "test_language_analyzer",
    "text": "阿美首脑会议将讨论巴以和平等问题"
}
```

## 2. 自定义分析器

[Customize analyzer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-custom-analyzer.html)

自定义分析器即组合现有分词器和过滤器，达到更复杂的分析目标。主要包含如下参数：

- `tokenizer`: 指定分词器。分词器参见 [分词器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-tokenizers.html)
- `char_filter`: 字符过滤器 [字符过滤器](https://www.elastic.co/guide/en/elasticsearch/reference/7.16/analysis-charfilters.html)
- `filter`: 词过滤器 [词过滤器](https://www.elastic.co/guide/en/elasticsearch/reference/7.16/analysis-tokenfilters.html)
- `position_increment_gap`: 词汇间插入的间隙（参见 [文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/position-increment-gap.html)）

```json
PUT /analyzer-customized
{
    "settings": {
        "analysis": {
            "analyzer": {
                "test_customized_analyzer": {
                    "type": "custom",
                    "tokenizer": "standard",
                    "char_filter": [
                        "html_strip"
                    ],
                    "filter": [
                        "lowercase",
                        "asciifolding"
                    ]
                }
            }
        }
    }
}

POST /analyzer-customized/_analyze
{
    "analyzer": "test_customized_analyzer",
    "text": "Is this <b>déjà vu</b>?"
}
```

## 3. 更新分析器

文本分析器定义在索引的 `settings` 结构内，所以需要通过 `_setting` API 对其进行更新

**注意：** 更新索引前需要关闭索引，更新后再打开

```json
// 关闭索引
POST /analyzer-customized/_open

// 更新索引的 settings 结构。注意，此时无需在使用 settings: {...}，直接设置其内容即可
PUT /analyzer-customized/_settings
{
    "analysis": {
        "analyzer": {
            "test_customized_analyzer": {
                "type": "custom",
                "tokenizer": "standard",
                "char_filter": [
                    "html_strip"
                ],
                "filter": [
                    "lowercase",
                    "asciifolding"
                ]
            }
        }
    }
}

// 重新打开索引
POST /analyzer-customized/_open
```

## 4. 三方语法分析器

Elasticsearch 内建的语法分析器较为适合拉丁语系，对于中文这类表意文字则不是非常合适

此时需要安装第三方开发的语法分析器以弥补 Elasticsearch 的不足

以 [analysis-smartcn](https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-smartcn.html) 和 [analysis-icu](https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-icu.html)

### 4.1. 安装插件

#### 通过插件管理器安装

直接安装

```bash
$ sudo bin/elasticsearch-plugin install analysis-smartcn
$ sudo bin/elasticsearch-plugin install analysis-icu
```

通过 docker 容器安装

```bash
$ docker exec es bin/elasticsearch-plugin install analysis-smartcn
$ docker exec es bin/elasticsearch-plugin install analysis-icu
```

#### 下载后安装

下载插件文件

[analysis-smartcn-7.16.2.zip](https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-smartcn/analysis-smartcn-7.16.2.zip)
[analysis-icu-7.16.2.zip](https://artifacts.elastic.co/downloads/elasticsearch-plugins/analysis-icu/analysis-icu-7.16.2.zip)

直接安装

```bash
$ sudo bin/elasticsearch-plugin install file:///some.domain/path/to/analysis-smartcn-7.6.2.zip
$ sudo bin/elasticsearch-plugin install file:///some.domain/path/to/analysis-icu-7.6.2.zip
```

通过 docker 容器安装

需要将插件复制到容器能访问到的路径下

```bash
$ docker exec es bin/elasticsearch-plugin install file:///some.domain/path/to/analysis-smartcn-7.6.2.zip
$ docker exec es bin/elasticsearch-plugin install file:///some.domain/path/to/analysis-icu-7.6.2.zip
```

### 4.2. 删除插件

直接卸载

```bash
sudo bin/elasticsearch-plugin remove analysis-icu
sudo bin/elasticsearch-plugin remove analysis-smartcn
```

通过 docker 容器卸载

```bash
$ docker exec es bin/elasticsearch-plugin remove analysis-icu
$ docker exec es bin/elasticsearch-plugin remove analysis-smartcn
```

### 4.2. SmartCN 插件

[Analysis SmartCN](https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-smartcn.html)

智能中文分析插件将 Lucene 的智能中文分析模块集成到 Elasticsearch 中

它提供了一个中文或中英文混合文本的分析器。该分析器使用概率知识来寻找简体中文文本的最佳分词。文本首先被分解成句子，然后每个句子被分割成单词

```json
PUT /analyzer-smartcn
{
    "settings": {
        "analysis": {
            "analyzer": {
                "smartcn_analyzer": {
                    "tokenizer": "smartcn_tokenizer",
                    "filter": [
                        "porter_stem",
                        "smartcn_stop_filter"
                    ]
                }
            },
            "filter": {
                "smartcn_stop_filter": {
                    "type": "smartcn_stop",
                    "stopwords": [
                        "_smartcn_",
                        "stack",
                        "的"
                    ]
                }
            }
        }
    }
}

POST /analyzer-smartcn/_analyze
{
    "analyzer": "smartcn_analyzer",
    "text": "阿美首脑会议将讨论巴以和平等问题"
}
```

### 4.3. ICU 插件

[Analysis ICU](https://www.elastic.co/guide/en/elasticsearch/plugins/current/analysis-icu.html)

ICU 分析插件将 Lucene ICU 模块集成到 Elasticsearch 中，使用 ICU 库添扩展了对 Unicode 支持，包括更好地分析亚洲语言、Unicode 规范化、Unicode 大小写感知、排序规则支持和音调等

#### 默认 ICU 分析器

默认的 ICU 分析器使用内置的分词器和过滤器，可以达到不错的分析效果

```json
PUT /analyzer-icu
{
  "settings": {
        "analysis": {
            "analyzer": {
                "icu_analyzer": {
                    "type": "icu_analyzer",
                    "mode": "compose",
                    "method": "nfkc_cf"
                }
            }
        }
    }
}

POST /analyzer-icu/_analyze
{
    "analyzer": "icu_analyzer",
    "text": "阿美首脑会议将讨论巴以和平等问题"
}
```

#### 自定义字符过滤器

可以为分析器指定字符过滤器，以达到更符合应用场景的分词结果

```json
PUT /analyzer-icu
{
  "settings": {
        "analysis": {
            "analyzer": {
                "nfkc_cf_analyzer": {
                    "tokenizer": "icu_tokenizer",
                    "char_filter": [
                        "icu_normalizer"
                    ]
                },
                "nfd_analyzer": {
                    "tokenizer": "icu_tokenizer",
                    "char_filter": [
                        "nfd_normalizer"
                    ]
                }
            },
            "char_filter": {
                "nfd_normalizer": {
                    "type": "icu_normalizer",
                    "name": "nfc",
                    "mode": "decompose"
                }
            }
        }
    }
}

POST /analyzer-icu/_analyze
{
    "analyzer": "icu_analyzer",
    "text": "阿美首脑会议将讨论巴以和平等问题"
}
```

- `nfkc_cf_analyzer` 使用了默认的 `nfkc_cf` 正则化规则
- `nfd_analyzer` 使用了自定义的 `nfd_normalizer` 正则化规则，是在 `nfc` 正则的基础上增加了分解规则
