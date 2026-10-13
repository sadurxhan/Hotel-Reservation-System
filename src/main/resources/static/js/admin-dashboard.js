/**
 * THE LOFT BY THE LAKE - ADMINISTRATIVE DASHBOARD JAVASCRIPT
 * Unified management for Bookings, Cancellations, Billing, Refunds, Menu & Activities
 */

(function () {
  "use strict";

  // State Management
  const state = {
    adminKey: window.SERVER_ADMIN_KEY || localStorage.getItem("loft_admin_key") || "1234",
    currentTab: "overview",
    reservations: [],
    cancellations: [],
    payments: [],
    refunds: [],
    menuItems: [],
    activities: [],
    reviews: [],
    calendarBlocks: [],
    rateOverrides: [],
    rooms: [],
    overviewData: null,
    resSearchQuery: "",
    resStatusFilter: "ALL",
    resPaymentFilter: "ALL",
    resCurrentPage: 1,
    resPageSize: 10,
    paySearchQuery: "",
    payStatusFilter: "ALL",
    cancStatusFilter: "ALL",
    blockSearchQuery: "",
    blockRoomFilter: "ALL",
    blockCurrentPage: 1,
    blockPageSize: 10,
    rateSearchQuery: "",
    rateStatusFilter: "ALL"
  };

  // ----------------------------------------------------
  // UTILITIES & NOTIFICATIONS
  // ----------------------------------------------------
  window.showToast = function (message, type = "success") {
    const container = document.getElementById("toastContainer");
    if (!container) return;

    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    const icon = type === "success" ? "✓" : (type === "error" ? "✕" : "⚠");
    toast.innerHTML = `<span>${icon}</span> <span>${message}</span>`;

    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transform = "translateX(50px)";
      toast.style.transition = "all 0.3s ease";
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  };

  window.formatLKR = function (amount) {
    if (amount === null || amount === undefined) return "LKR 0.00";
    const num = Number(amount);
    return "LKR " + num.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };

  window.formatDate = function (dateStr) {
    if (!dateStr) return "N/A";
    const d = new Date(dateStr);
    return d.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });
  };

  window.formatDateTime = function (dtStr) {
    if (!dtStr) return "N/A";
    const d = new Date(dtStr);
    return d.toLocaleDateString("en-US", { month: "short", day: "numeric" }) + " " +
      d.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" });
  };

  // Authenticated Fetch Helper
  async function adminFetch(url, options = {}) {
    options.headers = options.headers || {};
    if (!(options.body instanceof FormData)) {
      if (!options.headers["Content-Type"]) {
        options.headers["Content-Type"] = "application/json";
      }
    }
    options.headers["X-Admin-Key"] = state.adminKey;

    try {
      const response = await fetch(url, options);
      if (response.status === 401 || response.status === 403) {
        showAdminKeyModal();
        throw new Error("Administrative authentication required.");
      }
      return response;
    } catch (err) {
      throw err;
    }
  }

  // Confirmation Modal Trigger
  let confirmCallback = null;
  window.showConfirmDialog = function (title, message, confirmBtnText = "Confirm", onConfirm) {
    document.getElementById("confirmModalTitle").innerText = title;
    document.getElementById("confirmModalMsg").innerHTML = message;
    const actionBtn = document.getElementById("confirmModalBtn");
    actionBtn.innerText = confirmBtnText;
    confirmCallback = onConfirm;
    document.getElementById("confirmModal").classList.add("active");
  };

  window.closeConfirmDialog = function () {
    document.getElementById("confirmModal").classList.remove("active");
    confirmCallback = null;
  };

  window.handleConfirmSubmit = function () {
    if (typeof confirmCallback === "function") {
      confirmCallback();
    }
    closeConfirmDialog();
  };

  // ----------------------------------------------------
  // TAB NAVIGATION
  // ----------------------------------------------------
  window.switchTab = function (tabName) {
    state.currentTab = tabName;
    document.querySelectorAll(".nav-item").forEach(item => {
      item.classList.toggle("active", item.getAttribute("data-tab") === tabName);
    });

    document.querySelectorAll(".tab-content").forEach(section => {
      section.classList.toggle("active", section.id === `tab-${tabName}`);
    });

    const titleMap = {
      overview: "Executive Dashboard",
      reservations: "Reservation Management",
      cancellations: "Cancellation Requests",
      payments: "Billing & Payment History",
      refunds: "Guest Refund Reconciliation",
      calendar: "Calendar Availability & Blocks",
      rates: "Dynamic Pricing & Packages",
      menu: "Dining & Menu Showcase",
      activities: "Activities & Experiences",
      reviews: "Guest Reviews Moderation"
    };

    document.getElementById("pageHeaderTitle").innerText = titleMap[tabName] || "Admin Dashboard";
    window.location.hash = tabName;

    // Trigger tab data refreshes
    if (tabName === "overview") loadOverview();
    if (tabName === "reservations") loadReservations();
    if (tabName === "cancellations") loadCancellations();
    if (tabName === "payments") loadPayments();
    if (tabName === "refunds") loadRefunds();
    if (tabName === "calendar") loadCalendarBlocks();
    if (tabName === "rates") loadRateOverrides();
    if (tabName === "menu") loadMenu();
    if (tabName === "activities") loadActivities();
    if (tabName === "reviews") loadReviews();
  };

  // ----------------------------------------------------
  // 1. DASHBOARD OVERVIEW & KPIS
  // ----------------------------------------------------
  async function loadOverview() {
    try {
      const res = await adminFetch("/api/admin/overview");
      if (!res.ok) throw new Error("Could not load overview stats.");
      const data = await res.json();
      state.overviewData = data;

      document.getElementById("kpiTotalReservations").innerText = data.totalReservations || 0;
      document.getElementById("kpiConfirmedBadge").innerText = `${data.confirmedBookings || 0} Confirmed`;
      document.getElementById("kpiPendingBadge").innerText = `${data.pendingBookings || 0} Pending`;

      document.getElementById("kpiTotalRevenue").innerText = formatLKR(data.totalRevenue);
      document.getElementById("kpiPaymentsCount").innerText = `${data.successfulPayments || 0} Successful Payments`;

      document.getElementById("kpiPendingCancellations").innerText = data.pendingCancellations || 0;
      const cancBadge = document.getElementById("kpiCancellationAlert");
      if (data.pendingCancellations > 0) {
        cancBadge.className = "trend-badge alert";
        cancBadge.innerText = "Action Needed";
      } else {
        cancBadge.className = "trend-badge up";
        cancBadge.innerText = "All Clear";
      }

      document.getElementById("kpiPendingRefunds").innerText = data.pendingRefunds || 0;
      document.getElementById("kpiTotalRefunded").innerText = `${formatLKR(data.totalRefunded)} Disbursed`;

      // Render chart
      renderRevenueChart(data.revenueChart || []);

      // Update sidebar notification badges
      updateNavBadge("cancNavBadge", data.pendingCancellations);
      updateNavBadge("refundNavBadge", data.pendingRefunds);
    } catch (err) {
      console.error("Overview error:", err);
    }
  }

  function updateNavBadge(id, count) {
    const el = document.getElementById(id);
    if (!el) return;
    if (count > 0) {
      el.style.display = "inline-block";
      el.innerText = count;
    } else {
      el.style.display = "none";
    }
  }

  // Interactive Modern SVG Curve Chart
  function renderRevenueChart(points) {
    const container = document.getElementById("revenueChartBox");
    if (!container) return;

    if (!points || points.length === 0) {
      container.innerHTML = `
        <div style="height: 100%; display: flex; align-items: center; justify-content: center; color: #94A3B8; font-size: 0.9rem;">
          No revenue recorded in this period yet.
        </div>`;
      return;
    }

    const width = container.clientWidth || 600;
    const height = 230;
    const padX = 50;
    const padY = 30;

    const values = points.map(p => Number(p.revenue || 0));
    const maxVal = Math.max(...values, 10000);
    const minVal = 0;

    const chartW = width - padX * 2;
    const chartH = height - padY * 2;

    const stepX = points.length > 1 ? chartW / (points.length - 1) : chartW;

    const coords = points.map((p, idx) => {
      const x = padX + (idx * stepX);
      const y = height - padY - ((Number(p.revenue || 0) / maxVal) * chartH);
      return { x, y, date: p.date, val: p.revenue };
    });

    let pathD = `M ${coords[0].x} ${coords[0].y}`;
    for (let i = 1; i < coords.length; i++) {
      // Smooth cubic bezier curve
      const cpX1 = coords[i - 1].x + (coords[i].x - coords[i - 1].x) / 2;
      const cpY1 = coords[i - 1].y;
      const cpX2 = cpX1;
      const cpY2 = coords[i].y;
      pathD += ` C ${cpX1} ${cpY1}, ${cpX2} ${cpY2}, ${coords[i].x} ${coords[i].y}`;
    }

    const areaD = `${pathD} L ${coords[coords.length - 1].x} ${height - padY} L ${coords[0].x} ${height - padY} Z`;

    const gridLines = [0, 0.5, 1].map(pct => {
      const y = height - padY - (pct * chartH);
      const label = formatLKR(maxVal * pct).replace("LKR ", "");
      return `
        <line x1="${padX}" y1="${y}" x2="${width - padX}" y2="${y}" stroke="#E2E8F0" stroke-dasharray="4" />
        <text x="${padX - 8}" y="${y + 4}" font-size="10" fill="#94A3B8" text-anchor="end">${label}</text>
      `;
    }).join("");

    const dots = coords.map((c, i) => `
      <circle cx="${c.x}" cy="${c.y}" r="4.5" fill="#D4B46A" stroke="#1B3B2B" stroke-width="2.5" />
      <text x="${c.x}" y="${height - 10}" font-size="10.5" fill="#64748B" text-anchor="middle" font-weight="600">${c.date}</text>
    `).join("");

    container.innerHTML = `
      <svg width="100%" height="${height}" viewBox="0 0 ${width} ${height}" style="overflow: visible;">
        <defs>
          <linearGradient id="revenueGrad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#D4B46A" stop-opacity="0.28" />
            <stop offset="100%" stop-color="#D4B46A" stop-opacity="0.0" />
          </linearGradient>
        </defs>
        ${gridLines}
        <path d="${areaD}" fill="url(#revenueGrad)" />
        <path d="${pathD}" fill="none" stroke="#1B3B2B" stroke-width="3" stroke-linecap="round" />
        ${dots}
      </svg>
    `;
  }

  // ----------------------------------------------------
  // 2. RESERVATION MANAGEMENT
  // ----------------------------------------------------
  async function loadReservations() {
    const tbody = document.getElementById("reservationsTbody");
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 30px; color: #94A3B8;">Loading bookings...</td></tr>`;

    try {
      const res = await adminFetch("/api/admin/reservations");
      if (!res.ok) throw new Error("Failed to load reservations.");
      state.reservations = await res.json();
      renderReservationsTable();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 30px; color: #EF4444;">${err.message}</td></tr>`;
    }
  }

  window.handleReservationFilter = function () {
    state.resSearchQuery = (document.getElementById("resSearchInput").value || "").toLowerCase().trim();
    state.resStatusFilter = document.getElementById("resStatusSelect").value;
    state.resPaymentFilter = document.getElementById("resPaymentSelect").value;
    state.resCurrentPage = 1;
    renderReservationsTable();
  };

  function renderReservationsTable() {
    const tbody = document.getElementById("reservationsTbody");
    if (!tbody) return;

    let filtered = state.reservations.filter(r => {
      const matchesSearch = !state.resSearchQuery ||
        String(r.reservationId).includes(state.resSearchQuery) ||
        (r.guestName && r.guestName.toLowerCase().includes(state.resSearchQuery)) ||
        (r.guestEmail && r.guestEmail.toLowerCase().includes(state.resSearchQuery));

      const matchesStatus = state.resStatusFilter === "ALL" || r.reservationStatus === state.resStatusFilter;
      const matchesPayment = state.resPaymentFilter === "ALL" || r.paymentStatus === state.resPaymentFilter;

      return matchesSearch && matchesStatus && matchesPayment;
    });

    const total = filtered.length;
    const startIndex = (state.resCurrentPage - 1) * state.resPageSize;
    const paged = filtered.slice(startIndex, startIndex + state.resPageSize);

    document.getElementById("resCountNotice").innerText = `Showing ${Math.min(startIndex + 1, total)}–${Math.min(startIndex + paged.length, total)} of ${total} reservations`;

    document.getElementById("resPrevBtn").disabled = state.resCurrentPage <= 1;
    document.getElementById("resNextBtn").disabled = startIndex + state.resPageSize >= total;

    if (paged.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 32px; color: #94A3B8;">No reservations matched the search filter criteria.</td></tr>`;
      return;
    }

    tbody.innerHTML = paged.map(r => {
      const statusClass = `status-${(r.reservationStatus || "").toLowerCase()}`;
      const payClass = `status-${(r.paymentStatus || "").toLowerCase()}`;

      const nights = (r.checkInDate && r.checkOutDate) ?
        Math.max(1, Math.round((new Date(r.checkOutDate) - new Date(r.checkInDate)) / (1000 * 60 * 60 * 24))) : 1;

      return `
        <tr>
          <td>
            <strong>#${r.reservationId}</strong>
            <div style="font-size: 0.74rem; color: #94A3B8;">${formatDate(r.bookingDate)}</div>
          </td>
          <td>
            <div style="font-weight: 600; color: #1B3B2B;">${r.guestName || "Guest"}</div>
            <div style="font-size: 0.78rem; color: #64748B;">${r.guestEmail || "No Email"}</div>
            <div style="font-size: 0.75rem; color: #94A3B8;">${r.guestPhone || ""}</div>
          </td>
          <td>
            <div>${formatDate(r.checkInDate)} ➔ ${formatDate(r.checkOutDate)}</div>
            <span style="font-size: 0.75rem; color: #8A7352; font-weight: 600;">${nights} Night${nights > 1 ? "s" : ""} • ${r.numberOfGuests || 1} Guests</span>
          </td>
          <td>
            <strong style="color: #1B3B2B;">${formatLKR(r.totalAmount)}</strong>
          </td>
          <td>
            <span class="badge-pill ${statusClass}">${r.reservationStatus}</span>
          </td>
          <td>
            <span class="badge-pill ${payClass}">${r.paymentStatus}</span>
            ${r.transactionId ? `<div style="font-size: 0.72rem; color: #64748B; font-family: monospace;">TX: ${r.transactionId.substring(0, 10)}...</div>` : ""}
          </td>
          <td>
            <div class="table-actions">
              ${r.reservationStatus === "PENDING" ? `
                <button class="btn btn-sm btn-primary" onclick="confirmReservationAction(${r.reservationId})" title="Confirm Booking">Confirm</button>
              ` : ""}
              ${r.reservationStatus !== "CANCELLED" ? `
                <button class="btn btn-sm btn-secondary" onclick="cancelReservationAction(${r.reservationId})" title="Cancel Booking" style="color: #EF4444;">Cancel</button>
              ` : ""}
              <button class="btn btn-sm btn-secondary" onclick="viewReservationModal(${r.reservationId})">View</button>
            </div>
          </td>
        </tr>
      `;
    }).join("");
  }

  window.resPagePrev = function () {
    if (state.resCurrentPage > 1) {
      state.resCurrentPage--;
      renderReservationsTable();
    }
  };

  window.resPageNext = function () {
    state.resCurrentPage++;
    renderReservationsTable();
  };

  window.confirmReservationAction = async function (id) {
    showConfirmDialog(
      "Confirm Reservation",
      `Are you sure you want to officially confirm reservation <strong>#${id}</strong>?`,
      "Yes, Confirm Booking",
      async () => {
        try {
          const res = await adminFetch(`/api/reservations/${id}/confirm`, { method: "PUT" });
          if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || "Unable to confirm booking.");
          }
          showToast(`Reservation #${id} confirmed successfully.`);
          loadReservations();
          loadOverview();
        } catch (err) {
          showToast(err.message, "error");
        }
      }
    );
  };

  window.cancelReservationAction = async function (id) {
    showConfirmDialog(
      "Cancel Reservation",
      `Are you sure you want to cancel reservation <strong>#${id}</strong>? This will release reserved dates on the calendar.`,
      "Yes, Cancel Booking",
      async () => {
        try {
          const res = await adminFetch(`/api/reservations/${id}/cancel`, { method: "PUT" });
          if (!res.ok) {
            const errText = await res.text();
            throw new Error(errText || "Unable to cancel booking.");
          }
          showToast(`Reservation #${id} has been cancelled.`);
          loadReservations();
          loadOverview();
        } catch (err) {
          showToast(err.message, "error");
        }
      }
    );
  };

  window.viewReservationModal = function (id) {
    const r = state.reservations.find(item => item.reservationId === id);
    if (!r) return;

    const modalBody = document.getElementById("resDetailModalBody");
    modalBody.innerHTML = `
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px;">
        <div>
          <div style="font-size: 0.78rem; color: #94A3B8; text-transform: uppercase;">Reservation ID</div>
          <div style="font-size: 1.2rem; font-weight: 700; color: #1B3B2B;">#${r.reservationId}</div>
        </div>
        <div>
          <div style="font-size: 0.78rem; color: #94A3B8; text-transform: uppercase;">Booking Status</div>
          <span class="badge-pill status-${(r.reservationStatus || '').toLowerCase()}">${r.reservationStatus}</span>
        </div>
      </div>

      <div style="background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 8px; padding: 16px; margin-bottom: 16px;">
        <h5 style="margin-bottom: 8px; color: #1B3B2B;">Guest Details</h5>
        <p><strong>Name:</strong> ${r.guestName}</p>
        <p><strong>Email:</strong> ${r.guestEmail}</p>
        <p><strong>Phone:</strong> ${r.guestPhone}</p>
        <p><strong>Party Size:</strong> ${r.numberOfGuests} Guests</p>
      </div>

      <div style="background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 8px; padding: 16px; margin-bottom: 16px;">
        <h5 style="margin-bottom: 8px; color: #1B3B2B;">Stay & Payment</h5>
        <p><strong>Check-in:</strong> ${formatDate(r.checkInDate)}</p>
        <p><strong>Check-out:</strong> ${formatDate(r.checkOutDate)}</p>
        <p><strong>Total Amount:</strong> ${formatLKR(r.totalAmount)}</p>
        <p><strong>Payment Status:</strong> <span class="badge-pill status-${(r.paymentStatus || '').toLowerCase()}">${r.paymentStatus}</span></p>
        ${r.transactionId ? `<p><strong>Transaction ID:</strong> <code>${r.transactionId}</code></p>` : ""}
        ${r.paymentMethod ? `<p><strong>Method:</strong> ${r.paymentMethod}</p>` : ""}
      </div>
    `;

    document.getElementById("resDetailModal").classList.add("active");
  };

  window.closeResDetailModal = function () {
    document.getElementById("resDetailModal").classList.remove("active");
  };

  // ----------------------------------------------------
  // 3. CANCELLATION MANAGEMENT
  // ----------------------------------------------------
  async function loadCancellations() {
    const tbody = document.getElementById("cancellationsTbody");
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 30px; color: #94A3B8;">Loading cancellations...</td></tr>`;

    try {
      const res = await adminFetch("/api/admin/cancellations");
      if (!res.ok) throw new Error("Failed to load cancellation records.");
      state.cancellations = await res.json();
      renderCancellationsTable();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 30px; color: #EF4444;">${err.message}</td></tr>`;
    }
  }

  window.handleCancellationFilter = function () {
    state.cancStatusFilter = document.getElementById("cancStatusSelect").value;
    renderCancellationsTable();
  };

  function renderCancellationsTable() {
    const tbody = document.getElementById("cancellationsTbody");
    if (!tbody) return;

    let filtered = state.cancellations.filter(c => {
      return state.cancStatusFilter === "ALL" || c.cancellationStatus === state.cancStatusFilter;
    });

    if (filtered.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 32px; color: #94A3B8;">No cancellation records found.</td></tr>`;
      return;
    }

    tbody.innerHTML = filtered.map(c => {
      const statusClass = `status-${(c.cancellationStatus || "").toLowerCase()}`;

      return `
        <tr>
          <td>
            <strong>#${c.reservationId}</strong>
            <div style="font-size: 0.72rem; color: #94A3B8; font-family: monospace;">${(c.cancellationId || "").substring(0, 8)}...</div>
          </td>
          <td>
            <div style="font-weight: 600; color: #1B3B2B;">${c.guestName || "Guest"}</div>
            <div style="font-size: 0.78rem; color: #64748B;">${c.guestEmail || ""}</div>
          </td>
          <td>
            <span class="badge-pill status-unpaid">${c.requestedBy || "Guest"}</span>
            <div style="font-size: 0.74rem; color: #94A3B8; margin-top: 4px;">${formatDateTime(c.requestedDateTime)}</div>
          </td>
          <td style="max-width: 250px;">
            <div style="font-size: 0.84rem; color: #334155; line-height: 1.4; word-break: break-word;">${c.cancellationReason || "No reason provided"}</div>
          </td>
          <td>
            <strong>${formatLKR(c.totalAmount)}</strong>
            <div style="font-size: 0.74rem; color: #8A7352;">In: ${formatDate(c.checkInDate)}</div>
          </td>
          <td>
            <span class="badge-pill ${statusClass}">${c.cancellationStatus}</span>
          </td>
          <td>
            <div class="table-actions">
              ${c.cancellationStatus === "REQUESTED" ? `
                <button class="btn btn-sm btn-primary" onclick="approveCancellationAction('${c.cancellationId}')">Approve</button>
                <button class="btn btn-sm btn-danger" onclick="rejectCancellationAction('${c.cancellationId}')">Reject</button>
              ` : `
                <span style="font-size: 0.75rem; color: #94A3B8;">Finalized</span>
              `}
            </div>
          </td>
        </tr>
      `;
    }).join("");
  }

  window.approveCancellationAction = function (id) {
    showConfirmDialog(
      "Approve Cancellation Request",
      `Approving this request will cancel the booking, compute any eligible refund under the policy, and notify the guest.<br><br><strong>Refund Policy:</strong> 5+ days prior = 100% | 3–4 days = 75% | &le; 2 days = 0%.`,
      "Approve & Process",
      async () => {
        try {
          const res = await adminFetch(`/api/admin/cancellations/${id}/approve`, { method: "POST" });
          if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || "Failed to approve cancellation.");
          }
          showToast("Cancellation approved and refund record generated.");
          loadCancellations();
          loadOverview();
        } catch (err) {
          showToast(err.message, "error");
        }
      }
    );
  };

  window.rejectCancellationAction = function (id) {
    showConfirmDialog(
      "Reject Cancellation Request",
      `Are you sure you want to reject this request? The reservation will remain active and the guest will be emailed notification of rejection.`,
      "Reject Request",
      async () => {
        try {
          const res = await adminFetch(`/api/admin/cancellations/${id}/reject`, { method: "POST" });
          if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || "Failed to reject cancellation.");
          }
          showToast("Cancellation request was rejected.");
          loadCancellations();
          loadOverview();
        } catch (err) {
          showToast(err.message, "error");
        }
      }
    );
  };

  // ----------------------------------------------------
  // 4. BILLING & PAYMENTS HISTORY
  // ----------------------------------------------------
  async function loadPayments() {
    const tbody = document.getElementById("paymentsTbody");
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 30px; color: #94A3B8;">Loading transactions...</td></tr>`;

    try {
      const res = await adminFetch("/api/admin/payments");
      if (!res.ok) throw new Error("Failed to load payment transactions.");
      state.payments = await res.json();
      renderPaymentsTable();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 30px; color: #EF4444;">${err.message}</td></tr>`;
    }
  }

  window.handlePaymentFilter = function () {
    state.paySearchQuery = (document.getElementById("paySearchInput").value || "").toLowerCase().trim();
    state.payStatusFilter = document.getElementById("payStatusSelect").value;
    renderPaymentsTable();
  };

  function renderPaymentsTable() {
    const tbody = document.getElementById("paymentsTbody");
    if (!tbody) return;

    let filtered = state.payments.filter(p => {
      const matchesSearch = !state.paySearchQuery ||
        (p.transactionId && p.transactionId.toLowerCase().includes(state.paySearchQuery)) ||
        String(p.reservationId).includes(state.paySearchQuery) ||
        (p.guestEmail && p.guestEmail.toLowerCase().includes(state.paySearchQuery));

      const matchesStatus = state.payStatusFilter === "ALL" || p.paymentStatus === state.payStatusFilter;
      return matchesSearch && matchesStatus;
    });

    if (filtered.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 32px; color: #94A3B8;">No transactions found matching your criteria.</td></tr>`;
      return;
    }

    tbody.innerHTML = filtered.map(p => {
      const statusClass = `status-${(p.paymentStatus || "").toLowerCase()}`;
      return `
        <tr>
          <td>
            <div style="font-family: monospace; font-weight: 600; color: #1B3B2B;">${p.transactionId || "N/A"}</div>
            <div style="font-size: 0.72rem; color: #94A3B8;">ID: ${(p.paymentId || "").substring(0, 10)}...</div>
          </td>
          <td>
            <strong>#${p.reservationId}</strong>
          </td>
          <td>
            <div style="font-weight: 600; color: #1B3B2B;">${p.guestName || "Guest"}</div>
            <div style="font-size: 0.76rem; color: #64748B;">${p.guestEmail || ""}</div>
          </td>
          <td>
            <strong style="color: #1B3B2B;">${formatLKR(p.amount)}</strong>
          </td>
          <td>
            <span>${p.paymentMethod || "Credit_Card"}</span>
          </td>
          <td>
            <span class="badge-pill ${statusClass}">${p.paymentStatus}</span>
            <div style="font-size: 0.72rem; color: #94A3B8; margin-top: 3px;">${formatDateTime(p.paymentDateTime)}</div>
          </td>
          <td>
            <div class="table-actions">
              ${p.paymentStatus === "PAID" ? `
                <a href="/invoice?invoiceId=${p.invoiceId || ''}" target="_blank" class="btn btn-sm btn-secondary" title="Print Invoice">
                  📄 Invoice
                </a>
              ` : `
                <span style="font-size: 0.75rem; color: #94A3B8;">No Invoice</span>
              `}
            </div>
          </td>
        </tr>
      `;
    }).join("");
  }

  // CSV Export for Payments
  window.exportPaymentsCSV = function () {
    if (!state.payments || state.payments.length === 0) {
      showToast("No payment data available to export.", "warning");
      return;
    }

    const headers = ["Transaction ID", "Reservation ID", "Guest Name", "Guest Email", "Amount LKR", "Method", "Status", "Date Time"];
    const rows = state.payments.map(p => [
      `"${p.transactionId || ''}"`,
      p.reservationId,
      `"${p.guestName || ''}"`,
      `"${p.guestEmail || ''}"`,
      p.amount || 0,
      `"${p.paymentMethod || ''}"`,
      `"${p.paymentStatus || ''}"`,
      `"${p.paymentDateTime || ''}"`
    ]);

    const csvContent = "data:text/csv;charset=utf-8," + [headers.join(","), ...rows.map(e => e.join(","))].join("\n");
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `loft_payments_export_${new Date().toISOString().slice(0, 10)}.csv`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    showToast("Payment history exported to CSV.");
  };

  // ----------------------------------------------------
  // 5. REFUND RECONCILIATION
  // ----------------------------------------------------
  async function loadRefunds() {
    const tbody = document.getElementById("refundsTbody");
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 30px; color: #94A3B8;">Loading refund records...</td></tr>`;

    try {
      const res = await adminFetch("/api/admin/refunds");
      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || `Failed to load refunds (HTTP ${res.status}).`);
      }
      state.refunds = await res.json();
      renderRefundsTable();
    } catch (err) {
      console.error("Refunds error:", err);
      tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 30px; color: #EF4444;">${err.message}</td></tr>`;
    }
  }

  function renderRefundsTable() {
    const tbody = document.getElementById("refundsTbody");
    if (!tbody) return;

    const pendingRefunds = state.refunds.filter(r => r.refundStatus === "PENDING");
    const pendingAlertBanner = document.getElementById("pendingRefundsBanner");
    if (pendingAlertBanner) {
      if (pendingRefunds.length > 0) {
        pendingAlertBanner.style.display = "flex";
        const sum = pendingRefunds.reduce((acc, r) => acc + Number(r.refundAmount || 0), 0);
        document.getElementById("pendingRefundSummaryText").innerHTML =
          `You have <strong>${pendingRefunds.length} pending refund(s)</strong> totaling <strong>${formatLKR(sum)}</strong> requiring manual bank wire or card disbursement.`;
      } else {
        pendingAlertBanner.style.display = "none";
      }
    }

    if (state.refunds.length === 0) {
      tbody.innerHTML = `<tr><td colspan="7" style="text-align: center; padding: 32px; color: #94A3B8;">No refund records found.</td></tr>`;
      return;
    }

    tbody.innerHTML = state.refunds.map(r => {
      const isPending = r.refundStatus === "PENDING";
      const statusClass = `status-${(r.refundStatus || "").toLowerCase()}`;

      return `
        <tr style="${isPending ? 'background-color: #FEFCE8;' : ''}">
          <td>
            <strong>#${r.reservationId || 'N/A'}</strong>
            <div style="font-size: 0.72rem; color: #94A3B8; font-family: monospace;">${(r.refundId || '').substring(0, 8)}...</div>
          </td>
          <td>
            <div style="font-weight: 600; color: #1B3B2B;">${r.guestName || "Guest"}</div>
            <div style="font-size: 0.76rem; color: #64748B;">${r.guestEmail || ""}</div>
          </td>
          <td>
            <strong style="color: ${isPending ? '#B45309' : '#1B3B2B'}; font-size: 0.95rem;">${formatLKR(r.refundAmount)}</strong>
          </td>
          <td style="max-width: 250px;">
            <div style="font-size: 0.82rem; color: #475569; line-height: 1.4;">${r.refundReason || "Cancellation policy refund"}</div>
          </td>
          <td>
            <span class="badge-pill ${statusClass}">${r.refundStatus}</span>
          </td>
          <td>
            <div style="font-size: 0.78rem; color: #64748B;">${formatDateTime(r.refundDateTime)}</div>
          </td>
          <td>
            <div class="table-actions">
              ${isPending ? `
                <button class="btn btn-sm btn-gold" onclick="markRefundPaidAction('${r.refundId}', '${r.guestName}', '${r.refundAmount}')">
                  ✓ Mark as Paid
                </button>
              ` : `
                <span style="font-size: 0.76rem; color: #10B981; font-weight: 600;">Disbursed</span>
              `}
            </div>
          </td>
        </tr>
      `;
    }).join("");
  }

  window.markRefundPaidAction = function (refundId, guestName, amount) {
    showConfirmDialog(
      "Confirm Manual Refund Settlement",
      `Have you completed the payment transfer of <strong>${formatLKR(amount)}</strong> to <strong>${guestName}</strong>?<br><br>Marking this refund as PAID will update accounting records and trigger an email notification receipt to the guest.`,
      "Yes, Mark as Refunded",
      async () => {
        try {
          const res = await adminFetch(`/api/admin/refunds/${refundId}/paid`, { method: "POST" });
          if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || "Failed to mark refund as paid.");
          }
          showToast(`Refund of ${formatLKR(amount)} recorded as settled.`);
          loadRefunds();
          loadOverview();
        } catch (err) {
          showToast(err.message, "error");
        }
      }
    );
  };

  // ----------------------------------------------------
  // 6. DINING MENU SHOWCASE CRUD
  // ----------------------------------------------------
  async function loadMenu() {
    const grid = document.getElementById("menuCardsGrid");
    if (!grid) return;
    grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #94A3B8;">Loading dining menu items...</div>`;

    try {
      const res = await adminFetch("/api/admin/menu");
      if (!res.ok) throw new Error("Failed to load menu items.");
      state.menuItems = await res.json();
      renderMenuGrid();
    } catch (err) {
      grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #EF4444;">${err.message}</div>`;
    }
  }

  function renderMenuGrid() {
    const grid = document.getElementById("menuCardsGrid");
    if (!grid) return;

    if (state.menuItems.length === 0) {
      grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #94A3B8;">No menu items added yet. Click "+ Add Dish" above to create one.</div>`;
      return;
    }

    grid.innerHTML = state.menuItems.map(m => {
      const photo = m.fileUrl || "images/menu/roti.jpg";
      const isAvailable = Boolean(m.available);

      return `
        <div class="item-card">
          <div class="item-img-wrap">
            <img src="${photo}" alt="${m.title}" onerror="this.src='images/hero/dining-left.jpg'">
            <div class="item-badge-top">
              <span class="badge-pill ${isAvailable ? 'status-available' : 'status-hidden'}">
                ${isAvailable ? 'Active' : 'Hidden'}
              </span>
            </div>
          </div>
          <div class="item-card-body">
            <div>
              <span style="font-size: 0.72rem; font-weight: 700; color: #8A7352; text-transform: uppercase; letter-spacing: 0.5px;">${m.mealType}</span>
              <h4>${m.title}</h4>
              <p>${m.mealDescription || "No description provided."}</p>
            </div>
            <div class="item-card-footer">
              <button class="btn btn-sm btn-secondary" onclick="toggleMenuAction(${m.menuId})">
                ${isAvailable ? 'Hide from Guests' : 'Show on Site'}
              </button>
              <div style="display: flex; gap: 6px;">
                <button class="btn btn-sm btn-secondary" onclick="openMenuEditModal(${m.menuId})">Edit</button>
                <button class="btn btn-sm btn-danger" onclick="deleteMenuAction(${m.menuId})">✕</button>
              </div>
            </div>
          </div>
        </div>
      `;
    }).join("");
  }

  window.openMenuModal = function () {
    document.getElementById("menuModalTitle").innerText = "Add New Dining Dish";
    document.getElementById("menuFormId").value = "";
    document.getElementById("menuFormTitle").value = "";
    document.getElementById("menuFormType").value = "BREAKFAST";
    document.getElementById("menuFormDesc").value = "";
    document.getElementById("menuFormPhoto").value = "";
    document.getElementById("menuFormAvailable").checked = true;
    document.getElementById("menuModal").classList.add("active");
  };

  window.openMenuEditModal = function (id) {
    const item = state.menuItems.find(m => m.menuId === id);
    if (!item) return;

    document.getElementById("menuModalTitle").innerText = "Edit Dining Dish";
    document.getElementById("menuFormId").value = item.menuId;
    document.getElementById("menuFormTitle").value = item.title || "";
    document.getElementById("menuFormType").value = item.mealType || "BREAKFAST";
    document.getElementById("menuFormDesc").value = item.mealDescription || "";
    document.getElementById("menuFormPhoto").value = "";
    document.getElementById("menuFormAvailable").checked = Boolean(item.available);
    document.getElementById("menuModal").classList.add("active");
  };

  window.closeMenuModal = function () {
    document.getElementById("menuModal").classList.remove("active");
  };

  window.saveMenuSubmit = async function (event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);

    const submitBtn = document.getElementById("menuFormSubmitBtn");
    submitBtn.disabled = true;
    submitBtn.innerText = "Saving Dish...";

    try {
      const res = await adminFetch("/api/admin/menu/save", {
        method: "POST",
        body: formData
      });
      if (!res.ok) throw new Error("Failed to save menu dish.");
      showToast("Menu dish updated successfully!");
      closeMenuModal();
      loadMenu();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerText = "Save Dish";
    }
  };

  window.toggleMenuAction = async function (id) {
    try {
      const res = await adminFetch(`/api/admin/menu/${id}/toggle`, { method: "POST" });
      if (!res.ok) throw new Error("Failed to toggle menu status.");
      showToast("Menu visibility toggled.");
      loadMenu();
    } catch (err) {
      showToast(err.message, "error");
    }
  };

  window.deleteMenuAction = function (id) {
    showConfirmDialog(
      "Delete Menu Dish",
      "Are you sure you want to permanently delete this dish from the menu?",
      "Delete Dish",
      async () => {
        try {
          const res = await adminFetch(`/api/admin/menu/${id}`, { method: "DELETE" });
          if (!res.ok) throw new Error("Failed to delete menu item.");
          showToast("Menu dish deleted.");
          loadMenu();
        } catch (err) {
          showToast(err.message, "error");
        }
      }
    );
  };

  // ----------------------------------------------------
  // 7. ACTIVITIES & EXPERIENCES CRUD
  // ----------------------------------------------------
  async function loadActivities() {
    const grid = document.getElementById("activityCardsGrid");
    if (!grid) return;
    grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #94A3B8;">Loading activities & experiences...</div>`;

    try {
      const res = await adminFetch("/api/admin/activities");
      if (!res.ok) throw new Error("Failed to load activities.");
      state.activities = await res.json();
      renderActivitiesGrid();
    } catch (err) {
      grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #EF4444;">${err.message}</div>`;
    }
  }

  function renderActivitiesGrid() {
    const grid = document.getElementById("activityCardsGrid");
    if (!grid) return;

    if (state.activities.length === 0) {
      grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 40px; color: #94A3B8;">No activities added yet. Click "+ Add Activity" above to create one.</div>`;
      return;
    }

    grid.innerHTML = state.activities.map(a => {
      const photo = a.imageUrl || "images/activities/bicycle1.JPG";
      const isActive = Boolean(a.active);

      return `
        <div class="item-card">
          <div class="item-img-wrap">
            <img src="${photo}" alt="${a.title}" onerror="this.src='images/hero/front-exterior.jpg'">
            <div class="item-badge-top">
              <span class="badge-pill ${isActive ? 'status-active' : 'status-hidden'}">
                ${isActive ? 'Active' : 'Hidden'}
              </span>
            </div>
          </div>
          <div class="item-card-body">
            <div>
              <span style="font-size: 0.72rem; font-weight: 700; color: #8A7352; text-transform: uppercase; letter-spacing: 0.5px;">${a.pricingNote || 'Included with stay'}</span>
              <h4>${a.title}</h4>
              <p>${a.activityDescription || "No description provided."}</p>
            </div>
            <div class="item-card-footer">
              <button class="btn btn-sm btn-secondary" onclick="toggleActivityAction(${a.activityId})">
                ${isActive ? 'Hide from Guests' : 'Show on Site'}
              </button>
              <div style="display: flex; gap: 6px;">
                <button class="btn btn-sm btn-secondary" onclick="openActivityEditModal(${a.activityId})">Edit</button>
                <button class="btn btn-sm btn-danger" onclick="deleteActivityAction(${a.activityId})">✕</button>
              </div>
            </div>
          </div>
        </div>
      `;
    }).join("");
  }

  window.openActivityModal = function () {
    document.getElementById("activityModalTitle").innerText = "Add New Activity / Experience";
    document.getElementById("activityFormId").value = "";
    document.getElementById("activityFormTitle").value = "";
    document.getElementById("activityFormPrice").value = "Complimentary with stay";
    document.getElementById("activityFormDesc").value = "";
    document.getElementById("activityFormPhoto").value = "";
    document.getElementById("activityFormActive").checked = true;
    document.getElementById("activityModal").classList.add("active");
  };

  window.openActivityEditModal = function (id) {
    const item = state.activities.find(a => a.activityId === id);
    if (!item) return;

    document.getElementById("activityModalTitle").innerText = "Edit Activity / Experience";
    document.getElementById("activityFormId").value = item.activityId;
    document.getElementById("activityFormTitle").value = item.title || "";
    document.getElementById("activityFormPrice").value = item.pricingNote || "";
    document.getElementById("activityFormDesc").value = item.activityDescription || "";
    document.getElementById("activityFormPhoto").value = "";
    document.getElementById("activityFormActive").checked = Boolean(item.active);
    document.getElementById("activityModal").classList.add("active");
  };

  window.closeActivityModal = function () {
    document.getElementById("activityModal").classList.remove("active");
  };

  window.saveActivitySubmit = async function (event) {
    event.preventDefault();
    const form = event.target;
    const formData = new FormData(form);

    const submitBtn = document.getElementById("activityFormSubmitBtn");
    submitBtn.disabled = true;
    submitBtn.innerText = "Saving Activity...";

    try {
      const res = await adminFetch("/api/admin/activities/save", {
        method: "POST",
        body: formData
      });
      if (!res.ok) throw new Error("Failed to save activity.");
      showToast("Activity updated successfully!");
      closeActivityModal();
      loadActivities();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerText = "Save Activity";
    }
  };

  window.toggleActivityAction = async function (id) {
    try {
      const res = await adminFetch(`/api/admin/activities/${id}/toggle`, { method: "POST" });
      if (!res.ok) throw new Error("Failed to toggle activity status.");
      showToast("Activity status toggled.");
      loadActivities();
    } catch (err) {
      showToast(err.message, "error");
    }
  };

  window.deleteActivityAction = function (id) {
    showConfirmDialog(
      "Delete Activity",
      "Are you sure you want to permanently delete this experience?",
      "Delete Activity",
      async () => {
        try {
          const res = await adminFetch(`/api/admin/activities/${id}`, { method: "DELETE" });
          if (!res.ok) throw new Error("Failed to delete activity.");
          showToast("Activity deleted.");
          loadActivities();
        } catch (err) {
          showToast(err.message, "error");
        }
      }
    );
  };

  // ----------------------------------------------------
  // 8. GUEST REVIEWS MODERATION
  // ----------------------------------------------------
  async function loadReviews() {
    const tbody = document.getElementById("reviewsTbody");
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 30px; color: #94A3B8;">Loading reviews...</td></tr>`;

    try {
      const res = await fetch("/api/review/all");
      if (!res.ok) throw new Error("Failed to load reviews.");
      state.reviews = await res.json();
      renderReviewsTable();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 30px; color: #EF4444;">${err.message}</td></tr>`;
    }
  }

  function renderReviewsTable() {
    const tbody = document.getElementById("reviewsTbody");
    if (!tbody) return;

    if (state.reviews.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 32px; color: #94A3B8;">No guest reviews submitted yet.</td></tr>`;
      return;
    }

    tbody.innerHTML = state.reviews.map(r => {
      const statusClass = `status-${(r.status || "").toLowerCase()}`;
      const stars = "★".repeat(r.rating || 5) + "☆".repeat(Math.max(0, 5 - (r.rating || 5)));

      return `
        <tr>
          <td>
            <strong>${r.guestName || "Anonymous"}</strong>
            <div style="font-size: 0.76rem; color: #64748B;">Stay: ${formatDate(r.stayDate)}</div>
          </td>
          <td>
            <div style="color: #D4B46A; font-size: 1.1rem; letter-spacing: 2px;">${stars}</div>
          </td>
          <td style="max-width: 320px;">
            <div style="font-size: 0.84rem; color: #334155; line-height: 1.4;">${r.comment || ""}</div>
          </td>
          <td>
            <span class="badge-pill ${statusClass}">${r.status}</span>
          </td>
          <td>
            <div style="font-size: 0.74rem; color: #94A3B8;">${formatDate(r.createdAt)}</div>
          </td>
          <td>
            <div class="table-actions">
              ${r.status !== "APPROVED" ? `
                <button class="btn btn-sm btn-primary" onclick="moderateReviewAction(${r.reviewId}, 'APPROVED')">Approve</button>
              ` : ""}
              ${r.status !== "REJECTED" ? `
                <button class="btn btn-sm btn-danger" onclick="moderateReviewAction(${r.reviewId}, 'REJECTED')">Reject</button>
              ` : ""}
            </div>
          </td>
        </tr>
      `;
    }).join("");
  }

  window.moderateReviewAction = async function (id, status) {
    try {
      const res = await fetch(`/api/review/${id}/moderate?status=${status}`, { method: "PUT" });
      if (!res.ok) throw new Error("Failed to moderate review.");
      showToast(`Review #${id} ${status.toLowerCase()}.`);
      loadReviews();
    } catch (err) {
      showToast(err.message, "error");
    }
  };

  // ----------------------------------------------------
  // ROOMS HELPER (FOR CALENDAR & PRICING SCOPES)
  // ----------------------------------------------------
  async function loadRooms() {
    if (state.rooms && state.rooms.length > 0) return state.rooms;
    try {
      const res = await fetch("/api/rooms");
      if (res.ok) {
        state.rooms = await res.json();
        populateRoomDropdowns();
      }
    } catch (err) {
      console.warn("Could not fetch rooms list:", err);
    }
    return state.rooms;
  }

  function populateRoomDropdowns() {
    const blockSelect = document.getElementById("blockRoomSelect");
    const rateSelect = document.getElementById("rateRoomSelect");
    const filterSelect = document.getElementById("blockRoomFilter");

    const room1 = (state.rooms || []).find(r => r.roomNumber === "01" || r.roomNumber === "1" || (r.roomType && r.roomType.toLowerCase().includes("front")));
    const room2 = (state.rooms || []).find(r => r.roomNumber === "02" || r.roomNumber === "2" || (r.roomType && r.roomType.toLowerCase().includes("cozy")));

    const room1Id = room1 ? room1.roomId : 1;
    const room2Id = room2 ? room2.roomId : 2;

    if (blockSelect) {
      const cur = blockSelect.value;
      blockSelect.innerHTML = `
        <option value="">Entire Villa (Property-Wide Maintenance)</option>
        <option value="${room1Id}">Room 01 (The Front Loft)</option>
        <option value="${room2Id}">Room 02 (The Cozy Loft)</option>
      `;
      if (cur) blockSelect.value = cur;
    }

    if (rateSelect) {
      const cur = rateSelect.value;
      rateSelect.innerHTML = `
        <option value="">Entire Villa / All Rooms</option>
        <option value="${room1Id}">Room 01 (The Front Loft)</option>
        <option value="${room2Id}">Room 02 (The Cozy Loft)</option>
      `;
      if (cur) rateSelect.value = cur;
    }

    if (filterSelect) {
      const currentVal = filterSelect.value || "ALL";
      filterSelect.innerHTML = `
        <option value="ALL">All Scopes / Entire Villa</option>
        <option value="01">Room 01 (The Front Loft)</option>
        <option value="02">Room 02 (The Cozy Loft)</option>
      `;
      filterSelect.value = currentVal;
    }
  }

  // ----------------------------------------------------
  // 9. CALENDAR BLOCKS & AVAILABILITY ENGINE
  // ----------------------------------------------------
  async function loadCalendarBlocks() {
    const tbody = document.getElementById("calendarBlocksTbody");
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 30px; color: #94A3B8;">Loading calendar blocks...</td></tr>`;

    try {
      await loadRooms();
      const res = await adminFetch("/api/admin/calendar/blocks");
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || "Failed to load calendar blocks.");
      }
      state.calendarBlocks = await res.json();
      renderCalendarBlocksTable();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 30px; color: #EF4444;">${err.message}</td></tr>`;
    }
  }

  window.handleBlockFilter = function () {
    state.blockSearchQuery = (document.getElementById("blockSearchInput").value || "").toLowerCase().trim();
    state.blockRoomFilter = document.getElementById("blockRoomFilter").value;
    state.blockCurrentPage = 1;
    renderCalendarBlocksTable();
  };

  function renderCalendarBlocksTable() {
    const tbody = document.getElementById("calendarBlocksTbody");
    if (!tbody) return;

    let filtered = state.calendarBlocks.filter(b => {
      const roomText = (b.roomNumber || "Entire Villa").toLowerCase();
      const sourceText = (b.source || "").toLowerCase();
      const dateText = (b.blockedDate || "").toLowerCase();
      const idText = String(b.blockId || "");

      const matchesSearch = !state.blockSearchQuery ||
        idText.includes(state.blockSearchQuery) ||
        dateText.includes(state.blockSearchQuery) ||
        sourceText.includes(state.blockSearchQuery) ||
        roomText.includes(state.blockSearchQuery);

      let matchesScope = true;
      if (state.blockRoomFilter && state.blockRoomFilter !== "ALL") {
        if (state.blockRoomFilter === "01") {
          matchesScope = b.roomNumber === "01" || b.roomNumber === "1" || b.roomId === 1 || (b.roomNumber && b.roomNumber.toLowerCase().includes("front"));
        } else if (state.blockRoomFilter === "02") {
          matchesScope = b.roomNumber === "02" || b.roomNumber === "2" || b.roomId === 2 || (b.roomNumber && b.roomNumber.toLowerCase().includes("cozy"));
        } else {
          matchesScope = b.roomNumber === state.blockRoomFilter;
        }
      }

      return matchesSearch && matchesScope;
    });

    const total = filtered.length;
    const startIndex = (state.blockCurrentPage - 1) * state.blockPageSize;
    const paged = filtered.slice(startIndex, startIndex + state.blockPageSize);

    const countNotice = document.getElementById("blockCountNotice");
    if (countNotice) {
      countNotice.innerText = total > 0
        ? `Showing ${Math.min(startIndex + 1, total)}–${Math.min(startIndex + paged.length, total)} of ${total} calendar blocks`
        : "No calendar blocks to display";
    }

    const prevBtn = document.getElementById("blockPrevBtn");
    const nextBtn = document.getElementById("blockNextBtn");
    if (prevBtn) prevBtn.disabled = state.blockCurrentPage <= 1;
    if (nextBtn) nextBtn.disabled = startIndex + state.blockPageSize >= total;

    if (paged.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 32px; color: #94A3B8;">No calendar blocks found.</td></tr>`;
      return;
    }

    tbody.innerHTML = paged.map(b => {
      const isVilla = !b.roomNumber || b.roomNumber === "Entire Villa";
      let roomDisplay = "Entire Villa";
      if (!isVilla) {
        if (b.roomNumber === "01" || b.roomNumber === "1" || (b.roomNumber && b.roomNumber.includes("Front")) || b.roomId === 1) {
          roomDisplay = "Room 01 (The Front Loft)";
        } else if (b.roomNumber === "02" || b.roomNumber === "2" || (b.roomNumber && b.roomNumber.includes("Cozy")) || b.roomId === 2) {
          roomDisplay = "Room 02 (The Cozy Loft)";
        } else {
          roomDisplay = b.roomNumber;
        }
      }
      const scopeBadge = isVilla
        ? `<span class="badge-pill status-unpaid" style="background:#EEF2FF; color:#4338CA; border:1px solid #C7D2FE;">Entire Villa</span>`
        : `<span class="badge-pill status-active">${roomDisplay}</span>`;

      return `
        <tr>
          <td><strong>#${b.blockId}</strong></td>
          <td>
            <div style="font-weight: 600; color: #1B3B2B;">${formatDate(b.blockedDate)}</div>
            <div style="font-size: 0.72rem; color: #94A3B8;">${b.blockedDate || ''}</div>
          </td>
          <td>${scopeBadge}</td>
          <td style="max-width: 280px;">
            <div style="font-size: 0.85rem; color: #334155;">${b.source || 'MANUAL_ADMIN'}</div>
            ${b.externalUid ? `<div style="font-size: 0.72rem; color: #94A3B8; font-family: monospace;">UID: ${b.externalUid.substring(0, 15)}...</div>` : ''}
          </td>
          <td>
            <div style="font-size: 0.76rem; color: #64748B;">${formatDateTime(b.createdAt)}</div>
          </td>
          <td>
            <div class="table-actions">
              <button class="btn btn-sm btn-danger" onclick="deleteBlock(${b.blockId})" title="Unblock Date">
                Unblock / Delete
              </button>
            </div>
          </td>
        </tr>
      `;
    }).join("");
  }

  window.blockPagePrev = function () {
    if (state.blockCurrentPage > 1) {
      state.blockCurrentPage--;
      renderCalendarBlocksTable();
    }
  };

  window.blockPageNext = function () {
    state.blockCurrentPage++;
    renderCalendarBlocksTable();
  };

  window.openBlockModal = async function () {
    await loadRooms();
    populateRoomDropdowns();

    const form = document.getElementById("blockForm");
    if (form) form.reset();

    const todayStr = new Date().toISOString().split("T")[0];
    const startInput = document.getElementById("blockStartDate");
    const endInput = document.getElementById("blockEndDate");
    if (startInput) startInput.value = todayStr;
    if (endInput) endInput.value = todayStr;

    document.getElementById("blockModalTitle").innerText = "Add Calendar Block";
    document.getElementById("blockModal").classList.add("active");
  };

  window.closeBlockModal = function () {
    document.getElementById("blockModal").classList.remove("active");
  };

  window.submitBlock = async function (event) {
    event.preventDefault();
    const startDate = document.getElementById("blockStartDate").value;
    const endDate = document.getElementById("blockEndDate").value;
    const roomIdVal = document.getElementById("blockRoomSelect").value;
    const reason = document.getElementById("blockReason").value.trim();

    if (!startDate || !endDate) {
      showToast("Please specify both start date and end date.", "warning");
      return;
    }
    if (new Date(endDate) < new Date(startDate)) {
      showToast("End date cannot be earlier than start date.", "warning");
      return;
    }

    const submitBtn = document.getElementById("blockSubmitBtn");
    submitBtn.disabled = true;
    submitBtn.innerText = "Blocking Dates...";

    try {
      const payload = {
        startDate: startDate,
        endDate: endDate,
        roomId: roomIdVal ? parseInt(roomIdVal, 10) : null,
        reason: reason
      };

      const res = await adminFetch("/api/admin/calendar/blocks", {
        method: "POST",
        body: JSON.stringify(payload)
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || "Failed to block dates.");
      }

      const data = await res.json();
      showToast(data.message || "Calendar block(s) created successfully!");
      closeBlockModal();
      loadCalendarBlocks();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerText = "Save Block";
    }
  };

  window.deleteBlock = function (blockId) {
    showConfirmDialog(
      "Unblock Calendar Date",
      `Are you sure you want to delete calendar block <strong>#${blockId}</strong> and release this date for bookings?`,
      "Unblock / Delete",
      async () => {
        try {
          const res = await adminFetch(`/api/admin/calendar/blocks/${blockId}`, { method: "DELETE" });
          if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || "Failed to delete calendar block.");
          }
          showToast("Calendar block removed successfully.");
          loadCalendarBlocks();
        } catch (err) {
          showToast(err.message, "error");
        }
      }
    );
  };

  // ----------------------------------------------------
  // 10. DYNAMIC PRICING ENGINE & PACKAGES
  // ----------------------------------------------------
  async function loadRateOverrides() {
    const tbody = document.getElementById("ratesTbody");
    if (!tbody) return;
    tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 30px; color: #94A3B8;">Loading pricing rules and packages...</td></tr>`;

    try {
      await loadRooms();
      const res = await adminFetch("/api/admin/rates/overrides");
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || "Failed to load rate overrides.");
      }
      state.rateOverrides = await res.json();
      renderRateOverridesTable();
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 30px; color: #EF4444;">${err.message}</td></tr>`;
    }
  }

  window.handleRateFilter = function () {
    state.rateSearchQuery = (document.getElementById("rateSearchInput").value || "").toLowerCase().trim();
    state.rateStatusFilter = document.getElementById("rateStatusSelect").value;
    renderRateOverridesTable();
  };

  function renderRateOverridesTable() {
    const tbody = document.getElementById("ratesTbody");
    if (!tbody) return;

    let filtered = state.rateOverrides.filter(r => {
      const nameText = (r.packageName || "").toLowerCase();
      const descText = (r.packageDescription || "").toLowerCase();
      const idText = String(r.rateId || "");

      const matchesSearch = !state.rateSearchQuery ||
        idText.includes(state.rateSearchQuery) ||
        nameText.includes(state.rateSearchQuery) ||
        descText.includes(state.rateSearchQuery);

      let matchesStatus = true;
      if (state.rateStatusFilter === "ACTIVE") matchesStatus = Boolean(r.isActive);
      if (state.rateStatusFilter === "INACTIVE") matchesStatus = !Boolean(r.isActive);

      return matchesSearch && matchesStatus;
    });

    if (filtered.length === 0) {
      tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; padding: 32px; color: #94A3B8;">No dynamic pricing rules or packages found. Click "+ Add Pricing Rule" above to create one.</td></tr>`;
      return;
    }

    tbody.innerHTML = filtered.map(r => {
      const isActive = Boolean(r.isActive);
      const statusClass = isActive ? "status-active" : "status-unpaid";
      const mult = Number(r.rateMultiplier || 1.0);
      const multColor = mult > 1.0 ? "#B45309" : (mult < 1.0 ? "#059669" : "#1B3B2B");
      const multLabel = mult > 1.0 ? `+${Math.round((mult - 1) * 100)}% Surge` : (mult < 1.0 ? `-${Math.round((1 - mult) * 100)}% Discount` : "Standard");

      let scopeDisplay = "Entire Villa / All Rooms";
      if (r.roomNumber && r.roomNumber !== "Entire Villa" && r.roomNumber !== "Entire Villa / All Rooms") {
        if (r.roomNumber === "01" || r.roomNumber === "1" || (typeof r.roomNumber === "string" && r.roomNumber.includes("Front")) || r.roomId === 1) {
          scopeDisplay = "Room 01 (The Front Loft)";
        } else if (r.roomNumber === "02" || r.roomNumber === "2" || (typeof r.roomNumber === "string" && r.roomNumber.includes("Cozy")) || r.roomId === 2) {
          scopeDisplay = "Room 02 (The Cozy Loft)";
        } else {
          scopeDisplay = r.roomNumber;
        }
      }

      return `
        <tr>
          <td><strong>#${r.rateId}</strong></td>
          <td>
            <div style="font-weight: 600; color: #1B3B2B;">${r.packageName || "Custom Override"}</div>
            <div style="font-size: 0.76rem; color: #64748B; max-width: 220px; line-height: 1.3;">${r.packageDescription || "No description"}</div>
          </td>
          <td>${formatDate(r.startDate)}</td>
          <td>${formatDate(r.endDate)}</td>
          <td>
            <strong style="color: ${multColor}; font-size: 0.95rem;">${mult.toFixed(2)}x</strong>
            <div style="font-size: 0.72rem; color: #94A3B8;">${multLabel}</div>
          </td>
          <td>${r.minNights || 1} Night${(r.minNights || 1) > 1 ? 's' : ''}</td>
          <td>
            <span style="font-size: 0.8rem; color: #1B3B2B; font-weight: 500;">${scopeDisplay}</span>
          </td>
          <td>
            <span class="badge-pill ${statusClass}">${isActive ? 'ACTIVE' : 'INACTIVE'}</span>
          </td>
          <td>
            <div class="table-actions">
              <button class="btn btn-sm btn-secondary" onclick="openRateModal(true, ${r.rateId})" title="Edit Rule">Edit</button>
              <button class="btn btn-sm btn-secondary" onclick="toggleRate(${r.rateId})" title="Toggle Active">
                ${isActive ? 'Deactivate' : 'Activate'}
              </button>
              <button class="btn btn-sm btn-danger" onclick="deleteRate(${r.rateId})" title="Delete Rule">✕</button>
            </div>
          </td>
        </tr>
      `;
    }).join("");
  }

  window.openRateModal = async function (isEdit = false, rateIdOrData = null) {
    await loadRooms();
    populateRoomDropdowns();

    const form = document.getElementById("rateForm");
    if (form) form.reset();

    let rateObj = null;
    if (isEdit) {
      if (typeof rateIdOrData === "object" && rateIdOrData !== null) {
        rateObj = rateIdOrData;
      } else if (rateIdOrData !== null) {
        rateObj = state.rateOverrides.find(item => item.rateId === rateIdOrData);
      }
    }

    if (isEdit && rateObj) {
      document.getElementById("rateModalTitle").innerText = "Edit Dynamic Pricing Rule";
      document.getElementById("rateFormId").value = rateObj.rateId || "";
      document.getElementById("ratePackageName").value = rateObj.packageName || "";
      document.getElementById("rateDescription").value = rateObj.packageDescription || "";
      document.getElementById("rateStartDate").value = rateObj.startDate || "";
      document.getElementById("rateEndDate").value = rateObj.endDate || "";
      document.getElementById("rateMultiplier").value = Number(rateObj.rateMultiplier || 1.0).toFixed(2);
      document.getElementById("rateMinNights").value = rateObj.minNights || 1;
      document.getElementById("rateRoomSelect").value = rateObj.roomId || "";
      document.getElementById("rateIsActive").checked = Boolean(rateObj.isActive);
    } else {
      document.getElementById("rateModalTitle").innerText = "Add Dynamic Pricing Rule";
      document.getElementById("rateFormId").value = "";
      document.getElementById("ratePackageName").value = "";
      document.getElementById("rateDescription").value = "";

      const todayStr = new Date().toISOString().split("T")[0];
      document.getElementById("rateStartDate").value = todayStr;
      document.getElementById("rateEndDate").value = todayStr;
      document.getElementById("rateMultiplier").value = "1.00";
      document.getElementById("rateMinNights").value = "1";
      document.getElementById("rateRoomSelect").value = "";
      document.getElementById("rateIsActive").checked = true;
    }

    document.getElementById("rateModal").classList.add("active");
  };

  window.closeRateModal = function () {
    document.getElementById("rateModal").classList.remove("active");
  };

  window.submitRate = async function (event) {
    event.preventDefault();
    const rateId = document.getElementById("rateFormId").value;
    const packageName = document.getElementById("ratePackageName").value.trim();
    const packageDescription = document.getElementById("rateDescription").value.trim();
    const startDate = document.getElementById("rateStartDate").value;
    const endDate = document.getElementById("rateEndDate").value;
    const rateMultiplier = parseFloat(document.getElementById("rateMultiplier").value);
    const minNights = parseInt(document.getElementById("rateMinNights").value, 10);
    const roomIdVal = document.getElementById("rateRoomSelect").value;
    const isActive = document.getElementById("rateIsActive").checked;

    if (!packageName) {
      showToast("Package or rule name is required.", "warning");
      return;
    }
    if (!startDate || !endDate) {
      showToast("Both start date and end date are required.", "warning");
      return;
    }
    if (new Date(endDate) < new Date(startDate)) {
      showToast("End date cannot be earlier than start date.", "warning");
      return;
    }

    const submitBtn = document.getElementById("rateSubmitBtn");
    submitBtn.disabled = true;
    submitBtn.innerText = "Saving Rule...";

    try {
      const payload = {
        packageName,
        packageDescription,
        startDate,
        endDate,
        rateMultiplier,
        minNights: minNights || 1,
        roomId: roomIdVal ? parseInt(roomIdVal, 10) : null,
        isActive
      };

      let res;
      if (rateId) {
        res = await adminFetch(`/api/admin/rates/overrides/${rateId}`, {
          method: "PUT",
          body: JSON.stringify(payload)
        });
      } else {
        res = await adminFetch("/api/admin/rates/overrides", {
          method: "POST",
          body: JSON.stringify(payload)
        });
      }

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || "Failed to save rate override.");
      }

      showToast(rateId ? "Pricing rule updated successfully!" : "Dynamic pricing rule created successfully!");
      closeRateModal();
      loadRateOverrides();
    } catch (err) {
      showToast(err.message, "error");
    } finally {
      submitBtn.disabled = false;
      submitBtn.innerText = "Save Pricing Rule";
    }
  };

  window.toggleRate = async function (rateId) {
    try {
      const res = await adminFetch(`/api/admin/rates/overrides/${rateId}/toggle`, { method: "POST" });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || "Failed to toggle rule status.");
      }
      showToast("Rate override status toggled.");
      loadRateOverrides();
    } catch (err) {
      showToast(err.message, "error");
    }
  };

  window.deleteRate = function (rateId) {
    showConfirmDialog(
      "Delete Pricing Rule",
      `Are you sure you want to permanently delete rate override <strong>#${rateId}</strong>?`,
      "Delete Rule",
      async () => {
        try {
          const res = await adminFetch(`/api/admin/rates/overrides/${rateId}`, { method: "DELETE" });
          if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || "Failed to delete rate override.");
          }
          showToast("Rate override rule deleted.");
          loadRateOverrides();
        } catch (err) {
          showToast(err.message, "error");
        }
      }
    );
  };

  // ----------------------------------------------------
  // ADMIN SECURITY MODAL
  // ----------------------------------------------------
  function showAdminKeyModal() {
    const modal = document.getElementById("adminKeyModal");
    if (modal) modal.classList.add("active");
  }

  window.submitAdminKeyForm = async function (event) {
    event.preventDefault();
    const key = document.getElementById("adminKeyInput").value.trim();
    const feedback = document.getElementById("adminKeyFeedback");

    try {
      const res = await fetch("/api/admin/auth/verify", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: `key=${encodeURIComponent(key)}`
      });

      if (res.ok) {
        state.adminKey = key;
        localStorage.setItem("loft_admin_key", key);
        document.getElementById("adminKeyModal").classList.remove("active");
        showToast("Administrative key verified.");
        switchTab(state.currentTab);
      } else {
        feedback.innerText = "Invalid administrative key. Please try again.";
        feedback.style.display = "block";
      }
    } catch (err) {
      feedback.innerText = "Unable to connect to server.";
      feedback.style.display = "block";
    }
  };

  // ----------------------------------------------------
  // INITIALIZATION ON LOAD
  // ----------------------------------------------------
  document.addEventListener("DOMContentLoaded", () => {
    // Pre-load rooms for dropdown filters & modals
    loadRooms();

    // Check hash or URL query param for tab
    const urlParams = new URLSearchParams(window.location.search);
    const queryTab = urlParams.get("tab");
    const hashTab = (window.location.hash || "").replace("#", "");
    const initialTab = queryTab || hashTab || window.SERVER_DEFAULT_TAB || "overview";

    switchTab(initialTab);
  });

})();
