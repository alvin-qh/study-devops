# Task

- [Task](#task)
  - [1. 定义 Task](#1-定义-task)
    - [1.1. 定义基本 Task](#11-定义基本-task)
    - [1.2. Task 间依赖](#12-task-间依赖)
    - [1.3. 动态 Task](#13-动态-task)
    - [1.4. 为已有 task 设置依赖](#14-为已有-task-设置依赖)
    - [1.5. 使用函数](#15-使用函数)
    - [1.6. 默认任务](#16-默认任务)
    - [1.7. Task 调用链](#17-task-调用链)
    - [1.8. 使用 Java 包](#18-使用-java-包)
  - [2. gradle 命令行](#2-gradle-命令行)
    - [2.1. 显示所有 task](#21-显示所有-task)
    - [2.2. 显示调试信息](#22-显示调试信息)
    - [2.3. 构建 cache](#23-构建-cache)

## 1. 定义 Task

### 1.1. 定义基本 Task

gradle 任务的定义 DSL 为：

```groovy
task <taskName> {
  dependsOn "<dependencyTask>"  // 所依赖的其它任务，在被依赖任务执行结束后执行

  // 在当前任务执行之初执行
  doFirst {
    // ...
  }

  // 在所有 doFirst 执行完毕后执行
  doLast {
    // ...
  }
}
```

执行 gradle task

```bash
gradle -q --offline simpleTask
```

### 1.2. Task 间依赖

在任务定义中，`dependsOn` 表达了 task 之间的依赖关系

```groovy
task task1 {
  // ...
}

task task2 {
  dependsOn "task1" // 当前任务依赖于 task1，所以 task1 先被执行
  // ...
}
```

任务间依赖

```bash
gradle -q --offline dependTask
```

### 1.3. 动态 Task

可以动态设置任务名称，利用脚本动态生成 task

```groovy
4.times { num ->
  task "dynamicTask${num}" {  // 动态生成任务名
    doLast {
      println "This is dynamic task ${num}"
    }
  }
}
```

执行动态任务

```bash
for i in {0..3}; do \
  gradle -q --offline dynamicTask$i; \
done;
```

### 1.4. 为已有 task 设置依赖

已有如下 4 个 task，分别为 `taskDepends0` ~ `taskDepends3`

```groovy
4.times { num ->
  task "taskDepends${num}" {
    doLast {
      println "This is dynamic task ${num}"
    }
  }
}
```

设置这些 task 的依赖关系

```groovy
taskDepends1.dependsOn taskDepends3
taskDepends0.dependsOn taskDepends1, taskDepends2
```

执行依赖链的第一个任务

```bash
gradle -q --offline taskDepends0
```

### 1.5. 使用函数

gradle 中可以使用 `groovy` 语法定义的函数，函数定义如下

```groovy
File[] fileList(String dir) {
  file(dir).listFiles({ file -> file.isFile() } as FileFilter).sort()
}
```

之后即可在 task 中调用这些函数

```groovy
task loadfile {
  doLast {
    fileList('./').each { file ->
      ant.loadfile(srcFile: file, property: "fl_${file.name}")
      println "\n>>>>\nFile \"${file.name}\" was found:\n${ant.properties["fl_${file.name}"].substring(0, 10)}...\n<<<<"
    }
  }
}

task checksum {
  doLast {
    fileList('./').each { file ->
      ant.checksum(file: file, property: "cs_${file.name}")
      println "${file.name} - checksum: ${ant.properties["cs_${file.name}"]}"
    }
  }
}
```

在 task 中调用函数

```bash
gradle -q --offline loadfile
gradle -q --offline checksum
```

### 1.6. 默认任务

默认任务指的是在不指定具体任务名称时，执行的任务

```groovy
defaultTasks "<task name>"
```

执行默认 task

```bash
gradle -q --offline
```

### 1.7. Task 调用链

Task 调用链包含了一次执行的 task 以及该 task 依赖的其它 task，可以根据被执行的 task 进行一些额外的操作

```groovy
// 获取 task 调用链
gradle.taskGraph.whenReady { g ->
  if (g.hasTask(":release")) { // 判断调用链中是否有名为 release 的 task
    // ...
  }
}
```

通过调用链进行操作

```bash
gradle -q --offline distribution
gradle -q --offline release
```

### 1.8. 使用 Java 包

要在 gradle 中使用 Java 包，需要在 `buildscript` 设置中，设置所需的 JDK 依赖

增加外部 JDK 依赖

```groovy
buildscript {
  dependencies {
    classpath "commons-codec:commons-codec:1.11" // 引入 apache common codec 包
  }
}
```

引入依赖包后，即可以通过 `import` 导入包

```groovy
import org.apache.commons.codec.binary.Base64
```

此时即可在 gradle 中使用 `Base64` 类

```bash
gradle -q --offline base64encode
```

> 注意: 第一次执行 `base64encode` 任务前要执行一次 `gradle build`, 以加载 `org.apache.commons.codec.binary.Base64` 依赖 (注意, 不能用 `--offset` 参数)

## 2. gradle 命令行

### 2.1. 显示所有 task

```bash
gradle -q --offline tasks --all
```

- `-q` 只显示关键信息和错误 log
- `--offline` 不进行网络操作，在无需网络操作的情况可以加快速度
- `--all` 显示所有任务

### 2.2. 显示调试信息

```bash
gradle <task name> --info;
gradle <task name> --debug;
gradle <task name> --stacktrace;
```

- `--info, --debug, --stacktrace` 显示更详细的 log 信息

### 2.3. 构建 cache

使用构建 cache 可以加快 gradle 执行速度

方式1. 通过 `gradle.properties` 文件配置

```ini
org.gradle.caching=true # true 启用 cache（默认），false 禁用 cache
```

方式2. 通过命令行参数

```bash
gradle <task name> --build-cache; # 启用 cache
gradle <task name> --no-build-cache;  # 禁用 cache
```
