# Percona 容器

## 1. 初始化

### 1.1. 创建日志目录

为单实例容器设置日志路径

```bash
mkdir standalone/log
chmod 777 standalone/log
```

为主从实例容器设置日志路径

```bash
mkdir cluster-ms/log
chmod 777 cluster-ms/log
```

## 2. 相关 Docker 命令

删除主库卷文件

```bash
docker volume rm $(docker volume ls -q | grep percona_master) -f
```

删除从库卷文件

```bash
docker volume rm $(docker volume ls -q | grep percona_slave) -f
```

删除所有卷文件

```bash
docker volume rm $(docker volume ls -q | grep percona) -f
```

访问主库

```bash
docker exec -it percona-master mysql -u root -p
```

访问从库

```bash
docker exec -it percona-slave mysql -u root -p
```
