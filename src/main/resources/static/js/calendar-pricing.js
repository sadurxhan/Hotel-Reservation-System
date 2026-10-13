/**
 * ============================================================================
 * MEMBER 2: CALENDAR AVAILABILITY & DYNAMIC PRICING ENGINE CLIENT
 * ============================================================================
 */

(function () {
  "use strict";

  let currentViewDate = new Date();
  let blockedDatesSet = new Set();
  let selectedCheckIn = null;
  let selectedCheckOut = null;
  let activePackagesList = [];

  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ];

  function formatDateKey(dateObj) {
    const year = dateObj.getFullYear();
    const month = String(dateObj.getMonth() + 1).padStart(2, "0");
    const day = String(dateObj.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }

  async function loadActivePackages() {
    try {
      const response = await fetch("/api/pricing/packages");
      if (response.ok) {
        activePackagesList = await response.json();
        renderPackageBadges();
        renderPackagesShowcase();
      } else {
        renderPackagesShowcase();
      }
    } catch (err) {
      renderPackagesShowcase();
    }
  }

  function renderPackageBadges() {
    const container = document.getElementById("calendarPackagesContainer");
    if (!container) return;
    if (!activePackagesList.length) {
      container.innerHTML = "";
      return;
    }

    container.innerHTML = activePackagesList.map(pkg => `
      <span class="package-badge">
        🏷️ ${pkg.packageName || pkg.packageTitle || 'Seasonal Rate'} (${Number(pkg.rateMultiplier || 1.0).toFixed(2)}x)
      </span>
    `).join("");
  }

  function formatDateDisplay(dateStr) {
    if (!dateStr) return "";
    const parts = String(dateStr).split("-");
    if (parts.length === 3) {
      const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
      const mIdx = parseInt(parts[1], 10) - 1;
      return `${months[mIdx] || parts[1]} ${parseInt(parts[2], 10)}, ${parts[0]}`;
    }
    return dateStr;
  }

  function renderPackagesShowcase() {
    const grid = document.getElementById("activePackagesGrid");
    const placeholder = document.getElementById("noPackagesPlaceholder");
    if (!grid) return;

    if (!activePackagesList || activePackagesList.length === 0) {
      grid.style.display = "none";
      if (placeholder) placeholder.style.display = "block";
      return;
    }

    grid.style.display = "grid";
    if (placeholder) placeholder.style.display = "none";

    grid.innerHTML = activePackagesList.map(pkg => {
      const mult = Number(pkg.rateMultiplier || 1.0);
      let badgeHtml = "";
      if (mult < 1.0) {
        const discount = Math.round((1.0 - mult) * 100);
        badgeHtml = `<span class="pkg-card-badge discount">${discount}% OFF DISCOUNT</span>`;
      } else if (mult > 1.0) {
        const surge = Math.round((mult - 1.0) * 100);
        badgeHtml = `<span class="pkg-card-badge peak">+${surge}% PEAK SEASON SURGE</span>`;
      } else {
        badgeHtml = `<span class="pkg-card-badge special">EXCLUSIVE SPECIAL</span>`;
      }

      const minNights = pkg.minNights || 1;
      const minStayLabel = `${minNights} night${minNights > 1 ? 's' : ''}`;

      let roomScope = "Entire Villa / Both Lofts";
      if (pkg.room) {
        const rNum = String(pkg.room.roomNumber || "");
        if (rNum === "01" || rNum === "1" || (pkg.room.roomType && pkg.room.roomType.toLowerCase().includes("front"))) {
          roomScope = "Room 01 (The Front Loft)";
        } else if (rNum === "02" || rNum === "2" || (pkg.room.roomType && pkg.room.roomType.toLowerCase().includes("cozy"))) {
          roomScope = "Room 02 (The Cozy Loft)";
        } else {
          roomScope = `Room ${rNum}`;
        }
      }

      const datesText = (pkg.startDate && pkg.endDate)
        ? `<div class="pkg-dates">📅 Valid: ${formatDateDisplay(pkg.startDate)} – ${formatDateDisplay(pkg.endDate)}</div>`
        : "";

      const safeName = (pkg.packageName || "Seasonal Package").replace(/"/g, '&quot;');
      const safeDesc = (pkg.packageDescription || "Exclusive promotional stay experience with Lakefront dining and scenic views.").replace(/"/g, '&quot;');

      return `
        <div class="package-card">
          <div class="pkg-card-top">
            ${badgeHtml}
            <span class="pkg-scope-tag">${roomScope}</span>
          </div>
          <h3 class="pkg-title">${safeName}</h3>
          <p class="pkg-desc">${safeDesc}</p>
          ${datesText}
          <div class="pkg-card-footer">
            <div class="pkg-condition">
              <span class="condition-label">Condition:</span>
              <span class="condition-val">Min stay: ${minStayLabel}</span>
            </div>
            <button type="button" class="btn btn-gold pkg-cta-btn" onclick="selectPackageAndScroll('${safeName}')">
              Select Package &amp; Book Dates &darr;
            </button>
          </div>
        </div>
      `;
    }).join("");
  }

  window.selectPackageAndScroll = function (packageName) {
    const calendarEl = document.getElementById("calendar");
    if (calendarEl) {
      calendarEl.scrollIntoView({ behavior: "smooth", block: "start" });
    }
    showCalendarNotice(`Selected offer: ${packageName}. Choose your stay dates on the calendar below.`, "success");
  };

  window.scrollToCalendar = function () {
    const calendarEl = document.getElementById("calendar");
    if (calendarEl) {
      calendarEl.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  };

  async function fetchMonthlyBlocks(year, month) {
    const firstDay = `${year}-${String(month + 1).padStart(2, "0")}-01`;
    const lastDayObj = new Date(year, month + 1, 0);
    const lastDay = `${year}-${String(month + 1).padStart(2, "0")}-${String(lastDayObj.getDate()).padStart(2, "0")}`;

    try {
      const res = await fetch(`/api/calendar/blocks?startDate=${firstDay}&endDate=${lastDay}`);
      if (res.ok) {
        const blocks = await res.json();
        blockedDatesSet = new Set(blocks.map(b => b.blockedDate));
      }
    } catch (error) {
      blockedDatesSet = new Set();
    }

    renderCalendarGrid();
  }

  function renderCalendarGrid() {
    const year = currentViewDate.getFullYear();
    const month = currentViewDate.getMonth();

    const titleEl = document.getElementById("calCurrentMonthYear");
    if (titleEl) {
      titleEl.innerText = `${monthNames[month]} ${year}`;
    }

    const grid = document.getElementById("calendarGrid");
    if (!grid) return;

    while (grid.children.length > 7) {
      grid.removeChild(grid.lastChild);
    }

    const firstDayIndex = new Date(year, month, 1).getDay();
    const totalDaysInMonth = new Date(year, month + 1, 0).getDate();
    const todayStr = formatDateKey(new Date());

    for (let i = 0; i < firstDayIndex; i++) {
      const emptyCell = document.createElement("div");
      emptyCell.className = "cal-cell cal-empty";
      grid.appendChild(emptyCell);
    }

    for (let day = 1; day <= totalDaysInMonth; day++) {
      const dateStr = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
      const cell = document.createElement("div");
      cell.className = "cal-cell";
      cell.innerText = day;

      const isPast = dateStr < todayStr;
      const isBlocked = blockedDatesSet.has(dateStr);

      if (isPast) {
        cell.classList.add("cal-past");
      } else if (isBlocked) {
        cell.classList.add("cal-blocked");
      } else {
        cell.classList.add("cal-available");
        cell.onclick = () => onDateClicked(dateStr);
      }

      if (dateStr === selectedCheckIn || dateStr === selectedCheckOut) {
        cell.classList.add("cal-selected");
      } else if (selectedCheckIn && selectedCheckOut && dateStr > selectedCheckIn && dateStr < selectedCheckOut) {
        cell.classList.add("cal-range-mid");
      }

      grid.appendChild(cell);
    }
  }

  function onDateClicked(dateStr) {
    clearCalendarNotice();

    if (!selectedCheckIn || (selectedCheckIn && selectedCheckOut)) {
      selectedCheckIn = dateStr;
      selectedCheckOut = null;
    } else if (selectedCheckIn && !selectedCheckOut) {
      if (dateStr <= selectedCheckIn) {
        selectedCheckIn = dateStr;
      } else {
        if (hasIntermediateBlocks(selectedCheckIn, dateStr)) {
          showCalendarNotice("The selected range spans booked dates. Selection reset.", "error");
          selectedCheckIn = dateStr;
          selectedCheckOut = null;
        } else {
          selectedCheckOut = dateStr;
          verifyAvailabilityWithBackend(selectedCheckIn, selectedCheckOut);
        }
      }
    }

    updateSelectionSummary();
    renderCalendarGrid();
  }

  function hasIntermediateBlocks(startDateStr, endDateStr) {
    let curr = new Date(startDateStr);
    const end = new Date(endDateStr);

    while (curr < end) {
      const key = formatDateKey(curr);
      if (blockedDatesSet.has(key)) return true;
      curr.setDate(curr.getDate() + 1);
    }
    return false;
  }

  async function verifyAvailabilityWithBackend(checkIn, checkOut) {
    try {
      const res = await fetch(`/api/calendar/check-availability?checkIn=${checkIn}&checkOut=${checkOut}`);
      if (res.ok) {
        const data = await res.json();
        if (!data.available) {
          showCalendarNotice("Dates in this range were just booked.", "error");
          selectedCheckOut = null;
          updateSelectionSummary();
          renderCalendarGrid();
          return;
        }
      }
    } catch (e) {
      // Offline fallback: allow local selection
    }
    updateSelectionSummary();
  }

  function updateSelectionSummary() {
    const inEl = document.getElementById("calSpanCheckIn");
    const outEl = document.getElementById("calSpanCheckOut");
    const reserveBtn = document.getElementById("btnMakeReservation");

    if (inEl) inEl.innerText = selectedCheckIn || "Select arrival";
    if (outEl) outEl.innerText = selectedCheckOut || "Select departure";

    // Show button once both check-in and check-out are set
    if (reserveBtn) {
      if (selectedCheckIn && selectedCheckOut) {
        reserveBtn.style.display = "inline-flex";
      } else {
        reserveBtn.style.display = "none";
      }
    }
  }

  function showCalendarNotice(msg, type) {
    let box = document.getElementById("calNoticeBox");
    if (!box) {
      box = document.createElement("div");
      box.id = "calNoticeBox";
      document.querySelector(".calendar-card").appendChild(box);
    }
    box.className = `cal-notice-box ${type}`;
    box.innerText = msg;
  }

  function clearCalendarNotice() {
    const box = document.getElementById("calNoticeBox");
    if (box) {
      box.className = "cal-notice-box";
      box.style.display = "none";
      box.innerText = "";
    }
  }

  window.calculateStayDynamicPrice = async function (roomId, checkIn, checkOut) {
    if (!roomId || !checkIn || !checkOut) return null;
    try {
      const response = await fetch(
        `/api/pricing/calculate?roomId=${encodeURIComponent(roomId)}&checkInDate=${encodeURIComponent(checkIn)}&checkOutDate=${encodeURIComponent(checkOut)}`
      );
      if (response.ok) return await response.json();
    } catch (err) {
      // Handled via local fallback calculation
    }
    return null;
  };

  window.getSelectedStayDates = function () {
    return {
      checkInDate: selectedCheckIn,
      checkOutDate: selectedCheckOut
    };
  };

  document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("calPrevBtn")?.addEventListener("click", () => {
      currentViewDate.setMonth(currentViewDate.getMonth() - 1);
      fetchMonthlyBlocks(currentViewDate.getFullYear(), currentViewDate.getMonth());
    });

    document.getElementById("calNextBtn")?.addEventListener("click", () => {
      currentViewDate.setMonth(currentViewDate.getMonth() + 1);
      fetchMonthlyBlocks(currentViewDate.getFullYear(), currentViewDate.getMonth());
    });

    loadActivePackages();
    fetchMonthlyBlocks(currentViewDate.getFullYear(), currentViewDate.getMonth());
  });
})();