FROM python:3.11-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# No HTTP surface on this service (background worker) -- DB connectivity is
# the most meaningful liveness signal. See healthcheck.py.
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=5 \
    CMD python healthcheck.py || exit 1

# Default command: continuous polling worker (production / `docker compose up`).
# The `dev` profile's `cir-demo-seed` service overrides `command:` in
# docker-compose.yml to run the one-shot seeding scripts instead, reusing
# this same image.
CMD ["python", "worker.py"]
