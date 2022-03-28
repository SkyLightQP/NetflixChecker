from datetime import datetime

import discord
from discord.ext import commands, tasks

from common import logger


class Bot(commands.Bot):
    def __init__(self, bank, channel_id, **options):
        super().__init__(**options)
        self.bank = bank
        self.channel_id = channel_id

    async def on_ready(self):
        logger.info('Start discord bot successfully.')

        self.sendBankAlert.start()
        logger.info('Register schedule job for bank alert.')

    @tasks.loop(hours=4)
    async def sendBankAlert(self):
        channel = discord.utils.get(self.get_all_channels(), id=int(self.channel_id))
        for i in self.bank.getData():
            embed = discord.Embed(title=f"입금 확인 ({i.who})",
                                  description=f"넷플릭스 {i.cost}원 입금 확인",
                                  color=0xF93A2F,
                                  timestamp=datetime.strptime(f"{i.date} {i.time}", "%Y-%m-%d %H:%M:%S"))
            await channel.send(embed=embed)
