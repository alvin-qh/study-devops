# 命令行使用

Elasticsearch 没有专门的 CLI 客户端，可以直接使用 `curl` 或 `httpie` 进行操作

> [curl](https://curl.se/docs/manpage.html)
> [httpie](https://httpie.io/docs/cli)

## 使用 curl 命令

直接使用命令

```bash
curl -H 'Cache-Control: no-cache' \
       -H 'Content-Type: application/json' \
       -X GET 'http://localhost:9200/<index>?pretty' -d '{
           # query dsl
       }';
```

使用 JSON 文件

```bash
curl -H 'Cache-Control: no-cache' \
       -H 'Content-Type: application/json' \
       -X GET 'http://localhost:9200/<index>?pretty' -d '@<json file>'
```

## 使用 httpie 命令

直接使用命令

```bash
http GET http://localhost:9200/person 'Cache-Control: no-cache; Content-Type: application/json' a1=v1 a2:=v2
```

使用 JSON 文件

```bash
http GET http://localhost:9200/person 'Cache-Control: no-cache; Content-Type: application/json' < arg.json
```
