# Use Notebook

## 1. Setup

### 1.1. Install python virtualenv

```bash
$ python -m venv .venv --prompt='study-devops-redis'
$ source .venv/bin/activate
$ pip install -r requirements.txt
```

### 1.2. Install jupyter bash extension

#### 1.2.1. Install pip package

```bash
$ pip install bash_kernel
```

#### 1.2.2. Build and install extension

```bash
$ python -m bash_kernel.install
```

### 1.3. Enable "Jupyterlab Code Formatter" extension

#### 1.3.1. Install and enable extension

```bash
$ jupyter labextension install @ryantam626/jupyterlab_code_formatter
$ jupyter serverextension enable --py jupyterlab_code_formatter
```

> [Jupyterlab Code Formatter Homepage](https://jupyterlab-code-formatter.readthedocs.io/)

#### 1.3.2. Config extension

- Click menu `Settings`>`Advanced Settings Editor`>`Jupyterlab Code Formatter`，add content：

```json
{
    "autopep8": {
        "max_line_length": 120,
        "ignore": [
            "E226",
            "E302",
            "E41"
        ]
    },
    "preferences": {
        "default_formatter": {
            "python": "autopep8",
            "r": "formatR"
        }
    }
}
```

- Click menu `Settings`>`Advanced Settings Editor`>`Keyboard Shortcuts`，add content：

```json
{
    "shortcuts": [
        {
            "command": "jupyterlab_code_formatter:autopep8",
            "keys": [
                "Ctrl K",
                "Ctrl M"
            ],
            "selector": ".jp-Notebook.jp-mod-editMode"
        }
    ]
}
```

## 2. Startup

```bash
$ jupyter lab
```