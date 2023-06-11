# 使用 grgit 插件

参见 [Plugin](https://plugins.gradle.org/plugin/org.ajoberstar.grgit) 页面

## 引入插件

引入插件有两种方式

方式 1：使用新语法

```groovy
plugins {
  id "org.ajoberstar.grgit" version "4.1.1"
}
```

这种方式要求 `plugin` 语句必须在 `gradle` 文件的最开头

方式 2：使用旧语法

```groovy
// 设置引入插件的依赖
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "org.ajoberstar.grgit:grgit-gradle:4.1.1" // 设置 grgit 插件依赖
  }
}

// 引入 grgit 插件
apply plugin: "org.ajoberstar.grgit"
```

## 使用插件

`grgit` 对象表示 git 最新状体, 即 `HEAD` 的状态

`grgit.log()` 表示 git 的 log 信息

```groovy
headVer = grgit.head().getAbbreviatedId() // 获取 head 的版本 id

log = grgit.log() // 获取 log 对象
logs[1].getAbbreviatedId() // 获取倒数第 2 个 log 的版本 id
```

使用 grgit 插件

```bash
gradle -q showGitVersion
```
