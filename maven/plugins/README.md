# 常用插件

- [常用插件](#常用插件)
  - [1. Checkstyle 插件](#1-checkstyle-插件)
    - [1.1. 代码检查](#11-代码检查)
      - [1.1.1. 配置插件](#111-配置插件)
      - [1.2. 执行代码检查](#12-执行代码检查)
    - [1.2. 生成报告](#12-生成报告)
      - [1.2.1. 项目网站信息生成插件](#121-项目网站信息生成插件)
      - [1.2.2. 配置报告插件](#122-配置报告插件)
      - [1.2.3. 在报告中生成源码链接](#123-在报告中生成源码链接)
  - [2. SpotBugs 插件](#2-spotbugs-插件)

## 1. Checkstyle 插件

[`maven-checkstyle-plugin`](https://maven.apache.org/plugins/maven-checkstyle-plugin/index.html)

`maven-checkstyle-plugin` 用于对代码进行静态检查，找到其中不符合编码规范的部分，该插件有两部分：进行代码检查和生成检查报告

### 1.1. 代码检查

#### 1.1.1. 配置插件

在插件中增加 checkstyle 插件

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-checkstyle-plugin</artifactId>
            <version>${version.maven-checkstyle}</version>
            <dependencies>
                <dependency>
                    <groupId>com.puppycrawl.tools</groupId>
                    <artifactId>checkstyle</artifactId>
                    <version>${version.checkstyle}</version>
                </dependency>
            </dependencies>
            <configuration>
                <configLocation>checkstyle.xml</configLocation>
                <encoding>UTF-8</encoding>
                <consoleOutput>true</consoleOutput>
                <failsOnError>true</failsOnError>
                <linkXRef>true</linkXRef>
            </configuration>
            <executions>
                <execution>
                    <id>validate</id>
                    <phase>validate</phase>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

- `dependency` 设置插件依赖的 checkstyle 主体
- `configLocation` 设置检查规则文件路径
- `consoleOutput` 检查结果是否输出在控制台
- `failsOnError` 如果发现错误则停止构建
- `linkXRef` 错误信息连接到源代码

插件的 `goal` 为：

- `checkstyle:check` 执行 checkstyle 并将错误输出到控制台，根据配置可能会导致构建失败
- `checkstyle:checkstyle` 执行 checkstyle 并尝试生成报告
- `checkstyle:checkstyle-aggregate` 在多模块项目中执行所有的 checkstyle 并统一生成报告
- `checkstyle:help` 显示帮助信息

#### 1.2. 执行代码检查

```bash
$ mvn checkstyle:check
```

或

```bash
$ mvn checkstyle:checkstyle
```

输出

```plain
[INFO] --- maven-checkstyle-plugin:3.1.2:check (default-cli) @ study-maven-plugins ---
[INFO] Starting audit...
[ERROR] /home/alvin/Workspaces/Study/study-devops/maven/plugins/src/main/java/alvin/study/maven/invalid/style/SealClass.java:3:1: Utility classes should not have a public or default constructor. [HideUtilityClassConstructor]
[ERROR] /home/alvin/Workspaces/Study/study-devops/maven/plugins/src/main/java/alvin/study/maven/invalid/style/SealClass.java:4:19: Redundant 'final' modifier. [RedundantModifier]
Audit done.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
```

表示有两处代码样式不符合要求，并给出原因

另外，配置中将 `goal check` 绑定到 `validate` 指令上，所以在 `$ mvn compile` 时即可以自动执行 Checkstyle

### 1.2. 生成报告

可以通过 `$ mvn site` 生成当前代码的网站内容，网站内容可以包括 JavaDoc，Test Report，Checkstyle Report 等

#### 1.2.1. 项目网站信息生成插件

引入以下插件，用来为当前项目产生网站信息

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-site-plugin</artifactId>
            <version>${version.maven-site}</version>
        </plugin>
    </plugins>
</build>
```

之后即可在 `<reporting>` 标签下定义各类报告生成的规格

产生代码网站

```bash
$ mvn site
```

#### 1.2.2. 配置报告插件

[`maven-jxr-plugin`](https://maven.apache.org/jxr/maven-jxr-plugin/index.html)

`maven-checkstyle-plugin` 插件同时可以用作报告插件，配置如下：

```xml
<reporting>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-checkstyle-plugin</artifactId>
            <version>${version.maven-checkstyle}</version>
            <reportSets>
                <reportSet>
                    <reports>
                        <report>checkstyle</report>
                    </reports>
                </reportSet>
            </reportSets>

            <configuration>
                <configLocation>checkstyle.xml</configLocation>
            </configuration>
        </plugin>
    </plugins>
</reporting>
```

此时通过 `$ mvn site` 即可在生成的报告中加入 Checkstyle 报告

若 Checkstyle 插件绑定了其它任务，例如 `validate`，则需要在执行命令时跳过 Checkstyle 任务，否则会由于无法通过代码检测而中断生成报告的任务

```bash
$ mvn site -Dcheckstyle.skip
```

#### 1.2.3. 在报告中生成源码链接

[`maven-jxr-plugin`](https://maven.apache.org/jxr/maven-jxr-plugin/index.html)

```xml
<reporting>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jxr-plugin</artifactId>
            <version>${version.maven-jxr}</version>
        </plugin>
    </plugins>
</reporting>
```

此时，报告中出现文件名和行数的地方都会生成连接到源码的超链接

## 2. SpotBugs 插件

[`spotbugs-maven-plugin`](https://spotbugs.github.io/spotbugs-maven-plugin/index.html)

SpotBugs 用于取代已过时的 FindBugs 插件
