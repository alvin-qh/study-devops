# Percona XtraDB Cluster

## 1. 概述

PerconaDB 提供了一种集群建立模式 (OpenSource), 即 Percona XtraDB Cluster, 它是将 Percona Server for MySQL, Percona XtraBackup 以及 Calera 库集成在一起, 在实现数据库高可用的基础上, 提供了强数据一致性

这种模式类似于 MySQL 的 MGR 集群, 但提供了更简易的配置方式以及更强的数据一致性且无同步延迟

### 1.1. 架构

整个群集由节点组成, 其中每个节点都包含相同的数据集, 进行跨节点同步, 集群的每一台机器都可以读取和写入, 所有的读取都发生在本地, 不会获取任何远程数据, 而所有对数据的变更, 也会立即同步给集群中的所有节点

建议的配置是至少具有 `3` 个 节点，但也可以只有 `2` 个节点, 每个节点都是一个常规的 MySQL 数据库实例 (例如 Percona Server). 可以转换现有的 MySQL 服务器实例到节点, 并使用此节点作为基础运行群集, 也可以从群集中分离任何节点，并将其用作常规 MySQL 数据库实例, 非常灵活
