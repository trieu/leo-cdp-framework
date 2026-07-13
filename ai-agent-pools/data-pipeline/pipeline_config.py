import os
from dotenv import load_dotenv

load_dotenv(".env")

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))
REDIS_CHANNEL = os.getenv("REDIS_CHANNEL")

AUTH_USER = os.getenv("AUTH_USER")
AUTH_PASSWORD = os.getenv("AUTH_PASSWORD")

DEV_MODE = os.getenv("DEV_MODE", "false").lower() == "true"
USE_API = os.getenv("USE_API", "true").lower() == "true"