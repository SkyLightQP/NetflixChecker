from dataclasses import dataclass


@dataclass(frozen=True)
class BankModel:
    time: str
    date: str
    cost: str
    who: str
