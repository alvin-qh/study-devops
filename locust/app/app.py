import gevent.monkey

try:
    gevent.monkey.patch_all()
finally:
    pass

import os
import sqlite3 as db
from datetime import datetime
from typing import Any, List, Optional, Tuple, Union

from flask import Flask, g, redirect, render_template, request, session
from werkzeug.wrappers.response import Response
from wtforms import Form, PasswordField, StringField
from wtforms.validators import DataRequired

# 创建数据库连接字符串
DATABASE = os.path.join(os.path.dirname(__file__), "data.db")

# 创建 Flask app 对象
app = Flask(__name__)
app.config["SECRET_KEY"] = "\xfd{H\xe5<\x95\xf9\xe3\x96.5\xd1\x01O<!\xd5\xa2\xa0\x9fR\"\xa1\xa8"


class LoginForm(Form):
    """
    登录表单类型
    """
    username = StringField(validators=[DataRequired("username required")])
    password = PasswordField(validators=[DataRequired("password required")])


def get_conn() -> db.Connection:
    """
    获取数据库连接

    Returns:
        _type_: _description_
    """
    conn = getattr(g, "_conn", None)
    if not conn:
        conn = g._conn = db.connect(DATABASE)
    return conn


def query_sql(
    sql: str, args: Optional[Tuple] = None, one: bool = False,
) -> Union[Any, List[Any]]:
    """
    执行查询 SQL 语句

    Args:
        query (str): 查询 SQL
        args (Optional[Tuple], optional): 查询参数. Defaults to `None`.
        one (bool, optional): 返回单一结果或集合. Defaults to `False`.

    Returns:
        Union[Any, List[Any]]: 查询结果
    """
    if args is None:
        args = ()

    with get_conn() as conn:
        cur = conn.execute(sql, args)
        rv = cur.fetchall()
        return (rv[0] if rv else None) if one else rv


def execute_sql(sql: str, args: Optional[Tuple] = None) -> db.Cursor:
    """
    执行 SQL 语句

    Args:
        sql (str): SQL 语句
        args (Optional[Tuple], optional): 结果游标对象. Defaults to `None`.

    Returns:
        db.Cursor: 游标对象
    """
    if args is None:
        args = ()

    with get_conn() as conn:
        cur = conn.cursor()
        cur.execute(sql, args)
    return cur


def initialize_db():
    """
    初始化数据库
    """
    with app.app_context():
        execute_sql("CREATE TABLE IF NOT EXISTS users ("
                    "   id INTEGER PRIMARY KEY,"
                    "   username VARCHAR(100) NOT NULL,"
                    "   password VARCHAR(100) NOT NULL,"
                    "   UNIQUE(username)"
                    ");")


# 初始化数据库
initialize_db()


@app.teardown_appcontext
def close_connection(exception: Any) -> None:
    """
    在请求结束后执行, 关闭数据库连接

    Args:
        exception (Exception): 异常对象, 标识请求处理过程中发生的异常
    """
    conn = getattr(g, "_conn", None)
    if conn:
        conn.close()


@app.route("/login", methods=["POST"])
def login() -> Response:
    """
    处理登录请求

    Returns:
        Response: 响应对象
    """
    form = LoginForm(request.form)

    # 执行查询
    with app.app_context():
        user_ids = query_sql(
            "SELECT id FROM users WHERE username = ?", (form.username.data,),
        )

    if not user_ids:
        # 如果用户未查询到, 则创建该用户
        with app.app_context():
            cur = execute_sql(
                "INSERT INTO users(username, password) VALUES (?, ?)",
                (form.username.data, form.password.data),
            )

        # 获取用户 id
        user_id = cur.lastrowid
    else:
        # 获取查询用户结果 id
        user_id = user_ids[0]

    # 将用户信息存入会话上下文
    session["user_id"] = user_id

    return redirect("/")


@app.route("/", methods=["GET"])
def hello() -> Union[str, Response]:
    # 从会话上下文中获取用户 id
    user_id = session.get("user_id")
    if not user_id:
        return redirect("/login")

    # 渲染页面
    return render_template("index.html", ts=datetime.now().isoformat(), content="Hello")


if __name__ == "__main__":
    app.run("0.0.0.0", port=3000, debug=True)
