FROM python:3.12-slim
WORKDIR /app

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    MODEL_DIR=models

# Install runtime dependencies first (cached unless requirements change).
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy source.
COPY pipeline/ pipeline/
COPY app/ app/
COPY train.py ./

# Bake a trained model + metrics into the image so /predict works on startup.
RUN python train.py --rows 50000

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
