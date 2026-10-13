# ShopEase — E-Commerce Application

Responsive e-commerce platform with product search, cart management, user
authentication, and checkout.

**Stack:** React (Vite) · Java Servlets (Jakarta EE 10) · JDBC · MySQL · Bootstrap 5

## How it works

- **Search:** `products(name, description)` has a MySQL `FULLTEXT` index, and every
  query runs as a bound `PreparedStatement` (never string-concatenated SQL) — see
  `ProductDao.search()`. The API also reports how long each search took
  (`tookMs`) so you can see the sub-second response time in the network tab or
  right in the UI.
- **Auth:** passwords are hashed with bcrypt (`jbcrypt`) and login uses a plain
  `HttpSession` cookie — no separate token system to manage. `AuthFilter` guards
  `/api/cart/*` and `/api/orders/*`.
- **Cart → order:** checkout runs inside a single JDBC transaction
  (`OrderDao.checkout`) — it reserves stock for every line item, inserts the
  order, and empties the cart, rolling everything back if any item sold out
  mid-checkout.
- **Connection pooling:** a shared HikariCP pool (`DBConnectionManager`) avoids
  opening a new DB connection per request.

## Project layout

```
shopease/
├── backend/     Java Servlets API (Java 17, Maven, packaged as a .war)
├── frontend/    React + Vite app (Bootstrap 5)
└── database/    schema.sql + seed.sql (240 sample products)
```

## Prerequisites

- Java 17+ and Maven
- Node.js 18+
- MySQL 8+
- A servlet container: either Tomcat 10.1+ (Jakarta EE 10 / servlet 6.0) installed
  yourself, or let the `cargo-maven3-plugin` in `pom.xml` download one for you

All commands below are **PowerShell**, for a Windows setup.

## 1. Create the database

```powershell
cd shopease\database
mysql -u root -p -e "SOURCE schema.sql"
mysql -u root -p shopease -e "SOURCE seed.sql"
```

(If `mysql` isn't on your PATH, run those two `.sql` files from MySQL Workbench
instead.) `seed.sql` loads 240 sample products across 8 categories so search
and pagination have something real to work against.

## 2. Run the backend

Set the environment variables the app reads (`DBConnectionManager`,
`CorsFilter`):

```powershell
cd shopease\backend
$env:DB_URL = "jdbc:mysql://localhost:3306/shopease?useSSL=false&serverTimezone=UTC"
$env:DB_USER = "root"
$env:DB_PASSWORD = "your-mysql-password"
$env:FRONTEND_ORIGIN = "http://localhost:5173"
```

Then either:

**Option A — let Maven run an embedded Tomcat 10 for you:**

```powershell
mvn cargo:run
```

**Option B — build the WAR and deploy it to your own Tomcat 10.1+:**

```powershell
mvn clean package
Copy-Item target\shopease-backend.war "C:\path\to\tomcat\webapps\ROOT.war"
```

The API is then at `http://localhost:8080`. Check it with:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

> Environment variables set with `$env:` only last for the current PowerShell
> session — set them again (or add them to your Tomcat service config) next
> time you open a new terminal.

## 3. Run the frontend

```powershell
cd shopease\frontend
Copy-Item .env.example .env
npm install
npm run dev
```

Open `http://localhost:5173`.

## Building for production

```powershell
cd shopease\frontend
npm run build
```

`npm run build` outputs static files to `frontend\dist` — serve them with any
static host and point `VITE_API_BASE_URL` at your deployed backend. Also
update `FRONTEND_ORIGIN` on the backend to match that deployed frontend URL
(CORS requires an exact origin when credentials/cookies are involved).

## API summary

| Method | Path                     | Auth required | Description                     |
|--------|--------------------------|----------------|----------------------------------|
| GET    | `/api/products`          | no             | Search/filter/paginate products |
| GET    | `/api/products/{id}`     | no             | Single product                  |
| GET    | `/api/products/categories`| no           | Distinct category list          |
| POST   | `/api/auth/register`     | no             | Create an account                |
| POST   | `/api/auth/login`        | no             | Log in (sets session cookie)     |
| POST   | `/api/auth/logout`       | no             | Log out                          |
| GET    | `/api/auth/me`           | yes            | Current user                     |
| GET    | `/api/cart`              | yes            | Current user's cart              |
| POST   | `/api/cart`              | yes            | Add/increment an item            |
| PUT    | `/api/cart/{itemId}`     | yes            | Update item quantity             |
| DELETE | `/api/cart/{itemId}`     | yes            | Remove an item                   |
| POST   | `/api/orders`            | yes            | Checkout the current cart        |
| GET    | `/api/orders`            | yes            | Order history                    |
| GET    | `/api/orders/{id}`       | yes            | Single order + line items        |
