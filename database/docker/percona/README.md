# 相关 Docker 命令

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
