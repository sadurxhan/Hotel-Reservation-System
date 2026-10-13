package com.loft.hotel.controller;

import com.loft.hotel.entity.CalendarBlock;
import com.loft.hotel.entity.RateOverride;
import com.loft.hotel.entity.Room;
import com.loft.hotel.repository.*;
import com.loft.hotel.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminApiControllerMember2Test {

    @Mock private ReservationService reservationService;
    @Mock private ReservationRepository reservationRepository;
    @Mock private PaymentService paymentService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CancellationService cancellationService;
    @Mock private CancellationRepository cancellationRepository;
    @Mock private RefundService refundService;
    @Mock private RefundRepository refundRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private MenuShowcaseService menuShowcaseService;
    @Mock private ActivityService activityService;
    @Mock private FileStorageService fileStorageService;
    @Mock private AdminGuard adminGuard;
    @Mock private CalendarAdminService calendarAdminService;
    @Mock private CalendarBlockRepository calendarBlockRepository;
    @Mock private RateOverrideRepository rateOverrideRepository;
    @Mock private RoomRepository roomRepository;

    private MockMvc mockMvc;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        AdminApiController controller = new AdminApiController(
                reservationService,
                reservationRepository,
                paymentService,
                paymentRepository,
                cancellationService,
                cancellationRepository,
                refundService,
                refundRepository,
                invoiceRepository,
                menuShowcaseService,
                activityService,
                fileStorageService,
                adminGuard,
                calendarAdminService,
                calendarBlockRepository,
                rateOverrideRepository,
                roomRepository
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        session = new MockHttpSession();
        session.setAttribute(AdminApiController.SESSION_ADMIN_KEY, true);
    }

    // ----------------------------------------------------
    // CALENDAR BLOCKS TESTS
    // ----------------------------------------------------

    @Test
    void testGetCalendarBlocks_Success() throws Exception {
        CalendarBlock block1 = new CalendarBlock();
        block1.setBlockId(1);
        block1.setBlockedDate(LocalDate.of(2026, 11, 1));
        block1.setSource("MANUAL_ADMIN: Pool Maintenance");

        Room room = new Room();
        room.setRoomId(2);
        room.setRoomNumber("Room 2");

        CalendarBlock block2 = new CalendarBlock();
        block2.setBlockId(2);
        block2.setBlockedDate(LocalDate.of(2026, 11, 2));
        block2.setSource("MANUAL_ADMIN: AC Repair");
        block2.setRoom(room);

        when(calendarBlockRepository.findAll()).thenReturn(List.of(block1, block2));

        mockMvc.perform(get("/api/admin/calendar/blocks").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].blockId").value(2)) // sorted descending by date
                .andExpect(jsonPath("$[0].roomNumber").value("Room 2"))
                .andExpect(jsonPath("$[1].blockId").value(1))
                .andExpect(jsonPath("$[1].roomNumber").value("Entire Villa"));
    }

    @Test
    void testCreateCalendarBlocks_Success() throws Exception {
        CalendarBlock created = new CalendarBlock();
        created.setBlockId(10);
        created.setBlockedDate(LocalDate.of(2026, 11, 10));

        when(calendarAdminService.blockDateRange(eq(null), any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(List.of(created));

        String requestJson = """
                {
                    "startDate": "2026-11-10",
                    "endDate": "2026-11-12",
                    "reason": "Roof Maintenance"
                }
                """;

        mockMvc.perform(post("/api/admin/calendar/blocks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(1));

        verify(calendarAdminService).blockDateRange(isNull(), eq(LocalDate.of(2026, 11, 10)), eq(LocalDate.of(2026, 11, 12)), eq("Roof Maintenance"));
    }

    @Test
    void testDeleteCalendarBlock_Success() throws Exception {
        CalendarBlock block = new CalendarBlock();
        block.setBlockId(5);

        when(calendarBlockRepository.findById(5)).thenReturn(Optional.of(block));

        mockMvc.perform(delete("/api/admin/calendar/blocks/5").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(calendarBlockRepository).deleteById(5);
    }

    @Test
    void testDeleteCalendarBlock_NotFound() throws Exception {
        when(calendarBlockRepository.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/admin/calendar/blocks/99").session(session))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------
    // RATE OVERRIDES TESTS
    // ----------------------------------------------------

    @Test
    void testGetRateOverrides_Success() throws Exception {
        RateOverride ro = new RateOverride();
        ro.setRateId(1);
        ro.setPackageName("Summer Surge");
        ro.setStartDate(LocalDate.of(2026, 12, 1));
        ro.setEndDate(LocalDate.of(2026, 12, 15));
        ro.setRateMultiplier(new BigDecimal("1.25"));
        ro.setMinNights(2);
        ro.setIsActive(true);

        when(rateOverrideRepository.findAll()).thenReturn(List.of(ro));

        mockMvc.perform(get("/api/admin/rates/overrides").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rateId").value(1))
                .andExpect(jsonPath("$[0].packageName").value("Summer Surge"))
                .andExpect(jsonPath("$[0].rateMultiplier").value(1.25))
                .andExpect(jsonPath("$[0].roomNumber").value("Entire Villa / All Rooms"));
    }

    @Test
    void testCreateRateOverride_Success() throws Exception {
        RateOverride saved = new RateOverride();
        saved.setRateId(10);
        saved.setPackageName("Monsoon Special");
        saved.setStartDate(LocalDate.of(2026, 11, 1));
        saved.setEndDate(LocalDate.of(2026, 11, 10));
        saved.setRateMultiplier(new BigDecimal("0.85"));

        when(rateOverrideRepository.save(any(RateOverride.class))).thenReturn(saved);

        String requestJson = """
                {
                    "packageName": "Monsoon Special",
                    "packageDescription": "15% off during rainy days",
                    "startDate": "2026-11-01",
                    "endDate": "2026-11-10",
                    "rateMultiplier": 0.85,
                    "minNights": 2,
                    "isActive": true
                }
                """;

        mockMvc.perform(post("/api/admin/rates/overrides")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rateId").value(10))
                .andExpect(jsonPath("$.packageName").value("Monsoon Special"));

        verify(rateOverrideRepository).save(any(RateOverride.class));
    }

    @Test
    void testUpdateRateOverride_Success() throws Exception {
        RateOverride existing = new RateOverride();
        existing.setRateId(3);
        existing.setPackageName("Old Name");
        existing.setStartDate(LocalDate.of(2026, 11, 1));
        existing.setEndDate(LocalDate.of(2026, 11, 10));

        when(rateOverrideRepository.findById(3)).thenReturn(Optional.of(existing));
        when(rateOverrideRepository.save(any(RateOverride.class))).thenAnswer(inv -> inv.getArgument(0));

        String updateJson = """
                {
                    "packageName": "Updated Name",
                    "rateMultiplier": 1.15
                }
                """;

        mockMvc.perform(put("/api/admin/rates/overrides/3")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.packageName").value("Updated Name"))
                .andExpect(jsonPath("$.rateMultiplier").value(1.15));
    }

    @Test
    void testToggleRateOverride_Success() throws Exception {
        RateOverride existing = new RateOverride();
        existing.setRateId(4);
        existing.setIsActive(true);

        when(rateOverrideRepository.findById(4)).thenReturn(Optional.of(existing));
        when(rateOverrideRepository.save(any(RateOverride.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/admin/rates/overrides/4/toggle").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void testDeleteRateOverride_Success() throws Exception {
        RateOverride existing = new RateOverride();
        existing.setRateId(7);

        when(rateOverrideRepository.findById(7)).thenReturn(Optional.of(existing));

        mockMvc.perform(delete("/api/admin/rates/overrides/7").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(rateOverrideRepository).deleteById(7);
    }
}
