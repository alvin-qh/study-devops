# PostgreSQL

- [PostgreSQL](#postgresql)
  - [1. 使用 Docker 镜像](#1-使用-docker-镜像)
    - [1.1. 下载镜像](#11-下载镜像)
    - [1.2. 启动容器](#12-启动容器)
    - [1.3. 使用 psql 命令解释器](#13-使用-psql-命令解释器)
    - [1.4. 使用 docker-compose (或 docker stack deploy)](#14-使用-docker-compose-或-docker-stack-deploy)
  - [2. 配置容器](#2-配置容器)
    - [2.1. 环境变量](#21-环境变量)
    - [2.2. 使用密码文件](#22-使用密码文件)
    - [2.3. 配置文件](#23-配置文件)
      - [挂载配置文件的注意点](#挂载配置文件的注意点)
    - [2.4. 本地化](#24-本地化)
    - [2.5. 使用环境变量](#25-使用环境变量)
  - [3. 归档 WAL 日志](#3-归档-wal-日志)
    - [3.1. 挂载归档路径](#31-挂载归档路径)
    - [3.2. 配置归档脚本](#32-配置归档脚本)
    - [3.3. 重启容器](#33-重启容器)

## 1. 使用 Docker 镜像

### 1.1. 下载镜像

```bash
docker pull postgres[:tag]
```

### 1.2. 启动容器

```bash
docker run -d \
  --name <some-name> \
  --network <some-network> \
  -e POSTGRES_PASSWORD=mysecretpassword \
  postgres[:tag]
```

上述命令会通过 `postgres[:tag]` 镜像启动一个容器, 且默认的用户和数据库会通过容器入口进行初始化

### 1.3. 使用 psql 命令解释器

PostgreSQL 容器启动后, 可以通过如下命令启动命令行解释器并连接启动的 PostgreSQL 实例

可以重新创建一个容器, 执行 `psql` 进程

```bash
docker run -it --rm --network <some-network> postgres psql -h <host> -U postgres
```

注意, `psql` 容器要和 `postgresql` 容器在同一个网络下

或者在 [1.2. 启动容器](#12-启动容器) 中已经启动的容器中执行命令

```bash
docker exec -it <some-name> psql -U postgres
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

完整的 docker-compose 配置参见 [docker-compose.yml](./docker-compose.yml) 文件

在 `docker-compose.yml` 中定义配置文件的挂载时需要注意路径问题, 参见 [挂载配置文件的注意点](#挂载配置文件的注意点) 章节

## 2. 配置容器

### 2.1. 环境变量

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

上述环境变量并不是 Docker 专用, 使用 PostgreSQL 二进制程序时也能适用

### 2.2. 使用密码文件

某些环境变量可以通过在变量名后增加 `_FILE` 后缀来从文件中读取变量值, 例如 `POSTGRES_PASSWORD_FILE` 环境变量

特别的, 可以通过 [Docker Secret](https://docs.docker.com/compose/use-secrets/) 来保证这些环境变量文件的安全性, 即通过 `/run/secrets/<secret_name>` 文件, 例如:

```bash
docker run
  --name some-postgres
  -e POSTGRES_PASSWORD_FILE=/run/secrets/postgres-passwd
  -d postgres
```

目前, 仅 `POSTGRES_INITDB_ARGS`, `POSTGRES_PASSWORD`, `POSTGRES_USER` 和 `POSTGRES_DB` 这几个环境变量支持从文件中取值

### 2.3. 配置文件

对于配置项较为复杂的情况, 可以为容器挂载数据库的配置文件

1. 获取默认配置文件:

   ```bash
   docker run -i --rm postgres cat /usr/share/postgresql/postgresql.conf.sample > postgres.conf
   ```

   则在宿主机当前路径下, 会创建 `postgres.conf` 配置文件, 其中配置项为默认配置

2. 修改配置文件, 并挂载到容器中:

   ```bash
   docker run -d \
     --name some-postgres \
     -v "$PWD/postgres.conf":/etc/postgresql/postgresql.conf \
     -e POSTGRES_PASSWORD=mysecretpassword postgres \
     -c 'config_file=/etc/postgresql/postgresql.conf' \
     -c shared_buffers=256MB \
     -c max_connections=200
   ```

   其中, `-c` 参数可以在命令行中覆盖指定的配置项, 在 `docker-compose.yml` 文件中则为

   ```yml
   command: >
     -c "config_file=/etc/postgresql/postgresql.conf"
     -c "shared_buffers=256MB"
     -c "max_connections=200"
   ```

#### 挂载配置文件的注意点

- 容器默认的配置文件 `postgresql.conf` 位于 `/var/lib/postgresql/data` 目录下, 即 `/var/lib/postgresql/data/postgresql.conf`; 同时容器的其它配置文件, 例如: `pg_hba.conf` 也位于 `/var/lib/postgresql/data` 目录下;

- 如果希望从宿主机直接映射这些配置文件, 例如:

  ```yml
  volumes:
    - ./conf/postgres.conf:/var/lib/postgresql/data/postgresql.conf
  ```

  这会导致 `/var/lib/postgresql/data` 路径同时被映射, 则第一次启动容器后, 容器的初始化脚本误认为 PostgreSQL 的数据目录已经创建, 导致初始化失败;

- 解决方法 1:

  1. 将配置文件映射到其它位置, 例如 `/etc/postgresql/postgresql.conf`, 即:

     ```yml
     volumes:
       - ./conf/postgres.conf:/etc/postgresql/postgresql.conf
     ```

  2. 在启动容器时, 增加 `-c` 参数来指定配置文件的实际地址, 即:

     ```yml
     command: >
       -c "config_file=/etc/postgresql/postgresql.conf"
     ```

  3. 其它需要的配置文件的也映射到 `/etc/postgresql/` 路径下, 并在 `postgresql.conf` 中重新指定其位置;

- 解决方法 2:

  1. 继续使用默认配置文件;

  2. 配置文件中需要修改的项目, 全部通过在容器启动时增加 `-c` 参数来配置, 例如:

     ```yml
     command: >
       -c "archive_mode=on"
       -c "archive_command='bash /etc/postgresql/archive.sh %f %p'"
     ```

### 2.4. 本地化

如果需要修改镜像的本地化设置, 可以以官方 postgres 镜像为基础重新打包镜像, 例如

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

### 2.5. 使用环境变量

对于 docker-compose 来说, 可以有几种方式使用环境变量

- 通过 `docker-compose.yml` 文件的 `services.**.environment` 进行设置
- 通过 `docker-compose.yml` 文件的 `services.**.env_file` 进行设置
- 通过 `docker-compose.yml` 文件同一目录下的 `.env` 文件进行设置
- 通过 `docker-compose` 命令的 `--env-file` 参数设置

注意, 只有后两种方式才能在 `docker-compose.yml` 文件中通过 `${NAME}` 语法来引用环境变量的值, 最后一种方式的命令如下:

```bash
docker-compose --env-file ../env/pgsql.env up
```

## 3. 归档 WAL 日志

WAL 日志的归档流程参见 [WAL 日志](../../doc/WAL.md) 章节, 本章节只阐述在容器中如何操作

本例中, 将容器的 `/var/lib/postgresql/archive` 目录用作归档目录, 且映射一个新的卷 `pgsql_archive` 到这个目录, 用于归档文件的持久化存储, 步骤如下:

### 3.1. 挂载归档路径

首先停止容器, 在 `docker-compose.yml` 配置文件中加入归档目录 `/var/lib/postgresql/archive` 的挂载, 例如:

```yml
volumes:
  ...
  # 映射 WAL 归档路径
  - pgsql_archive:/var/lib/postgresql/archive
```

### 3.2. 配置归档脚本

修改 [conf/postgres.conf](./conf/postgres.conf) 配置文件, 修改如下配置:

1. `wal_level = replica` 及 `archive_mode=on` 配置: 参见 [归档配置](../../doc/WAL.md#32-归档配置) 相关章节;

2. `archive_command` 配置 (即归档操作配置): 由于调用了 [script/archive.sh](./script/archive.sh), 所以需要将 [script/archive.sh](./script/archive.sh) 文件中表示归档目录的 `arch_dir` 变量改为 `/var/lib/postgresql/archive`;

3. `restore_command` 配置 (即归档恢复操作配置): 配置为 `cp /var/lib/postgresql/archive/%f %p` 进行归档目录下文件恢复到 `/var/lib/postgres/data/pg_wal` 目录下;

### 3.3. 重启容器

使用 `docker-compose --env-file ../env/pgsql.env up` 命令重新启动容器, 此时会报告错误, 表示 `/var/lib/postgresql/archive` 无权限访问, 原因为默认挂载的 `/var/lib/postgresql/archive` 目录权限为 `root:root`, 而 PostgreSQL 需要所有的路径和文件权限为 `postgres:postgres`, 所以通过如下命令修改路径所有权即可

```bash
docker exec -u root postgres chown postgres:postgres /var/lib/postgresql/archive
```
