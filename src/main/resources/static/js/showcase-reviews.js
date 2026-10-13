/**
 * ============================================================================
 * MEMBER 4: EXPERIENCES, DINING, REVIEWS & INQUIRIES CLIENT
 * ============================================================================
 * Handles:
 * 1. Fetching active dining menu items via /api/menu/all
 * 2. Fetching recreational activities via /api/activity/all (with fallback & modal)
 * 3. Fetching approved reviews via /api/review/all (or /reviews)
 * 4. Submitting a new review via POST /reviews
 * 5. Submitting contact inquiries via POST /inquiry
 * ============================================================================
 */

(function () {
  "use strict";

  // 1. Fallback & Active Data for 6 Activities
    const fallbackActivities = [
      {
        title: "Village & Lake Bund Cycling",
        activityDescription: "Explore quiet gravel bunds, green paddy fields, and morning views towards Galgiriya mountain on our rustic village bicycles.",
        pricingNote: "Complimentary with stay",
        imageUrl: "images/activities/bicycle1.JPG"
      },
      {
        title: "Scenic Village & Sunset Walks",
        activityDescription: "Take gentle guided or self-guided evening strolls along picturesque paddy fields and ancient tank edges as the sun sets behind the hills.",
        pricingNote: "Complimentary / Guided tours available",
        imageUrl: "images/activities/Walks1.jpg"
      },
      {
        title: "Kuda Wewa Kayaking",
        activityDescription: "Paddle across calm, glassy waters surrounded by lilies, birdlife, and the peaceful sounds of rural Kurunegala.",
        pricingNote: "Complimentary with stay",
        imageUrl: "images/activities/Kayak_in_lake.jpg"
      },
      {
        title: "Sunil Aiya’s Chena & Traditional Tree Hut (Pala)",
        activityDescription: "Discover traditional dry-zone Chena farming. Visit rural cultivators and climb traditional treetop huts built by farmers to keep watch.",
        pricingNote: "Guided village tour",
        imageUrl: "images/activities/pala.jpg"
      },
      {
        title: "Freshwater Tank Baths in the Village",
        activityDescription: "Enjoy a refreshing swim in traditional village water tanks (wewas) like Galketiyagama-wewa, where cool, clean waters offer an authentic rural bathing ritual.",
        pricingNote: "Complimentary / Self-guided",
        imageUrl: "images/activities/baths_ahatuwewa.JPG"
      },
      {
        title: "Scenic Hike up Galgiriya Rock Temple",
        activityDescription: "Follow serene forest trails up Galgiriya Rock to visit ancient meditation monk ruins, giant rock boulders, and panoramic hillside lookouts over the lowlands.",
        pricingNote: "Self-guided & guided trails",
        imageUrl: "images/activities/templeRock.JPG"
      }
    ];

    // 2. Fetch & Render Activities
    async function loadActivities() {
      const container = document.getElementById("dynamicActivitiesContainer");
      if (!container) return;

      let activities = [];
      try {
        const res = await fetch("/api/activity/all");
        if (res.ok) {
          const data = await res.json();
          if (Array.isArray(data)) {
            // Keep only active activities
            activities = data.filter(act => act.active === true || act.isActive === true);
          }
        }
      } catch (err) {
        console.info("Using curated fallback activity records:", err);
      }

      //if (activities.length === 0) {
       // activities = fallbackActivities;
      //}

      container.innerHTML = activities.map(act => {
        const photoSrc = (act.imageUrl || "images/hero/front-exterior.jpg").split(",")[0].trim();

        return `
          <div style="background: #FFFFFF; border: 1px solid #EFE9DE; border-radius: 16px; overflow: hidden; display: flex; flex-direction: column; box-shadow: 0 4px 15px rgba(27,59,43,0.04); transition: transform 0.2s ease;">
            <div style="height: 190px; width: 100%; overflow: hidden; background: #2D3748;">
              <img src="${photoSrc}" alt="${act.title}" style="width: 100%; height: 100%; object-fit: cover; display: block;">
            </div>
            <div style="padding: 22px; display: flex; flex-direction: column; justify-content: space-between; flex-grow: 1;">
              <div>
                <h4 style="font-size: 1.15rem; color: #1B3B2B; margin-bottom: 8px;">${act.title}</h4>
                <p style="font-size: 0.88rem; color: #6B726C; line-height: 1.5; margin-bottom: 14px;">${act.activityDescription}</p>
              </div>
              <div style="border-top: 1px solid #F0EAE1; padding-top: 10px;">
                <span style="font-size: 0.75rem; font-weight: 700; color: #8A7352; text-transform: uppercase; letter-spacing: 0.5px;">${act.pricingNote || "Included with stay"}</span>
              </div>
            </div>
          </div>
        `;
       }).join("");
    }

  // Activity Lightbox Opener
  window.openActivityModal = function (actIdx, photoIdx) {
    const album = globalActivityAlbums[actIdx];
    if (!album || typeof openGalleryModal !== "function") return;

    if (window.heroAlbum) {
      window.heroAlbum.length = 0;
      album.photos.forEach((src, pIdx) => {
        window.heroAlbum.push({
          src: src,
          caption: `${album.title} (Photo ${pIdx + 1} of ${album.photos.length})`
        });
      });
      openGalleryModal(photoIdx);
    }
  };

  // 3. Dynamic Menu Gallery & Loader
    const diningGallery = [
      { src: 'images/menu/A_lunch.jpg', caption: 'Authentic Village Clay-Pot Lunch Spread on the Verandah' },
      { src: 'images/menu/roti.jpg', caption: 'Traditional Kurakkan & Coconut Rotti with Fresh Country Banana & Pol Sambol' },
      { src: 'images/menu/lunch_sidedishes.jpg', caption: 'Fresh Garden Curries & Lake Fish Prepared by Mahinda & Sunitha' },
      { src: 'images/menu/menu_sheet.jpg', caption: 'Complete Guest Menu Options & Pricing Card' }
    ];

    window.openDiningGallery = function(index) {
      if (!window.heroAlbum || typeof openGalleryModal !== 'function') return;
      window.heroAlbum.length = 0;
      diningGallery.forEach(item => window.heroAlbum.push(item));
      openGalleryModal(index || 0);
    };

    async function loadMenuItems() {
      const container = document.getElementById("dynamicMenuContainer");
      if (!container) return;

      try {
        const response = await fetch("/api/menu/all");
        if (!response.ok) return;

        const data = await response.json();
        if (!Array.isArray(data)) return;

        // Only display active dishes where available is true
        const activeItems = data.filter(item => item.available === true);

         container.innerHTML = activeItems.map(item => `
          <div style="background: #FFFFFF; border: 1px solid #EFE9DE; border-radius: 14px; overflow: hidden; display: flex; flex-direction: column; box-shadow: 0 4px 12px rgba(27,59,43,0.03);">
            <div style="height: 190px; width: 100%; overflow: hidden; background: #2D3748;">
              <img src="${item.fileUrl || 'images/menu/A_lunch.jpg'}" alt="${item.title || 'Dish'}" style="width: 100%; height: 100%; object-fit: cover; display: block;">
            </div>
            <div style="padding: 24px; display: flex; flex-direction: column; flex-grow: 1;">
              <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <span style="font-size: 0.75rem; font-weight: 700; color: #8A7352; text-transform: uppercase; letter-spacing: 0.5px;">${item.mealType || 'MEAL'}</span>
                <span class="pill-badge" style="font-size: 0.7rem;">Village Style</span>
              </div>
              <h4 style="font-size: 1.2rem; color: #1B3B2B; margin-bottom: 8px;">${item.title || ''}</h4>
              <p style="font-size: 0.88rem; color: #6B726C; line-height: 1.5; margin: 0;">${item.mealDescription || ''}</p>
            </div>
          </div>
        `).join("");
      } catch (e) {
        console.error("Failed to load dynamic menu:", e);
      }
    }

  // 4. Fetch & Render Approved Reviews directly from MySQL Database
  async function loadApprovedReviews() {
    const container = document.getElementById("reviewsContainer");
    if (!container) return;

    try {
      const response = await fetch("/api/review/all");
      if (!response.ok) throw new Error("Could not fetch reviews from server");

      const allReviews = await response.json();

      // Filter strictly by APPROVED status set by the admin/owner in DB
      const approvedReviews = allReviews.filter(r => r.reviewStatus === "APPROVED");

      if (approvedReviews.length === 0) {
        container.innerHTML = `<p style="color: #6B726C; font-size: 0.9rem;">No reviews published yet.</p>`;
        return;
      }

      container.innerHTML = approvedReviews.map(r => {
        const starCount = Math.min(5, Math.max(1, r.rating || 5));
        const starsText = "★".repeat(starCount) + "☆".repeat(5 - starCount);

        return `
          <div class="review-card" style="background: #FFFFFF; border: 1px solid #EFE9DE; border-radius: 16px; padding: 26px; box-shadow: 0 4px 15px rgba(27,59,43,0.04); display: flex; flex-direction: column; justify-content: space-between;">
            <div>
              <div class="review-stars" style="color: #D4B46A; letter-spacing: 2px; margin-bottom: 10px; font-size: 1.05rem;">${starsText}</div>
              <p class="review-quote" style="font-style: italic; color: #2D3748; line-height: 1.6; margin-bottom: 16px; font-size: 0.92rem;">"${r.reviewComment}"</p>
            </div>
            <div class="review-author" style="border-top: 1px solid #F0EAE1; padding-top: 10px; font-size: 0.88rem; font-weight: 700; color: #1B3B2B;">
              ${r.guestName || ('Verified Guest #' + r.guestId)}
              <small style="font-weight: 400; color: #6B726C;">• Verified Stay</small>
            </div>
          </div>
        `;
      }).join("");
    } catch (err) {
      console.warn("Database review fetch error:", err);
      container.innerHTML = `<p style="color: #8A7352; font-size: 0.9rem;">Unable to load reviews from the database. Please ensure the server is running.</p>`;
    }
  }

  // 5. Handle Review Form Submission (POST /reviews)
  window.submitReviewForm = async function (event) {
    event.preventDefault();

    const form = event.target;
    const feedback = document.getElementById("reviewFormFeedback");
    const submitBtn = form.querySelector("button[type='submit']");
    const formData = new FormData(form);

    try {
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerText = "Submitting Review...";
      }

      const params = new URLSearchParams();
      params.append("fullName", formData.get("fullName"));
      params.append("email", formData.get("email"));
      params.append("phoneNo", formData.get("phoneNo"));
      params.append("rating", formData.get("rating"));
      params.append("comment", formData.get("comment"));

      const response = await fetch("/reviews", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: params.toString()
      });

      if (response.ok) {
        form.reset();
        if (feedback) {
          feedback.className = "inquiry-feedback success";
          feedback.innerText = "Thank you! Your review has been submitted and will appear after moderation.";
        }
      } else {
        throw new Error("Unable to save review. Please check your inputs.");
      }
    } catch (err) {
      if (feedback) {
        feedback.className = "inquiry-feedback error";
        feedback.innerText = err.message || "Failed to submit review.";
      }
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.innerText = "Submit Review";
      }
    }
  };

  // 6. Handle Inquiry Subject Change (Toggles between Standard Inquiry & Cancellation Request)
  window.handleInquirySubjectChange = function (subject) {
    const generalFields = document.getElementById("generalInquiryFields");
    const cancelFields = document.getElementById("cancellationFields");
    const submitBtn = document.getElementById("inquirySubmitBtn");

    const fullNameInput = document.getElementById("inquiryFullName");
    const emailInput = document.getElementById("inquiryEmail");
    const phoneInput = document.getElementById("inquiryPhoneNo");
    const messageInput = document.getElementById("inquiryMessage");

    const cancelResId = document.getElementById("cancelReservationId");
    const cancelEmail = document.getElementById("cancelEmail");
    const cancelReason = document.getElementById("cancelReason");

    if (subject === "Cancellation Request") {
      if (generalFields) generalFields.style.display = "none";
      if (cancelFields) cancelFields.style.display = "block";
      if (submitBtn) submitBtn.innerText = "Submit Cancellation Request ➔";

      if (fullNameInput) fullNameInput.required = false;
      if (emailInput) emailInput.required = false;
      if (phoneInput) phoneInput.required = false;
      if (messageInput) messageInput.required = false;

      if (cancelResId) cancelResId.required = true;
      if (cancelEmail) cancelEmail.required = true;
      if (cancelReason) cancelReason.required = true;
    } else {
      if (generalFields) generalFields.style.display = "block";
      if (cancelFields) cancelFields.style.display = "none";
      if (submitBtn) submitBtn.innerText = "Send Inquiry ➔";

      if (fullNameInput) fullNameInput.required = true;
      if (emailInput) emailInput.required = true;
      if (phoneInput) phoneInput.required = true;
      if (messageInput) messageInput.required = true;

      if (cancelResId) cancelResId.required = false;
      if (cancelEmail) cancelEmail.required = false;
      if (cancelReason) cancelReason.required = false;
    }
  };

  // 7. Handle Contact Inquiry & Cancellation Submission
  window.submitInquiryForm = async function (event) {
    event.preventDefault();

    const form = event.target;
    const feedback = document.getElementById("inquiryFormFeedback");
    const submitBtn = form.querySelector("button[type='submit']");
    const formData = new FormData(form);
    const subject = formData.get("subject");

    try {
      if (subject === "Cancellation Request") {
        const reservationId = (formData.get("reservationId") || "").trim();
        const email = (formData.get("cancelEmail") || "").trim();
        const reason = (formData.get("reason") || "").trim();

        if (!reservationId || !email || !reason) {
          throw new Error("Please fill in Reservation ID, Email, and Cancellation Reason.");
        }

        if (submitBtn) {
          submitBtn.disabled = true;
          submitBtn.innerText = "Submitting Cancellation Request...";
        }

        const url = `/api/cancellations?reservationId=${encodeURIComponent(reservationId)}&email=${encodeURIComponent(email)}&reason=${encodeURIComponent(reason)}`;
        const response = await fetch(url, {
          method: "POST"
        });

        const data = await response.json().catch(() => null);

        if (response.ok && data) {
          form.reset();
          if (feedback) {
            if (data.cancellationStatus === "REJECTED") {
              feedback.className = "inquiry-feedback warning";
              feedback.innerText = "Your cancellation request was submitted, but check-in is within 2 days so it cannot be cancelled as per policy. Your booking remains active.";
            } else {
              feedback.className = "inquiry-feedback success";
              feedback.innerText = `Cancellation request for Reservation #${data.reservationId} has been submitted successfully. A confirmation email has been dispatched.`;
            }
          }
          // Reset view to standard inquiry after successful cancellation submission
          handleInquirySubjectChange("General Inquiry");
        } else {
          const errMsg = (data && data.message) ? data.message : "Unable to submit cancellation request. Please verify your reservation ID and email.";
          throw new Error(errMsg);
        }
      } else {
        if (submitBtn) {
          submitBtn.disabled = true;
          submitBtn.innerText = "Sending Message...";
        }

        const params = new URLSearchParams();
        params.append("fullName", formData.get("fullName") || "");
        params.append("email", formData.get("email") || "");
        params.append("phoneNo", formData.get("phoneNo") || "");
        params.append("subject", formData.get("subject") || "");
        params.append("message", formData.get("message") || "");

        const response = await fetch("/inquiry", {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: params.toString()
        });

        if (response.ok) {
          form.reset();
          if (feedback) {
            feedback.className = "inquiry-feedback success";
            feedback.innerText = "Your message has been sent to our caretakers. We will reach back shortly!";
          }
        } else {
          throw new Error("Could not send inquiry. Please try again.");
        }
      }
    } catch (err) {
      if (feedback) {
        feedback.className = "inquiry-feedback error";
        feedback.innerText = err.message || "Failed to submit request.";
      }
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        const currentSubject = document.getElementById("inquirySubject") ? document.getElementById("inquirySubject").value : "";
        submitBtn.innerText = (currentSubject === "Cancellation Request") ? "Submit Cancellation Request ➔" : "Send Inquiry ➔";
      }
    }
  };

  // Run on page load
  document.addEventListener("DOMContentLoaded", () => {
    loadActivities();
    loadMenuItems();
    loadApprovedReviews();
  });
})();