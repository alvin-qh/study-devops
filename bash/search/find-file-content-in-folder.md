# 文件查找

在一个目录中查找包含指定内容的文件

本例中会在当前路径下查找包含 `velit` 字符串的文件, 并输出文件名, 行号和查找到的内容

方法 1:

```bash
find . -type f -print0 | xargs -0 grep -nH 'velit\w*'
```

方法 2:

```bash
grep -r 'velit\w*' .
```
