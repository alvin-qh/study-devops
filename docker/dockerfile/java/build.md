# 构建 Docker 镜像

## 1. 构建

```bash
$ docker build -t study/java:1.0 .
```

## 2. 删除无效镜像

```bash
$ docker image prune -f
```

## 3. 显示构建结果

```bash
$ docker image ls -f "reference=study/java:*"
```

## 4. 启动镜像

```bash
$ mkdir -p data
$ mkdir -p logs
$ docker run --name 'java' -itd --rm -v $(pwd)/logs:/logs -v $(pwd)/data:/data -p 8080:8080 study/java:1.0
```

- `--rm` 停止后自动删除容器

## 5. 显示容器日志

```bash
$ docker logs 'java'
```

## 6. 停止容器

```bash
$ docker stop 'java'
```

## 7. 使用 `docker-compose`

```bash
$ docker-compose up -d # 启动容器
$ docker-compose down # 停止容器
```

## 8. 删除镜像

```bash
$ docker rmi -f study/java:1.0 node:latest openjdk:8
```

## 9. 删除卷

```bash
$ docker volume rm -f $(pwd)/logs $(pwd)/data
```
