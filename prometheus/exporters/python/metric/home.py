from typing import Any, Dict

from flask import Blueprint

bp = Blueprint("home", __name__)


@bp.route("/", methods=["GET"])
def home() -> Dict[str, Any]:
    return {
        "title": "Welcome",
        "content": "Welcome to Prometheus Exporter Python Demo, "
                   "redirect to '/metrics' to visit result",
    }
