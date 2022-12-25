from sqlalchemy import Column, String, Integer

from common.database import Base


class Log(Base):
    __tablename__ = 'logs'

    id = Column(Integer, primary_key=True)
    who = Column(String(10), nullable=False)
    date = Column(String(10), nullable=False)
