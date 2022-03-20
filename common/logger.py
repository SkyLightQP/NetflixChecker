import logging

logger = logging.getLogger()
stream_handler = logging.StreamHandler()
file_handler = logging.FileHandler(filename="app.log")

formatter = logging.Formatter(fmt='%(asctime)s [%(levelname)s] %(message)s')

logger.setLevel(logging.DEBUG)
stream_handler.setLevel(logging.DEBUG)
file_handler.setLevel(logging.INFO)

stream_handler.setFormatter(formatter)
file_handler.setFormatter(formatter)

logger.addHandler(stream_handler)
logger.addHandler(file_handler)
