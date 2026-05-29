# VPS Deployment Guide - TrackWell Budget App Isolation

This guide provides step-by-step instructions and safe commands to configure a dedicated subdomain and database on your Ubuntu VPS. Following this checklist ensures **100% isolation** so that none of your other websites, databases, or running applications are affected.

---

## 1. Safe Isolated Database Creation

We will create a brand new database and a dedicated user. This ensures the TrackWell app has its own environment and cannot read, write, or interfere with any other app's data.

### Option A: MySQL / MariaDB (Recommended)
Run these commands to log into MySQL and set up the database:

1. Log into your database server as root:
   ```bash
   mysql -u root -p
   ```

2. Run these SQL commands inside the prompt (replace `SecurePassword123!` with a strong password):
   ```sql
   -- Create a clean, isolated database
   CREATE DATABASE trackwell_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

   -- Create a dedicated database user with access ONLY to this database
   CREATE USER 'trackwell_user'@'localhost' IDENTIFIED BY 'SecurePassword123!';

   -- Grant permissions ONLY to the trackwell_db database
   GRANT ALL PRIVILEGES ON trackwell_db.* TO 'trackwell_user'@'localhost';

   -- Apply changes and exit
   FLUSH PRIVILEGES;
   EXIT;
   ```

### Option B: PostgreSQL
If your other apps are using PostgreSQL:

1. Switch to the postgres user and open psql:
   ```bash
   sudo -i -u postgres psql
   ```

2. Run these commands inside the prompt (replace `SecurePassword123!` with a strong password):
   ```sql
   -- Create a dedicated user
   CREATE USER trackwell_user WITH PASSWORD 'SecurePassword123!';

   -- Create an isolated database owned by the new user
   CREATE DATABASE trackwell_db OWNER trackwell_user;

   -- Exit
   \q
   ```

---

## 2. Isolated Nginx Subdomain Configuration

To keep this subdomain completely independent of your other websites, we will create a dedicated configuration file in Nginx's `sites-available` directory instead of modifying default or existing configs.

1. Create a new configuration file for the TrackWell subdomain:
   ```bash
   sudo nano /etc/nginx/sites-available/budget.yourdomain.com
   ```
   *(Replace `budget.yourdomain.com` with your actual subdomain name).*

2. Copy and paste the following clean Nginx block into the file:
   ```nginx
   server {
       listen 80;
       listen [::]:80;

       server_name budget.yourdomain.com; # Replace with your subdomain

       # Root directory for static web files if you host a dashboard
       root /var/web/trackwell/public;
       index index.html;

       # Log paths isolated specifically for TrackWell debugging
       access_log /var/log/nginx/trackwell_access.log;
       error_log /var/log/nginx/trackwell_error.log;

       location / {
           try_files $uri $uri/ =404;
       }

       # If you run a backend sync API (e.g. Node.js on port 5000) later:
       # location /api {
       #     proxy_pass http://localhost:5000;
       #     proxy_http_version 1.1;
       #     proxy_set_header Upgrade $http_upgrade;
       #     proxy_set_header Connection 'upgrade';
       #     proxy_set_header Host $host;
       #     proxy_cache_bypass $http_upgrade;
       # }
   }
   ```

3. Save and close the file (in Nano, press `Ctrl+O`, `Enter`, then `Ctrl+X`).

4. Create the web root directory securely:
   ```bash
   sudo mkdir -p /var/web/trackwell/public
   sudo chown -R $USER:$USER /var/web/trackwell/public
   echo "<h1>TrackWell Subdomain is Online & Isolated</h1>" > /var/web/trackwell/public/index.html
   ```

5. Enable the new site block by creating a symbolic link:
   ```bash
   sudo ln -s /etc/nginx/sites-available/budget.yourdomain.com /etc/nginx/sites-enabled/
   ```

---

## 3. Safe Configuration Verification & SSL Setup

Before restarting Nginx, we **must** verify the syntax to ensure your existing websites remain online without any downtime.

1. Test Nginx syntax configuration:
   ```bash
   sudo nginx -t
   ```
   *Make sure you see: `syntax is ok` and `test is successful`.*

2. Safe Nginx reload (this applies changes dynamically with **zero downtime** to your other websites):
   ```bash
   sudo systemctl reload nginx
   ```

3. Secure the subdomain with Let's Encrypt SSL (only configures the new subdomain, leaving your other domains untouched):
   ```bash
   sudo certbot --nginx -d budget.yourdomain.com
   ```
   *Follow the prompts to enable HTTPS redirect.*

---

## 4. Verification Check

To confirm complete isolation, run this command to list active server blocks and verify that your new subdomain is listed alongside your existing apps:
```bash
nginx -T | grep "server_name"
```
You are fully configured and 100% isolated!
