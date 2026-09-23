/**
 * Reyhoon Kitchen API - Cloudflare Worker
 * منو، مشتری، سفارش، وضعیت، پنل ادمین HTML
 */

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, X-Admin-Key",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8", ...CORS },
  });
}

function html(body, status = 200) {
  return new Response(body, {
    status,
    headers: { "Content-Type": "text/html; charset=utf-8", ...CORS },
  });
}

// حافظه موقت اگر KV نباشد
const mem = {
  menu: [],
  customers: [],
  orders: [],
  payments: [],
};

async function load(env, key) {
  if (env.STORE) {
    const v = await env.STORE.get(key, "json");
    return v ?? (key === "menu" || key === "customers" || key === "orders" || key === "payments" ? [] : null);
  }
  return mem[key] ?? [];
}

async function save(env, key, value) {
  if (env.STORE) {
    await env.STORE.put(key, JSON.stringify(value));
  } else {
    mem[key] = value;
  }
}

function uid() {
  return crypto.randomUUID();
}

function isAdmin(req, env) {
  const key = req.headers.get("X-Admin-Key") || "";
  return key && key === (env.ADMIN_KEY || "reyhoon-admin-2024");
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: CORS });
    }

    const url = new URL(request.url);
    const path = url.pathname.replace(/\/$/, "") || "/";

    try {
      // ---- Admin panel ----
      if (path === "/" || path === "/admin") {
        return html(adminHtml());
      }

      // ---- Menu ----
      if (path === "/api/menu" && request.method === "GET") {
        const menu = await load(env, "menu");
        return json(menu);
      }

      if (path === "/api/menu" && request.method === "POST") {
        if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
        const body = await request.json();
        const menu = await load(env, "menu");
        const item = {
          id: uid(),
          name: body.name || "",
          description: body.description || "",
          price: Number(body.price) || 0,
          category: body.category || "عمومی",
          isAvailable: body.isAvailable !== false,
        };
        menu.push(item);
        await save(env, "menu", menu);
        return json(item, 201);
      }

      if (path.startsWith("/api/menu/") && request.method === "PUT") {
        if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
        const id = path.split("/").pop();
        const body = await request.json();
        const menu = await load(env, "menu");
        const i = menu.findIndex((x) => x.id === id);
        if (i < 0) return json({ error: "not found" }, 404);
        menu[i] = { ...menu[i], ...body, id };
        await save(env, "menu", menu);
        return json(menu[i]);
      }

      if (path.startsWith("/api/menu/") && request.method === "DELETE") {
        if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
        const id = path.split("/").pop();
        let menu = await load(env, "menu");
        menu = menu.filter((x) => x.id !== id);
        await save(env, "menu", menu);
        return json({ ok: true });
      }

      // ---- Customers ----
      if (path === "/api/customers" && request.method === "GET") {
        if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
        return json(await load(env, "customers"));
      }

      if (path === "/api/customers" && request.method === "POST") {
        if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
        const body = await request.json();
        const customers = await load(env, "customers");
        const c = {
          id: uid(),
          name: body.name || "",
          phone: body.phone || "",
          address: body.address || { street: "", city: "", postalCode: "", notes: "" },
          subscriptionCode: body.subscriptionCode || null,
          debt: Number(body.debt) || 0,
          credit: Number(body.credit) || 0,
          notes: body.notes || "",
          createdAt: Date.now(),
        };
        customers.push(c);
        await save(env, "customers", customers);
        return json(c, 201);
      }

      if (path.startsWith("/api/customers/code/") && request.method === "GET") {
        const code = decodeURIComponent(path.split("/").pop());
        const customers = await load(env, "customers");
        const c = customers.find(
          (x) => x.subscriptionCode && x.subscriptionCode.toLowerCase() === code.toLowerCase()
        );
        if (!c) return json({ error: "not found" }, 404);
        return json(c);
      }

      if (path.startsWith("/api/customers/") && request.method === "PUT") {
        if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
        const id = path.split("/").pop();
        const body = await request.json();
        const customers = await load(env, "customers");
        const i = customers.findIndex((x) => x.id === id);
        if (i < 0) return json({ error: "not found" }, 404);
        customers[i] = { ...customers[i], ...body, id };
        await save(env, "customers", customers);
        return json(customers[i]);
      }

      if (path.startsWith("/api/customers/") && request.method === "DELETE") {
        if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
        const id = path.split("/").pop();
        let customers = await load(env, "customers");
        customers = customers.filter((x) => x.id !== id);
        await save(env, "customers", customers);
        return json({ ok: true });
      }

      // ---- Orders ----
      if (path === "/api/orders" && request.method === "GET") {
        const customerId = url.searchParams.get("customerId");
        const status = url.searchParams.get("status");
        let orders = await load(env, "orders");
        if (customerId) orders = orders.filter((o) => o.customerId === customerId);
        if (status) orders = orders.filter((o) => o.status === status);
        orders.sort((a, b) => b.createdAt - a.createdAt);
        return json(orders);
      }

      if (path === "/api/orders" && request.method === "POST") {
        const body = await request.json();
        const customers = await load(env, "customers");
        const customer = customers.find((c) => c.id === body.customerId);
        if (!customer) return json({ error: "customer not found" }, 404);

        const items = body.items || [];
        const total = items.reduce((s, it) => s + (Number(it.unitPrice) || 0) * (Number(it.quantity) || 1), 0);
        let credit = customer.credit || 0;
        let debt = customer.debt || 0;
        const creditApplied = Math.min(credit, total);
        credit -= creditApplied;
        const afterCredit = total - creditApplied;
        const paidNow = Math.max(0, Number(body.paidNow) || 0);
        const cashUsed = Math.min(paidNow, afterCredit);
        const overpay = Math.max(0, paidNow - afterCredit);
        if (overpay > 0) credit += overpay;
        const shortfall = afterCredit - cashUsed;
        if (shortfall > 0) debt += shortfall;

        const order = {
          id: uid(),
          customerId: customer.id,
          customerName: customer.name,
          customerPhone: customer.phone || "",
          items,
          totalAmount: total,
          paidAmount: creditApplied + cashUsed,
          creditApplied,
          status: "registered",
          createdAt: Date.now(),
          preparingAt: null,
          shippedAt: null,
          deliveredAt: null,
          note: body.note || "",
        };

        const orders = await load(env, "orders");
        orders.unshift(order);
        await save(env, "orders", orders);

        const ci = customers.findIndex((c) => c.id === customer.id);
        customers[ci] = { ...customers[ci], debt, credit };
        await save(env, "customers", customers);

        return json({ order, customer: customers[ci] }, 201);
      }

      // PATCH status: registered | preparing | shipped | delivered
      if (path.match(/^\/api\/orders\/[^/]+\/status$/) && request.method === "PATCH") {
        const id = path.split("/")[3];
        const body = await request.json();
        const status = body.status;
        const allowed = ["registered", "preparing", "shipped", "delivered"];
        if (!allowed.includes(status)) return json({ error: "invalid status" }, 400);

        const orders = await load(env, "orders");
        const i = orders.findIndex((o) => o.id === id);
        if (i < 0) return json({ error: "not found" }, 404);

        const now = Date.now();
        const o = { ...orders[i], status };
        if (status === "preparing" && !o.preparingAt) o.preparingAt = now;
        if (status === "shipped" && !o.shippedAt) o.shippedAt = now;
        if (status === "delivered" && !o.deliveredAt) o.deliveredAt = now;
        // customer confirms receive
        if (body.byCustomer) o.deliveredByCustomer = true;
        if (body.byKitchen) o.deliveredByKitchen = true;

        orders[i] = o;
        await save(env, "orders", orders);
        return json(o);
      }

      // New orders count (for kitchen alarm)
      if (path === "/api/orders/new-count" && request.method === "GET") {
        const since = Number(url.searchParams.get("since") || 0);
        const orders = await load(env, "orders");
        const list = orders.filter(
          (o) => o.status === "registered" && o.createdAt > since
        );
        return json({ count: list.length, orders: list });
      }

      // ---- Payments ----
      if (path === "/api/payments" && request.method === "POST") {
        if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
        const body = await request.json();
        const amount = Number(body.amount) || 0;
        if (amount <= 0) return json({ error: "invalid amount" }, 400);

        const customers = await load(env, "customers");
        const ci = customers.findIndex((c) => c.id === body.customerId);
        if (ci < 0) return json({ error: "customer not found" }, 404);

        let remainingPay = amount;
        const orders = await load(env, "orders");
        const open = orders
          .map((o, idx) => ({ o, idx }))
          .filter((x) => x.o.customerId === body.customerId && x.o.totalAmount - x.o.paidAmount > 0)
          .sort((a, b) => a.o.createdAt - b.o.createdAt);

        for (const { o, idx } of open) {
          if (remainingPay <= 0) break;
          const rem = o.totalAmount - o.paidAmount;
          const pay = Math.min(remainingPay, rem);
          orders[idx] = { ...o, paidAmount: o.paidAmount + pay };
          remainingPay -= pay;
        }
        await save(env, "orders", orders);

        const debt = orders
          .filter((o) => o.customerId === body.customerId)
          .reduce((s, o) => s + Math.max(0, o.totalAmount - o.paidAmount), 0);
        const credit = (customers[ci].credit || 0) + remainingPay;
        customers[ci] = { ...customers[ci], debt, credit };
        await save(env, "customers", customers);

        const payments = await load(env, "payments");
        const p = {
          id: uid(),
          customerId: body.customerId,
          amount,
          note: body.note || "تسویه",
          createdAt: Date.now(),
        };
        payments.unshift(p);
        await save(env, "payments", payments);

        return json({ payment: p, customer: customers[ci] });
      }

      // Health
      if (path === "/api/health") {
        return json({ ok: true, service: "reyhoon-api", ts: Date.now() });
      }

      return json({ error: "not found", path }, 404);
    } catch (e) {
      return json({ error: String(e.message || e) }, 500);
    }
  },
};

