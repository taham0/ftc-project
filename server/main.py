import asyncio
import websockets
from controller import Controller

async def main():
    controller = Controller()
    async with websockets.serve(controller.client_handler, "localhost", 6789):
        print("Server started on ws://localhost:6789")
        await asyncio.Future()

if __name__ == "__main__":
    asyncio.run(main())
