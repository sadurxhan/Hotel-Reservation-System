package com.loft.hotel.controller;

import com.loft.hotel.entity.*;
import com.loft.hotel.repository.*;
import com.loft.hotel.service.*;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminApiController {

    private static final Logger log = LoggerFactory.getLogger(AdminApiController.class);

    public static final String SESSION_ADMIN_KEY = "admin_authenticated";

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final CancellationService cancellationService;
    private final CancellationRepository cancellationRepository;
    private final RefundService refundService;
    private final RefundRepository refundRepository;
    private final InvoiceRepository invoiceRepository;
    private final MenuShowcaseService menuShowcaseService;
    private final ActivityService activityService;
    private final FileStorageService fileStorageService;
    private final AdminGuard adminGuard;
    private final CalendarAdminService calendarAdminService;
    private final CalendarBlockRepository calendarBlockRepository;
    private final RateOverrideRepository rateOverrideRepository;
    private final RoomRepository roomRepository;

    @Value("${app.admin-key}")
    private String configuredAdminKey;

    @Value("${app.admin-username:admin}")
    private String configuredAdminUsername;

    public AdminApiController(
            ReservationService reservationService,
            ReservationRepository reservationRepository,
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            CancellationService cancellationService,
            CancellationRepository cancellationRepository,
            RefundService refundService,
            RefundRepository refundRepository,
            InvoiceRepository invoiceRepository,
            MenuShowcaseService menuShowcaseService,
            ActivityService activityService,
            FileStorageService fileStorageService,
            AdminGuard adminGuard,
            CalendarAdminService calendarAdminService,
            CalendarBlockRepository calendarBlockRepository,
            RateOverrideRepository rateOverrideRepository,
            RoomRepository roomRepository) {
        this.reservationService = reservationService;
        this.reservationRepository = reservationRepository;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.cancellationService = cancellationService;
        this.cancellationRepository = cancellationRepository;
        this.refundService = refundService;
        this.refundRepository = refundRepository;
        this.invoiceRepository = invoiceRepository;
        this.menuShowcaseService = menuShowcaseService;
        this.activityService = activityService;
        this.fileStorageService = fileStorageService;
        this.adminGuard = adminGuard;
        this.calendarAdminService = calendarAdminService;
        this.calendarBlockRepository = calendarBlockRepository;
        this.rateOverrideRepository = rateOverrideRepository;
        this.roomRepository = roomRepository;
    }

    private void verifyAccess(String keyHeader, HttpSession session) {
        if (session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_ADMIN_KEY))) {
            return;
        }
        if (keyHeader != null && !keyHeader.isBlank()) {
            adminGuard.check(keyHeader.trim());
            if (session != null) {
                session.setAttribute(SESSION_ADMIN_KEY, true);
            }
            return;
        }
        throw new SecurityException("Admin access required.");
    }

    private String getGuestFullName(Guest g) {
        if (g == null) return "Guest";
        try {
            String fname = g.getFname() != null ? g.getFname().trim() : "";
            String lname = g.getLname() != null ? g.getLname().trim() : "";
            String full = (fname + " " + lname).trim();
            return full.isEmpty() ? "Guest" : full;
        } catch (Exception e) {
            return "Guest";
        }
    }

    private String getGuestEmail(Guest g) {
        if (g == null) return "";
        try {
            return g.getEmail() != null ? g.getEmail().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String getGuestPhone(Guest g) {
        if (g == null) return "";
        try {
            return g.getPhoneNo() != null ? g.getPhoneNo().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ----------------------------------------------------
    // AUTH VERIFICATION
    // ----------------------------------------------------
    @PostMapping("/auth/verify")
    public ResponseEntity<Map<String, Object>> verifyKey(
            @RequestParam(required = false) String key,
            @RequestBody(required = false) Map<String, String> body,
            HttpSession session) {
        String inputKey = key;
        if (inputKey == null && body != null) {
            inputKey = body.get("key");
        }
        if (inputKey != null && inputKey.trim().equals(configuredAdminKey.trim())) {
            if (session != null) {
                session.setAttribute(SESSION_ADMIN_KEY, true);
            }
            return ResponseEntity.ok(Map.of("authenticated", true, "key", configuredAdminKey));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("authenticated", false, "message", "Invalid administrative security key."));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String password,
            @RequestBody(required = false) Map<String, String> body,
            HttpSession session) {
        String u = username;
        String p = password;
        if (body != null) {
            if (u == null) u = body.get("username");
            if (p == null) p = body.get("password");
        }
        if (u != null && p != null && configuredAdminUsername.equalsIgnoreCase(u.trim()) && configuredAdminKey.equals(p.trim())) {
            if (session != null) {
                session.setAttribute(SESSION_ADMIN_KEY, true);
            }
            return ResponseEntity.ok(Map.of("authenticated", true, "key", configuredAdminKey));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("authenticated", false, "message", "Invalid username or security key."));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_ADMIN_KEY);
        }
        return ResponseEntity.ok(Map.of("authenticated", false));
    }

    // ----------------------------------------------------
    // 1. DASHBOARD OVERVIEW & KPIS
    // ----------------------------------------------------
    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getOverview(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            List<Reservation> reservations = reservationRepository.findAll();
            List<Payment> payments = paymentRepository.findAll();
            List<Cancellation> cancellations = cancellationRepository.findAll();
            List<Refund> refunds = refundRepository.findAll();

            long totalReservations = reservations.size();
            long confirmedBookings = reservations.stream()
                    .filter(r -> r != null && r.getReservationStatus() == ReservationStatus.CONFIRMED).count();
            long pendingBookings = reservations.stream()
                    .filter(r -> r != null && r.getReservationStatus() == ReservationStatus.PENDING).count();
            long cancelledBookings = reservations.stream()
                    .filter(r -> r != null && r.getReservationStatus() == ReservationStatus.CANCELLED).count();

            long successfulPayments = payments.stream()
                    .filter(p -> p != null && p.getPaymentStatus() == Payment.PaymentStatus.PAID).count();
            BigDecimal totalRevenue = payments.stream()
                    .filter(p -> p != null && p.getPaymentStatus() == Payment.PaymentStatus.PAID && p.getAmount() != null)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long pendingCancellations = cancellations.stream()
                    .filter(c -> c != null && c.getCancellationStatus() == Cancellation.CancellationStatus.REQUESTED).count();

            long pendingRefunds = refunds.stream()
                    .filter(r -> r != null && r.getRefundStatus() == Refund.RefundStatus.PENDING).count();
            BigDecimal totalRefunded = refunds.stream()
                    .filter(r -> r != null && r.getRefundStatus() == Refund.RefundStatus.REFUNDED && r.getRefundAmount() != null)
                    .map(Refund::getRefundAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Date-range / daily revenue timeline
            Map<String, BigDecimal> dailyRevenue = new TreeMap<>();
            DateTimeFormatter df = DateTimeFormatter.ofPattern("MMM dd");
            for (Payment p : payments) {
                if (p != null && p.getPaymentStatus() == Payment.PaymentStatus.PAID && p.getPaymentDateTime() != null && p.getAmount() != null) {
                    String dayKey = p.getPaymentDateTime().format(df);
                    dailyRevenue.put(dayKey, dailyRevenue.getOrDefault(dayKey, BigDecimal.ZERO).add(p.getAmount()));
                }
            }

            List<Map<String, Object>> chartData = new ArrayList<>();
            dailyRevenue.forEach((day, amt) -> {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", day);
                point.put("revenue", amt);
                chartData.add(point);
            });

            Map<String, Object> overview = new LinkedHashMap<>();
            overview.put("totalReservations", totalReservations);
            overview.put("confirmedBookings", confirmedBookings);
            overview.put("pendingBookings", pendingBookings);
            overview.put("cancelledBookings", cancelledBookings);
            overview.put("successfulPayments", successfulPayments);
            overview.put("totalRevenue", totalRevenue);
            overview.put("pendingCancellations", pendingCancellations);
            overview.put("pendingRefunds", pendingRefunds);
            overview.put("totalRefunded", totalRefunded);
            overview.put("revenueChart", chartData);

            return ResponseEntity.ok(overview);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (Exception ex) {
            log.error("Error in getOverview", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error loading overview: " + ex.getMessage()));
        }
    }

    // ----------------------------------------------------
    // 2. RESERVATIONS MANAGEMENT
    // ----------------------------------------------------
    @GetMapping("/reservations")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getReservations(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            List<Reservation> reservations = reservationRepository.findAll();
            List<Payment> payments = paymentRepository.findAll();

            Map<Integer, Payment> paidPaymentsByResId = payments.stream()
                    .filter(p -> p != null && p.getReservationId() != null && p.getPaymentStatus() == Payment.PaymentStatus.PAID)
                    .collect(Collectors.toMap(Payment::getReservationId, p -> p, (a, b) -> a));

            Map<Integer, Payment> anyPaymentByResId = payments.stream()
                    .filter(p -> p != null && p.getReservationId() != null)
                    .collect(Collectors.toMap(Payment::getReservationId, p -> p, (a, b) -> a));

            List<Map<String, Object>> result = new ArrayList<>();
            for (Reservation r : reservations) {
                if (r == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("reservationId", r.getReservationId());
                row.put("checkInDate", r.getCheckInDate());
                row.put("checkOutDate", r.getCheckOutDate());
                row.put("bookingDate", r.getBookingDate());
                row.put("numberOfGuests", r.getNumberOfGuests());
                row.put("totalAmount", r.getTotalAmount() != null ? r.getTotalAmount() : BigDecimal.ZERO);
                row.put("reservationStatus", r.getReservationStatus() != null ? r.getReservationStatus().name() : "PENDING");

                row.put("guestName", getGuestFullName(r.getGuest()));
                row.put("guestEmail", getGuestEmail(r.getGuest()));
                row.put("guestPhone", getGuestPhone(r.getGuest()));

                Payment paid = r.getReservationId() != null ? paidPaymentsByResId.get(r.getReservationId()) : null;
                Payment any = r.getReservationId() != null ? anyPaymentByResId.get(r.getReservationId()) : null;

                if (paid != null) {
                    row.put("paymentStatus", "PAID");
                    row.put("paymentMethod", paid.getPaymentMethod() != null ? paid.getPaymentMethod().name() : "Credit_Card");
                    row.put("transactionId", paid.getTransactionId());
                    row.put("paymentId", paid.getPaymentId());
                } else if (any != null) {
                    row.put("paymentStatus", any.getPaymentStatus() != null ? any.getPaymentStatus().name() : "PENDING");
                    row.put("paymentMethod", any.getPaymentMethod() != null ? any.getPaymentMethod().name() : "Credit_Card");
                    row.put("transactionId", any.getTransactionId());
                    row.put("paymentId", any.getPaymentId());
                } else {
                    row.put("paymentStatus", "UNPAID");
                    row.put("paymentMethod", "None");
                    row.put("transactionId", null);
                    row.put("paymentId", null);
                }

                result.add(row);
            }

            // Sort latest reservations first
            result.sort((a, b) -> {
                Integer idA = (Integer) a.get("reservationId");
                Integer idB = (Integer) b.get("reservationId");
                if (idA == null && idB == null) return 0;
                if (idA == null) return 1;
                if (idB == null) return -1;
                return Integer.compare(idB, idA);
            });

            return ResponseEntity.ok(result);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (Exception ex) {
            log.error("Error in getReservations", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error loading reservations: " + ex.getMessage()));
        }
    }

    // ----------------------------------------------------
    // 3. CANCELLATION MANAGEMENT
    // ----------------------------------------------------
    @GetMapping("/cancellations")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCancellations(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            List<Cancellation> cancellations = cancellationRepository.findAll();
            Map<Integer, Reservation> resMap = reservationRepository.findAll().stream()
                    .filter(r -> r != null && r.getReservationId() != null)
                    .collect(Collectors.toMap(Reservation::getReservationId, r -> r, (a, b) -> a));

            List<Map<String, Object>> result = new ArrayList<>();
            for (Cancellation c : cancellations) {
                if (c == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("cancellationId", c.getCancellationId());
                row.put("reservationId", c.getReservationId());
                row.put("requestedBy", c.getRequestedBy() != null ? c.getRequestedBy().name() : "Guest");
                row.put("cancellationReason", c.getCancellationReason() != null ? c.getCancellationReason() : "");
                row.put("cancellationStatus", c.getCancellationStatus() != null ? c.getCancellationStatus().name() : "REQUESTED");
                row.put("requestedDateTime", c.getRequestedDateTime());

                Reservation res = c.getReservationId() != null ? resMap.get(c.getReservationId()) : null;
                if (res != null) {
                    row.put("checkInDate", res.getCheckInDate());
                    row.put("totalAmount", res.getTotalAmount() != null ? res.getTotalAmount() : BigDecimal.ZERO);
                    row.put("guestName", getGuestFullName(res.getGuest()));
                    row.put("guestEmail", getGuestEmail(res.getGuest()));
                } else {
                    row.put("checkInDate", "N/A");
                    row.put("totalAmount", BigDecimal.ZERO);
                    row.put("guestName", "Guest");
                    row.put("guestEmail", "");
                }

                result.add(row);
            }

            result.sort((a, b) -> {
                var dtA = (java.time.LocalDateTime) a.get("requestedDateTime");
                var dtB = (java.time.LocalDateTime) b.get("requestedDateTime");
                if (dtA == null && dtB == null) return 0;
                if (dtA == null) return 1;
                if (dtB == null) return -1;
                return dtB.compareTo(dtA);
            });

            return ResponseEntity.ok(result);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (Exception ex) {
            log.error("Error in getCancellations", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error loading cancellations: " + ex.getMessage()));
        }
    }

    @PostMapping("/cancellations/{id}/approve")
    public ResponseEntity<Cancellation> approveCancellation(
            @PathVariable String id,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);
        return ResponseEntity.ok(cancellationService.approveCancellation(id));
    }

    @PostMapping("/cancellations/{id}/reject")
    public ResponseEntity<Cancellation> rejectCancellation(
            @PathVariable String id,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);
        return ResponseEntity.ok(cancellationService.rejectCancellation(id));
    }

    // ----------------------------------------------------
    // 4. PAYMENTS & BILLING
    // ----------------------------------------------------
    @GetMapping("/payments")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPayments(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            List<Payment> payments = paymentRepository.findAll();
            Map<Integer, Reservation> resMap = reservationRepository.findAll().stream()
                    .filter(r -> r != null && r.getReservationId() != null)
                    .collect(Collectors.toMap(Reservation::getReservationId, r -> r, (a, b) -> a));
            Map<String, Invoice> invoiceMap = invoiceRepository.findAll().stream()
                    .filter(inv -> inv != null && inv.getPaymentId() != null)
                    .collect(Collectors.toMap(Invoice::getPaymentId, inv -> inv, (a, b) -> a));

            List<Map<String, Object>> result = new ArrayList<>();
            for (Payment p : payments) {
                if (p == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("paymentId", p.getPaymentId());
                row.put("reservationId", p.getReservationId());
                row.put("amount", p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO);
                row.put("paymentMethod", p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "Credit_Card");
                row.put("paymentStatus", p.getPaymentStatus() != null ? p.getPaymentStatus().name() : "PENDING");
                row.put("transactionId", p.getTransactionId() != null ? p.getTransactionId() : "N/A");
                row.put("paymentDateTime", p.getPaymentDateTime());

                Reservation res = p.getReservationId() != null ? resMap.get(p.getReservationId()) : null;
                if (res != null) {
                    row.put("guestName", getGuestFullName(res.getGuest()));
                    row.put("guestEmail", getGuestEmail(res.getGuest()));
                } else {
                    row.put("guestName", "Guest");
                    row.put("guestEmail", "");
                }

                Invoice inv = p.getPaymentId() != null ? invoiceMap.get(p.getPaymentId()) : null;
                row.put("invoiceId", inv != null ? inv.getInvoiceId() : null);

                result.add(row);
            }

            result.sort((a, b) -> {
                var dtA = (java.time.LocalDateTime) a.get("paymentDateTime");
                var dtB = (java.time.LocalDateTime) b.get("paymentDateTime");
                if (dtA == null && dtB == null) return 0;
                if (dtA == null) return 1;
                if (dtB == null) return -1;
                return dtB.compareTo(dtA);
            });

            return ResponseEntity.ok(result);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (Exception ex) {
            log.error("Error in getPayments", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error loading payments: " + ex.getMessage()));
        }
    }

    // ----------------------------------------------------
    // 5. REFUND MANAGEMENT
    // ----------------------------------------------------
    @GetMapping("/refunds")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRefunds(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            List<Refund> refunds = refundRepository.findAll();
            Map<String, Payment> paymentMap = paymentRepository.findAll().stream()
                    .filter(p -> p != null && p.getPaymentId() != null)
                    .collect(Collectors.toMap(Payment::getPaymentId, p -> p, (a, b) -> a));
            Map<Integer, Reservation> resMap = reservationRepository.findAll().stream()
                    .filter(r -> r != null && r.getReservationId() != null)
                    .collect(Collectors.toMap(Reservation::getReservationId, r -> r, (a, b) -> a));

            List<Map<String, Object>> result = new ArrayList<>();
            for (Refund ref : refunds) {
                if (ref == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("refundId", ref.getRefundId() != null ? ref.getRefundId() : "");
                row.put("paymentId", ref.getPaymentId() != null ? ref.getPaymentId() : "");
                row.put("refundStatus", ref.getRefundStatus() != null ? ref.getRefundStatus().name() : "PENDING");
                row.put("refundReason", ref.getRefundReason() != null ? ref.getRefundReason() : "Cancellation refund");
                row.put("refundAmount", ref.getRefundAmount() != null ? ref.getRefundAmount() : BigDecimal.ZERO);
                row.put("refundDateTime", ref.getRefundDateTime() != null ? ref.getRefundDateTime() : LocalDateTime.now());

                Payment payment = ref.getPaymentId() != null ? paymentMap.get(ref.getPaymentId()) : null;
                if (payment != null) {
                    row.put("reservationId", payment.getReservationId());
                    Reservation res = payment.getReservationId() != null ? resMap.get(payment.getReservationId()) : null;
                    if (res != null) {
                        row.put("guestName", getGuestFullName(res.getGuest()));
                        row.put("guestEmail", getGuestEmail(res.getGuest()));
                    } else {
                        row.put("guestName", "Guest");
                        row.put("guestEmail", "");
                    }
                } else {
                    row.put("reservationId", null);
                    row.put("guestName", "Guest");
                    row.put("guestEmail", "");
                }

                result.add(row);
            }

            result.sort((a, b) -> {
                var dtA = (java.time.LocalDateTime) a.get("refundDateTime");
                var dtB = (java.time.LocalDateTime) b.get("refundDateTime");
                if (dtA == null && dtB == null) return 0;
                if (dtA == null) return 1;
                if (dtB == null) return -1;
                return dtB.compareTo(dtA);
            });

            return ResponseEntity.ok(result);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (Exception ex) {
            log.error("Error in getRefunds", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error loading refunds: " + ex.getMessage()));
        }
    }

    @PostMapping("/refunds/{id}/paid")
    public ResponseEntity<Refund> markRefundPaid(
            @PathVariable String id,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);
        return ResponseEntity.ok(refundService.markRefundAsPaid(id));
    }

    // ----------------------------------------------------
    // 6. MENU SHOWCASE CRUD
    // ----------------------------------------------------
    @GetMapping("/menu")
    public ResponseEntity<List<MenuShowcase>> getAllMenu(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);
        return ResponseEntity.ok(menuShowcaseService.getAllMenuItems());
    }

    @PostMapping("/menu/save")
    public ResponseEntity<MenuShowcase> saveMenuItem(
            @RequestParam(required = false) Integer menuId,
            @RequestParam String title,
            @RequestParam String mealType,
            @RequestParam String mealDescription,
            @RequestParam(required = false, defaultValue = "true") Boolean available,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);

        MenuShowcase item;
        if (menuId == null || menuId <= 0) {
            item = menuShowcaseService.addMenuItem(title, mealType, mealDescription, null);
        } else {
            item = menuShowcaseService.updateMenuItem(menuId, title, mealType, mealDescription, null, available);
        }

        if (photo != null && !photo.isEmpty()) {
            String url = fileStorageService.store(photo, "menu");
            item = menuShowcaseService.updateImageUrl(item.getMenuId(), url);
        }

        return ResponseEntity.ok(item);
    }

    @PostMapping("/menu/{id}/toggle")
    public ResponseEntity<MenuShowcase> toggleMenu(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);
        return ResponseEntity.ok(menuShowcaseService.toggleAvailable(id));
    }

    @DeleteMapping("/menu/{id}")
    public ResponseEntity<Map<String, Object>> deleteMenu(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);
        menuShowcaseService.deleteMenuItem(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Menu item deleted successfully."));
    }

    // ----------------------------------------------------
    // 7. ACTIVITIES CRUD
    // ----------------------------------------------------
    @GetMapping("/activities")
    public ResponseEntity<List<Activity>> getAllActivities(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);
        return ResponseEntity.ok(activityService.getAllActivities());
    }

    @PostMapping("/activities/save")
    public ResponseEntity<Activity> saveActivity(
            @RequestParam(required = false) Integer activityId,
            @RequestParam String title,
            @RequestParam String activityDescription,
            @RequestParam String pricingNote,
            @RequestParam(required = false, defaultValue = "true") Boolean active,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);

        Activity item;
        if (activityId == null || activityId <= 0) {
            item = activityService.addActivity(title, activityDescription, pricingNote, null);
        } else {
            item = activityService.updateActivity(activityId, title, activityDescription, pricingNote, null, active);
        }

        if (photo != null && !photo.isEmpty()) {
            String url = fileStorageService.store(photo, "activities");
            item = activityService.updateImageUrl(item.getActivityId(), url);
        }

        return ResponseEntity.ok(item);
    }

    @PostMapping("/activities/{id}/toggle")
    public ResponseEntity<Activity> toggleActivity(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);
        return ResponseEntity.ok(activityService.toggleActive(id));
    }

    @DeleteMapping("/activities/{id}")
    public ResponseEntity<Map<String, Object>> deleteActivity(
            @PathVariable Integer id,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        verifyAccess(key, session);
        activityService.deleteActivity(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Activity deleted successfully."));
    }

    // ----------------------------------------------------
    // 8. CALENDAR AVAILABILITY & MAINTENANCE BLOCKS
    // ----------------------------------------------------
    @GetMapping("/calendar/blocks")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCalendarBlocks(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            List<CalendarBlock> blocks = new ArrayList<>(calendarBlockRepository.findAll());
            blocks.sort((a, b) -> {
                if (a.getBlockedDate() == null && b.getBlockedDate() == null) return 0;
                if (a.getBlockedDate() == null) return 1;
                if (b.getBlockedDate() == null) return -1;
                return b.getBlockedDate().compareTo(a.getBlockedDate());
            });

            List<Map<String, Object>> result = new ArrayList<>();
            for (CalendarBlock cb : blocks) {
                if (cb == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("blockId", cb.getBlockId());
                row.put("blockedDate", cb.getBlockedDate());
                row.put("source", cb.getSource());
                row.put("externalUid", cb.getExternalUid());
                row.put("roomNumber", cb.getRoom() != null ? cb.getRoom().getRoomNumber() : "Entire Villa");
                row.put("roomId", cb.getRoom() != null ? cb.getRoom().getRoomId() : null);
                row.put("createdAt", cb.getCreatedAt());
                result.add(row);
            }

            return ResponseEntity.ok(result);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (Exception ex) {
            log.error("Error in getCalendarBlocks", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error loading calendar blocks: " + ex.getMessage()));
        }
    }

    @PostMapping("/calendar/blocks")
    @Transactional
    public ResponseEntity<?> createCalendarBlocks(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer roomId,
            @RequestParam(required = false) String reason,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            LocalDate start = startDate;
            LocalDate end = endDate;
            Integer rId = roomId;
            String rsn = reason;

            if (body != null) {
                if (start == null && body.get("startDate") != null && !body.get("startDate").toString().isBlank()) {
                    start = LocalDate.parse(body.get("startDate").toString().trim());
                }
                if (end == null && body.get("endDate") != null && !body.get("endDate").toString().isBlank()) {
                    end = LocalDate.parse(body.get("endDate").toString().trim());
                }
                if (rId == null && body.get("roomId") != null && !body.get("roomId").toString().isBlank()) {
                    try {
                        int parsed = Integer.parseInt(body.get("roomId").toString().trim());
                        if (parsed > 0) rId = parsed;
                    } catch (NumberFormatException ignored) {}
                }
                if (rsn == null && body.get("reason") != null) {
                    rsn = body.get("reason").toString().trim();
                }
            }

            if (start == null || end == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Start date and end date are required."));
            }
            if (end.isBefore(start)) {
                return ResponseEntity.badRequest().body(Map.of("message", "End date cannot be earlier than start date."));
            }

            List<CalendarBlock> created = calendarAdminService.blockDateRange(rId, start, end, rsn);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Successfully created " + created.size() + " calendar block(s).",
                    "count", created.size(),
                    "blocks", created
            ));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception ex) {
            log.error("Error creating calendar blocks", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error creating calendar blocks: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/calendar/blocks/{blockId}")
    @Transactional
    public ResponseEntity<?> deleteCalendarBlock(
            @PathVariable Integer blockId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            Optional<CalendarBlock> blockOpt = calendarBlockRepository.findById(blockId);
            if (blockOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Calendar block not found with ID: " + blockId));
            }

            calendarBlockRepository.deleteById(blockId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Calendar block deleted successfully."));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (Exception ex) {
            log.error("Error deleting calendar block", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error deleting calendar block: " + ex.getMessage()));
        }
    }

    // ----------------------------------------------------
    // 9. RATE OVERRIDES & PROMOTIONAL PACKAGES
    // ----------------------------------------------------
    @GetMapping("/rates/overrides")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRateOverrides(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            List<RateOverride> list = new ArrayList<>(rateOverrideRepository.findAll());
            list.sort((a, b) -> {
                if (a.getRateId() == null && b.getRateId() == null) return 0;
                if (a.getRateId() == null) return 1;
                if (b.getRateId() == null) return -1;
                return b.getRateId().compareTo(a.getRateId());
            });

            List<Map<String, Object>> result = new ArrayList<>();
            for (RateOverride ro : list) {
                if (ro == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("rateId", ro.getRateId());
                row.put("packageName", ro.getPackageName() != null ? ro.getPackageName() : "Custom Override");
                row.put("packageDescription", ro.getPackageDescription() != null ? ro.getPackageDescription() : "");
                row.put("startDate", ro.getStartDate());
                row.put("endDate", ro.getEndDate());
                row.put("rateMultiplier", ro.getRateMultiplier() != null ? ro.getRateMultiplier() : BigDecimal.ONE);
                row.put("minNights", ro.getMinNights() != null ? ro.getMinNights() : 1);
                row.put("isActive", ro.getIsActive() != null ? ro.getIsActive() : true);
                row.put("roomId", ro.getRoom() != null ? ro.getRoom().getRoomId() : null);
                row.put("roomNumber", ro.getRoom() != null ? ro.getRoom().getRoomNumber() : "Entire Villa / All Rooms");
                row.put("createdAt", ro.getCreatedAt());
                result.add(row);
            }

            return ResponseEntity.ok(result);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (Exception ex) {
            log.error("Error in getRateOverrides", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error loading rate overrides: " + ex.getMessage()));
        }
    }

    @PostMapping("/rates/overrides")
    @Transactional
    public ResponseEntity<?> createRateOverride(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            if (body == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Request body is required."));
            }

            String packageName = body.get("packageName") != null ? body.get("packageName").toString().trim() : null;
            if (packageName == null || packageName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Package/Rule name is required."));
            }

            String desc = body.get("packageDescription") != null ? body.get("packageDescription").toString().trim() : "";

            if (body.get("startDate") == null || body.get("startDate").toString().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Start date is required."));
            }
            if (body.get("endDate") == null || body.get("endDate").toString().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "End date is required."));
            }

            LocalDate startDate = LocalDate.parse(body.get("startDate").toString().trim());
            LocalDate endDate = LocalDate.parse(body.get("endDate").toString().trim());
            if (endDate.isBefore(startDate)) {
                return ResponseEntity.badRequest().body(Map.of("message", "End date cannot be earlier than start date."));
            }

            BigDecimal multiplier = BigDecimal.ONE;
            if (body.get("rateMultiplier") != null && !body.get("rateMultiplier").toString().isBlank()) {
                multiplier = new BigDecimal(body.get("rateMultiplier").toString().trim());
            }

            int minNights = 1;
            if (body.get("minNights") != null && !body.get("minNights").toString().isBlank()) {
                try {
                    int parsed = Integer.parseInt(body.get("minNights").toString().trim());
                    if (parsed > 0) minNights = parsed;
                } catch (NumberFormatException ignored) {}
            }

            boolean isActive = true;
            if (body.get("isActive") != null) {
                isActive = Boolean.parseBoolean(body.get("isActive").toString().trim());
            }

            Room room = null;
            if (body.get("roomId") != null && !body.get("roomId").toString().isBlank()) {
                try {
                    int rId = Integer.parseInt(body.get("roomId").toString().trim());
                    if (rId > 0) {
                        room = roomRepository.findById(rId).orElse(null);
                    }
                } catch (NumberFormatException ignored) {}
            }

            RateOverride ro = new RateOverride();
            ro.setPackageName(packageName);
            ro.setPackageDescription(desc);
            ro.setStartDate(startDate);
            ro.setEndDate(endDate);
            ro.setRateMultiplier(multiplier);
            ro.setMinNights(minNights);
            ro.setIsActive(isActive);
            ro.setRoom(room);

            RateOverride saved = rateOverrideRepository.save(ro);
            return ResponseEntity.ok(saved);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception ex) {
            log.error("Error creating rate override", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error creating rate override: " + ex.getMessage()));
        }
    }

    @PutMapping("/rates/overrides/{rateId}")
    @Transactional
    public ResponseEntity<?> updateRateOverride(
            @PathVariable Integer rateId,
            @RequestBody(required = false) Map<String, Object> body,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            Optional<RateOverride> opt = rateOverrideRepository.findById(rateId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Rate override not found with ID: " + rateId));
            }

            if (body == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Request body is required."));
            }

            RateOverride ro = opt.get();

            if (body.containsKey("packageName")) {
                String name = body.get("packageName") != null ? body.get("packageName").toString().trim() : null;
                if (name != null && !name.isBlank()) {
                    ro.setPackageName(name);
                }
            }
            if (body.containsKey("packageDescription")) {
                ro.setPackageDescription(body.get("packageDescription") != null ? body.get("packageDescription").toString().trim() : "");
            }
            if (body.containsKey("startDate") && body.get("startDate") != null && !body.get("startDate").toString().isBlank()) {
                ro.setStartDate(LocalDate.parse(body.get("startDate").toString().trim()));
            }
            if (body.containsKey("endDate") && body.get("endDate") != null && !body.get("endDate").toString().isBlank()) {
                ro.setEndDate(LocalDate.parse(body.get("endDate").toString().trim()));
            }
            if (ro.getEndDate().isBefore(ro.getStartDate())) {
                return ResponseEntity.badRequest().body(Map.of("message", "End date cannot be earlier than start date."));
            }
            if (body.containsKey("rateMultiplier") && body.get("rateMultiplier") != null && !body.get("rateMultiplier").toString().isBlank()) {
                ro.setRateMultiplier(new BigDecimal(body.get("rateMultiplier").toString().trim()));
            }
            if (body.containsKey("minNights") && body.get("minNights") != null && !body.get("minNights").toString().isBlank()) {
                try {
                    int parsed = Integer.parseInt(body.get("minNights").toString().trim());
                    if (parsed > 0) ro.setMinNights(parsed);
                } catch (NumberFormatException ignored) {}
            }
            if (body.containsKey("isActive") && body.get("isActive") != null) {
                ro.setIsActive(Boolean.parseBoolean(body.get("isActive").toString().trim()));
            }
            if (body.containsKey("roomId")) {
                if (body.get("roomId") != null && !body.get("roomId").toString().isBlank()) {
                    try {
                        int rId = Integer.parseInt(body.get("roomId").toString().trim());
                        if (rId > 0) {
                            ro.setRoom(roomRepository.findById(rId).orElse(null));
                        } else {
                            ro.setRoom(null);
                        }
                    } catch (NumberFormatException ignored) {
                        ro.setRoom(null);
                    }
                } else {
                    ro.setRoom(null);
                }
            }

            RateOverride updated = rateOverrideRepository.save(ro);
            return ResponseEntity.ok(updated);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        } catch (Exception ex) {
            log.error("Error updating rate override", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error updating rate override: " + ex.getMessage()));
        }
    }

    @PostMapping("/rates/overrides/{rateId}/toggle")
    @Transactional
    public ResponseEntity<?> toggleRateOverride(
            @PathVariable Integer rateId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            Optional<RateOverride> opt = rateOverrideRepository.findById(rateId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Rate override not found with ID: " + rateId));
            }

            RateOverride ro = opt.get();
            ro.setIsActive(ro.getIsActive() == null || !ro.getIsActive());
            RateOverride saved = rateOverrideRepository.save(ro);
            return ResponseEntity.ok(saved);
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (Exception ex) {
            log.error("Error toggling rate override", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error toggling rate override: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/rates/overrides/{rateId}")
    @Transactional
    public ResponseEntity<?> deleteRateOverride(
            @PathVariable Integer rateId,
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            HttpSession session) {
        try {
            verifyAccess(key, session);

            Optional<RateOverride> opt = rateOverrideRepository.findById(rateId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Rate override not found with ID: " + rateId));
            }

            rateOverrideRepository.deleteById(rateId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Rate override deleted successfully."));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", se.getMessage()));
        } catch (Exception ex) {
            log.error("Error deleting rate override", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error deleting rate override: " + ex.getMessage()));
        }
    }
}

