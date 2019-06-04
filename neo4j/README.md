# Neo4J

## Install

### With docker

```bash
docker run \
    --publish=7474:7474 \
    --publish=7687:7687 \
    --volume=$HOME/Workspace/Docker/neo4j/data:/data \
    --volume=$HOME/Workspace/Docker/neo4j/logs:/logs \
    neo4j
```
