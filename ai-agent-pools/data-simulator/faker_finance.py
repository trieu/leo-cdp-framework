import os
import csv
import json
import random
import unicodedata
from datetime import timedelta
from faker import Faker
from pydantic import BaseModel, Field
from dotenv import load_dotenv

load_dotenv(".env")

try:
    from google import genai
    from google.genai import types
    GENAI_AVAILABLE = True
except ImportError:
    GENAI_AVAILABLE = False

# ---------------------------------------------------------
# 1. Pydantic Schemas for Gemini GenAI
# ---------------------------------------------------------
class AdCampaign(BaseModel):
    media_source: str = Field(description="Must be one of: googleadwords_int, Facebook Ads, tiktokglobal_int, Apple Search Ads")
    campaign: str = Field(description="Bank123 campaign name (e.g. vn_bank123_credit_card_q3)")
    adset_name: str = Field(description="Realistic adset group name")
    ad_name: str = Field(description="Realistic ad name (e.g. video_mo_the_nhanh)")
    ad_type: str = Field(description="e.g., video, display, search")
    keywords: str = Field(description="Comma separated keywords related to banking (only meaningful for search campaigns)")

class CampaignList(BaseModel):
    campaigns: list[AdCampaign]

class UserPersona(BaseModel):
    full_name: str = Field(description="Realistic Vietnamese full name")
    age: int = Field(description="Age between 18 and 60")
    gender: str = Field(description="Male or Female")
    city: str = Field(description="Realistic major Vietnamese city (e.g., Hà Nội, Hồ Chí Minh, Đà Nẵng, Cần Thơ, Hải Phòng)")
    occupation: str = Field(description="Realistic job title in Vietnamese")
    device_os: str = Field(description="ios or android")
    preferred_features: list[str] = Field(description="2 to 4 preferred features from this list: Tài khoản, Chuyển khoản (CK), Thanh toán hóa đơn, Thẻ, Vay, Đầu tư, Quà tặng, Bảo hiểm, Giải trí - Mua sắm, Du lịch - Di chuyển")

class PersonaList(BaseModel):
    personas: list[UserPersona]

# ---------------------------------------------------------
# 2. Media Source Config
# ---------------------------------------------------------
# Real AppsFlyer partner IDs / display names + how each network's data
# actually shows up on a Pull API raw-data report. This drives which
# columns get populated for a given row -- e.g. fb_* columns are only
# ever non-empty when media_source is a Facebook/Meta placement, ASA
# never sends a site_id, only search-type sources carry keywords, etc.
MEDIA_SOURCE_CONFIG = {
    "googleadwords_int": {
        "is_fb": False, "is_search": True, "ios_only": False,
        "cost_models": ["cpi", "cpc"], "cost_range": (15000, 60000),
    },
    "Facebook Ads": {
        "is_fb": True, "is_search": False, "ios_only": False,
        "cost_models": ["cpi", "cpm"], "cost_range": (12000, 55000),
    },
    "tiktokglobal_int": {
        "is_fb": False, "is_search": False, "ios_only": False,
        "cost_models": ["cpi", "cpm"], "cost_range": (10000, 45000),
    },
    "Apple Search Ads": {
        "is_fb": False, "is_search": True, "ios_only": True,
        "cost_models": ["cpi"], "cost_range": (20000, 90000),
    },
}
# Share of installs that are organic vs. attributed to a paid source.
# ~25% organic is a reasonable ballpark for a mid-size finance app.
ORGANIC_INSTALL_RATE = 0.25
# Share of paid, non-organic users who come back later via a retargeting
# (re-engagement) campaign touch.
RETARGETING_RATE = 0.18

