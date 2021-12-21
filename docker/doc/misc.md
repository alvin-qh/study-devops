# 其它

## Docker on macOS

### 1. 连接到虚拟机

在 macOS 上，docker 是通过一个虚拟机承载 Linux 内核的。macOS 并不作为直接的宿主机（Host），所以有时候要进入虚拟机内，进行一些操作

```bash
$ screen ~/Library/Containers/com.docker.docker/Data/vms/0/tty
```

将屏幕连接到 Docker 虚拟机的 tty 上，之后：

- 按 `Enter` 键启动 TTL
- 按 `CTRL` + `A` + `D` 离开 TTL，但不杀死进程
- 按 `CTRL` + `A` + `K` 离开 TTL 并杀死进程

如果离开了 TTL（但没有杀死进程），则可以回到 TTL

```bash
$ screen -dr
```

进入 Docker 卷存储目录

```bash
$ ls /var/lib/docker/volume
```
