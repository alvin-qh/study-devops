# Java Project

- [Java Project](#java-project)
  - [1. 根项目和子项目](#1-根项目和子项目)
  - [2. `java` 插件](#2-java-插件)
    - [2.1. `sourceSets` 变量](#21-sourcesets-变量)
    - [2.2. 设置自定义 `sourceSets`](#22-设置自定义-sourcesets)
    - [2.3. 构建项目](#23-构建项目)
    - [2.4. Run java main method](#24-run-java-main-method)
    - [2.5. Make thin jar package (exclude thirdpart jar libaraies)](#25-make-thin-jar-package-exclude-thirdpart-jar-libaraies)
      - [2.5.1. Make jar package](#251-make-jar-package)
      - [2.5.2. Show package output and thirdpart jar libs](#252-show-package-output-and-thirdpart-jar-libs)
      - [2.5.3. Run jar package](#253-run-jar-package)
      - [2.5.4. Delete all outputs](#254-delete-all-outputs)
    - [2.6. Make fat jar package (include all thirdpart jar libaraies)](#26-make-fat-jar-package-include-all-thirdpart-jar-libaraies)
      - [2.6.1 Build fat jar](#261-build-fat-jar)
      - [2.6.2. Show build output](#262-show-build-output)
      - [2.6.3. Run fat jar](#263-run-fat-jar)
    - [2.4. 清理生成的文件](#24-清理生成的文件)

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

### 2.3. 构建项目

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

### 2.4. Run java main method
./gradlew runMain -q
### 2.5. Make thin jar package (exclude thirdpart jar libaraies)
#### 2.5.1. Make jar package
./gradlew jar -q
#### 2.5.2. Show package output and thirdpart jar libs
ls build/libs
ls build/libs/libs
#### 2.5.3. Run jar package
java -jar build/libs/alvin.gradle.java-1.0.jar a b c
#### 2.5.4. Delete all outputs
./gradlew clean -q
### 2.6. Make fat jar package (include all thirdpart jar libaraies)

> Building fat jar is "unpacking of third-party jars and package all `.class` file into the target jar"
#### 2.6.1 Build fat jar
./gradlew fatjar -q
#### 2.6.2. Show build output
ls build/libs
#### 2.6.3. Run fat jar
java -jar build/libs/alvin.gradle.java-1.0-fat.jar 1 2 3

### 2.4. 清理生成的文件

构建过程中产生的文件可以清理

```bash
$ gradle -q clean
```
