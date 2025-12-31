# LEO CDP Admin – Internet to Service Architecture

**Purpose**
Secure, scalable, and auditable access path for LEO CDP Admin, with clear separation of concerns.

---

### 1️⃣ Traffic Flow (Single Source of Truth)

``` mermaid
flowchart TB
    U[User]
    I[Internet]

    H["HAProxy<br/>
    <b>Internet Load Balancer</b><br/>
    Version: 2.8.x<br/>
    Ports: 80 / 443<br/><br/>
    • SSL Termination (HTTPS)<br/>
    • HTTP → HTTPS Redirect<br/>
    • Forward X-Forwarded-* headers<br/>
    • No app-level load balancing
    "]

    N["NGINX<br/>
    <b>Internal Reverse Proxy</b><br/>
    Version: 1.24.x (stable)<br/>
    Port: 9070<br/><br/>
    • Serve static Admin UI<br/>
    • Reverse proxy<br/>
    • Load balance Java workers<br/>
    • App-aware routing
    "]

    J["Java Admin Cluster<br/>
    Ports: 9071 / 9072 / 9073<br/><br/>
    • Business logic<br/>
    • Admin APIs<br/>
    • Stateless services
    "]

    U --> I
    I --> H
    H -->|HTTP internal| N
    N --> J

```



---

### 2️⃣ Component Responsibilities (Engineer View)

**HAProxy (Edge Layer)**

* Version reference: **HAProxy 2.8 LTS**
* Internet-facing only
* Handles:

  * TLS / SSL
  * Redirects
  * Connection hygiene
* Does **not** know Java, APIs, or business logic

**NGINX (Application Gateway)**

* Version reference: **NGINX 1.24 (stable branch)**
* Internal-only
* Handles:

  * `/view/*.html|js|css` static assets
  * Reverse proxy rules
  * Load balancing Java admin services
  * Header normalization

**Java Admin Services**

* Stateless
* Horizontally scalable
* Shielded from direct Internet access

---

### 3️⃣ Why This Design Is “Production-Grade”

* 🔒 **Security**: Java never touches the public Internet
* 🔁 **Scalability**: Java nodes scale independently of HAProxy
* 🧠 **Clarity**: Each layer has exactly one job
* 🛠 **Maintainability**: Certs & edge logic live in one place
* 🚀 **Future-proof**:

  * Add WAF at HAProxy
  * Add caching / rate-limit at NGINX
  * Add more Java nodes with zero edge changes

---

