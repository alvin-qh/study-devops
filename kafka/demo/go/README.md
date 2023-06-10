# Kafka Golang Client

## 1. 创建 Go 工程

### 1.1. 确保 Go Module 包管理已启用

```bash
export GO111MODULE=on
```

### 1.2. 设置 Go 包下载国内代理

```bash
export GOPROXY=https://goproxy.cn
```

### 1.3. 创建 Go 工程

```bash
mkdir <go_project>
cd <go_project>

go mod init <go_project>
```

在 `go_project` 目录下创建 [go.mod](./go.mod) 和 [go.sum](./go.sum) 文件

### 1.4. 安装依赖包

```bash
# Kafka 客户端依赖包
go get -u github.com/confluentinc/confluent-kafka-go/kafka

# UUID 生成依赖包
go get -u github.com/google/uuid

# 测试断言依赖包
go get -u github.com/stretchr/testify
```

### 1.5. 重新初始化项目

在已经创建了 [go.mod](./go.mod) 和 [go.sum](./go.sum) 文件的基础上, 可以进行项目的重新初始化

```bash
go mod tidy
```

## 2. 执行测试

在项目路径下, 执行如下命令

```bash
go test
```
