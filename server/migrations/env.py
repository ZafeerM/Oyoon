"""Alembic environment: runs migrations against app.config's DATABASE_URL."""

from logging.config import fileConfig

from alembic import context
from sqlalchemy import create_engine

from app.config import settings
from app.models.tables import Base

config = context.config
if config.config_file_name is not None:
    fileConfig(config.config_file_name)

# A URL set on the Alembic config (e.g. by tests) wins over the environment.
url = config.get_main_option("sqlalchemy.url") or settings.database_url
target_metadata = Base.metadata


def run_migrations_offline() -> None:
    context.configure(url=url, target_metadata=target_metadata, literal_binds=True)
    with context.begin_transaction():
        context.run_migrations()


def run_migrations_online() -> None:
    engine = create_engine(url)
    with engine.connect() as connection:
        context.configure(connection=connection, target_metadata=target_metadata)
        with context.begin_transaction():
            context.run_migrations()
    engine.dispose()


if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
