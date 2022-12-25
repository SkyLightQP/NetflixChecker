from sqlalchemy import Column, String

from common.database import Base


class Private(Base):
    __tablename__ = 'privates'

    key = Column(String(255), primary_key=True)
    encrypted_value = Column(String(255), nullable=False)
