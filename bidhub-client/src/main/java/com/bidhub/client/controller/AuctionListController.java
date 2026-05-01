package com.bidhub.client.controller;

import com.bidhub.client.navigation.ViewRouter;
import com.bidhub.client.util.Views;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.Map;

/** Skeleton controller cho AuctionListView — TableView 4 cột, chưa networking. */
public class AuctionListController {

    @FXML private TableView<Map<String, String>> tableAuctions;
    @FXML private TableColumn<Map<String, String>, String> colId;
    @FXML private TableColumn<Map<String, String>, String> colItem;
    @FXML private TableColumn<Map<String, String>, String> colStatus;
    @FXML private TableColumn<Map<String, String>, String> colCurrentBid;
    @FXML private Button btnCreateAuction;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getOrDefault("id", "")));
        colItem.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getOrDefault("itemName", "")));
        colStatus.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getOrDefault("status", "")));
        colCurrentBid.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getOrDefault("currentHighestBid", "")));

        tableAuctions.setItems(FXCollections.emptyObservableList());
        tableAuctions.setPlaceholder(new Label("Chưa có phiên đấu giá nào."));
    }

    @FXML
    private void handleCreateAuction() {
        ViewRouter.getInstance().navigateTo(Views.CREATE_ITEM);
    }

    @FXML
    private void handleRowClick() {
        Map<String, String> selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected != null) {
            ViewRouter.getInstance().navigateTo(Views.AUCTION_DETAIL,
                    Map.of("auctionId", selected.get("id")));
        }
    }
}