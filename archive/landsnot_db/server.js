// server.js - Landslide & Earthquake Prediction API
const express = require('express');
const cors    = require('cors');
const bcrypt  = require('bcrypt');
const jwt     = require('jsonwebtoken');
const pool    = require('./db');
const { v4: uuidv4 } = require('uuid');
require('dotenv').config();

const app = express();
app.use(cors());
app.use(express.json());

const PORT       = process.env.PORT || 3000;
const SECRET_KEY = process.env.JWT_SECRET || 'landslide_secret_key_2025';

// =============================================================
// MIDDLEWARE: ตรวจสอบ JWT Token
// =============================================================
function authMiddleware(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    if (!token) return res.status(401).json({ error: true, message: 'ไม่มี Token กรุณาเข้าสู่ระบบ' });

    jwt.verify(token, SECRET_KEY, (err, user) => {
        if (err) return res.status(403).json({ error: true, message: 'Token หมดอายุหรือไม่ถูกต้อง' });
        req.user = user;
        next();
    });
}

// =============================================================
// 1. REGISTER - สมัครสมาชิก
// =============================================================
app.post('/api/register', async (req, res) => {
    const { name, phone, email, password, role } = req.body;

    if (!name || !email || !password) {
        return res.status(400).json({ error: true, message: 'กรุณากรอกข้อมูลให้ครบ (name, email, password)' });
    }

    try {
        const hashedPassword = await bcrypt.hash(password, 10);
        const userId = uuidv4();

        await pool.execute(
            'INSERT INTO users (user_id, name, phone, email, password_hash, role) VALUES (?, ?, ?, ?, ?, ?)',
            [userId, name, phone || null, email, hashedPassword, role || 'user']
        );

        res.status(201).json({
            error: false,
            message: 'สมัครสมาชิกสำเร็จ',
            user_id: userId
        });
    } catch (error) {
        if (error.code === 'ER_DUP_ENTRY') {
            return res.status(400).json({ error: true, message: 'อีเมลนี้มีในระบบแล้ว' });
        }
        res.status(500).json({ error: true, message: error.message });
    }
});

// =============================================================
// 2. LOGIN - เข้าสู่ระบบ
// =============================================================
app.post('/api/login', async (req, res) => {
    const { email, password } = req.body;

    if (!email || !password) {
        return res.status(400).json({ error: true, message: 'กรุณากรอก email และ password' });
    }

    try {
        const [users] = await pool.execute('SELECT * FROM users WHERE email = ?', [email]);

        if (users.length === 0) {
            return res.status(401).json({ error: true, message: 'อีเมลหรือรหัสผ่านไม่ถูกต้อง', user_id: '', role: '' });
        }

        const user = users[0];
        const isMatch = await bcrypt.compare(password, user.password_hash);

        if (!isMatch) {
            return res.status(401).json({ error: true, message: 'อีเมลหรือรหัสผ่านไม่ถูกต้อง', user_id: '', role: '' });
        }

        const token = jwt.sign(
            { userId: user.user_id, role: user.role },
            SECRET_KEY,
            { expiresIn: '24h' }
        );

        res.json({
            error:   false,
            message: 'เข้าสู่ระบบสำเร็จ',
            token:   token,
            user_id: user.user_id,
            name:    user.name,
            email:   user.email,
            role:    user.role
        });
    } catch (error) {
        res.status(500).json({ error: true, message: error.message, user_id: '', role: '' });
    }
});

// =============================================================
// 3. GET USER PROFILE
// =============================================================
app.get('/api/user/:id', async (req, res) => {
    try {
        const [rows] = await pool.execute(
            'SELECT user_id, name, email, phone, role FROM users WHERE user_id = ?',
            [req.params.id]
        );
        if (rows.length === 0) {
            return res.status(404).json({ error: true, message: 'ไม่พบข้อมูลผู้ใช้' });
        }
        res.json({ error: false, ...rows[0] });
    } catch (error) {
        res.status(500).json({ error: true, message: error.message });
    }
});

