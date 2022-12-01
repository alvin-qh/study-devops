# 构建 Docker 镜像

## 1. 构建

```bash
docker build -t study/hello:1.0 .
```

## 2. 删除无效镜像

```bash
docker image prune -f
```

## 3. 显示构建结果

```bash
docker image ls -a -f "reference=study/hello:*"
```

## 4. 启动镜像

```bash
docker run --rm --name hello study/hello:1.0
```

- `--rm` 停止后自动删除容器

## 5. 设置环境变量

```bash
docker run --rm --name hello --env "USER_NAME=lucy" study/hello:1.0
```

## 6. 删除镜像

```bash
docker rmi -f study/hello:1.0 alpine:latest
```
