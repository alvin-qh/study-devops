# C/C++ with vscode

## 1. Install C/C++ plugins

### 1.1. Install C/C++ toolchain

```bash
$ sudo apt install build-essential
```

Or

```bash
$ yum groupinstall "Development Tools"
```

### 1.2. Install plugins

- C/C++
- C/C++ Clang Command Adapter
- ~~Include Autocomplete~~
- Code Runner

### 1.3. Install LLVM

1. Download lastest version of LLVM from [Offical](https://releases.llvm.org/download.html)(eg: `xz -d clang+llvm-10.0.0-x86_64-linux-gnu-ubuntu-18.04.tar.xz`);

2. Unpackage downloaded `.tar.xz` file;

   ```bash
   $ xz -d clang+llvm-10.0.0-x86_64-linux-gnu-ubuntu-18.04.tar.xz
   $ tar xvf clang+llvm-10.0.0-x86_64-linux-gnu-ubuntu-18.04.tar
   ```

3. Add LLVM bin folder into $PATH enviroment

   ```bash
   $ sudo mv clang+llvm-10.0.0-x86_64-linux-gnu-ubuntu-18.04 /usr/lib/llvm
   ```

   Edit `~/.bashrc` (or `~/.zshrc`, `~/.bash_profile`) file, add following content:

   ```bash
   $ export LLVM_HOME="/usr/lib/llvm"
   $ export PATH="$PATH:$LLVM_HOME/bin"
   ```

### 1.4. Install GDB

```bash
$ sudo apt install gdb
```

### 1.5. Install CMAKE

```bash
$ sudo apt install cmake
```

## 2. Config vscode

### 2.1. Run single c/cpp file

If json config file not generate automate, add the following files into `.vscode` folder

#### 2.1.1. `launcher.json`

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "Launch main",
            "type": "cppdbg",
            "request": "launch",
            "program": "${workspaceFolder}/build/app",
            "args": [],
            "stopAtEntry": false,
            "cwd": "${workspaceFolder}",
            "environment": [],
            "externalConsole": false,
            "launchCompleteCommand": "exec-run",
            "linux": {
                "MIMode": "gdb",
            },
            "osx": {
                "MIMode": "lldb"
            }
        }
    ]
}
```

Fields:

- `name`: name of config;
- `type`: `cppdbg`, use cpptools debug tools;
- `request`: `launch` or `attach`;
- `program`: `"${fileDirname}/${fileBasenameNoExtension}"` binary file to debug;
- `args`: start arguments passed to debug program;
- `stopAtEntry`: add a breakpoint at start of main if set `true`;
- `cwd`: current work dir, `"${workspaceFolder}"`;
- `environment`: external environment variables;
- `externalConsole`: `true` if use common terminal window or `false` to use inner VCS terminal;
- `internalConsoleOptions`: `neverOpen`;
- `MIMode`: `gdb`, which debug tools to use;
- `miDebuggerPath`: the path of debug tools;
- `preLaunchTask`: value of `label` field in `task.json`, how to build file;

#### 2.1.2. `tasks.json`

```json
{
    "tasks": [
        {
            "type": "shell",
            "label": "g++ build active file",
            "command": "/usr/bin/g++",
            "args": [
                "-g",
                "${file}",
                "-o",
                "${fileDirname}/build/${fileBasenameNoExtension}"
            ],
            "options": {
                "cwd": "/usr/bin"
            }
        }
    ],
    "version": "2.0.0"
}
```

Fields:

- `type`: `process` is vsc to pass the predefined variables and escape slyly all directly to command; the `shell` is equivalent to opening the shell and then entering the command, so the args will be resolved again through the shell;
- `label`: Corresponding to the `preLaunchTask` field in the `launcher.json` file;
- `command`: The compiler to use (gcc for clang and g++ for cpp);
- `args`: arguments to compile source file:
  - `-g`: generate debug info;
  - `-o`: file name for output, same as source by default;
  - `-Wall`: show all warning;
  - `-static-libgcc`: static link to gcc lib;
  - `-fexec-charset=GBK`: charset for string compile;
  - `-std=c11`: version of cpp;
- `presentation`:
  - `reveal`: one of `always`, `silent` or `never`, whether to jump to the terminal panel while performing a task;
  - `focus`: `true` allows the focus to focus on the terminal when executing the task;
  - `panel`: `shared` if the Compilation information of different files information sharing a terminal panel;

#### 2.1.3. `c_cpp_properties.json`

```json
{
    "configurations": [
        {
            "name": "Linux",
            "includePath": [
                "${workspaceFolder}/**"
            ],
            "defines": [],
            "compilerPath": "/usr/bin/gcc",
            "cStandard": "c11",
            "cppStandard": "c++17",
            "intelliSenseMode": "clang-x64"
        },
        {
            "name": "Mac",
            "includePath": [
                "${workspaceFolder}/**"
            ],
            "defines": [],
            "macFrameworkPath": [
                "/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/System/Library/Frameworks"
            ],
            "compilerPath": "/usr/bin/clang",
            "cStandard": "c11",
            "cppStandard": "c++17",
            "intelliSenseMode": "clang-x64",
            "configurationProvider": "vector-of-bool.cmake-tools"
        }
    ],
    "version": 4
}
```

#### 2.1.4. Setup "code runner" plugin

Edit `settings.json` in `.vscode` folder, add "code runner" settings:

- Compile and run:

    ```json
    {
        ...,
        "code-runner.clearPreviousOutput": true,
        "code-runner.executorMap": {
            "cpp": "mkdir $workspaceRoot/build -p && g++ $fullFileName -o $workspaceRoot/build/$fileNameWithoutExt && $workspaceRoot/build/$fileNameWithoutExt"
        }
    }
    ```
- Or just run directly:

    ```json
    {
        ...,
        "code-runner.clearPreviousOutput": true,
        "code-runner.executorMap": {
            "cpp": "$workspaceRoot/build/app"
        }
    }
    ```



See also [Offical document](https://github.com/formulahendry/vscode-code-runner/blob/master/README.md)


### 2.2. Run with "CMakeLists.txt"

Install the following plugins:
- CMake
- CMake Tools
- CMake Integration
- cmake-format

Restart vscode, then some "cmake tools" would dock on status bar
![vscode-status-bar](./assets/vscode-status-bar.png)

Press "Build" label can build project by "CMakeLists.txt";

Press "Debug" label can start debug after build;



## 3. Todo

### 3.1. Try [Microsfot vcpkg](https://github.com/Microsoft/vcpkg)
