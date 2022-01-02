from datetime import date

import conf

from elasticsearch import Elasticsearch

es = Elasticsearch(hosts=conf.HOSTS)

INDEX_NAME = "person"


def teardown_function():
    """
    每个测试结束后执行
    """
    # 判断索引是否存在
    exists = es.indices.exists(index=INDEX_NAME)
    if exists:
        es.indices.delete(index=INDEX_NAME)  # 删除已存在的索引


def test_connetion():
    """
    测试服务器连通性
    """
    assert es is not None

    pong = es.ping()
    assert pong


def test_create_index():
    """
    测试创建索引
    """
    _create_person_index()


def _create_person_index():
    index_settings = {
        "settings": {
            "number_of_shards": 1,
            "number_of_replicas": 1
        },
        "mappings": {
            "properties": {
                "name": {
                    "type": "keyword",
                    "copy_to": "ALL"
                },
                "gender": {
                    "type": "keyword",
                    "copy_to": "ALL"
                },
                "birthday": {
                    "type": "date",
                    "copy_to": "ALL"
                },
                "role": {
                    "type": "keyword",
                    "copy_to": "ALL"
                },
                "department": {
                    "properties": {
                        "college": {
                            "type": "keyword",
                            "copy_to": "ALL"
                        },
                        "program": {
                            "type": "keyword",
                            "copy_to": "ALL"
                        }
                    }
                },
                "note": {
                    "type": "text",
                    "copy_to": "ALL"
                },
                "ALL": {
                    "type": "text"
                }
            }
        }
    }

    r = es.indices.create(
        index=INDEX_NAME,
        mappings=index_settings["mappings"],
        settings=index_settings["settings"],
        ignore=400,
    )
    assert r["acknowledged"]
    assert r["shards_acknowledged"]
    assert r["index"] == INDEX_NAME


def test_create_docs():
    """
    测试创建文档
    """

    _create_person_index()

    id = "001"
    doc = {
        "name": "Alvin",
        "gender": "M",
        "birthday": "1981-03-17",
        "role": "STUDENT",
        "department": {
            "college": "Computer science",
            "program": "Network engineering"
        },
        "note": "Top Outstanding student"
    }

    r = es.index(id=id, index=INDEX_NAME, document=doc)
    assert r["result"] == "created"
