package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.*;
import java.util.regex.Pattern;

public class SignUpController {

    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    
    // Validation patterns - FIXED PHONE PATTERN
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    // ACCEPTS BOTH FORMATS: +251-9XX-XXXXXX AND +2519XXXXXXXX
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+251-?9\\d{2}-?\\d{6}|09\\d{8})$");
    // SIMPLIFIED PASSWORD PATTERN for easier testing
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{4,}$"); // At least 4 chars
    
    private Connection connection;

    // FXML Fields
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private ComboBox<String> genderComboBox;
    @FXML private DatePicker dobDatePicker;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField businessNameField;
    @FXML private TextArea addressField;
    
    // Account Information fields
    @FXML private TextField usernameField;
    @FXML private TextField taxpayerIdField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> userTypeComboBox;
    @FXML private TextField businessTinField;
    
    // Checkboxes
    @FXML private CheckBox termsCheckBox;
    @FXML private CheckBox privacyCheckBox;
    @FXML private CheckBox newsletterCheckBox;
    
    // Buttons
    @FXML private Button signUpButton;
    @FXML private Button clearButton;
    @FXML private Button cancelButton;
    @FXML private Button showPasswordBtn;
    
    // Language selector
    @FXML private ComboBox<String> languageComboBox;
    
    // Hyperlinks
    @FXML private Hyperlink viewTermsLink;
    @FXML private Hyperlink viewPrivacyLink;
    @FXML private Hyperlink signInLink;
    
    // Footer links
    @FXML private Hyperlink footerPrivacyLink;
    @FXML private Hyperlink footerTermsLink;
    @FXML private Hyperlink footerSupportLink;
    
    // State variables
    private boolean isPasswordVisible = false;
    private boolean isNavigating = false;
    
    @FXML
    public void initialize() {
        try {
            System.out.println("=== Initializing SignUpController ===");
            
            // Initialize database connection
            initializeDatabaseConnection();
            
            // Setup UI components
            setupUIComponents();
            
            // Setup event handlers
            setupEventHandlers();
            
            // Load combo box data
            loadComboBoxData();
            
            // Add footer link handlers
            setupFooterLinks();
            
            // Set up form validation
            setupFormValidation();
            
            System.out.println("=== SignUpController initialized successfully ===");
            System.out.println("=== TEST DATA FOR QUICK TESTING ===");
            System.out.println("Phone formats accepted:");
            System.out.println("1. +251-921-109998 (with dashes)");
            System.out.println("2. +251921109998 (no dashes)");
            System.out.println("3. 0912345678 (local format)");
            System.out.println("Password: At least 4 characters");
            
        } catch (Exception e) {
            System.err.println("=== ERROR initializing controller ===");
            showAlert("Initialization Error", 
                     "Error initializing controller: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void initializeDatabaseConnection() {
        try {
            System.out.println("Connecting to database...");
            
            // Close existing connection if any
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(true); // Auto-commit for simplicity
            
            System.out.println("✓ Database connected successfully!");
            
        } catch (ClassNotFoundException e) {
            System.err.println("✗ MySQL JDBC Driver not found");
            showAlert("Database Error", 
                     "MySQL JDBC Driver not found: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
        } catch (SQLException e) {
            System.err.println("✗ Failed to connect to database");
            showAlert("Database Error", 
                     "Failed to connect to database: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void setupUIComponents() {
        System.out.println("Setting up UI components...");
        
        // Set prompt texts - UPDATED PHONE PROMPT
        phoneField.setPromptText("+251-9XX-XXXXXX or +2519XXXXXXXX");
        emailField.setPromptText("example@domain.com");
        businessNameField.setPromptText("Enter business name");
        businessTinField.setPromptText("Enter business TIN");
        
        // Setup date picker
        if (dobDatePicker != null) {
            dobDatePicker.setPromptText("YYYY-MM-DD");
            System.out.println("✓ DatePicker initialized");
        } else {
            System.err.println("✗ ERROR: dobDatePicker is null!");
        }
        
        // Set initial button states
        if (signUpButton != null) {
            signUpButton.setDisable(true); // Disabled until form is valid
            System.out.println("✓ Sign Up button initialized");
        } else {
            System.err.println("✗ ERROR: signUpButton is null!");
        }
        
        if (clearButton != null) {
            System.out.println("✓ Clear button initialized");
        }
        
        if (showPasswordBtn != null) {
            showPasswordBtn.setText("👁");
            System.out.println("✓ Show Password button initialized");
        }
        
        System.out.println("✓ UI components setup complete");
    }

    private void setupEventHandlers() {
        System.out.println("Setting up event handlers...");
        
        // Button actions
        if (signUpButton != null) {
            System.out.println("Setting up signUpButton handler...");
            signUpButton.setOnAction(e -> {
                System.out.println("=== SIGN UP BUTTON CLICKED ===");
                handleSignUp(e);
            });
            System.out.println("✓ Sign Up button handler set");
        } else {
            System.err.println("✗ ERROR: signUpButton is null!");
        }
        
        if (clearButton != null) {
            clearButton.setOnAction(this::handleClear);
            System.out.println("✓ Clear button handler set");
        }
        
        if (cancelButton != null) {
            cancelButton.setOnAction(this::handleCancel);
            System.out.println("✓ Cancel button handler set");
        }
        
        if (showPasswordBtn != null) {
            showPasswordBtn.setOnAction(this::handleShowPassword);
            System.out.println("✓ Show Password button handler set");
        }
        
        // Checkbox actions
        if (termsCheckBox != null) {
            termsCheckBox.setOnAction(e -> {
                System.out.println("Terms checkbox changed: " + termsCheckBox.isSelected());
                validateFormCompletion();
            });
        }
        
        if (privacyCheckBox != null) {
            privacyCheckBox.setOnAction(e -> {
                System.out.println("Privacy checkbox changed: " + privacyCheckBox.isSelected());
                validateFormCompletion();
            });
        }
        
        // Hyperlink actions
        if (signInLink != null) {
            signInLink.setOnAction(e -> {
                System.out.println("Sign In link clicked");
                handleSignInLink();
            });
            System.out.println("✓ Sign In link handler set");
        }
        
        if (viewTermsLink != null) {
            viewTermsLink.setOnAction(e -> showTermsDialog());
        }
        
        if (viewPrivacyLink != null) {
            viewPrivacyLink.setOnAction(e -> showPrivacyDialog());
        }
        
        System.out.println("✓ Event handlers setup complete");
    }

    private void setupFormValidation() {
        System.out.println("Setting up form validation...");
        
        // Phone number validator - UPDATED FOR FLEXIBLE VALIDATION
        if (phoneField != null) {
            phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("Phone changed: " + newVal);
                if (!newVal.isEmpty()) {
                    if (validatePhoneNumber(newVal)) {
                        phoneField.setStyle("-fx-border-color: green; -fx-border-width: 1;");
                        System.out.println("✓ Phone number valid");
                    } else {
                        phoneField.setStyle("-fx-border-color: red; -fx-border-width: 1;");
                        System.out.println("✗ Phone number invalid");
                    }
                } else {
                    phoneField.setStyle("");
                }
                validateFormCompletion();
            });
        }
        
        // Email validator
        if (emailField != null) {
            emailField.textProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("Email changed: " + newVal);
                if (!newVal.isEmpty()) {
                    if (validateEmail(newVal)) {
                        emailField.setStyle("-fx-border-color: green; -fx-border-width: 1;");
                    } else {
                        emailField.setStyle("-fx-border-color: red; -fx-border-width: 1;");
                    }
                } else {
                    emailField.setStyle("");
                }
                validateFormCompletion();
            });
        }
        
        // Password strength indicator
        if (passwordField != null) {
            passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("Password changed (length): " + newVal.length());
                if (!newVal.isEmpty()) {
                    if (validatePassword(newVal)) {
                        passwordField.setStyle("-fx-border-color: green; -fx-border-width: 1;");
                    } else {
                        passwordField.setStyle("-fx-border-color: red; -fx-border-width: 1;");
                    }
                } else {
                    passwordField.setStyle("");
                }
                checkPasswordMatch();
                validateFormCompletion();
            });
        }
        
        // Confirm password match validator
        if (confirmPasswordField != null) {
            confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
                checkPasswordMatch();
                validateFormCompletion();
            });
        }
        
        // Add listeners to all required fields
        TextField[] requiredFields = {
            firstNameField, lastNameField, emailField, phoneField,
            usernameField, taxpayerIdField
        };
        
        for (TextField field : requiredFields) {
            if (field != null) {
                field.textProperty().addListener((obs, oldVal, newVal) -> {
                    validateFormCompletion();
                });
            }
        }
        
        // DatePicker listener
        if (dobDatePicker != null) {
            dobDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("Date of Birth changed: " + newVal);
                validateFormCompletion();
            });
        }
        
        // User type listener for business fields
        if (userTypeComboBox != null) {
            userTypeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("User Type changed: " + newVal);
                updateBusinessFieldsVisibility();
                validateFormCompletion();
            });
        }
        
        System.out.println("✓ Form validation setup complete");
    }

    private void setupFooterLinks() {
        if (footerPrivacyLink != null) {
            footerPrivacyLink.setOnAction(e -> showPrivacyDialog());
        }
        
        if (footerTermsLink != null) {
            footerTermsLink.setOnAction(e -> showTermsDialog());
        }
        
        if (footerSupportLink != null) {
            footerSupportLink.setOnAction(e -> showSupportDialog());
        }
    }

    private void loadComboBoxData() {
        System.out.println("Loading combo box data...");
        
        // Gender options
        if (genderComboBox != null) {
            genderComboBox.getItems().addAll("Male", "Female", "Other");
            genderComboBox.setValue("Male");
            System.out.println("✓ Gender ComboBox loaded");
        }
        
        // Language options
        if (languageComboBox != null) {
            languageComboBox.getItems().addAll(
                "English",
                "አማርኛ (Amharic)",
                "Afaan Oromoo",
                "ትግርኛ (Tigrinya)"
            );
            languageComboBox.setValue("English");
            System.out.println("✓ Language ComboBox loaded");
        }
        
        // User type options
        if (userTypeComboBox != null) {
            userTypeComboBox.getItems().addAll(
                "Individual Taxpayer",
                "Business Taxpayer",
                "Tax Consultant",
                "Government Employee"
            );
            userTypeComboBox.setValue("Individual Taxpayer");
            System.out.println("✓ User Type ComboBox loaded");
        }
        
        System.out.println("✓ Combo box data loaded");
    }

    // ==================== EVENT HANDLER METHODS ====================

    @FXML
    private void handleSignUp(ActionEvent event) {
        System.out.println("=== STARTING SIGN UP PROCESS ===");
        System.out.println("Validating form...");
        
        // Validate form
        if (validateForm()) {
            System.out.println("✓ Form validation passed!");
            
            // Save to database
            if (saveUserToDatabase()) {
                showAlert("Registration Successful",
                         "Your account has been created successfully!\n\n" +
                         "First Name: " + firstNameField.getText() + "\n" +
                         "Email: " + emailField.getText() + "\n" +
                         "Username: " + usernameField.getText() + "\n\n" +
                         "You can now log in with your credentials.",
                         Alert.AlertType.INFORMATION);
                
                // Clear form and navigate to login
                clearForm();
                navigateToLoginPage();
            } else {
                showAlert("Registration Failed",
                         "Failed to create account. Please try again.",
                         Alert.AlertType.ERROR);
            }
        } else {
            System.out.println("✗ Form validation failed!");
        }
    }

    @FXML
    private void handleClear(ActionEvent event) {
        System.out.println("Clear button clicked");
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Clear Form");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Are you sure you want to clear all form fields?");
        
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        confirmation.getButtonTypes().setAll(yesButton, noButton);
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == yesButton) {
                clearForm();
                System.out.println("✓ Form cleared");
            }
        });
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        System.out.println("Cancel button clicked");
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel Registration");
        confirmation.setHeaderText(null);
        confirmation.setContentText("Are you sure you want to cancel registration?");
        
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        confirmation.getButtonTypes().setAll(yesButton, noButton);
        
        confirmation.showAndWait().ifPresent(response -> {
            if (response == yesButton) {
                navigateToLoginPage();
            }
        });
    }

    @FXML
    private void handleShowPassword(ActionEvent event) {
        if (!isPasswordVisible) {
            // Show password
            showPasswordBtn.setText("Hide");
            String password = passwordField.getText();
            passwordField.clear();
            passwordField.setPromptText(password);
            isPasswordVisible = true;
            System.out.println("Password shown");
        } else {
            // Hide password
            showPasswordBtn.setText("👁");
            String visiblePassword = passwordField.getPromptText();
            passwordField.setText(visiblePassword);
            passwordField.setPromptText("Enter password");
            isPasswordVisible = false;
            System.out.println("Password hidden");
        }
    }
    
    // Handle Sign In link click
    @FXML
    private void handleSignInLink() {
        System.out.println("Sign In link clicked");
        navigateToLoginPage();
    }

    // ==================== VALIDATION METHODS ====================

    private void validateFormCompletion() {
        System.out.println("=== Validating form completion ===");
        
        boolean allFieldsValid = true;
        
        // Check required fields
        if (firstNameField.getText().trim().isEmpty()) {
            System.out.println("✗ First Name missing");
            allFieldsValid = false;
        } else {
            System.out.println("✓ First Name: " + firstNameField.getText());
        }
        
        if (lastNameField.getText().trim().isEmpty()) {
            System.out.println("✗ Last Name missing");
            allFieldsValid = false;
        } else {
            System.out.println("✓ Last Name: " + lastNameField.getText());
        }
        
        if (!validateEmail(emailField.getText())) {
            System.out.println("✗ Invalid Email: " + emailField.getText());
            allFieldsValid = false;
        } else {
            System.out.println("✓ Email: " + emailField.getText());
        }
        
        if (!validatePhoneNumber(phoneField.getText())) {
            System.out.println("✗ Invalid Phone: " + phoneField.getText());
            allFieldsValid = false;
        } else {
            System.out.println("✓ Phone: " + phoneField.getText());
        }
        
        if (usernameField.getText().trim().isEmpty()) {
            System.out.println("✗ Username missing");
            allFieldsValid = false;
        } else {
            System.out.println("✓ Username: " + usernameField.getText());
        }
        
        if (taxpayerIdField.getText().trim().isEmpty()) {
            System.out.println("✗ Taxpayer ID missing");
            allFieldsValid = false;
        } else {
            System.out.println("✓ Taxpayer ID: " + taxpayerIdField.getText());
        }
        
        if (!validatePassword(passwordField.getText())) {
            System.out.println("✗ Invalid Password (length: " + passwordField.getText().length() + ")");
            allFieldsValid = false;
        } else {
            System.out.println("✓ Password valid");
        }
        
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            System.out.println("✗ Passwords don't match");
            allFieldsValid = false;
        } else {
            System.out.println("✓ Passwords match");
        }
        
        if (userTypeComboBox.getValue() == null) {
            System.out.println("✗ User Type not selected");
            allFieldsValid = false;
        } else {
            System.out.println("✓ User Type: " + userTypeComboBox.getValue());
        }
        
        if (!termsCheckBox.isSelected()) {
            System.out.println("✗ Terms not accepted");
            allFieldsValid = false;
        } else {
            System.out.println("✓ Terms accepted");
        }
        
        if (!privacyCheckBox.isSelected()) {
            System.out.println("✗ Privacy not accepted");
            allFieldsValid = false;
        } else {
            System.out.println("✓ Privacy accepted");
        }
        
        // Additional validation for business users
        if ("Business Taxpayer".equals(userTypeComboBox.getValue())) {
            if (businessNameField.getText().trim().isEmpty()) {
                System.out.println("✗ Business Name required for Business Taxpayer");
                allFieldsValid = false;
            }
            if (businessTinField.getText().trim().isEmpty()) {
                System.out.println("✗ Business TIN required for Business Taxpayer");
                allFieldsValid = false;
            }
        }
        
        System.out.println("All fields valid: " + allFieldsValid);
        
        // Enable/disable sign up button
        if (signUpButton != null) {
            signUpButton.setDisable(!allFieldsValid);
            System.out.println("Sign Up button enabled: " + allFieldsValid);
        }
    }

    private boolean validateForm() {
        System.out.println("=== Performing full form validation ===");
        
        StringBuilder errorMessages = new StringBuilder();
        
        // Personal Information Validation
        if (firstNameField.getText().trim().isEmpty()) {
            errorMessages.append("• First Name is required\n");
        }
        
        if (lastNameField.getText().trim().isEmpty()) {
            errorMessages.append("• Last Name is required\n");
        }
        
        if (!validateEmail(emailField.getText())) {
            errorMessages.append("• Valid Email Address is required\n");
        }
        
        if (!validatePhoneNumber(phoneField.getText())) {
            errorMessages.append("• Valid Ethiopian Phone Number is required\n");
            errorMessages.append("  Accepted formats:\n");
            errorMessages.append("  - +251-921-109998 (with dashes)\n");
            errorMessages.append("  - +251921109998 (no dashes)\n");
            errorMessages.append("  - 0912345678 (local format)\n");
        }
        
        // Account Information Validation
        if (usernameField.getText().trim().isEmpty()) {
            errorMessages.append("• Username is required\n");
        } else if (usernameField.getText().length() < 4) {
            errorMessages.append("• Username must be at least 4 characters\n");
        }
        
        if (taxpayerIdField.getText().trim().isEmpty()) {
            errorMessages.append("• Taxpayer ID is required\n");
        }
        
        if (!validatePassword(passwordField.getText())) {
            errorMessages.append("• Password must be at least 4 characters\n");
        }
        
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            errorMessages.append("• Passwords do not match\n");
        }
        
        if (userTypeComboBox.getValue() == null) {
            errorMessages.append("• User Type is required\n");
        }
        
        // Terms and Privacy Validation
        if (!termsCheckBox.isSelected()) {
            errorMessages.append("• You must agree to the Terms and Conditions\n");
        }
        
        if (!privacyCheckBox.isSelected()) {
            errorMessages.append("• You must agree to the Privacy Policy\n");
        }
        
        // Business-specific validation
        if ("Business Taxpayer".equals(userTypeComboBox.getValue())) {
            if (businessNameField.getText().trim().isEmpty()) {
                errorMessages.append("• Business Name is required for Business Taxpayers\n");
            }
            if (businessTinField.getText().trim().isEmpty()) {
                errorMessages.append("• Business TIN is required for Business Taxpayers\n");
            }
        }
        
        if (errorMessages.length() > 0) {
            System.out.println("Validation errors found:\n" + errorMessages.toString());
            showAlert("Validation Errors", errorMessages.toString(), Alert.AlertType.ERROR);
            return false;
        }
        
        System.out.println("✓ All validations passed");
        return true;
    }

    private boolean validatePhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        
        String cleaned = phone.trim();
        
        // Check multiple formats
        return PHONE_PATTERN.matcher(cleaned).matches();
    }

    private boolean validateEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean validatePassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    private void checkPasswordMatch() {
        if (passwordField == null || confirmPasswordField == null) {
            return;
        }
        
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        
        if (!password.isEmpty() && !confirm.isEmpty()) {
            if (password.equals(confirm)) {
                confirmPasswordField.setStyle("-fx-border-color: green; -fx-border-width: 1;");
            } else {
                confirmPasswordField.setStyle("-fx-border-color: red; -fx-border-width: 1;");
            }
        } else {
            confirmPasswordField.setStyle("");
        }
    }

    private void updateBusinessFieldsVisibility() {
        boolean isBusinessUser = "Business Taxpayer".equals(userTypeComboBox.getValue());
        
        if (businessNameField != null) {
            businessNameField.setDisable(!isBusinessUser);
            if (isBusinessUser) {
                businessNameField.setPromptText("Enter business name *");
                businessNameField.setStyle("-fx-border-color: #BDC3C7;");
            } else {
                businessNameField.setPromptText("Enter business name (optional)");
                businessNameField.setStyle("");
            }
        }
        
        if (businessTinField != null) {
            businessTinField.setDisable(!isBusinessUser);
            if (isBusinessUser) {
                businessTinField.setPromptText("Enter business TIN *");
                businessTinField.setStyle("-fx-border-color: #BDC3C7;");
            } else {
                businessTinField.setPromptText("Enter business TIN (optional)");
                businessTinField.setStyle("");
            }
        }
    }

    // ==================== DATABASE METHODS ====================

    private boolean saveUserToDatabase() {
        System.out.println("Saving user to database...");
        
        // Check if connection is valid
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("Connection closed, reconnecting...");
                initializeDatabaseConnection();
            }
        } catch (SQLException e) {
            System.err.println("Error checking connection: " + e.getMessage());
            initializeDatabaseConnection();
        }
        
        // SIMULATED DATABASE SAVE FOR TESTING
        // Comment this out and uncomment the real database code below when ready
        try {
            Thread.sleep(1000); // Simulate database delay
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("✓ User saved (simulated for testing)");
        return true;
        
        /*
        // REAL DATABASE CODE (comment out the simulation code above when using this)
        String sql = "INSERT INTO users (first_name, last_name, gender, email, phone_number, " +
                    "date_of_birth, address, business_name, username, taxpayer_id, password_hash, " +
                    "user_type, business_tin, subscribed_to_newsletter, agreed_to_terms, " +
                    "agreed_to_privacy, language_preference, account_status, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Personal Information
            pstmt.setString(1, firstNameField.getText().trim());
            pstmt.setString(2, lastNameField.getText().trim());
            pstmt.setString(3, genderComboBox.getValue());
            pstmt.setString(4, emailField.getText().trim());
            pstmt.setString(5, phoneField.getText().trim());
            
            // Handle date of birth
            if (dobDatePicker.getValue() != null) {
                pstmt.setDate(6, Date.valueOf(dobDatePicker.getValue()));
            } else {
                pstmt.setDate(6, null);
            }
            
            pstmt.setString(7, addressField.getText().trim());
            pstmt.setString(8, businessNameField.getText().trim());
            
            // Account Information
            pstmt.setString(9, usernameField.getText().trim());
            pstmt.setString(10, taxpayerIdField.getText().trim());
            pstmt.setString(11, hashPassword(passwordField.getText()));
            pstmt.setString(12, userTypeComboBox.getValue());
            pstmt.setString(13, businessTinField.getText().trim());
            
            // Preferences and Agreements
            pstmt.setBoolean(14, newsletterCheckBox.isSelected());
            pstmt.setBoolean(15, termsCheckBox.isSelected());
            pstmt.setBoolean(16, privacyCheckBox.isSelected());
            pstmt.setString(17, languageComboBox.getValue());
            pstmt.setString(18, "ACTIVE");
            
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✓ User saved successfully");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Database error: " + e.getMessage());
            showAlert("Database Error", 
                     "Failed to save user data: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        
        return false;
        */
    }

    private String hashPassword(String password) {
        // Simple hash for testing
        return Integer.toString(password.hashCode());
    }

    // ==================== HELPER METHODS ====================

    private void clearForm() {
        System.out.println("Clearing form...");
        
        // Clear personal information
        if (firstNameField != null) firstNameField.clear();
        if (lastNameField != null) lastNameField.clear();
        if (genderComboBox != null) genderComboBox.setValue("Male");
        if (dobDatePicker != null) dobDatePicker.setValue(null);
        if (emailField != null) emailField.clear();
        if (phoneField != null) phoneField.clear();
        if (businessNameField != null) businessNameField.clear();
        if (addressField != null) addressField.clear();
        
        // Clear account information
        if (usernameField != null) usernameField.clear();
        if (taxpayerIdField != null) taxpayerIdField.clear();
        if (passwordField != null) passwordField.clear();
        if (confirmPasswordField != null) confirmPasswordField.clear();
        if (businessTinField != null) businessTinField.clear();
        
        // Reset checkboxes
        if (termsCheckBox != null) termsCheckBox.setSelected(false);
        if (privacyCheckBox != null) privacyCheckBox.setSelected(false);
        if (newsletterCheckBox != null) newsletterCheckBox.setSelected(false);
        
        // Reset comboboxes
        if (languageComboBox != null) languageComboBox.setValue("English");
        if (userTypeComboBox != null) userTypeComboBox.setValue("Individual Taxpayer");
        
        // Reset password visibility
        if (isPasswordVisible && showPasswordBtn != null) {
            showPasswordBtn.setText("👁");
            isPasswordVisible = false;
        }
        
        // Reset field styles
        resetFieldStyles();
        
        // Update business fields visibility
        updateBusinessFieldsVisibility();
        
        System.out.println("✓ Form cleared successfully.");
    }

    private void resetFieldStyles() {
        TextField[] allFields = {
            firstNameField, lastNameField, emailField, phoneField,
            usernameField, taxpayerIdField, passwordField,
            confirmPasswordField, businessNameField, businessTinField
        };
        
        for (TextField field : allFields) {
            if (field != null) {
                field.setStyle("");
            }
        }
    }

    private void navigateToLoginPage() {
        if (isNavigating) {
            return; // Prevent multiple navigation attempts
        }
        
        isNavigating = true;
        System.out.println("Navigating to login page...");
        
        try {
            // Get the current stage
            Stage currentStage = null;
            
            if (signUpButton != null && signUpButton.getScene() != null) {
                currentStage = (Stage) signUpButton.getScene().getWindow();
            } else if (cancelButton != null && cancelButton.getScene() != null) {
                currentStage = (Stage) cancelButton.getScene().getWindow();
            }
            
            if (currentStage == null) {
                showAlert("Navigation Error", "Cannot find current window", Alert.AlertType.ERROR);
                isNavigating = false;
                return;
            }
            
            // Try to load login page
            FXMLLoader loader = null;
            Parent root = null;
            
            // Try different paths
            String[] possiblePaths = {
                "/TMSFXML/Login.fxml",
                "/FXML/Login.fxml", 
                "/Login.fxml",
                "Login.fxml",
                "TMSFXML/Login.fxml",
                "FXML/Login.fxml"
            };
            
            for (String path : possiblePaths) {
                try {
                    System.out.println("Trying to load: " + path);
                    loader = new FXMLLoader(getClass().getResource(path));
                    root = loader.load();
                    System.out.println("Successfully loaded: " + path);
                    break;
                } catch (Exception e) {
                    System.out.println("Failed to load " + path + ": " + e.getMessage());
                }
            }
            
            if (root == null) {
                showAlert("Navigation Error", 
                         "Cannot find Login.fxml file.", 
                         Alert.AlertType.ERROR);
                isNavigating = false;
                return;
            }
            
            // Close database connection before navigation
            closeConnection();
            
            // Create new scene
            Scene scene = new Scene(root);
            currentStage.setScene(scene);
            currentStage.setTitle("Login - Tax Management System");
            currentStage.show();
            
            System.out.println("✓ Navigated to login page");
            
        } catch (Exception e) {
            System.err.println("✗ Failed to navigate to login page: " + e.getMessage());
            showAlert("Navigation Error", 
                     "Failed to navigate to login page: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
            e.printStackTrace();
            isNavigating = false;
        }
    }

    private void showTermsDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Terms and Conditions");
        alert.setHeaderText("Ethiopian Revenue Service - Terms and Conditions");
        alert.setContentText("By creating an account, you agree to:\n\n" +
                           "1. Provide accurate and truthful information\n" +
                           "2. Comply with Ethiopian tax laws and regulations\n" +
                           "3. Keep your login credentials secure\n" +
                           "4. Accept official communications from ERS\n" +
                           "5. Update your information when changes occur\n\n" +
                           "Violation of terms may result in account suspension.");
        alert.showAndWait();
    }

    private void showPrivacyDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Privacy Policy");
        alert.setHeaderText("Ethiopian Revenue Service - Privacy Policy");
        alert.setContentText("Your privacy is protected under Ethiopian law:\n\n" +
                           "1. We collect only necessary information for tax administration\n" +
                           "2. Your data is stored securely and confidentially\n" +
                           "3. Information is shared only as required by law\n" +
                           "4. You have the right to access and correct your data\n" +
                           "5. We implement industry-standard security measures");
        alert.showAndWait();
    }

    private void showSupportDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Support");
        alert.setHeaderText("Ethiopian Revenue Service - Support");
        alert.setContentText("For assistance, please contact:\n\n" +
                           "• Email: support@ers.gov.et\n" +
                           "• Phone: +251-11-123-4567\n" +
                           "• Office Hours: 8:30 AM - 5:30 PM (Monday-Friday)\n" +
                           "• Address: Addis Ababa, Ethiopia");
        alert.showAndWait();
    }

    private void showAlert(String title, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Clean up database connection
    private void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
    
    // Make sure connection is closed when controller is no longer needed
    public void shutdown() {
        closeConnection();
    }
}