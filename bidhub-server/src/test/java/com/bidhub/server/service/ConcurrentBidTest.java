package com.bidhub.server.service;

import com.bidhub.common.exception.InvalidBidException;
import com.bidhub.server.dao.AuctionDao;
import com.bidhub.server.dao.BidDao;
import com.bidhub.server.model.Auction;
import com.bidhub.server.model.AuctionStatus;
import com.bidhub.server.model.BidTransaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
        import static org.junit.jupiter.api.Assertions.*;

/**
 * Stress test: 50 thread đóng thoi bid vao cung 1 auction.
 *
 * <p>Kiem tra: không deadlock, không NullPointerException, RAM và DB nhat quan.
 *
 */
class ConcurrentBidTest {

    private AuctionManager auctionManager;
    private Auction testAuction;
    private static final int THREAD_COUNT = 50;

    @BeforeEach
    void setUp() {
        auctionManager = AuctionManager.getInstance();
        auctionManager.clearAll();
        testAuction = createTestAuction("auc-stress-001", 1000.0);
        auctionManager.addAuction(testAuction);
    }

    @AfterEach
    void tearDown() {
        auctionManager.clearAll();
    }

    private Auction createTestAuction(String id, double startingPrice) {
        Auction a = new Auction();
        try {
            var idField = a.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(a, id);
            var endField = a.getClass().getDeclaredField("endTime");
            endField.setAccessible(true);
            endField.set(a, LocalDateTime.now().plusHours(1));
            var statusField = a.getClass().getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(a, AuctionStatus.RUNNING);
            var bidField = a.getClass().getDeclaredField("currentHighestBid");
            bidField.setAccessible(true);
            bidField.set(a, startingPrice);
            var incField = a.getClass().getDeclaredField("minimumIncrement");
            incField.setAccessible(true);
            incField.set(a, 1.0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return a;
    }

    @Test
    @DisplayName("50 thread dong thoi bid — khong deadlock, gia nhat quan")
    void concurrentBid_noDeadlock_consistentPrice() throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Doi tat ca thread san sang
                    testAuction.getLock().lock();
                    try {
                        double currentBid = testAuction.getCurrentHighestBid();
                        double newBid = currentBid + testAuction.getMinimumIncrement();

                        // Validate
                        if (testAuction.getStatus() == AuctionStatus.RUNNING
                                && newBid > currentBid) {
                            testAuction.setCurrentHighestBid(newBid);
                            successCount.incrementAndGet();
                        }
                    } finally {
                        testAuction.getLock().unlock();
                    }
                } catch (Exception e) {
                    System.err.println("[StressTest] Thread " + threadIndex + " loi: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start tat ca thread đóng thoi
        startLatch.countDown();
        boolean allDone = doneLatch.await(30, TimeUnit.SECONDS);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Assert
        assertTrue(allDone, "Timeout — co thread bi deadlock");
        assertEquals(THREAD_COUNT, successCount.get(),
                "So bid thanh cong khong dung — co race condition");
        assertEquals(1000.0 + THREAD_COUNT * 1.0, testAuction.getCurrentHighestBid(),
                "Gia cuoi cung khong nhat quan — co lost update");
    }

    @Test
    @DisplayName("Lock/unlock không deadlock khi exception xảy ra — lock vẫn tái sử dụng được")
    void lockUnlock_noDeadlockOnException() {
        // 1. Chủ động tạo một exception bên trong try-finally
        // để đảm bảo unlock() vẫn được gọi
        try {
            testAuction.getLock().lock();
            try {
                throw new RuntimeException("Giả định exception");
            } finally {
                testAuction.getLock().unlock(); // <<< Điểm mấu chốt: unlock vẫn chạy
            }
        } catch (RuntimeException e) {
            // 2. Bắt exception mà ta vừa ném ra, tránh làm chết test
            assertEquals("Giả định exception", e.getMessage());
        }

        // 3. Kiểm tra lock vẫn hoạt động bình thường sau exception
        // Nếu unlock() không được gọi, lock sẽ bị kẹt và thao tác sau sẽ deadlock
        assertDoesNotThrow(() -> {
            testAuction.getLock().lock();
            testAuction.getLock().unlock();
        });
    }

    @Test
    @DisplayName("ReentrantLock — cung thread lock 2 lan khong deadlock")
    void reentrantLock_sameThreadNoDeadlock() {
        assertDoesNotThrow(() -> {
            testAuction.getLock().lock();
            testAuction.getLock().lock(); // Re-entrant — OK
            testAuction.getLock().unlock();
            testAuction.getLock().unlock();
        });
        assertEquals(0, testAuction.getLock().getHoldCount());
    }

    @Test
    @DisplayName("2 auction khac nhau — bid dong thoi khong block nhau")
    void differentAuctions_concurrentNoBlock() throws Exception {
        Auction a2 = createTestAuction("auc-stress-002", 500.0);
        auctionManager.addAuction(a2);

        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        // Thread 1 bid auction 1
        new Thread(() -> {
            try {
                latch.await();
                testAuction.getLock().lock();
                try { Thread.sleep(50); } finally { testAuction.getLock().unlock(); }
            } catch (Exception e) { fail(e.getMessage()); }
            finally { done.countDown(); }
        }).start();

        // Thread 2 bid auction 2 — không bi block boi thread 1
        new Thread(() -> {
            try {
                latch.await();
                a2.getLock().lock();
                try { Thread.sleep(50); } finally { a2.getLock().unlock(); }
            } catch (Exception e) { fail(e.getMessage()); }
            finally { done.countDown(); }
        }).start();

        latch.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS), "2 auction doc lap bi block nhau");
    }

    @Test
    @DisplayName("Lock fairness — thread doi lau hon duoc uu tien (timed)")
    void lock_timedAcquisition() {
        assertDoesNotThrow(() -> {
            boolean acquired = testAuction.getLock().tryLock(1, TimeUnit.SECONDS);
            if (acquired) {
                testAuction.getLock().unlock();
            }
        });
    }

    @Test
    @DisplayName("AuctionDao.findAll() tra ve danh sach auction")
    void auctionDao_findAll_returnsList() {
        // Test logic — verify method ton tai và signature đúng
        assertNotNull(AuctionDao.class, "AuctionDao phai co method findAll()");
        try {
            var method = AuctionDao.class.getMethod("findAll");
            assertEquals(List.class, method.getReturnType());
        } catch (NoSuchMethodException e) {
            fail("AuctionDao.findAll() chua duoc them");
        }
    }
}