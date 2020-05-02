# ELK with Docker

## 1. Trouble Shooting

### 1.1. Elasticsearch

#### 1.1.1. Failed to start Elasticsearch. Error opening log file '/gc.log': Permission denied

```bash
$ chmod -R 755 ./logs ./plugins
```

#### 1.1.2. Max virtual memory areas vm.max_map_count [65530] likely too low, increase to at least [262144]

- Change `sysctl.conf`
  1. Edit `/etc/sysctl.conf` file, add `vm.max_map_count=262144` at end;
  2. Flush with `sudo sysctl -p`

- Or
    ```bash
    $ sudo sysctl -w vm.max_map_count=262144
    ```