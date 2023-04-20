from apscheduler.schedulers.background import BackgroundScheduler
from flask import Flask

from .home import bp as home_bp
from .indicator import counter_demo, gauge_demo

app = Flask(__name__)
app.register_blueprint(home_bp)

sched = BackgroundScheduler()
sched.add_job(counter_demo, "interval", seconds=10)
sched.add_job(gauge_demo, "interval", seconds=5)
