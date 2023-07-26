FROM python:3.10

COPY / /workspace
WORKDIR /workspace

RUN pip install -r requirements.txt

VOLUME ["/workspace/app.log", "/workspace/config.json", "/workspace/data.db", "/workspace/chromedriver"]

CMD ["python", "main.py"]
