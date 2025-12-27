FROM python:3.12-alpine

WORKDIR /app
COPY mtls_init.py .

RUN chmod +x mtls_init.py

ENTRYPOINT ["python", "mtls_init.py"]
