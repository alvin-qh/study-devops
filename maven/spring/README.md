# Spring Boot

- [Spring Boot](#spring-boot)
  - [1. Spring Boot 插件](#1-spring-boot-插件)
    - [1.1. 基本使用](#11-基本使用)
    - [1.2. 启用远程调试](#12-启用远程调试)
  - [2. 依赖](#2-依赖)
    - [2.1. 引入 Spring Boot BOM](#21-引入-spring-boot-bom)
    - [2.2. 基于 BOM 引入相关依赖](#22-基于-bom-引入相关依赖)
  - [3. Lombok 支持](#3-lombok-支持)
    - [3.1. 引入依赖](#31-引入依赖)
    - [3.2. 增加注解处理器](#32-增加注解处理器)
  - [备注](#备注)
    - [1. 基于 VSCode 设置远程调试](#1-基于-vscode-设置远程调试)

## 1. Spring Boot 插件

参见 <https://docs.spring.io/spring-boot/docs/2.3.0.RELEASE/maven-plugin/reference/html/> 文档

Springboot 提供了一个完整的插件 `spring-boot-maven-plugin`, 一站式的解决运行, 调试和打包需求

### 1.1. 基本使用

`spring-boot-maven-plugin` 插件引入了 `spring-boot` 指令和若干的 `goal`s, 用于完成不同的操作

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <version>${version.spring-boot}</version>
  <configuration>
    <mainClass>alvin.study.maven.Application</mainClass>
    <layout>jar</layout>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>repackage</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

- `mainClass` 在 `MANIFEST.MF` 文件中指定入口类
- `layout` 打包格式
  - `jar` 默认值, 打包为可执行 jar 文件
  - `war` 默认值, 打包为可执行 war 文件, 需要的 servlet 容器依赖位于 `WEB-INF/lib-provided`
  - `zip` 即 dir, 类似于 jar
  - `module` 将所有的依赖库打包 (`scope` 为 `provided` 的除外), 但是不打包任何 Launcher
  - `none` 将所有的依赖库打包, 但是不打包任何 Launcher
- `<goal>repackage</goal>` 附加在 `spring-boot:repackage` 指令上, 但该指令不能独立运行, 必须在执行 `mvn package` 时被触发执行

除了 `repackage` 指令外, 该插件指令的其它 `goal`s 还包括：

- `spring-boot:build-image` 打包一个 `OCI` 规范的镜像, 可以通过 docker 执行
- `spring-boot:build-info` 在 `target` 目录生成 `build-info.properties` 文件, 包含本次构建的信息

  ```property
  build.artifact=study-maven-spring
  build.group=alvin.study
  build.name=study-maven-spring
  build.time=2022-01-14T17\:03\:01.343Z
  build.version=1.0-SNAPSHOT
  ```

- `spring-boot:help` 输出插件帮助信息, 调用 `mvn spring-boot:help -Ddetail=true -Dgoal=<goal-name>` 显示命令参数的详细信息
- `spring-boot:repackage` 将已经存在的 jar 或 war 包打包为 springboot 的 layout 形式
- `spring-boot:run` 执行程序, 并进行阻塞, 方便查看日志
- `spring-boot:start` 在新进程执行程序并转入后台
- `spring-boot:stop` 停止通过 `spring-boot:start` 启动的程序

### 1.2. 启用远程调试

可以为 `spring-boot-maven-plugin` 插件添加一个特殊的 `execution` 来启动服务的远程调试功能

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <configuration>
    <mainClass>alvin.study.maven.Application</mainClass>
  </configuration>
  <executions>
    <execution>
      <id>debug</id>
      <goals>
        <goal>run</goal>
      </goals>
      <phase>none</phase>
      <configuration>
        <jvmArguments>
          -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005
        </jvmArguments>
      </configuration>
    </execution>
  </executions>
</plugin>
```

这里设置了一个 `id` 为 `debug` 的 `execution`, 并和 `run` 这个 `goal` 进行绑定, 且不关联任何 `phase`, 可以通过如下命令启用:

```bash
mvn spring-boot:run@debug
```

## 2. 依赖

### 2.1. 引入 Spring Boot BOM

可以引入 Spring Boot 的 BOM 文件, 这样就无需为每个依赖引入指定版本号

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${version.spring-boot}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
<dependencyManagement>
```

### 2.2. 基于 BOM 引入相关依赖

直接引入其它 Spring Boot 相关依赖, 且无需声明依赖的版本号

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
    <exclusions>
      <exclusion>
        <groupId>junit</groupId>
        <artifactId>*</artifactId>
      </exclusion>
      <exclusion>
        <groupId>org.junit.vintage</groupId>
        <artifactId>*</artifactId>
      </exclusion>
    </exclusions>
  </dependency>

  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <scope>test</scope>
  </dependency>
</dependencies>
```

需要注意, 如果使用 JUnit5, 则需要在 `spring-boot-starter-test` 依赖中排除掉 JUnit4 的依赖

`spring-boot-devtools` 用于进行 "热加载", 即在调试模式下, 改动代码后可以自动重编译程序并重启, 减少调试的麻烦. 可以通过 `application.yml` 对该插件进行配置

```yml
spring:
  devtools:
    restart:
      enabled: true
      exclude: static/**,public/** # 排除路径, 这些路径文件变化不引发重编译
      additional-exclude: static/**,public/**
      additional-paths: src/main/java # 增加路径, 该路径的文件变化也会引起重编译
```

## 3. Lombok 支持

[`lombok`](https://projectlombok.org/setup/maven)

### 3.1. 引入依赖

引入依赖

```xml
<dependencies>
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
  </dependency>
</dependencies>
```

### 3.2. 增加注解处理器

在编译插件中增加 Annotation Processor (注解处理器)

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>${version.maven-compiler}</version>
  <configuration>
    <source>17</source>
    <target>17</target>
    <annotationProcessorPaths>
      <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${version.lombok}</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

## 备注

### 1. 基于 VSCode 设置远程调试

1. 在侧边工具栏点击 "Run and Debug" 按钮, 点击其中的 "create a launch.json file" 链接, 在弹出的窗口中, 选择 Java 作为调试语言;

2. 在生成的 `launch.json` 文件中, 为 `configurations` 字段增加配置如下:

```json
{
  "type": "java",
  "name": "Attach to Remote Program",
  "request": "attach",
  "hostName": "localhost",
  "port": "5005"
}
```

其中远程配置的相关配置和 [启用远程调试](#12-启用远程调试) 章节中启用远程调试使用的配置必须一致

> VSCode 具备自动生成配置模板的功能, 所以只需通过代码提示并选择对应的项, 即可生成配置模板

回到 "Run and Debug" 界面, 选择最上方的 "RUN AND DEBUG" 下拉框, 选中 "Attach to Remote Program" 并点击绿色运行按钮, 即可连接到远程进行调试
