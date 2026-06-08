# 🚀 คู่มือ Deploy ขึ้น AWS EC2 (ตั้งแต่ศูนย์จนเสร็จ)

คู่มือนี้พาทำ **ทุกขั้นตอน** ตั้งแต่สร้าง EC2 จนเว็บใช้งานได้จริงบน HTTPS
อ่านจากบนลงล่างตามลำดับได้เลย คำสั่งทุกอันคัดลอกวางได้

---

## 🗺️ ภาพรวมสถาปัตยกรรม (1 เครื่อง EC2)

```
                         Internet (HTTPS 443)
                                │
                    ┌───────────▼────────────┐
                    │   host nginx + certbot  │  ← TLS/โดเมน
                    └─────┬───────────────┬───┘
          app.example.com │               │ api.example.com
                          ▼               ▼
                 127.0.0.1:8081     127.0.0.1:8080
                 ┌──────────┐       ┌──────────────────────────┐
                 │ FE (nginx│       │ BE (Spring Boot, Docker)  │
                 │  + SPA)  │       │  + MySQL + Redis + Rabbit │
                 │ +return- │       └──────────────────────────┘
                 │  2c2p    │
                 └──────────┘
```

- **2 โดเมนย่อย**: `app.example.com` (เว็บ) + `api.example.com` (API)
- container ทั้งหมดฟังแค่ `127.0.0.1` → ข้างนอกเข้าได้ทาง **host nginx (HTTPS) เท่านั้น**

> 📌 แทน `example.com` ด้วยโดเมนจริงของคุณทุกที่ในคู่มือนี้

---

## ✅ สิ่งที่ต้องเตรียมก่อนเริ่ม
- บัญชี AWS
- โดเมน 1 อัน (ตั้งค่า DNS ได้)
- โปรแกรม SSH (Terminal/PuTTY)
- ค่าจริงทั้งหมด (DB pw, Gmail App Password, AWS S3 key, OAuth secret, payment key)

---

# ขั้นที่ 1 — สร้าง EC2 Instance

1. เข้า AWS Console → ค้น **EC2** → **Launch instance**
2. ตั้งค่า:
   | ช่อง | ค่าที่เลือก |
   |---|---|
   | **Name** | `membership-prod` |
   | **AMI** | Ubuntu Server **24.04 LTS** (64-bit x86) |
   | **Instance type** | **t3.small** (2GB) ขั้นต่ำ — ถ้างบถึงใช้ **t3.medium** (4GB) ลื่นกว่า |
   | **Key pair** | สร้างใหม่ → ดาวน์โหลดไฟล์ `.pem` เก็บไว้ดีๆ |
   | **Storage** | **20 GB** gp3 (ขั้นต่ำ; 30GB ยิ่งดี) |

3. **Network settings → Edit → Security Group** ตั้ง inbound rule:
   | Type | Port | Source | เหตุผล |
   |---|---|---|---|
   | SSH | 22 | **My IP** | เข้า SSH (จำกัดเฉพาะ IP คุณ) |
   | HTTP | 80 | Anywhere (0.0.0.0/0) | เว็บ + ต่ออายุ SSL |
   | HTTPS | 443 | Anywhere (0.0.0.0/0) | เว็บ |

   > ⚠️ **อย่าเปิด** 8080 / 3306 / 6379 / 5672 สู่สาธารณะเด็ดขาด (container ฟังแค่ localhost อยู่แล้ว)

4. กด **Launch instance** → รอ ~1 นาที → จด **Public IPv4 address** (เช่น `13.250.x.x`)

> 💡 แนะนำผูก **Elastic IP** (EC2 → Elastic IPs → Allocate → Associate กับ instance) เพื่อให้ IP ไม่เปลี่ยนเวลา reboot

---

# ขั้นที่ 2 — ตั้งค่า DNS (โดเมน)

ไปที่ผู้ให้บริการโดเมน เพิ่ม **A record** 2 ตัว ชี้ไป Public IP ของ EC2:

| Type | Name | Value |
|---|---|---|
| A | `app` | `13.250.x.x` |
| A | `api` | `13.250.x.x` |

รอ DNS propagate (ไม่กี่นาที–ชั่วโมง) เช็คด้วย: `ping app.example.com`