# ---------------------------------------------------------
# 3. AI Manager (Gemini AI Integration for Campaigns & Personas)
# ---------------------------------------------------------
class AIManager:
    """
    Generates campaign/persona data via Gemini when an API key + network
    access are available, and transparently falls back to local
    Faker-driven generation otherwise so the simulator remains runnable
    offline (e.g. in CI, or sandboxed environments without egress to
    generativelanguage.googleapis.com).
    """

    GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-3.5-flash")

    FALLBACK_CAMPAIGN_TEMPLATES = [
        {"media_source": "googleadwords_int", "campaign": "vn_bank123_savings_search_brand",
         "adset_name": "search_brand_exact", "ad_name": "rsa_gui_tiet_kiem_lai_suat_cao",
         "ad_type": "search", "keywords": "gui tiet kiem online, lai suat ngan hang, mo tai khoan tiet kiem"},
        {"media_source": "Facebook Ads", "campaign": "vn_bank123_credit_card_q3_prospecting",
         "adset_name": "prospecting_lookalike_1pct", "ad_name": "carousel_mo_the_tin_dung_mien_phi",
         "ad_type": "display", "keywords": ""},
        {"media_source": "tiktokglobal_int", "campaign": "vn_bank123_gen_z_savings_awareness",
         "adset_name": "interest_finance_18_24", "ad_name": "video_review_gui_tiet_kiem_gen_z",
         "ad_type": "video", "keywords": ""},
        {"media_source": "Apple Search Ads", "campaign": "vn_bank123_asa_discovery_finance",
         "adset_name": "discovery_finance_category", "ad_name": "asa_discovery_default",
         "ad_type": "search", "keywords": "ngan hang so, vay tien nhanh, mo the tin dung"},
    ]

    FALLBACK_OCCUPATIONS = ["Nhân viên văn phòng", "Kỹ sư phần mềm", "Giáo viên", "Kinh doanh tự do",
                             "Sinh viên", "Kế toán", "Nhân viên bán hàng", "Bác sĩ", "Freelancer"]
    FALLBACK_CITIES = ["Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Cần Thơ", "Hải Phòng"]
    FALLBACK_FEATURE_POOL = ["Tài khoản", "Chuyển khoản (CK)", "Thanh toán hóa đơn", "Thẻ", "Vay",
                              "Đầu tư", "Quà tặng", "Bảo hiểm", "Giải trí - Mua sắm", "Du lịch - Di chuyển"]

    def __init__(self, api_key: str):
        self.api_key = api_key
        self.fake = Faker('vi_VN')
        self.client = None
        if GENAI_AVAILABLE and api_key:
            try:
                self.client = genai.Client(api_key=api_key)
            except Exception as e:
                print(f"Could not initialize Gemini client ({e}). Falling back to offline generation.")
        self.campaigns = []
        self.personas = []

    def generate_campaigns(self, num_campaigns: int = 4):
        if self.client:
            try:
                print(f"Requesting {num_campaigns} realistic Bank123 campaign structures from Gemini...")
                prompt = (
                    f"Generate {num_campaigns} distinct, highly realistic mobile app marketing campaign "
                    f"hierarchies for a Vietnamese Mobile Internet Banking App named 'Bank123'. "
                    f"media_source must only be one of: googleadwords_int, Facebook Ads, tiktokglobal_int, "
                    f"Apple Search Ads. Only include keywords for search-type campaigns (googleadwords_int "
                    f"or Apple Search Ads); leave keywords empty for Facebook Ads and tiktokglobal_int."
                )
                response = self.client.models.generate_content(
                    model=self.GEMINI_MODEL,
                    contents=prompt,
                    config=types.GenerateContentConfig(
                        response_mime_type="application/json",
                        response_schema=CampaignList,
                        temperature=0.7,
                    ),
                )
                self.campaigns = response.parsed.campaigns
                print("Marketing campaigns generated successfully via Gemini.")
                return
            except Exception as e:
                print(f"Gemini campaign generation failed ({e}). Falling back to offline templates.")

        self.campaigns = [AdCampaign(**c) for c in self.FALLBACK_CAMPAIGN_TEMPLATES[:num_campaigns]]
        print(f"Using {len(self.campaigns)} offline fallback campaigns.")

    def generate_personas(self, num_personas: int = 10):
        if self.client:
            try:
                print(f"Requesting {num_personas} realistic Vietnamese user personas from Gemini...")
                prompt = f"Generate {num_personas} highly realistic Vietnamese user personas for a mobile banking app."
                response = self.client.models.generate_content(
                    model=self.GEMINI_MODEL,
                    contents=prompt,
                    config=types.GenerateContentConfig(
                        response_mime_type="application/json",
                        response_schema=PersonaList,
                        temperature=0.8,
                    ),
                )
                self.personas = response.parsed.personas
                print("User personas generated successfully via Gemini.")
                return
            except Exception as e:
                print(f"Gemini persona generation failed ({e}). Falling back to offline generation.")

        self.personas = [self._build_fallback_persona() for _ in range(num_personas)]
        print(f"Using {len(self.personas)} offline fallback personas.")

    def _build_fallback_persona(self) -> "UserPersona":
        is_male = random.random() < 0.5
        name = self.fake.name_male() if is_male else self.fake.name_female()
        return UserPersona(
            full_name=name,
            age=random.randint(18, 60),
            gender="Male" if is_male else "Female",
            city=random.choice(self.FALLBACK_CITIES),
            occupation=random.choice(self.FALLBACK_OCCUPATIONS),
            device_os=random.choice(["ios", "android"]),
            preferred_features=random.sample(self.FALLBACK_FEATURE_POOL, k=random.randint(2, 4)),
        )

