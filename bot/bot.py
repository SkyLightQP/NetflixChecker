import discord

from common import logger


class Bot(discord.Client):
    async def on_ready(self):
        logger.info('Start discord bot successfully.')