---

# ขั้นที่ 3 — เข้า SSH

```bash
# บนเครื่องคุณ — ตั้งสิทธิ์ไฟล์ key ก่อน (ครั้งเดียว)
chmod 400 ~/Downloads/your-key.pem

# เข้า server
ssh -i ~/Downloads/your-key.pem ubuntu@13.250.x.x
```
ถ้าเจอถาม "Are you sure...?" พิมพ์ `yes`

---

# ขั้นที่ 4 — ติดตั้งของบน server

รันทีละบล็อก (อยู่ใน EC2 แล้ว):

```bash
# 4.1 อัปเดตระบบ
sudo apt update && sudo apt upgrade -y

# 4.2 ติดตั้ง Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu          # ใช้ docker โดยไม่ต้อง sudo
newgrp docker                           # ให้ group มีผลทันที (หรือ logout/login ใหม่)

# 4.3 ติดตั้ง nginx + certbot (สำหรับ HTTPS) + git
sudo apt install -y nginx certbot python3-certbot-nginx git

# 4.4 เช็คว่าได้จริง
docker --version && docker compose version && nginx -v
```

---

# ขั้นที่ 5 — Clone โค้ดทั้ง 2 repo

```bash
cd ~
git clone https://github.com/MeowHito/REGISTER-backend.git
git clone https://github.com/MeowHito/REGISTER-FRONTEND.git
```

---

# ขั้นที่ 6 — ตั้งค่า + รัน Backend

```bash
cd ~/REGISTER-backend
cp .env.production.example .env.production
nano .env.production          # แก้ค่า (Ctrl+O บันทึก, Ctrl+X ออก)
```

**ค่าที่ต้องแก้ใน `.env.production`** (ตัวอย่างเต็ม — แทนด้วยค่าจริง):

```ini
# --- App ---
APP_ENV=PROD
APP_FE_URL=https://app.example.com
APP_BE_URL=https://api.example.com
APP_CONTACT_EMAIL=admin@example.com
SEED_USER_PASSWORD=ตั้งรหัสแอดมินเริ่มต้น

# --- Security (ใช้ค่าที่ generate ให้) ---
JWT_SECRET_KEY=QyQw4mdChwDYTm5DuREfP7FWvWHrc9g8RtPolUPCHQBjWQC3g2ZIbV3k03US9x6Q
PARTICIPANT_TOKEN_SECRET=GpGcZ7GgqWDUi4S15PovlWtovzNGf1H3tl2pOeowhNIhKS2ttxTPTd3xCXkTZcuH

# --- Database (host = ชื่อ service ใน compose) ---
DB_URL=jdbc:mysql://mysql:3306/membership_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Bangkok
DB_USER=root
DB_PASSWORD=ตั้งรหัส-db-ที่แข็งแรง
JPA_DDL_AUTO=none

# --- Mail ---
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=you@gmail.com
SMTP_PASSWORD=gmail-app-password-16ตัว

# --- Redis / RabbitMQ (host = ชื่อ service) ---
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=ตั้งรหัส-redis
RMQ_HOST=rabbitmq
RMQ_PORT=5672
RMQ_USER=membership
RMQ_PASSWORD=ตั้งรหัส-rabbit

# --- AWS S3 (bucket ต้องอยู่ ap-southeast-1) ---
AWS_S3_ACCESS_KEY=AKIA...
AWS_S3_SECRET_KEY=...
AWS_S3_BUCKET_NAME=ชื่อ-bucket
AWS_S3_ROOT_PATH=action_membership/

# --- OAuth ---
AUTH_GOOGLE_CLIENT_ID=...
AUTH_GOOGLE_CLIENT_SECRET=...
AUTH_GOOGLE_REDIRECT_URL=https://app.example.com/auth/google/callback
AUTH_FB_CLIENT_ID=...
AUTH_FB_CLIENT_SECRET=...
AUTH_LINE_CLIENT_ID=...
AUTH_LINE_CLIENT_SECRET=...

# --- Payment (เปลี่ยนเป็น production URL/key) ---
SCB_APP_KEY=...
SCB_APP_SECRET=...
SCB_BASE_URL=https://api.partners.scb
SCB_COMPANY_ID=...
SCB_TERMINAL_ID=...
SCB_PP_ID=...
SCB_REF3=SCB
MERCHANT_ID_2C2P=...
MERCHANT_ID_2C2P_EWALLET=...
PAYMENT_KEY_2C2P=...
INQUIRY_URL_2C2P=https://pgw.2c2p.com/payment/4.1/paymentInquiry
TOKEN_URL_2C2P=https://pgw.2c2p.com/payment/4.1/paymentToken

SWAGGER_ENABLED=false
```

