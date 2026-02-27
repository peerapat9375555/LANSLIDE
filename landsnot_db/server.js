// server.js
const express = require('express');
const cors = require('cors');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const pool = require('./db');
const { v4: uuidv4 } = require('uuid'); // อย่าลืม npm install uuid

const app = express();
app.use(cors());
app.use(express.json());

const PORT = 3000;
const SECRET_KEY = "your_secret_key"; // ในของจริงควรเก็บในไฟล์ .env

// -----------------------------------------------------
// 1. API สมัครสมาชิก (Register)
// -----------------------------------------------------
app.post('/api/register', async (req, res) => {
    const { name, phone, email, password } = req.body;
    
    try {
        // เข้ารหัสผ่านก่อนบันทึกลง Database
        const hashedPassword = await bcrypt.hash(password, 10);
        const userId = uuidv4();
        
        // บันทึกลงตาราง users
        const [result] = await pool.execute(
            'INSERT INTO users (user_id, name, phone, email, password_hash, role) VALUES (?, ?, ?, ?, ?, ?)',
            [userId, name, phone, email, hashedPassword, 'user']
        );
        
        res.status(201).json({ message: "สมัครสมาชิกสำเร็จ", userId: userId });
    } catch (error) {
        if(error.code === 'ER_DUP_ENTRY') {
            return res.status(400).json({ error: "อีเมลนี้มีในระบบแล้ว" });
        }
        res.status(500).json({ error: error.message });
    }
});

// -----------------------------------------------------
// 2. API เข้าสู่ระบบ (Login) สำหรับ User และ Admin
// -----------------------------------------------------
app.post('/api/login', async (req, res) => {
    const { email, password } = req.body;
    
    try {
        // ค้นหาผู้ใช้จากอีเมล
        const [users] = await pool.execute('SELECT * FROM users WHERE email = ?', [email]);
        
        if (users.length === 0) {
            return res.status(401).json({ error: "อีเมลหรือรหัสผ่านไม่ถูกต้อง" });
        }
        
        const user = users[0];
        
        // ตรวจสอบรหัสผ่าน
        const isMatch = await bcrypt.compare(password, user.password_hash);
        if (!isMatch) {
            return res.status(401).json({ error: "อีเมลหรือรหัสผ่านไม่ถูกต้อง" });
        }
        
        // สร้าง Token ส่งกลับไปให้ Mobile App
        const token = jwt.sign(
            { userId: user.user_id, role: user.role }, 
            SECRET_KEY, 
            { expiresIn: '24h' }
        );
        
        res.json({ 
            message: "เข้าสู่ระบบสำเร็จ", 
            token: token,
            role: user.role // ส่ง Role กลับไปให้แอป Kotlin เช็คว่าเป็น Admin หรือ User
        });
        
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});