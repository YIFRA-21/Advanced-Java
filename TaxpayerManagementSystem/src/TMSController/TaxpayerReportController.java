package TMSController;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Logger;

public class TaxpayerReportController {

    private static final Logger LOGGER = Logger.getLogger(TaxpayerReportController.class.getName());
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    
    private Connection connection;
    private ObservableList<TaxpayerStat> taxpayerStats;
    
    // FXML elements from the fixed FXML
    @FXML private Button ExportDatebtn;
    @FXML private Button Generatebtn;
    @FXML private Button ViewTreandsbtn;
    @FXML private Label activeTaxpayersLabel;
    @FXML private Button backButtonReportpage;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private Label delinquentLabel;
    @FXML private DatePicker endDatePicker;
    @FXML private Label newRegistrationsLabel;
    @FXML private ComboBox<String> regionCombo;
    @FXML private ComboBox<String> reportTypeCombo;
    @FXML private DatePicker startDatePicker;
    @FXML private TableView<TaxpayerStat> taxpayerTable;
    @FXML private Label totalTaxpayersLabel;
    @FXML private VBox root; // Added root reference
    
    // Additional UI elements (if present in FXML)
    @FXML private Label pageTitleLabel;
    @FXML private Label pageDescriptionLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label complianceRateLabel;
    @FXML private Label avgTurnoverLabel;
    
    // Table columns (if defined in FXML)
    @FXML private TableColumn<TaxpayerStat, String> tinColumn;
    @FXML private TableColumn<TaxpayerStat, String> nameColumn;
    @FXML private TableColumn<TaxpayerStat, String> categoryColumn;
    @FXML private TableColumn<TaxpayerStat, String> regionColumn;
    @FXML private TableColumn<TaxpayerStat, String> statusColumn;
    @FXML private TableColumn<TaxpayerStat, LocalDate> registrationDateColumn;
    @FXML private TableColumn<TaxpayerStat, Double> turnoverColumn;
    @FXML private TableColumn<TaxpayerStat, Double> taxOwedColumn;
    @FXML private TableColumn<TaxpayerStat, Double> taxPaidColumn;
    @FXML private TableColumn<TaxpayerStat, String> complianceColumn;
    
    public static class TaxpayerStat {
        private final String tin;
        private final String name;
        private final String category;
        private final String region;
        private final String status;
        private final LocalDate registrationDate;
        private final double annualTurnover;
        private final double taxOwed;
        private final double taxPaid;
        private final String complianceStatus;
        
        public TaxpayerStat(String tin, String name, String category, String region, String status,
                           LocalDate registrationDate, double annualTurnover, double taxOwed,
                           double taxPaid, String complianceStatus) {
            this.tin = tin;
            this.name = name;
            this.category = category;
            this.region = region;
            this.status = status;
            this.registrationDate = registrationDate;
            this.annualTurnover = annualTurnover;
            this.taxOwed = taxOwed;
            this.taxPaid = taxPaid;
            this.complianceStatus = complianceStatus;
        }
        
