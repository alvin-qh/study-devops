# 相似性模块

- [相似性模块](#相似性模块)
  - [1. 配置相似性模块](#1-配置相似性模块)
  - [2. 内建的相似性模块](#2-内建的相似性模块)
    - [2.1. BM25](#21-bm25)
    - [2.2. DFR](#22-dfr)
    - [2.3. DFI](#23-dfi)
    - [2.4. IB](#24-ib)
    - [2.5. LM Dirichlet](#25-lm-dirichlet)
    - [2.6. LM Jelinek Mercer](#26-lm-jelinek-mercer)
  - [3. 自定义相似性模块](#3-自定义相似性模块)

[Similarity Module](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules-similarity.html)

在定义索引时，可以为索引定义**相似性模块**，即索引 `settings` > `similarity` 模块

相似性（评分/排名模型）定义了检索结果的评分方式。相似性可以定义在文档的各个字段，即每个字段都可以以不同的相似性进行检索和评估

自定义相似性配置是高级功能，一般 Elasticsearch 内建的相似性模型即可满足需求

## 1. 配置相似性模块

基本所有的内建/自定义相似性模块都可以作为配置选项进行设置

```json
PUT /<index>
{
    "settings": {
        "similarity": {
            "dfr_similarity": {
                "type": "DFR",
                "basic_model": "g",
                "after_effect": "l",
                "normalization": "h2",
                "normalization.h2.c": "3.0"
            }
        }
    }
}
```

## 2. 内建的相似性模块

### 2.1. BM25

默认的相似性模块，基于 TF/IDF 算法，具有内置的 tf 规范化，在应对短字段时具有较好的效果。参考 [Okapi_BM25](https://en.wikipedia.org/wiki/Okapi_BM25)。具有如下选项：

- `k1` 控制非线性项归一化。默认值 `1.2`
- `b` 控制文档长度标准化 tf 值的程度。默认值 `0.75`
- `discount_overlaps` 计算范数时是否忽略重叠标记。默认值为 `true`，表示在计算范数时重叠标记不计算在内

### 2.2. DFR

通过 [Lucene 随机性发散](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/DFRSimilarity.html) 框架计算相似性。具有以下选项：

- `basic_model` 取值为 [`g`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/BasicModelG.html), [`if`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/BasicModelIF.html) [`in`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/BasicModelIn.html) 和 [`ine`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/BasicModelIne.html)
- `after_effect` 取值为 [`b`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/AfterEffectB.html) 和 [`l`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/AfterEffectL.html)
- `normalization` 取值为 [`no`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/Normalization.NoNormalization.html), [`h1`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/NormalizationH1.html), [`h2`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/NormalizationH2.html), [`h3`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/NormalizationH3.html) 和 [`z`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/NormalizationZ.html)

### 2.3. DFI

基于 [divergence from independence](https://trec.nist.gov/pubs/trec21/papers/irra.web.nb.pdf) 模型实现。具备以下选项：

- `independence_measure` 取值为 [`standardized`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/IndependenceStandardized.html), [`saturated`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/IndependenceSaturated.html) 和 [`chisquared`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/IndependenceChiSquared.html)

使用这种相似性模块时，建议不要删除**停用词**以获得更好的相关性。另外需注意频率低于预期频率的术语将获得等于 `0` 的分数

### 2.4. IB

[基于信息](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/IBSimilarity.html) 模型。该算法基于各类字符分布序列中的信息内容，主要由其基本字符元素的重复使用情况决定。对于书面文本，该算法对应于比较不同作者的写作风格。具有以下选项：

- `distribution` 取值为 [`ll`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/DistributionLL.html) 和 [`spl`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/DistributionSPL.html)
- `lambda` 取值为 [`df`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/LambdaDF.html) 和 [`ttf`](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/LambdaTTF.html)
- `normalization` 取值和 DFR 模块类似

### 2.5. LM Dirichlet

基于 [LM Dirichlet](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/LMDirichletSimilarity.html) 相似性模块。具有以下选项

- `mu` 默认值为 `2000`

论文中的评分公式会给**出现次数少于模型预测值的词判负分**，这在 Lucene 中是非法的，所以在 Lucene 中，此类词的评分结果为 `0`

### 2.6. LM Jelinek Mercer

[LM Jelinek Mercer](https://lucene.apache.org/core/8_10_1/core/org/apache/lucene/search/similarities/LMJelinekMercerSimilarity.html) 相似性模块。该模块用以捕捉文本中的重点信息，并试图排除噪音。具备以下选项：

- `lambda` 需要根据文本特性来匹配最佳值。标题查询的最佳值约为 `0.1`，长文本查询的最佳值约为 `0.7`。默认值为 `0.1`。匹配查询命中越多的结果排名将会越高

## 3. 自定义相似性模块

可以通过脚本来自定义相似性算法，例如一个 TF-IDF 相似性模块可定义如下：

```json
PUT /index
{
    "settings": {
        "number_of_shards": 1,
        "similarity": {
            "scripted_tfidf": {
                "type": "scripted",
                "script": {
                    "source": "double tf = Math.sqrt(doc.freq); double idf = Math.log((field.docCount+1.0)/(term.docFreq+1.0)) + 1.0; double norm = 1/Math.sqrt(doc.length); return query.boost * tf * idf * norm;"
                }
            }
        }
    },
    "mappings": {
        "properties": {
            "field": {
                "type": "text",
                "similarity": "scripted_tfidf"
            }
        }
    }
}
```
