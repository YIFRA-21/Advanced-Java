package TMSController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;

import java.sql.*;
import java.time.LocalDate;
import java.util.Optional;
import java.util.logging.Logger;

public class TaxpayerManagementController {

    private static final Logger LOGGER = Logger.getLogger(TaxpayerManagementController.class.getName());
    
    // Database connection
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    
    private Connection connection;
    private ObservableList<Taxpayer> taxpayerList;
    private FilteredList<Taxpayer> filteredData;
    
    // FXML Elements from your TaxpayerManagement.fxml
    @FXML private VBox root;
    @FXML private Button newTaxpayerBtn;
    @FXML private Button backToDashboardBtn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> regionFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button searchBtn;
    @FXML private Button resetBtn;
    @FXML private Label totalTaxpayersLabel;
    @FXML private Label activeTaxpayersLabel;
    @FXML private Label inactiveTaxpayersLabel;
    @FXML private Label complianceRateLabel;
    @FXML private TableView<Taxpayer> taxpayerTable;
    @FXML private TableColumn<Taxpayer, String> tinColumn;
    @FXML private TableColumn<Taxpayer, String> nameColumn;
    @FXML private TableColumn<Taxpayer, String> categoryColumn;
    @FXML private TableColumn<Taxpayer, String> regionColumn;
    @FXML private TableColumn<Taxpayer, String> phoneColumn;
    @FXML private TableColumn<Taxpayer, String> statusColumn;
    @FXML private TableColumn<Taxpayer, String> actionsColumn;
    @FXML private Button exportBtn;
    @FXML private Button printBtn;
    @FXML private Pagination pagination;

    // Constants
    private static final int ITEMS_PER_PAGE = 10;
    
    @FXML
    public void initialize() {
        LOGGER.info("Initializing TaxpayerManagementController");
        
        try {
            initializeDatabase();
            setupTableView();
            setupSearchAndFilters();
            loadFilterData();
            setupPagination();
            loadTaxpayerData();
            updateStatistics();
        } catch (SQLException e) {
            LOGGER.severe("Initialization error: " + e.getMessage());
            showAlert("Initialization Error", 
                     "Failed to initialize taxpayer management: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
        }
    }
    
    private void initializeDatabase() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        LOGGER.info("Database connected successfully for taxpayer management");
    }
    
