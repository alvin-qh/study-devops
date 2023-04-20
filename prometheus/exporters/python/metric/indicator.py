from prometheus_client import Counter, Gauge
import random

call_times = Counter(
    "demo_function_call_times",
    "Times of call demo function",
)


def counter_demo() -> None:
    call_times.inc(float(random.randint(1, 10)))


usage = Gauge(
    "demo_some_resource_usage",
    "Usage of Some Resources"
)

def gauge_demo() -> None:
    usage.set(random.randint(100, 1000))
