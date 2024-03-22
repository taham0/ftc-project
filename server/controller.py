from modules import Client

class Controller:
    def __init__(self):
        self.clients = {}

    async def process_request(self, message):
        """Process client request."""
        print(message)

    async def register_client(self, websocket):
        """Register a new client."""
        client = Client(websocket)
        self.clients[websocket] = client

    async def unregister_client(self, websocket):
        """Unregister a client."""
        self.clients.pop(websocket, None)

    async def message_handler(self, client, message):
        """Handle incoming messages."""
        

    async def client_handler(self, websocket, path):
        """Manage client connections and messages."""
        await self.register_client(websocket)
        try:
            async for message in websocket:
                await self.message_handler(self.clients[websocket], message)
        finally:
            await self.unregister_client(websocket)
