# mkcert 使用说明

- [mkcert 使用说明](#mkcert-使用说明)
  - [1. 前言](#1-前言)
  - [2. mkcert 简介](#2-mkcert-简介)
  - [3. mkcert 基本使用](#3-mkcert-基本使用)
    - [3.1. 将 CA 证书加入本地可信 CA](#31-将-ca-证书加入本地可信-ca)
    - [3.2. 生成自签证书](#32-生成自签证书)
  - [4. 使用生成的证书文件](#4-使用生成的证书文件)
  - [5. 局域网内使用](#5-局域网内使用)
  - [6. 其他一些高级用法](#6-其他一些高级用法)

## 1. 前言

在本地开发中, 有时候需要模拟 HTTPS 环境, 比如 PWA 应用要求必须使用 HTTPS 访问. 在传统的解决方案中, 需要使用自签证书, 然后在 HTTP Server 中使用自签证书. 由于自签证书浏览器不信任, 为了解决浏览器信任问题就需要将自签证书使用的 CA 证书添加到系统或浏览器的可信 CA 证书中, 来规避这个问题

以前这些步骤需要一系列繁琐的 openssl 命令生成, 尽管有脚本化的方案帮助来简化这些命令 (可以参考 [Nginx SSL 快速双向认证配置](./openssl.md)). 但是仍然非常麻烦. 本文将介绍一种更加简单友好的方式生成本地 HTTPS 证书, 并且信任自签 CA 的方案 - mkcert

## 2. mkcert 简介

mkcert 是一个使用 GO 语言编写的生成本地自签证书的小程序, 具有跨平台, 使用简单, 支持多域名, 自动信任 CA 等一系列方便的特性可供本地开发时快速创建 HTTPS 环境使用

安装方式也非常简单, 由于 GO 语言的静态编译和跨平台的特性, 官方提供各平台预编译的版本, 直接下载到本地, 给可执行权限(Linux/Unix) 就可以了. 下载地址 <https://github.com/FiloSottile/mkcert/releases/latest>

此外, mkcert 已经推送至 Homebrew, MacPorts, Linuxbrew, Chocolatey, Scoop 等包管理平台中, 也可以直接借助对应的包管理平台安装, 如:

```bash
brew install mkcert  # Homebrew/Linuxbrew
choco install mkcert  # Chocolatey
```

安装成功后, 就可以使用 mkcert 命令了

```bash
mkcert

Using the local CA at "/usr/local/bin/mkcert" ✨
Usage of mkcert:

    $ mkcert -install
    Install the local CA in the system trust store.

    $ mkcert example.org
    Generate "example.org.pem" and "example.org-key.pem".

    $ mkcert example.com myapp.dev localhost 127.0.0.1 ::1
    Generate "example.com+4.pem" and "example.com+4-key.pem".

    $ mkcert "*.example.it"
    Generate "_wildcard.example.it.pem" and "_wildcard.example.it-key.pem".

    $ mkcert -uninstall
    Uninstall the local CA (but do not delete it).

For more options, run "mkcert -help".
```

## 3. mkcert 基本使用

从上面自带的帮助输出来看, mkcert 已经给出了一个基本的工作流, 规避了繁杂的 openssl 命令, 几个简单的参数就可以生成一个本地可信的 HTTPS 证书了. 更详细的用法参考官方文档

### 3.1. 将 CA 证书加入本地可信 CA

```bash
mkcert -install

Using the local CA at "/usr/local/bin/mkcert" ✨
```

仅仅这么一条简单的命令, 就可将 mkcert 生成的根证书加入了本地可信 CA 中, 以后由该 CA 签发的证书在本地都是可信的

可以在当前操作系统中找到已经安装的 CA 证书

### 3.2. 生成自签证书

```bash
mkcert domain1 [domain2 [...]]
```

直接跟多个要签发的域名或 IP 就行了, 比如签发一个仅本机访问的证书 (可以通过 `127.0.0.1` 和 `localhost`, 以及 IPV6 地址`::1` 访问)

```bash
mkcert localhost 127.0.0.1 ::1

Using the local CA at "/usr/local/bin/mkcert" ✨

Created a new certificate valid for the following names 📜
 - "localhost"
 - "127.0.0.1"
 - "::1"

The certificate is at "./localhost+2.pem" and the key at "./localhost+2-key.pem" ✅
```

通过输出, 可以看到成功生成了 `localhost+2.pem` 证书文件和 `localhost+2-key.pem` 私钥文件, 只要在 WEB 服务上配置这两个文件即可

## 4. 使用生成的证书文件

默认生成的证书格式为 PEM (Privacy Enhanced Mail) 格式, 任何支持 PEM 格式证书的程序都可以使用. 比如常见的 Apache 或 Nginx 等, 这里我们用 Python 自带的 SimpleHttpServer 演示一下这个证书的效果:

```python
import http.server
import ssl

httpd = http.server.HTTPServer(('0.0.0.0', 443), http.server.SimpleHTTPRequestHandler)
httpd.socket = ssl.wrap_socket(
    httpd.socket,
    certfile='./localhost+2.pem',
    keyfile='./localhost+2-key.pem',
    server_side=True,
    ssl_version=ssl.PROTOCOL_TLSv1_2,
)
httpd.serve_forever()
```

## 5. 局域网内使用

有时候需要在局域网内测试 HTTPS 应用, 这种环境可能不对外, 因此也无法使用像 Let's encrypt 这种免费证书的方案给局域网签发一个可信的证书, 而且 Let's encrypt 本身也不支持认证 IP 地址

证书可信的三个要素:

- 由可信的 CA 机构签发
- 访问的地址跟证书认证地址相符
- 证书在有效期内

如果期望自签证书在局域网内使用, 以上三个条件都需要满足. 很明显自签证书一定可以满足证书在有效期内, 那么需要保证后两条. 我们签发的证书必须匹配浏览器的地址栏, 比如局域网的 IP 或者域名, 此外还需要信任 CA

重新签发一下证书, 加上本机的局域网 IP 认证:

```bash
mkcert localhost 127.0.0.1 ::1 192.168.31.170

Using the local CA at "/usr/local/bin/mkcert" ✨

Created a new certificate valid for the following names 📜
 - "localhost"
 - "127.0.0.1"
 - "::1"
 - "192.168.31.170"

The certificate is at "./localhost+3.pem" and the key at "./localhost+3-key.pem" ✅
```

再次验证发现使用 `https://192.168.31.170` 本机访问也是可信的. 然后我们需要将 CA 证书发放给局域网内其他的用户

使用 `mkcert -CAROOT` 命令可以列出 CA 证书的存放路径

```bash
ls $(mkcert -CAROOT)
```

可以看到 CA 路径下有两个文件 `rootCA-key.pem` 和 `rootCA.pem` 两个文件, 用户需要信任 `rootCA.pem` 这个文件

将 `rootCA.pem` 拷贝一个副本, 并命名为 `rootCA.crt` (大部分操作系统的 CA 证书文件扩展名为 `.crt`), 将 `rootCA.crt` 文件分发给其他用户, 手工导入:

1. Windows

   添加证书

   ```powershell
   certutil -addstore -f "ROOT" rootCA.crt
   ```

   移除证书

   ```powershell
   certutil -delstore "ROOT" <serial-number-hex>
   ```

   证书序列号可以通过证书工具进行查询

2. macOS

   添加证书

   ```bash
   sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain rootCA.crt
   ```

   移除证书

   ```bash
   sudo security delete-certificate -c "<name of existing certificate>"
   ```

   证书名称可以通过证书工具进行查询

3. Debian 系列

   添加证书

   ```bash
   sudo cp rootCA.crt /usr/local/share/ca-certificates
   sudo update-ca-certificates
   ```

   移除证书

   ```bash
   sudo rm /usr/local/share/ca-certificates/rootCA.crt
   sudo update-ca-certificates --fresh
   ```

4. RHEL 系列

   添加证书

   ```bash
   yum install ca-certificates
   sudo update-ca-trust force-enable
   sudo cp rootCA.crt /etc/pki/ca-trust/source/anchors/
   sudo update-ca-trust extract
   ```

## 6. 其他一些高级用法

以上演示了一些 mkcert 的基本用法, 通过 `mkcert --help` 命令还可以查看很多高级用法, 例如:

- `-cert-file FILE`, `-key-file FILE`, `-p12-file FILE` 参数可以定义输出的证书文件名
- `-client` 参数可以产生客户端认证证书, 用于 SSL 双向认证
- `-pkcs12` 命令可以产生 PKCS12 格式的证书. Java 程序通常不支持 PEM 格式的证书, 但是支持 PKCS12 格式的证书. 通过这个程序可以很方便的产生 PKCS12 格式的证书直接给 Java 程序使用
