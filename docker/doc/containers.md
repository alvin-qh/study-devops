# 容器

## 1. 容器操作

### 1.1. 创建容器

[`docker run`](https://docs.docker.com/engine/reference/commandline/run/)

```bash
$ docker run --name 'test' -it alpine ls /
```

使用 alpine 镜像创建一个名为 `test` 的容器，并在容器内执行 `ls /` 命令

`-it` 表示开启“交互式终端”，即将容器的输入输出定向到当前输入输出上

### 1.2. 启动和停止容器

[`docker start`](https://docs.docker.com/engine/reference/commandline/container_start/)

[`docker stop`](https://docs.docker.com/engine/reference/commandline/container_stop/)

```bash
$ docker start -a 'test'
```

启动名为 `test` 的容器，并将输入输出关联到宿主机的标准输入输出上

```bash
$ docker stop -t 10 'test'
```

停止一个容器，并在 10 秒后杀死相关进程

### 1.3. 重启容器

[`docker restart`](https://docs.docker.com/engine/reference/commandline/restart/)

[`docker container restart`](https://docs.docker.com/engine/reference/commandline/container_restart/)

```bash
$ docker restart 'test'
```

```bash
$ docker container restart 'test'
```

重启名为 `test` 的容器

### 1.4. 查看容器运行日志

[`docker logs`](https://docs.docker.com/engine/reference/commandline/logs/)

[`docker container restart`](https://docs.docker.com/engine/reference/commandline/container_logs/)

```bash
$ docker logs --tail 100 'test`
```

```bash
$ docker container logs --tail 100 'test`
```

输出名为 `test` 容器的后 100 行日志

### 1.5. 删除容器（保留镜像）

[`docker rm`](https://docs.docker.com/engine/reference/commandline/rm/)

[`docker container rm`](https://docs.docker.com/engine/reference/commandline/container_rm/)

```bash
$ docker rm -f 'test'
```

```bash
$ docker container rm -f 'test'
```

强制删除名为 `test` 的容器，如果没有 `-f` 参数，则容器必须在停止状态才能删除。

### 1.6. 后台运行

[`docker run`](https://docs.docker.com/engine/reference/commandline/run/)

```bash
$ docker run --rm --name 'test' -itd alpine /bin/sh -c "while true; do echo hello world; sleep 1; done"
```

启动 alpine 镜像的容器，容器命名为 `test`

`-it` 表示开启“交互式终端”，即将容器的输入输出定向到当前输入输出上

`-d` 表示后台启动（Daemon方式）

`--rm` 表示容器停止后自动删除

### 1.7. 附加到容器

[`docker attach`](https://docs.docker.com/engine/reference/commandline/attach/)

[`docker container attach`](https://docs.docker.com/engine/reference/commandline/container_attach/)

```bash
$ docker attach -it 'test'
```

```bash
$ docker container attach -it 'test'
```

附加到已执行的 `test` 容器上

### 1.8. 在运行容器中执行命令

```bash
$ docker exec -it 'test' ls /
```

在已运行的 `test` 容器中执行 `ls` 命令

### 1.9. 重命名容器

[`docker rename`](https://docs.docker.com/engine/reference/commandline/rename/)

[`docker container rename`](https://docs.docker.com/engine/reference/commandline/container_rename/)

```bash
$ docker rename 'test' 'test-alpha'
```

```bash
$ docker container rename 'test' 'test-alpha'
```

给容器重新命名

### 1.10. 杀死容器进程

[`docker kill`](https://docs.docker.com/engine/reference/commandline/kill/)

[`docker container kill`](https://docs.docker.com/engine/reference/commandline/container_kill/)

```bash
$ docker kill --signal=9 'test'
```

```bash
$ docker container kill --signal=9 'test'
```

### 1.11. 删除容器

[`docker rm`](https://docs.docker.com/engine/reference/commandline/rm/)

[`docker container rm`](https://docs.docker.com/engine/reference/commandline/container_rm/)

```bash
$ docker rm 'test'
```

```bash
$ docker container rm 'test'
```

## 2. 查看容器

### 2.1. 列举容器

[`docker container ls`](https://docs.docker.com/engine/reference/commandline/container_ls/)

[`docker ps`](https://docs.docker.com/engine/reference/commandline/ps/)

```bash
$ docker ps -as
```

```bash
$ docker container ls -as
```

列出容器

`-a` 表示列出所有容器，包括正在运行和已停止的，否则只列出正在运行的容器

`-s` 显示容器的磁盘使用情况

### 2.3. 查看容器详情

[`docker inspect`](https://docs.docker.com/engine/reference/commandline/inspect/)

[`docker container inspect`](https://docs.docker.com/engine/reference/commandline/container_inspect/)

```bash
$ docker inspect -s 'test'
```

```bash
$ docker container inspect -s 'test'
```

查看 `test` 容器的详情

### 2.4. 过滤器和格式化

#### 2.4.1. 过滤器

对于 `docker ps` 和 `docker ls` 等命令支持过滤器（filter），可以过滤出符合条件的结果，例如：

```bash
$ docker ps -f "name=test" -f "status=exited"
```

根据容器的“名称”和“状态”过滤结果

### 2.4.2. 结果格式化

可以定义 `docker ps`，`docker ls` 和 `docker inspect` 等命令的结果格式，例如：

```bash
$ docker inspect -s -f '{{.Id}}{{", "}}{{.Name}}' 'test'
```

显示容器的详情，并输出 ID 和 Name 两个字段
