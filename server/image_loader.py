import io
import glob
from PIL import Image
import base64
import logging

log = logging.getLogger("ImageLoader")
log.setLevel(logging.INFO)

class ImageLoader:
    def __init__(self, dir):
        self.dir = dir
        self.file_names = glob.glob(self.dir)
        self.byte_images = []
        
    
    def data_loader(self):
        for fname in self.file_names:
            try:
                with Image.open(fname) as img:
                    img_byte_arr = io.BytesIO()
                    img.save(img_byte_arr, format='PNG')
                    img_bytes = img_byte_arr.getvalue()
                    encoded_img = base64.b64encode(img_bytes).decode('utf-8')
                    self.byte_images.append(encoded_img)
            except Exception as e:
                logging.info(f"Error reading image {fname}: {e}")
                
        logging.info(f"Data loaded successfully.")

    def get_image(self, index=0):
        """
        gets the byte arr for the image at index i
        """
        if (index > len(self.byte_images) - 1):
            logging.warn(f"Index out of range ... defaulting to first image.")
        
        logging.info(f"Fetched image at index {index}")
        return self.byte_images[index]
            