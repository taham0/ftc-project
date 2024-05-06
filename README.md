### Create a python virtual environemnt
`python3 -m venv autochaos`

### Activate virtual environment
`source autochaos/bin/activate`

### Install dependencies for the controller
`pip install -r server/requirements.txt`

### Run the controller
`python3 main.py <fault-tolerant-mode>` 

**Supported `<fault-tolerant-mode>` are "fcfs" and "tmr"**

### Spawn application node
1. Open core-models as an android studio project
2. Start the virtual device
3. Build and run the project
4. Press "connect" to connect node to the controller
5. Press "chaos" to apply a faulty configuration
