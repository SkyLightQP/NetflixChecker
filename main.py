import sqlite3

from bank.bank import Bank
from bot import BotCommands
from bot import bot
from common.config import Config


def run():
    config = Config().getConfigModel()

    bank = Bank(config.bank_id, config.bank_password, config.account_password, config.bank_cost,
                config.remotedriver_enable, config.remotedriver_host)
    bank.fetchData()

    connection = sqlite3.connect('data.db')

    client = bot.Bot(connection, bank, config.discord_channel, command_prefix="!")
    client.add_cog(BotCommands(client))
    client.run(config.discord_token)

    connection.close()


if __name__ == '__main__':
    run()
