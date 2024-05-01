import asyncio
import websockets
import logging
from controller import Controller
from image_loader import ImageLoader

# Set up logging
logging.basicConfig(level=logging.DEBUG)
logging.getLogger("websockets").setLevel(logging.INFO)
async def main():
    # Initialize the Controller with the desired number of rounds and the required number of clients to begin a round
    # p = ImageLoader("../dataset/train/*.jpg")
    
    # p.data_loader()
    
    # print(p.get_image())
    
    
    rounds = 100
    required_clients = 3
    controller = Controller(rounds, required_clients, "../dataset/train/*.jpg")

    async with websockets.serve(controller.client_handler, "localhost", 6789):
        print("Server started on ws://localhost:6789")
        await asyncio.Future()

if __name__ == "__main__":
    asyncio.run(main())
