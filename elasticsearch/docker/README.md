# Docker 问题梳理

错误 `Failed to start Elasticsearch. Error opening log file '/gc.log': Permission denied`

```bash
$ chmod -R 755 ./logs ./plugins
```

---

错误 `Max virtual memory areas vm.max_map_count [65530] likely too low, increase to at least [262144]`

方法 1：

修改 `/etc/sysctl.conf` 文件，在文件末尾添加 `vm.max_map_count=262144`
执行 `$ sudo sysctl -p` 刷新配置

方法 2：

即时生效，重启失效

```bash
$ sudo sysctl -w vm.max_map_count=262144
```
