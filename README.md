# Study Devops

## 1. About Python Environment

### 1.1. Install and setup PyEnv

> See also: [https://github.com/pyenv/pyenv](https://github.com/pyenv/pyenv)

#### 1.1.1. On macOS

- Install by brew

    ```bash
    $ brew install pyenv
    $ brew install pyenv-virtualenv
    ```

- Setup pyenv

    Edit `.bash_profile` (or `.zshrc`) file, add script:

    ```bash
    export PATH="~/.pyenv/bin:$PATH"
    eval "$(pyenv init --path)"
    eval "$(pyenv virtualenv-init -)"
    ```

#### 1.1.2. On Linux

- Download install script and install

    > See also: [https://github.com/pyenv/pyenv-installer](https://github.com/pyenv/pyenv-installer)

    ```bash
    $ curl -L https://github.com/pyenv/pyenv-installer/raw/master/bin/pyenv-installer | bash
    ```

- Setup PyEnv

    Edit `.bashrc` (or `.zshrc`) file, add script:

    ```bash
    export PATH="~/.pyenv/bin:$PATH"
    eval "$(pyenv init --path)"
    ```

### 1.2. Install Python

- Checck the python versions installed

    ```bash
    $ pyenv versions
      system
      3.10.0a5
    * 3.8.5 (set by /home/alvin/Workspaces/Study/study-python/.python-version)
    ```

    The line marked by `*` is default version

- List all available python version

    ```bash
    $ pyenv install --list
    ```

- Install python with specified version

    ```bash
    $ pyenv install 3.8.5
    ```

- Set the global default python version

    ```bash
    $ pyenv global 3.8.5
    ```

## 1.2. Create virtualenv

- In project folder, determine the python version

    ```bash
    $ pyenv local 3.8.5
    ```

    Use python version==3.8.5 in this folder

- Create virutalenv

    ```bash
    $ python -m venv .venv --prompt="project prompt"
    ```

## 2. About Jupyter Lab Environment

### 2.1. Necessary python dependency

The following python packages need to install in every virtualenv:

```plain
jupyterlab
jupyterlab-git==0.30.0b1
bash_kernel
```

- `jupyterlab-git`: Git management user interface
- `bash_kernel`: Git management user interface

### 2.2. Setup each notebook

In every virtualenv:

```bash
$ pip install -r path/to/notebook-requirements.txt
$ source .venv/bin/activate
$ python -m bash_kernel.install
```
