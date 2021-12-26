# String token

可以通过一个模板文件，通过模板参数（token），产生一个新的文件

模板参数是以 `@` 包围的的参数

```ini
version.name = @name@   # name 参数
version.value = @value@ # value 参数
```

通过 gradle 内置的 `copy` 函数，可以在复制文件的过程中，替换文件中的模板参数

```groovy
task token {
    dependsOn deleteAll
    doLast {
        // 通过 copy 函数复制文件并设置模板参数
        copy {
            from 'conf'     // 源文件，这里为模板文件
            include '*.*'   // 过滤器，这里包含所有文件
            into 'build/conf'   // 目标，将文件复制到的位置
            filter(
                ReplaceTokens,  // 替换模板参数
                tokens: [name: 'user', value: 'Alvin']  // 设置模板参数
            )
        }
    }
}
```
