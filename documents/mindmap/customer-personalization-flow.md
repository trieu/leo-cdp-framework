```mermaid
flowchart TD
    A[Data Storage: User behavior, transactions, etc.] --> B[Feature Engineering: <br> Profile, Item & Campaign]
    B --> C[Run Data Vectorization Job]
    C --> D[Store data in Qdrant Vector Database]
    D --> E[For each profile: Run vector search get best matched items & campaign flows]
    E --> F[Set campaign flow in profile]
    F --> G[Monitoring & Feedback<br>Loop]
    G --> E
    E --> P[Set Item Ranking in Customer Profiles]
    E --> H[Real-time Recommendations Service]
    H --> I[API Gateway<br> FastAPI]
    I --> S[Run Marketing Campaign Automation: email, notification, chat]
    P --> S
    S --> T[Tracking behavior: short-link-click, item-view, purchase, made-payment]
    T --> A