# TODO 

import json
import random
import uuid
from datetime import datetime, timedelta
from faker import Faker

fake = Faker()

# Common AppsFlyer event types
AF_EVENTS = ['af_install', 'af_app_opened', 'af_add_to_cart', 'af_purchase', 'af_complete_registration']
MEDIA_SOURCES = ['organic', 'googleadwords_int', 'Facebook Ads', 'Apple Search Ads']

def generate_appsflyer_event(days_ago=0):
    """
    Generates a single synthetic AppsFlyer event payload.
    Includes mobile-specific identifiers and attribution data.
    """
    event_time = datetime.now() - timedelta(days=days_ago, minutes=random.randint(1, 1440))
    event_name = random.choice(AF_EVENTS)
    
    # Simulate a realistic purchase value if the event is a purchase
    event_value = round(random.uniform(10.0, 500.0), 2) if event_name == 'af_purchase' else None
    event_currency = "USD" if event_name == 'af_purchase' else None

    # Determine platform and matching device ID
    platform = random.choice(['ios', 'android'])
    idfa = fake.uuid4().upper() if platform == 'ios' else None
    gaid = fake.uuid4() if platform == 'android' else None

    payload = {
        "app_id": random.choice(["id123456789", "com.example.app"]),
        "platform": platform,
        "event_time": event_time.strftime("%Y-%m-%d %H:%M:%S"),
        "event_name": event_name,
        "appsflyer_id": str(uuid.uuid4()), # Unique AF device identifier
        "customer_user_id": fake.uuid4() if random.random() > 0.3 else None, # 70% logged-in users
        "ip": fake.ipv4(),
        "wifi": random.choice([True, False]),
        "language": random.choice(["en", "vi", "es", "ja"]),
        "media_source": random.choice(MEDIA_SOURCES),
        "campaign": fake.catch_phrase() if random.random() > 0.5 else "None",
        "idfa": idfa,
        "android_id": gaid,
        "event_value": event_value,
        "event_currency": event_currency,
        "country_code": fake.country_code()
    }
    
    # Remove None values for a cleaner JSON payload
    return {k: v for k, v in payload.items() if v is not None}

if __name__ == "__main__":
    # Generate and print 5 sample AppsFlyer events
    for _ in range(5):
        print(json.dumps(generate_appsflyer_event(), indent=2))