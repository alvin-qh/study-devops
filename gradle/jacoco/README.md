# JaCoCo Plugin

- [JaCoCo Plugin](#jacoco-plugin)
  - [1. 使用插件](#1-使用插件)
    - [1.1. 安装和配置](#11-安装和配置)
      - [引入插件](#引入插件)
      - [配置插件](#配置插件)
  - [2. 执行任务](#2-执行任务)
    - [2.1. 配置执行时机](#21-配置执行时机)
    - [2.2. 执行 task](#22-执行-task)

Jacoco 用于分析和统计测试覆盖率（行覆盖率, 分支覆盖率）

## 1. 使用插件

> [插件文档](https://docs.gradle.org/current/userguide/jacoco_plugin.html)

### 1.1. 安装和配置

#### 引入插件

```groovy
plugins {
  id "java"
  id "jacoco"
}
```

#### 配置插件

配置工具链版本和报告生成位置

```groovy
jacoco {
  toolVersion = "0.8.7"
  reportsDir = file("$buildDir/reports/jacoco")
}
```

配置报告生成方式

```groovy
jacocoTestReport {
  // 配置生成那类报告
  reports {
    xml.required = true
    html.required = true
    csv.required = false
    // html.outputLocation = layout.buildDirectory.dir("${buildDir}/test-repor")
  }

  // 不生成报告的路径
  afterEvaluate {
    classDirectories.from = files(classDirectories.files.collect {
      fileTree(dir: it, exclude: ["resources/**"])
    })
  }
}
```

配置覆盖率检查规则

```groovy
jacocoTestCoverageVerification {
  violationRules {
    rule {
      limit {
        minimum = 0.5
      }
    }

    rule {
      enabled = false
      element = "CLASS"
      includes = ["org.gradle.*"]

      limit {
        counter = "LINE"
        value = "TOTALCOUNT"
        maximum = 0.3
      }
    }
  }
}
```

## 2. 执行任务

### 2.1. 配置执行时机

确保每次测试完毕后都生成覆盖率报告

```groovy
test {
  finalizedBy jacocoTestReport
}

jacocoTestReport {
  dependsOn test
}
```

### 2.2. 执行 task

进行测试, 生成测试结果

```bash
gradle test
```

生成覆盖率报告

```bash
gradle jacocoTestReport
```
