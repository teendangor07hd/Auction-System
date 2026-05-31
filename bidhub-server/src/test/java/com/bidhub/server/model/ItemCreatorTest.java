package com.bidhub.server.model;

import com.bidhub.common.model.Entity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử Factory Method Pattern và cây kế thừa Item.
 *
 * <p>Test chứng minh:
 * <ul>
 *   <li>Factory Method Pattern: mỗi ConcreteCreator tạo đúng ConcreteProduct</li>
 *   <li>Polymorphism: {@code getCategoryDetails()} và {@code printInfo()} khác nhau theo loại</li>
 *   <li>Abstraction: {@code ItemCreator.forType()} trả về đúng creator lúc runtime</li>
 *   <li>Validation: giá khởi điểm &lt;= 0, extras thiếu field → ném exception</li>
 *   <li>Inheritance: {@code item instanceof Entity} = true (kế thừa 2 tầng)</li>
 * </ul>
 */
@DisplayName("ItemCreator — Kiểm thử Factory Method Pattern")
class ItemCreatorTest {

    private static final String SELLER_ID = "seller-test-001";

    // =========================================================================
    // Test 1: Factory Method tạo đúng ConcreteProduct
    // =========================================================================

    @Nested
    @DisplayName("Factory Method — Concrete Creator tạo đúng kiểu")
    class CreatorTypeTests {

        @Test
        @DisplayName("ElectronicsCreator.createItem() → trả về instance Electronics")
        void testElectronicsCreator_ReturnsElectronics() {
            // Arrange
            ItemCreator creator = new ElectronicsCreator();

            // Act
            Item item = creator.createItem(
                    "iPhone 15", "Điện thoại Apple", 25_000_000.0, SELLER_ID,
                    Map.of("brand", "Apple", "warrantyMonths", 12));

            // Assert
            assertInstanceOf(Electronics.class, item,
                    "ElectronicsCreator phải tạo Electronics, không phải " + item.getClass().getSimpleName());
        }

        @Test
        @DisplayName("ArtCreator.createItem() → trả về instance Art")
        void testArtCreator_ReturnsArt() {
            ItemCreator creator = new ArtCreator();

            Item item = creator.createItem(
                    "Sơn mài hoa sen", "Tranh sơn dầu", 15_000_000.0, SELLER_ID,
                    Map.of("artist", "Nguyễn Văn A", "yearCreated", 2020));

            assertInstanceOf(Art.class, item);
        }

        @Test
        @DisplayName("VehicleCreator.createItem() → trả về instance Vehicle")
        void testVehicleCreator_ReturnsVehicle() {
            ItemCreator creator = new VehicleCreator();

            Item item = creator.createItem(
                    "Toyota Camry", "Sedan sang", 800_000_000.0, SELLER_ID,
                    Map.of("manufacturer", "Toyota", "year", 2022, "mileageKm", 30000));

            assertInstanceOf(Vehicle.class, item);
        }

        @Test
        @DisplayName("ItemCreator.forType(ELECTRONICS) → trả về ElectronicsCreator")
        void testForType_Electronics_ReturnsElectronicsCreator() {
            ItemCreator creator = ItemCreator.forType(ItemType.ELECTRONICS);
            assertInstanceOf(ElectronicsCreator.class, creator,
                    "forType(ELECTRONICS) phải trả về ElectronicsCreator");
        }

        @Test
        @DisplayName("ItemCreator.forType(ART) → trả về ArtCreator")
        void testForType_Art_ReturnsArtCreator() {
            assertInstanceOf(ArtCreator.class, ItemCreator.forType(ItemType.ART));
        }

        @Test
        @DisplayName("ItemCreator.forType(VEHICLE) → trả về VehicleCreator")
        void testForType_Vehicle_ReturnsVehicleCreator() {
            assertInstanceOf(VehicleCreator.class, ItemCreator.forType(ItemType.VEHICLE));
        }
    }

    // =========================================================================
    // Test 2: Polymorphism — getCategoryDetails() khác nhau theo loại
    // =========================================================================

    @Nested
    @DisplayName("Polymorphism — getCategoryDetails() và printInfo() hành vi khác nhau")
    class PolymorphismTests {

        @Test
        @DisplayName("List<Item> từ 3 creator khác nhau → getCategoryDetails() trả về 3 chuỗi khác nhau")
        void testPolymorphism_GetCategoryDetails_DifferentPerType() {
            List<Item> items = List.of(
                    ItemCreator.forType(ItemType.ELECTRONICS).createItem(
                            "Laptop", "", 20_000_000, SELLER_ID,
                            Map.of("brand", "Dell", "warrantyMonths", 24)),
                    ItemCreator.forType(ItemType.ART).createItem(
                            "Tranh", "", 5_000_000, SELLER_ID,
                            Map.of("artist", "Picasso VN", "yearCreated", 2019)),
                    ItemCreator.forType(ItemType.VEHICLE).createItem(
                            "Xe máy", "", 30_000_000, SELLER_ID,
                            Map.of("manufacturer", "Honda", "year", 2023, "mileageKm", 5000))
            );

            // Act
            List<String> details = items.stream().map(Item::getCategoryDetails).toList();

            // Assert — mỗi loại phải khác nhau (polymorphism)
            assertNotEquals(details.get(0), details.get(1), "Electronics và Art phải khác nhau");
            assertNotEquals(details.get(1), details.get(2), "Art và Vehicle phải khác nhau");
            assertNotEquals(details.get(0), details.get(2), "Electronics và Vehicle phải khác nhau");
        }

