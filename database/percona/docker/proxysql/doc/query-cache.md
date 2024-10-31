# 查询缓冲

查询缓冲也是 ProxySQL 的一个常用用法

默认情况下, ProxySQL 未开启查询缓冲, 可以通过在规则表 (`mysql_query_rules`) 中设置 `cache_ttl` (单位为 `ms`)来开启查询缓冲

例如将指定规则对应的查询缓冲 `5s` 时间, 设置如下:

```sql
-- 为查询规则启用缓冲功能
UPDATE `mysql_query_rules`
SET `cache_ttl` = 5000
WHERE `active` = 1 AND `destination_hostgroup` = 2;

-- 将修改的配置加载到 RUNTIME
LOAD MYSQL QUERY RULES TO RUNTIME;
```

通过 ProxySQL 代理端执行若干条 SQL 语句后, 查看日志记录如下:

```sql
-- 重置查询日志
SELECT 1 FROM `stats_mysql_query_digest_reset` LIMIT 1;

SELECT `hostgroup` `hg`, `sum_time`, `count_star`, `digest_text`
FROM `stats_mysql_query_digest`
ORDER BY `sum_time` DESC;

+----+----------+------------+----------------------------------------------------------------------+
| hg | sum_time | count_star | digest_text                                                          |
+----+----------+------------+----------------------------------------------------------------------+
| 1  | 7457441  | 5963       | COMMIT                                                               |
| 1  | 6767681  | 5963       | SELECT c FROM sbtest1 WHERE id BETWEEN ? AND ?+? ORDER BY c          |
| 2  | 4891464  | 8369       | SELECT c FROM sbtest1 WHERE id=?                                     |
| 1  | 4573513  | 5963       | UPDATE sbtest1 SET k=k+? WHERE id=?                                  |
| 1  | 4531319  | 5963       | SELECT c FROM sbtest1 WHERE id BETWEEN ? AND ?+?                     |
| 1  | 3993283  | 5963       | SELECT SUM(K) FROM sbtest1 WHERE id BETWEEN ? AND ?+?                |
| 1  | 3482242  | 5963       | UPDATE sbtest1 SET c=? WHERE id=?                                    |
| 1  | 3209088  | 5963       | DELETE FROM sbtest1 WHERE id=?                                       |
| 1  | 2959358  | 5963       | INSERT INTO sbtest1 (id, k, c, pad) VALUES (?, ?, ?, ?)              |
| 1  | 2415318  | 5963       | BEGIN                                                                |
| 2  | 2266662  | 1881       | SELECT DISTINCT c FROM sbtest1 WHERE id BETWEEN ? AND ?+? ORDER BY c |
| -1 | 0        | 4082       | SELECT DISTINCT c FROM sbtest1 WHERE id BETWEEN ? AND ?+? ORDER BY c |
| -1 | 0        | 51261      | SELECT c FROM sbtest1 WHERE id=?                                     |
+----+----------+------------+----------------------------------------------------------------------+
```

可以看到 `hostgroup` 为 `-1` 的记录, 表示这次查询并未路由到后端的数据库实例上, 而是在 ProxySQL 的缓冲中获取到了结果
