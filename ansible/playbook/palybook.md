# Ansible playbook

## 1. 基本使用

### 1.1. Ping 模块

> 参见 playbook [ping.yml](./ping.yml) 文件

#### 1.1.1. 以当前用户执行 `ping` 模块

单主机

```bash
$ ansible-playbook ping.yml --tags G1
```

多主机

```bash
$ ansible-playbook ping.yml --tags G2
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
$ ansible-playbook ping.yml --tags G3 \
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
$ ansible-playbook vars.yml --tags G1
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
$ ansible-playbook vars.yml --tags G2
```

具备外部扩展变量时

```bash
$ ansible-playbook vars.yml --tags G2 -e "user_name=Emma user_age=32" 
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
$ ansible-playbook vars.yml --tags G3
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
$ VAR_MSG="HELLO WORLD" ansible-playbook vars.yml --tags G4
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
$ ansible-playbook vars.yml --tags G5
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
$ ansible-playbook vars.yml --tags G6
```

### 1.3. 条件执行

> 查看 [conditional.yml](./conditional.yml) 文件

#### 1.3.1. 基本条件

在任务中，可以通过 `when <conditional>` 可以进行条件判断，符合条件的任务才会被实际执行

```yml
tasks:
  - name: Run shell command if target distribution is "Debian"
    shell: uname -sa
    when: ansible_distribution == "Debian"
```

使用 `when` 处理条件

```bash
$ ansible-playbook conditional.yml --tags G1
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
$ ansible-playbook conditional.yml --tags G2
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
$ ansible-playbook conditional.yml --tags G3
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

令任务成功

```bash
$ ansible-playbook conditional.yml --tags G4
```

```bash
$ CMD="ls -al" ansible-playbook conditional.yml --tags G4
```

```bash
$ CMD="bad_command" ansible-playbook conditional.yml --tags G4
```

### 1.4. Loop

See [loop.yml](./loop.yml)
#### 1.4.1. Simple loop

- `with_items <collection>`

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
    Or 
```yml
vars:
  items: ['a', 'b', 'c']
tasks:
  - name: Loop by "with_items"
    debug:
      msg: "item is: {{ item }}"
    with_items: "{{ items }}"
```
ansible-playbook loop.yml --tags "G1"
#### 1.4.2. Nested loop

- `with_nested: [<collection1>, <collection2>, ...]`

```yml
tasks:
  - name: Loop by "with_nested"
    debug:
      msg: "item[0] is: {{ item[0] }} and item[1] is: {{ item[1] }}"
    with_nested:
      - ['a', 'b', 'c']
      - ['x', 'y']
```
    Or
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
ansible-playbook loop.yml --tags "G2"
#### 1.4.3. Loop with task results

- `with_items <register>.stdout_lines`

```yml
tasks:
  - name: List files in root
    shell:
      cmd: ls -al
      chdir: /
    register: ls_result
      
  - name: Loop by task result
    debug:
      msg: "Item is: {{ item }}"
    with_items: "{{ ls_result.stdout_lines }}"
```
ansible-playbook loop.yml --tags "G3"
#### 1.4.4. Loop in dictionary

- `with_dict <dictionary>`

```yml
vars:
  map:
    a: 100
    b: 200
tasks:
  - name: Loop in dictionary with key and value
    debug:
      msg: "{{ item.key }}: {{ item.value }}"
    with_dict: "{{ map }}"
