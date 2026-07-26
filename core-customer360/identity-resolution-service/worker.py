"""Continuous Customer Identity Resolution (CIR) worker for containerized
(production) deployments.

Repeatedly drains the ``cdp_raw_profiles_stage`` staging table via
``identity_resolution.daily_job.run_daily_identity_resolution()`` (the same
entry point used for cron/Airflow-style one-shot runs), sleeping
``CIR_POLL_INTERVAL_SECONDS`` between cycles. A single cycle failure (e.g. a
transient DB hiccup) is logged and retried on the next interval rather than
crashing the container, so `restart: unless-stopped` isn't fighting a
crash-loop for routine transient errors.
"""

import logging
import os
import time

from identity_resolution.daily_job import run_daily_identity_resolution

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

POLL_INTERVAL_SECONDS = int(os.environ.get("CIR_POLL_INTERVAL_SECONDS", "30"))


def main() -> None:
    logger.info("CIR worker starting; polling every %ss.", POLL_INTERVAL_SECONDS)
    while True:
        try:
            processed = run_daily_identity_resolution()
            if processed:
                logger.info("Processed %d raw profile(s) this cycle.", processed)
        except Exception:
            logger.exception("CIR resolution cycle failed; will retry next interval.")
        time.sleep(POLL_INTERVAL_SECONDS)


if __name__ == "__main__":
    main()
