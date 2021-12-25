# Ansible Role

- [Ansible Role](#ansible-role)
  - [1. 介绍](#1-介绍)
    - [1.1. Role 目录结构](#11-role-目录结构)
    - [1.2. Role 存储路径](#12-role-存储路径)
  - [2. 使用](#2-使用)
    - [2.1. 引入 Role](#21-引入-role)
    - [2.2. 执行顺序](#22-执行顺序)

## 1. 介绍

### 1.1. Role 目录结构

```plain
roles
 ├╌ <role name>
 │   ├╌ tasks
 │   ├╌ handlers
 │   ├╌ library
 │   ├╌ defaults
 │   ├╌ vars
 │   ├╌ files
 │   ├╌ templates
 │   └╌ meta
 |
 ├╌ conf
 │   ├╌ ssh_config
 │   └╌ inventory
 |
 ├╌ vault
 |   └╌ <vault-id>
 |
 ├╌ ansible.cfg
 └╌ <playbook>.yml
```

各目录文件说明

- `tasks/main.yml` 入口，用于定义被执行的任务
- `handlers/main.yml` handlers，用于响应任务中定义的 `notify` 属性
- `library/<module_name>.py` 自定义模块，定义的模块可以在任务中直接使用
- `defaults/main.yml` 定义变量初始值，如果其它地方也定义了这些变量（包括 `conf/inventory` 文件），则不使用默认值，以这些定义为准
- `vars/main.yml` 定义变量值
- `files/main.yml` 存放一系列文件，这些文件可以直接在任务中使用而无需指定路径
- `templates/main.yml` 定义模板，这些模板可以直接在任务中使用而无需指定路径
- `meta/main.yml` role 的元数据定义，包括多个 role 之间的依赖关系

### 1.2. Role 存储路径

默认情况下，Role 会从如下路径进行查找

- 和当前剧本同一目录的 `roles` 目录下
- 在 `/etc/ansible/roles` 目录下

也可以在剧本中定义 Role 存储的路径

```yml
- hosts: all
  roles:
    - role: '/path/to/my/roles/common' # 指定 Role 存储路径
```

## 2. 使用

可以通过 3 个途径使用 Role：

- 在剧本一级，通过 `roles` 属性引入所需的 Role
- 在任务一级，通过 `include_role` 属性动态的引入 Role
- 在任务一级，通过 `import_role` 属性静态的引入 Role

### 2.1. 引入 Role

引入 Role 的基本方式如下：

```yml
- hosts: all
  roles:
    - common # 引入多个 role
    - web
```

对于剧本 `roles` 属性包含的每个 role `x`，执行策略如下：

- 如果 `roles/x/tasks/main.yml` 文件存在，则作为主入口文件，其中定义的任务均被执行，参见：[roles/demo/tasks/main.yml](./roles/demo/tasks/main.yml)
- 如果 `roles/x/meta/main.yml` 文件存在，则按其内容定义的依赖，执行其它相关 role
- 如果 `roles/x/defaults/main.yml` 文件存在，其内容作为变量的默认值
- 如果 `roles/x/vars/main.yml` 文件存在，其内容作为变量，在整个剧本执行过程中生效
- 如果 `roles/x/handlers/main.yml` 文件存在，则内容作为 handler，可以在任务中通过 `notify` 指定，并在这些任务指向完毕后执行对应的 handler
- 对于 `roles/x/{files,templates,tasks}/` 存放的内容，当使用 `copy`，`script` 或 `template` 等模块，以及 `include_tasks` 或 `import_tasks` 属性，可以直接使用里面的文件而无需完整路径

### 2.2. 执行顺序

当在剧本一级引入 role（通过 `roles` 属性），则 Ansible 会以静态方式引入 role，并在剧本解析的过程中对 role 进行预处理，此时，Ansible 执行剧本的顺序如下：

- 所有定义在剧本中的 `pre_tasks` 属性
- 所有在 `pre_tasks` 中触发的 `handlers`
- 在 `roles` 属性列表中的每个 role（按书写顺序），其中：`meta/main.yml` 最先执行以解析 role 之间的依赖关系，之后 `tasks/main.yml` 被执行
- 剧本中定义的任务
- 在任务中除法的 `handlers`
- 所有定义在剧本中的 `post_tasks` 属性
- 在 `post_tasks` 中被触发的 `handlers`

所有的任务会依据其 `tag` 和条件依次执行

> 如果将标记与 role 中的任务一起使用，需确保 `pre_tasks`、`post_tasks` 和 `role` 依赖关系同样被标记，并传递这些标记，尤其是在 `pre/post task` 和 role 依赖关系用于监视中断窗口控制或负载平衡时

```yml
- hosts: all
  roles:
    - common
    - role: foo_app_instance
      vars:
        dir: '/opt/a'
        app_port: 5000
      tags: tA
    - role: foo_app_instance
      vars:
        dir: '/opt/b'
        app_port: 5001
      tags: tB
```

当为 `role` 属性添加标记（`tags`）后，role 之下的所有任务都会被标注同样的标记

```bash
$ ansible-playbook role.yml
```