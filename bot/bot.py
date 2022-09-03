from datetime import datetime
from sqlite3 import Connection

import discord
from discord.ext import commands, tasks

from bank import Bank
from common import logger, Config


class Bot(commands.Bot):
    config = Config().get_config_model()

    def __init__(self, connection: Connection, **options):
        super().__init__(**options)
        self.connection = connection
        self.bank = Bank()

    async def on_ready(self):
        logger.info("Start discord bot successfully.")

        self.bank.login()
        self.bank.select_account()
        logger.info("Initialize bank account.")

        self.check_account_log_job.start()
        self.renewal_login_job.start()
        self.send_report_job.start()
        logger.info("Register schedule job.")

    async def on_error(self, event, *args, **kwargs):
        raise Exception(args[0])

    @tasks.loop(hours=4)
    async def check_account_log_job(self):
        now = datetime.now()
        if (now.hour >= 23 and now.minute >= 30) and (now.hour <= 0 and now.minute <= 30):
            return

        self.bank.refresh()

        channel = discord.utils.get(self.get_all_channels(), id=int(self.config.discord_channel))
        count = 0
        for i in self.bank.get_data():
            cur = self.connection.cursor()
            cur.execute(f"SELECT * FROM data WHERE date='{i.date}' and name='{i.who}';")

            if cur.fetchone() is None:
                embed = discord.Embed(title=f"입금 확인 ({i.who})",
                                      description=f"넷플릭스 {i.cost}원 입금 확인",
                                      color=0xF93A2F)
                embed.set_footer(text=f"{i.date}")
                await channel.send(embed=embed)
                insert_cur = self.connection.cursor()
                insert_cur.execute(f"INSERT INTO data VALUES ('{i.date}', '{i.who}');")
                self.connection.commit()
                count += 1

        if count > 0:
            logger.info(f"Found new netflix {count} logs.")

    @tasks.loop(minutes=9)
    async def renewal_login_job(self):
        now = datetime.now()
        if (now.hour >= 23 and now.minute >= 30) and (now.hour <= 0 and now.minute <= 30):
            return

        self.bank.renewal()

    @tasks.loop(minutes=1)
    async def send_report_job(self):
        now = datetime.now()
        if now.weekday() == 6 and now.hour == 8 and now.minute == 0:
            user = await self.fetch_user(int(self.config.discord_owner))
            cur = self.connection.cursor()
            cur.execute(f"SELECT * FROM data WHERE date LIKE '{now.year}-{now.month:02d}-%';")
            data = cur.fetchall()

            if len(data) > 0:
                message = []
                for item in data:
                    message.append(f"- {item[0]} {item[1]}")
                embed = discord.Embed(title=f"{now.month}월 넷플릭스 입금 리포트",
                                      description='\n'.join(message),
                                      color=0xF93A2F)
                await user.send(embed=embed)
            else:
                embed = discord.Embed(title=f"{now.month}월 넷플릭스 입금 리포트",
                                      description='입금 내역이 없습니다.',
                                      color=0xF93A2F)
                await user.send(embed=embed)
