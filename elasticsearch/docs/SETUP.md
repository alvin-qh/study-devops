Install and Setup
===

## 1. Download and install

Download zip archive from [home page](https://www.elastic.co/cn/downloads/elasticsearch)

Unzip downloaded archive

## 2. Run

Run `bin/elasticsearch` to start elasticsearch, then visit localhost: http://127.0.0.1:9200/

## 3. Plugins

```bash
$ bin/elasticsearch-plugin install analysis-icu
$ bin/elasticsearch-plugin install org.wikimedia.search:extra:6.3.1.2
$ bin/elasticsearch-plugin install file:///Users/alvin/Downloads/elasticsearch-analysis-stconvert-6.3.1.zip
$ bin/elasticsearch-plugin install analysis-smartcn
```

## 4. Kibana

Download gz archive from [home page](https://www.elastic.co/cn/downloads/kibana)

Run `bin/kibana` to start kibana, then visit localhost: http://127.0.0.1:5601