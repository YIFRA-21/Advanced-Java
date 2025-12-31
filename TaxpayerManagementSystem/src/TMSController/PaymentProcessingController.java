package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

public class PaymentProcessingController {

    // Database connection
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    private Connection connection;
    
    // Payment status and method enums
    public enum PaymentStatus {
        PENDING("Pending", "#FF9800"),
        SUCCESSFUL("Successful", "#4CAF50"),
        FAILED("Failed", "#F44336"),
        DRAFT("Draft", "#9E9E9E"),
        CANCELLED("Cancelled", "#795548");
        
        private final String displayName;
        private final String color;
        
        PaymentStatus(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }
    
    public enum PaymentMethod {
        BANK_TRANSFER("Bank Transfer"),
        CASH("Cash"),
        CHECK("Check"),
        CREDIT_CARD("Credit Card"),
        MOBILE_BANKING("Mobile Banking"),
        ONLINE("Online Payment");
        
        private final String displayName;
        
        PaymentMethod(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Data model for payments table
    public static class PaymentRecord {
        private String paymentId;
        private String tin;
        private String taxpayerName;
        private LocalDate date;
        private double amount;
        private String method;
        private String status;
        
        public PaymentRecord(String paymentId, String tin, String taxpayerName, 
                            LocalDate date, double amount, String method, String status) {
            this.paymentId = paymentId;
            this.tin = tin;
            this.taxpayerName = taxpayerName;
            this.date = date;
            this.amount = amount;
            this.method = method;
            this.status = status;
        }
        
        // Getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        
        public String getTin() { return tin; }
        public void setTin(String tin) { this.tin = tin; }
        
        public String getTaxpayerName() { return taxpayerName; }
        public void setTaxpayerName(String taxpayerName) { this.taxpayerName = taxpayerName; }
        
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        
        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getFormattedAmount() {
            DecimalFormat formatter = new DecimalFormat("ETB #,##0.00");
            return formatter.format(amount);
        }
        
        public String getFormattedDate() {
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }

    @FXML
    private Tab Allpaymentsbtn;

    @FXML
    private Tab Quickpayment;

    @FXML
    private TableColumn<PaymentRecord, String> columonActions;

    @FXML
    private TableColumn<PaymentRecord, Double> columonAmount;

    @FXML
    private TableColumn<PaymentRecord, String> columonDate;

    @FXML
    private TableColumn<PaymentRecord, String> columonMethod;

    @FXML
    private TableColumn<PaymentRecord, String> columonPaymentID;

    @FXML
    private TableColumn<PaymentRecord, String> columonStatus;

    @FXML
    private TableColumn<PaymentRecord, String> columonTxapayer;

    @FXML
    private TableColumn<PaymentRecord, String> columontin;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Label failedPaymentsLabel;

    @FXML
    private Button filterbtn;

    @FXML
    private Button newPaymentBtn;

    @FXML
    private Button newTaxpayerBtn1;

    @FXML
    private ComboBox<String> paymentMethodFilter;

    @FXML
    private ComboBox<String> paymentStatusFilter;

    @FXML
    private TabPane paymentTabs;

    @FXML
    private TableView<PaymentRecord> paymentsTable;

    @FXML
    private Label pendingPaymentsLabel;

    @FXML
    private Button processQuickPaymentBtn;

    @FXML
    private TextField quickPaymentAmount;

    @FXML
    private ComboBox<String> quickPaymentMethod;

    @FXML
    private TextField quickPaymentTin;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private Label successfulPaymentsLabel;

    @FXML
    private Label totalPaymentsLabel;
    
    @FXML
    private VBox root;
    
    private ObservableList<String> paymentMethods = FXCollections.observableArrayList();
    private ObservableList<String> paymentStatuses = FXCollections.observableArrayList();

    // FIXED: Correct initialization method
    @FXML
    public void initialize() {
        System.out.println("PaymentProcessingController initialize() called");
        
        try {
            // Initialize database connection FIRST
            initializeDatabaseConnection();
            
            // Setup UI components
            setupUIComponents();
            
            // Setup table
            setupPaymentsTable();
            
            // Load payment statistics
            loadPaymentStatistics();
            
            // Load payments (using corrected query)
            loadAllPayments();
            
            // Setup event handlers
            setupEventHandlers();
            
            // Setup keyboard shortcuts
            setupKeyboardShortcuts();
            
            System.out.println("PaymentProcessingController initialized successfully");
            
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            showAlert("Database Error", "Cannot connect to database. Please check your connection.");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Initialization error: " + e.getMessage());
            showAlert("Initialization Error", "Error initializing payment processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeDatabaseConnection() throws SQLException {
        System.out.println("Attempting to connect to database...");
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("Database connection established for payment processing.");
        
        // Verify connection is working
        if (connection != null && !connection.isClosed()) {
            System.out.println("Database connection is valid and open");
        } else {
            throw new SQLException("Failed to establish database connection");
        }
    }
    
    private void setupUIComponents() {
        try {
            System.out.println("Setting up UI components...");
            
            // Setup date pickers
            if (startDatePicker != null) {
                startDatePicker.setValue(LocalDate.now().minusMonths(1));
            }
            if (endDatePicker != null) {
                endDatePicker.setValue(LocalDate.now());
            }
            
            // Initialize payment methods list
            paymentMethods.clear();
            for (PaymentMethod method : PaymentMethod.values()) {
                paymentMethods.add(method.getDisplayName());
            }
            
            // Initialize payment statuses list
            paymentStatuses.clear();
            for (PaymentStatus status : PaymentStatus.values()) {
                paymentStatuses.add(status.getDisplayName());
            }
            
            // Setup combo boxes
            if (paymentMethodFilter != null) {
                paymentMethodFilter.setItems(paymentMethods);
                paymentMethodFilter.setPromptText("All Methods");
            }
            
            if (paymentStatusFilter != null) {
                paymentStatusFilter.setItems(paymentStatuses);
                paymentStatusFilter.setPromptText("All Statuses");
            }
            
            if (quickPaymentMethod != null) {
                quickPaymentMethod.setItems(paymentMethods);
                quickPaymentMethod.setPromptText("Select method");
            }
            
            // Setup quick payment tab
            setupQuickPaymentTab();
            
            System.out.println("UI components setup completed");
            
        } catch (Exception e) {
            System.err.println("Error setting up UI components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupQuickPaymentTab() {
        try {
            // Set up quick payment amount field
            if (quickPaymentAmount != null) {
                quickPaymentAmount.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue.matches("\\d*(\\.\\d*)?")) {
                        quickPaymentAmount.setText(oldValue);
                    }
                });
            }
            
            // Set up TIN field validation
            if (quickPaymentTin != null) {
                quickPaymentTin.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.length() > 10) {
                        quickPaymentTin.setText(oldValue);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error setting up quick payment tab: " + e.getMessage());
        }
    }
    
    private void setupPaymentsTable() {
        try {
            // Initialize table columns
            if (columonPaymentID != null) {
                columonPaymentID.setCellValueFactory(new PropertyValueFactory<>("paymentId"));
            }
            if (columontin != null) {
                columontin.setCellValueFactory(new PropertyValueFactory<>("tin"));
            }
            if (columonTxapayer != null) {
                columonTxapayer.setCellValueFactory(new PropertyValueFactory<>("taxpayerName"));
            }
            if (columonDate != null) {
                columonDate.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
            }
            if (columonAmount != null) {
                columonAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
            }
            if (columonMethod != null) {
                columonMethod.setCellValueFactory(new PropertyValueFactory<>("method"));
            }
            if (columonStatus != null) {
                columonStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            }
            
            // Format amount column
            if (columonAmount != null) {
                columonAmount.setCellFactory(column -> new TableCell<PaymentRecord, Double>() {
                    @Override
                    protected void updateItem(Double amount, boolean empty) {
                        super.updateItem(amount, empty);
                        if (empty || amount == null) {
                            setText(null);
                        } else {
                            DecimalFormat formatter = new DecimalFormat("ETB #,##0.00");
                            setText(formatter.format(amount));
                        }
                    }
                });
            }
            
            // Format date column
            if (columonDate != null) {
                columonDate.setCellFactory(column -> new TableCell<PaymentRecord, String>() {
                    @Override
                    protected void updateItem(String date, boolean empty) {
                        super.updateItem(date, empty);
                        if (empty || date == null) {
                            setText(null);
                        } else {
                            setText(date);
                        }
                    }
                });
            }
            
            // Custom status column with color coding
            if (columonStatus != null) {
                columonStatus.setCellFactory(column -> new TableCell<PaymentRecord, String>() {
                    @Override
                    protected void updateItem(String status, boolean empty) {
                        super.updateItem(status, empty);
                        if (empty || status == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(status);
                            // Apply color based on status
                            String color = "#666666";
                            for (PaymentStatus ps : PaymentStatus.values()) {
                                if (ps.getDisplayName().equals(status)) {
                                    color = ps.getColor();
                                    break;
                                }
                            }
                            setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                        }
                    }
                });
            }
            
            // Setup actions column
            if (columonActions != null) {
                columonActions.setCellFactory(column -> new TableCell<PaymentRecord, String>() {
                    private final Button viewBtn = new Button("View");
                    private final HBox buttons = new HBox(5, viewBtn);
                    
                    {
                        viewBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3 8 3 8;");
                        
                        viewBtn.setOnAction(event -> {
                            PaymentRecord record = getTableView().getItems().get(getIndex());
                            if (record != null) {
                                viewPaymentDetails(record);
                            }
                        });
                    }
                    
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(buttons);
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error setting up payments table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadPaymentStatistics() {
        try {
            if (connection == null || connection.isClosed()) {
                System.err.println("Database connection is null or closed in loadPaymentStatistics");
                return;
            }
            
            System.out.println("Loading payment statistics...");
            
            // FIXED: Use consistent status names (UPPERCASE or as stored in DB)
            // Total payments count
            String totalQuery = "SELECT COUNT(*) as total_count, COALESCE(SUM(amount_paid), 0) as total_amount " +
                              "FROM payments WHERE status != 'DRAFT' AND status != 'CANCELLED'";
            try (PreparedStatement totalStmt = connection.prepareStatement(totalQuery);
                 ResultSet totalRs = totalStmt.executeQuery()) {
                
                if (totalRs.next() && totalPaymentsLabel != null) {
                    double totalAmount = totalRs.getDouble("total_amount");
                    totalPaymentsLabel.setText(String.format("ETB %,.0f", totalAmount));
                }
            }
            
            // Pending payments - FIXED: Use correct status name
            String pendingQuery = "SELECT COUNT(*) as pending_count FROM payments WHERE status = 'PENDING'";
            try (PreparedStatement pendingStmt = connection.prepareStatement(pendingQuery);
                 ResultSet pendingRs = pendingStmt.executeQuery()) {
                
                if (pendingRs.next() && pendingPaymentsLabel != null) {
                    pendingPaymentsLabel.setText(String.valueOf(pendingRs.getInt("pending_count")));
                }
            }
            
            // Successful payments - FIXED: Use correct status name
            String successQuery = "SELECT COUNT(*) as success_count FROM payments WHERE status = 'SUCCESSFUL'";
            try (PreparedStatement successStmt = connection.prepareStatement(successQuery);
                 ResultSet successRs = successStmt.executeQuery()) {
                
                if (successRs.next() && successfulPaymentsLabel != null) {
                    successfulPaymentsLabel.setText(String.valueOf(successRs.getInt("success_count")));
                }
            }
            
            // Failed payments - FIXED: Use correct status name
            String failedQuery = "SELECT COUNT(*) as failed_count FROM payments WHERE status = 'FAILED'";
            try (PreparedStatement failedStmt = connection.prepareStatement(failedQuery);
                 ResultSet failedRs = failedStmt.executeQuery()) {
                
                if (failedRs.next() && failedPaymentsLabel != null) {
                    failedPaymentsLabel.setText(String.valueOf(failedRs.getInt("failed_count")));
                }
            }
            
            System.out.println("Payment statistics loaded successfully");
            
        } catch (SQLException e) {
            System.err.println("Error loading payment statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadAllPayments() {
        try {
            if (connection == null || connection.isClosed()) {
                System.err.println("Database connection is null or closed in loadAllPayments");
                showAlert("Connection Error", "Database connection is not available. Please restart the application.");
                return;
            }
            
            System.out.println("Loading all payments...");
            
            // FIXED: Use CONCAT to combine first_name and last_name, or use business_name
            // Check what columns exist in taxpayers table
            String checkQuery = "SELECT column_name FROM information_schema.columns " +
                              "WHERE table_schema = 'tax_management_db' AND table_name = 'taxpayers'";
            
            boolean hasFirstName = false;
            boolean hasLastName = false;
            boolean hasBusinessName = false;
            
            try (Statement checkStmt = connection.createStatement();
                 ResultSet checkRs = checkStmt.executeQuery(checkQuery)) {
                
                while (checkRs.next()) {
                    String column = checkRs.getString("column_name");
                    if (column.equalsIgnoreCase("first_name")) hasFirstName = true;
                    if (column.equalsIgnoreCase("last_name")) hasLastName = true;
                    if (column.equalsIgnoreCase("business_name")) hasBusinessName = true;
                }
            }
            
            // Build query based on available columns
            StringBuilder taxpayerNameSelect = new StringBuilder();
            if (hasFirstName && hasLastName) {
                taxpayerNameSelect.append("CONCAT(t.first_name, ' ', t.last_name)");
            } else if (hasBusinessName) {
                taxpayerNameSelect.append("COALESCE(t.business_name, 'Unknown Taxpayer')");
            } else {
                taxpayerNameSelect.append("'Unknown Taxpayer'");
            }
            
            // FIXED: Correct SQL query - check what columns actually exist
            String query = "SELECT p.payment_id, " +
                         "COALESCE(t.tin, 'N/A') as tin, " +
                         taxpayerNameSelect.toString() + " as taxpayer_name, " +
                         "p.payment_date, p.amount_paid, p.payment_method, p.status " +
                         "FROM payments p " +
                         "LEFT JOIN taxpayers t ON p.taxpayer_id = t.taxpayer_id " +
                         "WHERE p.status != 'DRAFT' " +
                         "ORDER BY p.payment_date DESC";
            
            System.out.println("Executing query: " + query);
            
            try (PreparedStatement pstmt = connection.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {
                
                ObservableList<PaymentRecord> payments = FXCollections.observableArrayList();
                int count = 0;
                
                while (rs.next()) {
                    count++;
                    String status = rs.getString("status");
                    // Convert status to proper display name
                    String displayStatus = status;
                    for (PaymentStatus ps : PaymentStatus.values()) {
                        if (ps.name().equals(status)) {
                            displayStatus = ps.getDisplayName();
                            break;
                        }
                    }
                    
                    PaymentRecord record = new PaymentRecord(
                        rs.getString("payment_id"),
                        rs.getString("tin"),
                        rs.getString("taxpayer_name"),
                        rs.getDate("payment_date").toLocalDate(),
                        rs.getDouble("amount_paid"),
                        rs.getString("payment_method"),
                        displayStatus
                    );
                    payments.add(record);
                }
                
                System.out.println("Loaded " + count + " payment records");
                
                if (paymentsTable != null) {
                    paymentsTable.setItems(payments);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading payments: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: Try a simpler query without taxpayer join
            try {
                String fallbackQuery = "SELECT payment_id, amount_paid, payment_method, status, payment_date " +
                                     "FROM payments WHERE status != 'DRAFT' ORDER BY payment_date DESC";
                
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(fallbackQuery)) {
                    
                    ObservableList<PaymentRecord> payments = FXCollections.observableArrayList();
                    
                    while (rs.next()) {
                        String status = rs.getString("status");
                        String displayStatus = status;
                        for (PaymentStatus ps : PaymentStatus.values()) {
                            if (ps.name().equals(status)) {
                                displayStatus = ps.getDisplayName();
                                break;
                            }
                        }
                        
                        PaymentRecord record = new PaymentRecord(
                            rs.getString("payment_id"),
                            "N/A",
                            "Unknown Taxpayer",
                            rs.getDate("payment_date").toLocalDate(),
                            rs.getDouble("amount_paid"),
                            rs.getString("payment_method"),
                            displayStatus
                        );
                        payments.add(record);
                    }
                    
                    if (paymentsTable != null) {
                        paymentsTable.setItems(payments);
                    }
                }
                
            } catch (SQLException ex) {
                System.err.println("Fallback query also failed: " + ex.getMessage());
                showAlert("Data Load Error", "Cannot load payment data. Please check database connection.");
            }
        }
    }
    
    private void setupEventHandlers() {
        try {
            // Tab change handler
            if (paymentTabs != null) {
                paymentTabs.getSelectionModel().selectedItemProperty().addListener(
                    (observable, oldTab, newTab) -> {
                        if (newTab == Quickpayment) {
                            clearQuickPaymentForm();
                        }
                    }
                );
            }
        } catch (Exception e) {
            System.err.println("Error setting up event handlers: " + e.getMessage());
        }
    }
    
    private void setupKeyboardShortcuts() {
        try {
            if (paymentsTable != null) {
                paymentsTable.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.DELETE) {
                        PaymentRecord selected = paymentsTable.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                            deletePayment(selected);
                        }
                    } else if (event.getCode() == KeyCode.ENTER) {
                        PaymentRecord selected = paymentsTable.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                            viewPaymentDetails(selected);
                        }
                    } else if (event.getCode() == KeyCode.F5) {
                        refreshPayments();
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error setting up keyboard shortcuts: " + e.getMessage());
        }
    }
    
    // FIXED: Changed tab event handlers to accept Event (not ActionEvent)
    @FXML
    private void handlerclickAllpaymentsbtn() {
        loadAllPayments();
    }

    @FXML
    private void handlerclickQuickpayment() {
        clearQuickPaymentForm();
    }

    @FXML
    private void handlerclickendDatePicker(ActionEvent event) {
        // Validate date range
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                showAlert("Invalid Date Range", "End date cannot be before start date.");
                if (endDatePicker != null) endDatePicker.setValue(startDatePicker.getValue());
            }
        }
    }

    @FXML
    private void handlerclickfilterbtn(ActionEvent event) {
        filterPayments();
    }

    @FXML
    private void handlerclicknewPaymentBtn(ActionEvent event) {
        openNewPaymentWindow();
    }

    @FXML
    private void handlerclicknewTaxpayerBtn1(ActionEvent event) {
        navigateToDashboard();
    }

    @FXML
    private void handlerclickpaymentStatusFilter(ActionEvent event) {
        filterPayments();
    }

    @FXML
    private void handlerclickprocessQuickPaymentBtn(ActionEvent event) {
        processQuickPayment();
    }

    @FXML
    private void handlerclickquickPaymentMethod(ActionEvent event) {
        // Method selection handled in processQuickPayment
    }

    @FXML
    private void handlerclickstartDatePicker(ActionEvent event) {
        // Validate date range
        if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
                showAlert("Invalid Date Range", "Start date cannot be after end date.");
                if (startDatePicker != null) startDatePicker.setValue(endDatePicker.getValue());
            }
        }
    }
    
    // Additional methods
    
    private void filterPayments() {
        try {
            if (connection == null || connection.isClosed()) {
                showAlert("Connection Error", "Database connection is not available.");
                return;
            }
            
            // First check available columns
            boolean hasFirstName = false;
            boolean hasLastName = false;
            boolean hasBusinessName = false;
            
            String checkQuery = "SELECT column_name FROM information_schema.columns " +
                              "WHERE table_schema = 'tax_management_db' AND table_name = 'taxpayers'";
            
            try (Statement checkStmt = connection.createStatement();
                 ResultSet checkRs = checkStmt.executeQuery(checkQuery)) {
                
                while (checkRs.next()) {
                    String column = checkRs.getString("column_name");
                    if (column.equalsIgnoreCase("first_name")) hasFirstName = true;
                    if (column.equalsIgnoreCase("last_name")) hasLastName = true;
                    if (column.equalsIgnoreCase("business_name")) hasBusinessName = true;
                }
            }
            
            // Build taxpayer name selection
            StringBuilder taxpayerNameSelect = new StringBuilder();
            if (hasFirstName && hasLastName) {
                taxpayerNameSelect.append("CONCAT(t.first_name, ' ', t.last_name)");
            } else if (hasBusinessName) {
                taxpayerNameSelect.append("COALESCE(t.business_name, 'Unknown Taxpayer')");
            } else {
                taxpayerNameSelect.append("'Unknown Taxpayer'");
            }
            
            // Build main query
            StringBuilder query = new StringBuilder(
                "SELECT p.payment_id, COALESCE(t.tin, 'N/A') as tin, " +
                taxpayerNameSelect.toString() + " as taxpayer_name, " +
                "p.payment_date, p.amount_paid, p.payment_method, p.status " +
                "FROM payments p " +
                "LEFT JOIN taxpayers t ON p.taxpayer_id = t.taxpayer_id " +
                "WHERE p.status != 'DRAFT' "
            );
            
            List<Object> parameters = new ArrayList<>();
            
            // Date range filter
            if (startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
                query.append("AND p.payment_date BETWEEN ? AND ? ");
                parameters.add(java.sql.Date.valueOf(startDatePicker.getValue()));
                parameters.add(java.sql.Date.valueOf(endDatePicker.getValue().plusDays(1)));
            }
            
            // Payment method filter
            String method = paymentMethodFilter.getValue();
            if (method != null && !method.isEmpty()) {
                query.append("AND p.payment_method = ? ");
                parameters.add(method);
            }
            
            // Status filter - convert display name to database status
            String status = paymentStatusFilter.getValue();
            if (status != null && !status.isEmpty()) {
                // Convert display status to database status
                String dbStatus = status.toUpperCase();
                for (PaymentStatus ps : PaymentStatus.values()) {
                    if (ps.getDisplayName().equals(status)) {
                        dbStatus = ps.name();
                        break;
                    }
                }
                query.append("AND p.status = ? ");
                parameters.add(dbStatus);
            }
            
            query.append("ORDER BY p.payment_date DESC");
            
            try (PreparedStatement stmt = connection.prepareStatement(query.toString())) {
                for (int i = 0; i < parameters.size(); i++) {
                    stmt.setObject(i + 1, parameters.get(i));
                }
                
                try (ResultSet rs = stmt.executeQuery()) {
                    ObservableList<PaymentRecord> payments = FXCollections.observableArrayList();
                    
                    while (rs.next()) {
                        String dbStatus = rs.getString("status");
                        String displayStatus = dbStatus;
                        for (PaymentStatus ps : PaymentStatus.values()) {
                            if (ps.name().equals(dbStatus)) {
                                displayStatus = ps.getDisplayName();
                                break;
                            }
                        }
                        
                        PaymentRecord record = new PaymentRecord(
                            rs.getString("payment_id"),
                            rs.getString("tin"),
                            rs.getString("taxpayer_name"),
                            rs.getDate("payment_date").toLocalDate(),
                            rs.getDouble("amount_paid"),
                            rs.getString("payment_method"),
                            displayStatus
                        );
                        payments.add(record);
                    }
                    
                    if (paymentsTable != null) {
                        paymentsTable.setItems(payments);
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Error filtering payments: " + e.getMessage());
            e.printStackTrace();
            showAlert("Filter Error", "Error filtering payments: " + e.getMessage());
        }
    }
    
    private void processQuickPayment() {
        String tin = quickPaymentTin != null ? quickPaymentTin.getText().trim() : "";
        String amountStr = quickPaymentAmount != null ? quickPaymentAmount.getText().trim() : "";
        String method = quickPaymentMethod != null ? quickPaymentMethod.getValue() : null;
        
        // Validation
        if (tin.isEmpty()) {
            showAlert("Validation Error", "Please enter TIN.");
            if (quickPaymentTin != null) quickPaymentTin.requestFocus();
            return;
        }
        
        if (amountStr.isEmpty()) {
            showAlert("Validation Error", "Please enter amount.");
            if (quickPaymentAmount != null) quickPaymentAmount.requestFocus();
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showAlert("Validation Error", "Amount must be greater than 0.");
                if (quickPaymentAmount != null) quickPaymentAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Invalid amount format.");
            if (quickPaymentAmount != null) quickPaymentAmount.requestFocus();
            return;
        }
        
        if (method == null || method.isEmpty()) {
            showAlert("Validation Error", "Please select payment method.");
            if (quickPaymentMethod != null) quickPaymentMethod.requestFocus();
            return;
        }
        
        // Confirm payment
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Quick Payment");
        confirmation.setHeaderText("Process Payment");
        confirmation.setContentText(String.format(
            "Process payment for TIN: %s\n" +
            "Amount: ETB %,.2f\n" +
            "Method: %s\n\n" +
            "Are you sure you want to proceed?",
            tin, amount, method
        ));
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (connection == null || connection.isClosed()) {
                    showAlert("Connection Error", "Database connection is not available.");
                    return;
                }
                
                // Generate payment ID
                String paymentId = "PAY-" + System.currentTimeMillis();
                
                // FIXED: Check if taxpayer exists
                String taxpayerCheckQuery = "SELECT taxpayer_id FROM taxpayers WHERE tin = ? LIMIT 1";
                Integer taxpayerId = null;
                
                try (PreparedStatement checkStmt = connection.prepareStatement(taxpayerCheckQuery)) {
                    checkStmt.setString(1, tin);
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            taxpayerId = rs.getInt("taxpayer_id");
                        }
                    }
                }
                
                if (taxpayerId == null) {
                    showAlert("Payment Failed", "No taxpayer found with TIN: " + tin);
                    return;
                }
                
                // Insert payment
                String insertQuery = "INSERT INTO payments (payment_id, taxpayer_id, amount_paid, " +
                                   "payment_method, payment_date, status, created_at) " +
                                   "VALUES (?, ?, ?, ?, NOW(), 'SUCCESSFUL', NOW())";
                
                try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
                    stmt.setString(1, paymentId);
                    stmt.setInt(2, taxpayerId);
                    stmt.setDouble(3, amount);
                    stmt.setString(4, method);
                    
                    int rowsAffected = stmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        showAlert("Payment Successful", 
                            String.format("Payment processed successfully!\n" +
                                         "Payment ID: %s\n" +
                                         "Amount: ETB %,.2f\n" +
                                         "Method: %s", 
                                         paymentId, amount, method));
                        
                        // Clear form
                        clearQuickPaymentForm();
                        
                        // Refresh data
                        refreshPayments();
                    } else {
                        showAlert("Payment Failed", "Payment failed to process.");
                    }
                }
                
            } catch (SQLException e) {
                showAlert("Payment Error", "Error processing payment: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private void clearQuickPaymentForm() {
        if (quickPaymentTin != null) quickPaymentTin.clear();
        if (quickPaymentAmount != null) quickPaymentAmount.clear();
        if (quickPaymentMethod != null) quickPaymentMethod.setValue(null);
    }
    
    private void viewPaymentDetails(PaymentRecord record) {
        try {
            showAlert("Payment Details", 
                String.format("Payment ID: %s\n" +
                             "TIN: %s\n" +
                             "Taxpayer: %s\n" +
                             "Date: %s\n" +
                             "Amount: %s\n" +
                             "Method: %s\n" +
                             "Status: %s",
                             record.getPaymentId(),
                             record.getTin(),
                             record.getTaxpayerName(),
                             record.getFormattedDate(),
                             record.getFormattedAmount(),
                             record.getMethod(),
                             record.getStatus()));
        } catch (Exception e) {
            showAlert("Error", "Cannot view payment details: " + e.getMessage());
        }
    }
    
    private void deletePayment(PaymentRecord record) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel Payment");
        confirmation.setHeaderText("Confirm Cancellation");
        confirmation.setContentText(String.format(
            "Are you sure you want to mark payment %s as cancelled?\n" +
            "TIN: %s\n" +
            "Amount: %s\n\n" +
            "Note: Payments are marked as cancelled, not deleted.",
            record.getPaymentId(), record.getTin(), record.getFormattedAmount()
        ));
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (connection == null || connection.isClosed()) {
                    showAlert("Connection Error", "Database connection is not available.");
                    return;
                }
                
                String query = "UPDATE payments SET status = 'CANCELLED' WHERE payment_id = ?";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.setString(1, record.getPaymentId());
                    
                    int rowsAffected = stmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        showAlert("Payment Cancelled", "Payment has been marked as cancelled.");
                        refreshPayments();
                    }
                }
                
            } catch (SQLException e) {
                showAlert("Cancel Error", "Error cancelling payment: " + e.getMessage());
            }
        }
    }
    
    private void openNewPaymentWindow() {
        try {
            // Try multiple paths for newPayment.fxml
            String[] possiblePaths = {
                "/TMSFXML/newPayment.fxml",
                "/newPayment.fxml",
                "newPayment.fxml",
                "../TMSFXML/newPayment.fxml",
                "../newPayment.fxml"
            };
            
            Parent root = null;
            for (String path : possiblePaths) {
                try {
                    root = FXMLLoader.load(getClass().getResource(path));
                    System.out.println("Loaded newPayment.fxml from: " + path);
                    break;
                } catch (Exception e) {
                    System.out.println("Failed to load from " + path + ": " + e.getMessage());
                }
            }
            
            if (root != null) {
                Stage stage = new Stage();
                stage.setTitle("New Payment");
                stage.setScene(new Scene(root));
                stage.show();
            } else {
                showAlert("Navigation Error", "Cannot find newPayment.fxml file.");
            }
        } catch (Exception e) {
            showAlert("Navigation Error", "Cannot open new payment: " + e.getMessage());
        }
    }
    
    private void navigateToDashboard() {
        try {
            // Try multiple paths for Dashboard.fxml
            String[] possiblePaths = {
                "/TMSFXML/Dashboard.fxml",
                "/Dashboard.fxml",
                "Dashboard.fxml",
                "../TMSFXML/Dashboard.fxml",
                "../Dashboard.fxml"
            };
            
            Parent dashboardRoot = null;
            FXMLLoader loader = null;
            
            for (String path : possiblePaths) {
                try {
                    loader = new FXMLLoader(getClass().getResource(path));
                    dashboardRoot = loader.load();
                    System.out.println("Loaded Dashboard.fxml from: " + path);
                    break;
                } catch (Exception e) {
                    System.out.println("Failed to load from " + path + ": " + e.getMessage());
                }
            }
            
            if (dashboardRoot != null && newTaxpayerBtn1 != null) {
                Stage currentStage = (Stage) newTaxpayerBtn1.getScene().getWindow();
                Scene dashboardScene = new Scene(dashboardRoot);
                currentStage.setScene(dashboardScene);
                currentStage.setTitle("Dashboard");
                currentStage.show();
            } else {
                showAlert("Navigation Error", "Cannot find Dashboard.fxml file.");
            }
            
        } catch (Exception e) {
            showAlert("Navigation Error", "Failed to load Dashboard: " + e.getMessage());
        }
    }
    
    private void refreshPayments() {
        loadPaymentStatistics();
        loadAllPayments();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Cleanup method
    public void cleanup() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}