# ---------------------------------------------------------
# 4. Domain Logic: Bank123 Features
# ---------------------------------------------------------
class Bank123App:
    PRE_LOGIN_FEATURES = ["Mở tài khoản", "Quên mật khẩu", "Mở thẻ tín dụng"]
    POST_LOGIN_FEATURES = {
        "Login": ["Login"],
        "Tài khoản": ["Mở tài khoản", "Tài khoản số đẹp"],
        "Chuyển khoản (CK)": ["Chuyển khoản (CK) Home", "CK ngoài bank", "CK trong bank", "CK qua thẻ"],
        "Thanh toán hóa đơn": ["Nạp tiền ĐT", "Điện", "Nước", "Internet", "Vé máy bay", "Học phí"],
        "Thẻ": ["1CC", "Card home", "Trả góp", "Mở thẻ sau login"],
        "Vay": ["Vay home", "Đăng ký nhu cầu vay"],
        "Đầu tư": ["Đầu tư Homescreen", "Chứng khoán", "Mua vàng"],
        "Quà tặng": ["Quà tặng Homescreen", "Tặng tiền"],
        "Bảo hiểm": ["Bảo hiểm Homescreen", "Mua bảo hiểm xe"],
        "Giải trí - Mua sắm": ["Giải trí - Mua sắm Home screen", "Đặt vé xem phim"],
        "Du lịch - Di chuyển": ["Du lịch - Di chuyển Homescreen", "Taxi", "Đặt vé máy bay"]
    }

# ---------------------------------------------------------
# 5. Device Management
# ---------------------------------------------------------
class DeviceManager:
    ANDROID_APP_VERSIONS = ["3.4.0", "3.4.2", "3.5.0", "3.5.1", "3.6.0"]
    SDK_VERSIONS = ["v6.13.2", "v6.14.0", "v6.14.1", "v6.15.0"]
    ZERO_IDFA = "00000000-0000-0000-0000-000000000000"

    def __init__(self):
        self.fake = Faker('vi_VN')

    def create_device_profile(self, persona: UserPersona, force_ios: bool = False):
        # Apple Search Ads only exists on iOS -- force override if needed.
        is_ios = True if force_ios else (persona.device_os.lower() == "ios")
        platform = "ios" if is_ios else "android"

        if is_ios:
            os_major = random.randint(15, 17)
            os_minor = random.randint(0, 6)
            os_v = f"{os_major}.{os_minor}"
            device_type = random.choice(["iPhone 12", "iPhone 13", "iPhone 14 Pro", "iPhone 15", "iPhone 15 Pro"])
            ua = (f"Mozilla/5.0 (iPhone; CPU iPhone OS {os_major}_{os_minor} like Mac OS X) "
                  f"AppleWebKit/605.1.15 (KHTML, like Gecko) Version/{os_major}.{os_minor} Mobile/15E148 Safari/604.1")

            # ATT (App Tracking Transparency) status. iOS 14.5+ requires an explicit
            # prompt; industry opt-in rates hover around 20-30%. IDFA is only a real
            # value when the user has authorized tracking (status == 3) -- otherwise
            # AppsFlyer receives a zeroed-out IDFA, same as Apple returns it.
            att_status = random.choices(["0", "1", "2", "3"], weights=[15, 5, 55, 25])[0]
            idfa = self.fake.uuid4() if att_status == "3" else self.ZERO_IDFA
            idfv = self.fake.uuid4()  # IDFV is unaffected by ATT
            advertising_id = ""
            imei, android_id, gp_broadcast_referrer = "", "", ""
        else:
            os_v = str(random.randint(10, 14))
            device_type = random.choice(["Samsung Galaxy S23", "Samsung Galaxy A54", "Oppo Reno 10",
                                          "Xiaomi 13", "Vivo V27"])
            chrome_v = f"{random.randint(110, 124)}.0.0.0"
            ua = (f"Mozilla/5.0 (Linux; Android {os_v}; {device_type}) AppleWebKit/537.36 "
                  f"(KHTML, like Gecko) Chrome/{chrome_v} Mobile Safari/537.36")

            att_status = ""
            idfa, idfv = "", ""
            # A small share of Android users have "opt out of ads personalization"
            # enabled, which zeroes the GAID.
            advertising_id = self.ZERO_IDFA if random.random() < 0.08 else self.fake.uuid4()
            imei = self.fake.numerify('###############')
            android_id = self.fake.uuid4()
            gp_broadcast_referrer = ""  # filled in by the simulator once campaign is known

        clean_name = ''.join(c for c in unicodedata.normalize('NFD', persona.full_name)
                              if unicodedata.category(c) != 'Mn').replace(" ", "").lower()
        cuid = f"{clean_name}_{self.fake.random_number(digits=4)}"

        network_type = random.choices(["wifi", "4g", "5g"], weights=[55, 35, 10])[0]
        wifi = "true" if network_type == "wifi" else "false"

        return {
            "platform": platform,
            "device_type": device_type,
            "os_version": os_v,
            "app_version": random.choice(self.ANDROID_APP_VERSIONS),
            "sdk_version": random.choice(self.SDK_VERSIONS),
            "advertising_id": advertising_id,
            "idfa": idfa,
            "idfv": idfv,
            "att": att_status,
            "imei": imei,
            "android_id": android_id,
            "ip": self.fake.ipv4(),
            "ua": ua,
            "wifi_mac_address": self.fake.mac_address(),
            "operator": random.choice(["Viettel", "Vinaphone", "Mobifone"]),
            "network_type": network_type,
            "wifi": wifi,
            "city": persona.city,
            "customer_user_id": cuid,
            "gp_broadcast_referrer": gp_broadcast_referrer,
        }

