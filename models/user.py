from sqlalchemy import Column, String, Integer

from common.database import Base


class User(Base):
    __tablename__ = 'users'

    user_id = Column(Integer, primary_key=True)
    key = Column(String(255), nullable=False)
