import asyncio
from client import Client
import websockets

class Controller:
    def __init__(self, rounds, required_clients):
        self.clients = {}
        self.rounds = rounds
        self.required_clients = required_clients
        self.current_round = 0
        self.response_received = asyncio.Event()

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
        if not self.response_received.is_set():
            self.response_received.set()
            await self.process_response(client, message)

    async def start_rounds(self):
        """Start and manage the rounds of requests and responses."""
        for _ in range(self.rounds):
            self.response_received.clear()
            await self.dispatch_requests()
            # await self.response_received.wait()
            self.current_round += 1

    async def generate_requests(self):
        """Generate requests for all clients."""
        pass  # Placeholder for request generation logic

    async def dispatch_requests(self):
        """Dispatch the generated requests to each client."""
        requests = await self.generate_requests()
        for client in self.clients.values():
            await client.websocket.send(requests[client])

    async def process_response(self, client, message):
        """Process a client's response. Processing logic goes here."""
        print(f"Received response from {client.id}: {message}")
        

    async def client_handler(self, websocket):
        """Manage client connections and messages."""
        await self.register_client(websocket)
        try:
            async for message in websocket:
                await self.message_handler(self.clients[websocket], message)
        except websockets.exceptions.ConnectionClosedError:
            print(f"Client {websocket.remote_address} disconnected.")
        finally:
            await self.unregister_client(websocket)
