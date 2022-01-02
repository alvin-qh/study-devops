# Locust

- [Locust](#locust)
  - [1. 基本结构](#1-基本结构)
    - [1.1. 推荐目录结构](#11-推荐目录结构)
    - [1.2. 测试文件](#12-测试文件)
  - [2. 执行测试](#2-执行测试)
    - [2.1. 单机测试](#21-单机测试)
      - [2.1.1. 启动非 http 测试](#211-启动非-http-测试)
      - [2.1.2. 启动 http 测试](#212-启动-http-测试)
    - [2.2. 集群测试](#22-集群测试)
      - [2.2.1. 启动 master 节点](#221-启动-master-节点)
      - [2.2.2. 启动 slave 端](#222-启动-slave-端)
    - [2.3. 通过配置文件启动测试](#23-通过配置文件启动测试)

## 1. 基本结构

### 1.1. 推荐目录结构

```plain
<root>
 ├─ <common>
 │   ├─ __init__.py
 │   ├─ conf.py
 │   └─ auth.py
 └─ <locustfiles>
     ├─ test1.py
     └─ test2.py
```

### 1.2. 测试文件

[非 web 测试](./testcase/locustfiles/simple.py)

[web 测试](./testcase/locustfiles/web.py)

## 2. 执行测试

启动测试服务器

```bash
$ python app/app.py
```

### 2.1. 单机测试

#### 2.1.1. 启动非 http 测试

进入 `testcase` 路径

```bash
$ locust -f locustfiles/simple.py -P 8001
```

- `-f or --locustfile` 指定 Locust 测试文件
- `-P or --web-port` 指定 Web UI 的端口号

上述命令在 `8001` 端口启动 Locust 测试 UI，访问该地址可以对测试进行配置并启动

#### 2.1.2. 启动 http 测试

在 `8001` 端口启动 Locust 测试 UI，测试目标 web 地址为 `http://localhost:3000`

```bash
$ locust -f locustfiles/web.py -H http://localhost:3000 -P 8001
```

- `-H or --host` 指定要测试网站的 URL 地址

### 2.2. 集群测试

#### 2.2.1. 启动 master 节点

```bash
$ locust -f locustfiles/web.py -H http://localhost:3000 -P 8001 --master --master-port=5557
```

- `--master` 以 "主测试节点" 启动 Locust
- `--master-port` 指定 "测试主节点" 的端口号

#### 2.2.2. 启动 slave 端

```bash
$ locust -f locustfiles/web.py -H http://localhost:3000 --worker --master-host=localhost --master-port=5557
```

- `--worker` 以 "从测试节点" 启动 Locust
- `--master-port` 指定 "测试主节点" 的端口号


### 2.3. 通过配置文件启动测试

master 端，参见 [web.master.conf 配置文件](./testcase/web.master.conf)

```bash
$ locust --config web.master.conf
```

slave 端，参见 [web.worker.conf 配置文件](./testcase/web.worker.conf)

```bash
$ locust --config web.work.conf
```
