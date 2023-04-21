# Prometheus

## 1. 配置

### 1.1. Prometheus

#### 1.1.1. 设置服务发现

Prometheus 通过服务发现获取到目标服务, 并从目标服务上读取监控数据

服务发现在 `prometheus.yml` 配置文件的 `scrape_configs` 配置项中进行配置

服务发现主要配置目标服务的 **"URL", "标签"以及"认证方式"**

可以通过 `static_configs` 在同一个配置文件中配置服务发现, 也可以通过 `file_sd_configs` 将服务发现配置放在另外的 `json` 文件中, 参见 [prometheus.yml](./docker/conf/prometheus.yml) 文件

1. 通过 `static_configs` 进行配置

    ```yml
    scrape_configs:
      - job_name: prometheus
        static_configs:
          - targets: # 目标服务 URL 列表
              - localhost:9090
            labels: # 服务标签
              instance: prometheus
    ```

2. 通过 `file_sd_configs` 进行配置

    ```yml
    scrape_configs:
      - job_name: node-exporter
        file_sd_configs:
          - files: # 指定服务发现配置文件
              - targets/node_exporter_sd.json
            refresh_interval: 1m # 服务发现文件刷新周期
    ```

    而定义服务发现的 `json` 配置文件可以为

    ```json
    [
      {
        "targets": [
          "node_exporter:9100"
        ],
        "labels": {
          "instance": "Local Host"
        }
      }
    ]
    ```

> `job_name`+`labels` 共同组成了每个服务监控的唯一标识, 用于进行筛选

### 1.2. Grafana

#### 1.2.1. Grafana 容器配置文件

