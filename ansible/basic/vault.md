# Ansible Vault

- [Ansible Vault](#ansible-vault)
  - [1. 加密文件](#1-加密文件)
    - [1.1. 手工输入密码](#11-手工输入密码)
      - [1.1.1. 创建一个加密文件](#111-创建一个加密文件)
      - [1.1.2. 编辑密码库文件](#112-编辑密码库文件)
      - [1.1.3. 重置密码库文件密码](#113-重置密码库文件密码)
      - [1.1.4. 加密现有文件](#114-加密现有文件)
      - [1.1.5. 查看加密文件](#115-查看加密文件)
  - [1.2. 直接加密字符串](#12-直接加密字符串)
    - [2.1. 密码文件](#21-密码文件)
  - [2. 使用密码文件](#2-使用密码文件)
    - [2.1. 使用加密文件作为 `become` 密码](#21-使用加密文件作为-become-密码)
    - [2.2. 使用加密字符串作为 `become` 密码](#22-使用加密字符串作为-become-密码)
    - [2.3. 使用附加参数作为 `become` 参数](#23-使用附加参数作为-become-参数)

可以将一个文本文件进行加密, 以保证它在执行和传输时不会暴露其中的内容

一般情况下, 会将服务器密码或者服务密码存储在文本文件中进行加密

## 1. 加密文件

需要设置一个密码, 用于加密文件或解密文件, 以及查看加密文件的内容

### 1.1. 手工输入密码

#### 1.1.1. 创建一个加密文件

```bash
ansible-vault create <filename>
```

输入密码后, 随机会打开内置编辑器 (如 vim) 提示文件内容, 输入的内容会被加密存储

#### 1.1.2. 编辑密码库文件

如果需要更改被加密文件的内容, 则可以对加密文件进行编辑

```bash
ansible-vault edit <filename>
```

输入密码库文件密码后, 打开内置编辑器 (如 vim) 提示修改文本内容, 修改后的内容会被加密存储

#### 1.1.3. 重置密码库文件密码

如果需要更改加密文件的密码, 则可以进行密码重置操作

```bash
ansible-vault rekey <filename>
```

需要输入加密文件的原密码和新密码即可

#### 1.1.4. 加密现有文件

```bash
ansible-vault encrypt <filename>
```

输入密码后, 即可对目标文件进行加密

#### 1.1.5. 查看加密文件

```bash
ansible-vault view <filename>
```

输入密码文件密码后, 即可以只读方式打开默认编辑器显示密码原文

## 1.2. 直接加密字符串

```bash
ansible-vault encrypt_string "test" --name "sudo_pass"
```

- `--name` 只是给密钥文件增加一个 key, 方便拷贝粘贴

输入密码后, 即可打印出字符串加密后的内容, 这些内容可以作为 `playbook` 文件的密码内容

### 2.1. 密码文件

可以将加密文件的密码存储在文件中, 避免反复输入密码

```bash
ansible-vault encrypt <filename> --vault-id vault/vault-id
```

加密文件, 所需的密钥从 `vault-id` 文件中获取

- `--vault-id` 指定存储密码的文件

所有需要输入加密文件密码的命令, 都可以使用 `--vault-id` 参数, 例如:

- `ansible-vault encrypt`
- `ansible-vault view`
- `ansible-vault encrypt_string`
- ...

## 2. 使用密码文件

### 2.1. 使用加密文件作为 `become` 密码

编辑 `conf/inventory` 文件：为需要 `become` 密码的服务器组设置密码变量

```ini
[group_debian1_vault:vars]
ansible_become=yes          # 强制要求 become：相当于 -b 参数或 --become 参数
ansible_become_method=sudo  # 设置 become 命令为 sudo, 相当于 --become-method 参数
ansible_become_pass={{ sudo_pass }}  # become 密码为 sudo_pass 变量值
```

创建 `var/password.yml` 文件, 保存 `sudo_pass` 变量对应的远程服务器 sudo 密码

```yml
sudo_pass: "<sudo password>"
```

对 `var/password.yml` 文件进行加密

```bash
# 将密码存储在文件中
echo 12345 > vault/vault-id

# 利用存储的密码加密 var/password.yml 文件
ansible-vault encrypt var/password.yml --vault-id vault/vault-id
```

将 `var/password.yml` 内容作为扩展参数, 通过 `vault/vault-id` 进行解密

```bash
ansible group_debian1_vault \
    -a "ifconfig" \
    --extra-vars "@var/password.yml" \
    --vault-id vault/vault-id
```

- `--extra-vars` 设置扩展参数, `@var/password.yml` 表示这是一个存储扩展参数的 `YAML` 文件

### 2.2. 使用加密字符串作为 `become` 密码

如果 `var/password.yml` 文件中只存储了密码, 则也无需将整个文件加密, 只需对密码字符串进行加密即可

```bash
ansible-vault encrypt_string "<sudo password>" \
    --name "sudo_pass" \
    --vault-id vault/vault-id
```

将输出的密码拷贝到 `var/password.yml` 文件中即可

```yml
sudo_pass: !vault |
    $ANSIBLE_VAULT;1.1;AES256
    6635353839646335393663613......
```

此时直接执行命令即可

```bash
ansible group_debian1_vault \
    -a "ifconfig" \
    --extra-vars "@var/password.yml" \
    --vault-id vault/vault-id
```

### 2.3. 使用附加参数作为 `become` 参数

可以无需在 `conf/inventory` 文件中设置 `become` 参数, 而是在需要时, 通过 `--extra-vars` 引入这些参数

建立一个 `var/become.yml` 文件存储相关的扩展参数

```yml
ansible_become: "yes"
ansible_become_method: "sudo"
ansible_become_pass: !vault |
  $ANSIBLE_VAULT;1.1;AES256
  6635353839646335393663613......
```

在需要提升权限的时候使用如下命令

```bash
ansible group_debian1 -a "ifconfig" \
    -e "@var/become.yml" \
    --vault-id vault/vault-id
```

- `-e` 为 `--extra-vars` 参数的简写, 参数从 `var/become.yml` 文件中引入