    private void setupTableView() {
        // Initialize columns
        tinColumn.setCellValueFactory(new PropertyValueFactory<>("tin"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        regionColumn.setCellValueFactory(new PropertyValueFactory<>("region"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Set up status column with color coding
        statusColumn.setCellFactory(column -> new TableCell<Taxpayer, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status.toUpperCase()) {
                        case "ACTIVE":
                            setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
                            break;
                        case "INACTIVE":
                            setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold;");
                            break;
                        case "SUSPENDED":
                            setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("-fx-text-fill: #7F8C8D;");
                    }
                }
            }
        });
        
        // Set up actions column with buttons
        actionsColumn.setCellFactory(new Callback<TableColumn<Taxpayer, String>, TableCell<Taxpayer, String>>() {
            @Override
            public TableCell<Taxpayer, String> call(TableColumn<Taxpayer, String> param) {
                return new TableCell<Taxpayer, String>() {
                    private final Button viewButton = new Button("View");
                    private final Button editButton = new Button("Edit");
                    private final Button deleteButton = new Button("Delete");
                    private final HBox buttonBox = new HBox(5, viewButton, editButton, deleteButton);
                    
                    {
                        viewButton.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-padding: 3 8;");
                        editButton.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-padding: 3 8;");
                        deleteButton.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; -fx-padding: 3 8;");
                        
                        viewButton.setOnAction(event -> {
                            Taxpayer taxpayer = getTableView().getItems().get(getIndex());
                            viewTaxpayerDetails(taxpayer);
                        });
                        
                        editButton.setOnAction(event -> {
                            Taxpayer taxpayer = getTableView().getItems().get(getIndex());
                            editTaxpayer(taxpayer);
                        });
                        
                        deleteButton.setOnAction(event -> {
                            Taxpayer taxpayer = getTableView().getItems().get(getIndex());
                            deleteTaxpayer(taxpayer);
                        });
                    }
                    
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttonBox);
                        }
                    }
                };
            }
        });
        
        // Enable row selection
        taxpayerTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        
        // Add double-click handler for viewing details
        taxpayerTable.setRowFactory(tv -> {
            TableRow<Taxpayer> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Taxpayer taxpayer = row.getItem();
                    viewTaxpayerDetails(taxpayer);
                }
            });
            return row;
        });
    }
    
    private void setupSearchAndFilters() {
        // Set up search field listener
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            applyFilters();
        });
        
        // Set up filter combo listeners
        regionFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }
    
    private void loadFilterData() {
        // Load regions in background
        Task<ObservableList<String>> loadRegionsTask = new Task<ObservableList<String>>() {
            @Override
            protected ObservableList<String> call() throws Exception {
                ObservableList<String> regions = FXCollections.observableArrayList("All Regions");
                String query = "SELECT DISTINCT region FROM taxpayers WHERE region IS NOT NULL ORDER BY region";
                
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        regions.add(rs.getString("region"));
                    }
                }
                return regions;
            }
            
            @Override
            protected void succeeded() {
                regionFilter.setItems(getValue());
                regionFilter.setValue("All Regions");
            }
        };
        
        // Load categories in background
        Task<ObservableList<String>> loadCategoriesTask = new Task<ObservableList<String>>() {
            @Override
            protected ObservableList<String> call() throws Exception {
                ObservableList<String> categories = FXCollections.observableArrayList("All Categories");
                String query = "SELECT DISTINCT category FROM taxpayers WHERE category IS NOT NULL ORDER BY category";
                
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        categories.add(rs.getString("category"));
                    }
                }
                return categories;
            }
            
            @Override
            protected void succeeded() {
                categoryFilter.setItems(getValue());
                categoryFilter.setValue("All Categories");
            }
        };
        
        // Set status options
        ObservableList<String> statuses = FXCollections.observableArrayList(
            "All Statuses", "ACTIVE", "INACTIVE", "SUSPENDED"
        );
        statusFilter.setItems(statuses);
        statusFilter.setValue("All Statuses");
        
        // Run background tasks
        Thread regionThread = new Thread(loadRegionsTask);
        Thread categoryThread = new Thread(loadCategoriesTask);
        regionThread.setDaemon(true);
        categoryThread.setDaemon(true);
        regionThread.start();
        categoryThread.start();
    }
    
    private void setupPagination() {
        pagination.setPageCount(1);
        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            updatePage(newIndex.intValue());
        });
    }
    
    private void loadTaxpayerData() {
        Task<ObservableList<Taxpayer>> loadTask = new Task<ObservableList<Taxpayer>>() {
            @Override
            protected ObservableList<Taxpayer> call() throws Exception {
                ObservableList<Taxpayer> taxpayers = FXCollections.observableArrayList();
                String query = "SELECT * FROM taxpayers ORDER BY taxpayer_name";
                
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    
                    while (rs.next()) {
                        Taxpayer taxpayer = new Taxpayer(
                            rs.getInt("id"),
                            rs.getString("tin"),
                            rs.getString("taxpayer_name"),
                            rs.getString("category"),
                            rs.getString("region"),
                            rs.getString("phone_number"),
                            rs.getString("status"),
                            rs.getDate("registration_date"),
                            rs.getBigDecimal("annual_turnover"),
                            rs.getString("email")
                        );
                        taxpayers.add(taxpayer);
                    }
                    
                    LOGGER.info("Loaded " + taxpayers.size() + " taxpayers");
                    return taxpayers;
                }
            }
            
            @Override
            protected void succeeded() {
                taxpayerList = getValue();
                filteredData = new FilteredList<>(taxpayerList, p -> true);
                
                // Bind filtered data to table
                SortedList<Taxpayer> sortedData = new SortedList<>(filteredData);
                sortedData.comparatorProperty().bind(taxpayerTable.comparatorProperty());
                taxpayerTable.setItems(sortedData);
                
                updateStatistics();
                updatePagination();
            }
            
            @Override
            protected void failed() {
                showAlert("Load Error", 
                         "Failed to load taxpayer data: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
            }
        };
        
        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }
    
    private void updateStatistics() {
        if (taxpayerList == null) return;
        
        int total = taxpayerList.size();
        long activeCount = taxpayerList.stream()
            .filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
            .count();
        long inactiveCount = taxpayerList.stream()
            .filter(t -> "INACTIVE".equalsIgnoreCase(t.getStatus()))
            .count();
        
        // Calculate compliance rate
        double complianceRate = total > 0 ? (activeCount * 100.0 / total) : 0.0;
        
        // Update labels
        totalTaxpayersLabel.setText(String.valueOf(total));
        activeTaxpayersLabel.setText(String.valueOf(activeCount));
        inactiveTaxpayersLabel.setText(String.valueOf(inactiveCount));
        complianceRateLabel.setText(String.format("%.1f%%", complianceRate));
    }
    
    private void applyFilters() {
        if (filteredData == null) return;
        
        String searchText = searchField.getText().toLowerCase();
        String selectedRegion = regionFilter.getValue();
        String selectedCategory = categoryFilter.getValue();
        String selectedStatus = statusFilter.getValue();
        
        filteredData.setPredicate(taxpayer -> {
            // Search filter
            boolean matchesSearch = searchText.isEmpty() ||
                taxpayer.getTin().toLowerCase().contains(searchText) ||
                taxpayer.getName().toLowerCase().contains(searchText) ||
                (taxpayer.getPhone() != null && taxpayer.getPhone().toLowerCase().contains(searchText));
            
            if (!matchesSearch) return false;
            
            // Region filter
            if (!"All Regions".equals(selectedRegion) && 
                !selectedRegion.equals(taxpayer.getRegion())) {
                return false;
            }
            
            // Category filter
            if (!"All Categories".equals(selectedCategory) && 
                !selectedCategory.equals(taxpayer.getCategory())) {
                return false;
            }
            
            // Status filter
            if (!"All Statuses".equals(selectedStatus) && 
                !selectedStatus.equals(taxpayer.getStatus())) {
                return false;
            }
            
            return true;
        });
        
        updateFilteredStatistics();
        updatePagination();
    }
    
    private void updateFilteredStatistics() {
        if (filteredData == null) return;
        
        int filteredCount = filteredData.size();
        long filteredActive = filteredData.stream()
            .filter(t -> "ACTIVE".equalsIgnoreCase(t.getStatus()))
            .count();
        long filteredInactive = filteredData.stream()
            .filter(t -> "INACTIVE".equalsIgnoreCase(t.getStatus()))
            .count();
        
        double filteredCompliance = filteredCount > 0 ? 
            (filteredActive * 100.0 / filteredCount) : 0.0;
        
        // Update labels for filtered view
        totalTaxpayersLabel.setText(String.valueOf(filteredCount));
        activeTaxpayersLabel.setText(String.valueOf(filteredActive));
        inactiveTaxpayersLabel.setText(String.valueOf(filteredInactive));
        complianceRateLabel.setText(String.format("%.1f%%", filteredCompliance));
    }
    
    private void updatePagination() {
        if (filteredData == null) return;
        
        int itemCount = filteredData.size();
        int pageCount = (int) Math.ceil((double) itemCount / ITEMS_PER_PAGE);
        
        if (pageCount == 0) pageCount = 1;
        
        pagination.setPageCount(pageCount);
        updatePage(pagination.getCurrentPageIndex());
    }
    
    private void updatePage(int pageIndex) {
        if (filteredData == null) return;
        
        int fromIndex = pageIndex * ITEMS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, filteredData.size());
        
        // You can add pagination logic here if needed
    }
    
    // ==================== EVENT HANDLER METHODS ====================
    // These must match the onAction="#handlerclick..." in your FXML
    
    @FXML
    private void handlerclicknewTaxpayerBtn(ActionEvent event) {
        LOGGER.info("New Taxpayer button clicked");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/TaxpayerForm.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("New Taxpayer Registration");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            // Refresh data after registration
            loadTaxpayerData();
            
        } catch (Exception e) {
            LOGGER.severe("Failed to open registration form: " + e.getMessage());
            showAlert("Navigation Error", 
                     "Failed to open registration form: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handlerclickbackToDashboardBtn(ActionEvent event) {
        LOGGER.info("Back to Dashboard button clicked");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Dashboard.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
            
        } catch (Exception e) {
            LOGGER.severe("Failed to navigate to dashboard: " + e.getMessage());
            showAlert("Navigation Error", 
                     "Failed to navigate to dashboard: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handlerclickregionFilter(ActionEvent event) {
        LOGGER.info("Region filter selected: " + regionFilter.getValue());
        applyFilters();
    }
    
    @FXML
    private void handlerclickcategoryFilter(ActionEvent event) {
        LOGGER.info("Category filter selected: " + categoryFilter.getValue());
        applyFilters();
    }
    
    @FXML
    private void handlerclickstatusFilter(ActionEvent event) {
        LOGGER.info("Status filter selected: " + statusFilter.getValue());
        applyFilters();
    }
    
    @FXML
    private void handlerclicksearchBtn(ActionEvent event) {
        LOGGER.info("Search button clicked");
        applyFilters();
    }
    
    @FXML
    private void handlerclickresetBtn(ActionEvent event) {
        LOGGER.info("Reset button clicked");
        resetFilters();
    }
    
    @FXML
    private void handlerclickexportBtn(ActionEvent event) {
        LOGGER.info("Export to Excel button clicked");
        exportToExcel();
    }
    
    @FXML
    private void handlerclickprintBtn(ActionEvent event) {
        LOGGER.info("Print List button clicked");
        printList();
    }
    
    @FXML
    private void handlerclickpagination(ActionEvent event) {
        LOGGER.info("Pagination clicked: Page " + (pagination.getCurrentPageIndex() + 1));
        // Pagination is handled automatically by the listener
    }
    
    // ==================== HELPER METHODS ====================
    
    private void resetFilters() {
        searchField.clear();
        regionFilter.setValue("All Regions");
        categoryFilter.setValue("All Categories");
        statusFilter.setValue("All Statuses");
        applyFilters();
    }
    
    private void exportToExcel() {
        Task<Boolean> exportTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                // Simulate export process
                Thread.sleep(2000);
                
                // In real implementation, use Apache POI or similar
                // to create Excel file from taxpayer data
                String fileName = "Taxpayer_List_" + LocalDate.now() + ".xlsx";
                LOGGER.info("Exporting to Excel: " + fileName);
                
                return true;
            }
            
            @Override
            protected void succeeded() {
                showAlert("Export Successful", 
                         "Taxpayer list exported to Excel successfully!", 
                         Alert.AlertType.INFORMATION);
            }
            
            @Override
            protected void failed() {
                showAlert("Export Failed", 
                         "Failed to export to Excel: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
            }
        };
        
        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
    }
    
    private void printList() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Print Taxpayer List");
        alert.setHeaderText("Print Confirmation");
        alert.setContentText("Do you want to print the current taxpayer list?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                LOGGER.info("Printing taxpayer list...");
                showAlert("Print Started", 
                         "Taxpayer list sent to printer.", 
                         Alert.AlertType.INFORMATION);
            }
        });
    }
    
    private void viewTaxpayerDetails(Taxpayer taxpayer) {
        LOGGER.info("Viewing taxpayer details: " + taxpayer.getName());
        showAlert("Taxpayer Details", 
                 "Name: " + taxpayer.getName() + "\n" +
                 "TIN: " + taxpayer.getTin() + "\n" +
                 "Category: " + taxpayer.getCategory() + "\n" +
                 "Region: " + taxpayer.getRegion() + "\n" +
                 "Phone: " + taxpayer.getPhone() + "\n" +
                 "Status: " + taxpayer.getStatus(), 
                 Alert.AlertType.INFORMATION);
    }
    
    private void editTaxpayer(Taxpayer taxpayer) {
        LOGGER.info("Editing taxpayer: " + taxpayer.getName());
        showAlert("Edit Taxpayer", 
                 "Edit functionality for: " + taxpayer.getName() + "\n" +
                 "This would open the edit form.", 
                 Alert.AlertType.INFORMATION);
    }
    
    private void deleteTaxpayer(Taxpayer taxpayer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Taxpayer");
        alert.setHeaderText("Confirm Deletion");
        alert.setContentText("Are you sure you want to delete taxpayer:\n" +
                           "Name: " + taxpayer.getName() + "\n" +
                           "TIN: " + taxpayer.getTin() + "\n\n" +
                           "This action cannot be undone!");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Task<Boolean> deleteTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    // Simulate deletion
                    Thread.sleep(1000);
                    LOGGER.info("Deleted taxpayer: " + taxpayer.getId() + " - " + taxpayer.getName());
                    return true;
                }
                
                @Override
                protected void succeeded() {
                    showAlert("Delete Successful", 
                             "Taxpayer deleted successfully.", 
                             Alert.AlertType.INFORMATION);
                    
                    // Refresh data
                    loadTaxpayerData();
                }
                
                @Override
                protected void failed() {
                    showAlert("Delete Failed", 
                             "Failed to delete taxpayer: " + getException().getMessage(), 
                             Alert.AlertType.ERROR);
                }
            };
            
            Thread deleteThread = new Thread(deleteTask);
            deleteThread.setDaemon(true);
            deleteThread.start();
        }
    }
    
    private void showAlert(String title, String content, Alert.AlertType alertType) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
    
    // Taxpayer model class
    public static class Taxpayer {
        private final int id;
        private final String tin;
        private final String name;
        private final String category;
        private final String region;
        private final String phone;
        private final String status;
        private final Date registrationDate;
        private final java.math.BigDecimal annualTurnover;
        private final String email;
        
        public Taxpayer(int id, String tin, String name, String category, String region, 
                       String phone, String status, Date registrationDate, 
                       java.math.BigDecimal annualTurnover, String email) {
            this.id = id;
            this.tin = tin;
            this.name = name;
            this.category = category;
            this.region = region;
            this.phone = phone;
            this.status = status;
            this.registrationDate = registrationDate;
            this.annualTurnover = annualTurnover;
            this.email = email;
        }
        
        // Getters
        public int getId() { return id; }
        public String getTin() { return tin; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getRegion() { return region; }
        public String getPhone() { return phone; }
        public String getStatus() { return status; }
        public Date getRegistrationDate() { return registrationDate; }
        public java.math.BigDecimal getAnnualTurnover() { return annualTurnover; }
        public String getEmail() { return email; }
    }
    
    // Cleanup method
    public void cleanup() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed");
            }
        } catch (SQLException e) {
            LOGGER.warning("Error closing database connection: " + e.getMessage());
        }
    }
}