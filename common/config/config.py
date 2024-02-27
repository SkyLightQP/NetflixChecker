import json

from .model import ConfigModel


class Config:
    file_name: str = "config.json"
    config: ConfigModel = None

    def __init__(self, file_name="config.json"):
        self.file_name = file_name

    def read_file(self):
        with open(self.file_name, 'rt') as file:
            return json.load(file)

    def get_config_model(self) -> ConfigModel:
        data = self.read_file()
        if self.config is None:
            self.config = ConfigModel(discord_token=data['discord']['token'],
                                      discord_owner=data['discord']['owner'],
                                      discord_channel=data['discord']['channel'],
                                      bank_cost=data['bank']['cost'],
                                      bank_id=data['bank']['id'],
                                      bank_password=data['bank']['password'],
                                      account_password=data['account']['password'],
                                      selenium_use_remote=bool(data['selenium']['use_remote']),
                                      selenium_host=data['selenium']['host'],
                                      selenium_use_headless=bool(data['selenium']['use_headless']),
                                      pop_host=data['pop']['host'],
                                      pop_port=int(data['pop']['port']),
                                      pop_user=data['pop']['user'],
                                      pop_password=data['pop']['password'],
                                      dsn=data['dsn']
                                      )
        return self.config
