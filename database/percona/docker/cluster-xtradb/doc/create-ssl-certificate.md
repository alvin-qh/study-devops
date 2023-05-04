# Percona XtraDB Cluster 身份校验

Percona XtraDB Cluster 8 之后的版本默认会启用 SSL 连接, 不仅用于客户端的连接, 也用于集群节点之间的相互连接, 所以需要提供用于身份校验的密钥文件和证书文件

Percona XtraDB Cluster 需要在三个部分使用 SSL 证书, 通过配置文件指定:

```ini
[mysqld]
ssl-ca = /cert/ca.pem
ssl-cert = /cert/server-cert.pem
ssl-key = /cert/server-key.pem

[client]
ssl-ca = /cert/ca.pem
ssl-cert = /cert/client-cert.pem
ssl-key = /cert/client-key.pem

[sst]
encrypt = 4
ssl-ca = /cert/ca.pem
ssl-cert = /cert/server-cert.pem
ssl-key = /cert/server-key.pem
```

三部分分别为:

- `[mysqld]` 段下面的用于后端服务连接的证书
- `[client]` 段下面的配置用于设置客户端连接的证书
- `[sst]` 段下面的配置用于 SST 快照同步连接的证书

所需生成的证书密钥文件

| 名字               | 描述              |
|:------------------|:------------------|
| `ca.pem`          | 自签名的 CA 证书    |
| `ca-key.pem`      | CA 证书私钥        |
| `client-cert.pem` | 连接服务端的 CA 证书 |
| `client-key.pem`  | 服务端 CA 证书私钥  |
| `server-cert.pem` | 服务端 CA 证书      |
| `server-key.pem`  | 服务端 CA 证书私钥  |
| `public_key.pem`  | 密钥对的公钥        |
| `private_key.pem` | 密钥对的私钥        |

产生的证书密钥文件应具备如下特征:

- SSL 和 RSA 密钥的大小为 `2048` 位;
- SSL CA 证书是自签名的;
- 签名使用 SHA256WithRSAEncryption 算法, 使用 CA 证书和密钥对 SSL 服务器和客户端证书进行签名;

可以通过两种方式生成相关的证书文件, 即:

- 通过 MySQL 自带工具 `mysql_ssl_rsa_setup` 生成;
- 通过 OpenSSL 工具生成;

## 1. 使用 mysql_ssl_rsa_setup

`mysql_ssl_rsa_setup` 是专门用于 MySQL 数据库证书密钥生成的工具, 所以使用非常简单, 无需额外的操作即可产生符合上述要求的证书密钥文件, 且证书有效期为 `10` 年

使用命令如下

```bash
mysql_ssl_rsa_setup -d ./cert
```

此命令会在当前目录的 `cert` 子目录下生成所有所需的证书密钥文件

如果要通过容器, 则需要先建立存放证书文件的目录 (权限 `777`), 将此目录映射到容器路径中, 在执行容器中的 `mysql_ssl_rsa_setup` 命令, 整个操作如下:

```bash
mkdir -m 777 ./cert

docker run \
  --rm -v ./cert:/cert \
  percona/percona-xtradb-cluster \
  mysql_ssl_rsa_setup -d /cert
```

这样就可以在创建的 `cert` 目录下创建所需的证书密钥文件

可以通过 OpenSSL 对生产的证书密钥文件进行验证

```bash
openssl verify -CAfile ./cert/ca.pem ./cert/server-cert.pem ./cert/client-cert.pem
```

## 2. 使用 OpenSSL 工具

OpenSSL 工具使用起来就比较繁琐, 但 OpenSSL 可以在任何设备上执行, 这是其优点

注意, 下面的过程中, 每次创建证书要提供的 `Common Name` 都不能相同, 否则证书验签会失败, 切记

创建一个 `cert` 目录, 后面所有的操作都在这个目录下完成即可

### 2.1. 创建 CA 证书

```bash
# 创建 CA 密钥
openssl genrsa 2048 > ca-key.pem

# 创建证书, 需回答相关的交互问题
openssl req -new -x509 -nodes -days 3600 -key ca-key.pem -out ca.pem

You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:CN
State or Province Name (full name) [Some-State]:Shanxi
Locality Name (eg, city) []:Xi'an
Organization Name (eg, company) [Internet Widgits Pty Ltd]:Alvin
Organizational Unit Name (eg, section) []:
Common Name (e.g. server FQDN or YOUR name) []:MySQL Admin
Email Address []:quhao317@163.com
```

### 2.2. 创建 Server 证书

创建用于服务端的证书密钥文件, 其中:

- `server-cert.pem` 表示公钥文件
- `server-key.pem` 表示私钥文件

创建的过程如下:

```bash
openssl req -newkey rsa:2048 -nodes -keyout server-key.pem -out server-req.pem

.+..+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*...+.....+...+.......+...+........+...+....+...+.....+...+....+..+....+...........+......+.............+.........+..+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*.....................+..........+......+......+..............+.+..................+...........+............+.........+.+.......................+.+........+.......+...+...+........+.+......+..+...+....+...............+.....+.........+.......+.....+....+.....+...+................+.....+.......+...+.....+......+...+.+......+........+.......+...+........+..........+...+...........+...+......+.......+.....+............+.+.....+................+.....+.+.....+..........+...+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
....+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*....+....+...+.....+.......+......+.....+....+..+.............+.....+......+....+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*...+..+...+.........+...+.............+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
-----
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:CN
State or Province Name (full name) [Some-State]:Shanxi
Locality Name (eg, city) []:Xi'an
Organization Name (eg, company) [Internet Widgits Pty Ltd]:Alvin
Organizational Unit Name (eg, section) []:
Common Name (e.g. server FQDN or YOUR name) []:MySQL Server
Email Address []:quhao317@163.com

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:

openssl rsa -in server-key.pem -out server-key.pem
openssl x509 -req -in server-req.pem -days 3600 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out server-cert.pem
```

### 2.3. 创建 Client 证书

创建用于客户端的证书密钥文件, 其中:

- `client-cert.pem` 表示公钥文件
- `client-key.pem` 表示私钥文件

创建的过程如下:

```bash
# client-cert.pem = 公钥, client-key.pem = 私钥
openssl req -newkey rsa:2048 -nodes -keyout client-key.pem -out client-req.pem

..........+..+............+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*.+..+.......+..+.+..+................+......+...+..+...+...+...+....+...+.....+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*...+...+.+........+.+......+.....+...+...+...+....+........+.............+.........+........+....+.....+.............+.....+.+..+...+.........+.+......+......+........................+......+...+.........+..+....+.........+......+.....+.......+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
........+.+...........+....+..+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*....+...+..+......+....+..................+.................+....+.....+.+...+............+...+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*...+.+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
-----
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:CN
State or Province Name (full name) [Some-State]:Shanxi
Locality Name (eg, city) []:Xi'an
Organization Name (eg, company) [Internet Widgits Pty Ltd]:Alvin
Organizational Unit Name (eg, section) []:
Common Name (e.g. server FQDN or YOUR name) []:MySQL Client
Email Address []:quhao317@163.com

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:
An optional company name []:

openssl rsa -in client-key.pem -out client-key.pem
openssl x509 -req -in client-req.pem -days 3600 -CA ca.pem -CAkey ca-key.pem -set_serial 01 -out client-cert.pem
```

最后对生成的证书和密钥进行验证即可

```bash
openssl verify -CAfile ca.pem server-cert.pem client-cert.pem
```
