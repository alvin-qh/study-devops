# 常用插件

- [常用插件](#常用插件)
  - [1. Checkstyle 插件](#1-checkstyle-插件)
    - [1.1. 代码检查](#11-代码检查)
      - [1.1.1. 配置插件](#111-配置插件)
      - [1.1.2. 执行代码检查](#112-执行代码检查)
      - [1.2.3. 忽略插件](#123-忽略插件)
    - [1.2. 生成报告](#12-生成报告)
      - [1.2.1. 项目网站信息生成插件](#121-项目网站信息生成插件)
      - [1.2.2. 配置报告插件](#122-配置报告插件)
      - [1.2.3. 在报告中生成源码链接](#123-在报告中生成源码链接)
  - [2. SpotBugs 插件](#2-spotbugs-插件)
    - [2.1. 检查代码](#21-检查代码)
      - [2.1.1. 配置插件](#211-配置插件)
      - [2.1.2. 执行代码检查](#212-执行代码检查)
    - [2.2. 生成报告](#22-生成报告)
      - [2.2.3. 忽略插件](#223-忽略插件)

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
            <!--
            <executions>
                <execution>
                    <id>validate</id>
                    <phase>validate</phase>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
            -->
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

可以在 `executions` 标签中配置 `check goal` 和 `validate` 任务的关联，这样在执行 `$ mvn validate` 的时候同时执行 `checkstyle:check`

#### 1.1.2. 执行代码检查

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

#### 1.2.3. 忽略插件

可以在通过 `-Dcheckstyle.skip=true` 跳过插件，以防止因代码样式的原因打断构建过程，例如：

```bash
$ mvn clean compile -Dcheckstyle.skip=true
```

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

```bash
$ mvn site
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

SpotBugs 用于取代已过时的 FindBugs 插件，目标是对代码进行静态检查，找出代码中的隐含缺陷和安全缺陷

### 2.1. 检查代码

#### 2.1.1. 配置插件

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.spotbugs</groupId>
            <artifactId>spotbugs-maven-plugin</artifactId>
            <version>${version.maven-spotbugs}</version>
            <dependencies>
                <dependency>
                    <groupId>com.github.spotbugs</groupId>
                    <artifactId>spotbugs</artifactId>
                    <version>${version.spotbugs}</version>
                </dependency>
            </dependencies>
            <configuration>
                <encoding>UTF-8</encoding>
                <consoleOutput>true</consoleOutput>
                <failsOnError>true</failsOnError>
                <linkXRef>true</linkXRef>
            </configuration>
            <!--
            <executions>
                <execution>
                    <id>spotbugs-check</id>
                    <phase>compile</phase>
                    <goals>
                        <goal>check</goal>
                    </goals>
                </execution>
            </executions>
            -->
        </plugin>
    </plugins>
</build>
```

- `dependency` 设置插件依赖的 spotbugs 主体
- `configLocation` 设置检查规则文件路径
- `consoleOutput` 检查结果是否输出在控制台
- `failsOnError` 如果发现错误则停止构建
- `linkXRef` 错误信息连接到源代码

插件的 `goal` 为：

- `spotbugs:check` 执行 spotbugs 并将错误输出到控制台，根据配置可能会导致构建失败
- `spotbugs:spotbugs` 执行 spotbugs 并尝试生成报告
- `spotbugs:gui` 通过可视化 UI 显示错误信息
- `spotbugs:help` 显示帮助信息

可以在 `executions` 标签中配置 `check goal` 和 `compile` 任务的关联，这样在执行 `$ mvn compile` 的时候同时执行 `spotbugs:check`

#### 2.1.2. 执行代码检查

```bash
$ mvn compile spotbugs:check
```

或

```bash
$ mvn compile spotbugs:spotbugs
```

注意，`spotbugs-maven-plugin` 插件必须工作在 `.class` 文件上，所以必须先执行编译任务。输出

```plain
[INFO] --- spotbugs-maven-plugin:4.5.2.0:check (default-cli) @ study-maven-plugins ---
[INFO] BugInstance size is 1
[INFO] Error size is 0
[INFO] Total bugs: 1
[ERROR] Medium: Dead store to a in alvin.study.maven.invalid.bugs.UselessClass.run() [alvin.study.maven.invalid.bugs.UselessClass] At UselessClass.java:[line 5] DLS_DEAD_LOCAL_STORE
[INFO]


To see bug detail using the Spotbugs GUI, use the following command "mvn spotbugs:gui"
```

表示有一处代码有隐含的缺陷，需要修正

### 2.2. 生成报告

```xml
<reporting>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-jxr-plugin</artifactId>
            <version>${version.maven-jxr}</version>
        </plugin>

        <plugin>
            <groupId>com.github.spotbugs</groupId>
            <artifactId>spotbugs-maven-plugin</artifactId>
            <version>${version.maven-spotbugs}</version>
        </plugin>
    </plugins>
</reporting>
```

通过 `$ mvn compile site` 命令可生成代码精通检查报告

#### 2.2.3. 忽略插件

可以在通过 `-Dspotbugs.skip=true` 跳过插件，以防止因代码样式的原因打断构建过程，例如：

```bash
$ mvn clean compile -Dcheckstyle.skip=true
```