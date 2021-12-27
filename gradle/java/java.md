# Java Project

- [Java Project](#java-project)
  - [1. 根项目和子项目](#1-根项目和子项目)
  - [2. `java` 插件](#2-java-插件)
    - [2.1. `sourceSets` 变量](#21-sourcesets-变量)
    - [2.2. 设置自定义 `sourceSets`](#22-设置自定义-sourcesets)
    - [2.3. 设置编译参数](#23-设置编译参数)
    - [2.4. 执行 main 方法](#24-执行-main-方法)
    - [2.5. 构建项目](#25-构建项目)
    - [2.6. 构建 FatJar](#26-构建-fatjar)
    - [2.7. 清理生成的文件](#27-清理生成的文件)
  - [3. 单元测试](#3-单元测试)
    - [3.1. 配置 Junit 测试框架](#31-配置-junit-测试框架)
    - [3.2. 执行单元测试](#32-执行单元测试)

## 1. 根项目和子项目

一个 gradle 项目可以包含多个子项目，结构如下：

```plain
root_project
  ├── sub_project1
  │    └── build.gradle
  ├── sub_project2
  │    └── build.gradle
  ├── build.gradle
  └── settings.gradle
```

多少子项目生效由 `settings.gradle` 文件定义

```groovy
rootProject.name = 'study-java'

include( // 包含的子项目名称
    'sub_project1',
    'sub_project2'
)
```

## 2. `java` 插件

### 2.1. `sourceSets` 变量

`java` 插件会引入变量 `sourceSets`，是一系列项目相关路径的集合，符合 maven 规范

- `src/main/java` main 代码路径
- `src/main/resources` main 资源路径
- `src/main/output` main 编译结果输出
- `src/test/java` test 测试代码路径
- `src/test/resources` test 测试资源路径
- `src/test/output` test 编译结果输出路径

整个结构为：

```plain
source-sets
  ├── src
  │    ├── main
  │    │    ├── java
  │    │    │   ├── Main.java
  │    │    │   └── Lib.java
  │    │    └── resources
  │    └── test
  │         ├── java
  │         │   └── MainTest.java
  │         └── resources
  └── build.gradle
```

`srcDirs` 代表源代码路径集合，可以获取不同层级的源代码路径集合

- `allJava.srcDirs` 所有 java 代码的源码路径集合
- `java.srcDirs` **.java 下的源代码路径集合
- `resources.srcDirs` **.resources 下的源代码路径

例如可以获取如下路径：

- `sourceSets[0].allJava.srcDirs`
- `sourceSets.main.allJava.srcDirs`
- `sourceSets.main.java.srcDirs`
- `sourceSets.test.resource.srcDirs`
- ...

`output` 表示编译结果存放路径，包括 `classesDirs` 和 `resourcesDir`，分别代表类文件输出路径和资源文件输出路径

- `sourceSets.main.output.classesDirs.files` 获取 src/main 下所有类输出路径
- `sourceSets.main.output.resourcesDir` 获取 src/main 下资源输出路径

```bash
$ gradle -q --offset showProjectDir
```

`compileClasspath`, `runtimeClasspath` 分别表示编译期和运行期的 classpath 定义，包含了 `.class` 文件路径和 `.jar` 文件路径

- `sourceSets.main.compileClasspath.files` 编译期 classpath 集合
- `sourceSets.main.runtimeClasspath.files` 运行期 classpath 集合

### 2.2. 设置自定义 `sourceSets`

除过默认的 java, test 代码路径，可以添加自定义的代码路径，例如添加一个 `boot` 作为源码路径：

```plain
source-sets
  ├── src
  │    ├── main
  │    │   ├── java
  │    │   │   └── Main.java
  │    │   └── resources
  │    ├── test
  │    │   ├── java
  │    │   │   └── Main.java
  │    │   └── resources
  │    └── boot
  │        ├── java
  │        │   └── Bootloader.java
  │        └── resources
  └── build.gradle
```

只需为 `sourceSets` 添加新项目即可

```groovy
sourceSets {
    boot {
        java {
        }
        resources {
        }
    }
}
```

这样一来，`src/boot/java` 和 `src/boot/resources` 路径会被添加到 `sourceSets`

也可以定义明确的路径

```groovy
sourceSets {
    boot {
        java {
            srcDirs("src/boot/java")
        }
        resources {
            srcDirs("src/boot/resources")
        }
    }
}
```

设置和获取 `sourceSets` 变量值

```bash
$ gradle -q --offline showSourceSets
```

### 2.3. 设置编译参数

通过 `JavaCompile` task 可以设置 java 的编译选项

```groovy
tasks.withType(JavaCompile) {
    options.compilerArgs += ['-Xdoclint:none', '-Xlint:none', '-nowarn']
}
```

在执行和编译相关的 tasks 时，编译选项会按上述设置定义

### 2.4. 执行 main 方法

可以通过 gradle 直接执行 `main` 方法启动 java 程序，无需打包过程，即可快速的测试程序

运行程序需要知道 `main` 方法所在的**包**和**类**，需要对 `JavaExec` 类型的 task 进行配置

```groovy
task runMain(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath // 设置 classpath 参数
    main = 'alvin.gradle.java.Main' // 设置 main 方法所在的类
    args 'A', 'B', 'C'  // 传递给 main 方法的默认参数
}
```

使用默认参数执行 main 方法

```bash
$ gradle runMain
```

通过命令行传递参数

```bash
$ gradle runMain --args="X Y Z"
```

除了使用 `JavaExec` 类型任务外，也可以使用 `Exec` 类型任务，但该类型任务并不是专用于 java 项目的，所以需要设置启动项目的命令行

```groovy
task runWithExecJar(type: Exec) {
    dependsOn jar

    group = project.group
    description = "Run the output executable jar with ExecTask"
    commandLine "java", "-jar", jar.archiveFile.get(), "A B C"
}
```

这种方式本质上是将 java 项目打包为可执行 jar 文件后，通过命令行执行，所以需要配置 `jar` 任务正确完成打包，另外 `runWithExecJar` 代码必须要出现在 `jar` 任务之后，否则无法获取正确的 `jar` 打包参数

`jar` 任务配置方法见 [2.5. 构建项目](#25-构建项目)

### 2.5. 构建项目

`build` task 是插件内置的任务，依赖 `assemble`，`classes`，`buildDependents`，`check` 和 `jar` 这几个 task

构建完毕后，会在 `build/libs` 输出 jar 文件，该 jar 文件的输出可以通过改变 `jar` 任务的配置改变

```groovy
jar {
    manifest { // Manifest.MF 文件生成规则
        attributes(
            'Implementation-Title': 'Gradle Demo', // 项目标题
            'Implementation-Version': version,  // 版本号
            'Main-Class': 'alvin.gradle.java.Main', // 主类名
            "Class-Path": makeClassPath() // 依赖 classpath 路径
        )
    }

    // jar 打包文件名
    archiveFileName = "${project.group}.${project.name}-${project.version}.jar"
}
```

默认情况下，构建的 jar 文件只包含当前项目的所有 `.class` 文件，并不包含依赖的 `.jar` 文件，所以需要将 `.jar` 文件也输出到构建目录中

```groovy
task copyJarsToLib {
    def toDir = "${buildDir}/libs/libs"
    doLast {
        // 将运行时依赖拷贝到 build/libs/libs 目录下
        copy {
            from configurations.runtimeClasspath
            into toDir
        }
    }
}

jar.dependsOn copyJarsToLib // 令 jar 依赖于 copyJarsToLib
```

并在构建的 jar 包的 `Manifest.MF` 中，声明 classpath 路径

```groovy
def makeClassPath() {
    // 将 runtimeClasspath 的路径放入集合
    def jarNames = configurations.runtimeClasspath.collect {
        it.getName()
    }
    // 获取 classpath
    return '. libs/' + jarNames.join(' libs/')
}
```

如此一来，`build` task 可在 `build/libs` 下输出 jar 文件，在 `build/libs/libs` 下输出所有依赖的 jar 文件

```bash
$ gradle build
```

运行编译结果

```bash
$ java -jar build/libs/alvin.gradle.java-1.0.jar arg1 arg2
```

### 2.6. 构建 FatJar

上节中构建的 jar 文件称为 "ThinJar"，即不包含依赖，只包含当前项目本身编译出的 `.class` 文件，所以要正确运行一个 "ThinJar"，仍需要使用 `--classpath` 参数定位依赖所在的路径

而与之对应的 "FatJar" 则是把所有依赖的 `.class` 文件都打包到一个 jar 文件中，只需要该 jar 文件即可正确的运行，为应用的打包和分发提供了便利

通过定义一个 `Jar` 类型的 task，并增加对依赖的打包配置，即可完成 FatJar 的打包

```groovy
task fatJar(type: Jar) {
    // 打包结果中排除如下文件，会导致冲突
    exclude 'META-INF/*.SF', 'META-INF/*.DSA', 'META-INF/*.RSA', 'META-INF/*.MF'

    manifest { // Manifest.MF 文件生成规则
        attributes(
            'Implementation-Title': 'Gradle Demo',
            'Implementation-Version': project.version,
            'Main-Class': 'alvin.gradle.java.Main',
        )
    }

    // 打包文件名
    archiveFileName = "${project.group}.${project.name}-${project.version}-fat.jar"

    // 将运行时所需的 jar 文件解压缩后放入 jar 包
    from {
        configurations.runtimeClasspath.collect {
            it.isDirectory() ? it : zipTree(it)
        }
    }
    with jar
}
```

运行编译结果

```bash
$ java -jar build/libs/alvin.gradle.java-1.0-fat.jar arg1 arg2
```

### 2.7. 清理生成的文件

构建过程中产生的文件可以清理

```bash
$ gradle -q clean
```

## 3. 单元测试

以 JUnit5 为例，在 gradle 中可以配置测试任务的属性

### 3.1. 配置 Junit 测试框架

首先需要对 `test` task 进行配置，选择测试框架，配置测试框架参数

```groovy
test {
    useJUnitPlatform() // 使用 JUnit5 测试框架

    reports.html.required = true // 输出 HTML 测试报告

    // 配置测试日志输出类型
    testLogging {
        events "SKIPPED", "FAILED"  // 输出 log 的日志状态
        showStackTraces = true // 显示堆栈信息
        exceptionFormat = "full" // 异常信息格式化程度
    }
}
```

使用 JUnit5，需要引入 JUnit5 依赖

```groovy
dependencies {
    ...

    // 单元测试依赖
    testImplementation "org.junit.jupiter:junit-jupiter-api:5.7.2",
                       "org.junit.jupiter:junit-jupiter-engine:5.7.2",
                       dependencies.create("org.hamcrest:hamcrest-all:1.3") {
                           exclude group: 'junit' // 排除 JUnit4 依赖
                           exclude group: 'org.junit.vintage'
                       },
                       dependencies.create("com.google.guava:guava-testlib:31.0.1-jre") {
                           exclude group: 'junit' // 排除 JUnit4 依赖
                           exclude group: 'org.junit.vintage'
                       }

    // 单元测试编译期依赖
    testCompileOnly "org.junit.platform:junit-platform-launcher:1.7.2"
}
```

### 3.2. 执行单元测试

执行单元测试

```bash
$ gradle test
```

执行指定的单元测试

```bash
$ gradle test --tests <pattern>
```

例如

```bash
$ gradle test --tests '*shouldCommandLineArgsFormatted*'
```
