/**
 * Reyhoon API — کد اشتراک فقط رقم (۶ رقمی)
 */

var CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET,POST,PUT,PATCH,DELETE,OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, X-Admin-Key"
};

var mem = { menu: [], customers: [], orders: [], payments: [], ratings: [] };

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

/** کد اشتراک فقط رقم — ۶ رقمی مثل 482917 */
function genCode() {
  var n = Math.floor(100000 + Math.random() * 900000);
  return String(n);
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
      return html(menuAdminHtml());
    }

    if (path === "/api/health") {
      return json({ ok: true, service: "reyhoon-api", ts: Date.now() });
    }

    if (path === "/api/menu" && request.method === "GET") {
      return json(await load(env, "menu"));
    }

    if (path === "/api/menu" && request.method === "POST") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      var body = await request.json();
      var menu = await load(env, "menu");
      var item = {
        id: uid(),
        name: body.name || "",
        description: body.description || "",
        price: Number(body.price) || 0,
        category: body.category || "\u0639\u0645\u0648\u0645\u06cc",
        isAvailable: body.isAvailable !== false
      };
      menu.push(item);
      await save(env, "menu", menu);
      return json(item, 201);
    }

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

    if (path.indexOf("/api/menu/") === 0 && request.method === "DELETE") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      id = path.split("/").pop();
      menu = await load(env, "menu");
      menu = menu.filter(function (x) { return x.id !== id; });
      await save(env, "menu", menu);
      return json({ ok: true });
    }

    if (path === "/api/customers/register" && request.method === "POST") {
      body = await request.json();
      var name = (body.name || "").trim();
      var phone = (body.phone || "").trim();
      if (!name || !phone) return json({ error: "name and phone required" }, 400);

      var customers = await load(env, "customers");
      var existing = customers.find(function (x) { return x.phone === phone; });
      if (existing) {
        return json({ customer: existing, isNew: false, message: "already_registered" });
      }

      var code = genCode();
      while (customers.some(function (x) { return x.subscriptionCode === code; })) {
        code = genCode();
      }

      var c = {
        id: uid(),
        name: name,
        phone: phone,
        address: body.address || { street: "", city: "", postalCode: "", notes: "" },
        subscriptionCode: code,
        debt: 0,
        credit: 0,
        notes: "",
        createdAt: Date.now(),
        isSelfRegistered: true
      };
      customers.push(c);
      await save(env, "customers", customers);
      return json({ customer: c, isNew: true, message: "registered" }, 201);
    }

    if (path === "/api/customers" && request.method === "POST") {
      body = await request.json();
      customers = await load(env, "customers");
      var rawCode = (body.subscriptionCode || "").trim();
      // فقط رقم؛ اگر خالی یا حروف داشت کد جدید بساز
      code = /^\d{4,10}$/.test(rawCode) ? rawCode : genCode();
      while (customers.some(function (x) { return x.subscriptionCode === code; })) {
        code = genCode();
      }
      c = {
        id: uid(),
        name: (body.name || "").trim(),
        phone: (body.phone || "").trim(),
        address: body.address || { street: "", city: "", postalCode: "", notes: "" },
        subscriptionCode: code,
        debt: Number(body.debt) || 0,
        credit: Number(body.credit) || 0,
        notes: body.notes || "",
        createdAt: Date.now(),
        isSelfRegistered: false
      };
      if (!c.name || !c.phone) return json({ error: "name and phone required" }, 400);
      customers.push(c);
      await save(env, "customers", customers);
      return json(c, 201);
    }

    if (path === "/api/customers" && request.method === "GET") {
      return json(await load(env, "customers"));
    }

    if (path.indexOf("/api/customers/code/") === 0 && request.method === "GET") {
      var q = decodeURIComponent(path.split("/").pop());
      customers = await load(env, "customers");
      c = customers.find(function (x) {
        return x.subscriptionCode && String(x.subscriptionCode) === String(q);
      });
      if (!c) return json({ error: "not found" }, 404);
      return json(c);
    }

    if (path.indexOf("/api/customers/") === 0 && path.indexOf("/code/") < 0 && request.method === "PUT") {
      id = path.split("/").pop();
      body = await request.json();
      customers = await load(env, "customers");
      i = customers.findIndex(function (x) { return x.id === id; });
      if (i < 0) return json({ error: "not found" }, 404);
      customers[i] = Object.assign({}, customers[i], body, { id: id });
      await save(env, "customers", customers);
      return json(customers[i]);
    }

    if (path === "/api/orders" && request.method === "GET") {
      var customerId = url.searchParams.get("customerId");
      var statusFilter = url.searchParams.get("status");
      var orders = await load(env, "orders");
      if (customerId) orders = orders.filter(function (o) { return o.customerId === customerId; });
      if (statusFilter) orders = orders.filter(function (o) { return o.status === statusFilter; });
      orders.sort(function (a, b) { return b.createdAt - a.createdAt; });
      return json(orders);
    }

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
        customerCode: customer.subscriptionCode || "",
        isNewCustomer: !!customer.isSelfRegistered && !customer._orderedBefore,
        items: items,
        totalAmount: total,
        paidAmount: creditApplied + cashUsed,
        creditApplied: creditApplied,
        status: "registered",
        source: body.source || "online",
        createdAt: Date.now(),
        preparingAt: null,
        shippedAt: null,
        deliveredAt: null,
        note: body.note || "",
        rated: false
      };

      orders = await load(env, "orders");
      orders.unshift(order);
      await save(env, "orders", orders);

      var ci = customers.findIndex(function (x) { return x.id === customer.id; });
      customers[ci] = Object.assign({}, customers[ci], {
        debt: debt,
        credit: credit,
        _orderedBefore: true
      });
      await save(env, "customers", customers);

      return json({ order: order, customer: customers[ci] }, 201);
    }

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

    if (path === "/api/orders/new-count" && request.method === "GET") {
      var since = Number(url.searchParams.get("since") || 0);
      orders = await load(env, "orders");
      var list = orders.filter(function (o) {
        return o.status === "registered" && o.createdAt > since;
      });
      return json({ count: list.length, orders: list });
    }

    if (path === "/api/ratings" && request.method === "POST") {
      body = await request.json();
      var rating = Number(body.rating) || 0;
      if (rating < 1 || rating > 5) return json({ error: "rating 1-5" }, 400);
      if (!body.orderId) return json({ error: "orderId required" }, 400);

      orders = await load(env, "orders");
      var oi = orders.findIndex(function (x) { return x.id === body.orderId; });
      if (oi < 0) return json({ error: "order not found" }, 404);
      if (orders[oi].status !== "delivered") {
        return json({ error: "order not delivered yet" }, 400);
      }

      var ratings = await load(env, "ratings");
      if (ratings.some(function (r) { return r.orderId === body.orderId; })) {
        return json({ error: "already rated" }, 400);
      }

      var r = {
        id: uid(),
        orderId: body.orderId,
        customerId: orders[oi].customerId,
        customerName: orders[oi].customerName,
        rating: rating,
        comment: (body.comment || "").trim(),
        createdAt: Date.now()
      };
      ratings.unshift(r);
      await save(env, "ratings", ratings);

      orders[oi] = Object.assign({}, orders[oi], { rated: true });
      await save(env, "orders", orders);

      return json(r, 201);
    }

    if (path === "/api/ratings" && request.method === "GET") {
      ratings = await load(env, "ratings");
      ratings.sort(function (a, b) { return b.createdAt - a.createdAt; });
      var avg = ratings.length
        ? ratings.reduce(function (s, x) { return s + x.rating; }, 0) / ratings.length
        : 0;
      return json({ ratings: ratings, average: Math.round(avg * 10) / 10, count: ratings.length });
    }

    if (path === "/api/payments" && request.method === "POST") {
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
        note: body.note || "",
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

function menuAdminHtml() {
  return "<!DOCTYPE html><html lang=\"fa\" dir=\"rtl\"><head><meta charset=\"utf-8\"/>" +
    "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>" +
    "<title>\u0645\u062f\u06cc\u0631\u06cc\u062a \u0645\u0646\u0648 \u0631\u06cc\u062d\u0648\u0646</title>" +
    "<style>" +
    "body{margin:0;font-family:Tahoma,sans-serif;background:#F1F8E9;color:#0A0A0A;font-size:16px}" +
    "header{background:#1B5E20;color:#fff;padding:16px 20px}h1{margin:0;font-size:1.3rem}" +
    ".wrap{max-width:720px;margin:0 auto;padding:16px}.card{background:#fff;border-radius:14px;padding:16px;margin-bottom:14px;box-shadow:0 2px 8px rgba(0,0,0,.08)}" +
    "label{display:block;font-weight:700;margin:8px 0 4px}input{width:100%;padding:10px;border:1px solid #c5d6c7;border-radius:10px;font-size:1rem;box-sizing:border-box}" +
    "button{cursor:pointer;border:none;border-radius:10px;padding:10px 16px;font-weight:700;font-size:1rem}" +
    ".btn{background:#2E7D32;color:#fff}.btn-danger{background:#C62828;color:#fff}.btn-outline{background:#fff;border:2px solid #2E7D32;color:#2E7D32}" +
    ".row{display:flex;flex-wrap:wrap;gap:10px}.row>*{flex:1;min-width:120px}" +
    "table{width:100%;border-collapse:collapse}th,td{padding:10px 6px;border-bottom:1px solid #eee;text-align:right}" +
    "th{background:#E8F5E9}.price{color:#E65100;font-weight:700}.msg{padding:10px;border-radius:8px;margin:8px 0;display:none}" +
    ".msg.ok{display:block;background:#E8F5E9;color:#2E7D32}.msg.err{display:block;background:#FFEBEE;color:#C62828}" +
    "</style></head><body>" +
    "<header><h1>\u0645\u062f\u06cc\u0631\u06cc\u062a \u0645\u0646\u0648 \u0648 \u0642\u06cc\u0645\u062a \u2014 \u0631\u06cc\u062d\u0648\u0646</h1></header>" +
    "<div class=\"wrap\">" +
    "<div class=\"card\" id=\"loginBox\">" +
    "<p>\u06a9\u0644\u06cc\u062f: <b>reyhoon-admin-2024</b></p>" +
    "<input id=\"adminKey\" type=\"password\"/>" +
    "<div style=\"margin-top:10px\"><button class=\"btn\" onclick=\"unlock()\">\u0648\u0631\u0648\u062f</button></div>" +
    "<div id=\"loginMsg\" class=\"msg\"></div></div>" +
    "<div id=\"app\" style=\"display:none\">" +
    "<div id=\"flash\" class=\"msg\"></div>" +
    "<div class=\"card\"><h3>\u0627\u0641\u0632\u0648\u062f\u0646 / \u0648\u06cc\u0631\u0627\u06cc\u0634 \u063a\u0630\u0627</h3>" +
    "<input type=\"hidden\" id=\"foodId\"/>" +
    "<div class=\"row\"><div><label>\u0646\u0627\u0645</label><input id=\"foodName\"/></div>" +
    "<div><label>\u0642\u06cc\u0645\u062a</label><input id=\"foodPrice\" type=\"number\"/></div></div>" +
    "<div class=\"row\"><div><label>\u062f\u0633\u062a\u0647</label><input id=\"foodCat\" value=\"\u0639\u0645\u0648\u0645\u06cc\"/></div>" +
    "<div><label>\u062a\u0648\u0636\u06cc\u062d</label><input id=\"foodDesc\"/></div></div>" +
    "<div style=\"margin-top:12px\" class=\"row\">" +
    "<button class=\"btn\" onclick=\"saveFood()\">\u0630\u062e\u06cc\u0631\u0647</button>" +
    "<button class=\"btn-outline\" onclick=\"clearForm()\">\u067e\u0627\u06a9</button></div></div>" +
    "<div class=\"card\"><h3>\u0644\u06cc\u0633\u062a \u0645\u0646\u0648 <button class=\"btn-outline\" style=\"float:left\" onclick=\"loadMenu()\">\u0628\u0631\u0648\u0632</button></h3>" +
    "<table><thead><tr><th>\u0646\u0627\u0645</th><th>\u0642\u06cc\u0645\u062a</th><th></th></tr></thead><tbody id=\"tb\"></tbody></table>" +
    "</div></div></div>" +
    "<script>" +
    "var API=location.origin;var KEY=localStorage.getItem('rk')||'';" +
    "function H(j){var h={'X-Admin-Key':KEY};if(j)h['Content-Type']='application/json';return h}" +
    "function flash(m,ok){var e=document.getElementById('flash');e.className='msg '+(ok?'ok':'err');e.textContent=m;setTimeout(function(){e.className='msg'},3500)}" +
    "function fmt(n){return Number(n||0).toLocaleString('fa-IR')}" +
    "async function unlock(){KEY=document.getElementById('adminKey').value.trim();" +
    "var r=await fetch(API+'/api/menu',{method:'POST',headers:H(true),body:JSON.stringify({name:'__ping__',price:0})});" +
    "if(r.status===401){document.getElementById('loginMsg').className='msg err';document.getElementById('loginMsg').textContent='\u06a9\u0644\u06cc\u062f \u0627\u0634\u062a\u0628\u0627\u0647';return}" +
    "if(r.ok){var it=await r.json();await fetch(API+'/api/menu/'+it.id,{method:'DELETE',headers:H()});}" +
    "localStorage.setItem('rk',KEY);document.getElementById('loginBox').style.display='none';document.getElementById('app').style.display='block';loadMenu()}" +
    "if(KEY){document.getElementById('adminKey').value=KEY;unlock()}" +
    "function clearForm(){foodId.value='';foodName.value='';foodPrice.value='';foodCat.value='\u0639\u0645\u0648\u0645\u06cc';foodDesc.value=''}" +
    "function editF(f){foodId.value=f.id;foodName.value=f.name;foodPrice.value=f.price;foodCat.value=f.category||'';foodDesc.value=f.description||''}" +
    "async function loadMenu(){var r=await fetch(API+'/api/menu');var list=await r.json();" +
    "document.getElementById('tb').innerHTML=list.filter(function(f){return f.name!=='__ping__'}).map(function(f){" +
    "return '<tr><td><b>'+f.name+'</b></td><td class=\"price\">'+fmt(f.price)+'</td><td>'+" +
    "'<button class=\"btn-outline\" onclick=\'editF('+JSON.stringify(f)+')\'>\u0648\u06cc\u0631\u0627\u06cc\u0634</button> '+" +
    "'<button class=\"btn-danger\" onclick=\"delF(\\''+f.id+'\\')\">\u062d\u0630\u0641</button></td></tr>"}).join('')||'<tr><td colspan=3>\u062e\u0627\u0644\u06cc</td></tr>'}" +
    "async function saveFood(){var id=foodId.value;var body={name:foodName.value.trim(),price:Number(foodPrice.value)||0,category:foodCat.value.trim(),description:foodDesc.value.trim(),isAvailable:true};" +
    "if(!body.name)return flash('\u0646\u0627\u0645 \u0644\u0627\u0632\u0645',false);" +
    "var r=await fetch(API+(id?'/api/menu/'+id:'/api/menu'),{method:id?'PUT':'POST',headers:H(true),body:JSON.stringify(body)});" +
    "if(!r.ok)return flash('\u062e\u0637\u0627',false);flash('\u0630\u062e\u06cc\u0631\u0647 \u0634\u062f',true);clearForm();loadMenu()}" +
    "async function delF(id){if(!confirm('?'))return;await fetch(API+'/api/menu/'+id,{method:'DELETE',headers:H()});loadMenu()}" +
    "</scr"+"ipt></body></html>";
}
