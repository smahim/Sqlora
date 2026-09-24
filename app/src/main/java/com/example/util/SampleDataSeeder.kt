package com.example.util

object SampleDataSeeder {

    val ECOMMERCE_SQL = """
        CREATE TABLE categories (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            description TEXT
        );

        INSERT INTO categories (name, description) VALUES
        ('Electronics', 'Gadgets, devices, and accessories'),
        ('Audio & Sound', 'Headphones, earbuds, and speakers'),
        ('Computing', 'Laptops, monitors, and peripherals'),
        ('Smart Home', 'IoT sensors, lights, and hubs');

        CREATE TABLE products (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            category_id INTEGER,
            sku TEXT UNIQUE NOT NULL,
            name TEXT NOT NULL,
            price REAL NOT NULL,
            stock_qty INTEGER DEFAULT 0,
            rating REAL DEFAULT 4.5,
            FOREIGN KEY (category_id) REFERENCES categories(id)
        );

        INSERT INTO products (category_id, sku, name, price, stock_qty, rating) VALUES
        (1, 'EL-001', 'Quantum X1 Phone 256GB', 899.99, 45, 4.8),
        (1, 'EL-002', 'Aero Ultra Tablet 11\"', 599.50, 28, 4.6),
        (2, 'AU-101', 'PulsePro Noise-Cancelling Headphones', 249.99, 64, 4.9),
        (2, 'AU-102', 'BassBoom Mini Bluetooth Speaker', 79.00, 120, 4.4),
        (3, 'CP-301', 'BladeCore 15 32GB RAM Laptop', 1450.00, 15, 4.7),
        (3, 'CP-302', 'UltraWide 34\" Curved 4K Monitor', 680.00, 22, 4.8),
        (3, 'CP-303', 'Mechanical RGB Tactile Keyboard', 119.99, 85, 4.5),
        (4, 'SH-501', 'Ambient Glow Smart RGB Bulb', 19.99, 210, 4.3),
        (4, 'SH-502', 'HomeGuard Video Doorbell HD', 149.00, 39, 4.7);

        CREATE TABLE customers (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            full_name TEXT NOT NULL,
            email TEXT NOT NULL,
            city TEXT,
            tier TEXT DEFAULT 'Standard'
        );

        INSERT INTO customers (full_name, email, city, tier) VALUES
        ('Alex Mercer', 'alex.mercer@example.com', 'San Francisco', 'VIP Gold'),
        ('Elena Rostova', 'elena.r@example.com', 'Berlin', 'Premium'),
        ('David Kim', 'dkim99@example.com', 'Seoul', 'Standard'),
        ('Marcus Vance', 'marcus.v@example.com', 'London', 'VIP Platinum'),
        ('Sarah Jenkins', 'sjenkins@example.com', 'Toronto', 'Standard');

        CREATE TABLE orders (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            customer_id INTEGER,
            order_date TEXT NOT NULL,
            total_amount REAL NOT NULL,
            status TEXT NOT NULL,
            FOREIGN KEY (customer_id) REFERENCES customers(id)
        );

        INSERT INTO orders (customer_id, order_date, total_amount, status) VALUES
        (1, '2026-09-18 14:22:00', 1149.98, 'Delivered'),
        (2, '2026-09-19 09:15:30', 249.99, 'Shipped'),
        (3, '2026-09-20 18:40:12', 1569.99, 'Processing'),
        (4, '2026-09-21 11:05:45', 799.50, 'Delivered'),
        (5, '2026-09-22 08:30:00', 119.99, 'Pending');
    """.trimIndent()

    val PROJECT_HUB_SQL = """
        CREATE TABLE projects (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            code TEXT UNIQUE NOT NULL,
            title TEXT NOT NULL,
            status TEXT NOT NULL,
            budget REAL,
            target_release TEXT
        );

        INSERT INTO projects (code, title, status, budget, target_release) VALUES
        ('APOLLO', 'Core Cloud Microservices Migration', 'Active', 85000.0, '2026-11-15'),
        ('NEBULA', 'Mobile App Redesign & M3 Theming', 'Active', 42000.0, '2026-10-30'),
        ('TITAN', 'High-Throughput Analytics Pipeline', 'Planning', 120000.0, '2027-02-01'),
        ('CHRONOS', 'Automated Database Backup Engine', 'Completed', 18000.0, '2026-08-20');

        CREATE TABLE tasks (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            project_id INTEGER,
            summary TEXT NOT NULL,
            assignee TEXT,
            priority TEXT DEFAULT 'Medium',
            estimated_hours INTEGER,
            is_done INTEGER DEFAULT 0,
            FOREIGN KEY (project_id) REFERENCES projects(id)
        );

        INSERT INTO tasks (project_id, summary, assignee, priority, estimated_hours, is_done) VALUES
        (1, 'Refactor auth service tokens to JWT', 'David K.', 'High', 16, 1),
        (1, 'Implement rate limiting middleware with Redis', 'Sarah J.', 'High', 24, 0),
        (2, 'Convert color tokens to dynamic dark theme', 'Alex M.', 'Critical', 12, 1),
        (2, 'Add offline Room database caching layer', 'Elena R.', 'High', 20, 1),
        (2, 'Build interactive SQLite grid table viewer', 'Alex M.', 'High', 18, 0),
        (3, 'Benchmark Kafka consumer group latency', 'Marcus V.', 'Medium', 30, 0),
        (4, 'Deploy automated cron snapshot script', 'David K.', 'Low', 8, 1);
    """.trimIndent()
}
