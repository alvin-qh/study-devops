#!/usr/bin/env bash

# Comman parameters:
#   h: header names of display list
#   v: show header names
#   format=json: format of result
#   s: sort

# Get master node information
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/master?v';

# Get all indices informations
# Paramters:
#   bytes: unit of byte size
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/indices?h=index,docs.count,store.size&bytes=mb&format=json&pretty&s=docs.count:desc';

# Get shards of all indices
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/shards?h=index,shard,prirep,state,unassigned.reason&v';

# Get shards of all indices
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/shards/study?h=index,shard,prirep,state,unassigned.reason&v';

# Show cluster health status
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/health?v';

# Show nodes in cluster
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/nodes?v&h=id,ip,node.role,name,disk.avail,heap.percent,ram.percent,cpu,master';


# Show all plugins of each node
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/plugins?v';

# Show attributes of each node
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/nodeattrs?v';

# Show recovery view of indecs
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/recovery/study?v';

# Show snapshot repositories
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/repositories?v';

# Show snapshot backup
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/snapshots/my_backup?v';

# Show threadpool informations
curl -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' \
     -X GET 'http://localhost:9200/_cat/thread_pool/search?v';