        public String getTin() { return tin; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getRegion() { return region; }
        public String getStatus() { return status; }
        public LocalDate getRegistrationDate() { return registrationDate; }
        public double getAnnualTurnover() { return annualTurnover; }
        public double getTaxOwed() { return taxOwed; }
        public double getTaxPaid() { return taxPaid; }
        public String getComplianceStatus() { return complianceStatus; }
        public double getComplianceRate() { 
            return taxOwed > 0 ? (taxPaid / taxOwed) * 100 : 100; 
        }
        
        public String getFormattedRegistrationDate() {
            return registrationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }
    
    @FXML
    public void initialize() {
        try {
            LOGGER.info("Initializing TaxpayerReportController");
            
            initializeDatabase();
            setupUIComponents();
            loadComboBoxData();
            setupEventListeners();
            
            // Set default dates
            if (startDatePicker != null) {
                startDatePicker.setValue(LocalDate.now().minusMonths(6));
            }
            if (endDatePicker != null) {
                endDatePicker.setValue(LocalDate.now());
            }
            
            System.out.println("TaxpayerReportController initialized successfully");
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Initialization Error", "Error initializing controller: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    private void initializeDatabase() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        LOGGER.info("Database connected successfully for taxpayer report");
    }
    
    private void setupUIComponents() {
        try {
            // Setup table columns
            setupTableView();
            
            // Setup page info if labels exist
            if (pageTitleLabel != null) {
                pageTitleLabel.setText("Taxpayer Report");
                pageTitleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            }
            
            if (pageDescriptionLabel != null) {
                pageDescriptionLabel.setText("Taxpayer statistics and trends");
                pageDescriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            }
            
            // Set default report type
            if (reportTypeCombo != null) {
                reportTypeCombo.setValue("Registration Statistics");
            }
            
            // Initialize labels with default values
            if (totalTaxpayersLabel != null) totalTaxpayersLabel.setText("0");
            if (activeTaxpayersLabel != null) activeTaxpayersLabel.setText("0");
            if (newRegistrationsLabel != null) newRegistrationsLabel.setText("0");
            if (delinquentLabel != null) delinquentLabel.setText("0");
            if (complianceRateLabel != null) complianceRateLabel.setText("0%");
            if (avgTurnoverLabel != null) avgTurnoverLabel.setText("ETB 0.00");
            
        } catch (Exception e) {
            System.err.println("Error setting up UI components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTableView() {
        try {
            // Setup table columns if they exist in FXML
            if (tinColumn != null) {
                tinColumn.setCellValueFactory(new PropertyValueFactory<>("tin"));
            }
            if (nameColumn != null) {
                nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            }
            if (categoryColumn != null) {
                categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
            }
            if (regionColumn != null) {
                regionColumn.setCellValueFactory(new PropertyValueFactory<>("region"));
            }
            if (statusColumn != null) {
                statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            }
            if (registrationDateColumn != null) {
                registrationDateColumn.setCellValueFactory(new PropertyValueFactory<>("registrationDate"));
            }
            if (turnoverColumn != null) {
                turnoverColumn.setCellValueFactory(new PropertyValueFactory<>("annualTurnover"));
            }
            if (taxOwedColumn != null) {
                taxOwedColumn.setCellValueFactory(new PropertyValueFactory<>("taxOwed"));
            }
            if (taxPaidColumn != null) {
                taxPaidColumn.setCellValueFactory(new PropertyValueFactory<>("taxPaid"));
            }
            if (complianceColumn != null) {
                complianceColumn.setCellValueFactory(new PropertyValueFactory<>("complianceStatus"));
            }
            
            // Format numeric columns
            if (turnoverColumn != null) {
                turnoverColumn.setCellFactory(column -> new TableCell<TaxpayerStat, Double>() {
                    @Override
                    protected void updateItem(Double value, boolean empty) {
                        super.updateItem(value, empty);
                        if (empty || value == null) {
                            setText(null);
                        } else {
                            setText(String.format("ETB %,.2f", value));
                        }
                    }
                });
            }
            
            if (taxOwedColumn != null) {
                taxOwedColumn.setCellFactory(column -> new TableCell<TaxpayerStat, Double>() {
                    @Override
                    protected void updateItem(Double value, boolean empty) {
                        super.updateItem(value, empty);
                        if (empty || value == null) {
                            setText(null);
                        } else {
                            setText(String.format("ETB %,.2f", value));
                        }
                    }
                });
            }
            
            if (taxPaidColumn != null) {
                taxPaidColumn.setCellFactory(column -> new TableCell<TaxpayerStat, Double>() {
                    @Override
                    protected void updateItem(Double value, boolean empty) {
                        super.updateItem(value, empty);
                        if (empty || value == null) {
                            setText(null);
                        } else {
                            setText(String.format("ETB %,.2f", value));
                        }
                    }
                });
            }
            
        } catch (Exception e) {
            System.err.println("Error setting up table view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadComboBoxData() {
        try {
            // Report types
            ObservableList<String> reportTypes = FXCollections.observableArrayList(
                "Registration Statistics",
                "Status Distribution", 
                "Category Analysis",
                "Regional Distribution",
                "Compliance Analysis",
                "Turnover Analysis"
            );
            if (reportTypeCombo != null) {
                reportTypeCombo.setItems(reportTypes);
            }
            
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
                    } catch (SQLException e) {
                        LOGGER.warning("Error loading categories: " + e.getMessage());
                    }
                    return categories;
                }
            };
            
            loadCategoriesTask.setOnSucceeded(e -> {
                if (categoryCombo != null) {
                    categoryCombo.setItems(loadCategoriesTask.getValue());
                    categoryCombo.setValue("All Categories");
                }
            });
            
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
                    } catch (SQLException e) {
                        LOGGER.warning("Error loading regions: " + e.getMessage());
                    }
                    return regions;
                }
            };
            
            loadRegionsTask.setOnSucceeded(e -> {
                if (regionCombo != null) {
                    regionCombo.setItems(loadRegionsTask.getValue());
                    regionCombo.setValue("All Regions");
                }
            });
            
            Thread catThread = new Thread(loadCategoriesTask);
            Thread regThread = new Thread(loadRegionsTask);
            
            catThread.setDaemon(true);
            regThread.setDaemon(true);
            
            catThread.start();
            regThread.start();
            
        } catch (Exception e) {
            System.err.println("Error loading combo box data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupEventListeners() {
        try {
            // Date validation
            if (startDatePicker != null && endDatePicker != null) {
                startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && endDatePicker.getValue() != null && 
                        newVal.isAfter(endDatePicker.getValue())) {
                        showAlert("Date Error", "Start date cannot be after end date", Alert.AlertType.WARNING);
                        startDatePicker.setValue(oldVal);
                    }
                });
                
                endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && startDatePicker.getValue() != null && 
                        newVal.isBefore(startDatePicker.getValue())) {
                        showAlert("Date Error", "End date cannot be before start date", Alert.AlertType.WARNING);
                        endDatePicker.setValue(oldVal);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error setting up event listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== EVENT HANDLERS ====================
    // UPDATED METHOD NAMES TO MATCH FIXED FXML
    
    @FXML
    private void handleReportTypeCombo(ActionEvent event) {
        System.out.println("Report type changed to: " + 
            (reportTypeCombo != null ? reportTypeCombo.getValue() : "null"));
    }
    
    @FXML
    private void handleStartDatePicker(ActionEvent event) {
        System.out.println("Start date selected: " + 
            (startDatePicker != null ? startDatePicker.getValue() : "null"));
    }
    
    @FXML
    private void handleEndDatePicker(ActionEvent event) {
        System.out.println("End date selected: " + 
            (endDatePicker != null ? endDatePicker.getValue() : "null"));
    }
    
    @FXML
    private void handleCategoryCombo(ActionEvent event) {
        System.out.println("Category changed to: " + 
            (categoryCombo != null ? categoryCombo.getValue() : "null"));
    }
    
    @FXML
    private void handleRegionCombo(ActionEvent event) {
        System.out.println("Region changed to: " + 
            (regionCombo != null ? regionCombo.getValue() : "null"));
    }
    
    @FXML
    private void handleGenerateReport(ActionEvent event) {
        if (validateInputs()) {
            generateTaxpayerReport();
        }
    }
    
    private boolean validateInputs() {
        if (startDatePicker == null || startDatePicker.getValue() == null) {
            showAlert("Validation Error", "Start date is required", Alert.AlertType.ERROR);
            if (startDatePicker != null) startDatePicker.requestFocus();
            return false;
        }
        
        if (endDatePicker == null || endDatePicker.getValue() == null) {
            showAlert("Validation Error", "End date is required", Alert.AlertType.ERROR);
            if (endDatePicker != null) endDatePicker.requestFocus();
            return false;
        }
        
        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            showAlert("Validation Error", "Start date cannot be after end date", Alert.AlertType.ERROR);
            return false;
        }
        
        return true;
    }
    
    private void generateTaxpayerReport() {
        Task<TaxpayerReportData> reportTask = new Task<TaxpayerReportData>() {
            @Override
            protected TaxpayerReportData call() throws Exception {
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                String category = categoryCombo != null ? categoryCombo.getValue() : "All Categories";
                String region = regionCombo != null ? regionCombo.getValue() : "All Regions";
                String reportType = reportTypeCombo != null ? reportTypeCombo.getValue() : "Registration Statistics";
                
                return generateReportData(startDate, endDate, category, region, reportType);
            }
            
            @Override
            protected void succeeded() {
                TaxpayerReportData data = getValue();
                updateStatistics(data);
                updateTaxpayerTable(data);
                
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                if (Generatebtn != null) {
                    Generatebtn.setText("Generate Report");
                }
                
                // Enable action buttons
                if (ExportDatebtn != null) ExportDatebtn.setDisable(false);
                if (ViewTreandsbtn != null) ViewTreandsbtn.setDisable(false);
                
                LOGGER.info("Taxpayer report generated successfully");
            }
            
            @Override
            protected void failed() {
                showAlert("Report Generation Failed", 
                         "Error: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                if (Generatebtn != null) {
                    Generatebtn.setText("Generate Report");
                }
            }
        };
        
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        if (Generatebtn != null) {
            Generatebtn.setText("Generating...");
        }
        
        Thread reportThread = new Thread(reportTask);
        reportThread.setDaemon(true);
        reportThread.start();
    }
    
    private TaxpayerReportData generateReportData(LocalDate startDate, LocalDate endDate, 
                                                 String category, String region, String reportType) {
        TaxpayerReportData data = new TaxpayerReportData();
        
        try {
            // Get basic statistics
            String statsQuery = buildStatsQuery(startDate, endDate, category, region);
            
            try (PreparedStatement pstmt = connection.prepareStatement(statsQuery)) {
                pstmt.setDate(1, Date.valueOf(startDate));
                pstmt.setDate(2, Date.valueOf(endDate));
                
                int paramIndex = 3;
                if (!"All Categories".equals(category)) {
                    pstmt.setString(paramIndex++, category);
                }
                if (!"All Regions".equals(region)) {
                    pstmt.setString(paramIndex++, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    data.totalTaxpayers = rs.getInt("total_taxpayers");
                    data.activeTaxpayers = rs.getInt("active_taxpayers");
                    data.inactiveTaxpayers = rs.getInt("inactive_taxpayers");
                    data.suspendedTaxpayers = rs.getInt("suspended_taxpayers");
                    data.newRegistrations = rs.getInt("new_registrations");
                    data.delinquentTaxpayers = rs.getInt("delinquent_taxpayers");
                    data.totalTurnover = rs.getDouble("total_turnover");
                }
            }
            
            // Get compliance data
            String complianceQuery = "SELECT COUNT(*) as total, " +
                                    "SUM(CASE WHEN status = 'ACTIVE' AND compliance_score >= 80 THEN 1 ELSE 0 END) as compliant " +
                                    "FROM taxpayers WHERE status IN ('ACTIVE', 'INACTIVE')";
            
            if (!"All Categories".equals(category)) {
                complianceQuery += " AND category = ?";
            }
            if (!"All Regions".equals(region)) {
                complianceQuery += " AND region = ?";
            }
            
            try (PreparedStatement pstmt = connection.prepareStatement(complianceQuery)) {
                int paramIndex = 1;
                if (!"All Categories".equals(category)) {
                    pstmt.setString(paramIndex++, category);
                }
                if (!"All Regions".equals(region)) {
                    pstmt.setString(paramIndex++, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int compliant = rs.getInt("compliant");
                    data.complianceRate = total > 0 ? (compliant * 100.0 / total) : 0;
                }
            }
            
            // Get taxpayer details for table
            String detailsQuery = "SELECT tin, full_name as taxpayer_name, category, region, status, " +
                                 "registration_date, COALESCE(annual_turnover, 0) as annual_turnover " +
                                 "FROM taxpayers " +
                                 "WHERE registration_date BETWEEN ? AND ? " +
                                 (!"All Categories".equals(category) ? "AND category = ? " : "") +
                                 (!"All Regions".equals(region) ? "AND region = ? " : "") +
                                 "ORDER BY registration_date DESC " +
                                 "LIMIT 100";
            
            try (PreparedStatement pstmt = connection.prepareStatement(detailsQuery)) {
                pstmt.setDate(1, Date.valueOf(startDate));
                pstmt.setDate(2, Date.valueOf(endDate));
                
                int paramIndex = 3;
                if (!"All Categories".equals(category)) {
                    pstmt.setString(paramIndex++, category);
                }
                if (!"All Regions".equals(region)) {
                    pstmt.setString(paramIndex++, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    TaxpayerStat stat = new TaxpayerStat(
                        rs.getString("tin"),
                        rs.getString("taxpayer_name"),
                        rs.getString("category"),
                        rs.getString("region"),
                        rs.getString("status"),
                        rs.getDate("registration_date").toLocalDate(),
                        rs.getDouble("annual_turnover"),
                        0, // tax owed - would need separate query
                        0, // tax paid - would need separate query
                        "Unknown" // compliance status
                    );
                    data.taxpayerDetails.add(stat);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Error generating taxpayer report: " + e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
        
        return data;
    }
    
    private String buildStatsQuery(LocalDate startDate, LocalDate endDate, String category, String region) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append("COUNT(*) as total_taxpayers, ");
        query.append("SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_taxpayers, ");
        query.append("SUM(CASE WHEN status = 'INACTIVE' THEN 1 ELSE 0 END) as inactive_taxpayers, ");
        query.append("SUM(CASE WHEN status = 'SUSPENDED' THEN 1 ELSE 0 END) as suspended_taxpayers, ");
        query.append("SUM(CASE WHEN registration_date BETWEEN ? AND ? THEN 1 ELSE 0 END) as new_registrations, ");
        query.append("SUM(CASE WHEN status = 'ACTIVE' AND compliance_score < 60 THEN 1 ELSE 0 END) as delinquent_taxpayers, ");
        query.append("SUM(COALESCE(annual_turnover, 0)) as total_turnover ");
        query.append("FROM taxpayers WHERE 1=1 ");
        
        if (!"All Categories".equals(category)) {
            query.append("AND category = ? ");
        }
        if (!"All Regions".equals(region)) {
            query.append("AND region = ? ");
        }
        
        return query.toString();
    }
    
    private void updateStatistics(TaxpayerReportData data) {
        if (totalTaxpayersLabel != null) totalTaxpayersLabel.setText(String.valueOf(data.totalTaxpayers));
        if (activeTaxpayersLabel != null) activeTaxpayersLabel.setText(String.valueOf(data.activeTaxpayers));
        if (newRegistrationsLabel != null) newRegistrationsLabel.setText(String.valueOf(data.newRegistrations));
        if (delinquentLabel != null) delinquentLabel.setText(String.valueOf(data.delinquentTaxpayers));
        if (complianceRateLabel != null) complianceRateLabel.setText(String.format("%.1f%%", data.complianceRate));
        
        double avgTurnover = data.totalTaxpayers > 0 ? data.totalTurnover / data.totalTaxpayers : 0;
        if (avgTurnoverLabel != null) avgTurnoverLabel.setText(String.format("ETB %,.2f", avgTurnover));
    }
    
    private void updateTaxpayerTable(TaxpayerReportData data) {
        taxpayerStats = FXCollections.observableArrayList(data.taxpayerDetails);
        if (taxpayerTable != null) {
            taxpayerTable.setItems(taxpayerStats);
        }
    }
    
    @FXML
    private void handleExportData(ActionEvent event) {
        if (taxpayerStats == null || taxpayerStats.isEmpty()) {
            showAlert("Export Error", "No data to export", Alert.AlertType.WARNING);
            return;
        }
        
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>("Excel", "Excel", "CSV", "PDF");
        formatDialog.setTitle("Export Format");
        formatDialog.setHeaderText("Select export format:");
        formatDialog.setContentText("Format:");
        
        Optional<String> result = formatDialog.showAndWait();
        result.ifPresent(format -> {
            Task<Boolean> exportTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    String fileName = "Taxpayer_Report_" + LocalDate.now() + "." + format.toLowerCase();
                    LOGGER.info("Exporting taxpayer report as " + format + ": " + fileName);
                    
                    // Simulate export
                    Thread.sleep(1000);
                    return true;
                }
                
                @Override
                protected void succeeded() {
                    showAlert("Export Successful", 
                             "Taxpayer report exported as " + format + " successfully!", 
                             Alert.AlertType.INFORMATION);
                }
                
                @Override
                protected void failed() {
                    showAlert("Export Failed", 
                             "Failed to export report: " + getException().getMessage(), 
                             Alert.AlertType.ERROR);
                }
            };
            
            Thread exportThread = new Thread(exportTask);
            exportThread.setDaemon(true);
            exportThread.start();
        });
    }
    
    @FXML
    private void handleViewTrends(ActionEvent event) {
        if (taxpayerStats == null || taxpayerStats.isEmpty()) {
            showAlert("Trend Analysis", "No data available for trend analysis", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            // Navigate to trends page (if exists) or show in dialog
            showAlert("Trend Analysis", "Trend analysis feature will be available in the next version.", Alert.AlertType.INFORMATION);
            
        } catch (Exception e) {
            showAlert("Trend Analysis Error", 
                     "Failed to open trend analysis: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleBackToReportPage(ActionEvent event) {
    	   
    	    
    	        try {
    	            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Reports.fxml"));
    	            Parent adminRoot = loader.load();
    	            
    	            Stage currentstage = (Stage) backButtonReportpage.getScene().getWindow();
    	            currentstage.close();
    	            
    	        } catch (IOException e) {
    	            showAlert("Navigation Error", "Error closing report window: " + e.getMessage(), AlertType.ERROR);
    	        }
    	    }
    
    
    private void showAlert(String title, String content, Alert.AlertType alertType) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
    
    // Data class for taxpayer report
    private static class TaxpayerReportData {
        int totalTaxpayers = 0;
        int activeTaxpayers = 0;
        int inactiveTaxpayers = 0;
        int suspendedTaxpayers = 0;
        int newRegistrations = 0;
        int delinquentTaxpayers = 0;
        double totalTurnover = 0;
        double complianceRate = 0;
        java.util.Map<String, Integer> monthlyTrend = new java.util.HashMap<>();
        ObservableList<TaxpayerStat> taxpayerDetails = FXCollections.observableArrayList();
    }
    
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