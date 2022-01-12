# Maven 基础

## 1. 项目结构

```plain
my-app
|-- pom.xml
`-- src
    |-- main
    |   |-- java
    |   |   `-- com/company/app
    |   |       `-- App.java
    |   `-- resources
    |       `-- application.conf
    `-- test
        |-- java
        |   `-- com/company/app
        |       `-- AppTest.java
        `-- resources
            `-- application-test.conf
```

整个项目由最根部的 `pom.xml` 来进行整体配置

## 2. 基本配置

### 2.1. 配置文件结构

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <!--
    项目基本信息
    -->

    <properties>
    <!--
        项目属性
    -->
    </properties>

    <dependencyManagement>
    <!--
        项目整体依赖管理
    -->
    </dependencyManagement>

    <dependencies>
    <!--
        项目依赖项配置
    -->
    </dependencies>

    <build>
    <!--
        项目构建配置
    -->
    </build>
<project>
```

### 2.2. 项目基本信息

项目基本信息由以下几个标签组成

```xml
<groupId>...</groupId>
<artifactId>...</artifactId>
<version>1.0-SNAPSHOT</version>
<name>${project.artifactId}</name>
<packaging>jar</packaging>
<url>...</url>
```

- `groupId` 项目所在的组，一般是公司企业名称
- `artifactId` 项目本身的唯一 ID
- `version` 版本号
- `name` 项目名称，可以和 `artifactId` 一致
- `packaging` 项目打包方式，可以为 `war`, `jar`（默认值） 和 `pom`，其中 `pom` 表示下面还有子项目
- `url` 项目的网页地址，可省略

### 2.2. 项目属性

定义整个项目的基本属性，基本标签包括：

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>${maven.compiler.source}</maven.compiler.target>
</properties>
```

- `project.build.sourceEncoding` 源码文件字符集，默认 UTF-8
- `maven.compiler.source` 源码 JDK 版本
- `maven.compiler.target` 编译后字节码的 JDK 版本

### 2.3. 项目整体依赖管理

对依赖进行定义，主要是其版本。该标签下定义的依赖并不直接引入项目，但当前项目或子项目在引用依赖时，无需在明确版本号

以定义 `JUnit 5` 依赖为例，本例中引入 `Junit` 的 BOM 文件，即 Junit 所有相关组件的“物料清单”。一些成系列的依赖都具有 BOM 清单（例如 Spring 系列），便于引入

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.junit</groupId>
            <artifactId>junit-bom</artifactId>
            <version>5.8.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

- `type` 引入的依赖类型，可以为：
  - `pom` 引入依赖中定义的 `pom.xml` 文件（一般用来引入 BOM），以其内容作为依赖，例如对 `Junit` 的所有子项目进行依赖
  - `jar`（默认） 引入依赖中的 `jar` 文件
  - `war` 引入依赖中的 `war` 文件
- `scope` 引入的依赖在当前项目的使用范围
  - `import` 当 `type` 为 `pom`（一般用来引入 BOM）时，必须使用 `import`
  - `compile`（默认）在编译时使用，大部分依赖都会在编译时使用
  - `provided` 表示所依赖的库在运行时框架（或容器中）已经提供，只是在开发和测试时引入，打包时无需引入
  - `runtime` 表示该依赖在运行的时候是必须的，但在编译时无需编译。这个结果和 `compile` 非常类似，常用于一些只依赖于 JDK 接口的包（例如 JDBC Driver，编译时只依赖于 `java.sql` 中的接口，但运行时需要依赖具体的 JDBC 驱动包）
  - `test` 表示该依赖只被测试需要，在打包时，如果不打包测试代码，则不会引入该依赖

### 2.4. 依赖引用

定义实际要被引入的依赖，如果在 `<dependencyManagement>` 中已经定义了该依赖，则这里只需明确依赖的 `<groupId>` 和 `<artifactId>` 即可，无需指定 `<scope>` 和 `<version>` 标签

特别的，对于 `<dependencyManagement>` 引入的是一个 BOM（`<type>pom</type>`）时，引入依赖不能忽略 `<scope>` 标签

以引入 `JUnit` 测试框架为例，一般情况下应定义如下：

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.8.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

但因为在 `<dependencyManagement>` 中声明了 `Junit` 的 BOM 引用，所以整个 `Junit` 依赖都已被定义，此时引用无需在特别关注版本

```xml
<dependencies>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2.5. 项目构建配置

[maven-compiler-plugin](https://maven.apache.org/plugins/maven-compiler-plugin/index.html)
[maven-surefire-plugin](https://maven.apache.org/surefire/maven-surefire-plugin/index.html)

指定构建项目所用的插件集合。一般来说，一个插件即对应一组**指令**，用于对项目进行某种构建（例如：编译，资源处理，打包等等）

常用的构建插件如下：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.1</version>
            <configuration>
                <source>11</source>
                <target>11</target>
                <encoding>UTF-8</encoding>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>2.22.2</version>
        </plugin>
    </plugins>
</build>
```

- `maven-compiler-plugin` 增加 `$ mvn compile` 指令，用来编译整个项目
- `maven-surefire-plugin` 增加 `$ mvn test` 指令，执行单元测试
