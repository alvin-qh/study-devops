# Properties

在 `gradle.properties` 文件中，可以使用 `key=value` 的格式定义一组属性（Properties），这些属性及其值可以直接在 gradle 文件中使用

```ini
gradlePropertiesProp=gradlePropertiesValue
common.prop=commonValue
```

使用 `gradle.properties` 文件

```bash
gradle -q --offline showGlobalProps
```

设置环境变量 + 命令行参数情况

- `ORG_GRADLE_PROJECT_<name>` 特殊环境变量传值
- `-P<name>` 命令行中通过 gradle 参数传值
- `-Dorg.gradle.project.<name>` 命令行中通过特殊 JVM 参数传值

```bash
ORG_GRADLE_PROJECT_systemEnvironmentProp=systemEnvironmentValue \
  gradle -q --offline showProps \
    -PcommandLineProp=commandLineValue \
    -Dorg.gradle.project.systemCommandLineProp=systemCommandLineValue
```
