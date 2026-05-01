package com.bidhub.client.controller;

import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/** Skeleton form tạo item mới. Logic validate và gửi request sẽ thêm từ Tuần 5. */
public class CreateItemController {

    @FXML private TextField tfName;
    @FXML private TextArea  taDescription;
    @FXML private TextField tfStartingPrice;
    @FXML private ComboBox<String> cbItemType;
    @FXML private TextField tfExtra1; // brand / artist / manufacturer
    @FXML private TextField tfExtra2; // warrantyMonths / yearCreated / year
    @FXML private TextField tfExtra3; // mileageKm — chỉ dùng khi cbItemType = VEHICLE
    @FXML private Label lblMessage;
    @FXML private Button btnSubmit;

    @FXML
    public void initialize() {
        cbItemType.setItems(FXCollections.observableArrayList("ELECTRONICS", "ART", "VEHICLE"));
        cbItemType.setValue("ELECTRONICS");
        lblMessage.setVisible(false);
    }

    @FXML
    private void handleSubmit() {
        // Skeleton — Tuần 5 thêm validation + NetworkClient gọi CREATE_ITEM request
        lblMessage.setText("Chức năng tạo item sẽ sẵn sàng từ Tuần 5.");
        lblMessage.setVisible(true);
    }

    @FXML
    private void handleCancel() {
        ViewRouter.getInstance().navigateTo(Views.AUCTION_LIST);
    }
}