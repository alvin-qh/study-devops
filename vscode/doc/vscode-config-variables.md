# VSCode 配置变量

- [VSCode 配置变量](#vscode-配置变量)
  - [1. 预定义变量](#1-预定义变量)
    - [1.1. 预定义变量举例](#11-预定义变量举例)
    - [1.2. 各工作空间下变量的作用域范围](#12-各工作空间下变量的作用域范围)
  - [2. 环境变量](#2-环境变量)
  - [3. 配置变量](#3-配置变量)
  - [4. 命令变量](#4-命令变量)
  - [5. 输入变量](#5-输入变量)
    - [5.1. promptString](#51-promptstring)
    - [5.3. pickString](#53-pickstring)
    - [5.3. command](#53-command)
    - [5.4. 举例](#54-举例)
  - [6. 常见问题](#6-常见问题)

VSCode 配置变量即可以在 `launch.json` 以及 `task.json` 配置文件中使用的全局变量, 包括如下内容:

## 1. 预定义变量

以下为系统预设的"预定义变量":

- `${userHome}` - 用户的 home 目录路径;
- `${workspaceFolder}` - 当前打开的工作空间的路径;
- `${workspaceFolderBasename}` - 当前打开工作空间路径的目录名 (即路径的最后一层目录, 其中不包含 `/` 字符);
- `${file}` - 当前打开的文件;
- `${fileWorkspaceFolder}` - 当前打开文件所在的工作目录;
- `${relativeFile}` - 当前打开文件和当前工作空间路径的相对路径;
- `${relativeFileDirname}` - 当前打开文件和当前工作空间路径的相对路径的目录名 (即相对路径的最后一层目录, 其中不包含 `/` 字符);
- `${fileBasename}` - 当前打开文件的文件名 (不包含路径部分);
- `${fileBasenameNoExtension}` - 当前打开文件的文件名 (不包含路径部分以及文件扩展名);
- `${fileExtname}` - 当前打开文件的扩展名 (包含扩展名前的 `.` 分隔符)
- `${fileDirname}` - 当前打开文件的路径 (不包含文件名)
- `${fileDirnameBasename}` - 当前打开文件路径的目录名 (即路径的最后一层目录, 其中不包含 `/` 字符);
- `${cwd}` - 当 VSCode 启动时, 任务运行器的工作路径
- `${lineNumber}` - 当前打开文件光标所在的行号
- `${selectedText}` - 当前打开文件光标所选的文本
- `${execPath}` - VSCode 可执行文件所在的路径 (例如 `C:\Users\your-name\AppData\Local\Programs\Microsoft VS Code\Code.exe`)
- `${defaultBuildTask}` - 默认构建任务的名称
- `${pathSeparator}` - 当前系统路径分隔符, Unix 和 Linux 一般为 `/`, Windows 一般为 `\`

### 1.1. 预定义变量举例

假设有以下需求:

- 在 VSCode 中打开了路径为 `/home/your-username/your-project/folder/file.ext` 的文件;
- 将路径 `/home/your-username/your-project` 作为当前工作空间的根路径;

这样, 对于预定义变量, 会有如下的值

- `${userHome}` - `/home/your-username`
- `${workspaceFolder}` - `/home/your-username/your-project`
- `${workspaceFolderBasename}` - `your-project`
- `${file}` - `/home/your-username/your-project/folder/file.ext`
- `${fileWorkspaceFolder}` - `/home/your-username/your-project`
- `${relativeFile}` - `folder/file.ext`
- `${relativeFileDirname}` - `folder`
- `${fileBasename}` - `file.ext`
- `${fileBasenameNoExtension}` - `file`
- `${fileDirname}` - `/home/your-username/your-project/folder`
- `${fileExtname}` - `.ext`
- `${lineNumber}` - Line number of the cursor
- `${selectedText}` - Text selected in your code editor
- `${execPath}` - location of Code.exe
- `${pathSeparator}` - `/` on macOS or linux, `\` on Windows

> 提示: 可以通过在 `task.json` 以及 `launch.json` 中使用 IntelliSense 功能获取完整的变量提示列表

### 1.2. 各工作空间下变量的作用域范围

通过将根文件夹的名称附加到变量 (以冒号分隔), 可以访问工作空间的同级根文件夹. 如果没有根文件夹, 则变量的作用域将限定在使用它的同一文件夹中

例如, 在一个同时具有 `Server` 和 `Client` 根路径的工作空间中, 变量 `${workspaceFolder:Client}` 表示的是 `Client` 这个工作空间根目录的路径

## 2. 环境变量

可以通过 `env` 前缀引用操作系统的环境变量, 例如 `{env:USERNAME}`

```json
{
  "type": "node",
  "request": "launch",
  "name": "Launch Program",
  "program": "${workspaceFolder}/app.js",
  "cwd": "${workspaceFolder}",
  "args": ["${env:USERNAME}"]
}
```

## 3. 配置变量

可以通过 `config` 前缀来引用 VSCode 配置项的值, 例如 `${config:editor.fontSize}`

## 4. 命令变量

可以通过 `${command:commandID}` 语法格式, 将任意 VSCode 命令作为变量来使用

命令变量的值会被替换为 VSCode 命令返回的字符串结果, 可以从简单命令到扩展引入的负责命令. 如果命令无法返回字符串结果, 则这个命令变量也将无效

一个例子是 VSCode 的 node.js 调试器扩展, 该扩展提供了一个交互式命令 `pickNodeProcess`, 用于从所有运行的 node.js 进程列表中选择一个进程, 返回所选进程的进程号. 该命令在"附加调试进程 ID"中用于配置方法的如下:

```json
{
  "configurations": [
    {
      "type": "node",
      "request": "attach",
      "name": "Attach by Process ID",
      "processId": "${command:extension.pickNodeProcess}"
    }
  ]
}
```

当在 `launch.json` 中使用命令变量时, 整个包含该变量的 `launch.json` 配置将以对象类型参数传递给所引用的命令, 作为该命令的"上下文"信息

## 5. 输入变量

尽管命令变量已经足够强大, 但其仍缺乏一种机制来运行某些特定用例的命令, 例如无法将提示信息或默认值传递给通用的"用户输入提示框"

要解决这个问题, 可以使用"输入变量", 即通过 `${input:variableID}` 语法设置的变量. 该变量的 `variableID` 引用了当前 JSON 文件 (`launch.json` 或 `task.json`) 的 `inputs` 属性内容, 该属性设置了输入变量的其它属性.

输入变量不支持嵌套使用

下面的例子演示了如何在 `task.json` 中使用输入变量

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "task name",
      "command": "${input:variableID}"
      // ...
    }
  ],
  "inputs": [
    {
      "id": "variableID",
      "type": "type of input variable"
      // 其它配置内容
    }
  ]
}
```

目前, VSCode 支持以下三种输入变量:

- `promptString`, 通过弹出一个提示框, 从用户输入获取字符串值
- `pickString`, 通过一个下拉选择框, 让用户从若干选项中选择一个作为变量值
- `command`, 通过执行任意命令返回字符串结果作为变量值

这三种变量所需的配置属性如下:

### 5.1. promptString

- `description` 属性, 在输入框中显示内容, 提示用户如何进行输入
- `default` 属性: 用户在不输入任何内容时的默认值
- `password` 属性: 如果该属性为 `true`, 则表示输入的是密码信息, 不会直接显示在输入框中

### 5.3. pickString

- `description` 属性: 在输入框中显示内容, 提示用户如何进行输入
- `options` 属性: 包含所有可选项的数组
- `default` 属性: 在用户未选择任何项时, 默认的选择项

一个选项可以是一个字符串值, 也可以是一个包含 `label` 和 `value` 属性的对象, 如果是后者, 在下拉框中会显示为 `label:value`

### 5.3. command

- `command` 属性: 要运行的命令
- `args` 属性: 要传递给命令的参数项 (如果命令无参数, 则无需设置)

### 5.4. 举例

下面是一个在 `task.json` 中通过 Angular CLI 使用输入变量的例子

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "ng g",
      "type": "shell",
      "command": "ng",
      "args": ["g", "${input:componentType}", "${input:componentName}"]
    }
  ],
  "inputs": [
    {
      "type": "pickString",
      "id": "componentType",
      "description": "What type of component do you want to create?",
      "options": [
        "component",
        "directive",
        "pipe",
        "service",
        "class",
        "guard",
        "interface",
        "enum"
      ],
      "default": "component"
    },
    {
      "type": "promptString",
      "id": "componentName",
      "description": "Name your component.",
      "default": "my-new-component"
    }
  ]
}
```

执行效果如下:

![Inputs Example](./../assets/run-input-example.gif)

下面的例子演示了如何在调试配置中使用命令输入变量, 让用户从一个特定文件夹中包含的测试用例文件组成的列表中选择一个测试. 假设有扩展提供了 `extension.mochaSupport.testPicker` 命令, 该命令将所有测试用例文件组织在一起, 并显示一个选择框来选择其中一个, 命令输入的参数由命令本身定义

```json
{
  "configurations": [
    {
      "type": "node",
      "request": "launch",
      "name": "Run specific test",
      "program": "${workspaceFolder}/${input:pickTest}"
    }
  ],
  "inputs": [
    {
      "id": "pickTest",
      "type": "command",
      "command": "extension.mochaSupport.testPicker",
      "args": {
        "testFolder": "/out/tests"
      }
    }
  ]
}
```

命令类型输入变量也可以用于执行任务. 在下面的演示中, 通过一个使用内置终端类型 (`"type": "shell"`) 的任务, 在其中使用命令输入变量

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Terminate All Tasks",
      "command": "echo ${input:terminate}",
      "type": "shell",
      "problemMatcher": []
    }
  ],
  "inputs": [
    {
      "id": "terminate",
      "type": "command",
      "command": "workbench.action.tasks.terminate",
      "args": "terminateAll"
    }
  ]
}
```

## 6. 常见问题

**在调试配置或任务中进行变量替换的细节**

变量替换分为两个环节完成:

1. 在第一个环节中, 所有的变量会被计算称为字符串类型值, 如果一个变量出现了多次, 则只进行一次计算
2. 在第二个环节中, 在环节一计算的变量结果会插入到文件中使用变量的位置. 所以在变量求值的过程中, 是无法看到配置文件中其它变量的值的 (例如命令类型变量), 也就是说所有的变量必须一次性求值, 无法相互依赖

**变量可否用于用户或工作空间的配置文件?**

"预定义变量"是可以在 `setting.json` 中的某些属性值中使用的, 包括: `cwd`, `env`, `shell` 和 `shellArgs` 的属性值, 另一些属性值如 `window.title` 具有自己的变量:

```json
"window.title": "${dirty}${activeEditorShort}${separator}${rootName}${separator}${appName}"
```

可以参考配置编辑器 (通过 `Ctrl`+`,` 开启), 通过每项配置的注释信息来更多的了解这些特殊变量的设置方式

**为何 `${workspaceRoot}` 变量不在文档中?**

`${workspaceRoot}` 变量已经过时, 推荐使用 `${workspaceFolder}` 来更好的支持多个工作空间根目录的情况

**为何 `task.json` 中的部分变量未被解析**

并不是所有在 `task.json` 中的变量都能被正确的解析, 具体来说, 只有 `command`, `args` 以及 `options` 这几个属性的值支持插入变量

另外, `inputs` 属性的值不支持变量, 因为变量不支持嵌套使用

**如何获取变量的实际值**

一个简单的办法是创建一个任务来显示变量的实际值, 可以通过该任务将变量打印在终端. 例如, 想看到 `${workspaceFolder}` 这个变量的值, 可以创建如下任务, 然后通过 `Terminal > Run Task` 菜单项来运行它

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "echo",
      "type": "shell",
      "command": "echo ${workspaceFolder}"
    }
  ]
}
```
