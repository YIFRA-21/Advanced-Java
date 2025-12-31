package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class LoginController {

    // Database connection
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    private Connection connection;
    
    // Application settings
    private static final String APP_NAME = "ERS 7.0";
    private static final String APP_VERSION = "3.1.0";
    private static final String COPYRIGHT = "© 2025 Ethiopian Revenue Service";
    
    // User preferences
    private Preferences prefs;
    private static final String PREFS_NODE = "com.ers.taxsystem";
    
    // Language support
    public enum Language {
        ENGLISH("English", "en", "US"),
        AMHARIC("አማርኛ", "am", "ET"),
        OROMO("Afaan Oromoo", "om", "ET"),
        TIGRIGNA("ትግርኛ", "ti", "ET"),
        SOMALI("Soomaali", "so", "ET");
        
        private final String displayName;
        private final String code;
        private final String country;
        
        Language(String displayName, String code, String country) {
            this.displayName = displayName;
            this.code = code;
            this.country = country;
        }
        
        public String getDisplayName() { return displayName; }
        public String getCode() { return code; }
        public String getCountry() { return country; }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    // Security
    private SecureRandom random = new SecureRandom();
    private int loginAttempts = 0;
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private String lockedUntil = null;
    
    // FXML Elements - updated to match FXML camelCase
    @FXML private StackPane root;
    @FXML private Rectangle pattern;
    @FXML private VBox loginCard;
    @FXML private VBox logoInformation;
    @FXML private VBox selectLanguage;
    @FXML private ComboBox<Language> otherLanguagesCombo;
    @FXML private VBox formBox;
    @FXML private VBox usernameInformation;
    @FXML private Label usernameLabel;
    @FXML private TextField usernameField;
    @FXML private VBox passwordInformation;
    @FXML private Label passwordLabel;
    @FXML private HBox passwordFieldContainer;
    @FXML private PasswordField hiddenPasswordField;
    @FXML private TextField visiblePasswordField;
    @FXML private Button showPasswordBtn;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button loginBtn;
    @FXML private Hyperlink signupLink;

    @FXML
    public void initialize() {
        try {
            System.out.println("[LoginController] Initialization started");
            
            // Check if FXML elements are injected
            checkFXMLInjection();
            
            // Setup UI first
            setupUIComponents();
            
            // Then load preferences
            loadSavedPreferences();
            
            // Setup event handlers
            setupEventHandlers();
            
            // Setup keyboard shortcuts
            setupKeyboardShortcuts();
            
            // Try to initialize database (but don't block UI)
            initializeDatabaseAsync();
            
            System.out.println("[LoginController] Initialization completed successfully");
            
        } catch (Exception e) {
            System.err.println("[LoginController] Initialization error: " + e.getMessage());
            e.printStackTrace();
            showAlert("Initialization Error", "Application initialized with errors: " + e.getMessage());
        }
    }
    
    private void checkFXMLInjection() {
        System.out.println("[LoginController] Checking FXML injection...");
        
        // Log which elements are injected
        System.out.println("  - root: " + (root != null ? "✓" : "✗"));
        System.out.println("  - usernameField: " + (usernameField != null ? "✓" : "✗"));
        System.out.println("  - hiddenPasswordField: " + (hiddenPasswordField != null ? "✓" : "✗"));
        System.out.println("  - loginBtn: " + (loginBtn != null ? "✓" : "✗"));
        System.out.println("  - otherLanguagesCombo: " + (otherLanguagesCombo != null ? "✓" : "✗"));
        System.out.println("  - logoInformation: " + (logoInformation != null ? "✓" : "✗"));
        System.out.println("  - selectLanguage: " + (selectLanguage != null ? "✓" : "✗"));
        
        // Critical elements check
        if (root == null) {
            throw new IllegalStateException("Root StackPane is not injected from FXML!");
        }
    }
    
    private void initializeDatabaseAsync() {
        new Thread(() -> {
            try {
                initializeDatabaseConnection();
            } catch (SQLException e) {
                System.err.println("[LoginController] Database connection failed: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    // Show non-blocking error
                    System.err.println("Database connection failed. Running in offline mode.");
                });
            }
        }).start();
    }
    
    private void initializeDatabaseConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("[LoginController] Database connection established");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found: " + e.getMessage());
        }
    }
    
    private void setupUIComponents() {
        System.out.println("[LoginController] Setting up UI components...");
        
        // Setup pattern decoration
        if (pattern != null) {
            pattern.setFill(Color.web("#2E7D32"));
            pattern.setOpacity(0.1);
        }
        
        // Setup language selector
        setupLanguageSelector();
        
        // Setup form styling
        setupFormStyling();
        
        // Setup password visibility toggle
        setupPasswordVisibility();
        
        // Apply initial language
        applyCurrentLanguage();
        
        System.out.println("[LoginController] UI components setup completed");
    }
    
    private void setupLanguageSelector() {
        System.out.println("[LoginController] Setting up language selector...");
        
        if (otherLanguagesCombo != null) {
            // Clear existing items
            otherLanguagesCombo.getItems().clear();
            
            // Add all languages
            otherLanguagesCombo.getItems().addAll(Language.values());
            otherLanguagesCombo.setPromptText("Select Language");
            
            // Set default language
            Language defaultLang = Language.ENGLISH;
            otherLanguagesCombo.setValue(defaultLang);
            
            System.out.println("[LoginController] Language selector initialized");
        } else {
            System.err.println("[LoginController] otherLanguagesCombo is null!");
        }
    }
    
    private void setupFormStyling() {
        System.out.println("[LoginController] Setting up form styling...");
        
        // Setup field validation
        setupFieldValidation();
    }
    
    private void setupFieldValidation() {
        if (usernameField != null) {
            usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
                validateUsername(newValue);
            });
        }
        
        if (hiddenPasswordField != null) {
            hiddenPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
                validatePasswordStrength(newValue);
            });
        }
    }
    
    private void validateUsername(String username) {
        if (usernameField == null) return;
        
        if (username == null || username.trim().isEmpty()) {
            usernameField.setStyle("-fx-border-color: #ccc;");
            return;
        }
        
        if (Pattern.matches("^[a-zA-Z0-9._-]{3,50}$", username)) {
            usernameField.setStyle("-fx-border-color: #4CAF50;");
        } else {
            usernameField.setStyle("-fx-border-color: #F44336;");
        }
    }
    
    private void validatePasswordStrength(String password) {
        if (password == null || password.isEmpty() || passwordLabel == null) {
            return;
        }
        
        int strength = calculatePasswordStrength(password);
        String color;
        
        switch (strength) {
            case 5: color = "#4CAF50"; break;
            case 4: color = "#8BC34A"; break;
            case 3: color = "#FFC107"; break;
            case 2: color = "#FF9800"; break;
            default: color = "#F44336";
        }
        
        passwordLabel.setStyle("-fx-text-fill: " + color + ";");
    }
    
    private int calculatePasswordStrength(String password) {
        int strength = 0;
        
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) strength++;
        
        return Math.min(strength, 5);
    }
    
    private void setupPasswordVisibility() {
        System.out.println("[LoginController] Setting up password visibility...");
        
        if (visiblePasswordField != null && hiddenPasswordField != null) {
            // Initially hide visible password field
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            
            // Bind password fields
            visiblePasswordField.textProperty().bindBidirectional(hiddenPasswordField.textProperty());
            
            System.out.println("[LoginController] Password visibility setup completed");
        } else {
            System.err.println("[LoginController] Password fields are null!");
        }
    }
    
    private void loadSavedPreferences() {
        try {
            System.out.println("[LoginController] Loading saved preferences...");
            
            // Initialize preferences
            prefs = Preferences.userRoot().node(PREFS_NODE);
            
            // Load saved username
            String savedUsername = prefs.get("username", "");
            if (!savedUsername.isEmpty() && usernameField != null) {
                usernameField.setText(savedUsername);
                System.out.println("[LoginController] Loaded saved username: " + savedUsername);
            }
            
            // Load remember me setting
            boolean rememberMe = prefs.getBoolean("rememberMe", false);
            if (rememberMeCheck != null) {
                rememberMeCheck.setSelected(rememberMe);
                System.out.println("[LoginController] Remember me: " + rememberMe);
            }
            
            // Load saved password if remember me is checked
            if (rememberMe && hiddenPasswordField != null) {
                String encryptedPassword = prefs.get("password", "");
                if (!encryptedPassword.isEmpty()) {
                    String decryptedPassword = decryptPassword(encryptedPassword);
                    hiddenPasswordField.setText(decryptedPassword);
                    System.out.println("[LoginController] Loaded saved password");
                }
            }
            
            // Load language preference
            String langCode = prefs.get("language", "en");
            Language savedLang = Language.ENGLISH;
            for (Language lang : Language.values()) {
                if (lang.getCode().equals(langCode)) {
                    savedLang = lang;
                    break;
                }
            }
            
            if (otherLanguagesCombo != null) {
                otherLanguagesCombo.setValue(savedLang);
                System.out.println("[LoginController] Loaded language: " + savedLang.getDisplayName());
            }
            
            System.out.println("[LoginController] Preferences loaded successfully");
            
        } catch (Exception e) {
            System.err.println("[LoginController] Error loading preferences: " + e.getMessage());
        }
    }
    
    private void savePreferences() {
        try {
            // Save username if remember me is checked
            if (rememberMeCheck != null && rememberMeCheck.isSelected() && usernameField != null) {
                prefs.put("username", usernameField.getText());
                System.out.println("[LoginController] Saved username: " + usernameField.getText());
                
                // Encrypt and save password
                String password = hiddenPasswordField != null ? hiddenPasswordField.getText() : "";
                if (!password.isEmpty()) {
                    String encryptedPassword = encryptPassword(password);
                    prefs.put("password", encryptedPassword);
                    System.out.println("[LoginController] Saved password (encrypted)");
                }
            } else {
                // Clear saved credentials
                prefs.remove("username");
                prefs.remove("password");
                System.out.println("[LoginController] Cleared saved credentials");
            }
            
            // Save remember me setting
            if (rememberMeCheck != null) {
                prefs.putBoolean("rememberMe", rememberMeCheck.isSelected());
            }
            
            // Save language preference
            if (otherLanguagesCombo != null) {
                Language currentLang = otherLanguagesCombo.getValue();
                if (currentLang != null) {
                    prefs.put("language", currentLang.getCode());
                    System.out.println("[LoginController] Saved language: " + currentLang.getDisplayName());
                }
            }
            
        } catch (Exception e) {
            System.err.println("[LoginController] Error saving preferences: " + e.getMessage());
        }
    }
    
    private String encryptPassword(String password) {
        try {
            char[] key = {'E', 'R', 'S', '7', '.', '0'};
            char[] chars = password.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                chars[i] = (char) (chars[i] ^ key[i % key.length]);
            }
            return Base64.getEncoder().encodeToString(new String(chars).getBytes());
        } catch (Exception e) {
            return password;
        }
    }
    
    private String decryptPassword(String encrypted) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            String password = new String(decoded);
            char[] key = {'E', 'R', 'S', '7', '.', '0'};
            char[] chars = password.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                chars[i] = (char) (chars[i] ^ key[i % key.length]);
            }
            return new String(chars);
        } catch (Exception e) {
            return encrypted;
        }
    }
    
    private void setupEventHandlers() {
        System.out.println("[LoginController] Setting up event handlers...");
        
        // Setup language combo listener
        if (otherLanguagesCombo != null) {
            otherLanguagesCombo.setOnAction(this::handleLanguageSelection);
        }
        
        System.out.println("[LoginController] Event handlers setup completed");
    }
    
    private void setupKeyboardShortcuts() {
        if (root != null) {
            root.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    System.out.println("[LoginController] Enter key pressed - attempting login");
                    handleLogin(null);
                }
            });
        }
    }
    
    // ==================== FXML EVENT HANDLERS ====================
    // These must match the onAction="#methodName" in FXML
    
    @FXML
    private void handleLanguageSelection(ActionEvent event) {
        System.out.println("[LoginController] Language selected");
        Language selectedLang = otherLanguagesCombo.getValue();
        if (selectedLang != null) {
            changeApplicationLanguage(selectedLang);
        }
    }
    
    @FXML
    private void handleShowPassword(ActionEvent event) {
        System.out.println("[LoginController] Show password button clicked");
        togglePasswordVisibility();
    }
    
    @FXML
    private void handleRememberMe(ActionEvent event) {
        System.out.println("[LoginController] Remember me checkbox clicked");
        savePreferences();
    }
    
    
    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            System.out.println("[LoginController] Opening Forgot Password window...");
            
            // Close login window first
            Stage currentStage = (Stage) loginCard.getScene().getWindow();
            currentStage.close();
            
            // Load ForgotPassword FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/ForgotPassword.fxml"));
            Parent root = loader.load();
            
            // Create new stage
            Stage forgotStage = new Stage();
            forgotStage.setTitle("Forgot Password - Ethiopian Revenue Service");
            forgotStage.setScene(new Scene(root, 600, 700));
            forgotStage.initModality(Modality.APPLICATION_MODAL);
            
            // Set close handler to reopen login
            forgotStage.setOnCloseRequest(e -> {
                try {
                    FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/TMSFXML/Login.fxml"));
                    Parent loginRoot = loginLoader.load();
                    Stage loginStage = new Stage();
                    loginStage.setScene(new Scene(loginRoot));
                    loginStage.setTitle("Login - Ethiopian Revenue Service");
                    loginStage.show();
                } catch (Exception ex) {
                    System.err.println("[LoginController] Error reopening login: " + ex.getMessage());
                }
            });
            
            forgotStage.show();
            
        } catch (IOException e) {
            System.err.println("[LoginController] CRITICAL: Failed to load ForgotPassword.fxml");
            e.printStackTrace();
            
            // Show error alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Loading Error");
            alert.setHeaderText("Cannot Load Password Reset Form");
            alert.setContentText("The forgot password form could not be loaded. Please check if the file exists at: \n/TMSFXML/ForgotPassword.fxml");
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("[LoginController] Error opening forgot password window: " + e.getMessage());
            e.printStackTrace();
        }
    }
        
    
    
    @FXML
    private void handleLogin(ActionEvent event) {
        System.out.println("[LoginController] === Login Attempt ===");
        
        String username = usernameField != null ? usernameField.getText().trim() : "";
        String password = hiddenPasswordField != null ? hiddenPasswordField.getText() : "";
        
        System.out.println("[LoginController] Username: " + (username.isEmpty() ? "[empty]" : username));
        System.out.println("[LoginController] Password: " + (password.isEmpty() ? "[empty]" : "[entered]"));
        
        // Validate inputs
        if (!validateLoginInputs(username, password)) {
            return;
        }
        
        // Save preferences
        savePreferences();
        
        // For testing: simulate successful login
        simulateLogin(username, password);
    }
    
    @FXML
    private void handleSignUp(ActionEvent event) {
        System.out.println("[LoginController] Sign up link clicked");
        showAlert("Sign Up", "Please contact your system administrator to create a new account.");
    
            try {
                System.out.println("[LoginController] Opening Sign Up window...");
                
                // Load the Sign Up FXML
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/SignUp.fxml"));
                Parent signUpRoot = loader.load();
                
                // Create a new stage for the sign up window
                Stage signUpStage = new Stage();
                signUpStage.setTitle("Create New Account");
                signUpStage.setScene(new Scene(signUpRoot));
                signUpStage.initModality(Modality.APPLICATION_MODAL);
                signUpStage.initOwner(root.getScene().getWindow());
                
                // Set window properties
                signUpStage.setResizable(false);
                signUpStage.show();
                
                System.out.println("[LoginController] Sign Up window opened successfully");
                
            } catch (IOException e) {
                System.err.println("[LoginController] Error loading Sign Up window: " + e.getMessage());
                showAlert("Error", "Cannot load sign up page. Please check if SignUp.fxml exists.");
            } catch (Exception e) {
                System.err.println("[LoginController] Unexpected error: " + e.getMessage());
                showAlert("Error", "An unexpected error occurred: " + e.getMessage());
            }
        }
           
    
    
    // ==================== HELPER METHODS ====================
    
    private boolean validateLoginInputs(String username, String password) {
        // Check if fields are empty
        if (username.isEmpty()) {
            showAlert("Input Required", "Please enter your username.");
            if (usernameField != null) usernameField.requestFocus();
            return false;
        }
        
        if (password.isEmpty()) {
            showAlert("Input Required", "Please enter your password.");
            if (hiddenPasswordField != null) hiddenPasswordField.requestFocus();
            return false;
        }
        
        // Validate username format
        if (!Pattern.matches("^[a-zA-Z0-9._-]{3,50}$", username)) {
            showAlert("Invalid Username", 
                "Username must be 3-50 characters and can only contain:\n" +
                "• Letters (a-z, A-Z)\n" +
                "• Numbers (0-9)\n" +
                "• Special characters (._-)");
            if (usernameField != null) usernameField.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void simulateLogin(String username, String password) {
        System.out.println("[LoginController] Simulating login for: " + username);
        
        // Show success message
        showAlert("Login Successful", 
            "Welcome, " + username + "!\n\n" +
            "Redirecting to dashboard...");
        
        // Navigate to dashboard after a delay
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 2 second delay
                javafx.application.Platform.runLater(() -> {
                    navigateToDashboard();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
   
    private void navigateToDashboard() {
        try {
            System.out.println("[LoginController] Navigating to dashboard...");
            
            // Close database connection if open
            cleanup();
            
            // Load dashboard with proper path
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Dashboard.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) (loginBtn != null ? loginBtn.getScene().getWindow() : this.root.getScene().getWindow());
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Tax Management System - Dashboard");
            stage.setMaximized(true);
            stage.show();
            
            System.out.println("[LoginController] Dashboard loaded successfully");
            
        } catch (IOException e) {
            System.err.println("[LoginController] Error loading dashboard: " + e.getMessage());
            e.printStackTrace();
            showAlert("Navigation Error", "Cannot load dashboard. Please check if Dashboard.fxml exists.");
        } catch (Exception e) {
            System.err.println("[LoginController] Unexpected error: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }
    
    private void togglePasswordVisibility() {
        if (hiddenPasswordField == null || visiblePasswordField == null || showPasswordBtn == null) {
            return;
        }
        
        boolean isPasswordHidden = hiddenPasswordField.isVisible();
        
        if (isPasswordHidden) {
            // Show password
            hiddenPasswordField.setVisible(false);
            hiddenPasswordField.setManaged(false);
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
            showPasswordBtn.setText("🙈");
            visiblePasswordField.requestFocus();
            System.out.println("[LoginController] Password shown");
        } else {
            // Hide password
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            hiddenPasswordField.setVisible(true);
            hiddenPasswordField.setManaged(true);
            showPasswordBtn.setText("👁");
            hiddenPasswordField.requestFocus();
            System.out.println("[LoginController] Password hidden");
        }
    }
    
    private void changeApplicationLanguage(Language language) {
        System.out.println("[LoginController] Changing language to: " + language.getDisplayName());
        
        // Change locale
        Locale locale = new Locale(language.getCode(), language.getCountry());
        Locale.setDefault(locale);
        
        // Update UI elements with translations
        updateUITexts(language);
        
        // Save language preference
        saveLanguagePreference(language);
        
        System.out.println("[LoginController] Language changed successfully");
    }
    
    private void applyCurrentLanguage() {
        if (otherLanguagesCombo != null) {
            Language currentLang = otherLanguagesCombo.getValue();
            if (currentLang != null) {
                updateUITexts(currentLang);
            }
        }
    }
    
    private void updateUITexts(Language language) {
        System.out.println("[LoginController] Updating UI texts for language: " + language.getDisplayName());
        
        // Update form labels based on language
        switch (language) {
            case AMHARIC:
                if (usernameLabel != null) usernameLabel.setText("የተጠቃሚ ስም:");
                if (usernameField != null) usernameField.setPromptText("የተጠቃሚ ስም ያስገቡ");
                if (passwordLabel != null) passwordLabel.setText("የይለፍ ቃል:");
                if (hiddenPasswordField != null) hiddenPasswordField.setPromptText("የይለፍ ቃል ያስገቡ");
                if (visiblePasswordField != null) visiblePasswordField.setPromptText("የይለፍ ቃል ያስገቡ");
                if (rememberMeCheck != null) rememberMeCheck.setText("አስታውሰኝ");
                if (forgotPasswordLink != null) forgotPasswordLink.setText("የይለፍ ቃል ረሳሁ?");
                if (loginBtn != null) loginBtn.setText("ግባ");
                if (signupLink != null) signupLink.setText("መለያ የለህም? ይመዝገብ");
                break;
                
            default: // English
                if (usernameLabel != null) usernameLabel.setText("Username:");
                if (usernameField != null) usernameField.setPromptText("Enter username");
                if (passwordLabel != null) passwordLabel.setText("Password:");
                if (hiddenPasswordField != null) hiddenPasswordField.setPromptText("Enter password");
                if (visiblePasswordField != null) visiblePasswordField.setPromptText("Enter password");
                if (rememberMeCheck != null) rememberMeCheck.setText("Remember me");
                if (forgotPasswordLink != null) forgotPasswordLink.setText("Forgot password?");
                if (loginBtn != null) loginBtn.setText("Sign In");
                if (signupLink != null) signupLink.setText("Don't have an account? Sign Up");
                break;
        }
        
        System.out.println("[LoginController] UI texts updated");
    }
    
    private void saveLanguagePreference(Language language) {
        if (prefs != null) {
            prefs.put("language", language.getCode());
            System.out.println("[LoginController] Language preference saved: " + language.getDisplayName());
        }
    }
    
    private void showAlert(String title, String message) {
        try {
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.showAndWait();
            });
        } catch (Exception e) {
            System.err.println("[LoginController] Error showing alert: " + e.getMessage());
        }
    }
    
    public void cleanup() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[LoginController] Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("[LoginController] Error closing connection: " + e.getMessage());
        }
    }
}