// =============================================================
// 4. GET ALL PREDICTIONS - ดูการทำนายทั้งหมด
// =============================================================
app.get('/api/predictions', async (req, res) => {
    try {
        const [rows] = await pool.execute(
            'SELECT * FROM landslide_prediction ORDER BY analyzed_at DESC LIMIT 50'
        );
        res.json(rows);
    } catch (error) {
        res.status(500).json({ error: true, message: error.message });
    }
});

// =============================================================
// 5. GET PREDICTION BY ID
// =============================================================
app.get('/api/predictions/:id', async (req, res) => {
    try {
        const [rows] = await pool.execute(
            'SELECT * FROM landslide_prediction WHERE prediction_id = ?',
            [req.params.id]
        );
        if (rows.length === 0) {
            return res.status(404).json({ error: true, message: 'ไม่พบข้อมูลการทำนาย' });
        }
        res.json(rows[0]);
    } catch (error) {
        res.status(500).json({ error: true, message: error.message });
    }
});

// =============================================================
// 6. CREATE PREDICTION - บันทึกการทำนาย
// =============================================================
app.post('/api/predictions', async (req, res) => {
    const { latitude, longitude, district, risk_score, risk_level, confidence, model_version } = req.body;

    if (risk_score === undefined) {
        return res.status(400).json({ error: true, message: 'กรุณาระบุ risk_score' });
    }

    try {
        const predId = uuidv4();
        await pool.execute(
            'INSERT INTO landslide_prediction (prediction_id, latitude, longitude, district, risk_score, risk_level, confidence, model_version) VALUES (?, ?, ?, ?, ?, ?, ?, ?)',
            [predId, latitude || null, longitude || null, district || null, risk_score, risk_level || null, confidence || null, model_version || 'v1.0']
        );
        res.status(201).json({ error: false, message: 'บันทึกการทำนายสำเร็จ', prediction_id: predId });
    } catch (error) {
        res.status(500).json({ error: true, message: error.message });
    }
});

// =============================================================
// 7. GET LANDSLIDE EVENTS - เหตุการณ์แผ่นดินไหว
// =============================================================
app.get('/api/events', async (req, res) => {
    try {
        const [rows] = await pool.execute(
            'SELECT * FROM landslide_events ORDER BY occurred_at DESC LIMIT 50'
        );
        res.json(rows);
    } catch (error) {
        res.status(500).json({ error: true, message: error.message });
    }
});

// =============================================================
// 8. GET NOTIFICATIONS FOR USER
// =============================================================
app.get('/api/notifications/:user_id', async (req, res) => {
    try {
        const [rows] = await pool.execute(
            'SELECT * FROM notifications WHERE user_id = ? ORDER BY sent_at DESC',
            [req.params.user_id]
        );
        res.json(rows);
    } catch (error) {
        res.status(500).json({ error: true, message: error.message });
    }
});

// =============================================================
// 9. MARK NOTIFICATION AS READ
// =============================================================
app.put('/api/notifications/:notification_id/read', async (req, res) => {
    try {
        await pool.execute(
            'UPDATE notifications SET is_read = 1 WHERE notification_id = ?',
            [req.params.notification_id]
        );
        res.json({ error: false, message: 'อ่านการแจ้งเตือนแล้ว' });
    } catch (error) {
        res.status(500).json({ error: true, message: error.message });
    }
});

// =============================================================
// 10. GET EMERGENCY SERVICES
// =============================================================
app.get('/api/emergency', async (req, res) => {
    try {
        const [rows] = await pool.execute('SELECT * FROM emergency_services ORDER BY service_name');
        res.json(rows);
    } catch (error) {
        res.status(500).json({ error: true, message: error.message });
    }
});

// =============================================================
// 11. GET ALL USERS (admin only)
// =============================================================
app.get('/api/users', async (req, res) => {
    try {
        const [rows] = await pool.execute(
            'SELECT user_id, name, email, phone, role, created_at FROM users ORDER BY created_at DESC'
        );
        res.json(rows);
    } catch (error) {
        res.status(500).json({ error: true, message: error.message });
    }
});

// =============================================================
// START SERVER
// =============================================================
app.listen(PORT, () => {
    console.log(`🌍 Landslide Prediction Server running on http://localhost:${PORT}`);
    console.log(`📊 Database: ${process.env.DB_NAME || 'landsnot_db'}`);
});