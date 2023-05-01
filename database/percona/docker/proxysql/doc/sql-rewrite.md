# SQL Rewrite

ProxySQL 提供了两种机制来匹配查询 SQL 语句, 即:

- `match_digest`, 通过正则表达式匹配参数化 SQL 查询语句 (例如: `SELECT c FROM sbtest1 WHERE id = ?`), 该查询语句可以通过 `stats_mysql_query_digest` 表的 `query_digest` 字段查找到
- `match_pattern`, 通过正则表达式匹配原始 SQL 查询语句 (例如: `SELECT c FROM sbtest1 WHERE id = 2`)

通过摘要值 (即 `stats_mysql_query_digest` 表的 `digest` 字段) 匹配查询的成本总是小于通过查询 SQL 本身的匹配; 越短小的 SQL 语句通过正则匹配的速度越快，所以:

- 如果要匹配 SQL 语句, 建议使用 `digest` 或 `match_digest` 规则;
- 而要重写 SQL 语句或对 SQL 参数值进行匹配，建议使用 `match_pattern` 规则

演示 SQL Rewrite 规则如下:

```sql
INSERT INTO `mysql_query_rules` (`rule_id`, `active`, `match_pattern`, `replace_pattern`, `apply`)
VALUES (10, 1, '^SELECT\s+COUNT\(\*\)(.*)$', 'SELECT COUNT(1)\1', 1);

SELECT `rule_id`, `match_digest`, `match_pattern`, `replace_pattern`, `cache_ttl`, `apply`
FROM `mysql_query_rules`
ORDER BY `rule_id`;

+---------+------------------------+----------------------------+--------------------+-----------+-------+
| rule_id | match_digest           | match_pattern              | replace_pattern    | cache_ttl | apply |
+---------+------------------------+----------------------------+--------------------+-----------+-------+
| 10      | NULL                   | ^SELECT\s+COUNT\(\*\)(.*)$ | SELECT COUNT(1)\1  | NULL      | 1     |
| 20      | ^SELECT .* FOR UPDATE$ | NULL                       | NULL               | NULL      | 1     |
| 30      | ^SELECT                | NULL                       | NULL               | NULL      | 1     |
+---------+------------------------+----------------------------+--------------------+-----------+-------+

-- 将匹配规则加载到 RUNTIME
LOAD MYSQL QUERY RULES TO RUNTIME;
```

设置的规则用于将所有 `SELECT COUNT(*) ...` 语句重写为 `SELECT COUNT(1) ...` 语句

登录 ProxySQL 代理端 (在宿主机运行)

```bash
mysql -uproxysql -p -h'127.0.0.1'
```

执行如下 SQL 语句若干次, 类似如下:

```sql
SELECT COUNT(*) FROM `test` WHERE `value` LIKE '%test%';

+----------+
| COUNT(1) |
+----------+
|        0 |
+----------+
```

可以看到, 结果的字段已经变为了 `COUNT(1)`, 说明语句已经置换成功

可以通过从库的 general_log 日志查看后端数据库实际执行的 SQL 语句, 本例为 `cluster-ms/log/slave_mysqld.log` 文件 (注意, 路由规则是到从库)

查看规则命中情况

```sql
SELECT `s`.`hits`, `r`.`rule_id`, `r`.`match_digest`, `r`.`match_pattern`, `r`.`replace_pattern`, `r`.`cache_ttl`, `r`.`apply`
FROM `mysql_query_rules` `r` LEFT JOIN `stats_mysql_query_rules` `s`
ORDER BY `r`.`rule_id`;

+------+---------+--------------+----------------------------+--------------------+-----------+-------+
| hits | rule_id | match_digest | match_pattern              | replace_pattern    | cache_ttl | apply |
+------+---------+--------------+----------------------------+--------------------+-----------+-------+
| 8    | 10      | NULL         | ^SELECT\s+COUNT\(\*\)(.*)$ | SELECT COUNT(1) \1 | NULL      | 1     |
+------+---------+--------------+----------------------------+--------------------+-----------+-------+
```

可以看到 `rule_id` 为 `10` 的规则命中了 `8` 次, 成功完成置换
