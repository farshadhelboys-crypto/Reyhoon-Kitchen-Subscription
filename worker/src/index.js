/**
 * Reyhoon API — STORE KV + cancel + priceTier + customerLat/Lng
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

async function load(env, key) {
  if (env && env.STORE) {
    try {
      var v = await env.STORE.get(key, "json");
      if (v != null) return v;
    } catch (e) {}
    return [];
  }
  return mem[key] || [];
}
async function save(env, key, value) {
  if (env && env.STORE) {
    try { await env.STORE.put(key, JSON.stringify(value)); return; } catch (e) {}
  }
  mem[key] = value;
}
function uid() {
  try { return crypto.randomUUID(); } catch (e) {
    return Date.now().toString(36) + Math.random().toString(36).slice(2, 10);
  }
}
function genCode() {
  return String(Math.floor(100000 + Math.random() * 900000));
}
function isAdmin(req, env) {
  var key = (req.headers.get("X-Admin-Key") || "").trim();
  var expected = ((env && env.ADMIN_KEY) || "reyhoon-admin-2024").trim();
  return key.length > 0 && key === expected;
}
function calcDebtFromOrders(orders, customerId) {
  return orders
    .filter(function (o) { return o.customerId === customerId; })
    .reduce(function (s, o) {
      return s + Math.max(0, (Number(o.totalAmount) || 0) - (Number(o.paidAmount) || 0));
    }, 0);
}

async function handleRequest(request, env) {
  env = env || {};
  if (!env.ADMIN_KEY) env.ADMIN_KEY = "reyhoon-admin-2024";
  if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: CORS });
  var url = new URL(request.url);
  var path = url.pathname.replace(/\/$/, "") || "/";
  try {
    if (path === "/" || path === "/admin") return html(menuAdminHtml());
    if (path === "/api/health") return json({ ok: true, service: "reyhoon-api", ts: Date.now() });

    if (path === "/api/menu" && request.method === "GET") return json(await load(env, "menu"));
    if (path === "/api/menu" && request.method === "POST") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      var body = await request.json();
      var menu = await load(env, "menu");
      var item = {
        id: uid(), name: body.name || "", description: body.description || "",
        price: Number(body.price) || 0, category: body.category || "عمومی",
        extraSkewerPrice: Number(body.extraSkewerPrice) || 0,
        priceTier: (body.priceTier === "economy") ? "economy" : "regular",
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

    if (path === "/api/customers" && request.method === "GET") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      return json(await load(env, "customers"));
    }
    if (path.indexOf("/api/customers/code/") === 0 && request.method === "GET") {
      var code = decodeURIComponent(path.split("/").pop());
      var customers = await load(env, "customers");
      var found = customers.find(function (c) { return (c.subscriptionCode || "") === code; });
      if (!found) return json({ error: "not found" }, 404);
      var orders = await load(env, "orders");
      found.debt = calcDebtFromOrders(orders, found.id);
      return json(found);
    }
    if (path === "/api/customers/register" && request.method === "POST") {
      body = await request.json();
      var name = (body.name || "").trim();
      var phone = (body.phone || "").trim();
      if (!name || phone.length < 10) return json({ error: "invalid" }, 400);
      customers = await load(env, "customers");
      var newCode = genCode();
      while (customers.some(function (c) { return c.subscriptionCode === newCode; })) newCode = genCode();
      var addr = body.address || { street: "", city: "" };
      var cust = {
        id: uid(), name: name, phone: phone,
        address: {
          street: addr.street || "", city: addr.city || "",
          lat: (addr.lat != null) ? Number(addr.lat) : null,
          lng: (addr.lng != null) ? Number(addr.lng) : null
        },
        subscriptionCode: newCode, debt: 0, credit: 0,
        notes: "", createdAt: Date.now(), isSelfRegistered: true
      };
      customers.push(cust);
      await save(env, "customers", customers);
      return json({ customer: cust }, 201);
    }
    if (path === "/api/customers" && request.method === "POST") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      body = await request.json();
      customers = await load(env, "customers");
      var rawCode = (body.subscriptionCode || "").trim();
      code = rawCode || genCode();
      var priorDebt = Math.max(0, Number(body.debt) || 0);
      addr = body.address || { street: "", city: "", postalCode: "", notes: "" };
      cust = {
        id: uid(), name: (body.name || "").trim(), phone: (body.phone || "").trim(),
        address: {
          street: addr.street || "", city: addr.city || "",
          postalCode: addr.postalCode || "", notes: addr.notes || "",
          lat: (addr.lat != null) ? Number(addr.lat) : null,
          lng: (addr.lng != null) ? Number(addr.lng) : null
        },
        subscriptionCode: code, debt: priorDebt, credit: Number(body.credit) || 0,
        notes: body.notes || "", createdAt: Date.now(), isSelfRegistered: false
      };
      customers.push(cust);
      await save(env, "customers", customers);
      if (priorDebt > 0) {
        orders = await load(env, "orders");
        orders.unshift({
          id: uid(), customerId: cust.id, customerName: cust.name,
          customerPhone: cust.phone, customerAddress: "",
          items: [{ foodId: "prior_debt", foodName: "بدهی قبلی", unitPrice: priorDebt, quantity: 1 }],
          totalAmount: priorDebt, paidAmount: 0, creditApplied: 0,
          status: "delivered", source: "prior_debt", createdAt: Date.now(),
          note: "بدهی قبلی هنگام ثبت مشتری", rated: false
        });
        await save(env, "orders", orders);
      }
      return json(cust, 201);
    }
    if (path.indexOf("/api/customers/") === 0 && request.method === "DELETE") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      id = path.split("/").pop();
      customers = await load(env, "customers");
      customers = customers.filter(function (c) { return c.id !== id; });
      await save(env, "customers", customers);
      return json({ ok: true });
    }

    if (path === "/api/orders" && request.method === "GET") {
      orders = await load(env, "orders");
      var cid = url.searchParams.get("customerId");
      if (cid) orders = orders.filter(function (o) { return o.customerId === cid; });
      return json(orders);
    }
    if (path === "/api/orders/new-count" && request.method === "GET") {
      var since = Number(url.searchParams.get("since") || 0);
      orders = await load(env, "orders");
      var fresh = orders.filter(function (o) {
        return (o.createdAt || 0) > since && (o.source === "online" || !o.source);
      });
      return json({ count: fresh.length, orders: fresh });
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
      var paidNow = Math.max(0, Number(body.paidNow) || 0);
      var credit = Number(customer.credit) || 0;
      var creditUsed = Math.min(credit, total);
      var afterCredit = total - creditUsed;
      var cashUsed = Math.min(paidNow, afterCredit);
      var overpay = Math.max(0, paidNow - afterCredit);
      customer.credit = credit - creditUsed + overpay;
      var order = {
        id: uid(), customerId: customer.id, customerName: customer.name,
        customerPhone: customer.phone || "",
        customerAddress: customer.address
          ? [customer.address.street, customer.address.city].filter(Boolean).join(" - ")
          : "",
        customerLat: (customer.address && customer.address.lat != null) ? Number(customer.address.lat) : null,
        customerLng: (customer.address && customer.address.lng != null) ? Number(customer.address.lng) : null,
        items: items, totalAmount: total,
        paidAmount: cashUsed + creditUsed,
        creditApplied: creditUsed,
        status: "registered",
        source: body.source || "online", createdAt: Date.now(),
        note: body.note || "", rated: false
      };
      orders = await load(env, "orders");
      orders.unshift(order);
      await save(env, "orders", orders);
      var ci = customers.findIndex(function (x) { return x.id === customer.id; });
      if (ci >= 0) {
        customers[ci].credit = customer.credit;
        customers[ci].debt = calcDebtFromOrders(orders, customer.id);
        await save(env, "customers", customers);
      }
      if (cashUsed + overpay > 0) {
        var payments = await load(env, "payments");
        payments.unshift({
          id: uid(), customerId: customer.id,
          amount: cashUsed + overpay, note: body.note || "پرداخت هنگام سفارش", createdAt: Date.now()
        });
        await save(env, "payments", payments);
      }
      return json({ order: order }, 201);
    }
    if (path.indexOf("/api/orders/") === 0 && path.endsWith("/status") && request.method === "PATCH") {
      body = await request.json();
      id = path.split("/")[3];
      orders = await load(env, "orders");
      i = orders.findIndex(function (x) { return x.id === id; });
      if (i < 0) return json({ error: "not found" }, 404);
      var status = body.status;
      var cur = orders[i].status || "registered";
      if (status === "cancelled") {
        if (body.byCustomer && cur !== "registered" && cur !== "preparing") {
          return json({ error: "cannot cancel after ship" }, 400);
        }
        if (cur === "delivered" || cur === "cancelled") {
          return json({ error: "already final" }, 400);
        }
        orders[i].status = "cancelled";
        orders[i].cancelledAt = Date.now();
        if (body.byCustomer) orders[i].cancelledByCustomer = true;
        if (body.byKitchen) orders[i].cancelledByKitchen = true;
        await save(env, "orders", orders);
        return json(orders[i]);
      }
      orders[i].status = status;
      if (status === "preparing") orders[i].preparingAt = Date.now();
      if (status === "shipped") orders[i].shippedAt = Date.now();
      if (status === "delivered") {
        orders[i].deliveredAt = Date.now();
        if (body.byCustomer) orders[i].deliveredByCustomer = true;
        if (body.byKitchen) orders[i].deliveredByKitchen = true;
      }
      await save(env, "orders", orders);
      return json(orders[i]);
    }

    if (path === "/api/ratings" && request.method === "POST") {
      body = await request.json();
      var rating = Number(body.rating) || 0;
      if (!body.orderId) return json({ error: "orderId required" }, 400);
      orders = await load(env, "orders");
      var oi = orders.findIndex(function (x) { return x.id === body.orderId; });
      if (oi < 0) return json({ error: "order not found" }, 404);
      var ratings = await load(env, "ratings");
      if (ratings.some(function (r) { return r.orderId === body.orderId; })) {
        return json({ error: "already rated" }, 400);
      }
      ratings.unshift({
        id: uid(), orderId: body.orderId, customerId: orders[oi].customerId,
        customerName: orders[oi].customerName, rating: rating,
        comment: (body.comment || "").trim(), createdAt: Date.now()
      });
      orders[oi].rated = true;
      await save(env, "ratings", ratings);
      await save(env, "orders", orders);
      return json({ ok: true }, 201);
    }
    if (path === "/api/ratings" && request.method === "GET") {
      ratings = await load(env, "ratings");
      var avg = ratings.length
        ? ratings.reduce(function (s, r) { return s + (r.rating || 0); }, 0) / ratings.length
        : 0;
      return json({ ratings: ratings, average: avg, count: ratings.length });
    }

    if (path === "/api/payments" && request.method === "POST") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      body = await request.json();
      var amount = Number(body.amount) || 0;
      if (amount <= 0) return json({ error: "invalid amount" }, 400);
      customers = await load(env, "customers");
      ci = customers.findIndex(function (x) { return x.id === body.customerId; });
      if (ci < 0) return json({ error: "customer not found" }, 404);
      orders = await load(env, "orders");
      var left = amount;
      var openOrders = orders
        .filter(function (o) {
          return o.customerId === body.customerId &&
            (o.totalAmount || 0) > (o.paidAmount || 0);
        })
        .sort(function (a, b) { return (a.createdAt || 0) - (b.createdAt || 0); });
      openOrders.forEach(function (o) {
        if (left <= 0) return;
        var need = (o.totalAmount || 0) - (o.paidAmount || 0);
        var pay = Math.min(left, need);
        o.paidAmount = (o.paidAmount || 0) + pay;
        left -= pay;
      });
      if (left > 0) customers[ci].credit = (customers[ci].credit || 0) + left;
      customers[ci].debt = calcDebtFromOrders(orders, body.customerId);
      await save(env, "orders", orders);
      await save(env, "customers", customers);
      var paymentsList = await load(env, "payments");
      paymentsList.unshift({
        id: uid(), customerId: body.customerId, amount: amount,
        note: body.note || "دریافت از آشپزخانه", createdAt: Date.now()
      });
      await save(env, "payments", paymentsList);
      return json({ ok: true, customer: customers[ci] });
    }

    if (path === "/api/admin/reset" && request.method === "POST") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      body = await request.json();
      if (!body.confirm) return json({ error: "confirm required" }, 400);
      await save(env, "orders", []);
      await save(env, "payments", []);
      return json({ ok: true });
    }

    return json({ error: "not found", path: path }, 404);
  } catch (e) {
    return json({ error: String(e && e.message ? e.message : e) }, 500);
  }
}

function menuAdminHtml() {
  return "<!DOCTYPE html><html lang=fa dir=rtl><head><meta charset=utf-8><meta name=viewport content=\"width=device-width,initial-scale=1\"><title>ریحون API</title></head><body style=\"font-family:Tahoma,sans-serif;background:#F1F8E9;padding:24px;text-align:center\"><h1 style=\"color:#2E7D32\">آشپزخانه ریحون</h1><p>API فعال است.</p><p><a href=\"/api/health\">/api/health</a> · <a href=\"/api/menu\">/api/menu</a></p></body></html>";
}

export default {
  async fetch(request, env, ctx) {
    return handleRequest(request, env);
  }
};
