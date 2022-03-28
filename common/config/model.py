from dataclasses import dataclass


@dataclass(frozen=True)
class ConfigModel:
    discord_token: str
    discord_owner: str
    discord_channel: str

    bank_cost: str
    bank_id: str
    bank_password: str

    account_password: str
