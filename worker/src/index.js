/**
 * Reyhoon API - سازگار با ادیتور داشبورد کلادفلر (بدون export)
 */

var CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, X-Admin-Key"
};

var mem = { menu: [], customers: [], orders: [], payments: [] };

function json(data, status) {
  status = status || 200;
  return new Response(JSON.stringify(data), {
    status: status,
    headers: Object.assign({ "Content-Type": "application/json; charset=utf-8" }, CORS)
  });
}

function html(body) {
  return new Response(body, {
    status: 200,
    headers: Object.assign({ "Content-Type": "text/html; charset=utf-8" }, CORS)
  });
}

function getEnv() {
  return {
    STORE: typeof STORE !== "undefined" ? STORE : null,
    ADMIN_KEY: typeof ADMIN_KEY !== "undefined" ? ADMIN_KEY : "reyhoon-admin-2024"
  };
}

async function load(env, key) {
  if (env.STORE) {
    var v = await env.STORE.get(key, "json");
    if (v != null) return v;
    return [];
  }
  return mem[key] || [];
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
  var key = req.headers.get("X-Admin-Key") || "";
  return key && key === (env.ADMIN_KEY || "reyhoon-admin-2024");
}

addEventListener("fetch", function (event) {
  event.respondWith(handleRequest(event.request));
});

