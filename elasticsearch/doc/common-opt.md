# 常用选项

- [常用选项](#常用选项)
  - [优化结果输出](#优化结果输出)
  - [人机交互](#人机交互)
  - [日期计算](#日期计算)
  - [响应过滤](#响应过滤)
  - [平坦模式](#平坦模式)
  - [参数](#参数)
  - [布尔值](#布尔值)
  - [时间单位](#时间单位)
  - [字节大小单位](#字节大小单位)
  - [距离单位](#距离单位)
  - [模糊](#模糊)
  - [启用堆栈跟踪](#启用堆栈跟踪)

[Common Options](https://www.elastic.co/guide/en/elasticsearch/reference/current/common-options.html)

以下选项可应用于所有 REST API

## 优化结果输出

可以通过在 **任意请求** 后加 `?pretty=true` 来格式化输出的 json 数据，使其更具可读性（仅用于调试）。 另一种选择是添加 `?format=yaml` 可以使返回结果以 yaml 格式输出

## 人机交互

统计数据可以以适合人类理解的方式返回（例如：`"exists_time": "1h"` 或 `"size": "1kb"`）而不是便于机器理解（例如 `"exists_time_in_millis": 3600000` 或 `"size_in_bytes": 1024`）可以通过设置 `?human=false/true` 来关闭或开启（默认为 `false`）

## 日期计算

大多数接受日期格式化值的参数（例如 范围查询 或 日期聚合查询）中，均可处理日期计算

表达式以日期形式开始（例如 `now` 或 日期字符串）或以 `||` 结束，这些日期可参与如下计算：

- `+1h` 增加 1 小时
- `-1d` 减少 1 天
- `/d` 向下舍入到最接近的日期

The supported time units differ from those supported by time units for durations. The supported units are:

- `y` 年
- `M` 月份
- `w` 星期
- `d` 日
- `h`, `H` 小时（12 或 24 形式）
- `m` 分钟
- `s` 秒

假设当前日期为 `2001-01-01 12:00:00`，则可举例如下：

- `now+1h` 增加 1 小时，结果为 `2001-01-01 13:00:00`
- `now-1h` 减小 1 小时，结果为 `2001-01-01 11:00:00`
- `now-1h/d` 减少 1 小时并向下舍入到 UTC 00:00，结果为 `2001-01-01 00:00:00`
- `2001.02.01\|\|+1M/d` `2001-02-01` 增加 1 个月，结果为 `2001-03-01 00:00:00`

## 响应过滤

所有的 REST APIs 均可使用 `filter_path` 参数来减少从 Elasticsearch 返回的内容，可以用 `.` 来分隔属性路径

```json
GET /_search?q=kimchy&filter_path=took,hits.hits._id,hits.hits._score
```

响应为

```json
{
    "took" : 3,
    "hits" : {
        "hits" : [
            {
                "_id" : "0",
                "_score" : 1.6375021
            }
        ]
    }
}
```

也可以使用 `*` 通配符来匹配任意属性路径和属性字段名（或一部分），例如：

```json
GET /_cluster/state?filter_path=metadata.indices.*.stat*
```

响应为

```json
{
    "metadata" : {
        "indices" : {
            "my-index-000001": { "state": "open" }
        }
    }
}
```

也可以在不确定确切属性路径时，通过 `**` 通配符来查找所有可能的字段。例如，可以获取所有和 Lucene version 相关的结果:

```json
GET /_cluster/state?filter_path=routing_table.indices.**.state
```

响应为

```json
{
    "routing_table": {
        "indices": {
            "my-index-000001": {
                "shards": {
                    "0": [ { "state": "STARTED" }, { "state": "UNASSIGNED" } ]
                }
            }
        }
    }
}
```

也可以通过 `-` 符号将某些字段从结果中排除，例如排除结果中的 `_shards` 字段

```json
GET /_count?filter_path=-_shards
```

响应为

```json
{
    "count" : 5
}
```

## 平坦模式

参数 `flat_settings` 可以开启平坦模式，使用 `.` 连接属性路径（而不是 json 层级），可以减少返回 json 的复杂度

```json
GET my-index-000001/_settings?flat_settings=true
```

响应为

```json
{
    "my-index-000001" : {
        "settings": {
            "index.number_of_replicas": "1",
            "index.number_of_shards": "1",
            "index.creation_date": "1474389951325",
            "index.uuid": "n6gzFZTgS664GUfx0Xrpjw",
            "index.version.created": ...,
            "index.routing.allocation.include._tier_preference" : "data_content",
            "index.provided_name" : "my-index-000001"
        }
    }
}
```

如果 `flat_settings` 设置为 `false`（默认值）, 则会返回完整的 json 结构

```json
GET my-index-000001/_settings?flat_settings=false
```

响应为

```json
{
    "my-index-000001" : {
        "settings" : {
            "index" : {
                "number_of_replicas": "1",
                "number_of_shards": "1",
                "creation_date": "1474389951325",
                "uuid": "n6gzFZTgS664GUfx0Xrpjw",
                "version": {
                    "created": ...
                },
                "routing": {
                    "allocation": {
                        "include": {
                            "_tier_preference": "data_content"
                        }
                    }
                },
                "provided_name" : "my-index-000001"
            }
        }
    }
}
```

## 参数

REST 参数名使用下划线连接单词的形式

## 布尔值

使用字符串 `false` 和 `true` 表示 假 和 真

## 时间单位

时间单位包括

- `d` 天
- `h` 小时
- `m` 分钟
- `s` 秒
- `ms` 毫秒
- `micros` 微秒
- `nanos` 纳秒

## 字节大小单位

字节大小的单位包括

- `b` Bytes
- `kb` Kilobytes
- `mb` Megabytes
- `gb` Gigabytes
- `tb` Terabytes
- `pb` Petabytes

## 距离单位

在使用距离的场合（例如 [Geo 距离](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-geo-distance-query.html)），默认的单位为 `m`（米），其它可用单位为：

- `mi` 或 `miles` 英里
- `yd` 或 `yards` 码
- `ft` 或 `feet` 英尺
- `in` 或 `inch` 英寸
- `km` 或 `kilometers` 千米
- `m` 或 `meters` 米
- `cm` 或 `centimeters` 厘米
- `mm` 或 `millimeters` 毫米
- `NM`, `nmi`, 或 `nauticalmiles` 海里

## 模糊

一些查询 API 支持一些参数使用**模糊匹配**

当查询 `text` 类型或 `keyword` 类型字段时，模糊被解释为一个 [Levenshtein 编辑距离](https://en.wikipedia.org/wiki/Levenshtein_distance)——表示一个字符串可以通过多少次修改和另一个字符串匹配

`fuzziness` 参数可以定义为：

- `0, 1, 2` 允许的最大编辑距离（或编辑次数）
- `AUTO` 根据文本长度自动生成编辑距离。也可以选择性的提供高低距离参数，`AUTO:[low][,heigh]`，默认为 `AUTO:3,6`
  - `0..2` 必须完全匹配
  - `3..5` 允许一次编辑
  - `>5` 允许两次编辑

## 启用堆栈跟踪

可以在 API 请求 URL 中增加 `error_trace` 参数已开启错误堆栈跟踪。

```json
POST /my-index-000001/_search?size=surprise_me&error_trace=true
```

返回响应类似于

```json
{
    "error": {
        "root_cause": [
            {
                "type": "illegal_argument_exception",
                "reason": "Failed to parse int parameter [size] with value [surprise_me]",
                "stack_trace": "Failed to parse int parameter [size] with value [surprise_me]]; nested: IllegalArgumentException..."
            }
        ],
        "type": "illegal_argument_exception",
        "reason": "Failed to parse int parameter [size] with value [surprise_me]",
        "stack_trace": "java.lang.IllegalArgumentException: Failed to parse int parameter [size] with value [surprise_me]\n    at org.elasticsearch.rest.RestRequest.paramAsInt(RestRequest.java:175)...",
        "caused_by": {
            "type": "number_format_exception",
            "reason": "For input string: \"surprise_me\"",
            "stack_trace": "java.lang.NumberFormatException: For input string: \"surprise_me\"\n    at java.lang.NumberFormatException.forInputString(NumberFormatException.java:65)..."
        }
    },
    "status": 400
}
```
