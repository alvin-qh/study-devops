import os
import sqlite3
from datetime import datetime

from flask import Flask, request, g, redirect, session, render_template
from wtforms import Form, StringField, PasswordField
from wtforms.validators import DataRequired

DATABASE = os.path.join(os.path.dirname(__file__), 'data.db')

app = Flask(__name__)
app.config['SECRET_KEY'] = '\xfd{H\xe5<\x95\xf9\xe3\x96.5\xd1\x01O<!\xd5\xa2\xa0\x9fR"\xa1\xa8'


class LoginForm(Form):
    username = StringField(validators=[DataRequired('username required')])
    password = PasswordField(validators=[DataRequired('password required')])


def get_db():
    db = getattr(g, '_db', None)
    if not db:
        db = g._db = sqlite3.connect(DATABASE)
    return db


def query_db(query, args=(), one=False):
    cur = get_db().execute(query, args)
    rv = cur.fetchall()
    cur.close()
    return (rv[0] if rv else None) if one else rv


def execute_db(sql, args=()):
    with get_db() as db:
        cur = db.cursor()
        cur.execute(sql, args)
    return cur


def initialize_db():
    with app.app_context():
        execute_db('CREATE TABLE IF NOT EXISTS users ('
                   '   id INTEGER PRIMARY KEY,'
                   '   username VARCHAR(100) NOT NULL,'
                   '   password VARCHAR(100) NOT NULL,'
                   '   UNIQUE(username)'
                   ');')


initialize_db()


@app.teardown_appcontext
def close_connection(exception):
    db = getattr(g, '_db', None)
    if db:
        db.close()


@app.route('/login', methods=['POST'])
def login():
    form = LoginForm(request.form)

    with app.app_context():
        user_ids = query_db('SELECT id FROM users WHERE username = ?', (form.username.data,))

    if not user_ids:
        with app.app_context():
            cur = execute_db('INSERT INTO users(username, password)VALUES(?, ?)',
                             (form.username.data, form.password.data))
        user_id = cur.lastrowid
    else:
        user_id = user_ids[0]

    session['user_id'] = user_id

    return redirect('/')


@app.route('/', methods=['GET'])
def hello():
    user_id = session['user_id']
    if not user_id:
        return redirect('/login')
    return render_template('index.html', ts=datetime.now().isoformat(), content='Hello')


if __name__ == '__main__':
    app.run('0.0.0.0', port=3000, debug=True)
