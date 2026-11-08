FROM python:3.14-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /srv

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY alembic.ini pytest.ini start.sh ./
COPY app ./app
COPY migrations ./migrations
COPY tests ./tests

EXPOSE 8000

# Applies migrations, then starts the API.
CMD ["sh", "start.sh"]
