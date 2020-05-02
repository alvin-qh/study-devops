# Use Notebook

## 1. Setup

### 1.1. Install python virtualenv

```bash
$ python -m venv .venv --prompt='study-devops-ansible'
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

## 2. Startup

```bash
$ jupyter lab
```