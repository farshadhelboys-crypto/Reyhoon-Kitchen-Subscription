# Reyhoon API (Cloudflare Worker)

## استقرار سریع

1. نصب Wrangler: `npm i -g wrangler`
2. ورود: `wrangler login`
3. (اختیاری ولی توصیه‌شده) ساخت KV:
   ```
   wrangler kv:namespace create STORE
   ```
   سپس `id` را در `wrangler.toml` در بخش `kv_namespaces` بگذارید.
4. دیپلوی:
   ```
   cd worker
   wrangler deploy
   ```
5. آدرس خروجی مثل `https://reyhoon-api.XXXX.workers.dev` را در هر دو اپ اندروید در فایل `ApiConfig.kt` قرار دهید.

## پنل ادمین HTML

بعد از دیپلوی باز کنید:

`https://YOUR-WORKER.workers.dev/admin`

کلید پیش‌فرض: `reyhoon-admin-2024`  
(در `wrangler.toml` با `ADMIN_KEY` عوض کنید.)

## API خلاصه

| مسیر | روش | توضیح |
|------|-----|--------|
| `/api/menu` | GET | لیست منو (عمومی) |
| `/api/menu` | POST | افزودن غذا (ادمین) |
| `/api/menu/:id` | PUT/DELETE | ویرایش/حذف |
| `/api/orders` | GET/POST | لیست / ثبت سفارش |
| `/api/orders/:id/status` | PATCH | تغییر وضعیت |
| `/api/orders/new-count?since=` | GET | سفارش جدید برای آلارم |
| `/api/customers` | GET/POST | مشتریان |
| `/api/customers/code/:code` | GET | ورود با کد اشتراک |
