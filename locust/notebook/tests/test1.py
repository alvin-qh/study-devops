import random

from locust import Locust, TaskSet, task, events, seq_task


class TestTasks(TaskSet):

    def __init__(self, parent):
        super().__init__(parent)

    def on_start(self):
        print("one tasks started")

    def on_stop(self):
        print("one tasks stop")

    @task(2)
    class task1(TaskSet):

        def __init__(self, parent):
            super().__init__(parent)
            self._failure_rate = 0

        def on_start(self):
            self._failure_rate = random.randint(1, 10)
            print("sub tasks started, failure rate is: {}".format(self._failure_rate / 10 * 100))

        def on_stop(self):
            print("sub tasks stop")

        @seq_task(1)
        def task1_1(self):
            response_time = random.randint(100, 10000)
            response_length = random.randint(1000, 2000)

            if random.randint(0, self._failure_rate) == 0:
                events.request_failure.fire(request_type="demo", name="task1-1",
                                            response_time=response_time, response_length=response_length,
                                            exception=EOFError())
            else:
                events.request_success.fire(request_type="demo", name="task1-1",
                                            response_time=response_time, response_length=response_length)

        @seq_task(2)
        def task1_2(self):
            response_time = random.randint(100, 10000)
            response_length = random.randint(1000, 2000)

            events.request_success.fire(request_type="demo", name="task1-2",
                                        response_time=response_time, response_length=response_length)

    @task(1)
    def task2(self):
        response_time = random.randint(100, 10000)
        response_length = random.randint(1000, 2000)

        events.request_success.fire(request_type="demo", name="task2",
                                    response_time=response_time, response_length=response_length)


class TestSet(Locust):
    task_set = TestTasks
