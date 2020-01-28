# Neo4J

## Setup

### Install jupyter bash extension

#### Install pip package

```bash
$ pip install bash_kernel
```

#### Build and install extension

```bash
$ python -m bash_kernel.install
```

### Enable "Jupyterlab Code Formatter" extension

#### Install and enable extension

```bash
$ jupyter labextension install @ryantam626/jupyterlab_code_formatter
$ jupyter serverextension enable --py jupyterlab_code_formatter
```

> [Jupyterlab Code Formatter Homepage](https://jupyterlab-code-formatter.readthedocs.io/)

#### Config extension

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



## Run neo4j

### With docker

```bash
docker run \
    -p 7474:7474 \
    -p 7687:7687 \
    -v $(pwd)/import:/var/lib/neo4j/import
    -v $(pwd)/data:/data \
    -v $(pwd)/logs:/logs \
    -v $(pwd)/plugins:/plugins
    neo4j
```
