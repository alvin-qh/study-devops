# PostgreSQL 常用命令

- [PostgreSQL 常用命令](#postgresql-常用命令)
  - [1. 基本命令](#1-基本命令)
    - [1.1. 系统命令](#11-系统命令)
    - [1.2. 库表操作命令](#12-库表操作命令)
  - [2. 数据库操作命令](#2-数据库操作命令)
    - [2.1. 用户相关命令](#21-用户相关命令)
    - [2.2. 备份恢复数据库](#22-备份恢复数据库)

## 1. 基本命令

### 1.1. 系统命令

1. 进入命令解释器

   默认的用户和数据库是 `postgres`

   ```bash
   psql -U <username> -d <db-name> -h <host> -p <port> -W
   ```

   - `-W` 表示提示输入密码

   如未指定用户名, 则会使用当前操作系统的登录用户作为数据库用户进行登录

2. 显示 PostgreSQL 版本信息

   ```sql
   \copyright
   ```

3. 数据库默认字符集

   显示默认字符集

   ```sql
   \encoding
   ```

   设定默认字符集

   ```sql
   \encoding <charset>
   ```

4. 显示帮助信息

   显示特定命令帮助信息

   ```sql
   \h <command>
   ```

   显示所有命令的帮助信息

   ```sql
   \h *
   ```

5. 通过交互方式输入一个变量

   ```sql
   \prompt <display-message> <variable>
   ```

   例如:

   ```sql
   \prompt 'Input table name\n' t_name

   Input table name
   "user"

   \d :t_name
                             Table "public.user"
   Column |         Type          | Collation | Nullable | Default
   --------+-----------------------+-----------+----------+---------
   id     | integer               |           |          |
   name   | character varying(20) |           |          |
   ```

6. 修改指定用户的密码

   ```sql
   \password <username>
   ```

7. 导入脚本文件

   ```sql
   \i backup.sql
   ```

8. 在线编辑 SQL 脚本

   ```sql
   \e
   ```

   该命令会打开 VI 编辑器, 在其中输入 SQL 脚本, 保存退出后即会被执行

9. 输出字符串

   ```sql
   \echo <string/variable>
   ```

10. 退出 `psql` 命令解释器

   ```sql
   \q
   ```

### 1.2. 库表操作命令

1. 切换数据库

   相当于 MySQL 的 `USE <db-name>`

   ```sql
   \c <db-name>
   ```

2. 列举数据库

   相当于 MySQL 的 `SHOW DATABASES`

   ```sql
   \l
   ```

3. 列举表

   相当于 MySQL 的 `SHOW TABLES`

   ```sql
   \dt
   ```

   或

   ```sql
   \d
   ```

4. 查看表结构

   相当于 MySQL 的 `DESC <table-name>`; `SHOW COLUMNS FROM <table-name>`

   ```sql
   \d <table-name>
   ```

5. 查看索引

   ```sql
   \di
   ```

## 2. 数据库操作命令

### 2.1. 用户相关命令

1. 切换登录用户

   ```sql
   \c <db-name> <username>
   ```

   `\c` 命令在切换数据库时, 可以重新指定登录用户

   如果只需要切换用户, 则可以执行

   ```sql
   \c - <username>
   ```

2. 创建用户

   ```sql
   CREATE USER <username>;
   ```

   或

   ```sql
   CREATE USER <role-name>[ WITH <role-option>];
   ```

   `CREATE USER` 和 `CREATE ROLE` 命令的区别在于: `CREATE USER` 创建的用户默认是有登录权限的, 而 `CREATE ROLE` 没有

   例如创建一个具备登录权限的用户

   ```sql
   CREATE ROLE <role-name> WITH LOGIN;
   ```

   可以通过 `\h CREATE ROLE` 查看所有可用的选项

3. 修改用户

   ```sql
   ALTER ROLE <role-name> WITH <role-option>;
   ```

   例如禁止用户登录

   ```sql
   ALTER ROLE <role-name> WITH NOLOGIN;
   ```

4. 显示用户和用户的用户属性

   ```sql
   \du
   ```

5. 为用户授权

   ```sql
   GRANT <permission> ON <table-name> TO <role-name>;
   ```

   即为指定用户 (`<role-name>`) 在指定表 (`<table-name>`) 设置指定权限 (`<permission`>), 例如:

   ```sql
   GRANT UPDATE ON demo TO demo_role;                        -- 赋予 demo_role 用户在 demo 表的 UPDATE 权限
   GRANT SELECT ON ALL TABLES IN SCHEMA PUBLIC to demo_role; -- 赋予 demo_role 用户所有表的 SELECT 权限
   GRANT ALL ON demo TO demo_role;                           -- 赋予 demo_role 用户在 demo 表的所有权限
   GRANT SELECT ON demo TO PUBLIC;                           -- 将 SELECT 权限赋给所有用户
   ```

   - `ALL` 代表所访问权限, `PUBLIC` 代表所有用户

   可通过 `\h GRANT` 显示所有可用的权限

6. 显示用户的访问权限

### 2.2. 备份恢复数据库

1. 数据库备份

   备份指定数据库

   ```bash
   pg_dump -U <username> <db-name> > ./backup.sql
   ```

   备份全部数据库

   ```bash
   pg_dumpall -U <username> > ./backup.sql
   ```

2. 恢复数据库

   ```bash
   psql -U <username> -d <db-name> -f ./backup.sql
   ```

   该命令可以导入任意 SQL 脚本

3. 查看当前用户具备的访问权限

   ```sql
   \z
   ```

   或

   ```sql
   \dp
   ```

4. 撤销用户权限

   ```sql
   REVOKE <permission> ON <table-name> FROM <role-name>;
   ```

5. 用户组

   在 PostgreSQL 中组本质上也是用户角色, 即包含其它用户角色的角色即为组

   ```sql
   CREATE ROLE temp_user;        -- 创建用户 temp_user

   GRANT temp_users TO demo_role; -- 将 demo_role 用户加入 temp_users 组
   GRANT temp_users TO test_user; -- 将 test_user 用户加入 temp_users 组
   ```

   组继承权限: 一旦一个用户属于一个组, 则可以通过 `INHERIT` 权限来表示该用户继承所在组的所有权限

   ```sql
   ALTER ROLE demo_role INHERIT;
   ```

6. 切换会话用户

   ```sql
   SET ROLE <role-name>; -- 切换到指定用户
   RESET ROLE;           -- 切换回最初的用户
   ```

7. 查看当前用户

   ```sql
   \conninfo
   ```

   该命令用于查看连接到命令解释器的用户, 即连接用户

   ```sql
   SELECT current_user;
   ```

   该命令用户查看当前会话的用户, 即会话用户

8. 删除用户和组

   ```sql
   DROP ROLE <role-name>;
   DROP ROLE IF EXISTS <role-name>;
   ```

   删除组只会删除组本身, 组的成员并不会被删除, 相当于解散组

9. 列出所有用户和组

   ```sql
   \du
   ```

   或

   ```sql
   \du+
   ```

   `\du+` 表示在结果中显示额外的 `Description` 列

   通过 SQL 语句也可以查询所有用户

   ```sql
   SELECT usename AS role_name,
     CASE
       WHEN usesuper AND usecreatedb THEN
         CAST('superuser, create database' AS pg_catalog.text)
       WHEN usesuper THEN
         CAST('superuser' AS pg_catalog.text)
       WHEN usecreatedb THEN
         CAST('create database' AS pg_catalog.text)
       ELSE
         CAST('' AS pg_catalog.text)
       END role_attributes
   FROM pg_catalog.pg_user
   ORDER BY role_name desc;
   ```
