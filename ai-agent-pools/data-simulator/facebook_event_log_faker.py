# TODO

import json
import random
import time
import hashlib
from datetime import datetime, timedelta
from faker import Faker

fake = Faker()

# Meta Standard Events
FB_EVENTS = ['PageView', 'ViewContent', 'AddToCart', 'InitiateCheckout', 'Purchase', 'Lead']

def hash_data(data_string):
    """SHA256 hashing as required by Meta Conversions API for user data."""
    if not data_string:
        return None
    return hashlib.sha256(data_string.lower().encode('utf-8')).hexdigest()

def generate_facebook_event(days_ago=0):
    """
    Generates a single synthetic Facebook Conversions API event.
    Includes advanced matching parameters and standard fbp/fbc cookies.
    """
    event_time = int((datetime.now() - timedelta(days=days_ago, hours=random.randint(0, 24))).timestamp())
    event_name = random.choice(FB_EVENTS)
    
    # Simulate first-party cookies used by Meta
    fbp = f"fb.1.{event_time}.{random.randint(100000000, 999999999)}"
    fbc = f"fb.1.{event_time}.{fake.uuid4()}" if random.random() > 0.7 else None # Only present if clicked from an ad

    # Generate user data for Advanced Matching
    raw_email = fake.email()
    raw_phone = fake.phone_number()

    payload = {
        "data": [
            {
                "event_name": event_name,
                "event_time": event_time,
                "action_source": random.choice(["website", "app", "physical_store"]),
                "event_source_url": fake.url(),
                "user_data": {
                    "em": [hash_data(raw_email)] if random.random() > 0.2 else [],
                    "ph": [hash_data(raw_phone)] if random.random() > 0.5 else [],
                    "client_ip_address": fake.ipv4(),
                    "client_user_agent": fake.user_agent(),
                    "fbc": fbc,
                    "fbp": fbp
                },
                "custom_data": {}
            }
        ]
    }

    # Add transaction specific data
    if event_name == 'Purchase':
        payload["data"][0]["custom_data"] = {
            "currency": "USD",
            "value": round(random.uniform(20.0, 300.0), 2),
            "content_ids": [str(random.randint(1000, 9999))],
            "content_type": "product"
        }

    return payload

if __name__ == "__main__":
    # Generate and print 5 sample Facebook CAPI events
    for _ in range(5):
        print(json.dumps(generate_facebook_event(), indent=2))