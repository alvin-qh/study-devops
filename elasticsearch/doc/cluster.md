# 集群

- [集群](#集群)
  - [1. 集群健康](#1-集群健康)
    - [1.1. 集群健康](#11-集群健康)
      - [1.1.1. 健康情况概要](#111-健康情况概要)
      - [1.1.2. 集群监控情况](#112-集群监控情况)
    - [1.2. 集群节点](#12-集群节点)
      - [1.2.1. 主节点信息](#121-主节点信息)
      - [1.2.2. 所有节点信息](#122-所有节点信息)
  - [2. 集群和索引](#2-集群和索引)
    - [2.1. 在集群上建立索引](#21-在集群上建立索引)
    - [2.2. 索引在集群中的状态](#22-索引在集群中的状态)
      - [2.2.1. 集群索引信息](#221-集群索引信息)
      - [2.2.2. 显示索引分片信息](#222-显示索引分片信息)

## 1. 集群健康

### 1.1. 集群健康

#### 1.1.1. 健康情况概要

```json
GET /_cat/health?format=json
```

#### 1.1.2. 集群监控情况

```json
GET /_cluster/health?format=json
```

### 1.2. 集群节点

#### 1.2.1. 主节点信息

```json
GET /_cat/master?format=json
```

#### 1.2.2. 所有节点信息

```json
GET /_cat/nodes?format=json
```

## 2. 集群和索引

### 2.1. 在集群上建立索引

查看现有索引

```json
GET /person/_settings
```

关闭现有索引

```json
POST /person/_close
```

更新索引设置，重新设置索引的副本数和分片数

```json
PUT /person/_settings
{
  "settings": {
      "number_of_replicas": 2,
      "number_of_shards": 2
    }
}
```

### 2.2. 索引在集群中的状态

#### 2.2.1. 集群索引信息

```json
GET /_cat/indices?v=true&h=health,status,index,pri,rep,docs.count,docs.deleted,store.size&s=store.size:desc
```

#### 2.2.2. 显示索引分片信息

```json
GET /_cat/shards?v=true&s=store:desc
```
