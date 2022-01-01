# 分词器

- [分词器](#分词器)
  - [1. 面向词的分词器](#1-面向词的分词器)
    - [1.1. 标准分词器](#11-标准分词器)
    - [1.2. 字符分词器](#12-字符分词器)
    - [1.3. 小写分词器](#13-小写分词器)
    - [1.4. 空格分词器](#14-空格分词器)
    - [1.5. URL 和 电子邮件 分词器](#15-url-和-电子邮件-分词器)
    - [1.6. 经典分词器](#16-经典分词器)
    - [1.7. 泰语分词器](#17-泰语分词器)
  - [2. 部分匹配分词器](#2-部分匹配分词器)
    - [2.1. N-Gram 分词器](#21-n-gram-分词器)
    - [2.2. Edge N-Gram 分词器](#22-edge-n-gram-分词器)
  - [3. 结构文本分词器](#3-结构文本分词器)
    - [3.1. 关键词分词器](#31-关键词分词器)
    - [3.2. 正则分词器](#32-正则分词器)
    - [3.3. 字符组分词器](#33-字符组分词器)
    - [3.4. 正则组分词器](#34-正则组分词器)
    - [3.5. 路径分词器](#35-路径分词器)

[内建分词器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-tokenizers.html)

“分词器”接收字符流，将其分解为单个词汇，并输出词汇流。例如，[空格分词器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-whitespace-tokenizer.html) 会在在看到任何空白时将文本分解为词汇。它会将文本 “Quick brown fox!” 转换成词汇流 `[Quick, brown, fox!]`

分词器还负责记录以下内容：

- 每个词汇的顺序或位置（用于短语和单词邻近查询）
- 该词汇表示的原始单词的开始和结束字符偏移量（相对于原始文本，用于突出显示搜索片段）
- 词汇类型，产生的每个词汇的分类，如 `<ALPHANUM>`， `<HANGUL>`，或`<NUM>`。更简单的分析程序只生成单词标记类型

Elasticsearch 有许多内建的分词器，可用于构建 [文本分析器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-custom-analyzer.html)

## 1. 面向词的分词器

Word Oriented Tokenizers，以下标记器通常用于将全文标记为单个单词

### 1.1. 标准分词器

[Standard Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-standard-tokenizer.html)

标准分词器提供了基于语法的记号化（基于 Unicode 文本分割算法，如 [Unicode标准附录#29](http://unicode.org/reports/tr29/) 中所述），并且适用于大多数语言

```json
PUT /analyzer-standard-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "standard_tokenizer_analyzer": {
                    "tokenizer": "standard_tokenizer"
                }
            },
            "tokenizer": {
                "standard_tokenizer": {
                    "type": "standard",
                    "max_token_length": 5
                }
            }
        }
    }
}

POST /analyzer-standard-tokenizer/_analyze
{
    "analyzer": "standard_tokenizer_analyzer",
    "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

- `max_token_length` 最大分词长度。如果看到令牌超过此长度，则将其按 `max_token_length` 间隔分割。默认为 `255`

### 1.2. 字符分词器

[Letter Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-letter-tokenizer.html)

读取文本流时，在遇到非字母的字符时将文本分解成词汇。对于大多数欧洲语言来说，它的作用还算合理，但对于一些亚洲语言来说，就很糟糕了，因为亚洲语言单词之间没有空格

```json
PUT /analyzer-letter-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "letter_tokenizer_analyzer": {
                    "tokenizer": "letter_tokenizer"
                }
            },
            "tokenizer": {
                "letter_tokenizer": {
                    "type": "letter"
                }
            }
        }
    }
}

POST /analyzer-letter-tokenizer/_analyze
{
    "analyzer": "letter_tokenizer_analyzer",
    "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

### 1.3. 小写分词器

[`Lowercase Tokenizer`](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-lowercase-tokenizer.html)

功能上相当于 [Letter 分词器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-letter-tokenizer.html) 与 [Lowercase 词过滤器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-lowercase-tokenfilter.html) 的组合，但更有效，因为它在单次传递中执行这两个步骤

```json
PUT /analyzer-lowercase-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "lowercase_tokenizer_analyzer": {
                    "tokenizer": "lowercase_tokenizer"
                }
            },
            "tokenizer": {
                "lowercase_tokenizer": {
                    "type": "lowercase"
                }
            }
        }
    }
}

POST /analyzer-lowercase-tokenizer/_analyze
{
    "analyzer": "lowercase_tokenizer_analyzer",
    "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

### 1.4. 空格分词器

[Whitespace Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-whitespace-tokenizer.html)

每当遇到一个空白字符进行分词

```json
PUT /analyzer-whitespace-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "whitespace_tokenizer_analyzer": {
                    "tokenizer": "whitespace_tokenizer"
                }
            },
            "tokenizer": {
                "whitespace_tokenizer": {
                    "type": "whitespace",
                    "max_token_length": 50
                }
            }
        }
    }
}

POST /analyzer-whitespace-tokenizer/_analyze
{
  "analyzer": "whitespace_tokenizer_analyzer",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

- `max_token_length` 最大分词长度。如果看到令牌超过此长度，则将其按 `max_token_length` 间隔分割。默认为 `255`

### 1.5. URL 和 电子邮件 分词器

[UAX URL Email Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-uaxurlemail-tokenizer.html)

与标准的标记器类似，只是它将 **URL** 和 **电子邮件地址** 识别为单个词汇

```json
PUT /analyzer-uax-url-email-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "uax_url_email_tokenizer_analyzer": {
                    "tokenizer": "uax_url_email_tokenizer"
                }
            },
            "tokenizer": {
                "uax_url_email_tokenizer": {
                    "type": "uax_url_email",
                    "max_token_length": 120
                }
            }
        }
    }
}

POST /analyzer-uax-url-email-tokenizer/_analyze
{
  "analyzer": "uax_url_email_tokenizer_analyzer",
  "text": "Email me at john.smith@global-international.com"
}
```

- `max_token_length` 最大分词长度。如果看到令牌超过此长度，则将其按 `max_token_length` 间隔分割。默认为 `255`

### 1.6. 经典分词器

[Classic Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-classic-tokenizer.html)

经典分词器器是基于语法的，适用于英语文档。该分词器对缩写词、公司名称、电子邮件地址和网址的特殊处理具有规则。然而，这些规则并不总是有效，分词器并不适用于除英语以外的大多数语言:

- 主要依赖于标点符号分词，并去掉标点符号。但是没有空格的 `.` 被认为是词汇的一部分
- 它在连字符处拆分单词，除非词汇中有数字，在这种情况下，整个词汇被解释为产品编号，不进行拆分
- 它将电子邮件地址和互联网主机名识别为一个词汇

```json
PUT /analyzer-classic-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "classic_tokenizer_analyzer": {
                    "tokenizer": "classic_tokenizer"
                }
            },
            "tokenizer": {
                "classic_tokenizer": {
                    "type": "classic",
                    "max_token_length": 120
                }
            }
        }
    }
}

POST /analyzer-classic-tokenizer/_analyze
{
  "analyzer": "classic_tokenizer_analyzer",
  "text": "The 2 QUICK Brown-Foxes jumped over the lazy dog's bone."
}
```

- `max_token_length` 最大分词长度。如果看到令牌超过此长度，则将其按 `max_token_length` 间隔分割。默认为 `255`

### 1.7. 泰语分词器

[Thai Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-thai-tokenizer.html)

泰语标记器将泰语文本分割成单词，使用Java中包含的泰语分割算法。其他语言文本将被视为标准的分词

```json
// cspell: disable

PUT /analyzer-tai-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "thai_tokenizer_analyzer": {
                    "tokenizer": "thai_tokenizer"
                }
            },
            "tokenizer": {
                "thai_tokenizer": {
                    "type": "thai"
                }
            }
        }
    }
}

POST /analyzer-tai-tokenizer/_analyze
{
    "analyzer": "thai_tokenizer_analyzer",
    "text": "การที่ได้ต้องแสดงว่างานดี"
}

// cspell: enable
```

## 2. 部分匹配分词器

Partial World Tokenizers，这些标记器将文本或单词分解成小片段，用于部分匹配

### 2.1. N-Gram 分词器

[N-gram Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-ngram-tokenizer.html)

类似于一个滑动窗口，在单词上移动一个指定长度的连续字符序列。它们对于查询不使用空格或具有长复合词的语言（如德语）非常有用

```json
PUT /analyzer-ngram-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "ngram_tokenizer_analyzer": {
                    "tokenizer": "ngram_tokenizer"
                }
            },
            "tokenizer": {
                "ngram_tokenizer": {
                    "type": "ngram",
                    "min_gram": 3,
                    "max_gram": 3,
                    "token_chars": [
                        "letter",
                        "digit"
                    ]
                }
            }
        }
    }
}

POST /analyzer-ngram-tokenizer/_analyze
{
    "analyzer": "ngram_tokenizer_analyzer",
    "text": "2 Quick Foxes. "
}
```

- `min_gram` 分割的最小长度，默认为`1`
- `max_gram` 分割的最大长度，默认为`2`
- `token_chars` 应该包含在词汇中的字符类，这部分文本不进行拆分。默认为`[]`(保留所有字符)。字符类包括
  - `letter` 字母或字，例如 `a`, `b`, `ï` or `京`
  - `digit` 数字，例如 `3` or `7`
  - `whitespace` 空白字符，例如 `" "` or `"\n"`
  - `punctuation` 标点符号，例如 `!` or `"`
  - `symbol` 其它符合 `$` or `√`;
  - `custom` 需要使用 `custom_token_chars` 设置来设置的自定义字符
  - `custom_token_chars` 视为词汇的一部分的自定义字符，例如设置为 `+-_` 会使得分词器将正负号和下划线视为词汇的一部分

### 2.2. Edge N-Gram 分词器

[Edge N-gram Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-edgengram-tokenizer.html)

在遇到指定字符列表时将文本分解为单词，然后发出每个单词的 N-Gram，其中 N-gram 的开头锚定到单词的开头

```json
PUT /analyzer-edge-ngram-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "edge_ngram_tokenizer_analyzer": {
                    "tokenizer": "edge_ngram_tokenizer"
                }
            },
            "tokenizer": {
                "edge_ngram_tokenizer": {
                    "type": "ngram",
                    "min_gram": 3,
                    "max_gram": 3,
                    "token_chars": [
                        "letter",
                        "digit"
                    ]
                }
            }
        }
    }
}

POST /analyzer-edge-ngram-tokenizer/_analyze
{
    "analyzer": "edge_ngram_tokenizer_analyzer",
    "text": "Quick Fox"
}
```

- `min_gram` 分割的最小长度，默认为`1`
- `max_gram` 分割的最大长度，默认为`2`
- `token_chars` 应该包含在词汇中的字符类，这部分文本不进行拆分。默认为`[]`(保留所有字符)。字符类包括
  - `letter` 字母或字，例如 `a`, `b`, `ï` or `京`
  - `digit` 数字，例如 `3` or `7`
  - `whitespace` 空白字符，例如 `" "` or `"\n"`
  - `punctuation` 标点符号，例如 `!` or `"`
  - `symbol` 其它符合 `$` or `√`;
  - `custom` 需要使用 `custom_token_chars` 设置来设置的自定义字符
  - `custom_token_chars` 视为词汇的一部分的自定义字符，例如设置为 `+-_` 会使得分词器将正负号和下划线视为词汇的一部分

## 3. 结构文本分词器

### 3.1. 关键词分词器

[Keyword Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-keyword-tokenizer.html)

一个 "noop" 标记器，它接受它提供的任何文本，并输出与单个术语完全相同的文本。它可以与过滤器组合，使输出规范化，例如小写电子邮件地址

```json
PUT /analyzer-keyword-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "keyword_tokenizer_analyzer": {
                    "tokenizer": "keyword_tokenizer"
                }
            },
            "tokenizer": {
                "keyword_tokenizer": {
                    "type": "keyword",
                    "buffer_size": 256
                }
            }
        }
    }
}

POST /analyzer-keyword-tokenizer/_analyze
{
    "analyzer": "keyword_tokenizer_analyzer",
    "text": "New York"
}
```

- `buffer_size` 在一次传递中读取到缓冲区中的字符数，默认值为 `256`。缓冲区会根据此大小增长，直到存储所有文本。建议不要更改此设置

### 3.2. 正则分词器

[Pattern Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-pattern-tokenizer.html)
[Simple Pattern Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-simplepattern-tokenizer.html)

使用正则表达式在文本分隔符匹配时将文本拆分为词汇，或者将匹配的文本捕获为词汇。默认的正则表达式为`\W+`

```json
PUT /analyzer-pattern-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "pattern_tokenizer_analyzer": {
                    "tokenizer": "pattern_tokenizer"
                }
            },
            "tokenizer": {
                "pattern_tokenizer": {
                    "type": "pattern",
                    "pattern": ","
                }
            }
        }
    }
}

POST /analyzer-pattern-tokenizer/_analyze
{
    "analyzer": "pattern_tokenizer_analyzer",
    "text": "comma,separated,values"
}
```

- `pattern` 正则表达式，默认为 `\w+`
- `flags` 标志位，Java 正则表达式标记位。可以用 `|` 组合，例如：`CASE_INSENSITIVE|COMMENTS`
- `group` 指定分组编号为所提取词汇，默认为 `-1`，表示全部拆分

### 3.3. 字符组分词器

[Character Group Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/7.16/analysis-chargroup-tokenizer.html)

遇到字符组中的字符时将文本分解为词汇（例如 `,`）

```json
PUT /analyzer-char-group-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "char_group_tokenizer_analyzer": {
                    "tokenizer": "char_group_tokenizer"
                }
            },
            "tokenizer": {
                "char_group_tokenizer": {
                    "type": "char_group",
                    "tokenize_on_chars": [
                        "whitespace",
                        "-",
                        "\n"
                    ]
                }
            }
        }
    }
}

POST /analyzer-char-group-tokenizer/_analyze
{
    "analyzer": "char_group_tokenizer_analyzer",
    "text": "The QUICK brown-fox"
}
```

- `tokenize_on_chars` 包含用于对字符串进行标记的字符列表。每当遇到此列表中的字符时，就会分割一个新词汇。它可以接受单个字符(如 `-`)，也可以接受字符组: `whitespace`, `letter`, `digit`, `punctuation` or `symbol`

### 3.4. 正则组分词器

[Simple Pattern Split Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-simplepatternsplit-tokenizer.html)

通过一个正则表达式将输入分割为多个词汇。它支持的正则表达式特性集比**模式分词器**更有限，但执行速度通常更快

该分词器并不通过匹配项本身进行分词。要使用同一个受限制的正则表达式子集中的模式从匹配中生成词汇，请参阅 [Simple Pattern 分词器](https://www.elastic.co/guide/en/elasticsearch/reference/current/analysis-simplepattern-tokenizer.html)

这个分词器使用 [Lucene 正则表达式](http://lucene.apache.org/core/8_4_0/core/org/apache/lucene/util/automaton/RegExp.html)。有关受支持的特性和语法的说明，请参阅 [正则表达式语法](https://www.elastic.co/guide/en/elasticsearch/reference/current/regexp-syntax.html)

默认模式是空字符串。此分词器应 **始终配置为非默认模式**

```json
PUT /analyzer-simple-pattern-split-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "simple_pattern_split_tokenizer_analyzer": {
                    "tokenizer": "simple_pattern_split_tokenizer"
                }
            },
            "tokenizer": {
                "simple_pattern_split_tokenizer": {
                    "type": "simple_pattern_split",
                    "pattern": "_"
                }
            }
        }
    }
}

POST /analyzer-simple-pattern-split-tokenizer/_analyze
{
    "analyzer": "simple_pattern_split_tokenizer_analyzer",
    "text": "an_underscored_phrase"
}
```

- `pattern` [Lucene 正则表达式](https://lucene.apache.org/core/8_4_0/core/org/apache/lucene/util/automaton/RegExp.html)，默认为空字符串

### 3.5. 路径分词器

[Path Hierarchy Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/7.16/analysis-pathhierarchy-tokenizer.html)

接受一个类似于文件系统路径的层次值，在路径分隔符上进行分割

```json
PUT /analyzer-path-hierarchy-tokenizer
{
  "settings": {
        "analysis": {
            "analyzer": {
                "path_hierarchy_tokenizer_analyzer": {
                    "tokenizer": "path_hierarchy_tokenizer"
                }
            },
            "tokenizer": {
                "path_hierarchy_tokenizer": {
                    "type": "path_hierarchy",
                    "reverse": true,
                    "delimiter": "-",
                    "replacement": "/",
                    "skip": 2
                }
            }
        }
    }
}

POST /analyzer-path-hierarchy-tokenizer/_analyze
{
    "analyzer": "path_hierarchy_tokenizer_analyzer",
    "text": "one-two-three-four-five"
}
```

- `delimiter` 要用作路径分隔符的字符。默认为 `/`
- `replacement` 用于分隔符的可选替换字符。默认为 `delimiter`
- `buffer_size` 在每次遍历中读入缓冲区的字符数。默认为 `1024`。缓冲区将以这个大小增长，直到所有文本都被使用完。建议不要更改此设置
- `reverse` 如果设置为 `true`，则以相反的顺序发出词汇。默认值为 `false`
- `skip` 要跳过的初始字符的数量。默认值为`0`