async function handleRequest(request) {
  var env = getEnv();

  if (request.method === "OPTIONS") {
    return new Response(null, { headers: CORS });
  }

  var url = new URL(request.url);
  var path = url.pathname.replace(/\/$/, "") || "/";

  try {
    if (path === "/" || path === "/admin") {
      return html(adminHtml());
    }

    if (path === "/api/health") {
      return json({ ok: true, service: "reyhoon-api", ts: Date.now() });
    }

    // Menu GET
    if (path === "/api/menu" && request.method === "GET") {
      return json(await load(env, "menu"));
    }

    // Menu POST
    if (path === "/api/menu" && request.method === "POST") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      var body = await request.json();
      var menu = await load(env, "menu");
      var item = {
        id: uid(),
        name: body.name || "",
        description: body.description || "",
        price: Number(body.price) || 0,
        category: body.category || "عمومی",
        isAvailable: body.isAvailable !== false
      };
      menu.push(item);
      await save(env, "menu", menu);
      return json(item, 201);
    }

    // Menu PUT
    if (path.indexOf("/api/menu/") === 0 && request.method === "PUT") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      var id = path.split("/").pop();
      body = await request.json();
      menu = await load(env, "menu");
      var i = menu.findIndex(function (x) { return x.id === id; });
      if (i < 0) return json({ error: "not found" }, 404);
      menu[i] = Object.assign({}, menu[i], body, { id: id });
      await save(env, "menu", menu);
      return json(menu[i]);
    }

    // Menu DELETE
    if (path.indexOf("/api/menu/") === 0 && request.method === "DELETE") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      id = path.split("/").pop();
      menu = await load(env, "menu");
      menu = menu.filter(function (x) { return x.id !== id; });
      await save(env, "menu", menu);
      return json({ ok: true });
    }

    // Customers GET
    if (path === "/api/customers" && request.method === "GET") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      return json(await load(env, "customers"));
    }

    // Customers POST
    if (path === "/api/customers" && request.method === "POST") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      body = await request.json();
      var customers = await load(env, "customers");
      var c = {
        id: uid(),
        name: body.name || "",
        phone: body.phone || "",
        address: body.address || { street: "", city: "", postalCode: "", notes: "" },
        subscriptionCode: body.subscriptionCode || null,
        debt: Number(body.debt) || 0,
        credit: Number(body.credit) || 0,
        notes: body.notes || "",
        createdAt: Date.now()
      };
      customers.push(c);
      await save(env, "customers", customers);
      return json(c, 201);
    }

    // Customer by code
    if (path.indexOf("/api/customers/code/") === 0 && request.method === "GET") {
      var code = decodeURIComponent(path.split("/").pop());
      customers = await load(env, "customers");
      c = customers.find(function (x) {
        return x.subscriptionCode && x.subscriptionCode.toLowerCase() === code.toLowerCase();
      });
      if (!c) return json({ error: "not found" }, 404);
      return json(c);
    }

    // Customer PUT
    if (path.indexOf("/api/customers/") === 0 && path.indexOf("/code/") < 0 && request.method === "PUT") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      id = path.split("/").pop();
      body = await request.json();
      customers = await load(env, "customers");
      i = customers.findIndex(function (x) { return x.id === id; });
      if (i < 0) return json({ error: "not found" }, 404);
      customers[i] = Object.assign({}, customers[i], body, { id: id });
      await save(env, "customers", customers);
      return json(customers[i]);
    }

    // Customer DELETE
    if (path.indexOf("/api/customers/") === 0 && path.indexOf("/code/") < 0 && request.method === "DELETE") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      id = path.split("/").pop();
      customers = await load(env, "customers");
      customers = customers.filter(function (x) { return x.id !== id; });
      await save(env, "customers", customers);
      return json({ ok: true });
    }

    // Orders GET
    if (path === "/api/orders" && request.method === "GET") {
      var customerId = url.searchParams.get("customerId");
      var statusFilter = url.searchParams.get("status");
      var orders = await load(env, "orders");
      if (customerId) orders = orders.filter(function (o) { return o.customerId === customerId; });
      if (statusFilter) orders = orders.filter(function (o) { return o.status === statusFilter; });
      orders.sort(function (a, b) { return b.createdAt - a.createdAt; });
      return json(orders);
    }

    // Orders POST
    if (path === "/api/orders" && request.method === "POST") {
      body = await request.json();
      customers = await load(env, "customers");
      var customer = customers.find(function (x) { return x.id === body.customerId; });
      if (!customer) return json({ error: "customer not found" }, 404);

      var items = body.items || [];
      var total = items.reduce(function (s, it) {
        return s + (Number(it.unitPrice) || 0) * (Number(it.quantity) || 1);
      }, 0);
      var credit = customer.credit || 0;
      var debt = customer.debt || 0;
      var creditApplied = Math.min(credit, total);
      credit -= creditApplied;
      var afterCredit = total - creditApplied;
      var paidNow = Math.max(0, Number(body.paidNow) || 0);
      var cashUsed = Math.min(paidNow, afterCredit);
      var overpay = Math.max(0, paidNow - afterCredit);
      if (overpay > 0) credit += overpay;
      var shortfall = afterCredit - cashUsed;
      if (shortfall > 0) debt += shortfall;

      var order = {
        id: uid(),
        customerId: customer.id,
        customerName: customer.name,
        customerPhone: customer.phone || "",
        items: items,
        totalAmount: total,
        paidAmount: creditApplied + cashUsed,
        creditApplied: creditApplied,
        status: "registered",
        createdAt: Date.now(),
        preparingAt: null,
        shippedAt: null,
        deliveredAt: null,
        note: body.note || ""
      };

      orders = await load(env, "orders");
      orders.unshift(order);
      await save(env, "orders", orders);

      var ci = customers.findIndex(function (x) { return x.id === customer.id; });
      customers[ci] = Object.assign({}, customers[ci], { debt: debt, credit: credit });
      await save(env, "customers", customers);

      return json({ order: order, customer: customers[ci] }, 201);
    }

    // Order status PATCH
    if (/^\/api\/orders\/[^/]+\/status$/.test(path) && request.method === "PATCH") {
      id = path.split("/")[3];
      body = await request.json();
      var status = body.status;
      var allowed = ["registered", "preparing", "shipped", "delivered"];
      if (allowed.indexOf(status) < 0) return json({ error: "invalid status" }, 400);

      orders = await load(env, "orders");
      i = orders.findIndex(function (o) { return o.id === id; });
      if (i < 0) return json({ error: "not found" }, 404);

      var now = Date.now();
      var o = Object.assign({}, orders[i], { status: status });
      if (status === "preparing" && !o.preparingAt) o.preparingAt = now;
      if (status === "shipped" && !o.shippedAt) o.shippedAt = now;
      if (status === "delivered" && !o.deliveredAt) o.deliveredAt = now;
      if (body.byCustomer) o.deliveredByCustomer = true;
      if (body.byKitchen) o.deliveredByKitchen = true;

      orders[i] = o;
      await save(env, "orders", orders);
      return json(o);
    }

    // New orders for alarm
    if (path === "/api/orders/new-count" && request.method === "GET") {
      var since = Number(url.searchParams.get("since") || 0);
      orders = await load(env, "orders");
      var list = orders.filter(function (o) {
        return o.status === "registered" && o.createdAt > since;
      });
      return json({ count: list.length, orders: list });
    }

    // Payments POST
    if (path === "/api/payments" && request.method === "POST") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      body = await request.json();
      var amount = Number(body.amount) || 0;
      if (amount <= 0) return json({ error: "invalid amount" }, 400);

      customers = await load(env, "customers");
      ci = customers.findIndex(function (x) { return x.id === body.customerId; });
      if (ci < 0) return json({ error: "customer not found" }, 404);

      var remainingPay = amount;
      orders = await load(env, "orders");
      var open = orders
        .map(function (ord, idx) { return { o: ord, idx: idx }; })
        .filter(function (x) {
          return x.o.customerId === body.customerId && x.o.totalAmount - x.o.paidAmount > 0;
        })
        .sort(function (a, b) { return a.o.createdAt - b.o.createdAt; });

      for (var k = 0; k < open.length; k++) {
        if (remainingPay <= 0) break;
        var rem = open[k].o.totalAmount - open[k].o.paidAmount;
        var pay = Math.min(remainingPay, rem);
        orders[open[k].idx] = Object.assign({}, open[k].o, {
          paidAmount: open[k].o.paidAmount + pay
        });
        remainingPay -= pay;
      }
      await save(env, "orders", orders);

      debt = orders
        .filter(function (ord) { return ord.customerId === body.customerId; })
        .reduce(function (s, ord) {
          return s + Math.max(0, ord.totalAmount - ord.paidAmount);
        }, 0);
      credit = (customers[ci].credit || 0) + remainingPay;
      customers[ci] = Object.assign({}, customers[ci], { debt: debt, credit: credit });
      await save(env, "customers", customers);

      var payments = await load(env, "payments");
      var p = {
        id: uid(),
        customerId: body.customerId,
        amount: amount,
        note: body.note || "تسویه",
        createdAt: Date.now()
      };
      payments.unshift(p);
      await save(env, "payments", payments);

      return json({ payment: p, customer: customers[ci] });
    }

    return json({ error: "not found", path: path }, 404);
  } catch (e) {
    return json({ error: String(e.message || e) }, 500);
  }
}

