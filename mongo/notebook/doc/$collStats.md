# `$collStats` 聚合

返回有关集合或视图的统计信息。

## 1. 定义

### 1.1. 输入形式

```javascript
{
  $collStats:
    {
      latencyStats: { histograms: <boolean> },
      storageStats: { scale: <number> },
      count: {}
    }
}
```

字段说明：

- `latencyStats`: 向返回文档添加延迟统计信息。

- `latencyStats.histograms`: 如果`latencyStats`值为`true`，则将延迟直方图信息添加到 中的嵌入文档。

- `storageStats`: 将存储统计信息添加到返回文档。
    - `storageStats: {}`: 对各种大小数据使用默认比例因子`1`(比例尺`1`以字节为单位显示返回的大小)。
    - `storageStats: {scale: <number>}`: 用于对各种大小数据使用指定的比例因子。例如，要显示千字节而不是字节，可以指定比例值`1024`。
    
    如果指定非整数比例因子，MongoDB 将使用指定因子的整数部分。
    
    比例因子不会影响在字段名称中指定度量单位的大小。

- `count`: 将集合中的文档总数添加到返回文档。


### 1.2. 输出

对于副本集中的集合或群集中的非分片集合，`$collStats`输出单个文档。对于分片集合，`$collStats`会为每个分片输出一个文档。输出文档包括以下字段：

字段说明：

- `ns`： 请求的集合或视图的命名空间。
- `shard`: 输出文档对应的分片的名称。
    
    仅当`$collStats`运行在分片集群时该字段有效，但都将生成此字段。

    > 版本 3.6 中的新增功能。

- `host`: 主机名和端口号。

    > 版本 3.6 中的新增功能。

- `localTime`: MongoDB 服务器上的当前时间，Unix timestamp 格式。

- `latencyStats`: 与集合或视图的请求延迟相关的统计信息的集合。

    仅在指定选项时存在。`latencyStats: {}`

- `storageStats`: 与集合的存储引擎相关的统计信息的集合。

    数值会按指定因子缩放（字段名称中指定度量单位的大小除外）。

    仅在指定`storageStats`选项时存在。`storageStats: {scale: <number>}`

    如果应用于视图 (View)，则返回错误。

- `count`: 集合中的文档总数。此数据也可在`storageStats.count`中提供。

    > 注意: 计数基于集合的元数据，该元数据为分片群集提供了快速但有时不准确的计数。
    
    仅在指定`count: {}`选项时存在。
    
    如果应用于视图 (View)，则返回错误。
    

### 1.3. 行为

`$collStats`必须是聚合管道中的第一阶段，否则返回错误。


### 1.4. 事务

`$collStats`不允许在事务中使用


## 2. `latencyStats`

The  embedded document only exists in the output if you specify the latencyStats option.

仅当指定`latencyStats`选项时，嵌入的文档才会存在于输出中。


### 2.1. 输出形式

输出文档中包括如下字段:

- `reads`: 读取请求的延迟统计信息。

- `writes`: 写入请求的延迟统计信息。

- `commands`: 数据库命令的延迟统计信息。

其中，每个字段又会包含如下子文档，子文档包含字段如下:

- `latency`: 64位整数，表示总体延迟时间（以微秒为单位）。

- `ops`: 64位整数，提供自启动以来对集合执行的操作总数。

- `histogram`: 数组类型，表示一个延迟时间范围的直方图。数组每项表示一个延迟范围。每个文档涵盖前一个文档范围的两倍。对于 2048 微秒和大约 1 秒之间的上限值，直方图包括半步。

    此字段仅存在于给定`latencyStats: { histograms: true }`选项的情况下。输出中省略零的空范围。
    
    每个文档都包含以下字段：
    
    - `micros`: 64位整数，以微秒为单位提供当前延迟范围的包含上限。
    
        数值范围介于上一个文档的值 (排除) 和本文档的值 (包含) 之间。
        
    - `count`: 64位整数，用于提供延迟小于或等于指定值的操作数。
    
        例如，如果返回以下直方图：
        
        ```javascript
        histogram: [
          { micros: NumberLong(1), count: NumberLong(10) },
          { micros: NumberLong(2), count: NumberLong(1) },
          { micros: NumberLong(4096), count: NumberLong(1) },
          { micros: NumberLong(16384), count: NumberLong(1000) },
          { micros: NumberLong(49152), count: NumberLong(100) }
        ]
        ```
        
        这表明有：
        
        - `10`个操作，需要`1`微秒或更少
        - `1`个操作，范围`(1, 2]`微秒
        - `1`个操作，`(3072, 4096]`微秒
        - `1000`个操作`(12288, 16384]`微秒
        - `100`个操作`(32768, 49152]`微秒
 
 
例如，如果在`matrices`集合上以`$collStatslatencyStats: {}`选项运行：
 
```javascript
db.matrices.aggregate([ 
     {
         $collStats: {
             latencyStats: { 
                 histograms: true 
             }
         }
     }
])
```
 
此查询返回的结果类似于以下内容：

```json
{ "ns" : "test.matrices",
  "host" : mongo.example.net:27017",
  "localTime" : ISODate("2017-10-06T19:43:56.599Z"),
  "latencyStats" :
    { "reads" :
        { "histogram" : [
            { "micros" : NumberLong(16),
              "count" : NumberLong(3) },
            { "micros" : NumberLong(32),
              "count" : NumberLong(1) },
            { "micros" : NumberLong(128),
              "count" : NumberLong(1) } ],
          "latency" : NumberLong(264),
          "ops" : NumberLong(5) },
      "writes" :
        { "histogram" : [
            { "micros" : NumberLong(32),
              "count" : NumberLong(1) },
            { "micros" : NumberLong(64),
              "count" : NumberLong(3) },
            { "micros" : NumberLong(24576),
              "count" : NumberLong(1) } ],
          "latency" : NumberLong(27659),
          "ops" : NumberLong(5) },
      "commands" :
        { "histogram" : [
            {
               "micros" : NumberLong(196608),
               "count" : NumberLong(1)
            }
          ],
          "latency" : NumberLong(0),
          "ops" : NumberLong(0) },
      "transactions" : {
         "histogram" : [ ],
         "latency" : NumberLong(0),
         "ops" : NumberLong(0)
      }
    }
}
```

## 3. `storageStats`

仅当指定`storageStats`选项时，嵌入的文档才会存在于输出中。

本文档的内容取决于正在使用的存储引擎。有关本文档的参考，请参阅输出。