import asyncio
import websockets

async def hello():
    uri = "ws://localhost:6789"
    while True:
        async with websockets.connect(uri) as websocket:
            # wait for server request after connecting

            # testing
            message = input("Enter a message: ")
            await websocket.send(message)
            print(f"> {message}")

asyncio.get_event_loop().run_until_complete(hello())
