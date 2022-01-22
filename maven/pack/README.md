# 执行和打包

- [执行和打包](#执行和打包)
  - [1. 通过 maven 执行程序](#1-通过-maven-执行程序)
    - [1.1. 引入插件](#11-引入插件)
    - [1.2. 定义不同的执行入口](#12-定义不同的执行入口)
    - [1.3. 公共配置](#13-公共配置)
  - [2. 打包](#2-打包)
    - [2.1. Thin jar](#21-thin-jar)
    - [2.2. Fat jar](#22-fat-jar)
    - [2.2. Fat jar with shade](#22-fat-jar-with-shade)

## 1. 通过 maven 执行程序

> 参考 [插件主页](https://www.mojohaus.org/exec-maven-plugin/examples/example-exec-for-java-programs.html)

`org.codehaus.mojo:exec-maven-plugin` 插件用于通过 maven 执行程序，可以将一组命令行整合再 `pom.xml` 中，方便执行，并让命令具备一定跨平台执行能力

### 1.1. 引入插件

不在 `pom.xml` 中设置参数，参数由命令行引入

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.0.0</version>
</plugin>
```

此时，可以执行当前项目的入口类，例如：

```bash
$ mvn compile exec:java \
    -Dexec.mainClass="alvin.study.maven.Main" \
    -Dexec.args="12 13"
```

表示先执行 `compile` 指令，再执行 `exec:java` 指令

也可以执行任意进程，例如：

```bash
$ mvn compile exec:exec \
    -Dexec.executable="ls" \
    -Dexec.args="-al"
```

相当于执行 `ls -al` 命令

### 1.2. 定义不同的执行入口

`org.codehaus.mojo:exec-maven-plugin` 插件可以通过 `<executions>` 标签指定多个执行命令

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.0.0</version>
    <executions>
        <execution>
            <id>main</id>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>alvin.study.maven.Main</mainClass>
                <workingdir>${project.root.directory}</workingdir>
                <arguments>
                    <argument>12</argument>
                    <argument>13</argument>
                </arguments>
            </configuration>
        </execution>
        <execution>
            <id>dir</id>
            <goals>
                <goal>exec</goal>
            </goals>
            <configuration>
                <executable>ls</executable>
                <arguments>
                    <argument>-al</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

此时，可以通过不同的 `goal` 和 `@id` 执行不同的命令，例如：

执行当前项目入口类

```bash
$ mvn compile exec:java@main
```

执行查看目录命令

```bash
$ mvn compile exec:exec@dir
```

其中：

- `goal` 表示执行目标
  - `java` 表示执行当前项目的某个入口类，通过 `configuration` 标签配置 `mainClass` 和 `arguments`，即入口类名称和命令行参数
  - `exec` 表示启动任意进程，会另起一个进程执行。通过 `configuration` 标签配置 `executable` 和 `arguments`，即命令行命令和命令行参数

### 1.3. 公共配置

可以为 `executions` 下的所有的 `execute` 配置公共的参数（例如公共环境变量）

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.0.0</version>
    <executions>
        ...
    </executions>
    <configuration>
        <environmentVariables>
            <WORK_DIR>./target</WORK_DIR>
          </environmentVariables>
        </configuration>
    </configuration>
</plugin>
```

此时，`executions` 下的所有 `execute` 都共享这些配置

特别的，若当前项目只有一个入口类，也可以将入口类的配置放在公共部分

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.0.0</version>
    <executions>
        ...
    </executions>
    <configuration>
        <mainClass>alvin.study.maven.Main</mainClass>
        <workingdir>${project.root.directory}</workingdir>
        <arguments>
            <argument>12</argument>
            <argument>13</argument>
        </arguments>
    </configuration>
</plugin>
```

此时，直接通过 `$ mvn compile exec:java` 即可启动当前项目，而无需追加 `@id`

## 2. 打包

打包的目标是生成一个 `jar` 或 `war` 包。目前 `war` 包使用的较少了，主要是以 `jar` 包为主

maven 具备基本的打包功能，但仅是将项目产生的 `.class` 文件压缩到 `.jar` 文件中，很多必要的设置（如定义 `.MF` 文件）仍需要引入相关的插件来完成

通过不同的插件，可以进行不同类型的打包，常用的有如下几种类型

### 2.1. Thin jar

[maven-jar-plugin](https://maven.apache.org/plugins/maven-jar-plugin/index.html)
[maven-dependency-plugin](https://maven.apache.org/plugins/maven-dependency-plugin/index.html)

`maven-jar-plugin` 插件产生最小尺寸的 jar 包，只包含当前项目产生的 `.class` 文件，不包含引入的依赖。该 jar 包需要通过 `--classpath` 参数指定其它

如果项目不仅依赖于 JDK，可以通过 `maven-dependency-plugin` 将所需的依赖汇集到指定的位置，并将依赖声明增加在 manifest 声明中，以达到 jar 文件可以被运行的效果

`maven-jar-plugin` 定义

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>3.2.1</version>
    <configuration>
        <finalName>${project.artifactId}-${project.version}</finalName>
        <archive>
            <index>true</index>
            <manifest>
                <mainClass>alvin.study.maven.Main</mainClass>
                <addClasspath>true</addClasspath>
                <classpathPrefix>./lib/</classpathPrefix>
                <useUniqueVersions>false</useUniqueVersions>
            </manifest>
            <manifestEntries>
                <mode>development</mode>
                <url>${project.url}</url>
            </manifestEntries>
        </archive>
    </configuration>
</plugin>
```

- `finalName` 定义输出的 jar 文件名
- `archive` 打包配置
  - `manifest` 定义 jar 包中 `META-INF/MANIFEST.MF` 文件的内容
    - `mainClass` 改 jar 包执行时执行的入口类
    - `addClasspath` 将所需依赖加入到 `Class-Path` 配置项中
    - `classpathPrefix` 依赖所在路径的前缀
    - `useUniqueVersions` 是否为 jar 生成时间戳版本
  - `manifestEntries` 通过 key/value 方式将额外的信息加入 `META-INF/MANIFEST.MF` 文件

`maven-dependency-plugin` 定义

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <version>3.2.0</version>
    <executions>
        <execution>
            <id>copy</id>
            <phase>package</phase>
            <goals>
                <goal>copy-dependencies</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.build.directory}/lib</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- `<phase>package</phase>` 附加在 maven 的 `package` 指令上
- `<goal>copy-dependencies</goal>` 表示将依赖进行 copy 操作
- `outputDirectory` 依赖拷贝到的路径

通过如下命令即可进行打包

```bash
$ mvn compile package
```

### 2.2. Fat jar

[maven-assembly-plugin](https://maven.apache.org/plugins/maven-assembly-plugin/index.html)

所谓的 fat jar 即将当前项目的 `.class` 文件以及项目所依赖的 `.jar` 文件中包含的 `.class` 文件统一打包在输出的 `.jar` 文件中

这样打包出来的 `.jar` 文件尺寸会很大，但执行时只需依赖 JDK，比较方便

`maven-assembly-plugin` 插件配置如下

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <version>3.3.0</version>
    <configuration>
        <finalName>${project.artifactId}-${project.version}-fat</finalName>
        <appendAssemblyId>false</appendAssemblyId>
        <archive>
            <manifest>
                <mainClass>alvin.study.maven.Main</mainClass>
            </manifest>
        </archive>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
    </configuration>
    <executions>
        <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

- `finalName` 最终输出的 jar 文件名称
- `appendAssemblyId` 是否在输出的 jar 文件名后追加 `jar-with-dependencies` 部分

通过如下命令可完成打包：

```bash
$ mvn compile assembly:single
```

`assembly` 指令对应唯一 `goal` 为 `single`

由于配置文件中定义了 `execution` 并绑定到 `package` 指令上，所以如下命令也可以完成打包

```bash
$ mvn compile package
```

### 2.2. Fat jar with shade

`maven-assembly-plugin` 插件打包有可能会出现引用冲突的问题（例如引用的包也将依赖进行打包），此时应该使用 `maven-shade-plugin` 插件进行打包

`maven-shade-plugin` 在打包方面有更多的优势，一般情况下，优先选择 `maven-shade-plugin` 即可

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.2.4</version>
    <configuration>
        <finalName>${project.artifactId}-${project.version}-shade</finalName>
        <createDependencyReducedPom>false</createDependencyReducedPom>
    </configuration>
    <executions>
        <execution>
            <id>make-shade</id>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

- `createDependencyReducedPom` 是否保留生成的中间 `.pom` 文件

`maven-shade-plugin` 插件只有一个名为 `shade` 的 `goal`，可以通过 `phase` 绑定在 `package` 指令上

处理依赖冲突，则需要在插件的 `configuration` 标签中配置 `transformer` 对冲突的文件进行指定覆盖策略

```xml
<configuration>
    ...
    <transformers>
        <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
            <manifestEntries>
                <Main-Class>alvin.study.maven.Main</Main-Class>
            </manifestEntries>
        </transformer>
        <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
            <resource>META-INF/spring.handlers</resource>
        </transformer>
    </transformers>
</configuration>
```

- `org.apache.maven.plugins.shade.resource.ManifestResourceTransformer` 生成 `jar` 文件的 `META-INF/MANIFEST.MF` 文件内容
- `org.apache.maven.plugins.shade.resource.AppendingTransformer` 将符合指定文件名的多个文件内容进行合并，防止资源冲突