**รัน backend:**
```bash
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```
- ครั้งแรก build นาน (~2-5 นาที) + MySQL โหลด schema 53 ตารางอัตโนมัติ
- ดูสถานะ: `docker compose -f docker-compose.prod.yml ps` (ควร healthy ทุกตัว)
- ดู log: `docker logs -f membership-app` → รอ **`Started MembershipApplication`** (Ctrl+C ออกจากดู log)

ทดสอบในเครื่อง: `curl -I http://127.0.0.1:8080/v3/api-docs` (ควรได้ HTTP 200/403)

---

# ขั้นที่ 7 — ตั้งค่า + รัน Frontend

```bash
cd ~/REGISTER-FRONTEND
cp .env.production.example .env.production
nano .env.production
```
แก้:
```ini
VITE_CONTEXT_URL=https://api.example.com    # ⚠️ ไม่ใส่ /membershipms (embedded Tomcat = root)
VITE_CONTEXT_URL_LOCAL=http://localhost:8080
VITE_AUTH_GOOGLE_ID=...                       # client ID (ตัวเดียวกับ backend client-id)
VITE_AUTH_FACEBOOK_ID=...                     # App ID
VITE_AUTH_LINE_LIFF=...                       # LIFF ID
# ตัวแปร VITE_STORAGE_* ปล่อยตามค่า default ได้
```
รัน:
```bash
docker compose -f deploy/docker-compose.yml up -d --build
```
ทดสอบในเครื่อง: `curl -I http://127.0.0.1:8081` (ควรได้ HTTP 200)

---

# ขั้นที่ 8 — Host nginx (เชื่อมโดเมน → container)

สร้าง config 2 ไฟล์:

```bash
sudo nano /etc/nginx/sites-available/api.example.com
```
วาง (แก้โดเมน):
```nginx
server {
    listen 80;
    server_name api.example.com;
    client_max_body_size 25m;          # รองรับอัปโหลดไฟล์ (backend จำกัด 20MB)

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo nano /etc/nginx/sites-available/app.example.com
```
วาง (แก้โดเมน):
```nginx
server {
    listen 80;
    server_name app.example.com;
    client_max_body_size 25m;

    location / {
        proxy_pass http://127.0.0.1:8081;   # FE container (nginx ข้างในจัดการ SPA + return-2c2p เอง)
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

เปิดใช้งาน + รีโหลด:
```bash
sudo ln -s /etc/nginx/sites-available/api.example.com /etc/nginx/sites-enabled/
sudo ln -s /etc/nginx/sites-available/app.example.com /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default      # ลบ default page
sudo nginx -t                                    # ทดสอบ config (ต้องขึ้น OK)
sudo systemctl reload nginx
```
ทดสอบ: เปิด `http://app.example.com` ในเบราว์เซอร์ → ควรเห็นเว็บ (ยังเป็น http)

---

# ขั้นที่ 9 — เปิด HTTPS (certbot) 🔒 จำเป็น

```bash
sudo certbot --nginx -d app.example.com -d api.example.com
```
- ใส่อีเมล, กด `A` ยอมรับเงื่อนไข
- เลือก **redirect HTTP → HTTPS** (แนะนำ Yes)
- certbot จะแก้ nginx ให้เป็น 443 + ติดตั้ง cert อัตโนมัติ
- **ต่ออายุอัตโนมัติ** มีให้แล้ว เช็คได้: `sudo certbot renew --dry-run`

ทดสอบ: เปิด **`https://app.example.com`** → ต้องมีกุญแจเขียว 🔒

---

# ขั้นที่ 10 — ตั้งค่า Dashboard ผู้ให้บริการเป็น production

