from itertools import repeat

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


def test_make_relationship():
    """
    测试建立两个节点的关系（Relationship）
    """

    # 建立两个节点
    statement = """
CREATE (person:Person {id: $id, name: $name, gender: $gender})
    """

    args = [
        {
            "id": 1,
            "name": "Alvin",
            "gender": "M"
        },
        {
            "id": 2,
            "name": "Emma",
            "gender": "F"
        }
    ]

    for stat, args in zip(repeat(statement, 2), args):
        with driver.session() as session:
            session.run(stat, args)

    # 建立关系
    # CREATE 命令可以换为 MERGE 命令，表示对已有关联进行修改
    statement = """
MATCH (p1: Person), (p2: Person)
WHERE p1.id = $id1 AND p2.id = $id2
CREATE (p1)-[r: Married]->(p2)
RETURN p1, p2, r
    """

    with driver.session() as session:
        r = session.run(statement, {
            "id1": 1,
            "id2": 2,
        }).single()

    assert r["p1"]["id"] == 1
    assert r["p1"]["name"] == "Alvin"

    assert r["p2"]["id"] == 2
    assert r["p2"]["name"] == "Emma"

    assert r["r"].type == "Married"
    assert r["r"].nodes[0]["name"] == "Alvin"
    assert r["r"].nodes[1]["name"] == "Emma"


def test_query_by_relation():
    # 建立关系
    # 创建 p1, p2 和 cat
    # p1 - Married -> p2
    # p2 - Feed -> cat
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

    # 查找关系，找到养猫的人
    statement = """
MATCH (person: Person)-[r: Feed]->(cat: Cat)
WHERE cat.name = $cat_name
RETURN person, cat, r
    """

    with driver.session() as session:
        r = session.run(statement, {
            "cat_name": "Simba",
        }).single()

    assert r["person"]["id"] == 2
    assert r["person"]["name"] == "Emma"

    assert r["cat"]["id"] == 3
    assert r["cat"]["name"] == "Simba"

    assert r["r"].type == "Feed"
    assert r["r"].nodes[0]["name"] == "Emma"
    assert r["r"].nodes[1]["name"] == "Simba"

    # 查找所有和猫相关的节点
    # r* 表示可以跨越多个关系
    statement = """
MATCH (person: Person)-[r*]->(cat: Cat)
WHERE cat.name = $cat_name
RETURN DISTINCT person
    """

    with driver.session() as session:
        r = list(
            session.run(statement, {
                "cat_name": "Simba",
            })
        )

    assert len(r) == 2
    assert r[0]["person"]["name"] == "Emma"
    assert r[1]["person"]["name"] == "Alvin"

    # 查询和猫有关系的节点及其节点关系
    # Married*, Feed* 表示可以跨越多个指定关系
    statement = """
MATCH (p1: Person)-[m: Married*]-(p2: Person)-[f: Feed*]->(c: Cat)
WHERE c.name = $cat_name
RETURN DISTINCT p1, p2, c, m, f
    """

    with driver.session() as session:
        r = session.run(statement, {
            "cat_name": "Simba",
        }).single()

    assert r["p1"]["name"] == "Alvin"
    assert r["p2"]["name"] == "Emma"
    assert r["c"]["name"] == "Simba"

    assert len(r["m"]) == 1  # 带 * 后缀的关系结果为数组
    assert r["m"][0].type == "Married"

    assert len(r["f"]) == 1  # 带 * 后缀的关系结果为数组
    assert r["f"][0].type == "Feed"


def test_find_graph_of_relation():
    """
    测试查找节点间关系的路径
    """

    # 建立关系
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

    # 查询到猫节点的路径
    # r*2 表示查询具备两层关系的结果，也可以为 r*1..2，表示查询有 1~2 层关系的结果
    statement = """
MATCH g = (p: Person)-[r*2]->(c: Cat)
WHERE c.name = $cat_name
RETURN g, length(g)
    """

    with driver.session() as session:
        r = list(
            session.run(statement, {
                "cat_name": "Simba",
            })
        )

    # 结果包含一条路径
    assert len(r) == 1

    # 确认路径节点和关系
    assert len(r[0]["g"].nodes) == 3
    assert r[0]["g"].start_node["name"] == "Alvin"
    assert r[0]["g"].end_node["name"] == "Simba"
    assert r[0]["g"].nodes[0]["name"] == "Alvin"
    assert r[0]["g"].nodes[1]["name"] == "Emma"
    assert r[0]["g"].nodes[2]["name"] == "Simba"

    assert len(r[0]["g"].relationships) == 2
    assert r[0]["g"].relationships[0].type == "Married"
    assert r[0]["g"].relationships[1].type == "Feed"


def test_find_last_node_in_each_relation():
    """
    测试查找关系路径里最后的节点
    """

    # 建立关系
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

    # 查询最后一个节点是猫，且猫不是第一个节点的情况
    statement = """
MATCH ()-[*]->(c: Cat)
WHERE NOT (c)-[]->()
RETURN DISTINCT c
    """

    with driver.session() as session:
        r = list(
            session.run(statement, {
                "cat_name": "Simba",
            })
        )

    assert len(r) == 1
    assert r[0]["c"]["name"] == "Simba"


def test_delete_by_relation():
    """
    测试根据关系删除数据
    """

    # 建立关系
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

    # 根据关系删除数据
    statement = """
MATCH ()-[mr: Married]->()
MATCH ()-[fr: Feed]->()
OPTIONAL MATCH (ps: Person)
OPTIONAL MATCH (cs: Cat)
DELETE mr, fr, ps, cs
RETURN mr, fr, ps, cs
    """

    with driver.session() as session:
        r = list(
            session.run(statement, {
                "cat_name": "Simba",
            })
        )

    assert len(r) == 2
