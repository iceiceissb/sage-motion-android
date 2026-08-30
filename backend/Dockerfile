FROM python:3.12-slim AS runtime

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

RUN addgroup --system sage && adduser --system --ingroup sage sage

COPY pyproject.toml ./
COPY sage_backend ./sage_backend
RUN pip install --upgrade pip && pip install .

USER sage
EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8000/healthz', timeout=2)"

CMD ["uvicorn", "sage_backend.main:app", "--host", "0.0.0.0", "--port", "8000", "--no-server-header"]
