package TMSController;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.logging.Logger;

public class ComplianceReportController {

    private static final Logger LOGGER = Logger.getLogger(ComplianceReportController.class.getName());
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    
    private Connection connection;
    private ObservableList<NonComplianceDetail> nonComplianceList;
    
    // FXML Components from ComplianceReport.fxml
    @FXML private Button ExportDetailsbtn;
    @FXML private Button GenerateAlertsbtn;
    @FXML private Button Generatereportbtn;
    @FXML private Button backButtonReportpage;
    @FXML private ComboBox<String> businessSizeCombo;
    @FXML private ProgressBar complianceProgress;
    @FXML private Label complianceRateLabel;
    @FXML private ProgressBar filingProgress;
    @FXML private Label filingRateLabel;
    @FXML private TableView<NonComplianceDetail> nonComplianceTable;
    @FXML private Label paymentComplianceLabel;
    @FXML private ProgressBar paymentProgress;
    @FXML private ComboBox<String> periodCombo;
    @FXML private ComboBox<String> regionCombo;
    @FXML private ComboBox<String> taxTypeCombo;
    @FXML private TextField yearField;
    
    // Optional components (if present in FXML)
    @FXML private PieChart complianceChart;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label totalReturnsLabel;
    @FXML private Label lateReturnsLabel;
    @FXML private Label overduePaymentsLabel;
    
    // Table columns (if using FXML for columns, these might be declared there instead)
    @FXML private TableColumn<NonComplianceDetail, String> tinColumn;
    @FXML private TableColumn<NonComplianceDetail, String> taxpayerNameColumn;
    @FXML private TableColumn<NonComplianceDetail, String> taxTypeColumn;
    @FXML private TableColumn<NonComplianceDetail, String> periodColumn;
    @FXML private TableColumn<NonComplianceDetail, LocalDate> dueDateColumn;
    @FXML private TableColumn<NonComplianceDetail, LocalDate> filedDateColumn;
    @FXML private TableColumn<NonComplianceDetail, Double> amountOwedColumn;
    @FXML private TableColumn<NonComplianceDetail, Double> penaltyColumn;
    @FXML private TableColumn<NonComplianceDetail, String> statusColumn;
    @FXML private TableColumn<NonComplianceDetail, String> reasonColumn;
    @FXML private TableColumn<NonComplianceDetail, Integer> daysLateColumn;
    
    public static class NonComplianceDetail {
        private final String tin;
        private final String taxpayerName;
        private final String taxType;
        private final String period;
        private final LocalDate dueDate;
        private final LocalDate filedDate;
        private final double amountOwed;
        private final double penalty;
        private final String status;
        private final String reason;
        
        public NonComplianceDetail(String tin, String taxpayerName, String taxType, String period,
                                  LocalDate dueDate, LocalDate filedDate, double amountOwed,
                                  double penalty, String status, String reason) {
            this.tin = tin;
            this.taxpayerName = taxpayerName;
            this.taxType = taxType;
            this.period = period;
            this.dueDate = dueDate;
            this.filedDate = filedDate;
            this.amountOwed = amountOwed;
            this.penalty = penalty;
            this.status = status;
            this.reason = reason;
        }
        
        public String getTin() { return tin; }
        public String getTaxpayerName() { return taxpayerName; }
        public String getTaxType() { return taxType; }
        public String getPeriod() { return period; }
        public LocalDate getDueDate() { return dueDate; }
        public LocalDate getFiledDate() { return filedDate; }
        public double getAmountOwed() { return amountOwed; }
        public double getPenalty() { return penalty; }
        public String getStatus() { return status; }
        public String getReason() { return reason; }
        public int getDaysLate() { 
            return filedDate != null && dueDate != null ? 
                (int) java.time.temporal.ChronoUnit.DAYS.between(dueDate, filedDate) : 0;
        }
    }
    
    @FXML
    public void initialize() {
        LOGGER.info("Initializing ComplianceReportController");
        
        initializeDatabase();
        setupUIComponents();
        loadComboBoxData();
        setupEventListeners();
        
        // Set default values
        yearField.setText(String.valueOf(LocalDate.now().getYear()));
        
        // Disable action buttons initially
        ExportDetailsbtn.setDisable(true);
        GenerateAlertsbtn.setDisable(true);
    }
    
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            LOGGER.info("Database connected successfully for compliance report");
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void setupUIComponents() {
        // Setup table columns
        setupTableView();
        
        // Setup pie chart if present
        if (complianceChart != null) {
            complianceChart.setTitle("Compliance Distribution");
            complianceChart.setLabelsVisible(true);
            complianceChart.setLegendVisible(true);
        }
        
        // Setup progress bars
        complianceProgress.setProgress(0);
        filingProgress.setProgress(0);
        paymentProgress.setProgress(0);
        
        // Setup loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
        
        // Set year field validation
        yearField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                yearField.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (newValue.length() > 4) {
                yearField.setText(oldValue);
            }
        });
    }
    
    private void setupTableView() {
        if (nonComplianceTable != null) {
            // Initialize table columns
            if (tinColumn != null) tinColumn.setCellValueFactory(new PropertyValueFactory<>("tin"));
            if (taxpayerNameColumn != null) taxpayerNameColumn.setCellValueFactory(new PropertyValueFactory<>("taxpayerName"));
            if (taxTypeColumn != null) taxTypeColumn.setCellValueFactory(new PropertyValueFactory<>("taxType"));
            if (periodColumn != null) periodColumn.setCellValueFactory(new PropertyValueFactory<>("period"));
            if (dueDateColumn != null) dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
            if (filedDateColumn != null) filedDateColumn.setCellValueFactory(new PropertyValueFactory<>("filedDate"));
            if (amountOwedColumn != null) {
                amountOwedColumn.setCellValueFactory(new PropertyValueFactory<>("amountOwed"));
                amountOwedColumn.setCellFactory(column -> new TableCell<NonComplianceDetail, Double>() {
                    @Override
                    protected void updateItem(Double amount, boolean empty) {
                        super.updateItem(amount, empty);
                        if (empty || amount == null) {
                            setText(null);
                        } else {
                            setText(String.format("ETB %,.2f", amount));
                        }
                    }
                });
            }
            if (penaltyColumn != null) {
                penaltyColumn.setCellValueFactory(new PropertyValueFactory<>("penalty"));
                penaltyColumn.setCellFactory(column -> new TableCell<NonComplianceDetail, Double>() {
                    @Override
                    protected void updateItem(Double penalty, boolean empty) {
                        super.updateItem(penalty, empty);
                        if (empty || penalty == null) {
                            setText(null);
                        } else {
                            setText(String.format("ETB %,.2f", penalty));
                        }
                    }
                });
            }
            if (statusColumn != null) statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            if (reasonColumn != null) reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
            if (daysLateColumn != null) daysLateColumn.setCellValueFactory(new PropertyValueFactory<>("daysLate"));
        }
    }
    
    private void loadComboBoxData() {
        // Periods
        ObservableList<String> periods = FXCollections.observableArrayList(
            "Monthly", "Quarterly", "Yearly", "Custom"
        );
        periodCombo.setItems(periods);
        periodCombo.setValue("Monthly");
        
        // Tax types
        ObservableList<String> taxTypes = FXCollections.observableArrayList(
            "All Tax Types", "VAT", "Income Tax", "Withholding Tax", 
            "Excise Tax", "Customs Duty"
        );
        taxTypeCombo.setItems(taxTypes);
        taxTypeCombo.setValue("All Tax Types");
        
        // Regions
        Task<ObservableList<String>> loadRegionsTask = new Task<ObservableList<String>>() {
            @Override
            protected ObservableList<String> call() throws Exception {
                ObservableList<String> regions = FXCollections.observableArrayList("All Regions");
                String query = "SELECT DISTINCT region FROM tax_returns WHERE region IS NOT NULL ORDER BY region";
                
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
        
        // Business sizes
        ObservableList<String> businessSizes = FXCollections.observableArrayList(
            "All Sizes", "Micro", "Small", "Medium", "Large"
        );
        businessSizeCombo.setItems(businessSizes);
        businessSizeCombo.setValue("All Sizes");
        
        Thread regionThread = new Thread(loadRegionsTask);
        regionThread.setDaemon(true);
        regionThread.start();
    }
    
    private void setupEventListeners() {
        // Year validation
        yearField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Lost focus
                validateYear();
            }
        });
    }
    
    private boolean validateYear() {
        String yearText = yearField.getText();
        if (yearText.isEmpty()) {
            showAlert("Validation Error", "Year is required", Alert.AlertType.ERROR);
            yearField.requestFocus();
            return false;
        }
        
        try {
            int year = Integer.parseInt(yearText);
            if (year < 2000 || year > LocalDate.now().getYear() + 1) {
                showAlert("Validation Error", 
                         "Year must be between 2000 and " + (LocalDate.now().getYear() + 1), 
                         Alert.AlertType.ERROR);
                yearField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Invalid year format", Alert.AlertType.ERROR);
            yearField.requestFocus();
            return false;
        }
        
        return true;
    }
    
    // ==================== EVENT HANDLERS ====================
    
    @FXML
    void handlerclickGeneratereportbtn(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }
        
        generateComplianceReport();
    }
    
    private boolean validateInputs() {
        if (!validateYear()) {
            return false;
        }
        
        return true;
    }
    
    private void generateComplianceReport() {
        Task<ComplianceData> reportTask = new Task<ComplianceData>() {
            @Override
            protected ComplianceData call() throws Exception {
                int year = Integer.parseInt(yearField.getText());
                String period = periodCombo.getValue();
                String taxType = taxTypeCombo.getValue();
                String region = regionCombo.getValue();
                String businessSize = businessSizeCombo.getValue();
                
                return generateComplianceData(year, period, taxType, region, businessSize);
            }
            
            @Override
            protected void succeeded() {
                ComplianceData data = getValue();
                updateComplianceMetrics(data);
                updateComplianceChart(data);
                updateNonComplianceTable(data);
                
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                Generatereportbtn.setText("Generate Report");
                
                // Enable action buttons
                ExportDetailsbtn.setDisable(false);
                GenerateAlertsbtn.setDisable(false);
                
                LOGGER.info("Compliance report generated successfully");
            }
            
            @Override
            protected void failed() {
                showAlert("Report Generation Failed", 
                         "Error: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                Generatereportbtn.setText("Generate Report");
            }
        };
        
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        Generatereportbtn.setText("Generating...");
        
        Thread reportThread = new Thread(reportTask);
        reportThread.setDaemon(true);
        reportThread.start();
    }
    
    private ComplianceData generateComplianceData(int year, String period, String taxType, 
                                                 String region, String businessSize) {
        ComplianceData data = new ComplianceData();
        
        try {
            // Get compliance metrics
            String metricsQuery = buildMetricsQuery(year, taxType, region, businessSize);
            
            try (PreparedStatement pstmt = connection.prepareStatement(metricsQuery)) {
                pstmt.setInt(1, year);
                
                int paramIndex = 2;
                if (!"All Tax Types".equals(taxType)) {
                    pstmt.setString(paramIndex++, taxType);
                }
                if (!"All Regions".equals(region)) {
                    pstmt.setString(paramIndex++, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    data.totalReturns = rs.getInt("total_returns");
                    data.onTimeFilings = rs.getInt("on_time_filings");
                    data.lateFilings = rs.getInt("late_filings");
                    data.completedPayments = rs.getInt("completed_payments");
                    data.overduePayments = rs.getInt("overdue_payments");
                    
                    // Calculate rates
                    data.filingComplianceRate = data.totalReturns > 0 ? 
                        (data.onTimeFilings * 100.0 / data.totalReturns) : 0;
                    data.paymentComplianceRate = data.totalReturns > 0 ? 
                        (data.completedPayments * 100.0 / data.totalReturns) : 0;
                    data.overallComplianceRate = (data.filingComplianceRate + data.paymentComplianceRate) / 2;
                }
            }
            
            // Get non-compliance details
            String detailsQuery = "SELECT t.tin, t.taxpayer_name, tr.tax_type, tr.tax_period, " +
                                 "tr.due_date, tr.filing_date, tr.tax_amount, tr.penalty_amount, " +
                                 "tr.status, tr.non_compliance_reason " +
                                 "FROM tax_returns tr " +
                                 "JOIN taxpayers t ON tr.taxpayer_id = t.id " +
                                 "WHERE YEAR(tr.due_date) = ? " +
                                 (!"All Tax Types".equals(taxType) ? "AND tr.tax_type = ? " : "") +
                                 (!"All Regions".equals(region) ? "AND tr.region = ? " : "") +
                                 "AND (tr.filing_date IS NULL OR tr.filing_date > tr.due_date " +
                                 "OR tr.payment_status != 'PAID') " +
                                 "ORDER BY tr.due_date DESC " +
                                 "LIMIT 100";
            
            try (PreparedStatement pstmt = connection.prepareStatement(detailsQuery)) {
                pstmt.setInt(1, year);
                
                int paramIndex = 2;
                if (!"All Tax Types".equals(taxType)) {
                    pstmt.setString(paramIndex++, taxType);
                }
                if (!"All Regions".equals(region)) {
                    pstmt.setString(paramIndex++, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    NonComplianceDetail detail = new NonComplianceDetail(
                        rs.getString("tin"),
                        rs.getString("taxpayer_name"),
                        rs.getString("tax_type"),
                        rs.getString("tax_period"),
                        rs.getDate("due_date") != null ? rs.getDate("due_date").toLocalDate() : null,
                        rs.getDate("filing_date") != null ? rs.getDate("filing_date").toLocalDate() : null,
                        rs.getDouble("tax_amount"),
                        rs.getDouble("penalty_amount"),
                        rs.getString("status"),
                        rs.getString("non_compliance_reason")
                    );
                    data.nonComplianceDetails.add(detail);
                }
            }
            
            // Get compliance distribution for chart
            if (complianceChart != null) {
                String distributionQuery = "SELECT " +
                                          "SUM(CASE WHEN compliance_score >= 90 THEN 1 ELSE 0 END) as excellent, " +
                                          "SUM(CASE WHEN compliance_score >= 70 AND compliance_score < 90 THEN 1 ELSE 0 END) as good, " +
                                          "SUM(CASE WHEN compliance_score >= 50 AND compliance_score < 70 THEN 1 ELSE 0 END) as fair, " +
                                          "SUM(CASE WHEN compliance_score < 50 THEN 1 ELSE 0 END) as poor " +
                                          "FROM taxpayers " +
                                          "WHERE status = 'ACTIVE'";
                
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(distributionQuery)) {
                    if (rs.next()) {
                        data.excellentCompliance = rs.getInt("excellent");
                        data.goodCompliance = rs.getInt("good");
                        data.fairCompliance = rs.getInt("fair");
                        data.poorCompliance = rs.getInt("poor");
                    }
                }
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Error generating compliance data: " + e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
        
        return data;
    }
    
    private String buildMetricsQuery(int year, String taxType, String region, String businessSize) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append("COUNT(*) as total_returns, ");
        query.append("SUM(CASE WHEN filing_date <= due_date THEN 1 ELSE 0 END) as on_time_filings, ");
        query.append("SUM(CASE WHEN filing_date > due_date THEN 1 ELSE 0 END) as late_filings, ");
        query.append("SUM(CASE WHEN payment_status = 'PAID' THEN 1 ELSE 0 END) as completed_payments, ");
        query.append("SUM(CASE WHEN payment_status = 'OVERDUE' THEN 1 ELSE 0 END) as overdue_payments ");
        query.append("FROM tax_returns ");
        query.append("WHERE YEAR(due_date) = ? ");
        
        if (!"All Tax Types".equals(taxType)) {
            query.append("AND tax_type = ? ");
        }
        if (!"All Regions".equals(region)) {
            query.append("AND region = ? ");
        }
        
        return query.toString();
    }
    
    private void updateComplianceMetrics(ComplianceData data) {
        complianceRateLabel.setText(String.format("%.1f%%", data.overallComplianceRate));
        filingRateLabel.setText(String.format("%.1f%%", data.filingComplianceRate));
        paymentComplianceLabel.setText(String.format("%.1f%%", data.paymentComplianceRate));
        
        // Update summary labels if present
        if (totalReturnsLabel != null) {
            totalReturnsLabel.setText(String.valueOf(data.totalReturns));
        }
        if (lateReturnsLabel != null) {
            lateReturnsLabel.setText(String.valueOf(data.lateFilings));
        }
        if (overduePaymentsLabel != null) {
            overduePaymentsLabel.setText(String.valueOf(data.overduePayments));
        }
        
        // Update progress bars
        complianceProgress.setProgress(data.overallComplianceRate / 100);
        filingProgress.setProgress(data.filingComplianceRate / 100);
        paymentProgress.setProgress(data.paymentComplianceRate / 100);
    }
    
    private void updateComplianceChart(ComplianceData data) {
        if (complianceChart != null) {
            complianceChart.getData().clear();
            
            if (data.excellentCompliance > 0) {
                complianceChart.getData().add(new PieChart.Data(
                    "Excellent (" + data.excellentCompliance + ")", data.excellentCompliance));
            }
            if (data.goodCompliance > 0) {
                complianceChart.getData().add(new PieChart.Data(
                    "Good (" + data.goodCompliance + ")", data.goodCompliance));
            }
            if (data.fairCompliance > 0) {
                complianceChart.getData().add(new PieChart.Data(
                    "Fair (" + data.fairCompliance + ")", data.fairCompliance));
            }
            if (data.poorCompliance > 0) {
                complianceChart.getData().add(new PieChart.Data(
                    "Poor (" + data.poorCompliance + ")", data.poorCompliance));
            }
            
            complianceChart.setVisible(true);
        }
    }
    
    private void updateNonComplianceTable(ComplianceData data) {
        nonComplianceList = FXCollections.observableArrayList(data.nonComplianceDetails);
        if (nonComplianceTable != null) {
            nonComplianceTable.setItems(nonComplianceList);
        }
    }
    
    @FXML
    void handlerclickExportDetailsbtn(ActionEvent event) {
        if (nonComplianceList == null || nonComplianceList.isEmpty()) {
            showAlert("Export Error", "No non-compliance data to export", Alert.AlertType.WARNING);
            return;
        }
        
        Task<Boolean> exportTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                String fileName = "Non_Compliance_Details_" + LocalDate.now() + ".xlsx";
                LOGGER.info("Exporting non-compliance details: " + fileName);
                
                // Simulate export
                Thread.sleep(1500);
                return true;
            }
            
            @Override
            protected void succeeded() {
                showAlert("Export Successful", 
                         "Non-compliance details exported successfully!", 
                         Alert.AlertType.INFORMATION);
            }
            
            @Override
            protected void failed() {
                showAlert("Export Failed", 
                         "Failed to export details: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
            }
        };
        
        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
    }
    
    @FXML
    void handlerclickGenerateAlertsbtn(ActionEvent event) {
        if (nonComplianceList == null || nonComplianceList.isEmpty()) {
            showAlert("Alert Generation", "No non-compliance cases to generate alerts for", Alert.AlertType.WARNING);
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Generate Compliance Alerts");
        alert.setHeaderText("Alert Generation");
        alert.setContentText("Generate alerts for " + nonComplianceList.size() + " non-compliant taxpayers?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                generateComplianceAlerts();
            }
        });
    }
    
    private void generateComplianceAlerts() {
        Task<Integer> alertTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                int alertsGenerated = 0;
                
                for (NonComplianceDetail detail : nonComplianceList) {
                    // Generate alert for each non-compliant taxpayer
                    String alertMessage = String.format(
                        "Taxpayer: %s (TIN: %s)\nTax Type: %s, Period: %s\n" +
                        "Amount Owed: ETB %,.2f, Penalty: ETB %,.2f\n" +
                        "Status: %s, Reason: %s",
                        detail.getTaxpayerName(), detail.getTin(),
                        detail.getTaxType(), detail.getPeriod(),
                        detail.getAmountOwed(), detail.getPenalty(),
                        detail.getStatus(), detail.getReason()
                    );
                    
                    // Save alert to database
                    saveComplianceAlert(detail.getTin(), alertMessage);
                    alertsGenerated++;
                }
                
                return alertsGenerated;
            }
            
            @Override
            protected void succeeded() {
                int alertsGenerated = getValue();
                showAlert("Alerts Generated", 
                         "Successfully generated " + alertsGenerated + " compliance alerts!", 
                         Alert.AlertType.INFORMATION);
            }
            
            @Override
            protected void failed() {
                showAlert("Alert Generation Failed", 
                         "Failed to generate alerts: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
            }
        };
        
        Thread alertThread = new Thread(alertTask);
        alertThread.setDaemon(true);
        alertThread.start();
    }
    
    private void saveComplianceAlert(String tin, String message) {
        String sql = "INSERT INTO compliance_alerts (taxpayer_tin, alert_type, message, priority, status, created_at) " +
                    "VALUES (?, 'NON_COMPLIANCE', ?, 'HIGH', 'PENDING', NOW())";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tin);
            pstmt.setString(2, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOGGER.warning("Failed to save compliance alert: " + e.getMessage());
        }
    }
    
    @FXML
    void handlerclickbackButtonReportpage(ActionEvent event) {
        try {
            Stage stage = (Stage) backButtonReportpage.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            LOGGER.warning("Error closing report window: " + e.getMessage());
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
    
    // Data class for compliance report
    private static class ComplianceData {
        int totalReturns = 0;
        int onTimeFilings = 0;
        int lateFilings = 0;
        int completedPayments = 0;
        int overduePayments = 0;
        double overallComplianceRate = 0;
        double filingComplianceRate = 0;
        double paymentComplianceRate = 0;
        int excellentCompliance = 0;
        int goodCompliance = 0;
        int fairCompliance = 0;
        int poorCompliance = 0;
        ObservableList<NonComplianceDetail> nonComplianceDetails = FXCollections.observableArrayList();
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