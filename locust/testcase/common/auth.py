import uuid

from locust.clients import HttpSession


def login(client: HttpSession) -> None:
    """
    执行登录测试

    Args:
        client (HttpSession): 测试客户端对象
    """
    payload = {
        "username": f"{uuid.uuid4()}",
        "password": "123456"
    }

    # 向 /login 地址发送 post 请求
    with client.post(
        "/login",
        payload,  # 发送请求表单
        catch_response=True,  # 获取响应
        allow_redirects=False,  # 禁止重定向
    ) as resp:
        # 判断结果是否正确
        if resp.status_code != 302:
            resp.failure(f"Login failed: {resp.status_code}")