        @Test
        @DisplayName("Electronics.getCategoryDetails() chứa brand và warrantyMonths")
        void testElectronics_CategoryDetails_ContainsBrandAndWarranty() {
            Item item = new ElectronicsCreator().createItem(
                    "Galaxy S24", "", 22_000_000, SELLER_ID,
                    Map.of("brand", "Samsung", "warrantyMonths", 18));

            String details = item.getCategoryDetails();
            assertTrue(details.contains("Samsung"), "getCategoryDetails phải có brand");
            assertTrue(details.contains("18"), "getCategoryDetails phải có warrantyMonths");
        }

        @Test
        @DisplayName("printInfo() không ném exception cho cả 3 loại Item")
        void testAllTypes_PrintInfo_DoesNotThrow() {
            List<Item> items = List.of(
                    ItemCreator.forType(ItemType.ELECTRONICS).createItem(
                            "TV", "", 10_000_000, SELLER_ID,
                            Map.of("brand", "LG", "warrantyMonths", 12)),
                    ItemCreator.forType(ItemType.ART).createItem(
                            "Tượng", "", 3_000_000, SELLER_ID,
                            Map.of("artist", "A", "yearCreated", 2000)),
                    ItemCreator.forType(ItemType.VEHICLE).createItem(
                            "Xe đạp", "", 5_000_000, SELLER_ID,
                            Map.of("manufacturer", "Giant", "year", 2021, "mileageKm", 0))
            );

            items.forEach(item ->
                    assertDoesNotThrow(item::printInfo,
                            item.getClass().getSimpleName() + ".printInfo() không được ném exception"));
        }

        @Test
        @DisplayName("Item implements Displayable — có thể gán vào biến Displayable")
        void testItem_ImplementsDisplayable() {
            Item item = new ElectronicsCreator().createItem(
                    "Tai nghe", "", 2_000_000, SELLER_ID,
                    Map.of("brand", "Sony", "warrantyMonths", 6));

            assertInstanceOf(Displayable.class, item,
                    "Item phải implement Displayable");
            assertDoesNotThrow(((Displayable) item)::printInfo);
        }
    }

    // =========================================================================
    // Test 3: Inheritance — kế thừa 2 tầng Electronics → Item → Entity
    // =========================================================================

    @Nested
    @DisplayName("Inheritance — Item kế thừa 2 tầng từ Entity")
    class InheritanceTests {

        @Test
        @DisplayName("Electronics instanceof Entity — kế thừa 2 tầng Electronics→Item→Entity")
        void testElectronics_IsInstanceOfEntity() {
            Item item = new ElectronicsCreator().createItem(
                    "Máy tính bảng", "", 8_000_000, SELLER_ID,
                    Map.of("brand", "Apple", "warrantyMonths", 12));

            assertInstanceOf(Entity.class, item,
                    "Electronics phải là Entity (kế thừa 2 tầng Electronics→Item→Entity)");
        }

        @Test
        @DisplayName("Item mới tạo có UUID — getId() không null và không rỗng")
        void testItem_HasUuidAfterCreation() {
            Item item = new ArtCreator().createItem(
                    "Bức tranh", "", 1_000_000, SELLER_ID,
                    Map.of("artist", "B", "yearCreated", 2022));

            assertNotNull(item.getId(), "getId() không được null");
            assertFalse(item.getId().isBlank(), "getId() không được rỗng");
        }

        @Test
        @DisplayName("Hai Item tạo bởi cùng Creator có UUID khác nhau")
        void testTwoItems_HaveDifferentIds() {
            ItemCreator creator = new ElectronicsCreator();
            Item i1 = creator.createItem("P1", "", 1000, SELLER_ID,
                    Map.of("brand", "A", "warrantyMonths", 0));
            Item i2 = creator.createItem("P2", "", 2000, SELLER_ID,
                    Map.of("brand", "B", "warrantyMonths", 0));

            assertNotEquals(i1.getId(), i2.getId(), "Hai Item khác nhau phải có UUID khác nhau");
        }
    }

    // =========================================================================
    // Test 4: Validation — từ chối input không hợp lệ
    // =========================================================================

    @Nested
    @DisplayName("Validation — từ chối input không hợp lệ")
    class ValidationTests {

        @Test
        @DisplayName("startingPrice âm → IllegalArgumentException")
        void testCreate_NegativePrice_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new ArtCreator().createItem("Tranh", "", -100.0, SELLER_ID,
                            Map.of("artist", "X", "yearCreated", 2000)),
                    "Giá âm phải ném IllegalArgumentException");
        }

        @Test
        @DisplayName("startingPrice = 0 → IllegalArgumentException")
        void testCreate_ZeroPrice_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new ArtCreator().createItem("Tranh", "", 0.0, SELLER_ID,
                            Map.of("artist", "X", "yearCreated", 2000)));
        }

        @Test
        @DisplayName("ItemCreator.forType(null) → NullPointerException")
        void testForType_NullType_ThrowsNullPointerException() {
            assertThrows(
                    NullPointerException.class,
                    () -> ItemCreator.forType(null),
                    "forType(null) phải ném NullPointerException");
        }

        @Test
        @DisplayName("ElectronicsCreator thiếu 'brand' trong extras → IllegalArgumentException")
        void testElectronicsCreator_MissingBrand_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new ElectronicsCreator().createItem(
                            "Phone", "", 5_000_000, SELLER_ID,
                            Map.of("warrantyMonths", 12)),  // thiếu "brand"
                    "Thiếu extras[brand] phải ném IllegalArgumentException");
        }

        @Test
        @DisplayName("VehicleCreator thiếu 'mileageKm' trong extras → IllegalArgumentException")
        void testVehicleCreator_MissingMileage_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new VehicleCreator().createItem(
                            "Car", "", 500_000_000, SELLER_ID,
                            Map.of("manufacturer", "Ford", "year", 2020))  // thiếu "mileageKm"
            );
        }
    }
}