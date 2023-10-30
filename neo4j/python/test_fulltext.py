import conf
from neo4j.exceptions import ClientError

import neo4j

driver: neo4j.BoltDriver


def setup_function():
    # 连接到 neo4j
    global driver
    driver = neo4j.GraphDatabase.driver(
        conf.URL, auth=(conf.USERNAME, conf.PASSWORD), encrypted=False
    )

    # 删除所有节点
    with driver.session() as session:
        # 删除所有关系
        session.run("""
MATCH ()-[n]->()
DELETE n
        """)

        # 删除所有节点
        session.run("""
MATCH (n)
DELETE n
        """)


def teardown_function():
    driver.close()


def test_list_all_analyzers():
    """
    列出 neo4j 支持的语法分析器
    """

    statement = """
CALL db.index.fulltext.listAvailableAnalyzers()
    """

    with driver.session() as session:
        r = list(
            session.run(statement)
        )

    assert len(r) == 46  # 目前 neo4j 包含 46 种语法分析器


def test_create_index_by_analyzers():
    """
    创建全文索引
    """

    # 创建数据
    statement = """
CREATE (p1: Person {id: $id_1, name: $name_1, gender: $gender_1})
CREATE (p2: Person {id: $id_2, name: $name_2, gender: $gender_2})
CREATE (cat: Cat {id: $id_3, name: $name_3})
CREATE (p1)-[m: Married]->(p2)
CREATE (p2)-[f: Feed]->(cat)
    """

    with driver.session() as session:
        session.run(statement, {
            "id_1": 1,
            "name_1": "Alvin",
            "gender_1": "Male",
            "id_2": 2,
            "name_2": "Emma",
            "gender_2": "Female",
            "id_3": 3,
            "name_3": "Simba",
        })

    # 为人和猫的名字字段创建全文索引
    # 创建 ix_name 索引，将 Person 和 Cat 类型数据的 name 字段进行索引
    statement = """
CALL db.index.fulltext.createNodeIndex("ix_name", ["Person", "Cat"], ["name"], {analyzer: "cjk"})
    """

    with driver.session() as session:
        try:
            session.run(statement)
        except ClientError:
            pass

    # 通过全文索引查询数据
    # 查询 ix_name 索引值为 Alvin 的节点，返回节点和得分
    statement = """
CALL db.index.fulltext.queryNodes("ix_name", "Alvin") YIELD node, score
RETURN node, score
    """

    with driver.session() as session:
        r = list(
            session.run(statement)
        )

    assert len(r) == 1
    assert r[0]["node"]["name"] == "Alvin"
    assert r[0]["score"] > 0

    # 删除全文索引
    statement = """
CALL db.index.fulltext.drop("ix_name")
    """

    with driver.session() as session:
        session.run(statement)
