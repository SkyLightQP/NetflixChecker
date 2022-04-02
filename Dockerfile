FROM python:3.10

COPY / /workspace
WORKDIR /workspace

RUN pip install -r requirements.txt

VOLUME ["app.log", "config.json", "data.db", "chromedriver.exe"]

CMD ["python", "main.py"]
