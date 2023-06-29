# Docker Image

- [Docker Image](#docker-image)
  - [1. 使用 Docker 镜像](#1-使用-docker-镜像)
    - [1.1. 下载镜像](#11-下载镜像)
    - [1.2. 启动容器](#12-启动容器)
    - [1.3. 使用 psql 命令解释器](#13-使用-psql-命令解释器)
    - [1.4. 使用 docker-compose (或 docker stack deploy)](#14-使用-docker-compose-或-docker-stack-deploy)
  - [2. 环境变量](#2-环境变量)
  - [3. 使用密码文件](#3-使用密码文件)
  - [4. 数据库初始化脚本](#4-数据库初始化脚本)
  - [5. 数据库配置文件](#5-数据库配置文件)
  - [6. 本地化](#6-本地化)
  - [7. 使用环境变量](#7-使用环境变量)

## 1. 使用 Docker 镜像

### 1.1. 下载镜像

```bash
docker pull postgres:<tag>
```

### 1.2. 启动容器

```bash
docker run \
  --name some-postgres \
  -e POSTGRES_PASSWORD=mysecretpassword \
  -d postgres
```

- 默认的用户和数据库会通过容器入口进行初始化

### 1.3. 使用 psql 命令解释器

```bash
docker run -it --rm --network some-network postgres psql -h some-postgres -U postgres

psql (14.3)
Type "help" for help.

postgres=# SELECT 1
 ?column?
----------
        1
(1 row)
```

### 1.4. 使用 docker-compose (或 docker stack deploy)

```yml
# Use postgres/example user/password credentials

version: '3.9'
services:
  db:
    image: postgres
    restart: always
    environment:
      POSTGRES_PASSWORD: example

  adminer:
    image: adminer
    restart: always
    ports:
      - 8080:8080
```

通过 `docker stack deploy -c stack.yml postgres` (或者 `docker-compose -f stack.yml up`) 来启动容器, 通过访问 <http://localhost:8080> 访问 adminer

## 2. 环境变量

PostgreSQL 镜像通过一系列环境变量对容器进行配置, 但除了 `POSTGRES_PASSWORD` 环境变量外, 其余都是可选项

- `POSTGRES_PASSWORD`: 该环境变量为必备项, 用于设置超级用户的密码; 默认的超级用户通过 `POSTGRES_USER` 环境变量来指定;

- `POSTGRES_USER`: 可选项, 用于设置数据库默认的超级用户, 如果此环境变量缺省, 则超级用户为 `postgres`;

- `POSTGRES_DB`: 可选项, 设置登录用户默认访问的数据库, 如果此环境变量缺省, 则登录用户默认数据库为 `postgres`;

- `POSTGRES_INITDB_ARGS`: 可选项, 用于在初始化数据库时设置额外的参数, 例如为数据库添加数据页校验:

  ```bash
  docker run \
  --name some-postgres \
  -e POSTGRES_PASSWORD=mysecretpassword \
  -e POSTGRES_INITDB_ARGS="--data-checksums"
  -d postgres
  ```

- `POSTGRES_INITDB_WALDIR`: 可选项, 用于指定数据库事务日志的存储位置. 默认情况下, 数据库的事务日志和数据库数据文件在同一处存储;

- `POSTGRES_HOST_AUTH_METHOD`, 可选项, 用于指定用户验证方式. 默认为 `scram-sha-256` 方式, 也可以为 `md5`;

  如果将 `POSTGRES_HOST_AUTH_METHOD` 环境变量设置为 `trust`, 则 `POSTGRES_PASSWORD` 环境变量可以不用设置;

  如果设置了 `POSTGRES_HOST_AUTH_METHOD` 环境变量 (例如 `-e POSTGRES_INITDB_ARGS=scram-sha-256`), 则需要同时指定 `POSTGRES_INITDB_ARGS` 环境变量 (即 `-e POSTGRES_INITDB_ARGS=--auth-host=scram-sha-256`)

- `PGDATA`: 可选项, 用于指定数据库文件的存储路径, 默认为 `/var/lib/postgresql/data`, 例如:

  ```bash
  docker run -d \
    --name some-postgres \
    -e POSTGRES_PASSWORD=mysecretpassword \
    -e PGDATA=/var/lib/postgresql/data/pgdata \
    -v /custom/mount:/var/lib/postgresql/data \
    postgres
  ```

上述环境变量并不是 Docker 专用, 使用 PostgreSQL 二级制程序时也能适用

## 3. 使用密码文件

某些环境变量可以通过在变量名后增加 `_FILE` 后缀来从文件中读取变量值, 例如 `POSTGRES_PASSWORD_FILE` 环境变量

特别的, 可以通过 Docker Secrets 来保证这些环境变量文件的安全性, 即通过 `/run/secrets/<secret_name>` 文件, 例如:

```bash
docker run
  --name some-postgres
  -e POSTGRES_PASSWORD_FILE=/run/secrets/postgres-passwd
  -d postgres
```

目前, 仅 `POSTGRES_INITDB_ARGS`, `POSTGRES_PASSWORD`, `POSTGRES_USER` 和 `POSTGRES_DB` 这几个环境变量支持从文件中取值

## 4. 数据库初始化脚本

当容器第一次启动时, 容器的 `/docker-entrypoint-initdb.d` 路径下的所有 `*.sql` 和 `*.sh` 脚本会被执行, 用于对数据库的初始化, 将初始化脚本挂载到此路径下即可, 例如:

假设为容器挂载了 `/docker-entrypoint-initdb.d/init-user-db.sh` 文件, 内容如下

```bash
#!/usr/bin/env bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  CREATE USER docker;
  CREATE DATABASE docker;
  GRANT ALL PRIVILEGES ON DATABASE docker TO docker;
EOSQL
```

## 5. 数据库配置文件

对于配置项较为复杂的情况, 可以为容器挂载数据库的配置文件

1. 获取默认配置文件

   ```bash
   docker run -i --rm postgres cat /usr/share/postgresql/postgresql.conf.sample > postgres.conf
   ```

   则在宿主机当前路径下, 会创建 `postgres.conf` 配置文件, 其中配置项为默认配置

2. 修改配置文件, 并挂载到容器中

   ```bash
   docker run -d \
     --name some-postgres \
     -v "$PWD/postgres.conf":/etc/postgresql/postgresql.conf:ro \
     -e POSTGRES_PASSWORD=mysecretpassword postgres \
     -c 'config_file=/etc/postgresql/postgresql.conf' \
     -c shared_buffers=256MB \
     -c max_connections=200
   ```

   - `-c` 参数可以在命令行中覆盖配置文件中的配置项

## 6. 本地化

如果需要修改镜像的本地化设置, 可以以 postgres 镜像为基础, 重新打包镜像, 例如

```dockerfile
FROM postgres:14.3
RUN localedef -i de_DE -c -f UTF-8 -A /usr/share/locale/locale.alias de_DE.UTF-8
ENV LANG de_DE.utf8
```

从 PostgreSQL 15 开始, 可以设置 ICU Locales, 通过 `POSTGRES_INITDB_ARGS` 环境变量在初始化数据库文件时 (即第一次启动容器时) 设置本地化

```bash
docker run -d \
  -e LANG=de_DE.utf8 \
  -e POSTGRES_INITDB_ARGS="--locale-provider=icu --icu-locale=de-DE" \
  -e POSTGRES_PASSWORD=mysecretpassword
  postgres:15-alpine
```

## 7. 使用环境变量

对于 docker-compose 来说, 可以有几种方式使用环境变量

- 通过 `docker-compose.yml` 文件的 `services.*.environment` 进行设置
- 通过 `docker-compose.yml` 文件的 `services.*.env_file` 进行设置
- 通过 `docker-compose.yml` 文件同一目录下的 `.env` 文件进行设置
- 通过 `docker-compose` 命令的 `--env-file` 参数设置

注意, 只有后两种方式才能在 `docker-compose.yml` 文件中通过 `${NAME}` 语法来引用环境变量的值, 最后一种方式的命令如下:

```bash
docker-compose --env-file ../env/pgsql.env up
```
