# Percona 单实例

- [Percona 单实例](#percona-单实例)
  - [1. 容器配置](#1-容器配置)
    - [1.1. 通过环境变量定义管理员密码](#11-通过环境变量定义管理员密码)
    - [1.2. 映射配置文件](#12-映射配置文件)
    - [1.3. 映射端口](#13-映射端口)
    - [1.4. 映射数据卷](#14-映射数据卷)
    - [1.5. 映射日志](#15-映射日志)
      - [1.5.1. 映射到指定卷](#151-映射到指定卷)
      - [1.5.2. 映射到宿主机指定目录](#152-映射到宿主机指定目录)
      - [1.5.3. 注意事项](#153-注意事项)
    - [1.6. 创建健康检查用户](#16-创建健康检查用户)
  - [2. 启动容器](#2-启动容器)

为了易于演练, 本例中的所有操作均是基于 Docker 进行, 其它部署方式 (服务器部署或 K8S 部署) 方式类似

## 1. 容器配置

单实例模式的容器配置文件为 [docker-compose.yml](./docker-compose.yml) 文件

### 1.1. 通过环境变量定义管理员密码

Percona 的初始管理员密码可以通过容器的环境变量 `MYSQL_ROOT_PASSWORD` 来设置

可以通过 `MYSQL_DATABASE` 来设置登录 MySQL 后的默认数据库

可以通过 `MYSQL_USER` 和 `MYSQL_PASSWORD` 设置一个非 root 的新用户, 如果同时设置了 `MYSQL_DATABASE` 环境变量, 则会同时对这个数据库设置所有权限

可以在容器配置文件中, 通过 `environment` 属性设置容器环境变量, 如:

```yml
environment:
  - MYSQL_ROOT_PASSWORD=***
  - MYSQL_USER=user
  - MYSQL_PASSWORD=***
  - MYSQL_DATABASE=demo
```

也可以将环境变量保存在一个文件中 (例如: [env/percona.env](./env/percona.env) 文件), 并在容器配置文件中, 通过 `env_file` 属性指定环境变量文件的路径, 如:

```yml
env_file:
  - ./env/percona.env
```

### 1.2. 映射配置文件

如果需要修改默认配置, 需要将自定义的配置文件 (例如 [conf/default.cnf](./conf/default.cnf) 文件) 映射到容器的 `/etc/my.cnf.d` 路径下, 在容器配置文件中, 通过 `volumes` 属性指定文件映射

```yml
volumes:
  - ./conf/default.cnf:/etc/my.cnf.d/default.cnf:ro
```

配置文件中对密码处理插件做了约束如下:

```ini
default_authentication_plugin = mysql_native_password
```

`mysql_native_password` 是 MySQL 最传统的密码验证插件, 安全性较低, 但兼容性较高, 在和其它组件集成 (例如 ProxySQL) 时, 较为容易. 在内网环境, 可以使用 `mysql_native_password` 以达到最高的兼容性

### 1.3. 映射端口

如果需要在宿主机访问 Percona 服务, 需要将容器的 `3306` 端口进行映射

```yml
ports:
  - 3306:3306
```

如此便可以使用 MySQL Client 从宿主机访问 Percona 数据库实例

### 1.4. 映射数据卷

Percona 容器需要将其内部的 `var/lib/mysql` 路径映射到宿主机, 以便存储数据库文件, 如果不指定映射卷, Docker 会自行为容器创建一个匿名卷进行存储

创建一个卷

```yml
volumes:
  percona_data:
```

将创建的卷和容器进行映射

```yml
volumes:
    - percona_data:/var/lib/mysql
```

### 1.5. 映射日志

Percona 容器默认不输出日志文件, 这样不利于问题排查, 可以在配置文件 ([conf/default.conf](./conf/default.cnf)) 中要求输出日志如下:

```ini
[mysqld]
general_log = ON
general_log_file = /var/log/mysql/mysqld.log

slow_query_log = ON
long_query_time = 10.000000
slow_query_log_file = /var/log/mysql/mysql_slow_query.log
```

`/var/log/mysql` 目录是 MySQL (包括 Percona 等分支) 默认的日志输出路径, 有两种方式进行映射

#### 1.5.1. 映射到指定卷

可以创建一个卷用于存储日志

```yml
volumes:
  percona_log:
```

将这个卷和 `/var/log/mysql` 进行映射, 即可将日志写入指定的卷内

```yml
volumes:
  - percona_log:/var/log/mysql
```

这种方式不用关注权限问题, 但也方便日志查看和收集, 所以一般情况下可以采用和宿主机指定路径影射的方法

#### 1.5.2. 映射到宿主机指定目录

创建一个权限为 `777`, 用于存储 MySQL 日志的路径, 例如 `./log` 目录

```bash
mkdir -m 777 log
```

将此目录和容器的 `var/log/mysql` 进行映射

```yml
volumes:
  - ./log:/var/log/mysql
```

#### 1.5.3. 注意事项

由于 Percona 容器内部是通过 `mysql:mysql` 用户和组进行操作的, 而宿主机并无此用户, 所以宿主机上创建的路径并不能被容器直接访问, 需要将该目录的 Other 权限升至最高, 即 `777`

### 1.6. 创建健康检查用户

通过以下语句创建一个无权限且无密码, 只允许本地登录的用户, 用于进行服务的健康检查

```sql
CREATE USER 'health'@'localhost' IDENTIFIED WITH mysql_native_password BY '';
```

可以利用容器的 `/docker-entrypoint-initdb.d/` 路径执行初始化 SQL 脚本, 即将 [sql/initialize.sql](./sql/initialize.sql) 文件映射到 `/docker-entrypoint-initdb.d/` 路径下, 通过容器配置文件的 `volumes` 属性配置如下:

```yml
volumes:
  - ./sql/initialize.sql:/docker-entrypoint-initdb.d/initialize.sql:ro
```

之后就可以进行健康检查, 即通过容器配置文件的 `healthcheck` 属性配置如下:

```yml
healthcheck:
  test:
    [
      "CMD-SHELL",
      "mysqladmin ping -uhealth | grep -q 'alive'"
    ]
  interval: 30s
  timeout: 30s
  retries: 5
  start_period: 30s
```

## 2. 启动容器

按以上步骤配置完毕后, 既可启动容器, 可通过

```bash
docker-compose up
```

或

```bash
docker-compose up -d
```

启动容器 (后者为后台启动)

可通过

```bash
docker log <container-name>
```

来查看容器日志

可通过

```bash
docker-compose down
```

来停止容器

可通过

```bash
docker exec -it <container-name> mysql -uroot -p
```

通过容器内部的 MySQL Client 访问数据库实例, 也可以通过宿主机的 MySQL Client 进行访问, 即

```bash
mysql -h'127.0.01' -P3306 -uroot -p
```

其中, `-h` 参数是必须的, 因为宿主机上并不存在 Percona 的 sock 文件, 如果想省略 `-h` 参数, 需要将容器的 sock 文件映射到宿主机上 (必要性很小)

```yml
volume:
  - /var/run/mysqld/mysqld.sock:/var/run/mysqld/mysqld.sock:rw
```
