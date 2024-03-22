import asyncio
import websockets

async def message_handler(websocket, message):
    """
    Handles incoming messages and defines the response.
    This function can be expanded to include more complex logic
    based on the message content.
    """
    print(f"Received message: {message}")

    # process the client response
    await process_response(message)

async def client_handler(websocket, path):
    """
    Handles client connections. Each client connection invokes this coroutine.
    It listens for messages from the client and uses the message_handler
    to process them.
    """
    try:
        async for message in websocket:
            await message_handler(websocket, message)
    except websockets.ConnectionClosed:
        print("Client disconnected")

async def main():
    """
    Starts the WebSocket server and listens for connections.
    """
    async with websockets.serve(client_handler, "localhost", 6789):
        print("Server started on ws://localhost:6789")
        await asyncio.Future()  # Run forever

if __name__ == "__main__":
    asyncio.run(main())
