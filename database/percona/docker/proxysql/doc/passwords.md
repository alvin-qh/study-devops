# 在 ProxySQL 中管理 MySQL 密码

- [在 ProxySQL 中管理 MySQL 密码](#在-proxysql-中管理-mysql-密码)
  - [1. 密码格式](#1-密码格式)
    - [1.1. 散列密码和身份验证](#11-散列密码和身份验证)
  - [2. 如何输入新密码](#2-如何输入新密码)
    - [2.1. admin-hash\_passwords 变量](#21-admin-hash_passwords-变量)
  - [3. 注意事项](#3-注意事项)

ProxySQL 根据前端发送的不同 SQL 去访问不同的后端数据库实例, 因此登录 ProxySQL 需要进行身份验证, 所以 ProxySQL 需要存储足够的信息来对登录用户进行身份认证

ProxySQL 还需要这些信息来建立与后端的连接, 或在已建立的连接中发出 `CHANGE_USER` 操作

ProxySQL 将用户信息存储在 `mysql_users` 表中, 包括:

- `MySQL_Authentication()` 对象负责将用户信息存储到 RUNTIME;
- `main.mysql_users` 将用户信息存储在 MEMORY 中;
- `disk.mysql_users` 将用户信息存储在 DISK 中;

`mysql_user` 表通过 `username` 和 `password` 字段存储用户认证信息

## 1. 密码格式

密码以 `2` 种格式存储在 `mysql_users` 表的 `password` 字段中, 包括:

- 明文密码
- 哈希密码

明文密码利于阅读和修改, 但有安全隐患; 哈希密码要和后端 MySQL 数据库一致, 可以在 `mysql.user` 表中通过 `password` 字段查询得到

ProxySQL 中, 哈希密码必须以 `*` 字符开头

### 1.1. 散列密码和身份验证

在 MySQL 和 ProxySQ L中, 散列密码的生成方式为 `SHA1(SHA1('<明文密码>'))`, 所以通过密码散列值是无法还原明文密码的

当客户端连接到 ProxySQL 时, 其能够使用哈希密码对其进行身份验证

在第一次客户端身份验证期间, ProxySQL 可以对密码进行部分散列, 即 `SHA1('<明文密码>')`, 这些信息内部存储在 RUNTIME 层, 并且允许 ProxySQL 通过其连接后端的数据库

## 2. 如何输入新密码

ProxySQL 不提供产生散列密码的功能 (未提供 `PASSWORD()` 等函数), 这意味着:

- 在 ProxySQL 中, 密码以其插入 `mysql_users` 表时的格式进行保存;
- 如果希望在管理界面中输入密码时, 可以通过从后端数据库的 `mysql.user` 表的 `authentication_string` 字段获取哈希密码;

### 2.1. admin-hash_passwords 变量

如果设置变量 `admin-hash_passwords=true` 后 (默认为 `true`), 当加载用户信息到 RUNTIME 时, ProxySQL 会自动对密码字段进行哈希处理, 同时 MEMORY 和 DISK 中的密码不发生变化

要验证这一点, 只需要将 RUNTIME 的用户信息复制回 MEMORY, 即可看到哈希后的密码字段, 例如:

```sql
-- 在 MEMORY 中插入用户信息
INSERT INTO `mysql_users` (`username`, `password`)
VALUES ('user1', 'password1'),
       ('user2', 'password2');

-- 查询 MEMORY 中存储的用户信息
SELECT `username`, `password` FROM `mysql_users`;
+----------+-----------+
| username | password  |
+----------+-----------+
| user1    | password1 |
| user2    | password2 |
+----------+-----------+

-- 将 MEMORY 中的用户信息写入 RUNTIME
LOAD MYSQL USERS TO RUNTIME;

-- 确认此时 MEMORY 中的信息未发生变化
SELECT `username`, `password` FROM `mysql_users`;
+----------+-----------+
| username | password  |
+----------+-----------+
| user1    | password1 |
| user2    | password2 |
+----------+-----------+

-- 将 RUNTIME 信息写回 MEMORY
SAVE MYSQL USERS FROM RUNTIME;

-- 此时可以看到哈希后的密码字段
SELECT `username`, `password` FROM `mysql_users`;
+----------+-------------------------------------------+
| username | password                                  |
+----------+-------------------------------------------+
| user1    | *668425423DB5193AF921380129F465A6425216D0 |
| user2    |*DC52755F3C09F5923046BD42AFA76BD1D80DF2E9 |
+----------+-------------------------------------------+
```

当然, 也可以将哈希密码存回 DISK

```sql
SAVE MYSQL USERS TO DISK;
```

## 3. 注意事项

注意, MySQL 后端尽量选择 `mysql_native_password` 密码插件, 即在 MySQL 配置中设置

```ini
[mysqld]
default_authentication_plugin = mysql_native_password
```

以达到最好的兼容性