参考官网: [configure-docker](https://grafana.com/docs/grafana/latest/setup-grafana/configure-docker/)

容器路径 `/etc/grafana/grafana.ini` 文件中为 Grafana 配置信息

可以通过映射这个文件将配置文件放在宿主机上, 参考: [grafana.ini](./docker/conf/grafana.ini) 文件

```yml
volumes:
  - ../conf/grafana.ini:/etc/grafana/grafana.ini:ro
```

更常用的方式是通过系统变量修改 Grafana 配置

#### 1.2.2. Grafana 容器环境变量

通过设置容器的环境变量文件或环境变量值可以覆盖 `grafana.ini` 中的配置项

```yml
environment:
  - GF_DEFAULT_INSTANCE_NAME=Grafana
  - ...
```

或者

```yml
env_file:
  - ../env/grafana.env
```

在 [grafana.env](./docker/env/grafana.env) 文件中设置环境变量值

通过 `GF_<段名称>_<配置项名称> = <配置项值>` (字母为大写) 格式可以覆盖 `grafana.ini` 文件中的对应配置项, 例如:

配置项

```ini
[security]
admin_user=admin
```

对应的环境变量为

```ini
GF_SECURITY_ADMIN_USER=admin
```

> 注意: `GF_SECURITY_ADMIN_USER` 和 `GF_SECURITY_ADMIN_PASSWORD` 这两个配置项尽在第一次启动容器时有效, 一旦登录过系统, 用户名密码就会写入数据库

#### 1.2.3. Grafana 常用路径配置

配置项中部分配置用于对 Grafana 常用路径进行配置, 包括:

| 配置项                   | 默认值                       |
|:------------------------|:----------------------------|
| `GF_PATHS_CONFIG`       | `/etc/grafana/grafana.ini`  |
| `GF_PATHS_DATA`         | `/var/lib/grafana`          |
| `GF_PATHS_HOME`         | `/usr/share/grafana`        |
| `GF_PATHS_LOGS`         | `/var/log/grafana`          |
| `GF_PATHS_PLUGINS`      | `/var/lib/grafana/plugins`  |
| `GF_PATHS_PROVISIONING` | `/etc/grafana/provisioning` |

#### 1.2.4. 使用 MySQL 作为持久化数据库

在 Docker 中添加 Percona 容器 (或其它 MySQL 容器)

在容器的初始化脚步中增加创建数据库以及用户等内容, 参见 [grafana.sql](/docker/sql/grafana.sql) 文件内容

设置 Grafana 配置, 以设置 MySQL 数据库信息 (这里通过环境变量进行配置, 参见 [grafana.env](./docker/env/grafana.env) 文件)

#### 1.2.5. 配置数据源

1. 点击"设置", 进入"数据源设置"界面
    ![*](assets/datasource-1.png)

2. 选择"Prometheus"项目进行配置
    ![*](assets/datasource-2.png)

3. 填入"URL", 即 Prometheus 服务地址, 除此之外, 界面中的其它项均为可选项, 点击"保存 & 测试"按钮即可

#### 1.2.6. 配置 Dashboard

1. 点击"Dashboards", 进入"仪表盘"设置界面
    ![*](assets/dashboard-1.png)

    一般情况下, 不会从头去建立一个仪表盘, 而是从 `https://grafana.com/grafana/dashboards` 地址导入模板, 所以这里选"新建">"导入"菜单

2. 导入"仪表盘模板"
    ![*](assets/dashboard-2.png)
    在"从 grafana.com 导入"文本框中填入模板页面地址或者模板 ID, 并设置数据源, 即可利用该模板创建仪表盘

> 导入的仪表盘可以进一步进行编辑, 并进行命名, 分组等操作

## 2. 导出和导入仪表盘

修改后的仪表盘可以导出为 JSON 格式, 以方便之后恢复仪表盘或将仪表盘复制到另一个 Grafana 实例上

### 2.1. 导出仪表盘

1. 进入指定的仪表盘, 点击仪表盘设置
    ![*](assets/dashboard-3.png)

2. 点击 "JSON Model" 菜单, 将呈现出的 JSON 内容复制保存即可
    ![*](assets/dashboard-4.png)

### 2.2. 导入仪表盘

1. 在"仪表盘主页", 点击"导入"
    ![*](assets/dashboard-5.png)

2. 在"导入"界面, 填入之前保存的 JSON 文本, 点击"读取", 即可将之前保存的仪表盘导入
    ![*](assets/dashboard-6.png)

### 2.3. 完善导出的 JSON

导出的 JSON 可以在当前 Grafana 正确导入, 但无法直接导入到另一个 Grafana 实例中, 主要是另一个 Grafana 实例的 "Datasource" 和当前实例不同 (Datasource 的 `uid` 不一样, 所以导入后需要手动逐面板重新设置数据源, 非常麻烦), 所以需要对导出的 JSON 做适当修改, 使其能够支持任意设置数据源

1. 为仪表盘 JSON 加入 `__input` 字段, 使其可以支持导入时选择数据源, 并以名为 `DS_PROMETHEUS` 的变量来代表实际的数据源

    ```json
    "__inputs": [
      {
        "type": "datasource",
        "name": "DS_PROMETHEUS",
        "label": "prometheus",
        "description": "-",
        "pluginId": "prometheus",
        "pluginName": "Prometheus"
      }
    ]
    ```

    这段脚本可以增加在仪表盘 JSON 的任意位置 (例如最开头), 表示在导入仪表盘时, 可以选择一个已有的 Prometheus 作为数据源

2. 将仪表盘 JSON 中, 所有 `targets` 字段下的 `datasource` 字段修改为 `${DS_PROMETHEUS}`

    ```json
    "targets": [
      {
        "datasource": "${DS_PROMETHEUS}",
        // ...
      }
    ],
    ```

    至此, 导入的仪表盘即可绑定一个指定的 Prometheus 数据源

3. 可以在 `templating` 字段下增加一个数据源项, 即可在仪表盘导入后, 仍可以通过下拉框切换数据源

    ```json
    "templating": {
      "list": [
        {
          "datasource": "Prometheus",
          "description": null,
          "error": null,
          "hide": 0,
          "includeAll": false,
          "label": "datasource",
          "multi": false,
          "name": "DS_PROMETHEUS",
          "options": [],
          "query": "prometheus",
          "refresh": 1,
          "regex": "",
          "skipUrlSync": false,
          "type": "datasource"
        },
        // ...
      ]
    }
    ```

    注意, 这里的 `name` 字段定义的变量名需要和 JSON 中 `datasource` 字段使用的变量名一致, 这里应为 `${DS_PROMETHEUS}`

## 3. 常用监控配置

### 3.1. 监控 Prometheus 自身

1. **Prometheus 配置**

   Prometheus 自身即可报告监控数据, 可通过执行 `curl http://localhost:9090/metrics` 来进行测试

   在服务发现配置中, 将 `targets` 设置为 Prometheus 服务地址 (无需 `/metrics` 后缀), 添加 `labels` 配置即可, 参见 [prometheus.yml](./docker/conf/prometheus.yml) 中 `job_name: prometheus` 部分

2. **Grafana 仪表盘**

   - 使用 `https://grafana.com/grafana/dashboards/3662-prometheus-2-0-overview/` 仪表盘, ID 为 `3662` (推荐)

### 3.2. 监控宿主机

1. **`node-exporter` 容器配置**

    要在容器内获取到宿主机的状态, 需要将监控路径由 `/` 切换 (例如 `/host`), 再将宿主机的 `/` 路径挂载到容器的 `/host` 路径下 (只读)

    所以需要为 `node-exporter` 容器增加命令行参数 `--path.rootfs=/host`, 并且将宿主机的 `/` 挂载到容器的 `/host` 下

    ```yml
    volumes:
      - /:/host:ro
    ```

    参见 [docker-compose.yml](./docker/standalone/docker-compose.yml) 文件中的 `node-exporter` 部分

    可通过 `docker exec node_exporter wget -qO- localhost:9100/metrics` 命令进行测试

2. **Prometheus 配置**

    在服务发现配置中, 将 `targets` 设置为 `node-exporter` 服务地址, 添加 `labels` 配置即可

    参见 [prometheus.yml](./docker/conf/prometheus.yml) 文件的 `job_name: node-exporter` 部分以及 [targets/node_exporter_sd.json](./docker/conf/targets/node_exporter_sd.json) 文件内容

3. **Grafana 仪表盘**

    - 使用 `https://grafana.com/grafana/dashboards/1860-node-exporter-full/` 仪表盘, ID 为 `1860` (推荐)

### 3.3. 监控 MySQL

1. **`mysqld-exporter` 容器配置**

    需要在容器的环境变量中设置目标 MySQL 的访问地址, 以及用户名密码

    ```yml
    environment:
      - DATA_SOURCE_NAME=<username>:<password>@(url)/
    ```

    > 注意, 数据库连接串末尾必须具备 `/` 字符

    参见 [docker-compose.yml](./docker/standalone/docker-compose.yml) 文件中的 `mysqld-exporter` 部分

    可通过 `docker exec mysqld_exporter wget -qO- localhost:9104/metrics` 命令进行测试

2. **Prometheus 配置**

    在服务发现配置中, 将 `targets` 设置为 `mysqld-exporter` 服务地址, 添加 `labels` 配置即可

    参见 [prometheus.yml](./docker/conf/prometheus.yml) 文件的 `job_name: mysqld-exporter` 部分以及 [targets/mysqld_exporter_sd.json](./docker/conf/targets/mysqld_exporter_sd.json) 文件内容

3. **Grafana 仪表盘**

    - 使用 `https://grafana.com/grafana/dashboards/14031-mysql-dashboard/` 仪表盘, ID 为 `14031` (推荐)

### 3.4. 监控容器

1. **`cadvisor` 容器配置**

    使用 `cadvisor` 需要将宿主机的若干路径进行映射 (`volumes`), 并且赋予容器管理权限 (`privileged`), 参见 [docker-compose.yml](./docker/standalone/docker-compose.yml) 文件中的 `cadvisor` 部分

    可通过 `docker exec cadvisor wget -qO- localhost:8080/metrics` 命令进行测试

2. **Prometheus 配置**

    在服务发现配置中, 将 `targets` 设置为 `cadvisor` 服务地址, 添加 `labels` 配置即可

    参见 [prometheus.yml](./docker/conf/prometheus.yml) 文件的 `job_name: cadvisor` 部分以及 [targets/cadvisor_sd.json](./docker/conf/targets/cadvisor_sd.json) 文件内容

3. **Grafana 仪表盘**

    - 使用 `https://grafana.com/grafana/dashboards/179-docker-prometheus-monitoring/` 仪表盘, ID 为 `179` (推荐)
    - 使用 `https://grafana.com/grafana/dashboards/11600-docker-container/` 仪表盘, ID 为 `11600`
