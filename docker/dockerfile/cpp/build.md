# 构建 Docker 镜像

## 1. 构建

```bash
docker build -t study/cpp:1.0 .
```

## 2. 删除无效镜像

```bash
docker image prune -f
```

## 3. 显示构建结果

```bash
docker image ls -a -f "reference=study/cpp:*"
```

## 4. 启动镜像

```bash
docker run --name 'cpp' --rm study/cpp:1.0
```

- `--rm` 停止后自动删除容器

## 5. 设置环境变量

```bash
docker rmi -f study/cpp:1.0 spritsail/alpine-cmake
```

## 6. 删除镜像

```bash
docker rmi -f study/cpp:1.0 spritsail/alpine-cmake
```
