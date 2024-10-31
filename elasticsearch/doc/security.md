# 安全设置

- [安全设置](#安全设置)
  - [1. 安全配置](#1-安全配置)
    - [1.1. 配置用户名密码](#11-配置用户名密码)
      - [1.1.1. Elasticsearch 配置](#111-elasticsearch-配置)
        - [XPack 配置](#xpack-配置)
        - [生成用户名密码](#生成用户名密码)
      - [1.1.2. Kibana 配置](#112-kibana-配置)
        - [直接配置用户名密码](#直接配置用户名密码)
        - [使用 Kibana 密码管理器](#使用-kibana-密码管理器)
    - [1.2. 修改密码](#12-修改密码)
    - [1.3. 使用 TLS](#13-使用-tls)
      - [生成证书](#生成证书)
      - [使用证书](#使用证书)

## 1. 安全配置

### 1.1. 配置用户名密码

#### 1.1.1. Elasticsearch 配置

##### XPack 配置

开启 Elasticsearch 的 `x-pack` 插件。在 `elasticsearch/config/elasticsearch.yml` 配置文件中增加配置项：

```yml
xpack.security.enabled: true
xpack.security.audit.enabled: true
```

或者配置 `docker-compose.yml` 中的 `environment` 字段

```yml
environment:
    - xpack.security.enabled=true
    - xpack.security.audit.enabled=true
```

此时即启动了 `x-path` 安全开关，使得 Elasticsearch 的访问要求用户名和密码

##### 生成用户名密码

为 Elasticsearch 生成内置用户的密码，执行如下命令：

```bash
$ bin/elasticsearch-setup-passwords auto

Changed password for user apm_system
PASSWORD apm_system = 5R6rQ8sHTHCTvizSwZow
...
```

或者通过 docker 容器执行

```bash
$ docker exec -it es bin/elasticsearch-setup-passwords auto
```

- `auto` 自动生成所有内置用户的密码；也可以为 `interactive`，为每个内置用户手动输入密码

将生成的用户名密码进行记录

#### 1.1.2. Kibana 配置

接下来需要配置 Kibana，以用户名密码的方式登录 Elasticsearch

##### 直接配置用户名密码

修改 `kibana/config/kibana.yml`，添加如下配置：

```yml
monitoring.ui.container.elasticsearch.enabled: true
elasticsearch.username: elastic
elasticsearch.password: dQUtviHdUCryzvBVLR8O
```

或者通过 `docker-compose.yml` 中的 `environment` 字段

```yml
- SERVER_NAME=kibana
- ELASTICSEARCH_HOSTS=["http://es:9200"]
- MONITORING_UI_CONTAINER_ELASTICSEARCH_ENABLED=true
- ELASTICSEARCH_USERNAME=elastic
- ELASTICSEARCH_PASSWORD=dQUtviHdUCryzvBVLR8O
```

##### 使用 Kibana 密码管理器

密码管理器会生成 `kibana.keystore` 文件，无需在配置文件中明文存储密码，安全性更高

创建 `kibana.keystore` 文件

```bash
$ bin/kibana-keystore create
```

或通过 docker 容器

```bash
$ docker exec -it kibana bin/kibana-keystore create
```

如果是在容器内创建，则需要将该文件拷贝到宿主机，防止容器关闭后丢失

```bash
$ docker cp kibana:/usr/share/kibana/config/kibana.keystore .
```

然后将容器内的配置文件映射到宿主机上

```yml
volumes:
    - "./kibana.keystore:/usr/share/kibana/config/kibana.keystore"
```

重启容器，让 keystore 文件生效

接下来，为 keystore 文件添加密码，这个密码即之前在 Elasticsearch 中生成的密码

```bash
$ bin/kibana-keystore add elasticsearch.password
```

或通过 docker 容器

```bash
$ docker exec -it kibana bin/kibana-keystore add elasticsearch.password
```

**完成上述配置后，即可只在 `kibana.yml` 或 `docker-compose.yml` 中的 `environment` 字段中保留用户名配置（`elasticsearch.username` 或 `ELASTICSEARCH_USERNAME`）即可，无需保留密码配置**

重启 Kibana，再次进入时提示输入用户名密码

### 1.2. 修改密码

通过 Elasticsearch 的 xpack API 即可直接修改密码

```json
PUT _xpack/security/user/elastic/_password
{
    "password": "q5f2qNfUJQyvZPIz57MZ"
}
```

修改完毕后，需要重新设置 Kibana 的 keystore 或配置

如果是忘记密码，则需要先创建一个 Elasticsearch 超级用户

```bash
$ ./bin/elasticsearch-users useradd su -r superuser
Enter new password:
```

创建完毕后，用这个用户登录修改密码

```bash
$ curl -H "Content-Type: application/json" -u ryan:ryan123 \
       -XPUT http://localhost:9200/_xpack/security/user/elastic/_password -d '
{
    "password": "q5f2qNfUJQyvZPIz57MZ"
}'
```

### 1.3. 使用 TLS

#### 生成证书

生成 `elastic-stack-ca.p12` CA 文件

通过命令行

```bash
$ bin/elasticsearch-certutil ca
```

或通过 docker 容器

```bash
$ docker exec -it es bin/elasticsearch-certutil ca
```

生成 `elastic-certificates.p12` cert 文件

```bash
$ bin/elasticsearch-certutil cert --ca elastic-stack-ca.p12
```

或通过 docker 容器

```bash
$ docker exec -it es bin/elasticsearch-certutil cert --ca elastic-stack-ca.p12
```

如果是通过容器生成的证书，需要将其拷贝到宿主机

```bash
$ docker cp es:/usr/share/elasticsearch/elastic-certificates.p12 .
```

#### 使用证书

修改 `elasticsearch/config/elasticsearch.yml` 配置文件，添加如下内容

```yml
xpack.security.transport.ssl.keystore.type: PKCS12
xpack.security.transport.ssl.verification_mode: certificate
xpack.security.transport.ssl.keystore.path: elastic-certificates.p12
xpack.security.transport.ssl.truststore.path: elastic-certificates.p12
xpack.security.transport.ssl.truststore.type: PKCS12
```

或修改 `docker-compose.yml` 中的 `environment` 字段，添加如下内容

```yml
environment:
    - xpack.security.transport.ssl.keystore.type=PKCS12
    - xpack.security.transport.ssl.verification_mode=certificate
    - xpack.security.transport.ssl.keystore.path=elastic-certificates.p12
    - xpack.security.transport.ssl.truststore.path=elastic-certificates.p12
    - xpack.security.transport.ssl.truststore.type=PKCS12
```

将证书文件从容器内映射到宿主机

```yml
volumes:
    - "./elastic-certificates.p12:/usr/share/elasticsearch/config/elastic-certificates.p12"
```

重启 Elasticsearch 完成配置