function adminHtml() {
  return "<!DOCTYPE html>\n" +
"<html lang=\"fa\" dir=\"rtl\">\n" +
"<head>\n" +
"<meta charset=\"utf-8\"/>\n" +
"<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>\n" +
"<title>پنل ادمین ریحون</title>\n" +
"<style>\n" +
":root{--g:#2E7D32;--g2:#1B5E20;--bg:#F1F8E9;--card:#fff;--t:#0A0A0A;--muted:#3D5C40;--err:#C62828;--ok:#2E7D32}\n" +
"*{box-sizing:border-box}body{margin:0;font-family:Tahoma,Segoe UI,sans-serif;background:var(--bg);color:var(--t);font-size:16px}\n" +
"header{background:linear-gradient(135deg,var(--g2),var(--g));color:#fff;padding:16px 20px}\n" +
"header h1{margin:0;font-size:1.35rem}.wrap{max-width:960px;margin:0 auto;padding:16px}\n" +
".card{background:var(--card);border-radius:14px;padding:16px;margin-bottom:16px;box-shadow:0 2px 10px rgba(0,0,0,.08)}\n" +
"label{display:block;font-weight:700;margin:8px 0 4px}\n" +
"input{width:100%;padding:10px 12px;border:1px solid #c5d6c7;border-radius:10px;font-size:1rem}\n" +
"button{cursor:pointer;border:none;border-radius:10px;padding:10px 16px;font-size:1rem;font-weight:700}\n" +
".btn{background:var(--g);color:#fff}.btn-danger{background:var(--err);color:#fff}\n" +
".btn-outline{background:#fff;border:2px solid var(--g);color:var(--g)}\n" +
".row{display:flex;flex-wrap:wrap;gap:10px}.row>*{flex:1;min-width:140px}\n" +
"table{width:100%;border-collapse:collapse}th,td{text-align:right;padding:10px 8px;border-bottom:1px solid #e0e0e0}\n" +
"th{background:#E8F5E9}.tabs{display:flex;gap:8px;margin-bottom:16px;flex-wrap:wrap}\n" +
".tab{background:#fff;border:2px solid var(--g);color:var(--g)}.tab.active{background:var(--g);color:#fff}\n" +
".msg{padding:10px;border-radius:8px;margin:8px 0;display:none}\n" +
".msg.ok{display:block;background:#E8F5E9;color:var(--ok)}.msg.err{display:block;background:#FFEBEE;color:var(--err)}\n" +
".login-box{max-width:400px;margin:40px auto}.price{color:#E65100;font-weight:700}\n" +
".badge{display:inline-block;padding:2px 8px;border-radius:8px;background:#E8F5E9;font-size:.85rem}\n" +
"</style></head><body>\n" +
"<header><h1>پنل ادمین آشپزخانه ریحون</h1><span id=\"hdrStatus\"></span></header>\n" +
"<div class=\"wrap\">\n" +
"<div id=\"loginView\" class=\"card login-box\">\n" +
"<h2>ورود ادمین</h2>\n" +
"<p style=\"color:var(--muted)\">کلید پیش‌فرض: reyhoon-admin-2024</p>\n" +
"<label>کلید ادمین</label>\n" +
"<input id=\"adminKey\" type=\"password\"/>\n" +
"<div style=\"margin-top:12px\"><button class=\"btn\" onclick=\"doLogin()\">ورود</button></div>\n" +
"<div id=\"loginMsg\" class=\"msg\"></div></div>\n" +
"<div id=\"appView\" style=\"display:none\">\n" +
"<div class=\"tabs\">\n" +
"<button class=\"tab active\" data-tab=\"menu\" onclick=\"showTab('menu')\">منوی غذا</button>\n" +
"<button class=\"tab\" data-tab=\"orders\" onclick=\"showTab('orders')\">سفارش‌ها</button>\n" +
"<button class=\"tab\" data-tab=\"customers\" onclick=\"showTab('customers')\">مشتریان</button>\n" +
"</div><div id=\"flash\" class=\"msg\"></div>\n" +
"<div id=\"tab-menu\">\n" +
"<div class=\"card\"><h3>افزودن / ویرایش غذا</h3>\n" +
"<input type=\"hidden\" id=\"foodId\"/>\n" +
"<div class=\"row\"><div><label>نام غذا</label><input id=\"foodName\"/></div>\n" +
"<div><label>قیمت (تومان)</label><input id=\"foodPrice\" type=\"number\"/></div></div>\n" +
"<div class=\"row\"><div><label>دسته</label><input id=\"foodCat\" value=\"عمومی\"/></div>\n" +
"<div><label>توضیح</label><input id=\"foodDesc\"/></div></div>\n" +
"<div style=\"margin-top:12px\" class=\"row\">\n" +
"<button class=\"btn\" onclick=\"saveFood()\">ذخیره در منو</button>\n" +
"<button class=\"btn-outline\" onclick=\"clearFoodForm()\">پاک کردن فرم</button></div></div>\n" +
"<div class=\"card\"><h3>لیست منو <button class=\"btn-outline\" style=\"float:left\" onclick=\"loadMenu()\">بروزرسانی</button></h3>\n" +
"<div style=\"overflow-x:auto\"><table><thead><tr><th>نام</th><th>قیمت</th><th>دسته</th><th>عملیات</th></tr></thead>\n" +
"<tbody id=\"menuBody\"></tbody></table></div></div></div>\n" +
"<div id=\"tab-orders\" style=\"display:none\"><div class=\"card\">\n" +
"<h3>سفارش‌ها <button class=\"btn-outline\" style=\"float:left\" onclick=\"loadOrders()\">بروزرسانی</button></h3>\n" +
"<div id=\"ordersList\"></div></div></div>\n" +
"<div id=\"tab-customers\" style=\"display:none\">\n" +
"<div class=\"card\"><h3>افزودن مشتری</h3>\n" +
"<div class=\"row\"><div><label>نام</label><input id=\"cName\"/></div><div><label>تلفن</label><input id=\"cPhone\"/></div></div>\n" +
"<div class=\"row\"><div><label>کد اشتراک</label><input id=\"cCode\"/></div><div><label>آدرس</label><input id=\"cStreet\"/></div></div>\n" +
"<div class=\"row\"><div><label>شهر</label><input id=\"cCity\"/></div>\n" +
"<div style=\"display:flex;align-items:flex-end\"><button class=\"btn\" onclick=\"addCustomer()\">ثبت مشتری</button></div></div></div>\n" +
"<div class=\"card\"><h3>لیست مشتریان <button class=\"btn-outline\" style=\"float:left\" onclick=\"loadCustomers()\">بروزرسانی</button></h3>\n" +
"<div id=\"custList\"></div></div></div></div></div>\n" +
"<script>\n" +
"var API=location.origin;var KEY=localStorage.getItem('reyhoon_admin_key')||'';\n" +
"function headers(j){var h={'X-Admin-Key':KEY};if(j)h['Content-Type']='application/json';return h}\n" +
"function flash(m,ok){var el=document.getElementById('flash');el.className='msg '+(ok?'ok':'err');el.textContent=m;setTimeout(function(){el.className='msg'},4000)}\n" +
"function fmt(n){return Number(n||0).toLocaleString('fa-IR')}\n" +
"function fmtTime(ts){if(!ts)return '—';return new Date(ts).toLocaleString('fa-IR')}\n" +
"var statusFa={registered:'ثبت شد',preparing:'آماده\\u200cسازی',shipped:'ارسال شده',delivered:'تحویل داده شد'};\n" +
"async function doLogin(){KEY=document.getElementById('adminKey').value.trim();try{var r2=await fetch(API+'/api/customers',{headers:headers()});if(r2.status===401)throw new Error('کلید اشتباه');localStorage.setItem('reyhoon_admin_key',KEY);document.getElementById('loginView').style.display='none';document.getElementById('appView').style.display='block';document.getElementById('hdrStatus').textContent='متصل';loadMenu();loadOrders();loadCustomers()}catch(e){var m=document.getElementById('loginMsg');m.className='msg err';m.textContent=e.message}}\n" +
"if(KEY){document.getElementById('adminKey').value=KEY;doLogin()}\n" +
"function showTab(name){['menu','orders','customers'].forEach(function(t){document.getElementById('tab-'+t).style.display=t===name?'block':'none'});document.querySelectorAll('.tab').forEach(function(b){b.classList.toggle('active',b.dataset.tab===name)})}\n" +
"function esc(s){return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/\"/g,'&quot;')}\n" +
"async function loadMenu(){var r=await fetch(API+'/api/menu');var list=await r.json();var tb=document.getElementById('menuBody');tb.innerHTML=list.map(function(f){return '<tr><td><b>'+esc(f.name)+'</b></td><td class=\"price\">'+fmt(f.price)+'</td><td><span class=\"badge\">'+esc(f.category||'')+'</span></td><td><button class=\"btn-outline\" onclick=\'editFood('+JSON.stringify(f)+')\'>ویرایش</button> <button class=\"btn-danger\" onclick=\"delFood(\\''+f.id+'\\')\">حذف</button></td></tr>'}).join('')||'<tr><td colspan=\"4\">منو خالی</td></tr>'}\n" +
"function clearFoodForm(){foodId.value='';foodName.value='';foodPrice.value='';foodCat.value='عمومی';foodDesc.value=''}\n" +
"function editFood(f){foodId.value=f.id;foodName.value=f.name;foodPrice.value=f.price;foodCat.value=f.category||'عمومی';foodDesc.value=f.description||''}\n" +
"async function saveFood(){var id=foodId.value;var body={name:foodName.value.trim(),price:Number(foodPrice.value)||0,category:foodCat.value.trim()||'عمومی',description:foodDesc.value.trim(),isAvailable:true};if(!body.name)return flash('نام لازم است',false);var r=await fetch(API+(id?'/api/menu/'+id:'/api/menu'),{method:id?'PUT':'POST',headers:headers(true),body:JSON.stringify(body)});if(!r.ok)return flash('خطا',false);flash('ذخیره شد — در اپ‌ها اعمال می‌شود',true);clearFoodForm();loadMenu()}\n" +
"async function delFood(id){if(!confirm('حذف؟'))return;await fetch(API+'/api/menu/'+id,{method:'DELETE',headers:headers()});flash('حذف شد',true);loadMenu()}\n" +
"async function loadOrders(){var r=await fetch(API+'/api/orders');var list=await r.json();document.getElementById('ordersList').innerHTML=list.map(function(o){return '<div style=\"border:1px solid #e0e0e0;border-radius:12px;padding:12px;margin-bottom:10px\"><b>'+esc(o.customerName)+'</b> — <span class=\"badge\">'+(statusFa[o.status]||o.status)+'</span><br/><small>ثبت: '+fmtTime(o.createdAt)+(o.deliveredAt?' | تحویل: '+fmtTime(o.deliveredAt):'')+'</small><br/>'+(o.items||[]).map(function(i){return esc(i.foodName)+' ×'+i.quantity}).join('، ')+'<br/><span class=\"price\">'+fmt(o.totalAmount)+' تومان</span><div class=\"row\" style=\"margin-top:8px\"><button class=\"btn-outline\" onclick=\"setStatus(\\''+o.id+'\\',\\'preparing\\')\">آماده\\u200cسازی</button><button class=\"btn-outline\" onclick=\"setStatus(\\''+o.id+'\\',\\'shipped\\')\">ارسال</button><button class=\"btn\" onclick=\"setStatus(\\''+o.id+'\\',\\'delivered\\')\">تحویل</button></div></div>'}).join('')||'<p>سفارشی نیست</p>'}\n" +
"async function setStatus(id,status){await fetch(API+'/api/orders/'+id+'/status',{method:'PATCH',headers:headers(true),body:JSON.stringify({status:status,byKitchen:true})});flash('وضعیت به‌روز شد',true);loadOrders()}\n" +
"async function loadCustomers(){var r=await fetch(API+'/api/customers',{headers:headers()});if(!r.ok)return;var list=await r.json();document.getElementById('custList').innerHTML=list.map(function(c){return '<div style=\"border-bottom:1px solid #eee;padding:10px 0\"><b>'+esc(c.name)+'</b> — '+esc(c.phone)+' — کد: <b>'+esc(c.subscriptionCode||'—')+'</b><br/><small>'+esc((c.address&&c.address.street)||'')+' '+esc((c.address&&c.address.city)||'')+'</small><br/>بدهی: <span style=\"color:var(--err)\">'+fmt(c.debt)+'</span> | اعتبار: <span style=\"color:var(--ok)\">'+fmt(c.credit)+'</span></div>'}).join('')||'<p>مشتری نیست</p>'}\n" +
"async function addCustomer(){var body={name:cName.value.trim(),phone:cPhone.value.trim(),subscriptionCode:cCode.value.trim()||null,address:{street:cStreet.value.trim(),city:cCity.value.trim(),postalCode:'',notes:''}};if(!body.name||!body.phone)return flash('نام و تلفن لازم است',false);var r=await fetch(API+'/api/customers',{method:'POST',headers:headers(true),body:JSON.stringify(body)});if(!r.ok)return flash('خطا',false);flash('مشتری اضافه شد',true);cName.value=cPhone.value=cCode.value=cStreet.value=cCity.value='';loadCustomers()}\n" +
"</scr"+"ipt></body></html>";
}
