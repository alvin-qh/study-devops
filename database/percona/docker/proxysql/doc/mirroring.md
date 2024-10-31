# 镜像

- [镜像](#镜像)
  - [1. 概述](#1-概述)
  - [2. 范例](#2-范例)
    - [2.1. 镜像到同一个数据库组](#21-镜像到同一个数据库组)
    - [2.2. 镜像到不同数据库组](#22-镜像到不同数据库组)
    - [2.3. 同时重写原始查询和镜像查询](#23-同时重写原始查询和镜像查询)
    - [2.4. 只重写镜像查询](#24-只重写镜像查询)
  - [3. 进阶范例](#3-进阶范例)
    - [3.1. 通过镜像测试 SQL Rewrite](#31-通过镜像测试-sql-rewrite)
    - [3.2. 通过镜像查询和防火墙测试 SQL Rewrite](#32-通过镜像查询和防火墙测试-sql-rewrite)

## 1. 概述

所谓查询镜像, 指的是将查询发送到两个数据库后端, 取最早返回的结果, 这种方式可以避免某个后端数据库因压力过大导致的延迟, 和网站镜像具有类似的目的

当查询匹配到的规则 (`mysql_query_rules`) 记录中设置了 `mirror_flagOUT` 或 `mirror_hostgroup` 字段, 则实时查询镜像将被自动启用

注意: 如果 SQL Rewrite 被启用, 则会为最终重写的 SQL 查询启用镜像, 且镜像查询可以被后续的规则再次重写

如果原始查询与多个规则匹配, 则可能 `mirror_flagOUT` 或 `mirror_hostgroup` 会被多次更改, 整个镜像的逻辑如下:

- 对于一个原始查询, 如果在执行过程中, 规则中设置了 `mirror_flagOUT` 或 `mirror_hostgroup`字段, 则一个新的 MySQL 会话将被创建;
- 新的 MySQL 会话会继承原始 MySQL 会话的所有设置的属性, 包括相同的"凭证", "模式", "数据库组" 等 (注意, 字符集并未继承);
- 如果原始会话中设置了 `mirror_hostgroup` 字段, 则新会话会将自身的数据库组设置为 `mirror_hostgroup` 字段指定的组;
- 如果 `mirror_flagOUT` 字段未设置, 则新会话会在 `mirror_hostgroup` 指定的数据库组上执行查询;
- 如果 `mirror_flagOUT` 字段已设置, 则新会话会尝试取匹配 `mysql_query_rules` 的另一条规则, 该规则的 `FlagIN` 字段等于当前规则的 `mirror_flagOUT` 值, 所以这有可能会导致查询被修改 (新规则会执行 SQL Rewrite 或者指定新的数据库组);

## 2. 范例

### 2.1. 镜像到同一个数据库组

本例中会将 `SELECT` 查询 (包括原始查询和镜像查询) 均路由到 `hostgroup2` 数据库组

插入规则

```sql
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `match_pattern`, `destination_hostgroup`,`mirror_hostgroup`, `apply`)
VALUES (5, 1, '^SELECT', 2, 2, 1);
```

该规则表示: 将 `SELECT` 开头的查询进行路由, 且目标服务器组和镜像服务器组的编号都为 `2`

令规则生效

```sql
LOAD MYSQL QUERY RULES TO RUNTIME;
```

执行测试查询

```sql
SELECT `id` FROM `sbtest1` LIMIT 3;

+------+
| id   |
+------+
| 6204 |
| 3999 |
| 6650 |
+------+
```

登录 ProxySQL Admin, 查看 `SELECT` 查询的路由情况

```sql
SELECT `hostgroup`, `count_star`, `schemaname`, `digest_text`
FROM `stats_mysql_query_digest`
ORDER BY `digest`;

+-----------+------------+--------------------+------------------------------------+
| hostgroup | count_star | schemaname         | digest_text                        |
+-----------+------------+--------------------+------------------------------------+
| 2         | 2          | sbtest             | SELECT `id` FROM `sbtest1` LIMIT ? |
| 1         | 1          | information_schema | select @@version_comment limit ?   |
| 2         | 2          | information_schema | SELECT DATABASE()                  |
+-----------+------------+--------------------+------------------------------------+
```

可以看到查询语句的查询次数为 `2` (`count_star`), 且均路由到数据库组 `2`

继续执行一次相同的测试查询, 再次查看路由情况

```sql
SELECT `hostgroup`, `count_star`, `schemaname`, `digest_text`
FROM `stats_mysql_query_digest`
ORDER BY `digest`;

+-----------+------------+--------------------+------------------------------------+
| hostgroup | count_star | schemaname         | digest_text                        |
+-----------+------------+--------------------+------------------------------------+
| 2         | 4          | sbtest             | SELECT `id` FROM `sbtest1` LIMIT ? |
| 1         | 1          | information_schema | select @@version_comment limit ?   |
| 2         | 2          | information_schema | SELECT DATABASE()                  |
+-----------+------------+--------------------+------------------------------------+
```

可以得出结论, 每执行一次相关查询, ProxySQL 会将其路由两次, 且指向同一个数据库组 (但会是组内的两个数据库实例), 表示查询确实被镜像了

另外, ProxySQL 会对两次查询 (原始和镜像) 都记录指标, 事后可以分析不同数据库实例的能力差异

### 2.2. 镜像到不同数据库组

还是通过上一个例子, 但这次将 `mirror_hostgroup` 字段指向和 `destination_hostgroup` 字段不同的数据库组

插入规则

```sql
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `match_pattern`, `destination_hostgroup`,`mirror_hostgroup`, `apply`)
VALUES (5, 1, '^SELECT', 1, 2, 1);
```

该规则表示: 将 `SELECT` 开头的查询进行路由, 且原始查询路由到数据库组 `1`, 镜像查询路由到组 `2`

令规则生效

```sql
LOAD MYSQL QUERY RULES TO RUNTIME;
```

执行测试查询

```sql
SELECT `id` FROM `sbtest1` LIMIT 3;

+------+
| id   |
+------+
| 6204 |
| 3999 |
| 6650 |
+------+
```

登录 ProxySQL Admin, 查看 `SELECT` 查询的路由情况

```sql
SELECT `hostgroup`, `count_star`, `schemaname`, `digest_text`
FROM `stats_mysql_query_digest`
ORDER BY `digest`;

+-----------+------------+--------------------+------------------------------------+
| hostgroup | count_star | schemaname         | digest_text                        |
+-----------+------------+--------------------+------------------------------------+
| 1         | 1          | sbtest             | SELECT `id` FROM `sbtest1` LIMIT ? |
| 2         | 1          | sbtest             | SELECT `id` FROM `sbtest1` LIMIT ? |
+-----------+------------+--------------------+------------------------------------+
```

可以看到, 同一个查询被同时路由到数据库组 `1` 和 `2` 上, 且原始查询和镜像查询的指标是根据不同的数据库组进行分开统计的

### 2.3. 同时重写原始查询和镜像查询

本例中, 原始查询将被重写 (SQL Rewrite), 然后进行镜像

为了简单起见, 假设有 `sbtest0`~`sbtest9` 共 10 个表, 不管查询那个表, 最终都只查询 `sbtest3` 这个表, 按该规则插入记录

```sql
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `match_pattern`, `destination_hostgroup`, `replace_pattern`, `mirror_hostgroup`, `apply`)
VALUES (5, 1, 'sbtest[0-9]+', 1, 'sbtest3', 2, 1);
```

规则中定义, 原始数据库组为 `1`, 镜像数据库组为 `2`, 执行测试查询如下:

```sql
SELECT `id` FROM `sbtest1` LIMIT 3;

+-------+
| id    |
+-------+
| 24878 |
|  8995 |
| 33622 |
+-------+
```

查看此次查询的路由情况

```sql
SELECT `hostgroup`, `count_star`, `sum_time`, `digest_text`
FROM `stats_mysql_query_digest`
ORDER BY `digest`;

+-----------+------------+----------+------------------------------------+
| hostgroup | count_star | sum_time | digest_text                        |
+-----------+------------+----------+------------------------------------+
| 2         | 1          | 30018    | SELECT `id` FROM `sbtest3` LIMIT ? |
| 1         | 1          | 27227    | SELECT `id` FROM `sbtest3` LIMIT ? |
+-----------+------------+----------+------------------------------------+
```

可以看到, 原始查询被重写 (从查询 `sbtest1` 重写为查询 `sbtest3`), 且同时发往了原始数据库组和镜像数据库组

### 2.4. 只重写镜像查询

可以只重写镜像查询, 不改变原始查询, 这样做的目的可以用来比较原始查询和镜像查询的性能差异 (比如镜像查询使用了新的索引)

下面就以比较使用索引和不使用索引的性能差异来举例, 规则如下:

- 规则匹配 `FROM sbtest1` 语句;
- 原始数据库组为 `2` (`hostgroup=2`);
- 设置规则的 `mirror_flagOUT=100`;
- 不设置 `mirror_hostgroup` 字段值;

第一条规则如下

```sql
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `match_pattern`, `destination_hostgroup`,`mirror_flagOUT`, `apply`)
VALUES (5, 1, 'FROM sbtest1 ', 2, 100, 1);
```

由于该规则中存在 `mirror_flagOUT=100`, 且未设置 `mirror_hostgroup`, 所以 ProxySQL 会发出原始查询, 并将镜像查询通过 `flagIN=100` 的下一条规则进行继续处理

下一条规则具备 SQL Rewrite 设置, 由此就达成了只对镜像查询进行修改的目标

插入第二条规则

```sql
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `match_pattern`, `replace_pattern`, `destination_hostgroup`, `apply`)
VALUES (10, 1, 'FROM sbtest1', 'FROM sbtest1 IGNORE INDEX(k_1)', 2, 1);
```

第二条规则中, 对查询进行了重写, 去掉了对 `k_1` 索引的使用, 并且也路由到编号为 `2` 的数据库组, 所以数据库组 `2` 会同时处理原始查询和重写后的镜像查询

令规则生效

```sql
LOAD MYSQL QUERY RULES TO RUNTIME;
```

执行测试查询

```sql
SELECT `id` FROM `sbtest1` ORDER BY `k` DESC LIMIT 3;
+-------+
| id    |
+-------+
| 26372 |
| 81250 |
| 60085 |
+-------+

SELECT `id`, `k` FROM `sbtest1` ORDER BY `k` DESC LIMIT 3;
+-------+-------+
| id    | k     |
+-------+-------+
| 26372 | 80626 |
| 81250 | 79947 |
| 60085 | 79142 |
+-------+-------+
```

查看路由情况, 可以得到两条不同语句的性能差异

```sql
SELECT `hostgroup`, `count_star`, `sum_time`, `digest_text`
FROM `stats_mysql_query_digest`
ORDER BY `sum_time` DESC;

+-----------+------------+----------+---------------------------------------------------------------------------+
| hostgroup | count_star | sum_time | digest_text                                                               |
+-----------+------------+----------+---------------------------------------------------------------------------+
| 2         | 1          | 1135673  | SELECT `id`, `k` FROM `sbtest1` IGNORE INDEX(k_1) ORDER BY k DESC LIMIT ? |
| 2         | 1          | 683906   | SELECT `id` FROM `sbtest1` IGNORE INDEX(k_1) ORDER BY k DESC LIMIT ?      |
| 2         | 1          | 7478     | SELECT `id`, `k` FROM `sbtest1` ORDER BY k DESC LIMIT ?                   |
| 2         | 1          | 4422     | SELECT `id` FROM `sbtest1` ORDER BY k DESC LIMIT ?                        |
+-----------+------------+----------+---------------------------------------------------------------------------+
```

通过查询 `stats_mysql_query_digest` 表, 这个例子可以得出如下结论:

- 查询被镜像执行;
- 原始查询未被重写;
- 镜像查询被重写;
- 镜像查询因为未使用索引, 所以相比原始查询慢了很多;

## 3. 进阶范例

### 3.1. 通过镜像测试 SQL Rewrite

在该范例中, 通过一个规则匹配所有的 `SELECT` 语句, 对其进行镜像, 并尝试重写这些语句

插入规则如下:

```sql
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `match_pattern`, `destination_hostgroup`, `mirror_flagOUT`, `apply`)
VALUES (5, 1, '^SELECT ', 2, 100, 1);

INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `flagIN`, `match_pattern`, `destination_hostgroup`, `replace_pattern`, `apply`)
VALUES (10, 1, 100, '^SELECT DISTINCT `c` FROM `sbtest([0-9]{1,2})` WHERE `id` BETWEEN ([0-9]+) AND ([0-9]+)\+([0-9]+) ORDER BY `c`$', 2, 'SELECT DISTINCT `c` FROM `sbtest\1` WHERE `id` = \3 \+ \4 ORDER BY `c`', 1);

LOAD MYSQL QUERY RULES TO RUNTIME;
```

第一条规则指定了将匹配所有 `SEELCT` 语句, 将原始查询路由到编号为 `2` 的数据库组, 且将镜像查询的规则匹配到 `flagIN=100` 的下一条规则上

第二条规则将尝试将类似

```sql
SELECT DISTINCT `c` FROM `sbtest1` WHERE `id` BETWEEN 10 AND 10+2 ORDER BY `c`;
```

重写为

```sql
SELECT DISTINCT `c` FROM `sbtest1` WHERE `id` = 10 \+ 2 ORDER BY `c`;
```

显然这次重写是错误的, 无法正确执行, 可以确认如下:

```sql
SELECT DISTINCT `c` FROM `sbtest1` WHERE `id` BETWEEN 10 AND 10+2 ORDER BY `c`;

+-------------------------------------------------------------------------------------------------------------------------+
| c                                                                                                                       |
+-------------------------------------------------------------------------------------------------------------------------+
| 41189576069-45553989496-19463022727-28789271530-61175755423-36502565636-61804003878-85903592313-68207739135-17129930980 |
| 48090103407-09222928184-34050945574-85418069333-36966673537-23363106719-15284068881-04674238815-26203696337-24037044694 |
| 74234360637-48574588774-94392661281-55267159983-87261567077-93953988073-73238443191-61462412385-80374300764-69242108888 |
+-------------------------------------------------------------------------------------------------------------------------+
```

可以返回结果表示原始查询和镜像查询至少有一个执行成功, 进一步查看 `stats_mysql_query_digest` 表中记录的日志

```sql
SELECT `hostgroup`, `count_star`, `sum_time`, `digest_text`
FROM `stats_mysql_query_digest`
ORDER BY `digest_text`;

+-----------+------------+----------+----------------------------------------------------------------------+
| hostgroup | count_star | sum_time | digest_text                                                          |
+-----------+------------+----------+----------------------------------------------------------------------+
| 2         | 2          | 25461    | SELECT DISTINCT c FROM sbtest1 WHERE id BETWEEN ? AND ?+? ORDER BY c |
+-----------+------------+----------+----------------------------------------------------------------------+
```

可以看到, 原始查询被执行了两次, 而 SQL Rewrite 没有被记录, 这说明整个过程中有错误发生, 通过进一步查询 `stats_mysql_query_rules` 表, 看规则是否被匹配到

```sql
SELECT * FROM `stats_mysql_query_rules`;

+---------+------+
| rule_id | hits |
+---------+------+
| 5       | 1    |
| 10      | 1    |
+---------+------+
```

表示原始查询对应的规则以及该规则 `mirror_flagOUT` 字段指向镜像规则都被匹配到了, 说明 `mysql_query_rules` 表中的规则没有配置错误, 那么错误就大概率出现在 SQL Rewrite 过程

查询 ProxySQL 的错误日志, 可以看到如下一条错误:

```log
re2/re2.cc:881: invalid rewrite pattern: SELECT DISTINCT c FROM sbtest\1 WHERE id = \3 \+ \4 ORDER BY c
```

说明重写 SQL 的 `replace_pattern` 字段出现了语法错误, 将其改正如下:

```sql
UPDATE `mysql_query_rules`
SET `replace_pattern` = 'SELECT DISTINCT `c` FROM `sbtest\1` WHERE `id` = \3 + \4 ORDER BY `c`'
WHERE `rule_id` = 10;

LOAD MYSQL QUERY RULES TO RUNTIME;
```

重新执行测试 SQL

```sql
SELECT DISTINCT `c` FROM `sbtest1` WHERE `id` BETWEEN 10 AND 10 + 2 ORDER BY `c`;

+-------------------------------------------------------------------------------------------------------------------------+
| c                                                                                                                       |
+-------------------------------------------------------------------------------------------------------------------------+
| 41189576069-45553989496-19463022727-28789271530-61175755423-36502565636-61804003878-85903592313-68207739135-17129930980 |
| 48090103407-09222928184-34050945574-85418069333-36966673537-23363106719-15284068881-04674238815-26203696337-24037044694 |
| 74234360637-48574588774-94392661281-55267159983-87261567077-93953988073-73238443191-61462412385-80374300764-69242108888 |
+-------------------------------------------------------------------------------------------------------------------------+
```

再次查询 `stats_mysql_query_digest` 表检验规则匹配情况

```sql
SELECT `hostgroup`, `count_star`, `sum_time`, `digest_text`
FROM `stats_mysql_query_digest`
ORDER BY `digest_text`;

+-----------+------------+----------+------------------------------------------------------------------------------+
| hostgroup | count_star | sum_time | digest_text                                                                  |
+-----------+------------+----------+------------------------------------------------------------------------------+
| 2         | 1          | 2756     | SELECT DISTINCT `c` FROM `sbtest1` WHERE `id` = ? + ? ORDER BY c             |
| 2         | 1          | 891      | SELECT DISTINCT `c` FROM `sbtest1` WHERE `id` BETWEEN ? AND ?+? ORDER BY `c` |
+-----------+------------+----------+------------------------------------------------------------------------------+
```

两条规则均被执行, 说明这次的设置是正确的

### 3.2. 通过镜像查询和防火墙测试 SQL Rewrite

再前一个演示的基础上, 尝试仅执行重写后的查询, 而不执行原始查询

通过在镜像查询设置 `error_msg` 字段, 如此以来, ProxySQL 会处理相关的查询, 但不会将其路由到后端的 MySQL 实例上, 所以也说明了镜像查询是可以被修改的, 范例如下:

基于前一个范例修改规则

```sql
UPDATE `mysql_query_rules`
SET `error_msg` = "random error, blah blah"
WHERE `rule_id` = 10;

LOAD MYSQL QUERY RULES TO RUNTIME;
```

执行测试查询

```sql
SELECT DISTINCT `c`
FROM `sbtest1`
WHERE `id` BETWEEN 10 AND 10+2
ORDER BY `c`;

+-------------------------------------------------------------------------------------------------------------------------+
| c                                                                                                                       |
+-------------------------------------------------------------------------------------------------------------------------+
| 41189576069-45553989496-19463022727-28789271530-61175755423-36502565636-61804003878-85903592313-68207739135-17129930980 |
| 48090103407-09222928184-34050945574-85418069333-36966673537-23363106719-15284068881-04674238815-26203696337-24037044694 |
| 74234360637-48574588774-94392661281-55267159983-87261567077-93953988073-73238443191-61462412385-80374300764-69242108888 |
+-------------------------------------------------------------------------------------------------------------------------+
```

再次检查规则执行的统计情况

```sql
SELECT `hostgroup`, `count_star`, `sum_time`, `digest_text`
FROM `stats_mysql_query_digest`
ORDER BY `digest_text`;

+-----------+------------+----------+------------------------------------------------------------------------------+
| hostgroup | count_star | sum_time | digest_text                                                                  |
+-----------+------------+----------+------------------------------------------------------------------------------+
| -1        | 1          | 0        | SELECT DISTINCT `c` FROM `sbtest1` WHERE `id` = ? + ? ORDER BY c             |
| 2         | 1          | 3219     | SELECT DISTINCT `c` FROM `sbtest1` WHERE `id` BETWEEN ? AND ?+? ORDER BY `c` |
+-----------+------------+----------+------------------------------------------------------------------------------+
```

以及查看规则命中情况

```sql
SELECT * FROM `stats_mysql_query_rules`;

+---------+------+
| rule_id | hits |
+---------+------+
| 5       | 1    |
| 10      | 1    |
+---------+------+
```

可以看到两条规则都被执行, 但重写前的规则因为设置了 `error_msg` 而未实际路由
