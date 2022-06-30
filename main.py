import sqlite3

from bot import BotCommands
from bot import bot
from common.config import Config


def run():
    config = Config().get_config_model()

    connection = sqlite3.connect('data.db')

    client = bot.Bot(connection, command_prefix="!")
    client.add_cog(BotCommands(client))
    client.run(config.discord_token)

    connection.close()


if __name__ == '__main__':
    run()
