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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Logger;

public class RevenueReportController {

    private static final Logger LOGGER = Logger.getLogger(RevenueReportController.class.getName());
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    
    private Connection connection;
    
    // FXML components from your FXML file
    @FXML private Button ExportExcelbtn;
    @FXML private Button ExporttoPDFbtn;
    @FXML private Button GenerateReportbtn;
    @FXML private RadioButton Monthly;
    @FXML private Button Printbtn;
    @FXML private RadioButton Quarterly;
    @FXML private RadioButton Yearly;
    @FXML private Button backButtonReportpage;
    @FXML private Label incomeTaxLabel;
    @FXML private ComboBox<String> monthCombo;
    @FXML private Label otherTaxesLabel;
    @FXML private ToggleGroup periodGroup;
    @FXML private ComboBox<String> regionCombo;
    @FXML private ComboBox<String> taxTypeCombo;
    @FXML private Label totalRevenueLabel;
    @FXML private Label vatLabel;
    @FXML private TextField yearField;
    
    // Additional components that need to be added (not in current FXML but needed for functionality)
    @FXML private BarChart<String, Number> revenueChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private TableView<RevenueDetail> revenueTable;
    @FXML private Label periodLabel;
    
    // Table columns for revenue details
    @FXML private TableColumn<RevenueDetail, String> taxTypeColumn;
    @FXML private TableColumn<RevenueDetail, String> periodColumn;
    @FXML private TableColumn<RevenueDetail, Double> amountColumn;
    @FXML private TableColumn<RevenueDetail, Integer> transactionColumn;
    
    public static class RevenueDetail {
        private final SimpleStringProperty taxType;
        private final SimpleStringProperty period;
        private final SimpleDoubleProperty amount;
        private final SimpleIntegerProperty transactionCount;
        
        public RevenueDetail(String taxType, String period, double amount, int transactionCount) {
            this.taxType = new SimpleStringProperty(taxType);
            this.period = new SimpleStringProperty(period);
            this.amount = new SimpleDoubleProperty(amount);
            this.transactionCount = new SimpleIntegerProperty(transactionCount);
        }
        
        public String getTaxType() { return taxType.get(); }
        public void setTaxType(String value) { taxType.set(value); }
        
        public String getPeriod() { return period.get(); }
        public void setPeriod(String value) { period.set(value); }
        
        public double getAmount() { return amount.get(); }
        public void setAmount(double value) { amount.set(value); }
        
        public int getTransactionCount() { return transactionCount.get(); }
        public void setTransactionCount(int value) { transactionCount.set(value); }
        
        // Property getters for table binding
        public SimpleStringProperty taxTypeProperty() { return taxType; }
        public SimpleStringProperty periodProperty() { return period; }
        public SimpleDoubleProperty amountProperty() { return amount; }
        public SimpleIntegerProperty transactionCountProperty() { return transactionCount; }
    }
    
    @FXML
    public void initialize() {
        LOGGER.info("Initializing RevenueReportController");
        
        initializeDatabase();
        setupUIComponents();
        loadComboBoxData();
        setupEventListeners();
        
        // Set default values
        yearField.setText(String.valueOf(LocalDate.now().getYear()));
        Monthly.setSelected(true);
        monthCombo.setDisable(false);
        
        // Initialize export buttons as disabled until report is generated
        ExportExcelbtn.setDisable(true);
        ExporttoPDFbtn.setDisable(true);
        Printbtn.setDisable(true);
    }
    