# ---------------------------------------------------------
# 6. User Journey Simulation
# ---------------------------------------------------------
class UserJourneySimulator:
    def __init__(self, device_manager: DeviceManager, ai_manager: AIManager):
        self.device = device_manager
        self.ai = ai_manager

    def simulate_user_events(self) -> list:
        events = []
        is_organic = random.random() < ORGANIC_INSTALL_RATE

        if is_organic:
            campaign = None
            eligible_personas = self.ai.personas
        else:
            campaign = random.choice(self.ai.campaigns)
            cfg = MEDIA_SOURCE_CONFIG[campaign.media_source]
            eligible_personas = (
                [p for p in self.ai.personas if p.device_os.lower() == "ios"]
                if cfg["ios_only"] else self.ai.personas
            )
            eligible_personas = eligible_personas or self.ai.personas

        persona = random.choice(eligible_personas)
        is_asa = campaign is not None and "apple" in campaign.media_source.lower()
        device_profile = self.device.create_device_profile(persona, force_ios=is_asa)

        install_time = self.device.fake.date_time_between(start_date='-30d', end_date='now')
        if is_organic:
            click_time = None
        else:
            click_time = install_time - timedelta(minutes=random.randint(1, 1440))
        current_time = install_time

        base_row = self._create_base_row(device_profile, campaign, click_time, install_time, is_organic)

        persona_demographics = {
            "name": persona.full_name,
            "age": persona.age,
            "occupation": persona.occupation
        }
        events.append(self._build_event_row(base_row, current_time, "install", persona_demographics))

        for _ in range(random.randint(0, 2)):
            current_time += timedelta(minutes=random.randint(1, 5))
            events.append(self._build_event_row(base_row, current_time, random.choice(Bank123App.PRE_LOGIN_FEATURES),
                                                 {"state": "pre_login"}))

        if random.random() < 0.85:
            current_time += timedelta(minutes=random.randint(1, 10))
            events.append(self._build_event_row(base_row, current_time, "Login", {"sub_feature": "Login"}))

            for _ in range(random.randint(2, 6)):
                current_time += timedelta(minutes=random.randint(2, 45))
                category = random.choice(persona.preferred_features)
                if category not in Bank123App.POST_LOGIN_FEATURES:
                    category = "Chuyển khoản (CK)"
                sub_feature = random.choice(Bank123App.POST_LOGIN_FEATURES[category])
                events.append(self._build_event_row(base_row, current_time, category,
                                                     {"sub_feature": sub_feature, "status": "success"}))

        # Simulate a retargeting re-engagement: user comes back later via a
        # (possibly different) paid touch. Only meaningful for non-organic
        # installs since you can't "retarget" someone into an organic install.
        if not is_organic and random.random() < RETARGETING_RATE:
            events.extend(self._simulate_reengagement(base_row, install_time, persona))

        return events

    def _simulate_reengagement(self, original_base_row: dict, install_time, persona) -> list:
        events = []
        reengage_campaign = random.choice([c for c in self.ai.campaigns
                                            if MEDIA_SOURCE_CONFIG[c.media_source]["is_fb"]
                                            or True])  # any campaign can run retargeting
        reattr_touch_time = install_time + timedelta(days=random.randint(3, 21),
                                                       hours=random.randint(0, 23))
        reattr_touch_type = random.choice(["click", "impression"])

        reengage_row = original_base_row.copy()
        reengage_row.update({
            "attributed_touch_type": reattr_touch_type,
            "click_time": reattr_touch_time.strftime("%Y-%m-%d %H:%M:%S") if reattr_touch_type == "click" else "",
            "media_source": reengage_campaign.media_source,
            "campaign": f"{reengage_campaign.campaign}_retargeting",
            "is_retargeting": "true",
            "reattr_touch_time": reattr_touch_time.strftime("%Y-%m-%d %H:%M:%S"),
            "reattr_touch_type": reattr_touch_type,
            "conversion_type": "re-engagement",
        })

        current_time = reattr_touch_time + timedelta(minutes=random.randint(1, 15))
        events.append(self._build_event_row(reengage_row, current_time, "af_app_reopen", {"trigger": "retargeting"}))

        if random.random() < 0.6:
            current_time += timedelta(minutes=random.randint(1, 10))
            events.append(self._build_event_row(reengage_row, current_time, "Login", {"sub_feature": "Login"}))
            for _ in range(random.randint(1, 3)):
                current_time += timedelta(minutes=random.randint(2, 30))
                category = random.choice(persona.preferred_features)
                if category not in Bank123App.POST_LOGIN_FEATURES:
                    category = "Chuyển khoản (CK)"
                sub_feature = random.choice(Bank123App.POST_LOGIN_FEATURES[category])
                events.append(self._build_event_row(reengage_row, current_time, category,
                                                     {"sub_feature": sub_feature, "status": "success"}))
        return events

    def _create_base_row(self, device, campaign, click_time, install_time, is_organic: bool) -> dict:
        fake = self.device.fake
        is_ios = device["platform"] == "ios"

        if is_organic:
            media_source = "organic"
            cfg = None
        else:
            media_source = campaign.media_source
            cfg = MEDIA_SOURCE_CONFIG[media_source]

        is_fb = bool(cfg and cfg["is_fb"])
        is_search = bool(cfg and cfg["is_search"])

        gp_broadcast_referrer = device["gp_broadcast_referrer"]
        if not is_ios:
            if is_organic:
                gp_broadcast_referrer = "utm_source=google-play&utm_medium=organic"
            else:
                gp_broadcast_referrer = (
                    f"utm_source={media_source}&utm_medium=cpi&utm_campaign={campaign.campaign}"
                    f"&af_tranid={fake.uuid4()}"
                )

        row = {
            "attributed_touch_type": "" if is_organic else random.choice(["click", "impression"]),
            "click_time": "" if is_organic else click_time.strftime("%Y-%m-%d %H:%M:%S"),
            "install_time": install_time.strftime("%Y-%m-%d %H:%M:%S"),
            "media_source": media_source,
            "campaign": "" if is_organic else campaign.campaign,
            "fb_campaign_id": fake.random_number(digits=9, fix_len=True) if is_fb else "",
            "agency": "", "channel": "",
            "keywords": campaign.keywords if (campaign and is_search) else "",
            "fb_adset_id": fake.random_number(digits=9, fix_len=True) if is_fb else "",
            "fb_adset_name": campaign.adset_name if is_fb else "",
            "fb_ad_id": fake.random_number(digits=9, fix_len=True) if is_fb else "",
            "fb_ad_name": campaign.ad_name if is_fb else "",
            "ad_id": "" if is_organic else fake.random_number(digits=9, fix_len=True),
            "ad_name": "" if is_organic else campaign.ad_name,
            "ad_type": "" if is_organic else campaign.ad_type,
            "site_id": "" if (is_organic or is_search) else fake.hexify(text="^^^^-^^^^"),
            "sub_site_id": "", "sub_param_1": "", "sub_param_2": "", "sub_param_3": "", "sub_param_4": "", "sub_param_5": "",

            "cost_model": "" if is_organic else random.choice(cfg["cost_models"]),
            "cost_value": "" if is_organic else round(random.uniform(*cfg["cost_range"]), 2),
            "cost_currency": "" if is_organic else "VND",

            "http_referrer": "",
            "advertising_id": device["advertising_id"],
            "idfa": device["idfa"],
            "idfv": device["idfv"],
            "platform": device["platform"],
            "device_type": device["device_type"],
            "os_version": device["os_version"],
            "app_version": device["app_version"],
            "sdk_version": device["sdk_version"],
            "app_id": "id123456789" if is_ios else "vn.bank123.app",
            "app_name": "Bank123",
            "bundle_id": "vn.bank123.app",
            "gp_broadcast_referrer": gp_broadcast_referrer,
            "blocked_reason": "", "blocked_reason_value": "",
            "operator": device["operator"],
            "carrier": device["operator"],
            "network_type": device["network_type"],
            "wifi": device["wifi"],
            "language": "vi",
            "country_code": "VN",
            "state": "", "city": device["city"],
            "postal_code": "", "ip": device["ip"],
            "ua": device["ua"], "wifi_mac_address": device["wifi_mac_address"],
            "imei": device["imei"], "android_id": device["android_id"],
            "customer_user_id": device["customer_user_id"],
            "is_retargeting": "false", "reattr_touch_time": "", "reattr_touch_type": "",
            "is_primary_attribution": "true",
            "att": device["att"],
            "conversion_type": "organic" if is_organic else "regular"
        }
        return row

    def _build_event_row(self, base_row: dict, event_time, event_name: str, event_value: dict) -> dict:
        row = base_row.copy()
        row["event_time"] = event_time.strftime("%Y-%m-%d %H:%M:%S")
        row["event_name"] = event_name
        row["event_value"] = json.dumps(event_value, ensure_ascii=False)
        return row

