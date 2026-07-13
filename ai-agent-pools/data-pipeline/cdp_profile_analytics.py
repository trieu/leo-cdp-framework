from datetime import datetime

import logging
import redis

from dagster import (
    Config,
    OpExecutionContext,
    Definitions,
    job,
    op,
)


logger = logging.getLogger("cdp_profile_analytics")


# ---------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------

class ProfileAnalyticsConfig(Config):
    segment_id: str
    service_params: dict[str, str] = {}


# ---------------------------------------------------------------------
# Redis
# ---------------------------------------------------------------------

def connect_to_redis():
    return redis.Redis(
        host="localhost",
        port=6480,
        decode_responses=True,
    )


# ---------------------------------------------------------------------
# Ops
# ---------------------------------------------------------------------

@op
def load_profiles(
    context: OpExecutionContext,
    config: ProfileAnalyticsConfig,
) -> dict:

    logger.info("load_profiles")
    logger.info(f"segment_id = {config.segment_id}")

    for key, value in config.service_params.items():
        logger.info(f"service_param {key} = {value}")

    # TODO:
    # Load profiles from database

    return {
        "segment_id": config.segment_id,
        "service_params": config.service_params,
    }


@op
def classify_profile(
    context: OpExecutionContext,
    profiles: dict,
) -> dict:

    logger.info("classify_profile_service")

    # TODO:
    # AI Classification

    profiles["classified"] = True
    return profiles


@op
def scoring_profile(
    context: OpExecutionContext,
    profiles: dict,
) -> dict:

    logger.info("scoring_profile_service")

    # TODO:
    # AI Scoring

    profiles["score"] = 95

    return profiles


@op
def save_profile(
    context: OpExecutionContext,
    profiles: dict,
):

    dt_string = datetime.now().strftime("%d/%m/%Y %H:%M:%S")

    r = connect_to_redis()

    r.set("hello_world_done_at", dt_string)

    logger.info("save_profile_service")
    logger.info(f"Saved at {dt_string}")

    logger.info(profiles)


# ---------------------------------------------------------------------
# Job
# ---------------------------------------------------------------------

@job
def cdp_profile_analytics():

    save_profile(
        scoring_profile(
            classify_profile(
                load_profiles()
            )
        )
    )


# ---------------------------------------------------------------------
# Dagster Definitions
# ---------------------------------------------------------------------

defs = Definitions(
    jobs=[cdp_profile_analytics],
)