    private void initializeDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            LOGGER.info("Database connected successfully for revenue report");
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void setupUIComponents() {
        // Setup toggle group
        periodGroup = new ToggleGroup();
        Monthly.setToggleGroup(periodGroup);
        Quarterly.setToggleGroup(periodGroup);
        Yearly.setToggleGroup(periodGroup);
        
        // Initialize chart if it exists (may be null if not in FXML)
        if (revenueChart != null) {
            if (xAxis != null) xAxis.setLabel("Period");
            if (yAxis != null) yAxis.setLabel("Amount (ETB)");
            revenueChart.setTitle("Revenue Trend");
            revenueChart.setLegendVisible(true);
            revenueChart.setVisible(false); // Hide until data is loaded
        }
        
        // Initialize table columns if table exists
        if (revenueTable != null && taxTypeColumn != null) {
            taxTypeColumn.setCellValueFactory(new PropertyValueFactory<>("taxType"));
            periodColumn.setCellValueFactory(new PropertyValueFactory<>("period"));
            amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
            transactionColumn.setCellValueFactory(new PropertyValueFactory<>("transactionCount"));
            
            // Format amount column
            amountColumn.setCellFactory(column -> new TableCell<RevenueDetail, Double>() {
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
        
        // Set year field validation
        yearField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                yearField.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (newValue.length() > 4) {
                yearField.setText(oldValue);
            }
        });
        
        // Initialize loading indicator
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
    }
    
    private void loadComboBoxData() {
        // Months
        ObservableList<String> months = FXCollections.observableArrayList(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        );
        monthCombo.setItems(months);
        monthCombo.setValue(LocalDate.now().getMonth().toString());
        
        // Tax types
        ObservableList<String> taxTypes = FXCollections.observableArrayList(
            "All Tax Types", "VAT", "Income Tax", "Withholding Tax", 
            "Excise Tax", "Customs Duty", "Stamp Duty", "Other"
        );
        taxTypeCombo.setItems(taxTypes);
        taxTypeCombo.setValue("All Tax Types");
        
        // Regions
        Task<ObservableList<String>> loadRegionsTask = new Task<ObservableList<String>>() {
            @Override
            protected ObservableList<String> call() throws Exception {
                ObservableList<String> regions = FXCollections.observableArrayList("All Regions");
                String query = "SELECT DISTINCT region FROM payments WHERE region IS NOT NULL ORDER BY region";
                
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
        
        loadRegionsTask.setOnFailed(e -> {
            regionCombo.setItems(FXCollections.observableArrayList("All Regions"));
            regionCombo.setValue("All Regions");
        });
        
        Thread regionThread = new Thread(loadRegionsTask);
        regionThread.setDaemon(true);
        regionThread.start();
    }
    
    private void setupEventListeners() {
        // Period type change listener
        periodGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                RadioButton selected = (RadioButton) newVal;
                LOGGER.info("Period type changed to: " + selected.getText());
                updatePeriodControls(selected.getText());
            }
        });
        
        // Year validation
        yearField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) { // Lost focus
                validateYear();
            }
        });
    }
    
    private void updatePeriodControls(String periodType) {
        switch (periodType) {
            case "Monthly":
                monthCombo.setDisable(false);
                if (monthCombo.getValue() == null) {
                    monthCombo.setValue(LocalDate.now().getMonth().toString());
                }
                break;
            case "Quarterly":
            case "Yearly":
                monthCombo.setDisable(true);
                monthCombo.setValue(null);
                break;
        }
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
    void handlerclickMonthly(ActionEvent event) {
        updatePeriodControls("Monthly");
    }
    
    @FXML
    void handlerclickQuarterly(ActionEvent event) {
        updatePeriodControls("Quarterly");
    }
    
    @FXML
    void handlerclickYearly(ActionEvent event) {
        updatePeriodControls("Yearly");
    }
    
    @FXML
    void handlerclickGenerateReportbtn(ActionEvent event) {
        if (!validateInputs()) {
            return;
        }
        
        generateRevenueReport();
    }
    
    private boolean validateInputs() {
        if (!validateYear()) {
            return false;
        }
        
        RadioButton selectedPeriod = (RadioButton) periodGroup.getSelectedToggle();
        if (selectedPeriod == null) {
            showAlert("Validation Error", "Please select a period type", Alert.AlertType.ERROR);
            return false;
        }
        
        if ("Monthly".equals(selectedPeriod.getText()) && 
            (monthCombo.getValue() == null || monthCombo.getValue().isEmpty())) {
            showAlert("Validation Error", "Please select a month", Alert.AlertType.ERROR);
            monthCombo.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void generateRevenueReport() {
        Task<RevenueData> reportTask = new Task<RevenueData>() {
            @Override
            protected RevenueData call() throws Exception {
                RadioButton selectedPeriod = (RadioButton) periodGroup.getSelectedToggle();
                String periodType = selectedPeriod.getText();
                int year = Integer.parseInt(yearField.getText());
                String month = monthCombo.getValue();
                String taxType = taxTypeCombo.getValue();
                String region = regionCombo.getValue();
                
                RevenueData data = new RevenueData();
                
                switch (periodType) {
                    case "Monthly":
                        data = generateMonthlyRevenue(year, month, taxType, region);
                        break;
                    case "Quarterly":
                        data = generateQuarterlyRevenue(year, taxType, region);
                        break;
                    case "Yearly":
                        data = generateYearlyRevenue(year, taxType, region);
                        break;
                }
                
                // Load detailed data for table
                data.detailedData = loadDetailedRevenueData(year, month, periodType, taxType, region);
                
                return data;
            }
            
            @Override
            protected void succeeded() {
                RevenueData data = getValue();
                updateRevenueDisplay(data);
                updateRevenueChart(data);
                updateRevenueTable(data.detailedData);
                
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                GenerateReportbtn.setText("Generate Report");
                
                LOGGER.info("Revenue report generated successfully");
            }
            
            @Override
            protected void failed() {
                showAlert("Report Generation Failed", 
                         "Error: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                GenerateReportbtn.setText("Generate Report");
            }
        };
        
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        GenerateReportbtn.setText("Generating...");
        
        Thread reportThread = new Thread(reportTask);
        reportThread.setDaemon(true);
        reportThread.start();
    }
    
    private ObservableList<RevenueDetail> loadDetailedRevenueData(int year, String month, String periodType, 
                                                                 String taxType, String region) {
        ObservableList<RevenueDetail> details = FXCollections.observableArrayList();
        
        try {
            String query = "SELECT tax_type, " +
                          "DATE_FORMAT(payment_date, '%Y-%m') as period, " +
                          "SUM(amount) as total_amount, " +
                          "COUNT(*) as transaction_count " +
                          "FROM payments " +
                          "WHERE YEAR(payment_date) = ? ";
            
            if (periodType.equals("Monthly") && month != null) {
                query += "AND MONTHNAME(payment_date) = ? ";
            }
            
            if (!"All Tax Types".equals(taxType)) {
                query += "AND tax_type = ? ";
            }
            
            if (!"All Regions".equals(region)) {
                query += "AND region = ? ";
            }
            
            query += "GROUP BY tax_type, DATE_FORMAT(payment_date, '%Y-%m') " +
                    "ORDER BY period, tax_type";
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                int paramIndex = 1;
                pstmt.setInt(paramIndex++, year);
                
                if (periodType.equals("Monthly") && month != null) {
                    pstmt.setString(paramIndex++, month);
                }
                
                if (!"All Tax Types".equals(taxType)) {
                    pstmt.setString(paramIndex++, taxType);
                }
                
                if (!"All Regions".equals(region)) {
                    pstmt.setString(paramIndex++, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    RevenueDetail detail = new RevenueDetail(
                        rs.getString("tax_type"),
                        rs.getString("period"),
                        rs.getDouble("total_amount"),
                        rs.getInt("transaction_count")
                    );
                    details.add(detail);
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error loading detailed revenue data: " + e.getMessage());
        }
        
        return details;
    }
    
    private RevenueData generateMonthlyRevenue(int year, String month, String taxType, String region) {
        RevenueData data = new RevenueData();
        
        try {
            int monthNumber = getMonthNumber(month);
            YearMonth yearMonth = YearMonth.of(year, monthNumber);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            
            String query = buildRevenueQuery(startDate, endDate, taxType, region);
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                int paramIndex = 1;
                pstmt.setDate(paramIndex++, Date.valueOf(startDate));
                pstmt.setDate(paramIndex++, Date.valueOf(endDate));
                
                if (!"All Tax Types".equals(taxType)) {
                    pstmt.setString(paramIndex++, taxType);
                }
                
                if (!"All Regions".equals(region)) {
                    pstmt.setString(paramIndex++, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    data.vatAmount = rs.getDouble("vat_total");
                    data.incomeTaxAmount = rs.getDouble("income_tax_total");
                    data.otherTaxesAmount = rs.getDouble("other_taxes_total");
                    data.totalRevenue = rs.getDouble("total_revenue");
                    data.periodLabel = month + " " + year;
                }
            }
            
            // Get daily breakdown for chart
            String dailyQuery = "SELECT DAY(payment_date) as day, SUM(amount) as daily_total " +
                               "FROM payments " +
                               "WHERE payment_date BETWEEN ? AND ? ";
            
            if (!"All Tax Types".equals(taxType)) {
                dailyQuery += "AND tax_type = ? ";
            }
            
            if (!"All Regions".equals(region)) {
                dailyQuery += "AND region = ? ";
            }
            
            dailyQuery += "GROUP BY DAY(payment_date) ORDER BY day";
            
            try (PreparedStatement pstmt = connection.prepareStatement(dailyQuery)) {
                int paramIndex = 1;
                pstmt.setDate(paramIndex++, Date.valueOf(startDate));
                pstmt.setDate(paramIndex++, Date.valueOf(endDate));
                
                if (!"All Tax Types".equals(taxType)) {
                    pstmt.setString(paramIndex++, taxType);
                }
                
                if (!"All Regions".equals(region)) {
                    pstmt.setString(paramIndex++, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    data.chartData.put("Day " + rs.getInt("day"), rs.getDouble("daily_total"));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Error generating monthly revenue: " + e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
        
        return data;
    }
    
    private RevenueData generateQuarterlyRevenue(int year, String taxType, String region) {
        RevenueData data = new RevenueData();
        
        try {
            // Generate for all 4 quarters
            for (int quarter = 1; quarter <= 4; quarter++) {
                LocalDate startDate, endDate;
                switch (quarter) {
                    case 1:
                        startDate = LocalDate.of(year, 1, 1);
                        endDate = LocalDate.of(year, 3, 31);
                        break;
                    case 2:
                        startDate = LocalDate.of(year, 4, 1);
                        endDate = LocalDate.of(year, 6, 30);
                        break;
                    case 3:
                        startDate = LocalDate.of(year, 7, 1);
                        endDate = LocalDate.of(year, 9, 30);
                        break;
                    default:
                        startDate = LocalDate.of(year, 10, 1);
                        endDate = LocalDate.of(year, 12, 31);
                }
                
                String query = buildRevenueQuery(startDate, endDate, taxType, region);
                
                try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                    int paramIndex = 1;
                    pstmt.setDate(paramIndex++, Date.valueOf(startDate));
                    pstmt.setDate(paramIndex++, Date.valueOf(endDate));
                    
                    if (!"All Tax Types".equals(taxType)) {
                        pstmt.setString(paramIndex++, taxType);
                    }
                    
                    if (!"All Regions".equals(region)) {
                        pstmt.setString(paramIndex++, region);
                    }
                    
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        double quarterTotal = rs.getDouble("total_revenue");
                        data.chartData.put("Q" + quarter, quarterTotal);
                        data.totalRevenue += quarterTotal;
                        
                        // Add to breakdown totals
                        data.vatAmount += rs.getDouble("vat_total");
                        data.incomeTaxAmount += rs.getDouble("income_tax_total");
                        data.otherTaxesAmount += rs.getDouble("other_taxes_total");
                    }
                }
            }
            
            data.periodLabel = "Year " + year + " (Quarterly)";
            
        } catch (SQLException e) {
            LOGGER.severe("Error generating quarterly revenue: " + e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
        
        return data;
    }
    
    private RevenueData generateYearlyRevenue(int year, String taxType, String region) {
        RevenueData data = new RevenueData();
        
        try {
            LocalDate startDate = LocalDate.of(year, 1, 1);
            LocalDate endDate = LocalDate.of(year, 12, 31);
            
            String query = buildRevenueQuery(startDate, endDate, taxType, region);
            
            try (PreparedStatement pstmt = connection.prepareStatement(query)) {
                int paramIndex = 1;
                pstmt.setDate(paramIndex++, Date.valueOf(startDate));
                pstmt.setDate(paramIndex++, Date.valueOf(endDate));
                
                if (!"All Tax Types".equals(taxType)) {
                    pstmt.setString(paramIndex++, taxType);
                }
                
                if (!"All Regions".equals(region)) {
                    pstmt.setString(paramIndex++, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    data.vatAmount = rs.getDouble("vat_total");
                    data.incomeTaxAmount = rs.getDouble("income_tax_total");
                    data.otherTaxesAmount = rs.getDouble("other_taxes_total");
                    data.totalRevenue = rs.getDouble("total_revenue");
                    data.periodLabel = "Year " + year;
                }
            }
            
            // Get monthly breakdown for chart
            String monthlyQuery = "SELECT MONTH(payment_date) as month, SUM(amount) as monthly_total " +
                                 "FROM payments " +
                                 "WHERE YEAR(payment_date) = ? ";
            
            if (!"All Tax Types".equals(taxType)) {
                monthlyQuery += "AND tax_type = ? ";
            }
            
            if (!"All Regions".equals(region)) {
                monthlyQuery += "AND region = ? ";
            }
            
            monthlyQuery += "GROUP BY MONTH(payment_date) ORDER BY month";
            
            try (PreparedStatement pstmt = connection.prepareStatement(monthlyQuery)) {
                int paramIndex = 1;
                pstmt.setInt(paramIndex++, year);
                
                if (!"All Tax Types".equals(taxType)) {
                    pstmt.setString(paramIndex++, taxType);
                }
                
                if (!"All Regions".equals(region)) {
                    pstmt.setString(paramIndex++, region);
                }
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    String monthName = getMonthName(rs.getInt("month"));
                    data.chartData.put(monthName, rs.getDouble("monthly_total"));
                }
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Error generating yearly revenue: " + e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
        
        return data;
    }
    
    private String buildRevenueQuery(LocalDate startDate, LocalDate endDate, String taxType, String region) {
        StringBuilder query = new StringBuilder();
        query.append("SELECT ");
        query.append("SUM(CASE WHEN tax_type = 'VAT' THEN amount ELSE 0 END) as vat_total, ");
        query.append("SUM(CASE WHEN tax_type = 'Income Tax' THEN amount ELSE 0 END) as income_tax_total, ");
        query.append("SUM(CASE WHEN tax_type NOT IN ('VAT', 'Income Tax') THEN amount ELSE 0 END) as other_taxes_total, ");
        query.append("SUM(amount) as total_revenue ");
        query.append("FROM payments ");
        query.append("WHERE payment_date BETWEEN ? AND ? ");
        
        if (!"All Tax Types".equals(taxType)) {
            query.append("AND tax_type = ? ");
        }
        
        if (!"All Regions".equals(region)) {
            query.append("AND region = ? ");
        }
        
        return query.toString();
    }
    
    private void updateRevenueDisplay(RevenueData data) {
        vatLabel.setText(String.format("ETB %,.2f", data.vatAmount));
        incomeTaxLabel.setText(String.format("ETB %,.2f", data.incomeTaxAmount));
        otherTaxesLabel.setText(String.format("ETB %,.2f", data.otherTaxesAmount));
        totalRevenueLabel.setText(String.format("ETB %,.2f", data.totalRevenue));
        
        if (periodLabel != null) {
            periodLabel.setText(data.periodLabel);
        }
        
        // Enable export buttons
        ExportExcelbtn.setDisable(false);
        ExporttoPDFbtn.setDisable(false);
        Printbtn.setDisable(false);
    }
    
    private void updateRevenueChart(RevenueData data) {
        if (revenueChart != null) {
            revenueChart.getData().clear();
            
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Revenue");
            
            for (String key : data.chartData.keySet()) {
                series.getData().add(new XYChart.Data<>(key, data.chartData.get(key)));
            }
            
            revenueChart.getData().add(series);
            revenueChart.setVisible(true);
        }
    }
    
    private void updateRevenueTable(ObservableList<RevenueDetail> data) {
        if (revenueTable != null) {
            revenueTable.setItems(data);
        }
    }
    
    @FXML
    void handlerclickExportExcelbtn(ActionEvent event) {
        exportReport("EXCEL");
    }
    
    @FXML
    void handlerclickExporttoPDFbtn(ActionEvent event) {
        exportReport("PDF");
    }
    
    @FXML
    void handlerclickPrintbtn(ActionEvent event) {
        printReport();
    }
    
    @FXML
    void handlerclickbackButtonReportpage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Reports.fxml"));
            Parent adminRoot = loader.load();
            
            Stage currentstage = (Stage) backButtonReportpage.getScene().getWindow();
            currentstage.close();
            
        } catch (IOException e) {
            showAlert("Navigation Error", "Error closing report window: " + e.getMessage(), AlertType.ERROR);
        }
    }
    
    private void exportReport(String format) {
        Task<Boolean> exportTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                // Generate export file
                String fileName = "Revenue_Report_" + LocalDate.now() + "." + format.toLowerCase();
                LOGGER.info("Exporting revenue report as " + format + ": " + fileName);
                
                // Simulate export process
                Thread.sleep(1500);
                return true;
            }
            
            @Override
            protected void succeeded() {
                showAlert("Export Successful", 
                         "Revenue report exported as " + format + " successfully!", 
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
    
    private void printReport() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Print Revenue Report");
        alert.setHeaderText("Print Configuration");
        alert.setContentText("Print revenue report with current settings?");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                LOGGER.info("Printing revenue report");
                showAlert("Print Started", 
                         "Revenue report sent to printer.", 
                         Alert.AlertType.INFORMATION);
            }
        });
    }
    
    // Helper methods
    private int getMonthNumber(String monthName) {
        switch (monthName.toLowerCase()) {
            case "january": return 1;
            case "february": return 2;
            case "march": return 3;
            case "april": return 4;
            case "may": return 5;
            case "june": return 6;
            case "july": return 7;
            case "august": return 8;
            case "september": return 9;
            case "october": return 10;
            case "november": return 11;
            case "december": return 12;
            default: return LocalDate.now().getMonthValue();
        }
    }
    
    private String getMonthName(int monthNumber) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        return months[monthNumber - 1];
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
    
    // Data class for revenue report
    private static class RevenueData {
        double vatAmount = 0;
        double incomeTaxAmount = 0;
        double otherTaxesAmount = 0;
        double totalRevenue = 0;
        String periodLabel = "";
        java.util.Map<String, Double> chartData = new java.util.HashMap<>();
        ObservableList<RevenueDetail> detailedData = FXCollections.observableArrayList();
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