function adminHtml() {
  return `<!DOCTYPE html>
<html lang="fa" dir="rtl">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>پنل ادمین ریحون</title>
<style>
  :root { --g:#2E7D32; --g2:#1B5E20; --bg:#F1F8E9; --card:#fff; --t:#0A0A0A; --muted:#3D5C40; --err:#C62828; --ok:#2E7D32; }
  * { box-sizing:border-box; }
  body { margin:0; font-family:Tahoma,Segoe UI,sans-serif; background:var(--bg); color:var(--t); font-size:16px; }
  header { background:linear-gradient(135deg,var(--g2),var(--g)); color:#fff; padding:16px 20px; display:flex; flex-wrap:wrap; gap:12px; align-items:center; justify-content:space-between; }
  header h1 { margin:0; font-size:1.35rem; }
  .wrap { max-width:960px; margin:0 auto; padding:16px; }
  .card { background:var(--card); border-radius:14px; padding:16px; margin-bottom:16px; box-shadow:0 2px 10px rgba(0,0,0,.08); }
  label { display:block; font-weight:700; margin:8px 0 4px; }
  input, select, textarea { width:100%; padding:10px 12px; border:1px solid #c5d6c7; border-radius:10px; font-size:1rem; }
  button { cursor:pointer; border:none; border-radius:10px; padding:10px 16px; font-size:1rem; font-weight:700; }
  .btn { background:var(--g); color:#fff; }
  .btn:hover { background:var(--g2); }
  .btn-danger { background:var(--err); color:#fff; }
  .btn-outline { background:#fff; border:2px solid var(--g); color:var(--g); }
  .row { display:flex; flex-wrap:wrap; gap:10px; }
  .row > * { flex:1; min-width:140px; }
  table { width:100%; border-collapse:collapse; }
  th, td { text-align:right; padding:10px 8px; border-bottom:1px solid #e0e0e0; }
  th { background:#E8F5E9; font-weight:700; }
  .tabs { display:flex; gap:8px; margin-bottom:16px; flex-wrap:wrap; }
  .tab { background:#fff; border:2px solid var(--g); color:var(--g); }
  .tab.active { background:var(--g); color:#fff; }
  .msg { padding:10px; border-radius:8px; margin:8px 0; display:none; }
  .msg.ok { display:block; background:#E8F5E9; color:var(--ok); }
  .msg.err { display:block; background:#FFEBEE; color:var(--err); }
  .login-box { max-width:400px; margin:40px auto; }
  .price { color:#E65100; font-weight:700; }
  .badge { display:inline-block; padding:2px 8px; border-radius:8px; background:#E8F5E9; font-size:.85rem; }
</style>
</head>
<body>
<header>
  <h1>🌿 پنل ادمین آشپزخانه ریحون</h1>
  <span id="hdrStatus"></span>
</header>
<div class="wrap">
  <div id="loginView" class="card login-box">
    <h2>ورود ادمین</h2>
    <p style="color:var(--muted)">کلید ادمین را وارد کنید (پیش‌فرض: reyhoon-admin-2024)</p>
    <label>کلید ادمین</label>
    <input id="adminKey" type="password" placeholder="X-Admin-Key"/>
    <div style="margin-top:12px">
      <button class="btn" onclick="doLogin()">ورود</button>
    </div>
    <div id="loginMsg" class="msg"></div>
  </div>

  <div id="appView" style="display:none">
    <div class="tabs">
      <button class="tab active" data-tab="menu" onclick="showTab('menu')">منوی غذا</button>
      <button class="tab" data-tab="orders" onclick="showTab('orders')">سفارش‌ها</button>
      <button class="tab" data-tab="customers" onclick="showTab('customers')">مشتریان</button>
    </div>
    <div id="flash" class="msg"></div>

    <div id="tab-menu">
      <div class="card">
        <h3>افزودن / ویرایش غذا</h3>
        <input type="hidden" id="foodId"/>
        <div class="row">
          <div><label>نام غذا</label><input id="foodName"/></div>
          <div><label>قیمت (تومان)</label><input id="foodPrice" type="number"/></div>
        </div>
        <div class="row">
          <div><label>دسته</label><input id="foodCat" value="عمومی"/></div>
          <div><label>توضیح</label><input id="foodDesc"/></div>
        </div>
        <div style="margin-top:12px" class="row">
          <button class="btn" onclick="saveFood()">ذخیره در منو</button>
          <button class="btn-outline" onclick="clearFoodForm()">پاک کردن فرم</button>
        </div>
      </div>
      <div class="card">
        <h3>لیست منو <button class="btn-outline" style="float:left" onclick="loadMenu()">بروزرسانی</button></h3>
        <div style="overflow-x:auto">
          <table>
            <thead><tr><th>نام</th><th>قیمت</th><th>دسته</th><th>عملیات</th></tr></thead>
            <tbody id="menuBody"></tbody>
          </table>
        </div>
      </div>
    </div>

    <div id="tab-orders" style="display:none">
      <div class="card">
        <h3>سفارش‌ها <button class="btn-outline" style="float:left" onclick="loadOrders()">بروزرسانی</button></h3>
        <div id="ordersList"></div>
      </div>
    </div>

    <div id="tab-customers" style="display:none">
      <div class="card">
        <h3>افزودن مشتری</h3>
        <div class="row">
          <div><label>نام</label><input id="cName"/></div>
          <div><label>تلفن</label><input id="cPhone"/></div>
        </div>
        <div class="row">
          <div><label>کد اشتراک</label><input id="cCode"/></div>
          <div><label>آدرس</label><input id="cStreet"/></div>
        </div>
        <div class="row">
          <div><label>شهر</label><input id="cCity"/></div>
          <div style="display:flex;align-items:flex-end"><button class="btn" onclick="addCustomer()">ثبت مشتری</button></div>
        </div>
      </div>
      <div class="card">
        <h3>لیست مشتریان <button class="btn-outline" style="float:left" onclick="loadCustomers()">بروزرسانی</button></h3>
        <div id="custList"></div>
      </div>
    </div>
  </div>
</div>
<script>
const API = location.origin;
let KEY = localStorage.getItem('reyhoon_admin_key') || '';

function headers(json) {
  const h = { 'X-Admin-Key': KEY };
  if (json) h['Content-Type'] = 'application/json';
  return h;
}
function flash(msg, ok) {
  const el = document.getElementById('flash');
  el.className = 'msg ' + (ok ? 'ok' : 'err');
  el.textContent = msg;
  setTimeout(() => { el.className = 'msg'; }, 4000);
}
function fmt(n) { return Number(n||0).toLocaleString('fa-IR'); }
function fmtTime(ts) {
  if (!ts) return '—';
  return new Date(ts).toLocaleString('fa-IR');
}
const statusFa = { registered:'ثبت شد', preparing:'آماده‌سازی', shipped:'ارسال شده', delivered:'تحویل داده شد' };

async function doLogin() {
  KEY = document.getElementById('adminKey').value.trim();
  try {
    const r = await fetch(API + '/api/menu', { headers: headers() });
    if (!r.ok && r.status === 401) throw new Error('کلید اشتباه');
    // menu GET is public; test admin with POST empty is bad — test customers
    const r2 = await fetch(API + '/api/customers', { headers: headers() });
    if (r2.status === 401) throw new Error('کلید ادمین اشتباه است');
    localStorage.setItem('reyhoon_admin_key', KEY);
    document.getElementById('loginView').style.display = 'none';
    document.getElementById('appView').style.display = 'block';
    document.getElementById('hdrStatus').textContent = 'متصل ✓';
    loadMenu(); loadOrders(); loadCustomers();
  } catch (e) {
    const m = document.getElementById('loginMsg');
    m.className = 'msg err'; m.textContent = e.message;
  }
}

if (KEY) {
  document.getElementById('adminKey').value = KEY;
  doLogin();
}

function showTab(name) {
  ['menu','orders','customers'].forEach(t => {
    document.getElementById('tab-' + t).style.display = t === name ? 'block' : 'none';
  });
  document.querySelectorAll('.tab').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
}

async function loadMenu() {
  const r = await fetch(API + '/api/menu');
  const list = await r.json();
  const tb = document.getElementById('menuBody');
  tb.innerHTML = list.map(f => `<tr>
    <td><b>${esc(f.name)}</b><br/><small>${esc(f.description||'')}</small></td>
    <td class="price">${fmt(f.price)}</td>
    <td><span class="badge">${esc(f.category||'')}</span></td>
    <td>
      <button class="btn-outline" onclick='editFood(${JSON.stringify(f)})'>ویرایش</button>
      <button class="btn-danger" onclick="delFood('${f.id}')">حذف</button>
    </td>
  </tr>`).join('') || '<tr><td colspan="4">منو خالی است</td></tr>';
}
function esc(s) { return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/"/g,'&quot;'); }
function clearFoodForm() {
  foodId.value=''; foodName.value=''; foodPrice.value=''; foodCat.value='عمومی'; foodDesc.value='';
}
function editFood(f) {
  foodId.value=f.id; foodName.value=f.name; foodPrice.value=f.price; foodCat.value=f.category||'عمومی'; foodDesc.value=f.description||'';
  window.scrollTo(0,0);
}
async function saveFood() {
  const id = foodId.value;
  const body = { name: foodName.value.trim(), price: Number(foodPrice.value)||0, category: foodCat.value.trim()||'عمومی', description: foodDesc.value.trim(), isAvailable: true };
  if (!body.name) return flash('نام غذا لازم است', false);
  const r = await fetch(API + (id ? '/api/menu/' + id : '/api/menu'), {
    method: id ? 'PUT' : 'POST', headers: headers(true), body: JSON.stringify(body)
  });
  if (!r.ok) return flash('خطا در ذخیره', false);
  flash(id ? 'منو به‌روز شد — در هر دو اپ اعمال می‌شود' : 'غذا اضافه شد — در هر دو اپ دیده می‌شود', true);
  clearFoodForm(); loadMenu();
}
async function delFood(id) {
  if (!confirm('حذف این غذا؟')) return;
  await fetch(API + '/api/menu/' + id, { method: 'DELETE', headers: headers() });
  flash('حذف شد', true); loadMenu();
}

async function loadOrders() {
  const r = await fetch(API + '/api/orders');
  const list = await r.json();
  document.getElementById('ordersList').innerHTML = list.map(o => `
    <div style="border:1px solid #e0e0e0;border-radius:12px;padding:12px;margin-bottom:10px">
      <b>${esc(o.customerName)}</b> — <span class="badge">${statusFa[o.status]||o.status}</span><br/>
      <small>ثبت: ${fmtTime(o.createdAt)} ${o.deliveredAt ? ' | تحویل: '+fmtTime(o.deliveredAt) : ''}</small><br/>
      ${ (o.items||[]).map(i => esc(i.foodName)+' ×'+i.quantity).join('، ') }<br/>
      <span class="price">${fmt(o.totalAmount)} تومان</span>
      <div class="row" style="margin-top:8px">
        <button class="btn-outline" onclick="setStatus('${o.id}','preparing')">آماده‌سازی</button>
        <button class="btn-outline" onclick="setStatus('${o.id}','shipped')">ارسال شد</button>
        <button class="btn" onclick="setStatus('${o.id}','delivered')">تحویل داده شد</button>
      </div>
    </div>`).join('') || '<p>سفارشی نیست</p>';
}
async function setStatus(id, status) {
  await fetch(API + '/api/orders/' + id + '/status', {
    method: 'PATCH', headers: headers(true),
    body: JSON.stringify({ status, byKitchen: true })
  });
  flash('وضعیت به‌روز شد', true); loadOrders();
}

async function loadCustomers() {
  const r = await fetch(API + '/api/customers', { headers: headers() });
  if (!r.ok) return;
  const list = await r.json();
  document.getElementById('custList').innerHTML = list.map(c => `
    <div style="border-bottom:1px solid #eee;padding:10px 0">
      <b>${esc(c.name)}</b> — ${esc(c.phone)} — کد: <b>${esc(c.subscriptionCode||'—')}</b><br/>
      <small>${esc((c.address&&c.address.street)||'')} ${esc((c.address&&c.address.city)||'')}</small><br/>
      بدهی: <span style="color:var(--err)">${fmt(c.debt)}</span> | اعتبار: <span style="color:var(--ok)">${fmt(c.credit)}</span>
    </div>`).join('') || '<p>مشتری نیست</p>';
}
async function addCustomer() {
  const body = {
    name: cName.value.trim(), phone: cPhone.value.trim(),
    subscriptionCode: cCode.value.trim() || null,
    address: { street: cStreet.value.trim(), city: cCity.value.trim(), postalCode: '', notes: '' }
  };
  if (!body.name || !body.phone) return flash('نام و تلفن لازم است', false);
  const r = await fetch(API + '/api/customers', { method:'POST', headers: headers(true), body: JSON.stringify(body) });
  if (!r.ok) return flash('خطا', false);
  flash('مشتری اضافه شد', true);
  cName.value=cPhone.value=cCode.value=cStreet.value=cCity.value='';
  loadCustomers();
}
</script>
</body>
</html>`;
}