# ---------------------------------------------------------
# 7. Simulator Application (Main Orchestrator)
# ---------------------------------------------------------
class AppsFlyerSimulatorApp:
    def __init__(self, api_key: str, output_file: str):
        self.ai_manager = AIManager(api_key)
        self.device_manager = DeviceManager()
        self.simulator = UserJourneySimulator(self.device_manager, self.ai_manager)
        self.output_file = output_file

        self.headers = [
            "attributed_touch_type", "click_time", "install_time", "media_source", "campaign",
            "fb_campaign_id", "agency", "channel", "keywords", "fb_adset_id", "fb_adset_name",
            "fb_ad_id", "fb_ad_name", "ad_id", "ad_name", "ad_type", "site_id", "sub_site_id",
            "sub_param_1", "sub_param_2", "sub_param_3", "sub_param_4", "sub_param_5",
            "cost_model", "cost_value", "cost_currency", "http_referrer", "advertising_id",
            "idfa", "idfv", "platform", "device_type", "os_version", "app_version", "sdk_version",
            "app_id", "app_name", "bundle_id", "gp_broadcast_referrer", "blocked_reason",
            "blocked_reason_value", "operator", "carrier", "network_type", "wifi", "language",
            "country_code", "state", "city", "postal_code", "ip", "ua", "wifi_mac_address",
            "imei", "android_id", "customer_user_id", "is_retargeting", "reattr_touch_time",
            "reattr_touch_type", "is_primary_attribution", "att", "conversion_type",
            "event_time", "event_name", "event_value"
        ]

    def run(self, num_users: int = 100):
        self.ai_manager.generate_campaigns(num_campaigns=4)
        self.ai_manager.generate_personas(num_personas=10)

        print(f"Simulating journeys for {num_users} users...")
        all_rows = []
        for _ in range(num_users):
            user_events = self.simulator.simulate_user_events()
            all_rows.extend(user_events)

        print(f"Generated a total of {len(all_rows)} in-app events across all users.")
        print(f"Exporting to {self.output_file}...")

        with open(self.output_file, mode='w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=self.headers)
            writer.writeheader()
            writer.writerows(all_rows)

        print("Simulation complete!")

# ---------------------------------------------------------
# Execution
# ---------------------------------------------------------
if __name__ == "__main__":
    API_KEY = os.getenv("GEMINI_API_KEY", "")
    print("Starting Bank123 AppsFlyer In-App Event Simulation...")
    if API_KEY:
        print(f"Using Gemini API Key: {API_KEY[:4]}**** (hidden for security)")
    else:
        print("No GEMINI_API_KEY found -- running in offline fallback mode.")

    app = AppsFlyerSimulatorApp(
        api_key=API_KEY,
        output_file="bank123_appsflyer_in_app_events.csv"
    )

    app.run(num_users=10)