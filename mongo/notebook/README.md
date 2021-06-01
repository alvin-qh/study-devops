# MongoDB Notebook

## 1. Setup

See [Jupyter lab setup guide](../../README.md)

Run pip install python requirement packages

```bash
$ bash ./setup.sh
```

Install 

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