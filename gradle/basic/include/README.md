# 包含其它 Gradle 文件

可以将不同的构建逻辑分散在不同的 Gradle 文件中, 例如:

- `dependency.gradle` 依赖相关
- `checkstyle.gradle` 代码检查相关
- `unittest.gradle` 单元测试相关

使用 `apply from: <file name>` 即可引入指定的 Gradle 文件, 可以在当前 Gradle 文件的任何位置进行引用操作

被引入的 Gradle 文件将无法使用 `plugins` 语句引入插件, 因为无法保证该文件被引入后, `plugins` 语句仍在文件开头位置, 可以使用旧的 `apply plugin:` 语句引入插件

```groovy
apply from: "others.gradle"

// do any task
```

使用引入的 gradle 文件

```bash
gradle -q --offline showName
```
