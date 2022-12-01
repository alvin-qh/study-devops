# 构建 Docker 镜像

## 1. 构建

```bash
docker build -t study/web:1.0 .
```

## 2. 删除无效镜像

```bash
docker image prune -f
```

## 3. 显示构建结果

```bash
docker image ls -f "reference=study/web:*"
```

## 4. 启动镜像

```bash
mkdir -p logs
docker run -itd --name nginx --rm -p80:80 -v $(pwd)/logs:/logs study/web:1.0
```

- `--rm` 停止后自动删除容器

## 5. 显示容器日志

```bash
docker logs nginx
```

## 6. 停止容器

```bash
docker stop nginx
```

## 7. 使用 `docker-compose`

```bash
docker-compose up -d # 启动容器
docker-compose down # 停止容器
```

## 8. 删除镜像

```bash
docker rmi -f study/web:1.0 nginx:latest node:latest
```

## 9. 删除卷

```bash
docker volume rm -f $(pwd)/logs
```
