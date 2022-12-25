import sqlite3

import sentry_sdk

from bot import BotCommands
from bot import bot
from common.config import Config
from common.database import Base, engine


def run():
    config = Config().get_config_model()

    sentry_sdk.init(
        dsn=config.dsn,
        traces_sample_rate=1.0
    )

    from models import private, log, user
    Base.metadata.create_all(bind=engine)

    connection = sqlite3.connect('data.db')

    client = bot.Bot(connection, command_prefix="!")
    client.add_cog(BotCommands(client))
    client.run(config.discord_token)

    connection.close()


if __name__ == '__main__':
    run()
