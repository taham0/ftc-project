import asyncio
import websockets
import json

async def hello():
    uri = "ws://localhost:6789"
    
    async with websockets.connect(uri) as websocket:
        # wait for server request after connecting

        # testing

        while True:
            req = await websocket.recv()
            req = json.loads(req)

            print(req)

            if (req["type"] == "FR"):
                data = {
                    "type": "FR",
                    "round": req["round"],
                    "data": req["blob"]
                }

                await websocket.send(json.dumps(data))

asyncio.get_event_loop().run_until_complete(hello())
