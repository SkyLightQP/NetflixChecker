from dataclasses import dataclass


@dataclass(frozen=True)
class BankData:
    time: str
    date: str
    cost: str
    who: str
