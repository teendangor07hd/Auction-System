package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

/**
 * Controller cho man hinh tao san pham — chi cho SELLER.
 *
 * <p>// 📌 [Tieu chi: Quan ly san pham — tao san pham voi dynamic fields]
 * Khi chon itemType → hien thi form phu phu hop:
 * ELECTRONICS: brand, warrantyMonths
 * ART: artist, yearCreated
 * VEHICLE: manufacturer, year, mileageKm
 */
public class CreateItemController {

    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startingPriceField;
    @FXML private ComboBox<String> itemTypeComboBox;
    @FXML private Label lblMessage;

    // 📌 [Tieu chi: MVC — dynamic fields theo itemType]
    @FXML private VBox electronicsFields;
    @FXML private VBox artFields;
    @FXML private VBox vehicleFields;

    // Electronics fields
    @FXML private TextField brandField;
    @FXML private TextField warrantyMonthsField;

    // Art fields
    @FXML private TextField artistField;
    @FXML private TextField yearCreatedField;

    // Vehicle fields
    @FXML private TextField manufacturerField;
    @FXML private TextField yearField;
    @FXML private TextField mileageKmField;

    @FXML
    public void initialize() {
        itemTypeComboBox.setItems(
                FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));

        // 📌 [Tieu chi: MVC — an/hien dynamic fields khi doi itemType]
        itemTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            electronicsFields.setVisible("ELECTRONICS".equals(newVal));
            electronicsFields.setManaged("ELECTRONICS".equals(newVal));
            artFields.setVisible("ART".equals(newVal));
            artFields.setManaged("ART".equals(newVal));
            vehicleFields.setVisible("VEHICLE".equals(newVal));
            vehicleFields.setManaged("VEHICLE".equals(newVal));
        });

        // Mac dinh an tat ca form phu
        electronicsFields.setVisible(false);
        electronicsFields.setManaged(false);
        artFields.setVisible(false);
        artFields.setManaged(false);
        vehicleFields.setVisible(false);
        vehicleFields.setManaged(false);

        lblMessage.setVisible(false);
        lblMessage.getStyleClass().add("error-message");

        // Kiem tra role — chi SELLER duoc tao item
        String role = ClientSession.getInstance().getCurrentRole();
        if (!"SELLER".equals(role)) {
            lblMessage.setText("Chỉ người bán (SELLER) mới được tạo sản phẩm.");
            lblMessage.setVisible(true);
        }
    }

    /**
     * Xu ly tao san pham — gui request CREATE_ITEM.
     */
    @FXML
    public void handleSubmit() {
        String role = ClientSession.getInstance().getCurrentRole();
        if (!"SELLER".equals(role)) {
            showError("Chỉ người bán (SELLER) mới được tạo sản phẩm.");
            return;
        }

        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String priceStr = startingPriceField.getText().trim();
        String itemType = itemTypeComboBox.getValue();

        if (name.isBlank() || priceStr.isBlank() || itemType == null) {
            showError("Vui lòng điền đầy đủ thông tin cơ bản.");
            return;
        }

        double startingPrice;
        ObjectNode extras = JsonNodeFactory.instance.objectNode();

        try {
            startingPrice = Double.parseDouble(priceStr);
            if (startingPrice <= 0) {
                showError("Giá khởi điểm phải lớn hơn 0.");
                return;
            }

            // Kiểm tra và gán dữ liệu (assign data) tùy theo itemType
            switch (itemType) {
                case "ELECTRONICS" -> {
                    String brand = brandField.getText().trim();
                    String warranty = warrantyMonthsField.getText().trim();
                    if (brand.isBlank() || warranty.isBlank()) {
                        showError("Vui lòng điền đủ Brand và Warranty.");
                        return;
                    }
                    extras.put("brand", brand);
                    extras.put("warrantyMonths", Integer.parseInt(warranty));
                }
                case "ART" -> {
                    String artist = artistField.getText().trim();
                    String year = yearCreatedField.getText().trim();
                    if (artist.isBlank() || year.isBlank()) {
                        showError("Vui lòng điền đủ Artist và Year Created.");
                        return;
                    }
                    extras.put("artist", artist);
                    extras.put("yearCreated", Integer.parseInt(year));
                }
                case "VEHICLE" -> {
                    String manufacturer = manufacturerField.getText().trim();
                    String year = yearField.getText().trim();
                    String mileage = mileageKmField.getText().trim();
                    if (manufacturer.isBlank() || year.isBlank() || mileage.isBlank()) {
                        showError("Vui lòng điền đủ Manufacturer, Year và Mileage.");
                        return;
                    }
                    extras.put("manufacturer", manufacturer);
                    extras.put("year", Integer.parseInt(year));
                    extras.put("mileageKm", Integer.parseInt(mileage));
                }
            }
        } catch (NumberFormatException e) {
            // Catch Exception khi người dùng nhập chữ vào ô yêu cầu nhập số
            showError("Dữ liệu số không hợp lệ (Giá, năm, số tháng, số km).");
            e.printStackTrace(); // In log ra console để dễ debug
            return;
        }

        // 📌 [Tieu chi: Quan ly san pham — tao payload voi extras theo itemType]
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("name", name);
        payload.put("description", description);
        payload.put("startingPrice", startingPrice);
        payload.put("itemType", itemType);
        payload.set("extras", extras);

        MessageRequest request = new MessageRequest(
                "CREATE_ITEM", ClientSession.getInstance().getToken(), payload);

        lblMessage.setVisible(false);

        NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
                ServerGateway.getInstance().sendRequest(request));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if (response.isOk()) {
                ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
            } else {
                showError(response.getMessage());
            }
        });

        task.setOnFailed(e -> {
            showError("Không kết nối được máy chủ. Thử lại sau.");
        });

        new Thread(task).start();
    }

    /**
     * Huy tao san pham — quay lai danh sach dau gia.
     */
    @FXML
    public void handleCancel() {
        ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
    }

    /**
     * Hàm helper để hiển thị lỗi (Display error message)
     */
    private void showError(String message) {
        lblMessage.setText(message);
        lblMessage.setVisible(true);
    }
}