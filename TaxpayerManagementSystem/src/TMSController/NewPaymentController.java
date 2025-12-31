package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.util.Optional;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class NewPaymentController {

    // Database connection
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    private Connection connection;
    
    // Tax types and periods
    public enum TaxType {
        INCOME_TAX("Income Tax"),
        VAT("Value Added Tax"),
        EXCISE_TAX("Excise Tax"),
        CUSTOMS_DUTY("Customs Duty"),
        STAMP_DUTY("Stamp Duty"),
        PROPERTY_TAX("Property Tax"),
        BUSINESS_TAX("Business Tax");
        
        private final String displayName;
        
        TaxType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    public enum Period {
        MONTHLY("Monthly"),
        QUARTERLY("Quarterly"),
        SEMI_ANNUAL("Semi-Annual"),
        ANNUAL("Annual"),
        AD_HOC("Ad-hoc");
        
        private final String displayName;
        
        Period(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
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
    
    // Taxpayer information
    private int taxpayerId = 0;
    private double totalDueAmount = 0;
    private double baseTax = 0;
    private double penalty = 0;
    private double interest = 0;
    
    @FXML
    private Button Calculatebtn;

    @FXML
    private Button Paymentbtn;

    @FXML
    private TextField addressField;

    @FXML
    private TextField amountField;

    @FXML
    private Button backButtonpaymentpage;

    @FXML
    private Label baseTaxLabel;

    @FXML
    private Button cancelbtn;

    @FXML
    private Label dueAmountLabel;

    @FXML
    private TextField emailField;

    @FXML
    private Label interestLabel;

    @FXML
    private TextArea notesArea;

    @FXML
    private DatePicker paymentDatePicker;

    @FXML
    private ComboBox<String> paymentMethodCombo;

    @FXML
    private Label penaltyLabel;

    @FXML
    private ComboBox<String> periodCombo;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField referenceField;

    @FXML
    private Button saveasDraftbtn;

    @FXML
    private ComboBox<String> taxTypeCombo;

    @FXML
    private TextField taxpayerNameField;

    @FXML
    private TextField tinField;

    @FXML
    private Label totalDueLabel;

    @FXML
    private TextField yearField;
    
    @FXML
    private VBox root;
    
    private ObservableList<String> taxTypes = FXCollections.observableArrayList();
    private ObservableList<String> periods = FXCollections.observableArrayList();
    private ObservableList<String> paymentMethods = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        try {
            // Initialize database connection
            initializeDatabaseConnection();
            
            // Setup UI components
            setupUIComponents();
            
            // Setup event handlers
            setupEventHandlers();
            
            // Setup keyboard shortcuts
            setupKeyboardShortcuts();
            
            // Load current year
            yearField.setText(String.valueOf(LocalDate.now().getYear()));
            
            // Set default payment date to today
            paymentDatePicker.setValue(LocalDate.now());
            
            System.out.println("NewPaymentController initialized successfully");
            
        } catch (SQLException e) {
            showAlert("Database Error", "Cannot connect to database: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Initialization Error", "Error initializing controller: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeDatabaseConnection() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("Database connection established for new payment.");
    }
    
    private void setupUIComponents() {
        try {
            // Initialize tax types
            taxTypes.clear();
            for (TaxType type : TaxType.values()) {
                taxTypes.add(type.getDisplayName());
            }
            if (taxTypeCombo != null) {
                taxTypeCombo.setItems(taxTypes);
                taxTypeCombo.setPromptText("Select tax type");
            }
            
            // Initialize periods
            periods.clear();
            for (Period period : Period.values()) {
                periods.add(period.getDisplayName());
            }
            if (periodCombo != null) {
                periodCombo.setItems(periods);
                periodCombo.setPromptText("Select period");
            }
            
            // Initialize payment methods
            paymentMethods.clear();
            for (PaymentMethod method : PaymentMethod.values()) {
                paymentMethods.add(method.getDisplayName());
            }
            if (paymentMethodCombo != null) {
                paymentMethodCombo.setItems(paymentMethods);
                paymentMethodCombo.setPromptText("Select method");
            }
            
            // Setup text fields with TextFormatter to avoid "start must be <= end" error
            setupTextFieldValidationWithFormatter();
            
            // Setup text area
            if (notesArea != null) {
                notesArea.setPromptText("Enter any additional notes...");
            }
            
            // Setup labels with initial values
            if (baseTaxLabel != null) baseTaxLabel.setText("ETB 0.00");
            if (penaltyLabel != null) penaltyLabel.setText("ETB 0.00");
            if (interestLabel != null) interestLabel.setText("ETB 0.00");
            if (totalDueLabel != null) totalDueLabel.setText("ETB 0.00");
            if (dueAmountLabel != null) dueAmountLabel.setText("ETB 0.00");
            
            // Initially disable calculation and payment buttons
            if (Calculatebtn != null) Calculatebtn.setDisable(true);
            if (Paymentbtn != null) Paymentbtn.setDisable(true);
            if (saveasDraftbtn != null) saveasDraftbtn.setDisable(true);
            
        } catch (Exception e) {
            System.err.println("Error setting up UI components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTextFieldValidationWithFormatter() {
        try {
            // TIN field - 10 digits only using TextFormatter
            if (tinField != null) {
                TextFormatter<String> tinFormatter = new TextFormatter<>(change -> {
                    String newText = change.getControlNewText();
                    if (newText.matches("\\d{0,10}")) {
                        return change;
                    }
                    return null;
                });
                tinField.setTextFormatter(tinFormatter);
                
                // Add listener for auto-search when TIN is complete
                tinField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.length() == 10) {
                        // Use Platform.runLater to avoid text selection conflicts
                        javafx.application.Platform.runLater(() -> {
                            fetchTaxpayerInfo(newValue);
                        });
                    }
                });
            }
            
            // Year field - 4 digits only
            if (yearField != null) {
                TextFormatter<String> yearFormatter = new TextFormatter<>(change -> {
                    String newText = change.getControlNewText();
                    if (newText.matches("\\d{0,4}")) {
                        return change;
                    }
                    return null;
                });
                yearField.setTextFormatter(yearFormatter);
            }
            
            // Amount field - numeric with decimal
            if (amountField != null) {
                TextFormatter<String> amountFormatter = new TextFormatter<>(change -> {
                    String newText = change.getControlNewText();
                    if (newText.isEmpty() || newText.matches("\\d*(\\.\\d*)?")) {
                        return change;
                    }
                    return null;
                });
                amountField.setTextFormatter(amountFormatter);
                
                // Update due amount in real-time
                amountField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null && !newValue.isEmpty()) {
                        try {
                            double amount = Double.parseDouble(newValue);
                            updateDueAmount(amount);
                        } catch (NumberFormatException e) {
                            // Ignore invalid input
                        }
                    }
                });
            }
            
            // Phone field - allow digits, spaces, dashes, plus
            if (phoneField != null) {
                TextFormatter<String> phoneFormatter = new TextFormatter<>(change -> {
                    String newText = change.getControlNewText();
                    if (newText.matches("[\\d\\s\\-\\+]{0,20}")) {
                        return change;
                    }
                    return null;
                });
                phoneField.setTextFormatter(phoneFormatter);
            }
            
            // Email field validation on focus lost
            if (emailField != null) {
                emailField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                    if (!newValue && emailField.getText() != null && !emailField.getText().isEmpty()) {
                        validateEmail(emailField.getText());
                    }
                });
            }
            
            // Address field - no special validation needed
            
            // Reference field - limit length
            if (referenceField != null) {
                TextFormatter<String> refFormatter = new TextFormatter<>(change -> {
                    String newText = change.getControlNewText();
                    if (newText.length() <= 50) {
                        return change;
                    }
                    return null;
                });
                referenceField.setTextFormatter(refFormatter);
            }
            
        } catch (Exception e) {
            System.err.println("Error setting up text field validation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupEventHandlers() {
        try {
            // TIN field listener for auto-search
            if (tinField != null) {
                tinField.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue != null && newValue.length() == 10) {
                        fetchTaxpayerInfo(newValue);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error setting up event handlers: " + e.getMessage());
        }
    }
    
    private void setupKeyboardShortcuts() {
        try {
            if (tinField != null) {
                tinField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER && tinField.getText().length() == 10) {
                        fetchTaxpayerInfo(tinField.getText());
                    }
                });
            }
            
            if (amountField != null) {
                amountField.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    if (event.getCode() == KeyCode.ENTER) {
                        handleCalculate(null);
                    }
                });
            }
            
            if (notesArea != null) {
                notesArea.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    if (event.isControlDown() && event.getCode() == KeyCode.ENTER) {
                        handleProcessPayment(null);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Error setting up keyboard shortcuts: " + e.getMessage());
        }
    }
    
    @FXML
    void handlerclickCalculatebtn(ActionEvent event) {
        handleCalculate(event);
    }

    @FXML
    void handlerclickPaymentbtn(ActionEvent event) {
        handleProcessPayment(event);
    }

    @FXML
    void handlerclickbackButtonpaymentpage(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/PaymentProcessing.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            
            // Close current window
            Stage currentStage = (Stage) backButtonpaymentpage.getScene().getWindow();
            currentStage.close();
            
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation Error", "Cannot load Payment Processing page: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void handlerclickcancelbtn(ActionEvent event) {
        handleCancel(event);
    }

    @FXML
    void handlerclickpaymentDatePicker(ActionEvent event) {
        // Date selection handled automatically
    }

    @FXML
    void handlerclickpaymentMethodCombo(ActionEvent event) {
        // Method selection handled in process payment
    }

    @FXML
    void handlerclickperiodCombo(ActionEvent event) {
        // Period selection handled in calculate
    }

    @FXML
    void handlerclicksaveasDraftbtn(ActionEvent event) {
        handleSaveAsDraft(event);
    }

    @FXML
    void handlerclicktaxTypeCombo(ActionEvent event) {
        // Tax type selection handled in calculate
    }
    
    // Additional methods
    
    private void handleCalculate(ActionEvent event) {
        // Validate TIN exists in database
        if (!validateTaxpayerExists(tinField.getText())) {
            showAlert("Validation Error", "Taxpayer with TIN " + tinField.getText() + " does not exist. Please add the taxpayer first.");
            if (tinField != null) tinField.requestFocus();
            return;
        }
        
        // Validate required fields
        if (tinField.getText().isEmpty()) {
            showAlert("Validation Error", "Please enter TIN.");
            if (tinField != null) tinField.requestFocus();
            return;
        }
        
        if (taxTypeCombo.getValue() == null) {
            showAlert("Validation Error", "Please select tax type.");
            if (taxTypeCombo != null) taxTypeCombo.requestFocus();
            return;
        }
        
        if (periodCombo.getValue() == null) {
            showAlert("Validation Error", "Please select period.");
            if (periodCombo != null) periodCombo.requestFocus();
            return;
        }
        
        String yearText = yearField.getText();
        if (yearText.isEmpty()) {
            showAlert("Validation Error", "Please enter year.");
            if (yearField != null) yearField.requestFocus();
            return;
        }
        
        try {
            int year = Integer.parseInt(yearText);
            if (year < 2000 || year > LocalDate.now().getYear() + 1) {
                showAlert("Validation Error", "Year must be between 2000 and " + (LocalDate.now().getYear() + 1));
                if (yearField != null) yearField.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Invalid year format.");
            if (yearField != null) yearField.requestFocus();
            return;
        }
        
        // Calculate tax amounts
        calculateTaxAmounts();
    }
    
    private boolean validateTaxpayerExists(String tin) {
        try {
            if (tin == null || tin.trim().isEmpty()) {
                return false;
            }
            
            String query = "SELECT COUNT(*) as count FROM taxpayer WHERE tin = ?";
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, tin.trim());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next() && rs.getInt("count") > 0) {
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error validating taxpayer: " + e.getMessage());
        }
        return false;
    }
    
    private void calculateTaxAmounts() {
        try {
            String taxType = taxTypeCombo.getValue();
            String period = periodCombo.getValue();
            int year = Integer.parseInt(yearField.getText());
            
            // Fetch tax rate from database
            String rateQuery = "SELECT tax_rate FROM tax_rates WHERE tax_type = ?";
            PreparedStatement rateStmt = connection.prepareStatement(rateQuery);
            rateStmt.setString(1, taxType);
            ResultSet rateRs = rateStmt.executeQuery();
            
            double taxRate = 0.15; // Default 15%
            if (rateRs.next()) {
                taxRate = rateRs.getDouble("tax_rate");
            }
            
            // Calculate base tax (simplified - in real app would use actual income/transaction data)
            String incomeQuery = "SELECT COALESCE(SUM(taxable_amount), 0) as total_income " +
                               "FROM taxpayer_transactions " +
                               "WHERE taxpayer_id = ? AND YEAR(transaction_date) = ? " +
                               "AND transaction_type = 'INCOME'";
            
            PreparedStatement incomeStmt = connection.prepareStatement(incomeQuery);
            incomeStmt.setInt(1, taxpayerId);
            incomeStmt.setInt(2, year);
            ResultSet incomeRs = incomeStmt.executeQuery();
            
            double taxableIncome = 0;
            if (incomeRs.next()) {
                taxableIncome = incomeRs.getDouble("total_income");
            }
            
            // If no income data, use a default for calculation
            if (taxableIncome == 0) {
                taxableIncome = 100000; // Default for demonstration
            }
            
            // Calculate base tax
            baseTax = taxableIncome * taxRate;
            
            // Calculate penalty (if any)
            penalty = calculatePenalty(taxpayerId, taxType, year);
            
            // Calculate interest
            interest = calculateInterest(baseTax);
            
            // Calculate total due
            totalDueAmount = baseTax + penalty + interest;
            
            // Update UI
            updateCalculationSummary();
            
            // Auto-fill amount field with total due
            DecimalFormat df = new DecimalFormat("#,##0.00");
            if (amountField != null) {
                amountField.setText(df.format(totalDueAmount));
            }
            updateDueAmount(totalDueAmount);
            
            // Enable payment buttons
            if (Calculatebtn != null) Calculatebtn.setDisable(false);
            if (Paymentbtn != null) Paymentbtn.setDisable(false);
            if (saveasDraftbtn != null) saveasDraftbtn.setDisable(false);
            
        } catch (SQLException e) {
            showAlert("Calculation Error", "Error calculating tax amounts: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private double calculatePenalty(int taxpayerId, String taxType, int year) {
        try {
            // Check for overdue payments
            String penaltyQuery = "SELECT COALESCE(SUM(penalty_amount), 0) as total_penalty " +
                                "FROM tax_penalties " +
                                "WHERE taxpayer_id = ? AND tax_type = ? AND year = ? AND status = 'UNPAID'";
            
            PreparedStatement stmt = connection.prepareStatement(penaltyQuery);
            stmt.setInt(1, taxpayerId);
            stmt.setString(2, taxType);
            stmt.setInt(3, year);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getDouble("total_penalty");
            }
        } catch (SQLException e) {
            System.err.println("Error calculating penalty: " + e.getMessage());
        }
        return 0;
    }
    
    private double calculateInterest(double baseAmount) {
        // Simple interest calculation (in real app would use actual overdue days)
        double interestRate = 0.01; // 1% per month
        int overdueMonths = 1; // Assume 1 month overdue
        return baseAmount * interestRate * overdueMonths;
    }
    
    private void updateCalculationSummary() {
        try {
            DecimalFormat df = new DecimalFormat("ETB #,##0.00");
            
            if (baseTaxLabel != null) baseTaxLabel.setText(df.format(baseTax));
            if (penaltyLabel != null) penaltyLabel.setText(df.format(penalty));
            if (interestLabel != null) interestLabel.setText(df.format(interest));
            if (totalDueLabel != null) totalDueLabel.setText(df.format(totalDueAmount));
            
            // Color coding
            if (baseTaxLabel != null) baseTaxLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
            if (penaltyLabel != null) penaltyLabel.setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
            if (interestLabel != null) interestLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
            if (totalDueLabel != null) totalDueLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 16px; -fx-font-weight: bold;");
        } catch (Exception e) {
            System.err.println("Error updating calculation summary: " + e.getMessage());
        }
    }
    
    private void updateDueAmount(double amount) {
        try {
            DecimalFormat df = new DecimalFormat("ETB #,##0.00");
            if (dueAmountLabel != null) {
                dueAmountLabel.setText(df.format(amount));
            }
        } catch (Exception e) {
            System.err.println("Error updating due amount: " + e.getMessage());
        }
    }
    
    private void fetchTaxpayerInfo(String tin) {
        try {
            // First try to check if additional columns exist
            boolean hasFullName = true;
            boolean hasAddress = checkColumnExists("taxpayer", "address");
            boolean hasPhone = checkColumnExists("taxpayer", "phone");
            boolean hasEmail = checkColumnExists("taxpayer", "email");
            
            // Build query based on available columns
            StringBuilder queryBuilder = new StringBuilder("SELECT taxpayer_id");
            
            if (hasFullName) queryBuilder.append(", full_name");
            if (hasAddress) queryBuilder.append(", address");
            if (hasPhone) queryBuilder.append(", phone");
            if (hasEmail) queryBuilder.append(", email");
            
            queryBuilder.append(" FROM taxpayer WHERE tin = ? AND status = 'ACTIVE'");
            
            String query = queryBuilder.toString();
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, tin);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                taxpayerId = rs.getInt("taxpayer_id");
                
                // Set fields if columns exist
                if (hasFullName && taxpayerNameField != null) {
                    taxpayerNameField.setText(rs.getString("full_name"));
                }
                
                if (hasAddress && addressField != null) {
                    String address = rs.getString("address");
                    addressField.setText(address != null ? address : "");
                }
                
                if (hasPhone && phoneField != null) {
                    String phone = rs.getString("phone");
                    phoneField.setText(phone != null ? phone : "");
                }
                
                if (hasEmail && emailField != null) {
                    String email = rs.getString("email");
                    emailField.setText(email != null ? email : "");
                }
                
                // Enable form fields
                enableFormFields(true);
                
                // Auto-calculate if enough data is available
                if (taxpayerId > 0 && !taxTypeCombo.getValue().isEmpty() && 
                    !periodCombo.getValue().isEmpty() && !yearField.getText().isEmpty()) {
                    handleCalculate(null);
                }
                
            } else {
                showAlert("Taxpayer Not Found", "No active taxpayer found with TIN: " + tin);
                clearTaxpayerInfo();
                taxpayerId = 0; // Reset taxpayer ID
            }
            
        } catch (SQLException e) {
            // Fallback to simple query if the dynamic one fails
            try {
                String simpleQuery = "SELECT taxpayer_id, full_name FROM taxpayer WHERE tin = ? AND status = 'ACTIVE'";
                PreparedStatement simpleStmt = connection.prepareStatement(simpleQuery);
                simpleStmt.setString(1, tin);
                ResultSet simpleRs = simpleStmt.executeQuery();
                
                if (simpleRs.next()) {
                    taxpayerId = simpleRs.getInt("taxpayer_id");
                    if (taxpayerNameField != null) {
                        taxpayerNameField.setText(simpleRs.getString("full_name"));
                    }
                    
                    // Clear other fields
                    if (addressField != null) addressField.clear();
                    if (phoneField != null) phoneField.clear();
                    if (emailField != null) emailField.clear();
                    
                    // Enable form fields
                    enableFormFields(true);
                    
                    // Auto-calculate if enough data is available
                    if (taxpayerId > 0 && !taxTypeCombo.getValue().isEmpty() && 
                        !periodCombo.getValue().isEmpty() && !yearField.getText().isEmpty()) {
                        handleCalculate(null);
                    }
                    
                } else {
                    showAlert("Taxpayer Not Found", "No active taxpayer found with TIN: " + tin);
                    clearTaxpayerInfo();
                    taxpayerId = 0;
                }
            } catch (SQLException ex) {
                showAlert("Database Error", "Error fetching taxpayer info: " + ex.getMessage());
                ex.printStackTrace();
                clearTaxpayerInfo();
                taxpayerId = 0;
            }
        }
    }
    
    private boolean checkColumnExists(String tableName, String columnName) {
        try {
            String query = "SELECT COUNT(*) as column_exists FROM information_schema.columns " +
                         "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("column_exists") > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking if column exists: " + e.getMessage());
        }
        return false;
    }
    
    private void handleProcessPayment(ActionEvent event) {
        // Validate taxpayer exists in database
        if (!validateTaxpayerExists(tinField.getText())) {
            showAlert("Validation Error", "Taxpayer with TIN " + tinField.getText() + " does not exist. Please add the taxpayer first.");
            return;
        }
        
        // Validate all fields
        if (!validatePaymentForm()) {
            return;
        }
        
        // Confirm payment
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Payment");
        confirmation.setHeaderText("Process Payment");
        confirmation.setContentText(String.format(
            "Taxpayer: %s\n" +
            "Tax Type: %s\n" +
            "Amount: %s\n" +
            "Method: %s\n\n" +
            "Are you sure you want to process this payment?",
            taxpayerNameField.getText(),
            taxTypeCombo.getValue(),
            amountField.getText(),
            paymentMethodCombo.getValue()
        ));
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            processPayment();
        }
    }
    
    private boolean validatePaymentForm() {
        // Validate TIN exists
        String tin = tinField.getText().trim();
        if (tin.isEmpty() || tin.length() != 10) {
            showAlert("Validation Error", "Please enter a valid 10-digit TIN.");
            if (tinField != null) tinField.requestFocus();
            return false;
        }
        
        // Check if taxpayer exists in database
        if (!validateTaxpayerExists(tin)) {
            showAlert("Validation Error", "Taxpayer with TIN " + tin + " does not exist in the system.");
            if (tinField != null) tinField.requestFocus();
            return false;
        }
        
        // Validate taxpayer info
        if (taxpayerNameField.getText().isEmpty() || taxpayerId == 0) {
            showAlert("Validation Error", "Please search for a valid taxpayer.");
            if (tinField != null) tinField.requestFocus();
            return false;
        }
        
        // Validate tax type
        if (taxTypeCombo.getValue() == null) {
            showAlert("Validation Error", "Please select tax type.");
            if (taxTypeCombo != null) taxTypeCombo.requestFocus();
            return false;
        }
        
        // Validate period
        if (periodCombo.getValue() == null) {
            showAlert("Validation Error", "Please select period.");
            if (periodCombo != null) periodCombo.requestFocus();
            return false;
        }
        
        // Validate year
        if (yearField.getText().isEmpty()) {
            showAlert("Validation Error", "Please enter year.");
            if (yearField != null) yearField.requestFocus();
            return false;
        }
        
        // Validate amount
        if (amountField.getText().isEmpty()) {
            showAlert("Validation Error", "Please enter amount.");
            if (amountField != null) amountField.requestFocus();
            return false;
        }
        
        try {
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) {
                showAlert("Validation Error", "Amount must be greater than 0.");
                if (amountField != null) amountField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Invalid amount format.");
            if (amountField != null) amountField.requestFocus();
            return false;
        }
        
        // Validate payment method
        if (paymentMethodCombo.getValue() == null) {
            showAlert("Validation Error", "Please select payment method.");
            if (paymentMethodCombo != null) paymentMethodCombo.requestFocus();
            return false;
        }
        
        // Validate payment date
        if (paymentDatePicker.getValue() == null) {
            showAlert("Validation Error", "Please select payment date.");
            if (paymentDatePicker != null) paymentDatePicker.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void processPayment() {
        try {
            // Double-check taxpayer exists
            if (!validateTaxpayerExists(tinField.getText())) {
                showAlert("Payment Error", "Taxpayer no longer exists in the system.");
                return;
            }
            
            // Generate payment ID
            String paymentId = generatePaymentId();
            double amount = Double.parseDouble(amountField.getText());
            
            // Insert payment record
            String query = "INSERT INTO payments (payment_id, taxpayer_id, tax_type, period, year, " +
                         "amount_paid, payment_method, payment_date, reference_number, status, " +
                         "notes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'SUCCESSFUL', ?, NOW())";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, paymentId);
            stmt.setInt(2, taxpayerId);
            stmt.setString(3, taxTypeCombo.getValue());
            stmt.setString(4, periodCombo.getValue());
            stmt.setInt(5, Integer.parseInt(yearField.getText()));
            stmt.setDouble(6, amount);
            stmt.setString(7, paymentMethodCombo.getValue());
            stmt.setDate(8, java.sql.Date.valueOf(paymentDatePicker.getValue()));
            stmt.setString(9, referenceField.getText().isEmpty() ? null : referenceField.getText());
            stmt.setString(10, notesArea.getText().isEmpty() ? null : notesArea.getText());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Update tax assessments
                updateTaxAssessmentsAfterPayment(paymentId);
                
                // Generate receipt
                generatePaymentReceipt(paymentId);
                
                // Show success message
                showSuccessMessage(paymentId, amount);
                
                // Clear form
                handleClearForm();
            }
            
        } catch (SQLException e) {
            if (e.getErrorCode() == 1452) { // Foreign key constraint violation
                showAlert("Payment Error", "Taxpayer does not exist in the system. Please add the taxpayer first.");
            } else {
                showAlert("Payment Error", "Error processing payment: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }
    
    private String generatePaymentId() {
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", (int)(Math.random() * 10000));
        return "PAY-" + timestamp + "-" + random;
    }
    
    private void updateTaxAssessmentsAfterPayment(String paymentId) {
        try {
            // Find or create tax assessment
            String assessmentQuery = "SELECT assessment_id FROM tax_assessments " +
                                   "WHERE taxpayer_id = ? AND tax_type = ? AND year = ? AND period = ?";
            
            PreparedStatement assessmentStmt = connection.prepareStatement(assessmentQuery);
            assessmentStmt.setInt(1, taxpayerId);
            assessmentStmt.setString(2, taxTypeCombo.getValue());
            assessmentStmt.setInt(3, Integer.parseInt(yearField.getText()));
            assessmentStmt.setString(4, periodCombo.getValue());
            ResultSet assessmentRs = assessmentStmt.executeQuery();
            
            int assessmentId;
            if (assessmentRs.next()) {
                assessmentId = assessmentRs.getInt("assessment_id");
                
                // Update existing assessment
                String updateQuery = "UPDATE tax_assessments SET amount_paid = amount_paid + ?, " +
                                   "status = CASE WHEN (amount_due - (amount_paid + ?)) <= 0 THEN 'PAID' ELSE 'PENDING' END " +
                                   "WHERE assessment_id = ?";
                
                PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
                updateStmt.setDouble(1, Double.parseDouble(amountField.getText()));
                updateStmt.setDouble(2, Double.parseDouble(amountField.getText()));
                updateStmt.setInt(3, assessmentId);
                updateStmt.executeUpdate();
                
            } else {
                // Create new assessment
                String insertQuery = "INSERT INTO tax_assessments (taxpayer_id, tax_type, period, year, " +
                                   "amount_due, amount_paid, status, due_date, created_at) " +
                                   "VALUES (?, ?, ?, ?, ?, ?, 'PARTIAL', DATE_ADD(NOW(), INTERVAL 30 DAY), NOW())";
                
                PreparedStatement insertStmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
                insertStmt.setInt(1, taxpayerId);
                insertStmt.setString(2, taxTypeCombo.getValue());
                insertStmt.setString(3, periodCombo.getValue());
                insertStmt.setInt(4, Integer.parseInt(yearField.getText()));
                insertStmt.setDouble(5, totalDueAmount);
                insertStmt.setDouble(6, Double.parseDouble(amountField.getText()));
                insertStmt.executeUpdate();
                
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    assessmentId = generatedKeys.getInt(1);
                } else {
                    return;
                }
            }
            
            // Record payment allocation
            recordPaymentAllocation(assessmentId, paymentId, Double.parseDouble(amountField.getText()));
            
        } catch (SQLException e) {
            System.err.println("Error updating tax assessments: " + e.getMessage());
        }
    }
    
    private void recordPaymentAllocation(int assessmentId, String paymentId, double amount) {
        try {
            String query = "INSERT INTO payment_allocations (assessment_id, payment_id, amount, allocated_date) " +
                         "VALUES (?, ?, ?, NOW())";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, assessmentId);
            stmt.setString(2, paymentId);
            stmt.setDouble(3, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error recording payment allocation: " + e.getMessage());
        }
    }
    
    private void generatePaymentReceipt(String paymentId) {
        try {
            // Insert receipt record
            String query = "INSERT INTO payment_receipts (payment_id, receipt_number, issued_date, " +
                         "issued_by) VALUES (?, ?, NOW(), 'System')";
            
            String receiptNumber = "RCPT-" + paymentId.substring(4);
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, paymentId);
            stmt.setString(2, receiptNumber);
            stmt.executeUpdate();
            
            // Log receipt generation
            logPaymentActivity(paymentId, "RECEIPT_GENERATED", "Receipt: " + receiptNumber);
            
        } catch (SQLException e) {
            System.err.println("Error generating receipt: " + e.getMessage());
        }
    }
    
    private void showSuccessMessage(String paymentId, double amount) {
        DecimalFormat df = new DecimalFormat("ETB #,##0.00");
        
        Alert success = new Alert(Alert.AlertType.INFORMATION);
        success.setTitle("Payment Successful");
        success.setHeaderText("Payment Processed Successfully");
        success.setContentText(String.format(
            "Payment ID: %s\n" +
            "Amount: %s\n" +
            "Taxpayer: %s\n" +
            "Date: %s\n\n" +
            "A receipt has been generated. Please provide the receipt to the taxpayer.",
            paymentId,
            df.format(amount),
            taxpayerNameField.getText(),
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ));
        success.showAndWait();
    }
    
    private void handleSaveAsDraft(ActionEvent event) {
        // Validate taxpayer exists
        String tin = tinField.getText().trim();
        if (!tin.isEmpty() && !validateTaxpayerExists(tin)) {
            showAlert("Validation Error", "Taxpayer with TIN " + tin + " does not exist. Please add the taxpayer first.");
            return;
        }
        
        if (!validateDraftForm()) {
            return;
        }
        
        try {
            String paymentId = "DRAFT-" + System.currentTimeMillis();
            
            String query = "INSERT INTO payments (payment_id, taxpayer_id, tax_type, period, year, " +
                         "amount_paid, payment_method, payment_date, reference_number, status, " +
                         "notes, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, NOW())";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, paymentId);
            
            // Only set taxpayer_id if it exists
            if (taxpayerId > 0) {
                stmt.setInt(2, taxpayerId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            
            stmt.setString(3, taxTypeCombo.getValue() != null ? taxTypeCombo.getValue() : null);
            stmt.setString(4, periodCombo.getValue() != null ? periodCombo.getValue() : null);
            
            // Handle year
            String yearText = yearField.getText();
            if (!yearText.isEmpty()) {
                try {
                    stmt.setInt(5, Integer.parseInt(yearText));
                } catch (NumberFormatException e) {
                    stmt.setNull(5, Types.INTEGER);
                }
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            
            // Handle amount
            String amountText = amountField.getText();
            if (!amountText.isEmpty()) {
                try {
                    stmt.setDouble(6, Double.parseDouble(amountText));
                } catch (NumberFormatException e) {
                    stmt.setNull(6, Types.DOUBLE);
                }
            } else {
                stmt.setNull(6, Types.DOUBLE);
            }
            
            stmt.setString(7, paymentMethodCombo.getValue() != null ? paymentMethodCombo.getValue() : null);
            
            // Handle payment date
            if (paymentDatePicker.getValue() != null) {
                stmt.setDate(8, java.sql.Date.valueOf(paymentDatePicker.getValue()));
            } else {
                stmt.setNull(8, Types.DATE);
            }
            
            stmt.setString(9, referenceField.getText().isEmpty() ? null : referenceField.getText());
            stmt.setString(10, notesArea.getText().isEmpty() ? null : notesArea.getText());
            
            stmt.executeUpdate();
            
            showAlert("Draft Saved", "Payment draft saved successfully. Draft ID: " + paymentId);
            
            // Clear form
            handleClearForm();
            
        } catch (SQLException e) {
            if (e.getErrorCode() == 1452) { // Foreign key constraint violation
                showAlert("Save Error", "Cannot save draft: Taxpayer does not exist in the system.");
            } else {
                showAlert("Save Error", "Error saving draft: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }
    
    private boolean validateDraftForm() {
        // Minimum validation for draft
        if (tinField.getText().isEmpty()) {
            showAlert("Validation Error", "Please enter TIN for draft.");
            if (tinField != null) tinField.requestFocus();
            return false;
        }
        
        // If TIN is provided, check if taxpayer exists
        String tin = tinField.getText().trim();
        if (!tin.isEmpty() && !validateTaxpayerExists(tin)) {
            showAlert("Validation Error", "Taxpayer with TIN " + tin + " does not exist.");
            return false;
        }
        
        return true;
    }
    
    private void handleCancel(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel Payment");
        confirmation.setHeaderText("Are you sure you want to cancel?");
        confirmation.setContentText("All unsaved changes will be lost.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            handleClearForm();
        }
    }
    
    private void handleClearForm() {
        clearTaxpayerInfo();
        clearPaymentDetails();
        clearCalculationSummary();
    }
    
    private void clearTaxpayerInfo() {
        if (tinField != null) tinField.clear();
        if (taxpayerNameField != null) taxpayerNameField.clear();
        if (addressField != null) addressField.clear();
        if (phoneField != null) phoneField.clear();
        if (emailField != null) emailField.clear();
        taxpayerId = 0;
        enableFormFields(false);
    }
    
    private void clearPaymentDetails() {
        if (taxTypeCombo != null) taxTypeCombo.setValue(null);
        if (periodCombo != null) periodCombo.setValue(null);
        if (yearField != null) yearField.clear();
        if (amountField != null) amountField.clear();
        if (paymentDatePicker != null) paymentDatePicker.setValue(null);
        if (paymentMethodCombo != null) paymentMethodCombo.setValue(null);
        if (referenceField != null) referenceField.clear();
        if (notesArea != null) notesArea.clear();
    }
    
    private void clearCalculationSummary() {
        if (baseTaxLabel != null) baseTaxLabel.setText("ETB 0.00");
        if (penaltyLabel != null) penaltyLabel.setText("ETB 0.00");
        if (interestLabel != null) interestLabel.setText("ETB 0.00");
        if (totalDueLabel != null) totalDueLabel.setText("ETB 0.00");
        if (dueAmountLabel != null) dueAmountLabel.setText("ETB 0.00");
    }
    
    private void enableFormFields(boolean enabled) {
        if (taxTypeCombo != null) taxTypeCombo.setDisable(!enabled);
        if (periodCombo != null) periodCombo.setDisable(!enabled);
        if (yearField != null) yearField.setDisable(!enabled);
        if (amountField != null) amountField.setDisable(!enabled);
        if (paymentDatePicker != null) paymentDatePicker.setDisable(!enabled);
        if (paymentMethodCombo != null) paymentMethodCombo.setDisable(!enabled);
        if (referenceField != null) referenceField.setDisable(!enabled);
        if (notesArea != null) notesArea.setDisable(!enabled);
        if (Calculatebtn != null) Calculatebtn.setDisable(!enabled);
        if (Paymentbtn != null) Paymentbtn.setDisable(!enabled);
        if (saveasDraftbtn != null) saveasDraftbtn.setDisable(!enabled);
    }
    
    private void validateEmail(String email) {
        if (email != null && !email.isEmpty()) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
            if (!email.matches(emailRegex)) {
                if (emailField != null) emailField.setStyle("-fx-border-color: #F44336;");
                showAlert("Validation", "Invalid email format.");
            } else {
                if (emailField != null) emailField.setStyle("-fx-border-color: #4CAF50;");
            }
        }
    }
    
    private void logPaymentActivity(String paymentId, String action, String details) {
        try {
            String query = "INSERT INTO payment_audit_log (payment_id, action, details, user_id) " +
                         "VALUES (?, ?, ?, 'admin')";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, paymentId);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error logging payment activity: " + e.getMessage());
        }
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
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}