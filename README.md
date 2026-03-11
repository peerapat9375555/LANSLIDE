# 🛡️ ระบบพยากรณ์และแจ้งเตือนภัยดินถล่ม (Landslide Warning System)

> [!CAUTION]
> ### 🛑 คำเตือนเรื่องความปลอดภัย (CRITICAL SECURITY WARNING)
> **ห้ามนำไฟล์ `.env` หรือ API Key / Service Account ID ใดๆ อัปโหลดขึ้น GitHub หรือแชร์ให้ผู้อื่นโดยเด็ดขาด!**
> - ตรวจสอบให้แน่ใจว่าไฟล์ `.env` อยู่ใน `.gitignore` เสมอ
> - หากเผลอทำหลุด ให้รีบทำการ Revoke Key และสร้างใหม่ทันที
> - **ห้ามแชร์ไฟล์ `google-key.json` (ถ้ามี) หรือสิทธิ์เข้าถึง GEE ของคุณให้ใคร**

โปรเจคนี้คือระบบแจ้งเตือนและประเมินความเสี่ยงดินถล่ม ประกอบด้วย 2 ส่วนหลักคือ **API Backend** (Python FastAPI) และแอปพลิเคชัน **Android** (Kotlin + Jetpack Compose)

## 🚀 สถาปัตยกรรมระบบ
1. **Backend** (`server/main.py`): จัดการ AI Model, API, และการดึงข้อมูลจาก GEE/Open-Meteo
2. **Database**: MySQL (XAMPP) สำหรับเก็บพิกัด, ข้อมูลสถิติ และประวัติการแจ้งเตือน
3. **Frontend**: Android App พัฒนาด้วย Jetpack Compose ใช้แผนที่ OSMDroid (ฟรี 100%)
4. **Data Sources**:
   - **Google Earth Engine (GEE)**: ดึงข้อมูล Elevation, Slope, NDVI, ฯลฯ
   - **Open-Meteo**: ดึงข้อมูลน้ำฝนย้อนหลัง 10 วัน
5. **AI Model**: ใช้ Random Forest ในการวิเคราะห์ความเสี่ยงจาก 27 ปัจจัย

---

## 📝 สิ่งที่อัพเดตล่าสุด 
- UXUI USER บางส่วน      USER  หน้าปักหมุด บอกชื่อสถานที่ที่ใกล้ ตำบล อำเภอ   แล้วลง ตรงแก้ไขข้อมูล  DBใหม่   รายชื่อ ดูรูปชื่อ จากแอดมิน
- Admin ความน่าจะเป็น เรียงจากมากไปน้อย อัพรูปลงรายชื่อ ปุ่มลบรายชื่อ
- แจ้งเตือนในระยะ 20 กม. ของUSER  แล้วก็สามารถกดเข้าไปดูตรงจุดที่แจ้งเตือนได้จะบอกข้อมูล แล้วก็จะขึ้นแจ้งเตือนก็ต่อเมื่อ admin กดยืนยันแล้ว
- pv3: เพิ่มเส้นกั่น แถบ แฮมบาร์ เปลี่ยนไอคอนแอป   มีโนติเด้งข้างบน หรือล็อกจากเหตุใกล้ user20 กม       มีฟังก์ชั่นผู้ใช้ส่งข้อความ รูป ขอความช่วยเหลือ มาแจ้งแอดมิน   มีหน้าแอดมินเห็นเหตุที่แจ้ง แล้วกดยืนยัน เหตุจะไปหน้าประวัติการช่วยเหลือ
- อัปเดตล่าสุด: เพิ่มฟังก์ชันอัปเดตข้อมูลโปรไฟล์ (มีการเข้ารหัสรหัสผ่านใหม่), ปรับระบบแผนที่ให้ซูมเข้าสู่เป้าหมายอัตโนมัติ (ระดับ 16.0), และเพิ่ม Checkbox คัดกรองระดับความเสี่ยง (สูง/กลาง/ต่ำ) บนแผนที่ของทั้ง User และ Admin

- ลงดาต้าเบสใหม่ landsnot_db.sql ก่อนใช้งาน
---

