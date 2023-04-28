# ProxySQL 路由规则

- [ProxySQL 路由规则](#proxysql-路由规则)
  - [1. 概述](#1-概述)
    - [1.1. mysql\_query\_rules 表详解](#11-mysql_query_rules-表详解)
  - [2. 路由规则](#2-路由规则)
    - [2.1. 基于端口的路由](#21-基于端口的路由)
    - [2.2. 基于用户的路由](#22-基于用户的路由)
    - [2.3. 基于数据库名的路由](#23-基于数据库名的路由)
    - [2.4. 基于 SQL 语句的路由](#24-基于-sql-语句的路由)
      - [2.4.1. SQL语句参数化](#241-sql语句参数化)
      - [2.4.2. 基于参数化 SQL 散列值匹配的路由](#242-基于参数化-sql-散列值匹配的路由)
      - [2.4.3. 基于参数化 SQL 正则匹配的路由](#243-基于参数化-sql-正则匹配的路由)
      - [2.4.4. 基于原始 SQL 正则匹配的路由](#244-基于原始-sql-正则匹配的路由)
      - [2.4.5. 实用的读写分离](#245-实用的读写分离)
  - [3. 路由相关的统计表](#3-路由相关的统计表)
    - [3.1. stats\_mysql\_query\_digest](#31-stats_mysql_query_digest)
    - [3.2. stats\_mysql\_query\_digest\_reset](#32-stats_mysql_query_digest_reset)
    - [3.3. stats\_mysql\_query\_rules](#33-stats_mysql_query_rules)

## 1. 概述

当 ProxySQL 收到客户端发送的 SQL 语句后, 会将这个 SQL 语句 (或者重写后的 SQL 语句) 发送给后端的数据库实例, 并将将后端数据库返回的查询结果返回客户端 (如果设置了查询缓存, 则先缓存查询结果)

ProxySQL 可以实现多种方式的路由:

- 基于 IP 地址
- 基于登录用户
- 基于操作的 Schema
- 基于执行的 SQL 语句

其中, 基于 SQL 语句的路由是按照规则进行匹配的, 匹配方式有"哈希匹配" (高效), "正则匹配", 还支持更复杂的链式规则匹配

本例演示了基于"IP", "登录用户" 和 "schema" 的路由, 然后再详细介绍基于 SQL 语句的路由规则

### 1.1. mysql_query_rules 表详解

路由规则存储在 ProxySQL 的 `mysql_query_rules` 中, 每条记录表示一个规则, 以 `rule_id` 为顺序和每个发送到 ProxySQL 的 SQL 语句进行匹配, 如果匹配成功, 则按照规则定义将 SQL 转发到指定的后端数据库, 未匹配到的 SQL 语句会送达到缺省的后端数据库

在 ProxySQL Admin 中, 可以查看 `mysql_query_rules` 表的详情

```sql
SHOW CREATE TABLE `mysql_query_rules`;

+-----------------------+---------+----------+------------+
|       COLUMN          |  TYPE   |  NULL?   | DEFAULT    |
|-----------------------|---------|----------|------------|
| rule_id   (pk)        | INTEGER | NOT NULL |            |
| active                | INT     | NOT NULL | 0          |
| username              | VARCHAR |          |            |
| schemaname            | VARCHAR |          |            |
| flagIN                | INT     | NOT NULL | 0          |
| client_addr           | VARCHAR |          |            |
| proxy_addr            | VARCHAR |          |            |
| proxy_port            | INT     |          |            |
| digest                | VARCHAR |          |            |
| match_digest          | VARCHAR |          |            |
| match_pattern         | VARCHAR |          |            |
| negate_match_pattern  | INT     | NOT NULL | 0          |
| re_modifiers          | VARCHAR |          | 'CASELESS' |
| flagOUT               | INT     |          |            |
| replace_pattern       | VARCHAR |          |            |
| destination_hostgroup | INT     |          | NULL       |
| cache_ttl             | INT     |          |            |
| reconnect             | INT     |          | NULL       |
| timeout               | INT     |          |            |
| retries               | INT     |          |            |
| delay                 | INT     |          |            |
| mirror_flagOU         | INT     |          |            |
| mirror_hostgroup      | INT     |          |            |
| error_msg             | VARCHAR |          |            |
| sticky_conn           | INT     |          |            |
| multiplex             | INT     |          |            |
| log                   | INT     |          |            |
| apply                 | INT     | NOT NULL | 0          |
| comment               | VARCHAR |          |            |
+-----------------------+---------+----------+------------+
```

各个字段的意义如下:

- `rule_id`, 规则的 `id`, 规则是按照 `rule_id` 的顺序进行处理的
- `active`, 只有该字段值为 `1` 的规则才会加载到 RUNTIME
- `username`, 用户名筛选, 当设置为非 `NULL` 值时, 只有匹配的用户建立的连接发出的 SQL 语句才会被匹配
- `schemaname`, Schema 筛选, 当设置为非 `NULL` 值时, 只有当连接使用该字段值作为默认 Schema 时, 该连接发出的查询才会被匹配 (在 MariaDB/Percona/MySQL 中, Schema 等价于数据库名)
- `flagIN`/`flagOUT`, 这些字段允许我们创建"链式规则" (Chains Of Rules), 即串联的多个规则
- `apply`, 当匹配到该规则时, 立即应用该规则
- `client_addr`, 通过源地址进行匹配
- `proxy_addr`, 通过 ProxySQL 设定的代理地址进行匹配, 即 ProxySQL 对外暴露多个"代理地址", 通过连接到不同的代理地址进行匹配
- `proxy_port`, 通过 ProxySQL 监听的多个端口进行匹配
- `digest`, 通过 `digest` 进行匹配, `digest` 的值在 `stats_mysql_query_digest` 表的 `digest` 字段中
- `match_digest`, 通过正则表达式匹配 `digest` 值
- `match_pattern`, 通过正则表达式匹配 SQL 语句内容
- `negate_match_pattern`, 设置为 `1` 时, 表示反向匹配, 即与 `match_digest` 或 `match_pattern` 字段匹配的 SQL 语句被认为"不匹配"
- `re_modifiers`, 正则引擎的修饰符列表, 多个修饰符使用逗号分隔. 例如:
  - `CASELESS` 表示忽略大小写, 为默认值
  - `GLOBAL` 表示进行全局匹配
  - 注意, RE2 正则引擎不支持同时设置多个修饰符, 建议使用 PCRE 引擎, 可以通过 `@@mysql-query_processor_regex` 变量来设置, `PCRE=1`, `RE2=2`, 默认为 PCRE
- `replace_pattern`, 将匹配到的内容替换为此字段中的内容. 它使用的是 `RE2` 正则引擎的 `Replace`, 当该字段为 `NULL` 时, 将不会重写语句, 只会缓存, 路由以及设置其它参数
- `destination_hostgroup`, 将匹配到的查询路由到该主机组, 但注意, 如果用户的 `transaction_persistent=1` (见 `mysql_users` 表), 且该用户建立的连接开启了一个事务, 则这个事务内的所有语句都将路由到同一主机组, 无视匹配规则
- `cache_ttl`, 查询结果缓存的时间 (单位 `ms`)
- `reconnect`, 已作废
- `timeout`, 被匹配或被重写的查询执行的最大超时时长 (单位 `ms`). 如果一个查询执行的时间超过了这个值, 该查询将被强制结束. 如果未设置该值, 将使用全局变量 `mysql-default_query_timeout` 的值
- `retries`, 当在执行查询时探测到故障后, 重新执行查询的最大次数. 如果未指定, 则使用全局变量 `mysql-query_retries_on_failure` 的值
- `delay`, 延迟执行该查询的毫秒数, 本质上是一个"限流机制"和"QoS", 使得可以将优先级让位于其它查询. 这个值会写入到`mysql-default_query_delay` 全局变量中, 所以它会应用于所有的查询. 将来的版本中将会提供一个更高级的限流机
- `mirror_flagOUT`/`mirror_hostgroup`, `mirroring` 相关的设置, 目前 `mirroring` 正处于实验阶段, 先不做解释
- `error_msg`, 查询将被取消, 然后向客户端返回指定的 `error_msg` 信息
- `sticky_conn`, 当前还未实现该功能
- `multiplex`, 如果设置为 `0`, 将禁用 `multiplexing`. 如果设置为 `1`, 则启用或重新启用 `multiplexing`, 除非有其它条件 (如用户变量或事务) 阻止启用. 如果设置为 `2`, 则只对当前查询不禁用 `multiplexing`. 默认值为 `NULL`, 表示不会修改 `multiplexing` 的策略
- `log`, 查询将记录日志
- `apply`, 当设置为 `1` 后, 当匹配到该规则后, 将立即应用该规则, 不会再评估其它的规则 (注意: 应用之后, 将不会评估 `mysql_query_rules_fast_routing` 中的规则)
- `comment`, 注释说明字段, 用于描述规则的意义, 不会对路由产生影响

## 2. 路由规则

### 2.1. 基于端口的路由

首先, 登录 ProxySQL Admin, 修改 ProxySQL 监听 SQL 流量的端口号, 让其监听在不同端口上

```sql
SET mysql-interfaces='0.0.0.0:3306;0.0.0.0:3307';

SAVE MYSQL VARIABLES TO DISK;
```

重启服务 (或容器), 确认端口已经正确监听

```bash
netstat -tnlp | grep proxysql
tcp  0  0 0.0.0.0:6032  0.0.0.0:*   LISTEN  27572/proxysql
tcp  0  0 0.0.0.0:3306  0.0.0.0:*   LISTEN  27572/proxysql
tcp  0  0 0.0.0.0:3307  0.0.0.0:*   LISTEN  27572/proxysql
```

> 如果是 Docker 容器启动, 则可以使用 `docker ps -a` 查看结果

接下来需要修改 `mysql_query_rules` 表, 即 ProxySQL 的路由规则定制表

本例中插入两条规则, 分别监听在 `3306` 端口和 `3307` 端口, 其中:

- 设置 `3306` 端口对应的 `hostgroup_id=100` 为负责写的组;
- 设置 `3307` 端口对应的 `hostgroup_id=101` 是负责读的组;

执行如下设置语句

```sql
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `proxy_port`, `destination_hostgroup`, `apply`)
VALUES (1, 1, 3306, 100, 1),
       (2, 1, 3307, 101, 1);

load mysql query rules to runtime;
save mysql query rules to disk;
```

至此通过"端口"配置的"读写分离"即完成, 通过不同端口 (`3306`/`3307`) 到 ProxySQL 的连接, 会将 SQL 语句发送到不同的数据库后端

除了基于端口进行分离, 还可以基于"监听地址" (通过修改字段 `proxy_addr`), 甚至可以基于客户端地址 (通过修改字段 `client_addr`, 该用法可用于采集数据、数据分析等) 进行读写分离

### 2.2. 基于用户的路由

基于用户的路由配置方式和基于端口的配置是类似的

需要注意, 在插入用户到 `mysql_users` 表中时, 就已经指定了默认的路由目标组, 相当于已经具备一个路由规则了 (默认路由规则). 但当成功匹配到 `mysql_query_rules` 中的规则时, 这个默认规则就不再生效, 所以, 通过默认路由目标也能简单地实现读写分离

例如, 在后端数据库实例上先创建好用于读, 写分离的用户. 例如: `root` 用户用于写操作, `reader` 用户用于读操作

在后端数据库的 Master 节点上创建两个用户 (这里前提是开启主从复制, 所以这两个用户会被同步到从库)

```sql
CREATE USER 'root'@'%' IDENTIFIED WITH mysql_native_password BY 'rootpass';
GRANT ALL ON *.* TO 'root'@'%';

CREATE USER 'reader'@'%' IDENTIFIED WITH mysql_native_password BY 'readerpass';
GRANT SELECT, SHOW DATABASES, SHOW VIEW ON *.* TO 'reader'@'%';
```

然后将这两个用户添加到 ProxySQL 的 `mysql_users` 表中, 并设置每个用户的默认路由规则

```sql
INSERT INTO `mysql_users` (`username`, `password`, `default_hostgroup`)
VALUES ('root', 'rootpass', 101),
       ('reader','readerpass', 100);

LOAD MYSQL USERS TO RUNTIME;
SAVE MYSQL USERS TO DISK;
```

默认情况下, `root` 用户的 SQL 回转发给 `101` 组表示的后端数据库 (Slave), 而 `reader` 用户的 SQL 会发送给 `100` 组表示的后端数据库 (Master), 这显然和这两个用户的最初定义不符

接下来为这两个用户创建规则, 修正默认路由规则

```sql
-- 先清空已有规则 (仅为测试)
DELETE FROM `mysql_query_rules`;

INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `username`, `destination_hostgroup`, `apply`)
VALUES (1, 1, 'root', 100, 1),
       (2, 1, 'reader', 101, 1);

LOAD MYSQL QUERY RULES TO RUNTIME;
SAVE MYSQL QUERY RULES TO DISK;
```

通过规则 `mysql_query_rules` 中基于 `username` 的规则, 覆盖了 `mysql_users` 中设置的默认路由规则

### 2.3. 基于数据库名的路由

ProxySQL 支持基于 Schema 进行路由, 这在一定程度上实现了简单的 Sharding 功能

例如, 可以将后端数据库集群中的节点 A 和节点 B 定义在不同主机组中, ProxySQL 将所有对于 DB1 库的查询路由到节点 A 所在的主机组, 将所有对 DB2 库的查询路由到节点B所在的主机组

这个规则只需配置 `mysql_query_rules` 表的 `schemaname` 即可, 之后 ProxySQL 即可根据不同请求所在的数据库 (`using <db name>` 或 `SELECT <db name>.<table name>`) 进行路由, 将 SQL 转发到不同的后端数据库上

还可以通过 ProxySQL 的 SQL Rewrite 功能, 为不同 `WHERE` 条件的 SQL 增加不同的数据库访问, 从而达到简单的 Sharding 功能, 例如: 对于 `WHERE city = 'xi'an'`, 可通过条件 `'xi'an'` 重写 SQL 为 `WHERE province_shanxi.city = 'xi'an'`, 接着再有之后的规则基于 `province_shanxi` 这个数据库进行转发

### 2.4. 基于 SQL 语句的路由

ProxySQL 接收到前端发送的 SQL 语句后, 首先分析语句, 然后从 `mysql_query_rules` 表中寻找是否有匹配该语句的规则, 如果是被基于 SQL 语句的规则匹配, 则按照规则定义将 SQL 转发给对应的后端组, 如果规则中指定了"替换字段", 则还会对 SQL 语句进行重写后再发送给后端

ProxySQL 支持两种类型的 SQL 语句匹配方式: `match_digest` 和 `match_pattern`. 在解释这两种匹配方式之前, 有必要先解释下 SQL 语句的参数化

#### 2.4.1. SQL语句参数化

对于一条 SQL 语句

```sql
SELECT * FROM `tbl` WHERE `id` = ?;
```

这里将 `WHERE` 条件语句中字段 `id` 的值进行了参数化, 也就是语句中的问号 `?`

客户端发送的 SQL 语句都是完整格式的语句, 但是 SQL 优化引擎出于优化的目的需要考虑很多事情, 例如: 如何缓存查询结果, 如何匹配查询缓存中的数据并取出等

将 SQL 语句参数化是优化引擎其中的一个行为, 对于那些参数相同但参数值不同的查询语句, SQL 语句认为这些是同类查询, 同类查询的 SQL 语句不会重复去编译而增加额外的开销

例如下面的两个语句就是同类 SQL 语句:

```sql
SELECT * FROM `tbl` WHERE `id` = 10;
SELECT * FROM `tbl` WHERE `id` = 20;
```

将这两条语句参数化后结果如下:

```sql
SELECT * FROM `tbl` WHERE `id` = ?;
```

通俗来说, 这里的 `?` 就是一个变量, 任何满足这个语句类型的值都可以传递到这个变量中

所以, 所谓参数化即: 对于那些参数相同, 参数值不同的 SQL 语句, 使用问号 `?` 去替换参数值, 替换后返回的语句就是参数化的结果

当 SQL 语句到达 ProxySQL, 其会将语句进行参数化, 例如下面是 `sysbench` 测试过程中, ProxySQL 统计的参数化语句

```sql
+----+----------+------------+-------------------------------------------------------------+
| hg | sum_time | count_star | digest_text                                                 |
+----+----------+------------+-------------------------------------------------------------+
| 2  | 14520738 | 50041      | SELECT c FROM sbtest1 WHERE id=?                            |
| 1  | 3142041  | 5001       | COMMIT                                                      |
| 1  | 2270931  | 5001       | SELECT c FROM sbtest1 WHERE id BETWEEN ? AND ?+? ORDER BY c |
| 1  | 2021320  | 5003       | SELECT c FROM sbtest1 WHERE id BETWEEN ? AND ?+?            |
| 1  | 1768748  | 5001       | UPDATE sbtest1 SET k=k+? WHERE id=?                         |
| 1  | 1697175  | 5003       | SELECT SUM(K) FROM sbtest1 WHERE id BETWEEN ? AND ?+?       |
| 1  | 1346791  | 5001       | UPDATE sbtest1 SET c=? WHERE id=?                           |
| 1  | 1263259  | 5001       | DELETE FROM sbtest1 WHERE id=?                              |
| 1  | 1191760  | 5001       | INSERT INTO sbtest1 (id, k, c, pad) VALUES (?, ?, ?, ?)     |
| 1  | 875343   | 5005       | BEGIN                                                       |
+----+----------+------------+-------------------------------------------------------------+
```

ProxySQL 的 `mysql_query_rules` 表中有三个字段, 能基于参数化后的 SQL 语句进行三种不同方式的匹配:

- `digest`, 将参数化后的语句进行散列运算得到一个散列值值 (即 `digest` 值), 可以对这个散列值进行精确匹配, 匹配效率最高
- `match_digest`, 对 `digest` 值进行正则匹配
- `match_pattern`, 对原始 SQL 语句的文本内容进行正则匹配

注意: 如果要进行 SQL 语句重写 (即正则替换), 或者对参数值匹配, 则必须采用 `match_pattern`, 如果可以, 尽量采用 `digest` 匹配方式, 因为它的效率更高

#### 2.4.2. 基于参数化 SQL 散列值匹配的路由

散列匹配规则是对 SQL 语句的散列值 (`digest`) 进行精确匹配

通过 ProxySQL Admin 从 `stats_mysql_query_digest` 中获取对应语句的 `digest` 值

```sql
-- 重置 stats_mysql_query_digest 记录
SELECT * FROM `stats_mysql_query_digest_reset` WHERE 1=0;

-- 另外登录 ProxySQL 代理, 执行相关的 SQL 语句

-- 查询所执行 SQL 语句的散列情况
SELECT `hostgroup`, `count_star`, `sum_time`, `digest`, `digest_text`
FROM `stats_mysql_query_digest`;
+-----------+------------+----------+--------------------+------------------------------------------+
| hostgroup | count_star | sum_time | digest             | digest_text                              |
+-----------+------------+----------+--------------------+------------------------------------------+
| 100       | 3          | 21259    | 0x69D660B55393BF50 | INSERT INTO `test` (`value`) VALUES (?)  |
| 101       | 1          | 992      | 0x1B172DC2405102C4 | SELECT `value` FROM `test`               |
| ...                                                                                               |
+-----------+------------+----------+--------------------+------------------------------------------+
```

可以看到, 当前情况下, 散列值为 `0x69D660B55393BF50` 的语句发送到编号为 `100` 主机组 (即 Master), 而散列值为 `0x1B172DC2405102C4` 的语句发送到编号为 `101` 主机组 (Slave)

基于查询出的 `digest` 值, 插入两条匹配规则

```sql
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `digest`, `destination_hostgroup`, `apply`)
VALUES (1, 1, "0x69D660B55393BF50", 101, 1),
       (2, 1, "0x1B172DC2405102C4", 100, 1);
```

这样就将之前这两条语句的路由情况对调了

#### 2.4.3. 基于参数化 SQL 正则匹配的路由

规则字段 `match_digest` 的字面意思是对"散列值进行匹配", 但注意这个字段的实际含意是对"参数化 SQL"进行匹配, 匹配方式是通过正则表达式

设置基于 `match_digest` 的匹配规则

```sql
-- 先清空已有规则 (仅为测试)
DELETE FROM `mysql_query_rules`;

-- 重置 stats_mysql_query_digest 记录
SELECT * FROM `stats_mysql_query_digest_reset` WHERE 1=0;

-- 另外登录 ProxySQL 代理, 执行相关的 SQL 语句

-- 查询已执行的参数化 SQL
SELECT `hostgroup`, `count_star`, `sum_time`, `digest`, `digest_text`
FROM `stats_mysql_query_digest`;
+-----------+------------+----------+--------------------+-----------------------------------------+
| hostgroup | count_star | sum_time | digest             | digest_text                             |
+-----------+------------+----------+--------------------+-----------------------------------------+
| 100       | 1          | 856      | 0x69D660B55393BF50 | INSERT INTO `test` (`value`) VALUES (?) |
| 101       | 1          | 537      | 0x1B172DC2405102C4 | SELECT `value` FROM `test`              |
+-----------+------------+----------+--------------------+-----------------------------------------+
```

可以看到 `digest_text` 字段即为"参数化"的 SQL 语句, 基于其设计正则表达式并插入为规则:

```sql
-- 插入基于 match_digest 的规则
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `match_digest`, `destination_hostgroup`, `apply`)
VALUES (1, 1, "^INSERT INTO `test`.*", 100, 1),
       (2, 1, "^SELECT .*? FROM `test` .*", 101, 1);

-- 保存规则并令规则生效
LOAD MYSQL QUERY RULES TO RUNTIME;
SAVE MYSQL QUERY RULES TO DISK;
```

至此, 基于参数化 SQL 的匹配规则即可生效, 登录 ProxySQL 代理, 执行 SQL 语句即可验证

如果想对 `match_digest` 的匹配逻辑取反, 即不被正则匹配的 SQL 语句才命中规则, 则设置 `mysql_query_rules` 表中的字段 `negate_match_pattern=1` 即可

#### 2.4.4. 基于原始 SQL 正则匹配的路由

规则字段 `match_pattern` 的匹配方式和 `match_digest` 的类似, 但 `match_pattern` 的匹配是基于原始 SQL 的, 所以可以在匹配中包括查询参数, 除此之外, 还有两种情况必须使用 `match_pattern`:

- SQL Rewrite, 即同时设置了 `replace_pattern` 字段, 对 SQL 进行重写
- 需要对参数的值进行匹配

如果想对 `match_pattern` 的匹配逻辑取反, 即不被正则匹配的 SQL 语句才命中规则, 则设置 `mysql_query_rules` 表中的字段 `negate_match_pattern=1` 即可

具体设置请参考[创建读写分离规则](./proxysql.md#25-创建读写分离规则)章节

#### 2.4.5. 实用的读写分离

一个极简却实用的读写分离配置方式: 将默认路由组设置为写组, 然后再插入下面两个 `SELECT` 语句的规则

```sql
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `match_digest`, `destination_hostgroup`, `apply`)
VALUES (1, 1, '^SELECT .* FOR UPDATE$', 100, 1),
       (2, 1, '^SELECT ', 101, 1);
```

但需要注意的是, 这样的规则只适用于小环境下的读写分离, 对于稍复杂的环境, 需要对不同语句进行开销分析, 对于开销大的语句需要制定专门的路由规则

## 3. 路由相关的统计表

在 ProxySQL 的 stats 库中, 包含了几个统计表

```sql
SHOW TABLES FROM `stats`;
+--------------------------------------+
| tables                               |
+--------------------------------------+
| global_variables                     |
| stats_memory_metrics                 |
| stats_mysql_commands_counters        |     -- 已执行查询语句的统计信息
| stats_mysql_connection_pool          |     -- 连接池信息
| stats_mysql_connection_pool_reset    |     -- 重置连接池统计数据
| stats_mysql_global                   |     -- 全局统计数据
| stats_mysql_prepared_statements_info |
| stats_mysql_processlist              |     -- 模拟show processlist 的结果
| stats_mysql_query_digest             |     -- 本文解释
| stats_mysql_query_digest_reset       |     -- 本文解释
| stats_mysql_query_rules              |     -- 本文解释
| stats_mysql_users                    |     -- 各 mysql user 前端和 ProxySQL 的连接数
| stats_proxysql_servers_checksums     |     -- ProxySQL 集群相关
| stats_proxysql_servers_metrics       |     -- ProxySQL 集群相关
| stats_proxysql_servers_status        |     -- ProxySQL 集群相关
+--------------------------------------+
```

### 3.1. stats_mysql_query_digest

该表对于分析 SQL 语句至关重要, 是分析语句性能, 定制路由规则指标的最主要来源. 其中记录了每个参数化分类后的 SQL 语句对应的统计数据, 包括该类语句的执行次数, 所花总时间, 所花最短和最长时间, 还包括语句的文本以及它的散列值 (`digest`)

以下是该表各个字段的意义:

- `hostgroup`, 查询将要路由到的目标主机组. 如果值为 `-1`, 则表示命中了查询缓存, 直接从缓存取数据返回给客户端
- `schemaname`, 当前正在执行的查询所在的 Schema 名称 (即数据库名称)
- `username`, MySQL 客户端连接到 ProxySQL 使用的用户名
- `digest`, 一个十六进制的散列值, 代表一条参数化 SQL 的唯一标示值
- `digest_text`, 参数化 SQL 语句. 注意, 如果重写了 SQL 语句, 则这个字段是显示的是**重写后的 SQL 语句**, 换句话说, 这个字段是真正路由到后端, 被后端节点执行的语句
- `count_star`, 该参数化 SQL 语句总共被执行的次数
- `first_seen`, UNIX 格式时间戳, 表示该参数化 SQL 语句首次被 ProxySQL 路由的时间
- `last_seen`, UNIX 格式时间戳, 表示该参数化 SQL 语句最后一次被 ProxySQL 路由的时间
- `sum_time`, 该参数化 SQL 语句总共执行花费的时间, 单位为 `ms` (包括该参数化 SQL 所有参数值的 SQL 语句)
- `min_time`/`max_time`, 执行该参数化 SQL 语句的时间范围, 单位为 `ms`
  - `min_time` 表示的是目前为止执行该类查询所花的最短时间
  - `max_time` 表示的是目前为止执行该类查询所花的最长时间

注意, 该表中的查询所花时长是指 ProxySQL 从接收到客户端查询开始, 到 ProxySQL 准备向客户端发送查询结果的时长. 因此, 这些时间更像是客户端看到的发起和接收的时间间隔 (尽管客户端到服务端数据传输也需要时间). 更精确一点, 在执行查询之前, ProxySQL 可能需要更改字符集或模式, 可能当前后端不可用 (当前后端执行语句失败) 而找一个新的后端, 可能因为所有连接都繁忙而需要等待空闲连接, 这些都不应该计算到查询执行所花时间内

其中 `hostgroup`, `digest`, `digest_text`, `count_start`, `{sum, min, max}_time` 这几列最常用, 例如:

```sql
SELECT `hostgroup`, `count_star`, `sum_time`, `min_time`, `max_time`, `digest`, `digest_text`
FROM `stats_mysql_query_digest`;

+-----------+------------+----------+----------+----------+--------------------+-----------------------------------------+
| hostgroup | count_star | sum_time | min_time | max_time | digest             | digest_text                             |
+-----------+------------+----------+----------+----------+--------------------+-----------------------------------------+
| 100       | 1          | 1067     | 1067     | 1067     | 0xC88D8508558CFABC | delete from `test`                      |
| 101       | 1          | 537      | 537      | 537      | 0x1B172DC2405102C4 | SELECT `value` FROM `test`              |
| 100       | 1          | 856      | 856      | 856      | 0x69D660B55393BF50 | INSERT INTO `test` (`value`) VALUES (?) |
+-----------+------------+----------+----------+----------+--------------------+-----------------------------------------+
```

### 3.2. stats_mysql_query_digest_reset

这个表的表结构和 `stats_mysql_query_digest` 是完全一样的, 但一旦对该表进行查询 (任何查询, 无论有无结果), 则 `stats_mysql_query_digest` 表会被重置

### 3.3. stats_mysql_query_rules

这个表有两个字段:

- `rule_id`, 对应 `mysql_query_rules` 表的 `rule_id` 字段, 即匹配规则编号
- `hits`, 该规则被命中的次数
