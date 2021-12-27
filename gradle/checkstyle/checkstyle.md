# Checkstyle

- [Checkstyle](#checkstyle)
  - [1. 安装和配置插件](#1-安装和配置插件)
    - [1.1. 安装插件](#11-安装插件)
    - [1.2. 配置插件](#12-配置插件)
  - [2. 使用插件](#2-使用插件)
    - [2.1. 定义检查 task](#21-定义检查-task)
    - [2.2. 执行 task](#22-执行-task)
  - [3. 检查规则](#3-检查规则)
    - [3.1. 检查规则文件](#31-检查规则文件)
    - [3.2. 过滤列表](#32-过滤列表)

## 1. 安装和配置插件

> [插件介绍页面]([checkStyle](https://docs.gradle.org/current/userguide/checkstyle_plugin.html))

### 1.1. 安装插件

`checkstyle` 是一个 gradle 官方插件，直接引入即可

```groovy
plugins {
    id 'java'
    id 'checkstyle'  // 引入插件
}
```

### 1.2. 配置插件

```groovy
checkstyle {
    toolVersion = '9.0.1' // 工具链版本
    showViolations true  // 显示验证规则

    reports {
        xml.required = false
        html.required = true
        // html.stylesheet resources.text.fromFile('config/xsl/checkstyle-custom.xsl')
    }
}
```

`checkstyle` 插件引入了 `checkstyleMain` 和 `checkstyleTest` 两个 task，分别用于对 `main.java` 和 `test.java` 两个位置的代码进行检测

需要对这两个 task 进行配置，主要是指定**规则文件的路径**

```groovy
// 对 main.java 配置检查
checkstyleMain {
    outputs.upToDateWhen { false }
    configFile = file("${project.projectDir}/config/checkstyle.xml") // 指定检查规则文件
}

// 对 test.java 配置检查
checkstyleTest {
    outputs.upToDateWhen { false }
    configFile = file("${project.projectDir}/config/checkstyle-test.xml") // 指定检查规则文件
}
```

## 2. 使用插件

### 2.1. 定义检查 task

```groovy
// 注意 task 名称需要和 checkstyle 插件有所区别
task checkStyle(dependsOn: [checkstyleMain, checkstyleTest]) {
}
```

定义一个同时依赖 `checkstyleMain` 和 `checkstyleTest` 的 task，可以一次性完成所有检测

### 2.2. 执行 task

`checkstyle` 插件引入后，`check` task 会依赖 `checkstyleMain` 和 `checkstyleTest` 两个 task，而 `build` 任务又会依赖 `check` task，所以很多 task 都会导致 `checkstyle` task 执行

```bash
$ gradle checkStyle
$ gradle checkstyleMain
$ gradle checkstyleTest
$ gradle check
$ gradle build
```

## 3. 检查规则

> [配置 checkstyle](https://checkstyle.org/config.html)

### 3.1. 检查规则文件

> [检查规则配置](https://checkstyle.org/checks.html)

checkstyle 的检查规则是通过一组 `module` 组合完成的，最顶部的是一个 `Checker` module，用于包含一组对代码文件整体检测的 module，其中的 `TreeWalker` module 用于包含一组对语法进行进一步检测的 module

### 3.2. 过滤列表

> [文件过滤器模块](https://checkstyle.org/config_filefilters.html)
> [规则过滤器模块](https://checkstyle.org/config_filters.html)

如果要过滤部分文件不参与代码检测，则使用 `BeforeExecutionExclusionFileFilter` module 即可

```xml
<module name="BeforeExecutionExclusionFileFilter">
    <!-- 忽略所有的 module-info.java 文件 -->
    <property name="fileNamePattern" value="module\-info\.java$"/>
</module>
```

规则过滤一般是通过 `SuppressionFilter` 的过滤器 module 以及一个 `suppressions.xml` 来定义

在 `Checker` module 下，定义 `SuppressionFilter` module，引入忽略列表

```xml
<module name="Checker">
    ...
    <module name="SuppressionFilter">
        <!-- 引入当前目录的 suppressions.xml 文件 -->
        <property name="file" value="${config_loc}/suppressions.xml"/>
    </module>
</module>
```

对于 `suppressions.xml` 文件，需要说明那些文件忽略那些规则

```xml
<?xml version="1.0"?>

<!DOCTYPE suppressions PUBLIC
  "-//Puppy Crawl//DTD Suppressions 1.1//EN"
  "http://www.puppycrawl.com/dtds/suppressions_1_1.dtd">

<suppressions>
    <suppress files="Main.java" checks="."/>
    <suppress files="Versions.java" checks="."/>
</suppressions>
```
