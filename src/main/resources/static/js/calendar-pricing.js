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
      }
    } catch (err) {
      // Local fallback
    }
  }

  function renderPackageBadges() {
    const container = document.getElementById("calendarPackagesContainer");
    if (!container || !activePackagesList.length) return;

    container.innerHTML = activePackagesList.map(pkg => `
      <span class="package-badge">
        🏷️ ${pkg.packageTitle || 'Seasonal Rate'} (${pkg.rateMultiplier}x)
      </span>
    `).join("");
  }

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