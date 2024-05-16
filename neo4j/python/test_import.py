import conf

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


def xtest_import_from_csv():
    """
    测试从 csv 中导入数据
    """

    statement = """
LOAD CSV WITH HEADERS FROM "file:///data.csv" AS line
MERGE (p1:Person {id: line["id"], name: line["name"]})
MERGE (p2:Person {id: line["married"]})
MERGE (p1)-[:Married]->(p2)
RETURN p1, p2
    """

    with driver.session() as session:
        r = list(
            session.run(statement)
        )

    assert len(r) == 2
