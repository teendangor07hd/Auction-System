package com.bidhub.client.controller;

import com.bidhub.client.network.ClientSession;
import com.bidhub.client.network.NetworkTask;
import com.bidhub.client.network.ServerGateway;
import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import com.bidhub.client.util.UiUtils; // THÊM IMPORT UiUtils
import com.bidhub.common.network.MessageRequest;
import com.bidhub.common.network.MessageResponse;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;

/**
 * Controller cho man hinh tao san pham — chi cho SELLER.
 */
public class CreateItemController {

    @FXML private TextField nameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startingPriceField; // Giá khởi điểm dự kiến
    @FXML private ComboBox<String> itemTypeComboBox;
    @FXML private Label lblMessage;
    
    // Image selection
    @FXML private Button btnSelectImage;
    @FXML private Label lblImageName;
    private String selectedImagePath = "";

    // Dynamic fields
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

    // 📌 [Tieu chi: UX — Loading state components]
    @FXML private Button btnSubmit;
    @FXML private Button btnCancel;
    @FXML private ProgressIndicator loadingSpinner;

    @FXML
    public void initialize() {
        itemTypeComboBox.setItems(
                FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));

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

        // 📌 [Tieu chi: UX — TextField chi nhan so]
        // Chỉ apply numeric filter cho các trường số
        if (startingPriceField != null) UiUtils.applyNumericFilter(startingPriceField);
        UiUtils.applyNumericFilter(warrantyMonthsField);
        UiUtils.applyNumericFilter(yearCreatedField);
        UiUtils.applyNumericFilter(yearField);
        UiUtils.applyNumericFilter(mileageKmField);

        // Kiem tra role — chi SELLER duoc tao item
        String role = ClientSession.getInstance().getCurrentRole();
        if (!"SELLER".equals(role)) {
            UiUtils.showError("Lỗi phân quyền", "Chỉ người bán (SELLER) mới được tạo sản phẩm.");
            if (btnSubmit != null) btnSubmit.setDisable(true);
        }
        
        // Setup chon anh
        if (btnSelectImage != null) {
            btnSelectImage.setOnAction(e -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Chọn ảnh sản phẩm");
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
                );
                File file = fileChooser.showOpenDialog(btnSelectImage.getScene().getWindow());
                if (file != null) {
                    selectedImagePath = file.toURI().toString();
                    if (lblImageName != null) lblImageName.setText(file.getName());
                }
            });
        }
    }

    /**
     * Xu ly tao san pham — gui request CREATE_ITEM.
     */
    @FXML
    public void handleSubmit() {
        String role = ClientSession.getInstance().getCurrentRole();
        if (!"SELLER".equals(role)) {
            UiUtils.showError("Lỗi phân quyền", "Chỉ người bán (SELLER) mới được tạo sản phẩm.");
            return;
        }

        // 📌 [Tieu chi: UX — Form validation client-side]
        if (!UiUtils.validateNotEmpty(nameField, "Tên sản phẩm")) return;
        // Giá dự kiến là tuỳ chọn, mặc định 0 = chưa định giá

        String itemType = itemTypeComboBox.getValue();
        if (itemType == null) {
            UiUtils.showError("Lỗi nhập liệu", "Vui lòng chọn Loại sản phẩm.");
            return;
        }

        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        double startingPrice = 0.0; // Mặc định 0 = chưa định giá
        if (startingPriceField != null && !startingPriceField.getText().trim().isEmpty()) {
            try { startingPrice = Double.parseDouble(startingPriceField.getText().trim()); } catch (NumberFormatException ignored) {}
        }
        ObjectNode extras = JsonNodeFactory.instance.objectNode();

        // Kiểm tra và gán dữ liệu (assign data) tùy theo itemType
        switch (itemType) {
            case "ELECTRONICS" -> {
                if (!UiUtils.validateNotEmpty(brandField, "Thương hiệu")) return;
                if (!UiUtils.validatePositiveNumber(warrantyMonthsField, "Bảo hành (tháng)")) return;
                extras.put("brand", brandField.getText().trim());
                extras.put("warrantyMonths", Integer.parseInt(warrantyMonthsField.getText().trim()));
            }
            case "ART" -> {
                if (!UiUtils.validateNotEmpty(artistField, "Nghệ sĩ")) return;
                if (!UiUtils.validatePositiveNumber(yearCreatedField, "Năm sáng tác")) return;
                extras.put("artist", artistField.getText().trim());
                extras.put("yearCreated", Integer.parseInt(yearCreatedField.getText().trim()));
            }
            case "VEHICLE" -> {
                if (!UiUtils.validateNotEmpty(manufacturerField, "Hãng xe")) return;
                if (!UiUtils.validatePositiveNumber(yearField, "Năm sản xuất")) return;
                if (!UiUtils.validatePositiveNumber(mileageKmField, "Số KM đã đi")) return;
                extras.put("manufacturer", manufacturerField.getText().trim());
                extras.put("year", Integer.parseInt(yearField.getText().trim()));
                extras.put("mileageKm", Integer.parseInt(mileageKmField.getText().trim()));
            }
        }

        // 📌 [Tieu chi: UX — Loading state]
        Runnable onComplete = (btnSubmit != null && loadingSpinner != null)
                ? UiUtils.showLoading(btnSubmit, loadingSpinner)
                : () -> { if (btnSubmit != null) btnSubmit.setDisable(false); };

        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("name", name);
        payload.put("description", description);
        payload.put("startingPrice", startingPrice);
        payload.put("itemType", itemType);
        payload.put("imageUrl", selectedImagePath);
        payload.set("extras", extras);

        MessageRequest request = new MessageRequest(
                "CREATE_ITEM", ClientSession.getInstance().getToken(), payload);

        lblMessage.setVisible(false);

        NetworkTask<MessageResponse> task = new NetworkTask<>(() ->
                ServerGateway.getInstance().sendRequest(request));

        task.setOnSucceeded(e -> {
            MessageResponse response = task.getValue();
            if (response.isOk()) {
                UiUtils.showInfo("Thành công", "Đã tạo sản phẩm mới thành công!");
                ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
            } else {
                Platform.runLater(() -> UiUtils.showError("Lỗi tạo sản phẩm", response.getMessage()));
            }
            onComplete.run();
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> UiUtils.showError("Lỗi kết nối", "Không kết nối được máy chủ. Thử lại sau."));
            onComplete.run();
        });

        new Thread(task, "create-item").start();
    }

    @FXML
    public void handleCancel() {
        ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
    }
}