import asyncio
import websockets

async def hello():
    uri = "ws://localhost:6789"
    async with websockets.connect(uri) as websocket:
        message = "Connection request."
        await websocket.send(message)
        print(f"> {message}")

asyncio.get_event_loop().run_until_complete(hello())
