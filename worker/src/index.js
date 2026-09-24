/**
 * Reyhoon API — accounting fixed (debt = sum of order remainings only)
 * DELETE customer, admin reset, address/phone on orders
 * Menu items support extraSkewerPrice for kebab category
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
  if (env.STORE) await env.STORE.put(key, JSON.stringify(value));
  else mem[key] = value;
}
function uid() { return crypto.randomUUID(); }
function genCode() {
  return String(Math.floor(100000 + Math.random() * 900000));
}
function isAdmin(req, env) {
  var key = req.headers.get("X-Admin-Key") || "";
  return key && key === (env.ADMIN_KEY || "reyhoon-admin-2024");
}
function calcDebtFromOrders(orders, customerId) {
  return orders
    .filter(function (o) { return o.customerId === customerId; })
    .reduce(function (s, o) {
      return s + Math.max(0, (Number(o.totalAmount) || 0) - (Number(o.paidAmount) || 0));
    }, 0);
}

addEventListener("fetch", function (event) {
  event.respondWith(handleRequest(event.request));
});

async function handleRequest(request) {
  var env = getEnv();
  if (request.method === "OPTIONS") return new Response(null, { headers: CORS });
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
      if (existing) return json({ customer: existing, isNew: false }, 200);
      var code = genCode();
      while (customers.some(function (x) { return x.subscriptionCode === code; })) code = genCode();
      var c = {
        id: uid(), name: name, phone: phone,
        address: body.address || { street: "", city: "", postalCode: "", notes: "" },
        subscriptionCode: code, debt: 0, credit: 0, notes: "",
        createdAt: Date.now(), isSelfRegistered: true
      };
      customers.push(c);
      await save(env, "customers", customers);
      return json({ customer: c, isNew: true }, 201);
    }

    if (path === "/api/customers" && request.method === "POST") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      body = await request.json();
      customers = await load(env, "customers");
      var rawCode = (body.subscriptionCode || "").trim();
      code = /^\d{4,10}$/.test(rawCode) ? rawCode : genCode();
      while (customers.some(function (x) { return x.subscriptionCode === code; })) code = genCode();
      c = {
        id: uid(), name: (body.name || "").trim(), phone: (body.phone || "").trim(),
        address: body.address || { street: "", city: "", postalCode: "", notes: "" },
        subscriptionCode: code, debt: 0, credit: Number(body.credit) || 0,
        notes: body.notes || "", createdAt: Date.now(), isSelfRegistered: false
      };
      if (!c.name || !c.phone) return json({ error: "name and phone required" }, 400);
      customers.push(c);
      await save(env, "customers", customers);
      return json(c, 201);
    }

    if (path === "/api/customers" && request.method === "GET") {
      customers = await load(env, "customers");
      var ordersAll = await load(env, "orders");
      customers = customers.map(function (cu) {
        return Object.assign({}, cu, { debt: calcDebtFromOrders(ordersAll, cu.id) });
      });
      return json(customers);
    }

    if (path.indexOf("/api/customers/code/") === 0 && request.method === "GET") {
      var q = decodeURIComponent(path.split("/").pop());
      customers = await load(env, "customers");
      ordersAll = await load(env, "orders");
      c = customers.find(function (x) { return String(x.subscriptionCode) === String(q); });
      if (!c) return json({ error: "not found" }, 404);
      c = Object.assign({}, c, { debt: calcDebtFromOrders(ordersAll, c.id) });
      return json(c);
    }

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

    if (path.indexOf("/api/customers/") === 0 && path.indexOf("/code/") < 0 && request.method === "DELETE") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      id = path.split("/").pop();
      customers = await load(env, "customers");
      var before = customers.length;
      customers = customers.filter(function (x) { return x.id !== id; });
      if (customers.length === before) return json({ error: "not found" }, 404);
      await save(env, "customers", customers);
      return json({ ok: true, deleted: id });
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
      var credit = Number(customer.credit) || 0;
      var creditApplied = Math.min(credit, total);
      credit -= creditApplied;
      var afterCredit = total - creditApplied;
      var paidNow = Math.max(0, Number(body.paidNow) || 0);
      var cashUsed = Math.min(paidNow, afterCredit);
      var overpay = Math.max(0, paidNow - afterCredit);
      if (overpay > 0) credit += overpay;
      var paidOnOrder = creditApplied + cashUsed;
      var addr = customer.address || {};
      var addressStr = [addr.street || "", addr.city || ""].filter(Boolean).join(" - ");
      var order = {
        id: uid(), customerId: customer.id, customerName: customer.name,
        customerPhone: customer.phone || "", customerCode: customer.subscriptionCode || "",
        customerAddress: addressStr,
        isNewCustomer: !!customer.isSelfRegistered && !customer._orderedBefore,
        items: items, totalAmount: total, paidAmount: paidOnOrder,
        creditApplied: creditApplied, status: "registered",
        source: body.source || "online", createdAt: Date.now(),
        preparingAt: null, shippedAt: null, deliveredAt: null,
        note: body.note || "", rated: false
      };
      orders = await load(env, "orders");
      orders.unshift(order);
      await save(env, "orders", orders);
      var debt = calcDebtFromOrders(orders, customer.id);
      var ci = customers.findIndex(function (x) { return x.id === customer.id; });
      customers[ci] = Object.assign({}, customers[ci], { debt: debt, credit: credit, _orderedBefore: true });
      await save(env, "customers", customers);
      if (cashUsed > 0 || overpay > 0) {
        var payments = await load(env, "payments");
        payments.unshift({
          id: uid(), customerId: customer.id, orderId: order.id,
          amount: cashUsed + overpay, note: body.note || "پرداخت هنگام سفارش", createdAt: Date.now()
        });
        await save(env, "payments", payments);
      }
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
      if (orders[oi].status !== "delivered") return json({ error: "not delivered" }, 400);
      var ratings = await load(env, "ratings");
      if (ratings.some(function (r) { return r.orderId === body.orderId; })) {
        return json({ error: "already rated" }, 400);
      }
      var r = {
        id: uid(), orderId: body.orderId, customerId: orders[oi].customerId,
        customerName: orders[oi].customerName, rating: rating,
        comment: (body.comment || "").trim(), createdAt: Date.now()
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
          return x.o.customerId === body.customerId &&
            (Number(x.o.totalAmount) || 0) - (Number(x.o.paidAmount) || 0) > 0;
        })
        .sort(function (a, b) { return a.o.createdAt - b.o.createdAt; });
      for (var k = 0; k < open.length; k++) {
        if (remainingPay <= 0) break;
        var rem = (Number(open[k].o.totalAmount) || 0) - (Number(open[k].o.paidAmount) || 0);
        var pay = Math.min(remainingPay, rem);
        orders[open[k].idx] = Object.assign({}, open[k].o, {
          paidAmount: (Number(open[k].o.paidAmount) || 0) + pay
        });
        remainingPay -= pay;
      }
      await save(env, "orders", orders);
      var debt = calcDebtFromOrders(orders, body.customerId);
      var credit = (Number(customers[ci].credit) || 0) + remainingPay;
      customers[ci] = Object.assign({}, customers[ci], { debt: debt, credit: credit });
      await save(env, "customers", customers);
      var paymentsList = await load(env, "payments");
      var p = {
        id: uid(), customerId: body.customerId, amount: amount,
        note: body.note || "تسویه بدهی", createdAt: Date.now()
      };
      paymentsList.unshift(p);
      await save(env, "payments", paymentsList);
      return json({ payment: p, customer: customers[ci] });
    }

    if (path === "/api/admin/reset" && request.method === "POST") {
      if (!isAdmin(request, env)) return json({ error: "unauthorized" }, 401);
      await save(env, "menu", []);
      await save(env, "customers", []);
      await save(env, "orders", []);
      await save(env, "payments", []);
      await save(env, "ratings", []);
      return json({ ok: true, message: "all data cleared" });
    }

    return json({ error: "not found", path: path }, 404);
  } catch (e) {
    return json({ error: String(e.message || e) }, 500);
  }
}

function menuAdminHtml() {
  return "<!DOCTYPE html><html lang=fa dir=rtl><head><meta charset=utf-8><meta name=viewport content=\"width=device-width,initial-scale=1\"><title>Menu Admin</title></head><body style=\"font-family:Tahoma;background:#F1F8E9;padding:16px\"><h1>\u0645\u062f\u06cc\u0631\u06cc\u062a \u0645\u0646\u0648</h1><p>API: /api/menu</p><p>Admin key: reyhoon-admin-2024</p></body></html>";
}