> ทำในเว็บของแต่ละเจ้า (นอก server)

- **Google** (console.cloud.google.com → Credentials):
  - Authorized JavaScript origins: `https://app.example.com`
  - Authorized redirect URIs: `https://app.example.com/auth/google/callback`
  - **Publish app** (OAuth consent screen → Publish) เพื่อให้คนทั่วไปล็อกอินได้
- **Facebook** (developers.facebook.com):
  - Valid OAuth Redirect URIs + App Domain = `app.example.com`
  - สลับเป็น **Live mode** (ต้องมี Privacy Policy URL)
- **LINE** (developers.line.biz):
  - LIFF Endpoint URL = `https://app.example.com`
  - Callback URL = `https://app.example.com/auth/line/callback`
- **Payment**: เปลี่ยน SCB/2C2P เป็น merchant/key **production** + URL จริง (ใน `.env.production` ของ backend)

---

# ขั้นที่ 11 — ทดสอบใช้งานจริง (smoke test)

- [ ] `https://app.example.com` โหลดได้ + refresh หน้าลึกๆ ไม่ 404
- [ ] login ด้วยอีเมล แล้วลอง Google / Facebook / LINE
- [ ] ทำรายการที่ส่งอีเมล → อีเมลส่งจริง (ต้อง `APP_ENV=PROD`)
- [ ] อัปโหลดไฟล์ → ขึ้น S3 ได้
- [ ] ทดสอบจ่ายเงิน → เด้งกลับมาหน้า `/registrationPaymentResult`

---

# 🛠️ การดูแลหลัง deploy (Operations)

```bash
# ดู log
docker logs -f membership-app
docker compose -f ~/REGISTER-backend/docker-compose.prod.yml logs -f

# restart
docker compose -f ~/REGISTER-backend/docker-compose.prod.yml restart

# อัปเดตโค้ดใหม่ (หลัง git push)
cd ~/REGISTER-backend && git pull && docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
cd ~/REGISTER-FRONTEND && git pull && docker compose -f deploy/docker-compose.yml up -d --build

# Backup ฐานข้อมูล (ทำสม่ำเสมอ!)
docker exec membership-mysql mysqldump -uroot -p"$DB_PASSWORD" membership_db > ~/backup_$(date +%F).sql
```

---

# 🧯 แก้ปัญหาที่เจอบ่อย

| อาการ | สาเหตุ / วิธีแก้ |
|---|---|
| login แล้วเด้งออก / ไม่จำ session | ยังไม่ได้ HTTPS หรือ `APP_ENV` ไม่ใช่ PROD (cookie Secure) → ทำขั้นที่ 9 + เช็ค env |
| `redirect_uri_mismatch` ตอน login Google | redirect URI ใน Google console ไม่ตรง → ต้องเป็น `https://app.example.com/auth/google/callback` เป๊ะ |
| เว็บเปิดได้แต่ refresh แล้ว 404 | SPA fallback — FE container จัดการให้แล้ว ถ้ายังเจอเช็ค host nginx proxy_pass ถูก container |
| อีเมลไม่ส่ง | `APP_ENV` ต้อง PROD + SMTP ถูก + (Gmail ต้องเป็น App Password) |
| backend start ไม่ขึ้น | `docker logs membership-app` ดู error — มักเป็น env ผิด/DB ต่อไม่ได้ |
| 502 Bad Gateway | container ยังไม่ขึ้น/พัง → `docker compose ps` + ดู log |
| อัปโหลดไฟล์ fail | bucket ต้องอยู่ region **ap-southeast-1** + key ถูก |

---

## 📌 สรุป checklist
1. ✅ EC2 (t3.small+) + Security Group (22/80/443)
2. ✅ DNS app./api. → IP
3. ✅ ลง Docker + nginx + certbot
4. ✅ clone 2 repo + เติม `.env.production` ค่าจริง
5. ✅ รัน 2 compose
6. ✅ host nginx 2 vhost + certbot HTTPS
7. ✅ ตั้ง dashboard OAuth/payment เป็น production
8. ✅ smoke test

> ⚠️ `.env.production` ทั้ง 2 repo **ห้าม commit** (gitignore ไว้แล้ว) — เก็บไว้บน server เท่านั้น
