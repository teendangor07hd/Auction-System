package com.bidhub.server.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Kiểm thử AuctionStatus enum và state machine. */
@DisplayName("AuctionStatus — State machine và canBid()/isTerminal()")
class AuctionStatusTest {

    @Test
    @DisplayName("Chỉ RUNNING.canBid() = true, các status khác false")
    void testCanBid_OnlyRunningReturnsTrue() {
        assertTrue(AuctionStatus.RUNNING.canBid(), "RUNNING phải cho phép đặt giá");
        assertFalse(AuctionStatus.OPEN.canBid(), " OPEN chưa được đặt giá");
        assertFalse(AuctionStatus.FINISHED.canBid(), "FINISHED không còn đặt giá được");
        assertFalse(AuctionStatus.PAID.canBid(), "PAID là terminal");
        assertFalse(AuctionStatus.CANCELED.canBid(), "CANCELED là terminal");
    }

    @Test
    @DisplayName("FINISHED, PAID, CANCELED là terminal states")
    void testIsTerminal_CorrectStatuses() {
        assertFalse(AuctionStatus.OPEN.isTerminal());
        assertFalse(AuctionStatus.RUNNING.isTerminal());
        assertTrue(AuctionStatus.FINISHED.isTerminal());
        assertTrue(AuctionStatus.PAID.isTerminal());
        assertTrue(AuctionStatus.CANCELED.isTerminal());
    }

    @Test
    @DisplayName("Từ OPEN: Chỉ được phép sang RUNNING, chặn mọi hướng khác")
    void testTransition_FromOpen_StrictRules() {
        // Luồng đúng (Happy path)
        assertTrue(AuctionStatus.OPEN.canTransitionTo(AuctionStatus.RUNNING), "OPEN -> RUNNING hợp lệ");

        // Các luồng sai (Negative paths)
        assertFalse(AuctionStatus.OPEN.canTransitionTo(AuctionStatus.OPEN), "Không tự chuyển sang chính nó");
        assertFalse(AuctionStatus.OPEN.canTransitionTo(AuctionStatus.FINISHED), "Cấm nhảy cóc sang FINISHED");
        assertFalse(AuctionStatus.OPEN.canTransitionTo(AuctionStatus.PAID), "Cấm nhảy cóc sang PAID");
        assertFalse(AuctionStatus.OPEN.canTransitionTo(AuctionStatus.CANCELED), "Cấm nhảy cóc sang CANCELED");
    }

    @Test
    @DisplayName("Từ RUNNING: Chỉ được phép sang FINISHED, chặn mọi hướng khác")
    void testTransition_FromRunning_StrictRules() {
        // Luồng đúng
        assertTrue(AuctionStatus.RUNNING.canTransitionTo(AuctionStatus.FINISHED), "RUNNING -> FINISHED hợp lệ");

        // Các luồng sai
        assertFalse(AuctionStatus.RUNNING.canTransitionTo(AuctionStatus.OPEN), "Cấm quay lại OPEN");
        assertFalse(AuctionStatus.RUNNING.canTransitionTo(AuctionStatus.RUNNING), "Không tự chuyển sang chính nó");
        assertFalse(AuctionStatus.RUNNING.canTransitionTo(AuctionStatus.PAID), "Cấm nhảy cóc sang PAID");
        assertFalse(AuctionStatus.RUNNING.canTransitionTo(AuctionStatus.CANCELED), "Cấm nhảy cóc sang CANCELED");
    }

    @Test
    @DisplayName("Từ FINISHED: Được sang PAID hoặc CANCELED, cấm đi lùi")
    void testTransition_FromFinished_StrictRules() {
        // Luồng đúng
        assertTrue(AuctionStatus.FINISHED.canTransitionTo(AuctionStatus.PAID), "FINISHED -> PAID hợp lệ");
        assertTrue(AuctionStatus.FINISHED.canTransitionTo(AuctionStatus.CANCELED), "FINISHED -> CANCELED hợp lệ");

        // Các luồng sai
        assertFalse(AuctionStatus.FINISHED.canTransitionTo(AuctionStatus.OPEN), "Cấm đi lùi về OPEN");
        assertFalse(AuctionStatus.FINISHED.canTransitionTo(AuctionStatus.RUNNING), "Cấm đi lùi về RUNNING");
        assertFalse(AuctionStatus.FINISHED.canTransitionTo(AuctionStatus.FINISHED), "Không tự chuyển sang chính nó");
    }

    @Test
    @DisplayName("Từ Terminal (PAID, CANCELED): Cấm chuyển sang bất kỳ trạng thái nào")
    void testTransition_FromTerminal_StrictRules() {
        // Quét qua tất cả 5 trạng thái để đảm bảo PAID và CANCELED bị khóa cứng
        for (AuctionStatus target : AuctionStatus.values()) {
            assertFalse(AuctionStatus.PAID.canTransitionTo(target), "PAID là ngõ cụt, cấm chuyển sang " + target);
            assertFalse(AuctionStatus.CANCELED.canTransitionTo(target), "CANCELED là ngõ cụt, cấm chuyển sang " + target);
        }
    }
}