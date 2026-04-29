package com.bidhub.server.dao;

import com.bidhub.server.model.*;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class ItemDaoTest {

    private Connection conn;
    private ItemDao dao;
    private static final String SELLER_ID = "seller-uuid-001";

    @BeforeEach
    void setup() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement s = conn.createStatement()) {
            s.execute("""
          CREATE TABLE items (
            id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '',
            starting_price REAL NOT NULL, item_type TEXT NOT NULL, seller_id TEXT NOT NULL,
            extra_data TEXT NOT NULL DEFAULT '{}', created_at TEXT NOT NULL, updated_at TEXT NOT NULL)
          """);
        }
        dao = new ItemDao(conn);
    }

    @AfterEach
    void teardown() throws SQLException { conn.close(); }

    @Test
    @DisplayName("save Electronics → findById trả về instanceof Electronics đúng brand")
    void save_electronics_findById_correctSubclass() {
        Item item = new ElectronicsCreator().createItem(
                "MacBook Pro", "Laptop", 30_000_000.0, SELLER_ID,
                Map.of("brand", "Apple", "warrantyMonths", 12));
        dao.save(item);

        Optional<Item> found = dao.findById(item.getId());
        assertTrue(found.isPresent());
        assertInstanceOf(Electronics.class, found.get());
        assertEquals("Apple", ((Electronics) found.get()).getBrand());
        assertEquals(12, ((Electronics) found.get()).getWarrantyMonths());
    }

    @Test
    @DisplayName("save Art → findById trả về instanceof Art đúng artist")
    void save_art_findById_correctSubclass() {
        Item item = new ArtCreator().createItem(
                "Mùa thu", "", 5_000_000.0, SELLER_ID,
                Map.of("artist", "Nguyễn Tư Nghiêm", "yearCreated", 1970));
        dao.save(item);

        Optional<Item> found = dao.findById(item.getId());
        assertTrue(found.isPresent());
        assertInstanceOf(Art.class, found.get());
        assertEquals(1970, ((Art) found.get()).getYearCreated());
    }

    @Test
    @DisplayName("save Vehicle → findById trả về instanceof Vehicle đúng mileageKm")
    void save_vehicle_findById_correctSubclass() {
        Item item = new VehicleCreator().createItem(
                "Toyota Camry", "", 600_000_000.0, SELLER_ID,
                Map.of("manufacturer", "Toyota", "year", 2022, "mileageKm", 30000));
        dao.save(item);

        Optional<Item> found = dao.findById(item.getId());
        assertTrue(found.isPresent());
        assertInstanceOf(Vehicle.class, found.get());
        assertEquals(30000, ((Vehicle) found.get()).getMileageKm());
    }

    @Test
    @DisplayName("findById với id không tồn tại → Optional.empty()")
    void findById_notFound_returnsEmpty() {
        assertTrue(dao.findById("ghost-id").isEmpty());
    }

    @Test
    @DisplayName("findBySellerId → trả về đúng danh sách item của seller đó")
    void findBySellerId_returnsCorrectItems() {
        dao.save(new ElectronicsCreator().createItem(
                "Phone", "", 10_000_000.0, SELLER_ID,
                Map.of("brand", "Samsung", "warrantyMonths", 6)));
        dao.save(new ElectronicsCreator().createItem(
                "Laptop", "", 20_000_000.0, "other-seller",
                Map.of("brand", "Dell", "warrantyMonths", 24)));

        assertEquals(1, dao.findBySellerId(SELLER_ID).size());
    }

    @Test
    @DisplayName("warrantyMonths Jackson deserialize Number.intValue() — không ClassCastException")
    void save_load_electronics_warrantyMonths_noClassCastException() {
        // Kiểm tra Jackson không deserialize Integer thành Long và gây ClassCastException
        Item item = new ElectronicsCreator().createItem(
                "TV", "", 5_000_000.0, SELLER_ID,
                Map.of("brand", "LG", "warrantyMonths", Integer.MAX_VALUE));
        dao.save(item);

        assertDoesNotThrow(() -> dao.findById(item.getId()));
        Optional<Item> found = dao.findById(item.getId());
        assertTrue(found.isPresent());
        assertEquals(Integer.MAX_VALUE, ((Electronics) found.get()).getWarrantyMonths());
    }
}
