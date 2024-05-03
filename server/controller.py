import asyncio
import base64
from client import Client
from image_loader import ImageLoader
import websockets
import json
import logging
import time

import matplotlib.pyplot as plt
import numpy as np

log = logging.getLogger("Controller")
log.setLevel(logging.INFO)

plt.set_loglevel (level = 'warning')

class Controller:
    def __init__(self, rounds, required_clients, mode, data_dir):
        self.clients = {}

        # FCFS, TMR 
        self.mode = mode
        self.rounds = rounds
        self.required_clients = required_clients
        self.current_round = 0
        self.response_count = 0
        self.all_responses_received = asyncio.Event()
        self.image_loader = ImageLoader(data_dir)
        self.image_loader.data_loader()
        self.start = 0
        self.latencies = []
        self.current_result = []

        self.done_task = False

    def create_request(self, type, round, data=None):
        """Create a request object"""
        request = {}
        request["type"] = type
        request["round"] = round
        request["blob"] = data

        return json.dumps(request)

    async def register_client(self, websocket):
        """Register a new client and start rounds if conditions are met."""
        client = Client(websocket)
        self.clients[websocket] = client
        if len(self.clients) == self.required_clients:
            asyncio.create_task(self.start_rounds())

    async def unregister_client(self, websocket):
        """Unregister a client."""
        self.clients.pop(websocket, None)

    async def dotmr(self) -> bool:
        """Perform TMR logic."""
        ## Check if atlest 2 responses are the same
        if self.current_result[0] == self.current_result[1] or self.current_result[0] == self.current_result[2]:
            return True
        elif self.current_result[1] == self.current_result[2]:
            return True
        else:
            False

    async def message_handler(self, client, message):
        """Process the first response in the current round."""
        log.debug(f'Received message from {client.id}')
        try:
            message = json.loads(message)
        except json.JSONDecodeError as e:
            log.info('JSONDecodeError request not in spec')
            return
        
        
        if self.current_round == message["round"]:
            self.responses_count += 1
            log.debug(f'Processed message from {client.id}, total responses: {self.responses_count}')
            if self.responses_count == self.required_clients:
                self.all_responses_received.set()

            await self.process_response(client, message)

            match self.mode:
                case "fcfs":
                    # In this case we just take the first response we get and move to the next round
                    # due to time constraints we receive the next two responses instead of cancelling them
                    if self.done_task:
                        return
                    elapsed = time.time() - self.start
                    self.latencies.append(elapsed)
                    self.done_task = True
                    return
                case "tmr":
                    if self.responses_count == self.required_clients:
                        await self.dotmr()
                        elapsed = time.time() - self.start
                        self.latencies.append(elapsed)
                        return
                    else:
                        return

        else:
            log.error(f'Round mismatch: {self.current_round} != {message["round"]}')
            await client.websocket.send("{'type': 'error', 'message', 'round mismatch'}")
            return
        
        log.info(f'Discarded message from {client.id}')


    async def start_rounds(self):
        """Start and manage the rounds of requests and responses."""
        for _ in range(self.rounds):
            log.info(f'Starting round {self.current_round}')

            self.all_responses_received.clear()
            self.responses_count = 0
            await self.dispatch_requests(self.current_round)
            await self.all_responses_received.wait()

            log.info(f'Round {self.current_round} complete.')
            self.done_task = False
            self.current_round += 1

        log.info(f'All rounds complete. Average latency: {sum(self.latencies) / len(self.latencies)}')
        
        # do not cut the axes of the plot start from 0
        
        plt.figure(figsize=(10, 5))
        plt.plot(self.latencies, marker='o')
        plt.xlabel('Round')
        plt.ylabel("Latency (s) Avg:{:.2f}".format(sum(self.latencies)/len(self.latencies)))
        plt.title('Latency per round')
        plt.grid(True)
        plt.show()

    async def generate_requests(self, round):
        """Generate requests for all clients."""
        return self.create_request("image", round, self.image_loader.get_image(round))
        # return self.create_request("image", round)

    async def dispatch_requests(self, round):
        """Dispatch the generated requests to each client."""
        request = await self.generate_requests(round)
        self.start = time.time()
        for client in self.clients.values():
            await client.websocket.send(request)

    async def process_response(self, client, message):
        """Process a client's response. Processing logic goes here."""
        
        match message["type"]:
            case "REG": # Registration message
                log.info(f"Client {client.id} registered.")
            case "LB": # Labels received
                labels = message["data"]
                labels = base64.b64decode(labels)
                
                log.info(f"Received labels from {client.id} - {labels}")
                self.current_result.append(labels)

        pass
        
    async def clear_messages(self):
    # Iterate over all client connections and clear pending messages
        for websocket in self.clients.keys():
            try:
                # Attempt to receive a message from the client (non-blocking)
                while True:
                    message = await asyncio.wait_for(websocket.recv(), timeout=0.1)
            except asyncio.TimeoutError:
                # No more messages available, move to the next client
                continue

    async def client_handler(self, websocket):
        """Manage client connections and messages."""
        await self.register_client(websocket)
        
        log.info(f"Client {self.clients[websocket].id} running.")

        try:
            async for message in websocket:
                await self.message_handler(self.clients[websocket], message)
        except websockets.exceptions.ConnectionClosedError:
            log.info(f"Client {websocket.remote_address} disconnected. closed")
        finally:
            log.info(f"Client {self.clients[websocket].id} disconnected.")
            await self.unregister_client(websocket)
