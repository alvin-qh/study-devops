# Ansible playbook

- [Ansible playbook](#ansible-playbook)
  - [1. 基本使用](#1-基本使用)
    - [1.1. Ping 模块](#11-ping-模块)
      - [1.1.1. 以当前用户执行 `ping` 模块](#111-以当前用户执行-ping-模块)
      - [1.1.2. 通过 become 用户执行](#112-通过-become-用户执行)
    - [1.2. Ansible 变量](#12-ansible-变量)
      - [1.2.1. Ansible 内置变量](#121-ansible-内置变量)
      - [1.2.2. 自定义变量](#122-自定义变量)
      - [1.2.3. 引入变量文件](#123-引入变量文件)
      - [1.2.4. 使用环境变量](#124-使用环境变量)
      - [1.2.5. facts 变量](#125-facts-变量)
      - [1.2.6. 在任务中设置变量值](#126-在任务中设置变量值)
    - [1.3. 条件执行](#13-条件执行)
      - [1.3.1. 基本条件](#131-基本条件)
      - [1.3.2. 组合条件](#132-组合条件)
      - [1.3.3. 在注册的变量上使用条件](#133-在注册的变量上使用条件)
      - [1.3.4. 将操作结果作为条件变量](#134-将操作结果作为条件变量)
    - [1.4. 循环](#14-循环)
      - [1.4.1. 简单循环](#141-简单循环)
      - [1.4.2. 遍历多集合组合后的结果](#142-遍历多集合组合后的结果)
      - [1.4.3. 对前一个任务结果进行遍历](#143-对前一个任务结果进行遍历)
      - [1.4.4. 对字典进行遍历](#144-对字典进行遍历)
      - [1.4.5. 遍历文件列表](#145-遍历文件列表)
    - [1.5. Block](#15-block)
      - [1.5.1. 执行一个子任务组](#151-执行一个子任务组)
      - [1.5.2. 利用任务组处理错误](#152-利用任务组处理错误)
      - [1.5.3. 因错误终止任务](#153-因错误终止任务)
    - [1.6. 错误处理](#16-错误处理)
      - [1.6.1. 忽略错误任务](#161-忽略错误任务)
      - [1.6.2. 忽略无法访问的远程主机](#162-忽略无法访问的远程主机)
      - [1.6.3. 定义任务失败](#163-定义任务失败)
      - [1.6.4. 定义 "changed" 状态](#164-定义-changed-状态)
      - [1.6.5. 确保命令执行返回成功](#165-确保命令执行返回成功)
      - [1.6.6. Aborting a play on all hosts](#166-aborting-a-play-on-all-hosts)
  - [2. 高级功能](#2-高级功能)
    - [2.1. 前置、后置任务](#21-前置后置任务)
    - [2.2. 导入外部剧本](#22-导入外部剧本)
    - [2.3. 导入外部任务](#23-导入外部任务)

Playbook 是一个 `yaml` 文件，由 "play" 和 "task" 构成，每个 "task" 相当于一个 Ad-Hoc 命令，组织起来就是一个批处理命令，可以在远程主机执行一个完整的操作

## 1. 基本使用

### 1.1. Ping 模块

> 参见 playbook [ping.yml](./ping.yml) 文件

#### 1.1.1. 以当前用户执行 `ping` 模块

单主机

```bash
ansible-playbook ping.yml --tags G1
```

多主机

```bash
ansible-playbook ping.yml --tags G2
```

#### 1.1.2. 通过 become 用户执行

`become`: 以 `sudo` 或 `su` 权限执行

```yml
tasks:
  - name: Run as root user
    become: True  # 以 become 权限执行
```

使用 become 用户权限

```bash
ansible-playbook ping.yml --tags G3 \
    -e "@var/become.yml" \
    --vault-id vault/vault-id
```

- `-e "@var/become.yml"` 以 `var/become.yml` 文件作为 become 扩展参数
- `--vault-id vault/vault-id` 指定 `var/become.yml` 文件的解密密码

### 1.2. Ansible 变量

> 参见 [vars.yml](./vars.yml) 文件

#### 1.2.1. Ansible 内置变量

内置变量一般以 `ansible_` 为前缀

```bash
ansible-playbook var.yml --tags G1
```

#### 1.2.2. 自定义变量

在 playbook 中，通过 `vars` 字段定义自定义变量

```yml
vars:
  var1: value1
  var2: value2
```

无外部扩展变量时

```bash
ansible-playbook var.yml --tags G2
```

具备外部扩展变量时

```bash
ansible-playbook var.yml --tags G2 -e "user_name=Emma user_age=32"
```

#### 1.2.3. 引入变量文件

变量文件可以定义为 `json` 或 `yaml` 格式，在 playbook 中通过 `include_vars` 属性引入外部变量文件

```yml
tasks:
  - name: Include vars from file
    include_vars: variables_file.yml
```

使用外部变量文件

```bash
ansible-playbook var.yml --tags G3
```

变量中可以使用加密内容，并通过 `vault-id` 进行解密

#### 1.2.4. 使用环境变量

为远程主机的当前进程设置环境变量

```yml
environment:
  env_var1: value1
  env_var2: value2
```

可以通过 `ansible_env` 内置变量获取到远程主机的环境变量

```yml
tasks:
  - name: Run when value of remote environment variable "RUNNING_STATUS" is "OK"
    when: ansible_env.RUNNING_STATUS == "OK"
    # or ansible_env["RUNNING_STATUS"] == "OK"

  - name: Show value of remote environment variable "NAME"
    debug:
    msg: "The value is: {{ ansible_env.NAME }}"
```

可以通过 `lookup('env', <name>)` 在远程主机查找指定的环境变量值

```yml
  tasks:
    - name: Show value of local environment variable "HOST"
      debug:
        msg: "The value is: {{ lookup('env', 'HOST') }}"
      when: (lookup('env', 'HOST') is defined)
```

使用环境变量

```bash
VAR_MSG="HELLO WORLD" ansible-playbook var.yml --tags G4
```

#### 1.2.5. facts 变量

当 playbook 的 `gather_facts` 属性为 `True` 时，会收集远程主机的 `ansible_facts` 变量集合，即远程主机的所有 facts 变量

```yml
tasks:
  - name: Show ansible facts variables
    debug:
      msg:
        - "Architecture: {{ ansible_facts.architecture }}"
```

使用 facts 变量

```bash
ansible-playbook var.yml --tags G5
```

#### 1.2.6. 在任务中设置变量值

可以通过 `set_fact` 模块在任务执行过程中设置 `ansible_facts` 变量的值

```yml
tasks:
  - name: Set variables "a" and "b"
    set_fact:
      a: 100
      b: 200
```

```bash
ansible-playbook var.yml --tags G6
```

### 1.3. 条件执行

> 查看 [cond.yml](./cond.yml) 文件

#### 1.3.1. 基本条件

在任务中，可以通过 `when <cond>` 可以进行条件判断，符合条件的任务才会被实际执行

```yml
tasks:
  - name: Run shell command if target distribution is "Debian"
    shell: uname -sa
    when: ansible_distribution == "Debian"
```

使用 `when` 处理条件

```bash
ansible-playbook cond.yml --tags G1
```

#### 1.3.2. 组合条件

可以通过 `and`, `or` 组合多个条件，或通过 `not` 否定某个条件

```yml
tasks:
  - name: Run shell command if target distribution is "Debian" and version ge than 10
    shell: uname -sa
    when: >
      ansible_facts.distribution == "Debian" and
      (ansible_facts.distribution_major_version | int) >= 10
```

使用组合条件

```bash
ansible-playbook cond.yml --tags G2
```

#### 1.3.3. 在注册的变量上使用条件

前一步任务可以使用 `register` 将结果保存到变量中，后面的任务可以根据注册的变量进行条件操作

```yaml
tasks:
  - name: Get "uname -sa" result as register variable
    shell: uname -sa
    register: uname_result # 注册到变量

  - name: Show "stdout" property of register variable
    debug:
    msg: "{{ uname_result.stdout }}"
    when: (uname_result.stdout | length) > 0 # 使用注册的变量
```

使用注册变量进行条件处理

```bash
ansible-playbook cond.yml --tags G3
```

#### 1.3.4. 将操作结果作为条件变量

任务的操作结果包括 `skipped`，`succeeded`，`failed`，分别表示“任务被跳过”，“任务成功”和“任务失败”

当任务执行结果符合上述三种情况时，任务注册的变量会保存任务结果

```yaml
# 任务跳过的情况
tasks:
  - name: "Skipping" task
    command: /usr/bin/true
    register: cmd_result  # 保存任务被跳过的结果
    when: False # 导致任务被跳过

  - name: Execute if latest task is skipped
    debug:
      msg: "command {{ cmd }} is skipped"
    when: (cmd_result is skipped)  # 判断任务是否被跳过

# 任务成功的情况
tasks:
  - name: "Succeeded" task
    command: /usr/bin/false
    register: cmd_result

  - name: Show "{{ cmd }}" result if it succeeded
    debug:
      msg: "{{ cmd_result.msg }}"
    when: (cmd_result is succeeded)

# 任务失败的情况
tasks:
  - name: "Failed" task
    command: /usr/bin/false
    register: cmd_result

  - name: Show "{{ cmd }}" result if it failed
    debug:
      msg: "{{ cmd_result.msg }}"
    when: (cmd_result is failed)
```

令任务跳过

```bash
ansible-playbook cond.yml --tags G4
```

令任务成功

```bash
CMD="ls -al" ansible-playbook cond.yml --tags G4
```

令任务失败

```bash
CMD="bad_command" ansible-playbook cond.yml --tags G4
```

### 1.4. 循环

> 查看 [loop.yml](./loop.yml) 文件

#### 1.4.1. 简单循环

任务中的 `with_items <collection>` 属性可以为集合中的每个元素执行一次当前任务

```yml
tasks:
  - name: Loop by "with_items"
    debug:
      msg: "item is: {{ item }}"
    with_items:
      - a
      - b
      - c
      - d
```

也可以通过集合变量来设置 `with_items` 属性

```yml
vars:
  items: ['a', 'b', 'c']
tasks:
  - name: Loop by "with_items"
    debug:
      msg: "item is: {{ item }}"
    with_items: "{{ items }}"
```

使用循环

```bash
ansible-playbook loop.yml --tags G1
```

#### 1.4.2. 遍历多集合组合后的结果

在任务中通过 `with_nested: [<collection1>, <collection2>, ...]` 可以对多个集合求笛卡尔积，并进行循环遍历

对两个集合求笛卡尔积后遍历

```yml
tasks:
  - name: Loop by "with_nested"
    debug:
      msg: "item[0] is: {{ item[0] }} and item[1] is: {{ item[1] }}"
    with_nested:
      - ['a', 'b', 'c']
      - ['x', 'y']
```

也可以对多个集合变量进行类似操作

```yml
vars:
  list1: ['a', 'b', 'c']
  list2: ['x', 'y']
tasks:
  - name: Loop by "with_nested"
    debug:
      msg: "item[0] is: {{ item[0] }} and item[1] is: {{ item[1] }}"
    with_nested:
      - "{{ list1 }}"
      - "{{ list2 }}"
```

遍历多个集合

```bash
ansible-playbook loop.yml --tags G2
```

#### 1.4.3. 对前一个任务结果进行遍历

通过 `with_items <register>.stdout_lines` 可以对前一个任务执行结果的输出内容进行遍历

```yml
tasks:
  - name: List files in root
    shell:
      cmd: ls -al
      chdir: /
    register: ls_result # 将 ls 执行的结果存入变量

  - name: Loop by task result
    debug:
      msg: "Item is: {{ item }}"
    with_items: "{{ ls_result.stdout_lines }}" # 遍历 ls 结果的输出内容
```

遍历任务输出结果

```bash
ansible-playbook loop.yml --tags G3
```

#### 1.4.4. 对字典进行遍历

通过 `with_dict <dictionary>` 可以对一个字典集合进行遍历，得到其每一个 key 和 value 值，并为每一次遍历重复执行一次任务

```yml
vars:
  map: # 定义名为 map 的字典变量
    a: 100
    b: 200
tasks:
  - name: Loop in dictionary with key and value
    debug:
      msg: "{{ item.key }}: {{ item.value }}" # 输出 key/value 值
    with_dict: "{{ map }}" # 遍历字典变量
```

```bash
ansible-playbook loop.yml --tags G4
```

#### 1.4.5. 遍历文件列表

在任务中，通过 `with_fileglob [<glob1>, <glob2>, ...]` 可以以 glob 语法遍历文件，获取一个文件列表，并为列表中的每个文件重复执行当前任务

```yml
tasks:
  - name: Loop in file list
    debug:
      msg: "{{ item }}"
    with_fileglob: # 获取文件列表
      - *.yml
```

遍历文件列表

```bash
ansible-playbook loop.yml --tags G5
```

### 1.5. Block

子任务块可以看作是一个整体，包含了多个子任务，共享整个任务的信息

> 查看 [block.yml](./block.yml) 文件

#### 1.5.1. 执行一个子任务组

任务中的 `block` 属性设置了一个子任务组

```yml
tasks:
  - name: Run task in blocks
    block: # 子任务组
      - name: Step 1
        shell: /usr/bin/true

      - name: Step 2
        shell: /usr/bin/false

      - name: Step 3
        shell: /usr/bin/true
    when: ...
    ignore_errors: True # 任意子任务错误后继续执行其它任务
    ...
```

执行任务组

```bash
ansible-playbook block.yml --tags G1
```

#### 1.5.2. 利用任务组处理错误

`rescue` 和 `always` 也各是一组子任务。当 `block` 执行出现错误时，可以通过 `rescue` 块进行错误处理；在 `block` 执行结束后，`always` 块必然执行

```yml
tasks:
  - name: Run task with error handle
    block: # 任务组
      - name: Normal task
        shell: /usr/bin/true

      - name: Raise error
        shell: /usr/bin/false

      - name: Never executed
        shell: /usr/bin/true

    rescue: # 处理 block 错误
      - name: Error handle task
        debug:
          msg: "Errors be caught"

      - name: Raise error again
        command: /bin/false

      - name: Never executed
        shell: /usr/bin/true

    always: # 在 block 执行完毕后执行
      - name: Always executed
        shell: /usr/bin/true
```

错误处理

```bash
ansible-playbook block.yml --tags G2
```

#### 1.5.3. 因错误终止任务

通过 `any_errors_fatal` 属性，当出现错误时，会停止所有远程主机上后续任务执行

```yml
hosts: all_servers
tasks:
  - meta: clear_host_errors
  - name: Aborting on error
    block: # 子任务块
      - name: Task 1 (CentOS)
        shell: /usr/bin/false
        when: ansible_distribution == "CentOS"

      - name: Task 1 (Debian)
        shell: /usr/bin/true
        when: ansible_distribution == "Debian"

      - name: Task 2 (Never executed, cause "Task 1 (CentOS)" error)
        shell: /usr/bin/true

    any_errors_fatal: True # 出现错误时停止所有任务
```

出现错误时停止任务

```bash
ansible-playbook block.yml --tags G3
```

### 1.6. 错误处理

> 查看 [error_handle.yml](./error_handle.yml) 文件

#### 1.6.1. 忽略错误任务

任务的 `ignore_errors` 属性可以忽略出现错误的任务，继续后续任务

```yml
tasks:
  - name: Ignoring failed tasks
    command: /bin/false
    ignore_errors: True
```

忽略错误任务

```bash
ansible-playbook error.yml --tags G1
```

#### 1.6.2. 忽略无法访问的远程主机

任务的 `ignore_unreachable` 属性可以忽略无法访问的远程主机，并停止任务发送给该主机；`meta: clear_host_errors` 可以清除主机上的错误信息，让每次任务都进行尝试，而不是因为前一次任务导致忽略该主机的后续任务

```yml
hosts: unknown
tasks:
  - name: This executes, fails, and the failure is ignored
    command: /bin/true
    ignore_unreachable: True

  - meta: clear_host_errors  # optional

  - name: This executes, fails, and ends the play for this host
    command: /bin/true

  - name: This task never execute
    command: /bin/true
```

```bash
ansible-playbook error.yml --tags G2
```

#### 1.6.3. 定义任务失败

任务的 `failed_when` 属性定义了任务失败的条件，只要满足该条件，则定义该任务失败

当执行命令返回结果中包含 "Error" 字符串时，定义任务失败

```yml
tasks:
  - name: Fail task when result of command contains "Error" string
    command: echo "error"
    register: command_result
    failed_when: ("error" in command_result.stdout)
```

可以通过 `and` 或 `or` 组合多个失败条件

```yml
tasks:
  - name: Example of many failed_when conditions with "OR"
    shell: ./not_exist_path
    register: shell_result
    failed_when: > # 组合多个失败条件
      ("No such file or directory" in shell_result.stdout) or
      (ret.stderr != '') or
      (ret.rc == 10)
```

为任务定义失败条件

```bash
ansible-playbook error.yml --tags G3
```

#### 1.6.4. 定义 "changed" 状态

如果一个 task 对远程主机做出改变（文件、状态），则该 task 被标记为 `changed`，可以通过任务的 `changed_when` 属性来定义 `changed` 状态

```yml
tasks:
  - name: Report "changed" when the return code is not equal to 0
    shell: /usr/bin/true
    register: shell_result # 在变量中保存执行结果
    changed_when: shell_result.rc == 0
```

后续任务可以通过 `shell_result is changed` 条件来判断前一个任务是否让远程主机发送改变

可以组合多个条件

```yml
tasks:
  - name: Combine multiple conditions to override "changed" result
    command: /usr/bin/false
    register: command_result
    changed_when:
      - command_result.stderr == ''
      - command_result.rc == 1
```

```bash
ansible-playbook error.yml --tags G4
```

#### 1.6.5. 确保命令执行返回成功

可以使用 shell 命令的 `||` 操作，保证 shell 命令永远返回正确，即 `<shell command> || /usr/bin/true`

```yml
tasks:
  - name: Run this shell command and ignore the result
    shell: /usr/bin/bad_command || /usr/bin/true

  - name: Run this raw command and ignore the result
    raw: /usr/bin/bad_command || /usr/bin/true
```

保证命令行执行返回正确状态

```bash
ansible-playbook error.yml --tags G5
```

#### 1.6.6. Aborting a play on all hosts

通过任务的 `any_errors_fatal` 属性，可以定义一旦发生错误，立即停止所有主机后续任务的执行

```yml
hosts: all_server
any_errors_fatal: True
tasks:
  - name: Task 1 (CentOS)
    shell: /usr/bin/false
    when: ansible_distribution == "CentOS"

  - name: Task 1 (Debian)
    shell: /usr/bin/true
    when: ansible_distribution == "Debian"

  - name: Task 2 (Never executed, cause "Task 1(CentOS)" error)
    shell: /usr/bin/true
```

```bash
ansible-playbook error.yml --tags G6-1
```

可以通过剧本的 `max_fail_percentage` 属性，设置最大错误比率的阈值，超过该阈值则停止整个剧本

```yml
tasks:
  - name: Task 1
    shell: /usr/bin/false
    when: (t1 % 2 == 0)

  - name: Task 2
    shell: /usr/bin/false
    when: (t2 % 2 == 0)

  - name: Task 3
    shell: /usr/bin/false
    when: (t3 % 2 == 0)
max_fail_percentage: 49
serial: 10
```

约束错误比率

```bash
ansible-playbook error.yml --tags G6-2
```

可以通过剧本的 `serial` 属性定义多少连续工作同时执行

## 2. 高级功能

> 查看 [adv.yml](./adv.yml) 文件

### 2.1. 前置、后置任务

剧本的 `pre_tasks` 部分会在所有任务执行前执行；`post_tasks` 部分会在所有任务结束后执行

```yml
pre_tasks:
  - name: pre-task executing
    shell: /usr/bin/true

post_tasks:
  - name: post-task executing
    shell: /usr/bin/true

tasks:
  - name: main task executing
    shell: /usr/bin/true
```

设置前置和后置任务

```bash
ansible-playbook adv.yml --tags G1
```

### 2.2. 导入外部剧本

剧本的 `import_playbook` 表示从外部导入剧本的内容，包括剧本的属性和任务

```yml
- name: Import external playbooks
  import_playbook: ./import/ext_playbook.yml  # 导入外部剧本
```

> 查看 [./import/ext_playbook.yml](./import/ext_playbook.yml)

外部导入剧本

```bash
ansible-playbook adv.yml --tags G2
```

### 2.3. 导入外部任务

任务的 `import_tasks` 表示从外部导入子任务，子任务行为类似于 `block` 定义的子任务，是一个整体

```yml
tasks: Import external task
  - name: Task import from external # 导入外部剧本
    import_tasks: ./import/ext_task.yml
```

> 查看 [./import/ext_task.yml](./import/ext_task.yml)

外部导入任务

```bash
ansible-playbook adv.yml --tags G3
```
