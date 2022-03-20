import json

from .model import ConfigModel


class Config:
    file_name: str = "config.json"
    config: ConfigModel = None

    def __init__(self, file_name="config.json"):
        self.file_name = file_name

    def readFile(self):
        with open(self.file_name, 'rt') as file:
            return json.load(file)

    def getConfigModel(self):
        data = self.readFile()
        if self.config is None:
            self.config = ConfigModel(discord_token=data['discord']['token'],
                                      discord_owner=data['discord']['owner'],
                                      bank_cost=data['bank']['cost'],
                                      bank_id=data['bank']['id'],
                                      bank_password=data['bank']['password'],
                                      account_id=data['account']['id'],
                                      account_password=data['account']['password']
                                      )
        return self.config
