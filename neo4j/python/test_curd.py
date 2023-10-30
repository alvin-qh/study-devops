import conf

import neo4j

driver: neo4j.Driver


def setup_function() -> None:
    """
    初始化每次测试
    """
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


def teardown_function() -> None:
    """
    结束测试
    """
    # 关闭 neo4j 连接
    driver.close()


def test_create_node() -> None:
    """
    测试创建一个节点
    """

    # 创建节点语句，创建一个 Person 类型的节点并返回
    statement = """
CREATE (person: Person {id: $id, name: $name, gender: $gender})
RETURN person
    """

    # 执行语句
    with driver.session() as session:
        # 返回执行结果
        r = session.run(statement, {  # 设置语句参数
            "id": 1,
            "name": "Alvin",
            "gender": "M"
        }).single()

    # 返回一个 Record 类型结果
    assert isinstance(r, neo4j.Record)
    assert r.keys() == ["person"]

    # 获取返回的结果内容
    person = r["person"]
    assert dict(person) == {
        "id": 1,
        "name": "Alvin",
        "gender": "M",
    }


def test_merge_node() -> None:
    """
    测试创建一个节点
    """

    statement = """
CREATE (person: Person {id: $id, name: $name, gender: $gender})
    """

    with driver.session() as session:
        session.run(statement, {
            "id": 1,
            "name": "Alvin",
            "gender": "M"
        })

    # 执行节点合并，如果不存在则创建该节点
    statement = """
MERGE (person: Person {id: $id, name: $name, gender: $gender, birthday: $birthday})
    """

    # 执行合并语句
    with driver.session() as session:
        session.run(statement, {  # 设置语句参数
            "id": 1,
            "name": "Alvin",
            "gender": "M",
            "birthday": "1981-03-17",
        })


def test_update_node() -> None:
    """
    测试更新一个节点
    """

    # 建立节点
    statement = """
CREATE (person: Person {id: $id, name: $name, gender: $gender})
    """

    with driver.session() as session:
        session.run(statement, {  # 设置语句参数
            "id": 1,
            "name": "Alvin",
            "gender": "M"
        })

    # 更新节点语句，添加生日信息
    statement = """
MATCH (person: Person)
WHERE person.id = $id
SET person.birthday = $birthday
RETURN person
    """

    # 执行更新语句
    with driver.session() as session:
        r = session.run(statement, {
            "id": 1,
            "birthday": "1981-03-17",
        }).single()

        assert r is not None

    # 查看执行后结果
    person = r["person"]

    assert person["id"] == 1
    assert person["birthday"] == "1981-03-17"


def test_query_node() -> None:
    """
    测试查询一个节点
    """

    # 建立节点
    statement = """
CREATE (person: Person {id: $id, name: $name, gender: $gender})
    """

    with driver.session() as session:
        session.run(statement, {  # 设置语句参数
            "id": 1,
            "name": "Alvin",
            "gender": "M"
        })

    # 查询所有节点
        statement = """
MATCH (person: Person)
RETURN person
    """

    with driver.session() as session:
        r = list(session.run(statement))

    assert len(r) == 1
    assert r[0]["person"]["name"] == "Alvin"

    # 查询符合条件的节点
    statement = """
MATCH (person: Person)
WHERE person.name = $name
RETURN person
    """

    with driver.session() as session:
        r = list(session.run(statement, {
            "name": "Alvin",
        }))

    assert len(r) == 1
    assert r[0]["person"]["name"] == "Alvin"


def test_delete_node() -> None:
    """
    测试删除一个节点
    """

    # 创建节点
    statement = """
CREATE (person: Person {id: $id, name: $name, gender: $gender})
    """

    with driver.session() as session:
        session.run(statement, {  # 设置语句参数
            "id": 1,
            "name": "Alvin",
            "gender": "M"
        })

    # 删除节点
    statement = """
MATCH (person: Person)
WHERE person.id = $id
DELETE person
RETURN person
    """

    # 执行删除语句
    with driver.session() as session:
        r = session.run(statement, {  # 设置语句参数
            "id": 1,
        }).single()

        assert r is not None

    assert r["person"].id

    # 执行查询
    statement = """
MATCH (person: Person)
WHERE person.id = $id
RETURN person
    """

    with driver.session() as session:
        r = session.run(statement, {  # 设置语句参数
            "id": 1,
        }).single()

    assert r is None