## 📂 โครงสร้างโปรเจค
```
Project_mobile_app/
├── Landslideproject_cola/          # Android App (Kotlin + Jetpack Compose)
├── server/                         # Backend API หลัก (FastAPI)
│   ├── main.py                     
│   ├── seed_data.py                # Seed ข้อมูลเริ่มต้น + สร้าง Admin
│   ├── requirements.txt            # Python dependencies
│   ├── prepare_static_data.py      # ดึง GEE data แบบ standalone
│   └── data/                       # ข้อมูลที่ cache ไว้ (latest_predictions.json)
├── ml_pipeline/                    # ระบบ Machine Learning และการดึงข้อมูล
│   ├── gee_extractor.py            # สคริปต์ดึงข้อมูล GEE
│   ├── modifier_data.py            # สคริปต์แปลงค่า features ก่อนเข้าโมเดล
│   ├── retrain_model.py            # สคริปต์เทรนโมเดล ML ใหม่
│   ├── models/                     # ไฟล์โมเดลที่เซฟไว้ (.pkl)
│   └── data/                       # ไฟล์พิกัด 2,727 จุด (.csv)
├── database/                       # จัดการฐานข้อมูล (Database)
│   ├── init_database.sql           # SQL สร้างตารางข้อมูลทั้งหมด
    └── scripts/                    # เครื่องมือจัดการ DB (fix_db.py, etc)
├── docs/                           # เอกสารโครงการ สไลด์นำเสนอ
├── archive/                        # ไฟล์โค้ดเก่า Database สำรองที่ไม่ได้ใช้งานแล้ว
├── .env                            # ⚠️ ห้ามอัพ Git! (GEE Project ID, DB Config)
└── .gitignore
```

---

## 🛠️ วิธีตั้งค่าสำหรับเพื่อนร่วมทีม (Setup from Scratch)

### ขั้นตอนที่ 1: สร้างไฟล์ `.env`

สร้างไฟล์ `.env` ที่ root ของโปรเจค (`Project_mobile_app/.env`):
```
GEE_PROJECT_ID=your-gee-project-id-here
```

