

from locust.clients import HttpSession, Response

from . import conf


def login(client: HttpSession) -> None:
    payload = {
        "username": conf.USER,
        "password": conf.PASSWORD
    }

    with client.post("/login", payload, catch_response=True) as resp:
        if resp.status_code != 302:
            resp.failure("Login failed")
