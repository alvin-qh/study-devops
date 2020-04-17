# Study mongodb

## Setup environment

### Install python virtualenv

```bash
$ python -m venv .venv --prompt='study-devops-mongo'
$ source .venv/bin/activate
```

### Install and enable python code format extention

```bash
$ jupyter labextension install @ryantam626/jupyterlab_code_formatter
$ jupyter serverextension enable --py jupyterlab_code_formatter
```

### Install bash extention

```bash
$ python -m bash_kernel.install
```

## Start

### Start 

In `docker` folder:

```bash
$ docker-compose up
```

## Start jupyter lab notebook

```bash
$ jupyter lab
```

