# VoiceStock — Voice-Based Inventory Management

Full-stack multilingual inventory platform for small shops: voice commands, camera/barcode capture, radius vendor discovery, translated chat, orders, and sales analytics.

**Roles:** Admin · Shopkeeper · Vendor

## Stack

| Layer | Technology |
|-------|------------|
| Frontend | React (Vite), React Router, Axios |
| Backend | Java 21, Spring Boot 4, Spring Security, JPA |
| Auth | JWT + BCrypt |
| Database | H2 (default) or MySQL |

## Quick start

### 1. Backend

```bash
cd backend
.\mvnw.cmd spring-boot:run
```

API: http://localhost:8080  
H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:inventorydb`, user `sa`, blank password)

**MySQL** (optional):

```bash
# Create DB (or use database/schema.sql)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=mysql"
```

Edit `src/main/resources/application-mysql.properties` for username/password.

### 2. Frontend

```bash
cd frontend
npm install
npm run dev
```

App: http://localhost:5173

## Demo accounts

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@admin.gmail.com` | `Admin@123` |
| Shopkeeper | `shop@demo.com` | `Shop@123` |
| Vendor | `vendor@demo.com` | `Vendor@123` |

Admin emails must match `*@admin.gmail.com`.

## Features implemented

- Registration/login with role-based JWT auth and account blocking
- Admin: vendors, shopkeepers, location counts, queries, profile
- Shopkeeper: inventory list, add via camera / barcode / voice, thresholds & low-stock alerts
- Radius vendor search (Haversine), multilingual chat (text + voice transcript + translation stub)
- Book/withdraw offers, order lifecycle, sales history & highest-selling product
- Vendor: stock management, orders, other-vendor discovery, chat, contact admin

Voice AI uses rule-based intent parsing (add/remove/low-stock/find vendors) plus browser Speech Recognition where available. Translation is a demo stub ready to swap for a cloud API.

## Project layout

```
samll_Business/
├── frontend/          # React app
├── backend/           # Spring Boot API
└── database/          # MySQL schema reference
```

## Example voice commands

- `10 Pepsi bottles add cheyyi`
- `5 soaps remove cheyyi`
- `Show my low stock items`
- `Rice vendors ni 5 kilometers lo chupinchu`
