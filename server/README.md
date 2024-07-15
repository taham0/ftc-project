<h2>Controller Node</h2>

The controller node, implemented on a laptop, acts as the central
hub of the system. It hosts an object detection platform that is both
low-latency and accurate, communicating with the edge nodes
through WebSockets for efficient communication.
The controller schedules requests to the edge nodes and manages
the responses, using different strategies to balance fault tolerance
with latency.
