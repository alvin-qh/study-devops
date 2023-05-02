# 后端 SSL 配置

- [后端 SSL 配置](#后端-ssl-配置)
  - [1. 配置](#1-配置)
    - [1.1. 启用后端数据库的 SSL 连接](#11-启用后端数据库的-ssl-连接)
    - [1.2. 配置密钥和证书](#12-配置密钥和证书)
    - [1.3. 验证 SSL 配置](#13-验证-ssl-配置)
    - [1.4. 更新密钥和证书](#14-更新密钥和证书)
    - [1.5. 扩展配置变量](#15-扩展配置变量)
  - [2. 对前端连接启用 SSL](#2-对前端连接启用-ssl)
    - [2.1. 配置](#21-配置)
    - [2.2. 支持情况](#22-支持情况)
    - [2.3. 更新密钥和证书](#23-更新密钥和证书)

从 ProxySQL v1.2.0e 版本开始, 已经支持通过 SSL 连接后端数据库

重要说明:

- 1.x 版本仅支持与后端数据库的 SSL 连接; v2.x之前的版本无法进行客户端的 SSL 连接;
- 对于 v1.4.5 版本, 由于 ProxySQL 使用了 `mariadb-connector-c-2.3.1` 库, 所以只支持 SSL/TLSv1.0 协议;
- 对于 v2.x 版本, 通过 `mariadb-connector-3.0.2` 支持了 SSL/TLSv1.0, TLSv1.1 以及 TLSv1.2 版本的协议, 同时支持前后端的 SSL 连接;

## 1. 配置

### 1.1. 启用后端数据库的 SSL 连接

要启用 SSL 连接, 需要进行如下配置:

- 在 ProxySQL Admin 中, 针对所需的后端数据库连接配置, 更新 `mysql_servers` 表的 `use_ssl` 字段值为 `1`;
- 如果是使用 ProxySQL v1.x 版本, 还需要更新相关的 ProxySQL Admin 全局变量;
- 如果针对某个后端数据库, 需要同时启用 SSL 和非 SSL 连接, 则需要将该后端数据库配置两次, 且分配到不同的数据库组中, 再通过路由规则设置其访问方式

例如, 要针对指定后端数据库 (`127.0.0.1:21891`) 配置 SSL 连接:

```sql
-- 查看当前 MySQL 后端数据库连接配置
SELECT * FROM `mysql_servers`;
+--------------+-----------+-------+--------+--------+-------------+-----------------+---------------------+---------+----------------+
| hostgroup_id | hostname  | port  | status | weight | compression | max_connections | max_replication_lag | use_ssl | max_latency_ms |
+--------------+-----------+-------+--------+--------+-------------+-----------------+---------------------+---------+----------------+
| 1            | 127.0.0.1 | 21891 | ONLINE | 1      | 0           | 1000            | 0                   | 0       | 0              |
| 2            | 127.0.0.1 | 21892 | ONLINE | 1      | 0           | 1000            | 0                   | 0       | 0              |
| 2            | 127.0.0.1 | 21893 | ONLINE | 1      | 0           | 1000            | 0                   | 0       | 0              |
+--------------+-----------+-------+--------+--------+-------------+-----------------+---------------------+---------+----------------+

-- 更新当前 MySQL 后端数据库连接配置, 开启 use_ssl 选项
UPDATE `mysql_servers` SET `use_ssl`=1 WHERE `port`=21891;

-- 查看修改后的结果
SELECT * FROM `mysql_servers`;
+--------------+-----------+-------+--------+--------+-------------+-----------------+---------------------+---------+----------------+
| hostgroup_id | hostname  | port  | status | weight | compression | max_connections | max_replication_lag | use_ssl | max_latency_ms |
+--------------+-----------+-------+--------+--------+-------------+-----------------+---------------------+---------+----------------+
| 1            | 127.0.0.1 | 21891 | ONLINE | 1      | 0           | 1000            | 0                   | 1       | 0              |
| 2            | 127.0.0.1 | 21892 | ONLINE | 1      | 0           | 1000            | 0                   | 0       | 0              |
| 2            | 127.0.0.1 | 21893 | ONLINE | 1      | 0           | 1000            | 0                   | 0       | 0              |
+--------------+-----------+-------+--------+--------+-------------+-----------------+---------------------+---------+----------------+

-- 将修改的配置从 MEMORY 加载到 RUNTIME
LOAD MYSQL SERVERS TO RUNTIME;

-- 查询 RUNTIME 中的 MySQL 后端数据库连接配置, 确认 use_ssl 已经开启
SELECT * FROM `runtime_mysql_servers`;
+--------------+-----------+-------+--------+--------+-------------+-----------------+---------------------+---------+----------------+
| hostgroup_id | hostname  | port  | status | weight | compression | max_connections | max_replication_lag | use_ssl | max_latency_ms |
+--------------+-----------+-------+--------+--------+-------------+-----------------+---------------------+---------+----------------+
| 1            | 127.0.0.1 | 21891 | ONLINE | 1      | 0           | 1000            | 0                   | 1       | 0              |
| 2            | 127.0.0.1 | 21892 | ONLINE | 1      | 0           | 1000            | 0                   | 0       | 0              |
| 2            | 127.0.0.1 | 21893 | ONLINE | 1      | 0           | 1000            | 0                   | 0       | 0              |
+--------------+-----------+-------+--------+--------+-------------+-----------------+---------------------+---------+----------------+
```

到这一步为止:

- 对于 ProxySQL v1.x, 如果通过 `127.0.0.1:21891` 配置连接后端数据库是不会启用 SSL 连接的, 因为尚未配置密钥和证书. 取而代之的是非 SSL 连接被发起;
- 对于 ProxySQL v2.x, 如果设置了 `use_ssl=1` 字段值, 则所有到该后端数据库的新建连接会尝试启用 SSL (依赖 MySQL 内置的密钥和证书);

下一步就是要配置密钥和证书 (对于 ProxySQL v2.x 版本, 意味着使用非默认认证)

### 1.2. 配置密钥和证书

```sql
-- 查询 ProxySQL 和 SSL 配置相关的全局变量
SELECT * FROM `global_variables` WHERE `variable_name` LIKE 'mysql-ssl%';
+-----------------------+----------------+
| variable_name         | variable_value |
+-----------------------+----------------+
| mysql-ssl_p2s_ca      |                |
| mysql-ssl_p2s_capath  |                |
| mysql-ssl_p2s_cert    |                |
| mysql-ssl_p2s_key     |                |
| mysql-ssl_p2s_cipher  |                |
| mysql-ssl_p2s_crl     |                |
| mysql-ssl_p2s_crlpath |                |
+-----------------------+----------------+

-- 设置证书文件路径
SET `mysql-ssl_p2s_cert`="/home/vagrant/newcerts/client-cert.pem";

-- 设置密钥文件路径
SET mysql-ssl_p2s_key="/home/vagrant/newcerts/client-key.pem";

-- 查看设置结果
SELECT * FROM global_variables WHERE variable_name LIKE 'mysql-ssl%';
+-----------------------+-----------------------------------------+
| variable_name         | variable_value                          |
+-----------------------+-----------------------------------------+
| mysql-ssl_p2s_ca      |                                         |
| mysql-ssl_p2s_capath  |                                         |
| mysql-ssl_p2s_cert    | /home/vagrant/newcerts/client-cert.pem  |
| mysql-ssl_p2s_key     | /home/vagrant/newcerts/client-key.pem   |
| mysql-ssl_p2s_cipher  |                                         |
| mysql-ssl_p2s_crl     |                                         |
| mysql-ssl_p2s_crlpath |                                         |
+-----------------------+-----------------------------------------+

-- 将 MEMORY 中的修改更新到 RUNTIME 中
LOAD MYSQL VARIABLES TO RUNTIME;
```

完成上述操作后, 所有到指定后端数据库 (`127.0.0.1:21891`) 的新连接将启用 SSL 连接

如果配置测试无误, 即可将其持久化到 DISK 层

```sql
SAVE MYSQL SERVERS TO DISK;
SAVE MYSQL VARIABLES TO DISK;
```

### 1.3. 验证 SSL 配置

如果要验证 ProxySQL 和 MySQL 之间是否能依照预期建立 SSL 连接, 只需要连接到 **ProxySQL 代理端** 并且执行 `SHOW SESSION STATUS LIKE "Ssl_cipher"` 指令即可, 例如:

```sql
SHOW SESSION STATUS LIKE "Ssl_cipher"
+---------------+----------------------+
| Variable_name | Value                |
+---------------+----------------------+
| Ssl_cipher    | ECDHE-RSA-AES256-SHA |
+---------------+----------------------+
```

如果返回结果中 `Value` 字段值为空, 则表示设置失败, 例如:

```sql
SHOW SESSION STATUS LIKE "Ssl_cipher";
+---------------+-------+
| Variable_name | Value |
+---------------+-------+
| Ssl_cipher    |       |
+---------------+-------+
```

### 1.4. 更新密钥和证书

如果要针对指定后端数据库连接更新密钥和证书, 可以采用如下两种方法:

1. 用新的密钥和证书文件替换掉原有的证书密钥文件, 文件替换是一个原子操作, 所以不会中断 ProxySQL 对这些证书文件的访问;
2. 将配置密钥和证书文件的 ProxySQL 全局变量进行更改, 然后将其加载到 RUNTIME 中 (`LOAD MYSQL VARIABLES TO RUNTIME`), 这一步操作也是原子操作, 不会中断 ProxySQL 的执行

### 1.5. 扩展配置变量

除了上面提到的于 SSL 相关变量外, ProxySQL 还提供了以下扩展变量用于配置 ProxySQL 和后端数据库的 SSL 连接:

- `mysql-ssl_p2s_ca`
- `mysql-ssl_p2s_capath`
- `mysql-ssl_p2s_crl`
- `mysql-ssl_p2s_crlpath`

请参考 [官方文档](https://proxysql.com/documentation/global-variables/mysql-variables/) 中对这几个变量的说明

## 2. 对前端连接启用 SSL

自 2.0 版本后, ProxySQL 支持对前端连接启用 SSL, 该功能默认为关闭

### 2.1. 配置

要为前端连接启用 SSL, 需要设置 ProxySQL 的 `mysql-have_ssl=true` 全局变量, 设置后, ProxySQL 会在 `/var/lib/proxysql` 路径下自动生成如下几个文件:

- `proxysql-ca.pem`
- `proxysql-cert.pem`
- `proxysql-key.pem`

> 注意, 这些文件也可以替换成通过其它渠道生成的密钥证书文件

另外要注意, 配置生效后 (即设置 `mysql-have_ssl=true` 且执行 `LOAD MYSQL VARIABLES TO RUNTIME` 之后), 只有新的前端连接会启用 SSL 连接, 之前已建立的连接不受影响

要验证前端 SSL 连接是否正常, 只需要连接到 ProxySQL 代理端并发送 `'\s'` 字符即可, 例如;

```bash
mysql -h'127.0.0.1' -uproxysql -p -e '\s' | grep -P 'SSL|Connection'

SSL: Cipher in use is DHE-RSA-AES256-SHA
Connection: 127.0.0.1 via TCP/IP
```

### 2.2. 支持情况

ProxySQL 对前端 SSL 的支持协议包括:

```plaintext
TLSv1
TLSv1.1
TLSv1.2
TLSv1.3
```

> SSLv2 and SSLv3 were removed in version 2.0.6

支持的加密方式包括:

```plaintext
TLS_AES_256_GCM_SHA384:  TLS_AES_256_GCM_SHA384  TLSv1.3 Kx=any      Au=any  Enc=AESGCM(256) Mac=AEAD
TLS_CHACHA20_POLY1305_SHA256:  TLS_CHACHA20_POLY1305_SHA256 TLSv1.3 Kx=any      Au=any  Enc=CHACHA20/POLY1305(256) Mac=AEAD
TLS_AES_128_GCM_SHA256:  TLS_AES_128_GCM_SHA256  TLSv1.3 Kx=any      Au=any  Enc=AESGCM(128) Mac=AEAD
ECDHE-ECDSA-AES256-GCM-SHA384:  ECDHE-ECDSA-AES256-GCM-SHA384 TLSv1.2 Kx=ECDH     Au=ECDSA Enc=AESGCM(256) Mac=AEAD
ECDHE-RSA-AES256-GCM-SHA384:  ECDHE-RSA-AES256-GCM-SHA384 TLSv1.2 Kx=ECDH     Au=RSA  Enc=AESGCM(256) Mac=AEAD
DHE-RSA-AES256-GCM-SHA384:  DHE-RSA-AES256-GCM-SHA384 TLSv1.2 Kx=DH       Au=RSA  Enc=AESGCM(256) Mac=AEAD
ECDHE-ECDSA-CHACHA20-POLY1305:  ECDHE-ECDSA-CHACHA20-POLY1305 TLSv1.2 Kx=ECDH     Au=ECDSA Enc=CHACHA20/POLY1305(256) Mac=AEAD
ECDHE-RSA-CHACHA20-POLY1305:  ECDHE-RSA-CHACHA20-POLY1305 TLSv1.2 Kx=ECDH     Au=RSA  Enc=CHACHA20/POLY1305(256) Mac=AEAD
DHE-RSA-CHACHA20-POLY1305:  DHE-RSA-CHACHA20-POLY1305 TLSv1.2 Kx=DH       Au=RSA  Enc=CHACHA20/POLY1305(256) Mac=AEAD
ECDHE-ECDSA-AES128-GCM-SHA256:  ECDHE-ECDSA-AES128-GCM-SHA256 TLSv1.2 Kx=ECDH     Au=ECDSA Enc=AESGCM(128) Mac=AEAD
ECDHE-RSA-AES128-GCM-SHA256:  ECDHE-RSA-AES128-GCM-SHA256 TLSv1.2 Kx=ECDH     Au=RSA  Enc=AESGCM(128) Mac=AEAD
DHE-RSA-AES128-GCM-SHA256:  DHE-RSA-AES128-GCM-SHA256 TLSv1.2 Kx=DH       Au=RSA  Enc=AESGCM(128) Mac=AEAD
ECDHE-ECDSA-AES256-SHA384:  ECDHE-ECDSA-AES256-SHA384 TLSv1.2 Kx=ECDH     Au=ECDSA Enc=AES(256)  Mac=SHA384
ECDHE-RSA-AES256-SHA384:  ECDHE-RSA-AES256-SHA384 TLSv1.2 Kx=ECDH     Au=RSA  Enc=AES(256)  Mac=SHA384
DHE-RSA-AES256-SHA256:  DHE-RSA-AES256-SHA256   TLSv1.2 Kx=DH       Au=RSA  Enc=AES(256)  Mac=SHA256
ECDHE-ECDSA-AES128-SHA256:  ECDHE-ECDSA-AES128-SHA256 TLSv1.2 Kx=ECDH     Au=ECDSA Enc=AES(128)  Mac=SHA256
ECDHE-RSA-AES128-SHA256:  ECDHE-RSA-AES128-SHA256 TLSv1.2 Kx=ECDH     Au=RSA  Enc=AES(128)  Mac=SHA256
DHE-RSA-AES128-SHA256:  DHE-RSA-AES128-SHA256   TLSv1.2 Kx=DH       Au=RSA  Enc=AES(128)  Mac=SHA256
ECDHE-ECDSA-AES256-SHA:  ECDHE-ECDSA-AES256-SHA  TLSv1 Kx=ECDH     Au=ECDSA Enc=AES(256)  Mac=SHA1
ECDHE-RSA-AES256-SHA:  ECDHE-RSA-AES256-SHA    TLSv1 Kx=ECDH     Au=RSA  Enc=AES(256)  Mac=SHA1
DHE-RSA-AES256-SHA:  DHE-RSA-AES256-SHA      SSLv3 Kx=DH       Au=RSA  Enc=AES(256)  Mac=SHA1
ECDHE-ECDSA-AES128-SHA:  ECDHE-ECDSA-AES128-SHA  TLSv1 Kx=ECDH     Au=ECDSA Enc=AES(128)  Mac=SHA1
ECDHE-RSA-AES128-SHA:  ECDHE-RSA-AES128-SHA    TLSv1 Kx=ECDH     Au=RSA  Enc=AES(128)  Mac=SHA1
DHE-RSA-AES128-SHA:  DHE-RSA-AES128-SHA      SSLv3 Kx=DH       Au=RSA  Enc=AES(128)  Mac=SHA1
RSA-PSK-AES256-GCM-SHA384:  RSA-PSK-AES256-GCM-SHA384 TLSv1.2 Kx=RSAPSK   Au=RSA  Enc=AESGCM(256) Mac=AEAD
DHE-PSK-AES256-GCM-SHA384:  DHE-PSK-AES256-GCM-SHA384 TLSv1.2 Kx=DHEPSK   Au=PSK  Enc=AESGCM(256) Mac=AEAD
RSA-PSK-CHACHA20-POLY1305:  RSA-PSK-CHACHA20-POLY1305 TLSv1.2 Kx=RSAPSK   Au=RSA  Enc=CHACHA20/POLY1305(256) Mac=AEAD
DHE-PSK-CHACHA20-POLY1305:  DHE-PSK-CHACHA20-POLY1305 TLSv1.2 Kx=DHEPSK   Au=PSK  Enc=CHACHA20/POLY1305(256) Mac=AEAD
ECDHE-PSK-CHACHA20-POLY1305:  ECDHE-PSK-CHACHA20-POLY1305 TLSv1.2 Kx=ECDHEPSK Au=PSK  Enc=CHACHA20/POLY1305(256) Mac=AEAD
AES256-GCM-SHA384:  AES256-GCM-SHA384       TLSv1.2 Kx=RSA      Au=RSA  Enc=AESGCM(256) Mac=AEAD
PSK-AES256-GCM-SHA384:  PSK-AES256-GCM-SHA384   TLSv1.2 Kx=PSK      Au=PSK  Enc=AESGCM(256) Mac=AEAD
PSK-CHACHA20-POLY1305:  PSK-CHACHA20-POLY1305   TLSv1.2 Kx=PSK      Au=PSK  Enc=CHACHA20/POLY1305(256) Mac=AEAD
RSA-PSK-AES128-GCM-SHA256:  RSA-PSK-AES128-GCM-SHA256 TLSv1.2 Kx=RSAPSK   Au=RSA  Enc=AESGCM(128) Mac=AEAD
DHE-PSK-AES128-GCM-SHA256:  DHE-PSK-AES128-GCM-SHA256 TLSv1.2 Kx=DHEPSK   Au=PSK  Enc=AESGCM(128) Mac=AEAD
AES128-GCM-SHA256:  AES128-GCM-SHA256       TLSv1.2 Kx=RSA      Au=RSA  Enc=AESGCM(128) Mac=AEAD
PSK-AES128-GCM-SHA256:  PSK-AES128-GCM-SHA256   TLSv1.2 Kx=PSK      Au=PSK  Enc=AESGCM(128) Mac=AEAD
AES256-SHA256:  AES256-SHA256           TLSv1.2 Kx=RSA      Au=RSA  Enc=AES(256)  Mac=SHA256
AES128-SHA256:  AES128-SHA256           TLSv1.2 Kx=RSA      Au=RSA  Enc=AES(128)  Mac=SHA256
ECDHE-PSK-AES256-CBC-SHA384:  ECDHE-PSK-AES256-CBC-SHA384 TLSv1 Kx=ECDHEPSK Au=PSK  Enc=AES(256)  Mac=SHA384
ECDHE-PSK-AES256-CBC-SHA:  ECDHE-PSK-AES256-CBC-SHA TLSv1 Kx=ECDHEPSK Au=PSK  Enc=AES(256)  Mac=SHA1
SRP-RSA-AES-256-CBC-SHA:  SRP-RSA-AES-256-CBC-SHA SSLv3 Kx=SRP      Au=RSA  Enc=AES(256)  Mac=SHA1
SRP-AES-256-CBC-SHA:  SRP-AES-256-CBC-SHA     SSLv3 Kx=SRP      Au=SRP  Enc=AES(256)  Mac=SHA1
RSA-PSK-AES256-CBC-SHA384:  RSA-PSK-AES256-CBC-SHA384 TLSv1 Kx=RSAPSK   Au=RSA  Enc=AES(256)  Mac=SHA384
DHE-PSK-AES256-CBC-SHA384:  DHE-PSK-AES256-CBC-SHA384 TLSv1 Kx=DHEPSK   Au=PSK  Enc=AES(256)  Mac=SHA384
RSA-PSK-AES256-CBC-SHA:  RSA-PSK-AES256-CBC-SHA  SSLv3 Kx=RSAPSK   Au=RSA  Enc=AES(256)  Mac=SHA1
DHE-PSK-AES256-CBC-SHA:  DHE-PSK-AES256-CBC-SHA  SSLv3 Kx=DHEPSK   Au=PSK  Enc=AES(256)  Mac=SHA1
AES256-SHA:  AES256-SHA              SSLv3 Kx=RSA      Au=RSA  Enc=AES(256)  Mac=SHA1
PSK-AES256-CBC-SHA384:  PSK-AES256-CBC-SHA384   TLSv1 Kx=PSK      Au=PSK  Enc=AES(256)  Mac=SHA384
PSK-AES256-CBC-SHA:  PSK-AES256-CBC-SHA      SSLv3 Kx=PSK      Au=PSK  Enc=AES(256)  Mac=SHA1
ECDHE-PSK-AES128-CBC-SHA256:  ECDHE-PSK-AES128-CBC-SHA256 TLSv1 Kx=ECDHEPSK Au=PSK  Enc=AES(128)  Mac=SHA256
ECDHE-PSK-AES128-CBC-SHA:  ECDHE-PSK-AES128-CBC-SHA TLSv1 Kx=ECDHEPSK Au=PSK  Enc=AES(128)  Mac=SHA1
SRP-RSA-AES-128-CBC-SHA:  SRP-RSA-AES-128-CBC-SHA SSLv3 Kx=SRP      Au=RSA  Enc=AES(128)  Mac=SHA1
SRP-AES-128-CBC-SHA:  SRP-AES-128-CBC-SHA     SSLv3 Kx=SRP      Au=SRP  Enc=AES(128)  Mac=SHA1
RSA-PSK-AES128-CBC-SHA256:  RSA-PSK-AES128-CBC-SHA256 TLSv1 Kx=RSAPSK   Au=RSA  Enc=AES(128)  Mac=SHA256
DHE-PSK-AES128-CBC-SHA256:  DHE-PSK-AES128-CBC-SHA256 TLSv1 Kx=DHEPSK   Au=PSK  Enc=AES(128)  Mac=SHA256
RSA-PSK-AES128-CBC-SHA:  RSA-PSK-AES128-CBC-SHA  SSLv3 Kx=RSAPSK   Au=RSA  Enc=AES(128)  Mac=SHA1
DHE-PSK-AES128-CBC-SHA:  DHE-PSK-AES128-CBC-SHA  SSLv3 Kx=DHEPSK   Au=PSK  Enc=AES(128)  Mac=SHA1
AES128-SHA:  AES128-SHA              SSLv3 Kx=RSA      Au=RSA  Enc=AES(128)  Mac=SHA1
PSK-AES128-CBC-SHA256:  PSK-AES128-CBC-SHA256   TLSv1 Kx=PSK      Au=PSK  Enc=AES(128)  Mac=SHA256
PSK-AES128-CBC-SHA:  PSK-AES128-CBC-SHA      SSLv3 Kx=PSK      Au=PSK  Enc=AES(128)  Mac=SHA1
```

### 2.3. 更新密钥和证书

自 v2.3.0 版本开始, ProxySQL 增加了动态更新前端认证相关 ProxySQL Admin 变量的命令: `PROXYSQL RELOAD TLS`

通过该命令更新当前认证的的正确方法为:

1. 确认前端 SSL 连接已经启用, 如果未启用, 则需要先设置 `mysql-have_ssl=true` 并让其在 RUNTIME 层生效
2. 将之前已存在的密钥和证书文件进行更新, 这些文件的存储路径如下:
    - `${DATADIR_PATH}/proxysql-ca.pem`
    - `${DATADIR_PATH}/proxysql-cert.pem`
    - `${DATADIR_PATH}/proxysql-key.pem`
3. 执行认证更新命令: `PROXYSQL RELOAD TLS`

如果上述操作均执行成功, 且检查 ProxySQL 错误日志中没有发现相关错误, 则新的认证信息已经被加载, 新建的前端 SSL 链接将使用新的认证信息
