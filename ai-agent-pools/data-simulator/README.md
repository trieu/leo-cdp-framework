# Data Simulation for Event Log Importing 

## Some code for demo 

1. AppsFlyer Synthetic Data Generator (appsflyer_faker.py)
AppsFlyer data focuses heavily on mobile attribution, campaign parameters, and device identifiers (IDFA/GAID).

2. Google Analytics Synthetic Data Generator (google_analytics_faker.py)
GA4 focuses on an event-driven data model, utilizing client_id for browsers, session_id for visits, and specific UTM parameters for web traffic.

3. Facebook Event Log Synthetic Data Generator (facebook_event_log_faker.py)
Meta's Conversions API (CAPI) and Pixel logs rely heavily on standard e-commerce events and advanced matching identifiers like hashed emails, fbp (browser ID), and fbc (click ID).