# Gradle

## 1. 设置环境

### 1.1. 安装 Gradle

#### 1.1.1. 安装 JDK

安装 OpenJDK

```bash
$ sudo apt install openjdk-11-jdk-headless
```

或按照 [官方文档](https://openjdk.java.net/install/) 的引导进行安装

#### 1.1.2. 设置默认的 JDK 版本

如果安装了多个 JDK，则需设置默认使用的 JDK 版本

```bash
$ sudo update-alternatives --install \
    /usr/bin/java java \
    /usr/lib/jvm/java-11-openjdk-amd64/bin/java 1

$ sudo update-alternatives --install \
    /usr/bin/javac javac \
    /usr/lib/jvm/java-11-openjdk-amd64/bin/javac 1
```

设置默认 JDK:

```bash
$ sudo update-alternatives --config java
$ sudo update-alternatives --config javac
```

可以通过 `java -version` 以及 `javac -version` 命令检查默认 JDK 的版本号

### 1.2. 安装 gradle

#### 1.2.1. 下载 gradle

> 从 [官网](https://gradle.org/releases/) 下载最新版的 gradle

下载并解压

```bash
$ wget https://services.gradle.org/distributions/gradle-x.y-bin.zip

$ unzip gradle-6.3-bin.zip
```

#### 1.2.2. 配置 gradle

编辑 `~/.zshrc` (或者 `~/.bash_profile`，`~/.bashrc` 等) 文件, 添加如下内容：

```bash
GRADLE_HOME=/usr/lib/gradle-6.3 # 设置 gradle 根目录
PATH=$GRADLE_HOME/bin:$PATH # 将 gradle 的 bin 目录加入 PATH
export PATH GRADLE_HOME
```
