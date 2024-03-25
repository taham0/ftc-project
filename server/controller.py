import asyncio
from client import Client
import websockets
import json

class Controller:
    def __init__(self, rounds, required_clients):
        self.clients = {}
        self.rounds = rounds
        self.required_clients = required_clients
        self.current_round = 0
        self.response_received = asyncio.Event()

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

    async def message_handler(self, client, message):
        """Process the first response in the current round."""
        message = json.loads(message)
        
        if self.current_round != message["round"]:
            return
        
        print(f'Received message from {client.id}')
        if not self.response_received.is_set():
            print(f'Processed message from {client.id}')
            self.response_received.set()
            await self.process_response(client, message)

            return
        
        print(f'Discarded message from {client.id}')

    async def start_rounds(self):
        """Start and manage the rounds of requests and responses."""
        for _ in range(self.rounds):
            print(f'Starting round {self.current_round}')

            self.response_received.clear()
            await self.dispatch_requests(self.current_round)
            await self.response_received.wait()

            print(f'Round {self.current_round} complete.')

            self.current_round += 1

    async def generate_requests(self, round):
        """Generate requests for all clients."""
        return self.create_request("FR", round)

    async def dispatch_requests(self, round):
        """Dispatch the generated requests to each client."""
        request = await self.generate_requests(round)
        for client in self.clients.values():
            await client.websocket.send(request)

    async def process_response(self, client, message):
        """Process a client's response. Processing logic goes here."""
        # print(f"Received response from {client.id}: {message}")
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
        
        print(f"Client {self.clients[websocket].id} running.")

        try:
            async for message in websocket:
                await self.message_handler(self.clients[websocket], message)
        except websockets.exceptions.ConnectionClosedError:
            print(f"Client {websocket.remote_address} disconnected.")
        finally:
            await self.unregister_client(websocket)
