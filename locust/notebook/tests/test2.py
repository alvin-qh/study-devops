import uuid

from locust import HttpLocust, task, TaskSet


class TestTasks(TaskSet):

    def on_start(self):
        self.client.post('/login', {
            'username': str(uuid.uuid4()),
            'password': '123456'
        })

    @task
    def index(self):
        self.client.get('/')


class TestSet(HttpLocust):
    task_set = TestTasks
