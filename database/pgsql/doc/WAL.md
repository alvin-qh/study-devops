# WAL 日志

- [WAL 日志](#wal-日志)
  - [1. WAL 介绍](#1-wal-介绍)
    - [1.1. 简介](#11-简介)
    - [1.2. 基本概念](#12-基本概念)
  - [2. 相关操作和配置](#2-相关操作和配置)
    - [2.1. 查看 WAL 日志](#21-查看-wal-日志)
    - [2.2. WAL 日志的最大大小](#22-wal-日志的最大大小)
    - [2.3. WAL 日志切换](#23-wal-日志切换)
    - [2.4. 查看 WAL 日志内容](#24-查看-wal-日志内容)
  - [3. 开启 WAL 日志归档](#3-开启-wal-日志归档)
    - [3.1. 归档脚本](#31-归档脚本)
    - [3.2. 归档配置](#32-归档配置)
  - [3. 恢复数据](#3-恢复数据)

## 1. WAL 介绍

### 1.1. 简介

WAL 日志即 "预写式日志" (**W**rite **A**head **L**og), 是保证数据完整性的一种标准方法

WAL 的核心概念是: 要修改数据文件 (存储着表和索引), 需要先将这些修改的操作记录到 WAL 日志文件中. 如果遵循这种过程, 则无需在每个事务提交时将数据页面刷新到磁盘, 而是将其操作记录到 WAL 日志中, 其它任何还没有被应用到数据页面的改变可以根据其日志记录重放操作 (也被称为 REDO)

使用 WAL 可以显著降低磁盘的写次数, 因为只有日志文件需要被刷出到磁盘以保证事务被提交, 而被事务改变的每一个数据文件则不必被立即被刷新. 日志文件被按照顺序写入, 因此同步日志的代价要远低于刷写数据页面的代价, 在处理很多影响数据存储不同部分的小事务的服务器上这一点尤其明显

此外, 当服务器在处理很多小的并行事务时, 日志文件的一个 `fsync` 可以提交很多事务。关闭 `fsync` 对 `SELECT` 无影响, 而对 `UPDATE` 性能有较大提升 (某些场景提升了 `111%`); 关闭 `fsync` 参数的代价是巨大的, 当数据库主机遭受操作系统故障或硬件故障时, 数据库很有可能无法启动并丢失数据, 建议生产库不要关闭这参数

WAL 也使得在线备份和从指定时间点恢复数据的能力被支持. 通过归档 WAL 数据, 可以支持回转到被可用 WAL 数据覆盖的任何时间: 通过简单地安装数据库的一个较早的物理备份, 并且重放 WAL 日志一直到所期望的时间. 另外, 该物理备份不需要是数据库状态的一个一致的快照, 如果它的制作经过了一段时间, 则重放这一段时间的 WAL 日志将会修复任何内部不一致性

### 1.2. 基本概念

类似于 Oracle 的 REDO, PostgreSQL 的 REDO 文件被称为 WAL 文件或 XLOG 文件, 存放在 `$PGDATA/pg_xlog` 或 `$PGDATA/pg_wal` 目录中 (从 10 版本开始, 只保留 WAL, 不再支持 XLOG 方式). 任何试图修改数据库数据的操作都会写一份日志到 WAL 文件

所以, 当数据库中数据发生变更时:

- `change` 发生时: 先要将变更后内容计入 WAL Buffer 中, 再将变更后的数据写入 Data Buffer;
- `commit` 发生时: WAL Buffer 中数据刷新到磁盘;
- `checkpoint` 发生时: 将所有 Data Buffer 刷新的磁盘;

WAL 文件的文件名由 24 个 16 进制字符组成, 每 8 个字符一组, 每组的意义如下:

```plaintext
00000001  00000000  0000000E0
时间线Id   LogId     LogSeg
```

其定义如下: `timelineId + (uint32)LSN − 1 / (16M ∗ 256） + (uint32)(LSN − 1 / 16M) % 256`

## 2. 相关操作和配置

### 2.1. 查看 WAL 日志

查看 WAL 文件列表

```sql
SELECT * FROM pg_ls_waldir() ORDER BY modification ASC;

           name           |   size   |      modification
--------------------------+----------+------------------------
 000000010000000000000006 | 16777216 | 2023-07-02 11:48:30+00
 000000010000000000000005 | 16777216 | 2023-07-02 11:53:35+00
```

查看当前 WAL 文件名称

```sql
SELECT pg_walfile_name(pg_current_wal_lsn());

     pg_walfile_name
--------------------------
 000000010000000000000005
```

其中 `pg_current_wal_lsn()` 函数用于查询当前 WAL 日志 LSN 位置

### 2.2. WAL 日志的最大大小

通过配置文件的 `max_wal_size` 配置项可以指定 WAL 日志文件的最大大小, 默认为 1 GB, 这个值设置的过大会导致崩溃回复时间增长

可以通过如下命令查看 `max_wal_size` 的设置值

```sql
SHOW max_wal_size;

 max_wal_size
--------------
 1GB
```

### 2.3. WAL 日志切换

WAL 日志切换会将当前 WAL 日志立即切换到新的日志上, 命令如下:

```sql
SELECT pg_switch_wal();

 pg_switch_wal
---------------
 0/7000078


SELECT * FROM pg_ls_waldir() ORDER BY modification ASC;

           name           |   size   |      modification
--------------------------+----------+------------------------
 000000010000000000000005 | 16777216 | 2023-07-02 14:06:34+00
 000000010000000000000006 | 16777216 | 2023-07-02 14:07:06+00
 000000010000000000000007 | 16777216 | 2023-07-02 14:07:36+00
```

### 2.4. 查看 WAL 日志内容

通过 PostgreSQL 附带的工具可以查看指定 WAL 日志的内容, 命令如下:

```bash
pg_waldump 000000010000000000000009

rmgr: Standby     len (rec/tot):     50/    50, tx:          0, lsn: 0/09000028, prev 0/08000328, desc: RUNNING_XACTS nextXid 740 latestCompletedXid 739 oldestRunningXid 740
rmgr: Heap        len (rec/tot):     54/   186, tx:        740, lsn: 0/09000060, prev 0/09000028, desc: INSERT off 3 flags 0x00, blkref #0: rel 1663/16385/16386 blk 0 FPW
rmgr: Transaction len (rec/tot):     34/    34, tx:        740, lsn: 0/09000120, prev 0/09000060, desc: COMMIT 2023-07-02 14:16:38.183753 UTC
rmgr: Standby     len (rec/tot):     50/    50, tx:          0, lsn: 0/09000148, prev 0/09000120, desc: RUNNING_XACTS nextXid 741 latestCompletedXid 740 oldestRunningXid 741
rmgr: Heap        len (rec/tot):     59/    59, tx:        741, lsn: 0/09000180, prev 0/09000148, desc: INSERT off 4 flags 0x00, blkref #0: rel 1663/16385/16386 blk 0
rmgr: Transaction len (rec/tot):     34/    34, tx:        741, lsn: 0/090001C0, prev 0/09000180, desc: COMMIT 2023-07-02 14:17:02.570169 UTC
rmgr: Standby     len (rec/tot):     50/    50, tx:          0, lsn: 0/090001E8, prev 0/090001C0, desc: RUNNING_XACTS nextXid 742 latestCompletedXid 741 oldestRunningXid 742
rmgr: Heap        len (rec/tot):     54/    54, tx:        742, lsn: 0/09000220, prev 0/090001E8, desc: DELETE off 1 flags 0x00 KEYS_UPDATED , blkref #0: rel 1663/16385/16386 blk 0
rmgr: Heap        len (rec/tot):     54/    54, tx:        742, lsn: 0/09000258, prev 0/09000220, desc: DELETE off 2 flags 0x00 KEYS_UPDATED , blkref #0: rel 1663/16385/16386 blk 0
rmgr: Heap        len (rec/tot):     54/    54, tx:        742, lsn: 0/09000290, prev 0/09000258, desc: DELETE off 3 flags 0x00 KEYS_UPDATED , blkref #0: rel 1663/16385/16386 blk 0
rmgr: Heap        len (rec/tot):     54/    54, tx:        742, lsn: 0/090002C8, prev 0/09000290, desc: DELETE off 4 flags 0x00 KEYS_UPDATED , blkref #0: rel 1663/16385/16386 blk 0
rmgr: Transaction len (rec/tot):     34/    34, tx:        742, lsn: 0/09000300, prev 0/090002C8, desc: COMMIT 2023-07-02 14:17:27.612908 UTC
rmgr: Standby     len (rec/tot):     50/    50, tx:          0, lsn: 0/09000328, prev 0/09000300, desc: RUNNING_XACTS nextXid 743 latestCompletedXid 742 oldestRunningXid 743
```

## 3. 开启 WAL 日志归档

所谓 WAL 日志归档, 即在 WAL 日志切换后, 将前一个日志文件复制到指定的路径进行备份

所以日志归档只需要一个简单 `cp` 命令即可, 但也可以增加复杂的, 加上一些文件检查和过期文件删除的逻辑

### 3.1. 归档脚本

一个典型的 WAL 日志归档脚本如下:

```bash
#!/usr/bin/env bash

# /etc/postgresql/archive.sh

set -ex;

# 设置日志归档目录
arch_dir="/var/lib/postgresql/archive/";

# 检查并创建归档日志路径
test ! -f "$arch_dir" && mkdir -p "$arch_dir";

# 将归档文件复制到指定位置
test ! -f "$arch_dir/$1" && cp --preserve=timestamps $2 "$arch_dir/$1";

# 删除 7 天以前的归档文件
find "$arch_dir" -type f -mtime +7 -exec rm -f {} \;
```

### 3.2. 归档配置

在 PostgreSQL 配置文件中, 通过如下配置开启归档

- `wal_level=replica`: 设置 WAL 日志的等级 (取值可以为 `minimal`, `replica` 或 `logical`), 默认为 `replica`, 该级别写入日志的信息足够支持 WAL 的归档和复制; 其它两个级别会减少或增加日志的内容;

- `archive_mode=on`: 开启归档模式, 如果该值为 `off`, 则不会执行 `archive_command` 指定的命令;

- `archive_command='bash /etc/postgresql/archive.sh %f %p'`: 当 `archive_mode=on` 是, 该配置必须填写. 该配置为一个 Shell 命令行, 用于将指定 WAL 拷贝到归档位置, 其中:

  - `%f` 表示要被归档的 WAL 日志文件的文件名;
  - `%p` 表示要被归档的 WAL 日志文件的全路径名;

  所以, 最简单的 `archive_command` 配置为 `cp --preserve=timestamps %p "$target\%p"` 即可;

## 3. 恢复数据

如果因为数据库崩溃导致数据丢失, 可以从归档的 WAL 文件中恢复数据, 具体方法如下:
