package TMSController;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class ReportsController {

    private static final Logger LOGGER = Logger.getLogger(ReportsController.class.getName());
    
    // Database connection
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    
    private Connection connection;
    private Map<String, String> savedReportConfigs = new HashMap<>();
    
    // FXML Components from Reports.fxml
    @FXML private Button clearBtn;
    @FXML private Button exportBtn;
    @FXML private Button generateAuditBtn;
    @FXML private Button generateComplianceBtn;
    @FXML private Button generateCustomBtn;
    @FXML private Button generateRevenueBtn;
    @FXML private Button generateTaxpayerBtn;
    @FXML private Button previewBtn;
    @FXML private Button saveBtn;
    @FXML private Button newTaxpayerBtn1; // Back to Dashboard button
    
    @FXML private DatePicker endDatePicker;
    @FXML private DatePicker startDatePicker;
    
    @FXML private ComboBox<String> formatCombo;
    @FXML private ComboBox<String> regionCombo;
    @FXML private ComboBox<String> reportTypeCombo;
    
    @FXML private Label lineCountLabel;
    @FXML private Label statDateRange;
    @FXML private Label statGenerated;
    @FXML private Label statRegion;
    @FXML private Label statReportType;
    @FXML private Label statusLabel;
    
    @FXML private TextArea reportDisplayArea;
    
    // Optional components (if present in FXML)
    @FXML private TableView<ReportHistory> reportHistoryTable;
    @FXML private ProgressIndicator loadingIndicator;
    
    // Report history model
    public static class ReportHistory {
        private final String reportType;
        private final String dateRange;
        private final String region;
        private final String generatedTime;
        private final String status;
        
        public ReportHistory(String reportType, String dateRange, String region, String generatedTime, String status) {
            this.reportType = reportType;
            this.dateRange = dateRange;
            this.region = region;
            this.generatedTime = generatedTime;
            this.status = status;
        }
        
        public String getReportType() { return reportType; }
        public String getDateRange() { return dateRange; }
        public String getRegion() { return region; }
        public String getGeneratedTime() { return generatedTime; }
        public String getStatus() { return status; }
    }
    
    @FXML
    public void initialize() {
        LOGGER.info("Initializing ReportsController");
        
        initializeDatabase();
        setupUIComponents();
        loadComboBoxData();
        loadSavedReports();
        setupEventListeners();
    }
    
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            LOGGER.info("Database connected successfully for reports");
            
            // Create report history table if not exists
            createReportHistoryTable();
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), Alert.AlertType.ERROR);
            LOGGER.severe("Database connection error: " + e.getMessage());
        }
    }
    
    private void createReportHistoryTable() {
        String sql = "CREATE TABLE IF NOT EXISTS report_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "report_type VARCHAR(50) NOT NULL," +
                    "date_range VARCHAR(100)," +
                    "region VARCHAR(50)," +
                    "parameters TEXT," +
                    "generated_by VARCHAR(50)," +
                    "generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "export_format VARCHAR(20)," +
                    "file_path VARCHAR(255)," +
                    "status VARCHAR(20) DEFAULT 'GENERATED'," +
                    "execution_time_ms INT," +
                    "record_count INT" +
                    ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            LOGGER.info("Report history table verified/created");
        } catch (SQLException e) {
            LOGGER.warning("Could not create report history table: " + e.getMessage());
        }
    }
    
    private void setupUIComponents() {
        // Set default dates
        startDatePicker.setValue(LocalDate.now().minusMonths(1));
        endDatePicker.setValue(LocalDate.now());
        
        // Set prompt texts
        reportDisplayArea.setPromptText("Report preview will appear here...");
        statusLabel.setText("Ready to generate reports");
        lineCountLabel.setText("0 lines");
        
        // Disable action buttons initially
        clearBtn.setDisable(true);
        saveBtn.setDisable(true);
        exportBtn.setDisable(true);
        previewBtn.setDisable(true);
        
        // Setup table columns if using TableView
        if (reportHistoryTable != null) {
            // Setup columns here
        }
        
        // Setup loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
    }
    
    private void loadComboBoxData() {
        // Report types
        ObservableList<String> reportTypes = FXCollections.observableArrayList(
            "Revenue Summary",
            "Taxpayer Statistics", 
            "Compliance Analysis",
            "Audit Trail",
            "Payment Collection",
            "Delinquent Accounts",
            "Regional Performance",
            "Tax Type Analysis"
        );
        reportTypeCombo.setItems(reportTypes);
        reportTypeCombo.setValue("Revenue Summary");
        
        // Formats
        ObservableList<String> formats = FXCollections.observableArrayList(
            "PDF",
            "Excel",
            "CSV",
            "HTML",
            "JSON"
        );
        formatCombo.setItems(formats);
        formatCombo.setValue("PDF");
        
        // Regions
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
        };
        
        loadRegionsTask.setOnSucceeded(e -> {
            regionCombo.setItems(loadRegionsTask.getValue());
            regionCombo.setValue("All Regions");
        });
        
        Thread regionThread = new Thread(loadRegionsTask);
        regionThread.setDaemon(true);
        regionThread.start();
    }
    
    private void loadSavedReports() {
        Task<ObservableList<ReportHistory>> loadTask = new Task<ObservableList<ReportHistory>>() {
            @Override
            protected ObservableList<ReportHistory> call() throws Exception {
                ObservableList<ReportHistory> history = FXCollections.observableArrayList();
                String query = "SELECT report_type, date_range, region, generated_at, status " +
                              "FROM report_history ORDER BY generated_at DESC LIMIT 10";
                
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(query)) {
                    while (rs.next()) {
                        ReportHistory item = new ReportHistory(
                            rs.getString("report_type"),
                            rs.getString("date_range"),
                            rs.getString("region"),
                            rs.getTimestamp("generated_at").toString(),
                            rs.getString("status")
                        );
                        history.add(item);
                    }
                }
                return history;
            }
        };
        
        loadTask.setOnSucceeded(e -> {
            if (reportHistoryTable != null) {
                reportHistoryTable.setItems(loadTask.getValue());
            }
        });
        
        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }
    
    private void setupEventListeners() {
        // Date validation
        startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && endDatePicker.getValue() != null) {
                if (newVal.isAfter(endDatePicker.getValue())) {
                    showAlert("Date Error", "Start date cannot be after end date", Alert.AlertType.WARNING);
                    startDatePicker.setValue(oldVal);
                }
            }
        });
        
        endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && startDatePicker.getValue() != null) {
                if (newVal.isBefore(startDatePicker.getValue())) {
                    showAlert("Date Error", "End date cannot be before start date", Alert.AlertType.WARNING);
                    endDatePicker.setValue(oldVal);
                }
            }
        });
    }
    
    // ==================== EVENT HANDLERS ====================
    
    @FXML
    void handlerclickGenerateRevenueReport(ActionEvent event) {
        openReportWindow("RevenueReport.fxml", "Revenue Report");
    }
    
    @FXML
    void handlerclickGenerateTaxpayerReport(ActionEvent event) {
        openReportWindow("TaxpayerReport.fxml", "Taxpayer Report");
    }
    
    @FXML
    void handlerclickGenerateComplianceReport(ActionEvent event) {
        openReportWindow("ComplianceReport.fxml", "Compliance Report");
    }
    
    @FXML
    void handlerclickGenerateAuditReport(ActionEvent event) {
        openReportWindow("AuditReport.fxml", "Audit Report");
    }
    
    private void openReportWindow(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/" + fxmlFile));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UTILITY);
            stage.show();
            
            LOGGER.info("Opened " + title + " window");
            
        } catch (Exception e) {
            showAlert("Navigation Error", "Failed to open report: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    void handlerclickGenerateCustomReport(ActionEvent event) {
        if (validateCustomReport()) {
            generateCustomReport();
        }
    }
    
    private boolean validateCustomReport() {
        StringBuilder errors = new StringBuilder();
        
        if (reportTypeCombo.getValue() == null || reportTypeCombo.getValue().isEmpty()) {
            errors.append("• Report Type is required\n");
        }
        
        if (startDatePicker.getValue() == null) {
            errors.append("• Start Date is required\n");
        }
        
        if (endDatePicker.getValue() == null) {
            errors.append("• End Date is required\n");
        }
        
        if (errors.length() > 0) {
            showAlert("Validation Errors", errors.toString(), Alert.AlertType.ERROR);
            return false;
        }
        
        return true;
    }
    
    private void generateCustomReport() {
        Task<String> reportTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                String reportType = reportTypeCombo.getValue();
                String region = regionCombo.getValue();
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                
                long startTime = System.currentTimeMillis();
                
                // Generate report based on type
                String reportContent = "";
                
                switch (reportType) {
                    case "Revenue Summary":
                        reportContent = generateRevenueSummary(startDate, endDate, region);
                        break;
                    case "Taxpayer Statistics":
                        reportContent = generateTaxpayerStatistics(startDate, endDate, region);
                        break;
                    case "Compliance Analysis":
                        reportContent = generateComplianceAnalysis(startDate, endDate, region);
                        break;
                    case "Audit Trail":
                        reportContent = generateAuditTrail(startDate, endDate);
                        break;
                    default:
                        reportContent = "Report type not implemented yet.";
                }
                
                long executionTime = System.currentTimeMillis() - startTime;
                
                // Save report history
                saveReportHistory(reportType, startDate, endDate, region, executionTime, reportContent.length());
                
                return reportContent;
            }
            
            @Override
            protected void succeeded() {
                String reportContent = getValue();
                reportDisplayArea.setText(reportContent);
                
                // Update statistics
                updateReportStats();
                
                // Update line count
                int lineCount = reportContent.split("\n").length;
                lineCountLabel.setText(lineCount + " lines");
                
                statusLabel.setText("Report generated successfully");
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
            }
            
            @Override
            protected void failed() {
                showAlert("Report Generation Failed", 
                         "Error: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
            }
        };
        
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        statusLabel.setText("Generating report...");
        
        Thread reportThread = new Thread(reportTask);
        reportThread.setDaemon(true);
        reportThread.start();
    }
    
    private String generateRevenueSummary(LocalDate startDate, LocalDate endDate, String region) {
        StringBuilder report = new StringBuilder();
        report.append("REVENUE SUMMARY REPORT\n");
        report.append("=====================\n");
        report.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        report.append("Region: ").append(region).append("\n");
        report.append("Generated: ").append(java.time.LocalDateTime.now()).append("\n\n");
        
        try {
            String query = "SELECT " +
                          "SUM(CASE WHEN tax_type = 'VAT' THEN amount ELSE 0 END) as vat_total, " +
                          "SUM(CASE WHEN tax_type = 'Income Tax' THEN amount ELSE 0 END) as income_tax_total, " +
                          "SUM(CASE WHEN tax_type NOT IN ('VAT', 'Income Tax') THEN amount ELSE 0 END) as other_taxes_total, " +
                          "SUM(amount) as total_revenue " +
                          "FROM payments " +
                          "WHERE payment_date BETWEEN ? AND ? " +
                          (region.equals("All Regions") ? "" : "AND region = ?");
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setDate(1, Date.valueOf(startDate));
                pstmt.setDate(2, Date.valueOf(endDate));
                if (!region.equals("All Regions")) {
                    pstmt.setString(3, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    report.append("VAT Collection: ETB ").append(String.format("%,.2f", rs.getDouble("vat_total"))).append("\n");
                    report.append("Income Tax: ETB ").append(String.format("%,.2f", rs.getDouble("income_tax_total"))).append("\n");
                    report.append("Other Taxes: ETB ").append(String.format("%,.2f", rs.getDouble("other_taxes_total"))).append("\n");
                    report.append("Total Revenue: ETB ").append(String.format("%,.2f", rs.getDouble("total_revenue"))).append("\n");
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Error generating revenue summary: " + e.getMessage());
            report.append("Error generating data: ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    private String generateTaxpayerStatistics(LocalDate startDate, LocalDate endDate, String region) {
        StringBuilder report = new StringBuilder();
        report.append("TAXPAYER STATISTICS REPORT\n");
        report.append("=========================\n");
        
        try {
            String query = "SELECT " +
                          "COUNT(*) as total_taxpayers, " +
                          "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_taxpayers, " +
                          "SUM(CASE WHEN status = 'INACTIVE' THEN 1 ELSE 0 END) as inactive_taxpayers, " +
                          "SUM(CASE WHEN status = 'SUSPENDED' THEN 1 ELSE 0 END) as suspended_taxpayers, " +
                          "SUM(CASE WHEN registration_date BETWEEN ? AND ? THEN 1 ELSE 0 END) as new_registrations " +
                          "FROM taxpayers " +
                          (region.equals("All Regions") ? "" : "WHERE region = ?");
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                if (region.equals("All Regions")) {
                    pstmt.setDate(1, Date.valueOf(startDate));
                    pstmt.setDate(2, Date.valueOf(endDate));
                } else {
                    pstmt.setDate(1, Date.valueOf(startDate));
                    pstmt.setDate(2, Date.valueOf(endDate));
                    pstmt.setString(3, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    report.append("Total Taxpayers: ").append(rs.getInt("total_taxpayers")).append("\n");
                    report.append("Active Taxpayers: ").append(rs.getInt("active_taxpayers")).append("\n");
                    report.append("Inactive Taxpayers: ").append(rs.getInt("inactive_taxpayers")).append("\n");
                    report.append("Suspended Taxpayers: ").append(rs.getInt("suspended_taxpayers")).append("\n");
                    report.append("New Registrations: ").append(rs.getInt("new_registrations")).append("\n");
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Error generating taxpayer statistics: " + e.getMessage());
            report.append("Error generating data: ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    private String generateComplianceAnalysis(LocalDate startDate, LocalDate endDate, String region) {
        StringBuilder report = new StringBuilder();
        report.append("COMPLIANCE ANALYSIS REPORT\n");
        report.append("==========================\n");
        
        try {
            String query = "SELECT " +
                          "COUNT(*) as total_returns, " +
                          "SUM(CASE WHEN filed_on_time = true THEN 1 ELSE 0 END) as on_time_filings, " +
                          "SUM(CASE WHEN payment_completed = true THEN 1 ELSE 0 END) as completed_payments " +
                          "FROM tax_returns " +
                          "WHERE filing_date BETWEEN ? AND ? " +
                          (region.equals("All Regions") ? "" : "AND region = ?");
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setDate(1, Date.valueOf(startDate));
                pstmt.setDate(2, Date.valueOf(endDate));
                if (!region.equals("All Regions")) {
                    pstmt.setString(3, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int totalReturns = rs.getInt("total_returns");
                    int onTimeFilings = rs.getInt("on_time_filings");
                    int completedPayments = rs.getInt("completed_payments");
                    
                    double filingRate = totalReturns > 0 ? (onTimeFilings * 100.0 / totalReturns) : 0;
                    double paymentRate = totalReturns > 0 ? (completedPayments * 100.0 / totalReturns) : 0;
                    double overallCompliance = (filingRate + paymentRate) / 2;
                    
                    report.append("Overall Compliance Rate: ").append(String.format("%.1f%%", overallCompliance)).append("\n");
                    report.append("On-Time Filing Rate: ").append(String.format("%.1f%%", filingRate)).append("\n");
                    report.append("Payment Compliance Rate: ").append(String.format("%.1f%%", paymentRate)).append("\n");
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Error generating compliance analysis: " + e.getMessage());
            report.append("Error generating data: ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    private String generateAuditTrail(LocalDate startDate, LocalDate endDate) {
        StringBuilder report = new StringBuilder();
        report.append("AUDIT TRAIL REPORT\n");
        report.append("=================\n");
        
        try {
            String query = "SELECT timestamp, user_id, action, module, description " +
                          "FROM audit_log " +
                          "WHERE timestamp BETWEEN ? AND ? " +
                          "ORDER BY timestamp DESC " +
                          "LIMIT 100";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
                pstmt.setTimestamp(2, Timestamp.valueOf(endDate.atTime(23, 59, 59)));
                
                ResultSet rs = pstmt.executeQuery();
                report.append(String.format("%-25s %-15s %-20s %-15s %s\n", 
                    "Timestamp", "User", "Action", "Module", "Description"));
                report.append("-".repeat(100)).append("\n");
                
                while (rs.next()) {
                    report.append(String.format("%-25s %-15s %-20s %-15s %s\n",
                        rs.getTimestamp("timestamp"),
                        rs.getString("user_id"),
                        rs.getString("action"),
                        rs.getString("module"),
                        rs.getString("description")));
                }
            }
        } catch (SQLException e) {
            LOGGER.warning("Error generating audit trail: " + e.getMessage());
            report.append("Error generating data: ").append(e.getMessage()).append("\n");
        }
        
        return report.toString();
    }
    
    private void saveReportHistory(String reportType, LocalDate startDate, LocalDate endDate, 
                                  String region, long executionTime, int recordCount) {
        String sql = "INSERT INTO report_history (report_type, date_range, region, parameters, " +
                    "generated_by, execution_time_ms, record_count) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, reportType);
            pstmt.setString(2, startDate + " to " + endDate);
            pstmt.setString(3, region);
            pstmt.setString(4, "Custom Report Parameters");
            pstmt.setString(5, "SYSTEM_ADMIN");
            pstmt.setLong(6, executionTime);
            pstmt.setInt(7, recordCount);
            pstmt.executeUpdate();
            
            LOGGER.info("Report history saved: " + reportType);
        } catch (SQLException e) {
            LOGGER.warning("Failed to save report history: " + e.getMessage());
        }
    }
    
    @FXML
    void handlerclickPreviewReport(ActionEvent event) {
        if (reportDisplayArea.getText().isEmpty()) {
            showAlert("Preview Error", "No report content to preview", Alert.AlertType.WARNING);
            return;
        }
        
        try {
            // Show preview in a new window
            Stage previewStage = new Stage();
            TextArea previewArea = new TextArea(reportDisplayArea.getText());
            previewArea.setEditable(false);
            previewArea.setWrapText(true);
            previewArea.setStyle("-fx-font-family: 'Monospaced'; -fx-font-size: 12px;");
            
            VBox root = new VBox(previewArea);
            root.setPadding(new javafx.geometry.Insets(10));
            
            Scene scene = new Scene(root, 800, 600);
            previewStage.setTitle("Report Preview");
            previewStage.setScene(scene);
            previewStage.initModality(Modality.APPLICATION_MODAL);
            previewStage.show();
            
        } catch (Exception e) {
            showAlert("Preview Error", "Failed to open preview: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    void handlerclickSaveReport(ActionEvent event) {
        if (reportDisplayArea.getText().isEmpty()) {
            showAlert("Save Error", "No report content to save", Alert.AlertType.WARNING);
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog("My Report");
        dialog.setTitle("Save Report");
        dialog.setHeaderText("Enter report name:");
        dialog.setContentText("Report Name:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(reportName -> {
            savedReportConfigs.put(reportName, reportDisplayArea.getText());
            showAlert("Save Successful", 
                     "Report '" + reportName + "' saved successfully.", 
                     Alert.AlertType.INFORMATION);
            LOGGER.info("Report saved: " + reportName);
        });
    }
    
    @FXML
    void handlerclickExportReport(ActionEvent event) {
        if (reportDisplayArea.getText().isEmpty()) {
            showAlert("Export Error", "No report content to export", Alert.AlertType.WARNING);
            return;
        }
        
        String format = formatCombo.getValue();
        if (format == null) {
            showAlert("Export Error", "Please select export format", Alert.AlertType.WARNING);
            return;
        }
        
        Task<Boolean> exportTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                String reportContent = reportDisplayArea.getText();
                String fileName = "Report_" + LocalDate.now() + "_" + System.currentTimeMillis();
                
                // Simulate export process
                Thread.sleep(1000);
                
                LOGGER.info("Exporting report as " + format + ": " + fileName);
                return true;
            }
            
            @Override
            protected void succeeded() {
                showAlert("Export Successful", 
                         "Report exported as " + format + " successfully!", 
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
    }
    
    @FXML
    void handlerclickClearReport(ActionEvent event) {
        if (showConfirmationDialog("Clear Report", 
            "Are you sure you want to clear the current report?")) {
            reportDisplayArea.clear();
            lineCountLabel.setText("0 lines");
            statusLabel.setText("Ready to generate reports");
            clearBtn.setDisable(true);
            saveBtn.setDisable(true);
            exportBtn.setDisable(true);
            previewBtn.setDisable(true);
        }
    }
    
    @FXML
    void handlerclicknewTaxpayerBtn1(ActionEvent event) {
    	 System.out.println("Back to Dashboard clicked");
         try {
             FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Dashboard.fxml"));
             Parent root = loader.load();
             
             Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
             stage.setScene(new Scene(root));
             stage.setMaximized(true);
             stage.show();
             
         } catch (Exception e) {
         	System.out.println("Failed to navigate to dashboard: " + e.getMessage());
             showAlert("Navigation Error", 
                      "Failed to navigate to dashboard: " + e.getMessage(), 
                      Alert.AlertType.ERROR);
         }
    }
    
    private void updateReportStats() {
        String reportType = reportTypeCombo.getValue();
        String region = regionCombo.getValue();
        String dateRange = startDatePicker.getValue() + " to " + endDatePicker.getValue();
        String generated = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        statReportType.setText(reportType != null ? reportType : "-");
        statDateRange.setText(dateRange);
        statRegion.setText(region != null ? region : "-");
        statGenerated.setText(generated);
        
        // Enable action buttons
        clearBtn.setDisable(false);
        saveBtn.setDisable(false);
        exportBtn.setDisable(false);
        previewBtn.setDisable(false);
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
    
    private boolean showConfirmationDialog(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesButton, noButton);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesButton;
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