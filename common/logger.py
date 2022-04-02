import logging

from selenium.webdriver.remote.remote_connection import LOGGER

LOGGER.setLevel(logging.WARNING)

logger = logging.getLogger()
stream_handler = logging.StreamHandler()
file_handler = logging.FileHandler(filename="app.log", encoding='utf-8')

formatter = logging.Formatter(fmt='%(asctime)s [%(levelname)s] %(message)s')

logger.setLevel(logging.DEBUG)
stream_handler.setLevel(logging.INFO)
file_handler.setLevel(logging.INFO)

stream_handler.setFormatter(formatter)
file_handler.setFormatter(formatter)

logger.addHandler(stream_handler)
logger.addHandler(file_handler)
