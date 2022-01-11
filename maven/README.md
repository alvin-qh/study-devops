# Apache Maven

## 1. 安装

### 1.1. macOS

```bash
$ brew install maven
```

### 1.2. Linux

从 [官网](https://maven.apache.org/download.cgi) 下载二进制包

```bash
$ wget https://dlcdn.apache.org/maven/maven-3/3.8.4/binaries/apache-maven-3.8.4-bin.zip
```

将二进制包移动到 `/opt` 下

```bash
$ sudo mv apache-maven-3.8.4-bin.zip /opt
```

解压缩

```bash
$ unzip /opt/apache-maven-3.8.4-bin.zip
```

配置环境变量，编辑 `~/.zshrc`（或其它相关）文件

```bash
...
export M2_HOME="/opt/apache-maven-3.8.4"
export PATH="$M2_HOME/bin:$PATH"
```

## 2. 初始化项目

### 2.1. 通过交互命令

输入以下命令，并按照提示一步步确认，即可完成项目创建

```bash
$ mvn archetype:generate
Choose archetype:
1: internal -> org.apache.maven.archetypes:maven-archetype-archetype (An archetype which contains a sample archetype.)
2: internal -> org.apache.maven.archetypes:maven-archetype-j2ee-simple (An archetype which contains a simplifed sample J2EE application.)
...
Choose a number or apply filter (format: [groupId:]artifactId, case sensitive contains): 7:
>
Define value for property 'groupId':
>
Define value for property 'artifactId':
>
Define value for property 'version' 1.0-SNAPSHOT:
>
Define value for property 'package' xxx.xxx:
>
Y:
```

### 2.2. 通过命令行参数

```bash
$ mvn archetype:generate
    -DgroupId = xxx.yyy
    -DartifactId = any-artifact-id
    -DarchetypeArtifactId = maven-archetype-quickstart
    -DinteractiveMode = false
```
