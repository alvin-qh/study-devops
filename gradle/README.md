# GRADLE

## 1. Setup

### 1.1. Install jupyter bash extension

#### 1.1.1. Install pip package

```bash
$ pip install bash_kernel
```

#### 1.1.2. Build and install extension

```bash
$ python -m bash_kernel.install
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
