# 执行和打包

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

打包的目标是生成一个 `jar` 或