**วิธีหา GEE Project ID:**
1. ไปที่ [Google Cloud Console](https://console.cloud.google.com/)
2. สร้าง Project ใหม่หรือใช้ Project ที่มีอยู่
3. เปิดใช้งาน **Earth Engine API** ใน APIs & Services
4. ไปที่ [Google Earth Engine](https://code.earthengine.google.com/) แล้วลงทะเบียนด้วย Google Account
5. Copy Project ID มาใส่ในไฟล์ `.env`

> ⚠️ **ห้ามใช้ key ของคนอื่น** แต่ละคนต้องสร้าง GEE Project ของตัวเอง

---
่่่่่่่
### ขั้นตอนที่ 2: ตั้งค่าฐานข้อมูล (Database)

1. เปิดโปรแกรม **XAMPP Control Panel** → กด Start **Apache** กับ **MySQL**
2. เข้า phpMyAdmin: `http://localhost/phpmyadmin`
3. สร้างฐานข้อมูลใหม่ชื่อ **`landsnot_db`** (Collation: `utf8mb4_general_ci`)
4. เข้าไปที่แท็บ **Import** → อัปโหลดไฟล์ **`landsnot_db.sql`**

---

### ขั้นตอนที่ 3: ตั้งค่า Python Environment

> ✅ ใช้ **Python venv** (ไม่ต้องติดตั้ง Anaconda)
> ต้องมี **Python 3.11** ติดตั้งไว้ในเครื่องก่อน → [ดาวน์โหลดที่นี่](https://www.python.org/downloads/release/python-3119/)

```bash
# 1. เข้าโฟลเดอร์ server
cd server

# 2. สร้าง virtual environment โดยใช้ py
py -m venv venv

# 3. เปิดใช้งาน (Windows CMD)
venv\Scripts\activate

# 4. ติดตั้ง dependencies ทั้งหมด
py -m pip install -r requirements.txt

# 5. Authenticate Google Earth Engine (ครั้งแรกครั้งเดียว)
py -c "import ee; ee.Authenticate()"
# → จะเปิดหน้าเว็บ login Google แล้ว copy token กลับมาวาง

# 6. ทดสอบว่า GEE ทำงานได้
py -c "import ee; ee.Initialize(project='YOUR_PROJECT_ID'); print('GEE OK!')"
```

---

### ขั้นตอนที่ 4: Seed ข้อมูลเริ่มต้น + สร้าง Admin

```bash
# สั่ง seed ข้อมูลพิกัด 2,727 จุดลง DB + สร้าง Admin Account
py server/seed_data.py
```

**Admin Account ที่สร้างอัตโนมัติ:**
| Field | Value |
|-------|-------|
| Email | `admin@admin.com` |
| Password | `admin` |
| Role | `admin` |

> ⚠️ **รัน seed_data.py ครั้งเดียวเท่านั้น!** ถ้ารันซ้ำจะมีข้อมูล node ซ้ำใน DB

---

### ขั้นตอนที่ 5: เปิดเซิร์ฟเวอร์ Backend

```bash
# เปิดใช้งาน venv (ถ้ายังไม่ได้ activate)
venv\Scripts\activate

# รันเซิร์ฟเวอร์
uvicorn main:app --reload
```

> 💡 **ครั้งต่อไปที่จะรัน** แค่เปิด CMD ใน `server/` แล้วรัน 2 คำสั่งนี้:
> ```
> venv\Scripts\activate
> uvicorn main:app --reload
> ```

เมื่อเซิร์ฟเวอร์ขึ้น `Application startup complete.` แสดงว่าพร้อมใช้งาน

---

### ขั้นตอนที่ 6: เปิดแอป Android

1. เปิด **Android Studio** → Open โฟลเดอร์ `Landslideproject_cola/`
2. รอ Gradle Sync เสร็จ
3. แก้ไฟล์ `EarthquakeClient.kt`:
   - **Emulator**: ใช้ `http://10.0.2.2:8000/`
   - **มือถือจริง (USB/WiFi)**: ใช้ `http://IP_คอมพิวเตอร์:8000/`
     - หา IP: เปิด CMD → `ipconfig` → ดูที่ `IPv4 Address`
     - มือถือและคอมต้องต่อ **WiFi เดียวกัน**
4. กด ▶️ Run

---

## 🔄 วิธีใช้งานระบบ (สำหรับ Admin)

1. **Login** ด้วย `admin@admin.com` / `admin`
2. ไปหน้า **ดึงข้อมูล & วิเคราะห์** (ผ่าน Drawer menu)
3. กด **"ดึงข้อมูล GEE"** (ครั้งแรก รอ ~5-10 นาที ดึง Elevation/Slope/NDVI ฯลฯ)
4. กด **"ดึงน้ำฝน & วิเคราะห์"** (ดึงน้ำฝน 10 วัน + รัน ML predict)
5. ไปหน้า **แจ้งเตือน** → กดเลือก Alert ที่ต้องการ → ยืนยัน/ไม่ยืนยัน

---

## 📋 สรุป Dependencies

| Library | Version | หน้าที่ |
|---------|---------|---------|
| fastapi | 0.115.0 | Web Framework |
| uvicorn | 0.30.6 | ASGI Server |
| mysql-connector-python | 9.0.0 | เชื่อมต่อ MySQL |
| pandas | 2.2.2 | จัดการ DataFrame |
| numpy | 1.26.4 | คำนวณตัวเลข |
| scikit-learn | 1.8.0 | ML Model (Random Forest, Gradient Boosting ฯลฯ) |
| joblib | 1.4.2 | โหลด/บันทึก model และ scaler |
| httpx | 0.27.0 | HTTP Client (Open-Meteo) |
| bcrypt | 4.2.0 | Hash รหัสผ่าน |
| PyJWT | 2.9.0 | JSON Web Token |
| earthengine-api | 1.7.15 | Google Earth Engine |
| python-dotenv | 1.2.1 | อ่าน .env |

**Optional (สำหรับ retrain_model.py เพื่อเทียบโมเดลเพิ่มเติม):**

| Library | หน้าที่ |
|---------|----------|
| xgboost | XGBoost Classifier |
| lightgbm | LightGBM Classifier |
| catboost | CatBoost Classifier |

ติดตั้ง optional packages:
```bash
pip install xgboost lightgbm catboost
```
