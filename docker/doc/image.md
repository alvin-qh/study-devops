# 镜像

## 1. 镜像操作

### 1.1. 拉取镜像

[`docker pull`](https://docs.docker.com/engine/reference/commandline/pull/)

```bash
docker pull hello-world:latest
```

拉去名为`hello-world`的镜像（最新版本）

### 1.2. 列举本机所有镜像

[`docker image ls`](https://docs.docker.com/engine/reference/commandline/image_ls/)

```bash
docker image ls
```

### 1.3. 删除镜像

[`docker image rm`](https://docs.docker.com/engine/reference/commandline/image_rm/)

[`docker rmi`](https://docs.docker.com/engine/reference/commandline/rmi/)

```bash
docker rmi -f hello-world
```

删除名为`hello-world`的镜像

`-f` 强制删除镜像，同时删除和该镜像相关的容器

### 1.4. 清理镜像

[`docker image prune`](https://docs.docker.com/engine/reference/commandline/image_prune/)

```bash
docker image prune -f
```

删除所有未被引用的镜像（即未产生容器的镜像）

```bash
docker image prune -af
```

清理所有无用的镜像（中间过程镜像或未被引用的镜像）
