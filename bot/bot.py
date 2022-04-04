from datetime import datetime
from sqlite3 import Connection

import discord
from discord.ext import commands, tasks

from bank import Bank
from common import logger, Config


class Bot(commands.Bot):
    def __init__(self, connection: Connection, **options):
        super().__init__(**options)
        self.connection = connection
        self.config = Config().getConfigModel()

    async def on_ready(self):
        logger.info("Start discord bot successfully.")

        self.checkAccountLog.start()
        logger.info("Register schedule job for bank alert.")

    @tasks.loop(hours=4)
    async def checkAccountLog(self):
        bank = Bank(self.config.bank_id, self.config.bank_password, self.config.account_password, self.config.bank_cost,
                    self.config.remotedriver_enable, self.config.remotedriver_host)
        bank.fetchData()

        channel = discord.utils.get(self.get_all_channels(), id=int(self.config.discord_channel))
        count = 0
        for i in bank.getData():
            cur = self.connection.cursor()
            cur.execute(f"SELECT * FROM data WHERE date='{i.date}' and name='{i.who}';")

            if cur.fetchone() is None:
                embed = discord.Embed(title=f"입금 확인 ({i.who})",
                                      description=f"넷플릭스 {i.cost}원 입금 확인",
                                      color=0xF93A2F,
                                      timestamp=datetime.strptime(f"{i.date} {i.time}", "%Y-%m-%d %H:%M:%S"))
                await channel.send(embed=embed)
                insert_cur = self.connection.cursor()
                insert_cur.execute(f"INSERT INTO data VALUES ('{i.date}', '{i.who}');")
                self.connection.commit()
                count += 1

        logger.info(f"Found new account {count} logs. ")
