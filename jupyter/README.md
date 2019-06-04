Jupyter
===



## 1. Install

### 1.1 Install virtualenv

```bash
$ python -m venv .venv
$ source .venv/bin/activate
```

### 1.2 Install jupyter

Install core program

```bash
$ pip install jupyter
```

Install and setup theme

```bash
$ pip install jupyterthemes
$ jt -r						# reset theme
$ jt -t onedork		# accept dark theme
```

Install extension

```bash
$ pip install jupyter_contrib_nbextensions
$ .venv/bin/jupyter contrib nbextension install --user

$pip install jupyter_nbextensions_configurator
$ .venv/bin/jupyter nbextensions_configurator enable --user
```

### 1.3 Start jupyter

```bash
$ .venv/bin/jupyter notebook
```

Open `Nbextensions` menu, enable `Hinterland` option