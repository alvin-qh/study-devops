# Docker Registry

- [Docker Registry](#docker-registry)
  - [1. 设置 Docker Registry](#1-设置-docker-registry)
    - [1.1. 镜像和容器](#11-镜像和容器)
    - [1.2. 容器配置](#12-容器配置)
      - [1.2.1. 设置用户名密码](#121-设置用户名密码)
      - [1.2.2. 启用 TLS (SSL) 访问](#122-启用-tls-ssl-访问)
        - [使用自签名证书进行测试](#使用自签名证书进行测试)
  - [2. 使用 Docker Registry](#2-使用-docker-registry)
    - [2.1. 登录 Dockers Registry](#21-登录-dockers-registry)
    - [2.2. 为镜像打标签](#22-为镜像打标签)
    - [2.3. 上传镜像](#23-上传镜像)
    - [2.4. 下载镜像](#24-下载镜像)
    - [2.5. 查看服务端的镜像](#25-查看服务端的镜像)

## 1. 设置 Docker Registry

### 1.1. 镜像和容器

拉取最新版本 Docker Registry 镜像

```bash
docker pull registry
```

通过 Docker Compose 启动容器

```bash
docker-compose up
```

### 1.2. 容器配置

#### 1.2.1. 设置用户名密码

安装 `htpasswd` 命令

- 对于 Debian 系列

  ```bash
  sudo apt update -y
  sudo apt install apache2-utils
  ```

- 对于 RHEL 系列

  ```bash
  yum -y install httpd
  ```

假设密码存储在 `./auth` 目录下的 `htpasswd` 文件中, 则通过 `htpasswd` 命令创建用户名密码

```bash
htpasswd -Bbn <user> <password> > auth/htpasswd
```

此时 `auth/htpasswd` 文件中存放加密后的用户名和密码

为容器设置如下文件映射

```yml
volumes:
    - ./auth:/auth
```

为容器设置如下环境变量

```yml
environment:
    - REGISTRY_AUTH=htpasswd
    - REGISTRY_AUTH_HTPASSWD_REALM=Registry Realm
    - REGISTRY_AUTH_HTPASSWD_PATH=/auth/htpasswd
```

重启容器, 此时 Registry 服务的用户名密码已启用

#### 1.2.2. 启用 TLS (SSL) 访问

假设 CA 文件存储在 `./cert` 路径下, 名为 `ca.crt` 和 `ca.key` 文件

为容器设置如下文件映射

```yml
volumes:
    - ./cert:/certs
```

为容器设置如下环境变量

```yml
environment:
    - REGISTRY_HTTP_ADDR=0.0.0.0:443  # 或其它端口
    - REGISTRY_HTTP_TLS_CERTIFICATE=/certs/ca.crt
    - REGISTRY_HTTP_TLS_KEY=/certs/ca.key
```

重启容器, 至此 Registry 服务支持 HTTPS 访问

##### 使用自签名证书进行测试

1. 生成证书

   ```bash
   openssl req -x509 -nodes -sha256 \
      # -days 365 \
      -newkey rsa:2048 \
      -out cert/ca.crt \
      -keyout cert/ca.key \
      -subj '/CN=localhost' \
      -extensions EXT
      -config <( \
       printf "[dn]\nCN=localhost\n[req]\ndistinguished_name = dn\n[EXT]\nsubjectAltName=DNS:localhost\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth")
   ```

   注意, 如果不使用 `-config` 配置, 则需要回答 OpenSSL 的一系列问题, 其中的 `Common Name (e.g. YOUR name)` 一项必须填为所使用的域名, 例如使用的 URL 为: `https://alvin.study-docker.edu`, 则需填写的域名为 `alvin.study-docker.edu`

   具体使用方法可以参考 [如何用 OpenSSL 创建自签名证书](https://docs.azure.cn/zh-cn/articles/azure-operations-guide/application-gateway/aog-application-gateway-howto-create-self-signed-cert-via-openssl) 文档

   也可以使用 [mkcert](https://github.com/FiloSottile/mkcert) 工具进行自建 CA, 具体参考 [mkcert 使用说明](./doc/mkcert.md) 章节

2. 设置当前系统信任该证书

   在需要访问 Docker Registry 的机器上执行如下命令:

   **方法一: 合并证书到系统证书集合文件中**

   - 对于 Debian 系列

     ```bash
     cat ca.crt >> /etc/ssl/certs/ca-certificates.crt
     ```

   - 对于 RHEL 系列

     ```bash
     cat domain.crt >> /etc/pki/tls/certs/ca-bundle.crt
     ```

   **方法二: 合并证书到系统证书集合文件中**

   - 对于 Debian 系列

     ```bash
     sudo ln -s $(pwd)/cert/ca.crt /usr/local/share/ca-certificates/docker-registry.crt
     sudo update-ca-certificates [--fresh]
     ```

     即将生成的 `ca.crt` 文件放入 `/etc/ssl/certs/` 路径下, 并刷新证书

   - 对于 RHEL 系列

     ```bash
     yum install ca-certificates
     sudo update-ca-trust force-enable
     sudo ln -s $(pwd)/cert/ca.crt /etc/pki/ca-trust/source/anchors/docker-registry.crt
     sudo update-ca-trust extract
     ```

3. 设置 Docker 信任该证书

   在需要推送镜像的机器上执行如下命令

   ```bash
   sudo mkdir -p /etc/docker/certs.d/localhost
   sudo ln -s $(pwd)/cert/ca.crt /etc/docker/certs.d/localhost/ca.crt
   ```

## 2. 使用 Docker Registry

### 2.1. 登录 Dockers Registry

假设 Docker Registry 监听本地的 `55055` 端口 (localhost:55055), 参见 [docker-compose.yml](./docker-compose.yml) 中的 `ports` 设置

```bash
docker login -u <username> -p <password> localhost:55055
```

这里的用户名和密码应使用 [设置用户名密码](#121-设置用户名密码) 章节中设置的用户名和密码

如果要退出登录, 可以使用

```bash
docker logout localhost:55055
```

### 2.2. 为镜像打标签

为了能让镜像传递到自建 Docker Registry 服务, 需要给镜像设置一个"特殊的" Tag, 格式为:

```plaintext
<server_addr>:<port>/<image_name>:<version>
```

- `server_addr` 即 Docker Registry 服务的地址
- `port` 即 Docker Registry 服务的端口号
- `image_name` 为镜像名称, 可以设置为任意字符串
- `version` 可以为任意字符串, 一般习惯于用 `latest` 表示最新版本

例如:

```plaintext
docker tag go-demo:1.0 localhost:55055/go-demo:1.0
```

当然, 也可以在产生 Docker 镜像时, 就设置为符合格式的名称

### 2.3. 上传镜像

通过 Docker 的 `push` 命令即可将镜像上传到 Docker Registry 服务, 即:

```bash
docker push <server_addr>:<port>/<image_name>:<version>
```

例如:

```bash
docker push localhost:55055/go-demo:1.0
```

### 2.4. 下载镜像

通过 Docker 的 `pull` 命令即可从 Docker Registry 服务上下载所需的镜像, 即:

```bash
docker pull <server_addr>:<port>/<image_name>:<version>
```

例如:

```bash
docker pull localhost:55055/go-demo:1.0
```

### 2.5. 查看服务端的镜像

查看 Docker Registry 服务上的镜像分类列表

```bash
curl -f http://<username>:<password>@<server_addr>:<port>/v2/_catalog
```

查看 Docker Registry 服务上某个镜像的详情

```bash
curl -f http://<username>:<password>@<server_addr>:<port>/v2/<image_name>/manifests/<version>
```
