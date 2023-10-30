# Kafka Python Client

## 1. 创建工程

### 1.1. 创建工程目录

```bash
mkdir <project_dir>
cd <project_dir>
```

### 1.2. 设置 Python 版本

```bash
pyenv local 3.11
```

### 1.3. 设置 VirtualEnv

创建 VirtualEnv

```bash
python -m venv .venv --prompt="kafka"
```

进入 VirtualEnv

```bash
source .venv/bin/activate
```

### 1.4. 安装依赖包

```bash
pip install -U kazoo            # Zookeeper 客户端
pip install -U confluent-kafka  # Kafka 客户端
pip install -U requests         # http 请求工具包, 用于 Kafka Schema Registry 客户端依赖
pip install -U fastavro         # Apache AVRO 协议包, 用于 Kafka Schema Registry 客户端依赖
pip install -U jsonschema       # Json Schema 协议包, 用于 Kafka Schema Registry 客户端依赖

pip install -U pytest  # 用于测试
```

所有依赖软件包参考 [requirements.txt](./requirements.txt) 文件, 可通过此文件一次性安装所有依赖

```bash
pip install -r requirements.txt
```

## 2. 执行测试

进入 VirtualEnv 之后, 执行如下命令进行测试

```bash
pytest .
```
