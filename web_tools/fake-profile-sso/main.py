from fastapi import FastAPI, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import jwt
from datetime import datetime, timedelta, timezone

# --- Configuration ---
SECRET_KEY = "SUPER_SECRET_KEY_CHANGE_ME"
ALGORITHM = "HS256"
app = FastAPI()

# Enable CORS so your S3/MinIO site can talk to this API
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], # In production, restrict this to your domain
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Models ---
class UserLogin(BaseModel):
    email: str
    password: str

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="api/profile/login")

# --- Endpoints ---

@app.post("/api/profile/login")
async def login(user: UserLogin):
    # DUMMY AUTH: In production, verify against a database!
    if user.email == "test@example.com" and user.password == "password123":
        # Create JWT
        expire = datetime.now(timezone.utc) + timedelta(minutes=30)
        token = jwt.encode({"sub": user.email, "exp": expire}, SECRET_KEY, algorithm=ALGORITHM)
        return {"token": token}
    
    raise HTTPException(status_code=401, detail="Invalid credentials")

@app.get("/api/event/list")
async def get_events(token: str = Depends(oauth2_scheme)):
    try:
        # Validate JWT
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return {
            "status": "success",
            "user": payload.get("sub"),
            "events": [
                {"id": 1, "name": "Tech Conference 2026"},
                {"id": 2, "name": "DataHub Launch Party"}
            ]
        }
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)