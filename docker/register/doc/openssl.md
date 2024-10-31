# Nginx SSL 快速双向认证配置脚本

- [Nginx SSL 快速双向认证配置脚本](#nginx-ssl-快速双向认证配置脚本)
  - [1. SSL 双向认证](#1-ssl-双向认证)
  - [2. Nginx 的 SSL 双向认证配置](#2-nginx-的-ssl-双向认证配置)
    - [2.1. 开启 HTTPS 访问](#21-开启-https-访问)
    - [2.2. 生成客户端证书并签证 (脚本)](#22-生成客户端证书并签证-脚本)
      - [create\_ca\_cert.sh](#create_ca_certsh)
      - [create\_client\_cert.sh](#create_client_certsh)
      - [revoke\_cert.sh](#revoke_certsh)
    - [2.3. 脚本分析](#23-脚本分析)
    - [2.4. 运行脚本](#24-运行脚本)
      - [2.4.1. 创建 CA](#241-创建-ca)
      - [2.4.2. 证书签发](#242-证书签发)
      - [2.4.3. 证书吊销](#243-证书吊销)
  - [3. 小结](#3-小结)

目前遇到一个项目有安全性要求, 要求只有个别用户有权限访问. 本着能用配置解决就绝不用代码解决的原则, 在 Nginx 上做一下限制和修改即可

这种需求其实实现方式很多, 经过综合评估考虑, 觉得 SSL 双向认证方案对用户使用最简单, 遂决定用此方案

> 注: 本方案在 Ubuntu Server 16.04 LTS 实施, 其他操作系统请酌情修改

## 1. SSL 双向认证

绝大多数 SSL 应用都以单向认证为主, 即客户端只要信任服务端, 就可以使用服务端的公钥加密后向服务端发起请求, 由服务端的私钥解密之后获得请求数据

如果这个过程反过来, 让服务端信任客户端, 服务端使用客户端的公钥加密之后将数据返回给客户端, 其实也是可以做到的, 原理和实现跟单向认证都差不多

服务端信任客户端的操作往往也会伴随着客户端认证服务端的过程, 所以让服务端信任客户端的 SSL 认证方式往往也被称为 SSL 双向认证, 并且要配置 SSL 双向认证必须先开启服务端 SSL, 先配置客户端信任服务端

## 2. Nginx 的 SSL 双向认证配置

### 2.1. 开启 HTTPS 访问

根据理论知识, 我们必须先开启 Nginx 的 SSL 配置, 即启用 HTTPS. 这个过程较为简单, 目前有 Let's encrypt 这种免费的证书方案, 再也不用发愁自己搭建 CA 自签了. 申请免费证书的过程略过, 直接贴启用 HTTPS 的配置:

```nginx
server {
  listen 80;
  listen 443 ssl http2;
  server_name example.com;

  ssl_certificate /etc/letsencrypt/live/example.com/fullchain.pem;
  ssl_certificate_key /etc/letsencrypt/live/example.com/privkey.pem;

  # 只有Nginx >= 1.13.0 版本才支持TLSv1.3协议
  # ssl_protocols TLSv1.3;
  # Nginx 低于 1.13.0 版本用这个配置
  ssl_protocols TLSv1 TLSv1.1 TLSv1.2;
  ssl_prefer_server_ciphers on;
  ssl_dhparam dhparam.pem; # openssl dhparam -out /etc/nginx/dhparam.pem 4096
  ssl_ciphers 'EECDH+AESGCM:EDH+AESGCM:AES256+EECDH:AES256+EDH';
  ssl_ecdh_curve secp384r1; # Requires nginx >= 1.1.0
  ssl_session_timeout  10m;
  ssl_session_cache shared:SSL:10m;
  ssl_session_tickets off; # Requires nginx >= 1.5.9
  ssl_stapling on; # Requires nginx >= 1.3.7
  ssl_stapling_verify on; # Requires nginx => 1.3.7
  resolver 223.5.5.5 114.114.114.114 valid=300s;
  resolver_timeout 5s;

  # 启用 HSTS 的配置, 如果你的域名下还有非标准端口访问的http应用, 请勿启用 HSTS
  # add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload";
  # 下面这个配置会拒绝Frame标签内容, 请确认你的网站没有frame / iframe
  add_header X-Frame-Options DENY;
  add_header X-Content-Type-Options nosniff;
  add_header X-XSS-Protection "1; mode=block";

  # 为了 Let's encrypt 续期用, 不用 Let's encrypt 不需要这个 location
  location /.well-known {
    root /usr/share/nginx/html;
  }

  ... SNIP ...

  # 强制http跳转为https
  if ($scheme != "https") {
    return 301 https://$http_host$request_uri;
  }
}
```

> 上述配置参考来自于 [加强 SSL 的安全性配置](https://cipherli.st/)

特别注意最后的强制 HTTPS 跳转, 其目的是 SSL 双向认证, 不走 HTTPS 无任何意义, 所以必须强制跳转 HTTPS

### 2.2. 生成客户端证书并签证 (脚本)

整个脚本目录结构如图:

```bash
tree /etc/nginx/ssl_certs/

/etc/nginx/ssl_certs/
├── create_ca_cert.sh
├── create_client_cert.sh
├── revoke_cert.sh

0 directories, 3 files
```

自行创建 `/etc/nginx/ssl_certs/`, 放入三个脚本, 分别用于生成 CA 证书以及 CA 目录 (`create_ca_cert.sh` 脚本的作用, 只有第一次需要运行), 创建客户端证书, 并用 CA 证书签证 (`create_client_cert.sh` 脚本的作用, 必须先生成 CA 证书), `revoke_cert.sh` 脚本用于吊销证书, 需要收回权限的时候可以使用

每个脚本内容如下:

#### create_ca_cert.sh

```bash
#!/bin/bash -e

# 创建 CA 根证书
# 非交互式方式创建以下内容:
# 国家名 (2 个字母的代号)
C=CN
# 省
ST=Shannxi
# 市
L=Xian
# 公司名
O=My Company
# 组织或部门名
OU=技术部
# 服务器 FQDN 或颁发者名
CN=www.example.com
# 邮箱地址
emailAddress=admin@example.com

mkdir -p ./demoCA/{private,newcerts}
touch ./demoCA/index.txt
[ ! -f ./demoCA/seria ] && echo 01 > ./demoCA/serial
[ ! -f ./demoCA/crlnumber ] && echo 01 > ./demoCA/crlnumber
[ ! -f ./demoCA/cacert.pem ] && openssl req -utf8 -new -x509 -days 36500 -newkey rsa:2048 -nodes -keyout ./demoCA/private/cakey.pem -out ./demoCA/cacert.pem -subj "/C=${C}/ST=${ST}/L=${L}/O=${O}/OU=${OU}/CN=${CN}/emailAddress=${emailAddress}"
[ ! -f ./demoCA/private/ca.crl ] && openssl ca -crldays 36500 -gencrl -out "./demoCA/private/ca.crl"
```

#### create_client_cert.sh

```bash
#!/bin/bash -e

show_help () {
    echo "$0 [-h|-?|--help] [--ou ou] [--cn cn] [--email email]"
    echo "-h|-?|--help    显示帮助"
    echo "--ou            设置组织或部门名, 如: 技术部"
    echo "--cn            设置 FQDN 或所有者名, 如: Alvin"
    echo "--email         设置 FQDN 或所有者邮件, 如: alvin@fake-mail.com"
}

while [[ $# -gt 0 ]]
do
    case $1 in
        -h|-\?|--help)
            show_help
            exit 0
            ;;
        --ou)
            OU="${2}"
            shift
            ;;
        --cn)
            CN="${2}"
            shift
            ;;
        --email)
            emailAddress="${2}"
            shift
            ;;
        --)
            shift
            break
        ;;
        *)
            echo -e "Error: $0 invalid option '$1'\nTry '$0 --help' for more information.\n" >&2
            exit 1
        ;;
    esac
shift
done

# 创建客户端证书
# 非交互式方式创建以下内容:
# 国家名(2个字母的代号)
C=CN
# 省
ST=Shannxi
# 市
L=Xian
# 公司名
O=My Company
# 组织或部门名
OU=${OU:-测试部门}
# 服务器FQDN或授予者名
CN=${CN:-demo}
# 邮箱地址
emailAddress=${emailAddress:-demo@example.com}

mkdir -p "${CN}"

[ ! -f "${CN}/${CN}.key" ] && openssl req -utf8 -nodes -newkey rsa:2048 -keyout "${CN}/${CN}.key" -new -days 36500 -out "${CN}/${CN}.csr" -subj "/C=${C}/ST=${ST}/L=${L}/O=${O}/OU=${OU}/CN=${CN}/emailAddress=${emailAddress}"
[ ! -f "${CN}/${CN}.crt" ] && openssl ca -utf8 -batch -days 36500 -in "${CN}/${CN}.csr" -out "${CN}/${CN}.crt"
[ ! -f "${CN}/${CN}.p12" ] && openssl pkcs12 -export -clcerts -CApath ./demoCA/ -inkey "${CN}/${CN}.key" -in "${CN}/${CN}.crt" -certfile "./demoCA/cacert.pem" -passout pass: -out "${CN}/${CN}.p12"
```

#### revoke_cert.sh

```bash
#!/bin/bash -e

# 吊销一个签证过的证书

openssl ca -revoke "${1}/${1}.crt"
openssl ca -gencrl -out "./demoCA/private/ca.crl"
```

### 2.3. 脚本分析

首先是创建 CA, 对于 Ubuntu 系统来说, `/etc/ssl/openssl.cnf` 配置中默认的 CA 路径就是 `./demoCA`, 为了不改动默认配置, 直接按照默认配置的内容创建这些目录和文件即可

还有就是 openssl 子命令非常多, 但是也和 git 一样, 可以合并命令, 比如用一条命令同时生成私钥和签证请求 `openssl req -nodes -newkey rsa:2048 -keyout client.key -new -out client.csr`, 在 `req` 的同时就做了 `genrsa`. 由于创建 CA 脚本只是第一次运行需要, 因此把证书配置直接写死在脚本中就完事了

接下来是创建客户端证书, 为了简化用户的使用, 在服务端帮助用户生成证书并签证, 把签证过的证书下发给用户就可以了. 由于用户可能是不同部门, 不同姓名, 不同邮件地址, 因此将这三个参数外部化, 做一下参数解析, 加上友好的命令行提示防止遗忘. 这个脚本特别注意最后一行, 会生成一个 PKCS12 格式的证书. openssl 默认产生的证书格式都是 PEM 的, 会将公钥和私钥分开, 但是浏览器导入的时候需要将这些内容合并起来形成证书链, 所以需要将签证过的证书和私钥文件合并成一个 PKCS12 格式的证书, 直接将这个 `.p12` 格式的证书交给用户就可以了

最后是吊销证书了, 当希望收回某个用户的访问权限时, 直接运行这个脚本跟上目录名就可以了

### 2.4. 运行脚本

#### 2.4.1. 创建 CA

```bash
./create_ca_cert.sh

Generating a 2048 bit RSA private key
.......................+++
........................................................................................................+++
writing new private key to './demoCA/private/cakey.pem'
-----
Using configuration from /usr/ssl/openssl.cnf
```

此时产生的 `./demoCA` 目录结构如下:

```bash
demoCA/
├── cacert.pem
├── crlnumber
├── crlnumber.old
├── index.txt
├── newcerts
├── private
│   ├── ca.crl
│   └── cakey.pem
└── serial

2 directories, 7 files
```

此时就可以配置 Nginx 了, 在上面单向 SSL 的配置中, 追加以下配置:

```nginx
ssl_client_certificate ssl_certs/demoCA/cacert.pem;
ssl_crl ssl_certs/demoCA/private/ca.crl;
ssl_verify_client on;
```

`ssl_client_certificate` 就是客户端证书的 CA 证书了, 代表此 CA 签发的证书都是可信的, `ssl_verify_client on` 代表强制启用客户端认证, 非法客户端 (无证书, 证书不可信) 都会返回 400 错误

特别注意 `ssl_crl` 这个配置, 代表 Nginx 会读取一个 CRL (Certificate Revoke List) 文件, 之前说过, 可能会有收回用户权限的需求, 因此我们必须有吊销证书的功能, 产生一个 CRL 文件让 Nginx 知道哪些证书被吊销了即可

注意: Nginx 配置都是静态的, 读取配置文件之后都会加载到内存中, 即使文件内容变化也不会重新读取. 因此当 CRL 文件发生变更之后, Nginx 并不能意识到有新的证书被吊销了, 所以必须使用 `reload` 指令让 Nginx 重新读取配置文件:

```bash
service nginx reload # 或 nginx -s reload
```

此时重启 Nginx 服务, 就可以完成 SSL 双向认证配置了

#### 2.4.2. 证书签发

```bash
./create_client_cert.sh --ou 财务部 --cn 财务经理 --email <cy@example.com>

Generating a 2048 bit RSA private key
................................+++
.............................................................................+++
writing new private key to '财务经理/财务经理.key'
-----

Using configuration from /usr/ssl/openssl.cnf
Check that the request matches the signature
Signature ok
Certificate Details:
        Serial Number: 1 (0x1)
        Validity
            Not Before: Jun 14 16:03:46 2018 GMT
            Not After : May 21 16:03:46 2118 GMT
        Subject:
            countryName               = CN
            stateOrProvinceName       = Shannxi
            organizationName          = My Company
            organizationalUnitName    = \U8D22\U52A1\U90E8
            commonName                = \U8D22\U52A1\U7ECF\U7406
            emailAddress              = <cy@example.com>
        X509v3 extensions:
            X509v3 Basic Constraints:
                CA:FALSE
            Netscape Comment:
                OpenSSL Generated Certificate
            X509v3 Subject Key Identifier:
                B5:91:0B:1F:FC:25:3B:2A:F9:EF:39:39:51:E3:1F:64:78:8A:C3:75
            X509v3 Authority Key Identifier:
                keyid:86:55:76:15:A3:F5:58:CB:8F:39:A3:56:8E:FF:18:97:AE:27:60:0F

Certificate is to be certified until May 21 16:03:46 2118 GMT (36500 days)

Write out database with 1 new entries
Data Base Updated

tree 财务经理/

财务经理/
├── 财务经理.crt
├── 财务经理.csr
├── 财务经理.key
└── 财务经理.p12

0 directories, 4 files
```

这个脚本生成了私钥文件 key, 签证请求文件 `csr`, 经过 CA 签证后的证书文件 `crt` (里面没有私钥), 以及将 `crt` 文件和 key 文件进行 bundle 之后的 PKCS12 格式的证书文件 `p12`, 将 `p12` 文件下载到本地, 双击一路 Next 导入证书即可

注: 由于 CA 的证书文件不会发生变化, 因此签证新的客户端证书不需要 `restart` 或 `reload` Nginx

打开网站, 浏览器就会提示选择一个已有的客户端证书进行认证了, 没问题就可以看到网站内容了

> 注: 每次导入新的证书之后, 必须重启浏览器才能提示使用新的证书文件

按照这种方式, 有多少人需要授权, 就可以用这个脚本签发多少个这样的证书, 用户将p12证书导入本地就可以正常访问网站了

#### 2.4.3. 证书吊销

当需要收回某人的权限的时候 (比如离职了), 需要吊销他的证书:

```bash
./revoke_cert.sh 财务经理

Using configuration from /usr/ssl/openssl.cnf
Revoking Certificate 01.
Data Base Updated
Using configuration from /usr/ssl/openssl.cnf

service nginx reload
```

这个脚本会自动吊销他的签证文件 `crt`, 并且自动更新 CRL 文件. 特别注意需要 `reload` 或 `restart` Nginx 才能让 Nginx 重新加载 CRL 文件. 这样被吊销的证书将无法访问网站了

## 3. 小结

本文通过 Nginx 配置 SSL 双向认证实现对客户端的加密认证, 通过使用了简易的脚本以快速生成各种证书与签证, 免除记忆繁琐 openssl 命令行, 简化使用

当然这只是一个最小可用集, 当规模比较大的时候可能需要做很多改进, 比如加入 CA 的 WEB UI, 直接可以操作签证和吊销证书, 并且可以自动重启 Nginx

再比如 CRL 这种静态配置文件不适合, 希望的动态更新吊销证书列表, 那么可以考虑 OCSP 方案, 这个 Nginx 也是支持的, 通过 `ssl_stapling_responder` 配置指定一个 OCSP 地址, 这样将不需要每次吊销证书的时候都去重启 Nginx 了, openssl 也提供了 OCSP 服务端的功能, 这里就不赘述了, 可以自行查找相关资料
