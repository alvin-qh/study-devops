# OS

## 1. Setup environment

### 1.1. Install python

#### 1.1.1. Install pyenv

- Download and install pyenv

```bash
$ curl -L https://github.com/pyenv/pyenv-installer/raw/master/bin/pyenv-installer | bash
```

- Set shell enviroment: modify `~/.bashrc` (or `~/.zshrc` or `~/.bash_profile`), and add the following content

```bash
export PATH="~/.pyenv/bin:$PATH"
eval "$(pyenv init -)"
eval "$(pyenv virtualenv-init -)"
```

#### 1.1.2. Install python

```bash
$ pyenv install 3.7.5
```

#### 1.1.3. Use python

In notebook folder:

```bash
$ pyenv local 3.7.5
```

## 2. Setup notebook

[Use jupyter notebook](./notebook/README.md)