```
ansible-playbook loop.yml --tags "G4"
#### 1.4.5. Loop in file list

- `with_fileglob [<glob1>, <glob2>, ...]`

```yml
tasks:
  - name: Loop in file list
    debug:
      msg: "{{ item }}"
    with_fileglob: 
      - ./*.yml
```
ansible-playbook loop.yml --tags "G5"
### 1.5. Block

See [block.yml](./block.yml)
#### 1.5.1. Run task group by blocks

- `block`: Task group in block

```yml
tasks:
  - name: Run task in blocks
    block:
      - name: Step 1
        shell: /usr/bin/true
        
      - name: Step 2
        shell: /usr/bin/false
        
      - name: Step 3
        shell: /usr/bin/true
    when: ...
    ignore_errors: True
    ...
```
ansible-playbook block.yml --tags "G1"
#### 1.5.2. Error handle by blocks

- `rescue`: with same level of `block`, when `block` raise errors, `rescue` would be executed
- `always`: always execute after `block` finished

```yml
tasks:
  - name: Run task with error handle
    block:
      - name: Normal task
        shell: /usr/bin/true
        
      - name: Raise error
        shell: /usr/bin/false
        
      - name: Never executed
        shell: /usr/bin/true
        
    rescue:
      - name: Error handle task
        debug: 
          msg: "Errors be caught"
          
      - name: Raise error again
        command: /bin/false

      - name: Never executed
        shell: /usr/bin/true
          
    always:
      - name: Always executed
        shell: /usr/bin/true
```
ansible-playbook block.yml --tags "G2"
#### 1.5.3. Error aborting

- `any_errors_fatal`: When each remote server raise error, the whole play abort on all servers

```yml
hosts: all_servers
tasks:
  - meta: clear_host_errors
  - name: Aborting on error
    block:
      - name: Task 1 (CentOS)
        shell: /usr/bin/false
        when: ansible_distribution == "CentOS"

      - name: Task 1 (Debian)
        shell: /usr/bin/true
        when: ansible_distribution == "Debian"

      - name: Task 2 (Never executed, cause "Task 1 (CentOS)" error)
        shell: /usr/bin/true
        any_errors_fatal: True
```
    This code would cause error on CentOS server of all servers, then the play aborted on all servers.
ansible-playbook block.yml --tags "G3"
### 1.6. Error handle

See [error_handle.yml](./error_handle.yml)
#### 1.6.1. Ignoring failed tasks

- `ignore_errors`

```yml
tasks:
  - name: Ignoring failed tasks
    command: /bin/false
    ignore_errors: True
```
ansible-playbook error_handle.yml --tags "G1"
#### 1.6.2. Ignoring unreachable host errors

- `ignore_unreachable`: When server unreachable on some task, ignore this error, and try to run next task on the same server
- `meta: clear_host_errors`: If Ansible cannot connect to a host, it marks that host as ‘UNREACHABLE’ and removes it from the list of active hosts for the run. You can use 'meta: clear_host_errors' to reactivate all hosts, so subsequent tasks can try to reach them again.

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
ansible-playbook error_handle.yml --tags "G2"
#### 1.6.3. Defining failure

- `failed_when`: Define "failed" status by conditional for task

```yml
tasks:
  - name: Fail task when the command stdandard output prints "error"
    command: echo "error"
    register: command_result
    failed_when: ("error" in command_result.stdout)
```

    Multi-conditionals with 'or'

```yml
tasks:
  - name: Example of many failed_when conditions with "OR"
    shell: ./not_exist_path
    register: shell_result
    failed_when: >
      ("No such file or directory" in shell_result.stdout) or
      (ret.stderr != '') or
      (ret.rc == 10)
```
ansible-playbook error_handle.yml --tags "G3"
#### 1.6.4. Defining “changed”

- `changed_when`: Define "changed" status by conditional for task

```yml
tasks:
  - name: Report "changed" when the return code is not equal to 0
    shell: /usr/bin/true
    register: shell_result
    changed_when: (shell_result.rc == 0)
```

     Multi-conditionals with 'and'
```yml
tasks:
  - name: Combine multiple conditions to override "changed" result
    command: /usr/bin/false
    register: command_result
    changed_when:
      - (command_result.stderr == '')
      - (command_result.rc == 1)
```
ansible-playbook error_handle.yml --tags "G4"
#### 1.6.5. Ensuring success for shell command

- `<conditional> || /usr/bin/true`

```yml
tasks:
  - name: Run this shell command and ignore the result
    shell: /usr/bin/bad_command || /usr/bin/true
      
  - name: Run this raw command and ignore the result
    raw: /usr/bin/bad_command || /usr/bin/true
```
ansible-playbook error_handle.yml --tags "G5"
#### 1.6.6. Aborting a play on all hosts

- `any_errors_fatal`: If any remote server raise error, the whole play would be aborted on all servers

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
ansible-playbook error_handle.yml --tags "G6-1"
- `max_fail_percentage`: Set a maximum failure percentage
- `serial`: How many serials work at same time

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
ansible-playbook error_handle.yml --tags "G6-2"
## 2. Task advanced
### 2.1. Task execution order

- `pre_tasks`: All tasks in `pre_tasks` block executed first
- `tasks`: All tasks in `tasks` block executed after
- `post_tasks`: All tasks in `post_tasks` block executed at last
ansible-playbook task.yml --tags "G1"