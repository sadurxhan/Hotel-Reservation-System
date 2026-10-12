/**
 * ============================================================================
 * MEMBER 1: ROOM SHOWCASE, CAPACITY LOGIC & RESERVATION INTAKE CLIENT
 * ============================================================================
 * Handles:
 * 1. Room card selection and pre-filling the booking intake drawer
 * 2. Party-size constraint rule:
 *    - <= 5 guests: Can select 1 room or both
 *    - > 5 guests: Automatically locks to Entire Lodge (Room ID 3 / Both)
 * 3. Client-side field validations (email, phone, required fields)
 * 4. Calls Member 2's dynamic pricing calculator for instant stay estimate
 * 5. POSTs to Member 1 endpoint (/api/reservations)
 * 6. Sets the checkout session key and redirects to Member 3's payment page
 * ============================================================================
 */

(function () {
  "use strict";

  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const phonePattern = /^[0-9+\-\s()]{7,}$/;

  // 1. Hook for card buttons: Select room and scroll to calendar
  window.selectRoomAndScroll = function (roomId, roomName) {
    const calendarSection = document.getElementById("calendar");
    if (calendarSection) {
      calendarSection.scrollIntoView({ behavior: "smooth" });
    }
    // Store requested room for modal pre-fill
    sessionStorage.setItem("preferred_room_id", roomId ? String(roomId) : "3");
  };

  // 2. Open the intake modal once dates are validated in the calendar
  window.openBookingDrawer = function () {
    const stayDates = window.getSelectedStayDates ? window.getSelectedStayDates() : null;
    if (!stayDates || !stayDates.checkInDate || !stayDates.checkOutDate) {
      alert("Please select both Check-in and Check-out dates on the calendar first.");
      return;
    }

    const checkInInput = document.getElementById("formCheckIn");
    const checkOutInput = document.getElementById("formCheckOut");
    if (checkInInput) checkInInput.value = stayDates.checkInDate;
    if (checkOutInput) checkOutInput.value = stayDates.checkOutDate;

    // Apply preferred room if clicked from room cards
    const preferredRoom = sessionStorage.getItem("preferred_room_id");
    const roomSelect = document.getElementById("formRoomSelect");
    if (roomSelect && preferredRoom) {
      roomSelect.value = preferredRoom;
    }

    handleGuestCountChange();
    recalculateDynamicPrice();

    const modal = document.getElementById("bookingModal");
    if (modal) modal.classList.add("active");
  };

  window.closeBookingDrawer = function () {
    const modal = document.getElementById("bookingModal");
    if (modal) modal.classList.remove("active");
  };

  // 3. Room capacity rule enforcement
  window.handleGuestCountChange = function () {
    const guestInput = document.getElementById("formGuestCount");
    const roomSelect = document.getElementById("formRoomSelect");
    const guestErr = document.getElementById("errGuests");

    if (!guestInput || !roomSelect) return;
    const count = parseInt(guestInput.value, 10) || 1;

    if (count > 5) {
      // More than 5 guests requires booking both suites / entire lodge
      roomSelect.value = "3"; // Entire Lodge
      // Disable single suites
      Array.from(roomSelect.options).forEach(opt => {
        if (opt.value === "1" || opt.value === "2") {
          opt.disabled = true;
        }
      });
      if (guestErr) {
        guestErr.innerText = "Parties exceeding 5 guests require reserving the Entire Lodge.";
      }
    } else {
      // 5 or fewer guests: allow any room choice
      Array.from(roomSelect.options).forEach(opt => opt.disabled = false);
      if (guestErr) guestErr.innerText = "";
    }

    recalculateDynamicPrice();
  };

  // 4. Request dynamic pricing estimation from Member 2's API
  window.recalculateDynamicPrice = async function () {
    const checkIn = document.getElementById("formCheckIn")?.value;
    const checkOut = document.getElementById("formCheckOut")?.value;
    const roomSelect = document.getElementById("formRoomSelect");
    const liveTotalEl = document.getElementById("pricingLiveTotal");
    const nightsEl = document.getElementById("pricingNightsCount");
    const tierStatusEl = document.getElementById("pricingTierStatus");

    if (!checkIn || !checkOut || !roomSelect || !liveTotalEl) return;

    // Calculate nights
    const start = new Date(checkIn);
    const end = new Date(checkOut);
    const nights = Math.max(1, Math.round((end - start) / (1000 * 60 * 60 * 24)));
    if (nightsEl) nightsEl.innerText = `${nights} Night${nights > 1 ? "s" : ""}`;

    const roomId = roomSelect.value;
    liveTotalEl.innerText = "Calculating...";

    if (typeof window.calculateStayDynamicPrice === "function") {
      const priceDto = await window.calculateStayDynamicPrice(roomId, checkIn, checkOut);
      if (priceDto && priceDto.totalPrice !== undefined) {
        liveTotalEl.innerText = `LKR ${Number(priceDto.totalPrice).toLocaleString(undefined, { minimumFractionDigits: 2 })}`;
        if (tierStatusEl && priceDto.appliedTier) {
          tierStatusEl.innerText = priceDto.appliedTier;
        }
        return;
      }
    }

    // Fallback display if dynamic engine is loading
    const defaultRates = { "1": 19500, "2": 16500, "3": 35000 };
    const est = (defaultRates[roomId] || 20000) * nights;
    liveTotalEl.innerText = `LKR ${est.toLocaleString(undefined, { minimumFractionDigits: 2 })}`;
  };

  // 5. Validation Helpers
  function setFieldValid(inputId, errId) {
    const input = document.getElementById(inputId);
    const err = document.getElementById(errId);
    if (input) input.classList.remove("invalid");
    if (err) err.innerText = "";
    return true;
  }

  function setFieldInvalid(inputId, errId, msg) {
    const input = document.getElementById(inputId);
    const err = document.getElementById(errId);
    if (input) input.classList.add("invalid");
    if (err) err.innerText = msg;
    return false;
  }

  function validateBookingFields() {
    let isValid = true;

    const fn = document.getElementById("formFirstName")?.value.trim();
    if (!fn) isValid = setFieldInvalid("formFirstName", "errFirstName", "First name is required.") && isValid;
    else setFieldValid("formFirstName", "errFirstName");

    const ln = document.getElementById("formLastName")?.value.trim();
    if (!ln) isValid = setFieldInvalid("formLastName", "errLastName", "Last name is required.") && isValid;
    else setFieldValid("formLastName", "errLastName");

    const email = document.getElementById("formEmail")?.value.trim();
    if (!email || !emailPattern.test(email)) {
      isValid = setFieldInvalid("formEmail", "errEmail", "Enter a valid email address.") && isValid;
    } else {
      setFieldValid("formEmail", "errEmail");
    }

    const phone = document.getElementById("formPhone")?.value.trim();
    if (!phone || !phonePattern.test(phone)) {
      isValid = setFieldInvalid("formPhone", "errPhone", "Enter a valid contact phone number.") && isValid;
    } else {
      setFieldValid("formPhone", "errPhone");
    }

    return isValid;
  }

  // 6. Form Submission & Handoff to Member 3 Payment
  window.handleReservationSubmit = async function (event) {
    event.preventDefault();

    const statusEl = document.getElementById("submissionStatus");
    const submitBtn = document.getElementById("btnProceedPayment");

    if (!validateBookingFields()) {
      if (statusEl) {
        statusEl.className = "submission-notice error";
        statusEl.innerText = "Please correct the highlighted errors above.";
      }
      return;
    }

    const firstName = document.getElementById("formFirstName").value.trim();
    const lastName = document.getElementById("formLastName").value.trim();
    const guestEmail = document.getElementById("formEmail").value.trim();
    const guestPhone = document.getElementById("formPhone").value.trim();
    const checkInDate = document.getElementById("formCheckIn").value;
    const checkOutDate = document.getElementById("formCheckOut").value;
    const roomId = parseInt(document.getElementById("formRoomSelect").value, 10);
    const numberOfGuests = parseInt(document.getElementById("formGuestCount").value, 10);

    const bookingPayload = {
      guestName: `${firstName} ${lastName}`,
      guestEmail: guestEmail,
      guestPhone: guestPhone,
      checkInDate: checkInDate,
      checkOutDate: checkOutDate,
      roomId: roomId,
      numberOfGuests: numberOfGuests
    };

    try {
      submitBtn.disabled = true;
      submitBtn.innerText = "Securing Reservation...";
      if (statusEl) {
        statusEl.className = "submission-notice";
        statusEl.innerText = "Creating reservation record...";
      }

      // Member 1 intake endpoint
      const response = await fetch("/api/reservations", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(bookingPayload)
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Reservation creation failed.");
      }

      const reservationData = await response.json();
      const reservationId = reservationData.reservationId;

      if (statusEl) {
        statusEl.className = "submission-notice success";
        statusEl.innerText = `Reservation #${reservationId} created (PENDING). Redirecting to checkout...`;
      }

      // Handoff to Member 3's payment page:
      // In dev mode, initialize the session guard via dev endpoint or directly open checkout
      try {
        await fetch(`/dev/checkout-session?reservationId=${encodeURIComponent(reservationId)}`);
      } catch (e) {
        // Dev endpoint optional if running with persistent sessions
      }

      setTimeout(() => {
        window.location.href = `/checkout?reservationId=${encodeURIComponent(reservationId)}`;
      }, 800);

    } catch (err) {
      console.error("Booking submission error:", err);
      if (statusEl) {
        statusEl.className = "submission-notice error";
        statusEl.innerText = err.message || "Failed to connect to reservation service.";
      }
      submitBtn.disabled = false;
      submitBtn.innerText = "Proceed to Payment ➔";
    }
  };

  // Bind live validation blur listeners
  document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("formFirstName")?.addEventListener("blur", () => {
      const v = document.getElementById("formFirstName").value.trim();
      if (!v) setFieldInvalid("formFirstName", "errFirstName", "First name is required.");
      else setFieldValid("formFirstName", "errFirstName");
    });

    document.getElementById("formLastName")?.addEventListener("blur", () => {
      const v = document.getElementById("formLastName").value.trim();
      if (!v) setFieldInvalid("formLastName", "errLastName", "Last name is required.");
      else setFieldValid("formLastName", "errLastName");
    });

    document.getElementById("formEmail")?.addEventListener("blur", () => {
      const v = document.getElementById("formEmail").value.trim();
      if (!v || !emailPattern.test(v)) setFieldInvalid("formEmail", "errEmail", "Enter a valid email.");
      else setFieldValid("formEmail", "errEmail");
    });

    document.getElementById("formPhone")?.addEventListener("blur", () => {
      const v = document.getElementById("formPhone").value.trim();
      if (!v || !phonePattern.test(v)) setFieldInvalid("formPhone", "errPhone", "Enter a valid phone.");
      else setFieldValid("formPhone", "errPhone");
    });
  });

  /* ==========================================================================
     ROOM PHOTO CAROUSEL CONTROLLER
     ========================================================================== */
  const roomSlideIndices = { 1: 0, 2: 0 };
  const totalRoomSlides = 4;

  window.moveRoomSlide = function (roomId, direction) {
    roomSlideIndices[roomId] = (roomSlideIndices[roomId] + direction + totalRoomSlides) % totalRoomSlides;
    updateCarouselView(roomId);
  };

  window.jumpRoomSlide = function (roomId, index) {
    roomSlideIndices[roomId] = index;
    updateCarouselView(roomId);
  };

  function updateCarouselView(roomId) {
    const carousel = document.getElementById(`carousel-room1`);
    const targetCarousel = document.getElementById(`carousel-room${roomId}`);
    if (!targetCarousel) return;

    const track = targetCarousel.querySelector('.carousel-track');
    const dotsContainer = document.getElementById(`dots-room${roomId}`);
    const idx = roomSlideIndices[roomId];

    if (track) {
      track.style.transform = `translateX(-${idx * 100}%)`;
    }

    if (dotsContainer) {
      const dots = dotsContainer.querySelectorAll('.dot');
      dots.forEach((dot, i) => {
        dot.classList.toggle('active', i === idx);
      });
    }
  }

})();