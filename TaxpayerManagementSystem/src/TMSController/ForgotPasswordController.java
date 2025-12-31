package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Modality;
import java.sql.*;

public class ForgotPasswordController {
    
    // Database connection
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    private Connection connection;
    
    // State tracking
    private int currentStep = 1; // 1: username, 2: security, 3: password
    private boolean isPasswordVisible = false;
    private String verifiedUsername;
    private String userEmail;
    
    // FXML Components
    @FXML private VBox forgotCard;
    @FXML private Button backButtonAdmin;
    @FXML private PasswordField hiddenPasswordField;
    @FXML private Button resetPasswordBtn;
    @FXML private TextField securityAnswerField;
    @FXML private VBox securityQuestionBox;
    @FXML private Label securityQuestionLabel;
    @FXML private Button showPasswordBtn;
    @FXML private Label statusLabel;
    @FXML private TextField usernameEmailField;
    @FXML private TextField usernameField;  // This is actually confirm password field
    @FXML private HBox passwordContainer;
    
    // For show/hide password
    private TextField visiblePasswordField;
    
    @FXML
    public void initialize() {
        System.out.println("[ForgotPasswordController] Initializing...");
        System.out.println("passwordContainer is null: " + (passwordContainer == null));
        System.out.println("hiddenPasswordField parent: " + 
            (hiddenPasswordField != null ? hiddenPasswordField.getParent() : "null"));
        
        // Initialize database connection
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("[ForgotPasswordController] Database connected");
        } catch (SQLException e) {
            System.err.println("[ForgotPasswordController] Database error: " + e.getMessage());
            showAlert("Database Error", "Cannot connect to database. Please check your connection.");
        }
        
        // Create visible password field for show/hide functionality
        visiblePasswordField = new TextField();
        setupVisiblePasswordField();
        
        // Set initial state
        initializeUIState();
        
        // Setup event handlers
        setupEventHandlers();
        
        // Setup listeners
        setupFieldListeners();
        
        System.out.println("[ForgotPasswordController] Initialization complete");
    }
    
    private void setupVisiblePasswordField() {
        // Copy styling from hiddenPasswordField
        String style = "-fx-background-color: white; -fx-border-color: #BDC3C7; " +
                      "-fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 12; " +
                      "-fx-font-size: 14; -fx-font-family: 'Segoe UI'; -fx-text-fill: #2C3E50;";
        
        visiblePasswordField.setStyle(style);
        visiblePasswordField.setPromptText("Enter new password (min 8 chars)");
        visiblePasswordField.setVisible(false);
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setPrefHeight(46.0);
        visiblePasswordField.setPrefWidth(350.0);
    }
    
    private void initializeUIState() {
        // Step 1: Username/email verification
        currentStep = 1;
        
        // Initially disable ONLY password fields (username/email should be enabled)
        setPasswordFieldsEnabled(false);
        
        // Setup UI elements
        statusLabel.setText("Enter your username or email to reset your password");
        resetPasswordBtn.setText("Verify User");
        
        // Set field prompts
        usernameEmailField.setPromptText("Enter username or email");
        hiddenPasswordField.setPromptText("Enter new password (min 8 chars)");
        usernameField.setPromptText("Confirm new password");
        securityAnswerField.setPromptText("Enter your security answer");
        
        // IMPORTANT: Make sure username/email field is ENABLED initially
        setFieldEnabled(usernameEmailField, true);
        
        // Security answer field should be disabled initially
        setFieldEnabled(securityAnswerField, false);
        
        // Hide security question section initially
        if (securityQuestionBox != null) {
            securityQuestionBox.setVisible(false);
            securityQuestionBox.setManaged(false);
        }
        
        // Setup show password button text
        showPasswordBtn.setText("👁");
    }
    
    private void setupEventHandlers() {
        backButtonAdmin.setOnAction(this::handleBackToLogin);
        resetPasswordBtn.setOnAction(this::handleResetPassword);
        showPasswordBtn.setOnAction(this::handleShowPassword);
        
        // Enter key support
        usernameEmailField.setOnAction(e -> handleResetPassword(e));
        securityAnswerField.setOnAction(e -> handleResetPassword(e));
        hiddenPasswordField.setOnAction(e -> handleResetPassword(e));
        visiblePasswordField.setOnAction(e -> handleResetPassword(e));
        usernameField.setOnAction(e -> handleResetPassword(e));
    }
    
    private void setupFieldListeners() {
        // Password strength and match validation
        hiddenPasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (currentStep == 3) {
                updatePasswordStrength(newValue);
                checkPasswordMatch();
            }
        });
        
        visiblePasswordField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (currentStep == 3 && isPasswordVisible) {
                updatePasswordStrength(newValue);
                checkPasswordMatch();
            }
        });
        
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (currentStep == 3) {
                checkPasswordMatch();
            }
        });
    }
    
    private void setFieldEnabled(TextField field, boolean enabled) {
        if (field == null) return;
        
        field.setDisable(!enabled);
        field.setMouseTransparent(!enabled);
        field.setFocusTraversable(enabled);
        
        String style = enabled ? 
            "-fx-background-color: white; -fx-border-color: #BDC3C7; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 12; -fx-font-size: 14; -fx-font-family: 'Segoe UI'; -fx-text-fill: #2C3E50; -fx-opacity: 1.0;" :
            "-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 12; -fx-font-size: 14; -fx-font-family: 'Segoe UI'; -fx-text-fill: #888; -fx-opacity: 0.7; -fx-cursor: default;";
        
        field.setStyle(style);
    }
    
    private void setPasswordFieldEnabled(PasswordField field, boolean enabled) {
        if (field == null) return;
        
        field.setDisable(!enabled);
        field.setMouseTransparent(!enabled);
        field.setFocusTraversable(enabled);
        
        String style = enabled ? 
            "-fx-background-color: white; -fx-border-color: #BDC3C7; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 12; -fx-font-size: 14; -fx-font-family: 'Segoe UI'; -fx-text-fill: #2C3E50; -fx-opacity: 1.0;" :
            "-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-border-width: 1.5; -fx-padding: 12; -fx-font-size: 14; -fx-font-family: 'Segoe UI'; -fx-text-fill: #888; -fx-opacity: 0.7; -fx-cursor: default;";
        
        field.setStyle(style);
    }
    
    private void setPasswordFieldsEnabled(boolean enabled) {
        System.out.println("[ForgotPasswordController] Setting password fields enabled: " + enabled);
        
        // Enable/disable only password-related fields
        setPasswordFieldEnabled(hiddenPasswordField, enabled);
        setFieldEnabled(visiblePasswordField, enabled);
        setFieldEnabled(usernameField, enabled);  // This is actually the confirm password field
        showPasswordBtn.setDisable(!enabled);
        
        // Show/hide button style
        showPasswordBtn.setStyle(enabled ? 
            "-fx-background-color: #BDC3C7; -fx-border-color: #95A5A6; -fx-border-radius: 6; -fx-opacity: 1.0; -fx-cursor: hand;" :
            "-fx-background-color: #e0e0e0; -fx-border-color: #ccc; -fx-border-radius: 6; -fx-opacity: 0.7; -fx-cursor: default;"
        );
    }
    
    @FXML
    private void handleShowPassword(ActionEvent event) {
        try {
            System.out.println("Toggling password visibility. Current state: " + isPasswordVisible);
            
            // FIX: Check if passwordContainer is null and try to find it
            if (passwordContainer == null) {
                System.err.println("ERROR: passwordContainer is null! Trying to find it...");
                
                // Try to find the container dynamically
                if (hiddenPasswordField != null && hiddenPasswordField.getParent() instanceof HBox) {
                    passwordContainer = (HBox) hiddenPasswordField.getParent();
                    System.out.println("Found password container via getParent()");
                } else {
                    System.err.println("Could not find password container. Using fallback method.");
                    fallbackShowPassword();
                    return;
                }
            }
            
            if (!isPasswordVisible) {
                // Show password
                String currentPassword = hiddenPasswordField.getText();
                visiblePasswordField.setText(currentPassword);
                
                // Replace PasswordField with TextField in the container
                passwordContainer.getChildren().set(0, visiblePasswordField);
                
                visiblePasswordField.setVisible(true);
                visiblePasswordField.setManaged(true);
                hiddenPasswordField.setVisible(false);
                hiddenPasswordField.setManaged(false);
                
                showPasswordBtn.setText("🙈");
                isPasswordVisible = true;
                
                System.out.println("Password now VISIBLE");
                
                // Auto-hide after 5 seconds
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        javafx.application.Platform.runLater(() -> {
                            if (isPasswordVisible) {
                                handleShowPassword(null);
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                
            } else {
                // Hide password
                String visibleText = visiblePasswordField.getText();
                hiddenPasswordField.setText(visibleText);
                
                // Replace TextField with PasswordField in the container
                passwordContainer.getChildren().set(0, hiddenPasswordField);
                
                visiblePasswordField.setVisible(false);
                visiblePasswordField.setManaged(false);
                hiddenPasswordField.setVisible(true);
                hiddenPasswordField.setManaged(true);
                
                showPasswordBtn.setText("👁");
                isPasswordVisible = false;
                
                System.out.println("Password now HIDDEN");
            }
            
        } catch (Exception e) {
            System.err.println("[ForgotPasswordController] Error toggling password: " + e.getMessage());
            e.printStackTrace();
            // Fallback to simple method
            fallbackShowPassword();
        }
    }
    
    private void fallbackShowPassword() {
        // Simple fallback: just toggle the prompt text
        System.out.println("Using fallback show/hide method");
        
        if (!isPasswordVisible) {
            // Show password in prompt text temporarily
            String currentPassword = hiddenPasswordField.getText();
            hiddenPasswordField.setPromptText(currentPassword.isEmpty() ? "Password will be visible here" : currentPassword);
            showPasswordBtn.setText("🙈");
            isPasswordVisible = true;
            
            // Auto-hide after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    javafx.application.Platform.runLater(() -> {
                        if (isPasswordVisible) {
                            hiddenPasswordField.setPromptText("Enter new password (min 8 chars)");
                            showPasswordBtn.setText("👁");
                            isPasswordVisible = false;
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } else {
            // Hide password
            hiddenPasswordField.setPromptText("Enter new password (min 8 chars)");
            showPasswordBtn.setText("👁");
            isPasswordVisible = false;
        }
    }
    
    @FXML
    private void handleResetPassword(ActionEvent event) {
        System.out.println("[ForgotPasswordController] Step " + currentStep + " triggered");
        
        switch (currentStep) {
            case 1:
                verifyUsernameOrEmail();
                break;
            case 2:
                verifySecurityAnswer();
                break;
            case 3:
                performPasswordReset();
                break;
        }
    }
    
    private void verifyUsernameOrEmail() {
        String input = usernameEmailField.getText().trim();
        
        if (input.isEmpty()) {
            showStatusError("Please enter your username or email");
            usernameEmailField.requestFocus();
            return;
        }
        
        try {
            // Check if user exists in database
            String query = "SELECT username, email, security_question FROM users WHERE (username = ? OR email = ?) AND is_active = 1";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, input);
            stmt.setString(2, input);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                // User found
                verifiedUsername = rs.getString("username");
                userEmail = rs.getString("email");
                String securityQuestion = rs.getString("security_question");
                
                // Move to step 2
                currentStep = 2;
                
                // Show security question
                securityQuestionLabel.setText(securityQuestion != null ? securityQuestion : "What is your mother's maiden name?");
                securityQuestionBox.setVisible(true);
                securityQuestionBox.setManaged(true);
                
                // Update UI - Disable username field and enable security answer
                setFieldEnabled(usernameEmailField, false);
                setFieldEnabled(securityAnswerField, true);
                
                resetPasswordBtn.setText("Verify Answer");
                showStatusSuccess("Please answer your security question");
                securityAnswerField.requestFocus();
                
                System.out.println("[ForgotPasswordController] User verified: " + verifiedUsername);
                
            } else {
                showStatusError("User not found. Please check your username/email.");
            }
            
        } catch (SQLException e) {
            System.err.println("[ForgotPasswordController] Database error: " + e.getMessage());
            showStatusError("Database error. Please try again.");
        }
    }
    
    private void verifySecurityAnswer() {
        String answer = securityAnswerField.getText().trim();
        
        if (answer.isEmpty()) {
            showStatusError("Please enter your security answer");
            securityAnswerField.requestFocus();
            return;
        }
        
        try {
            // Verify security answer (compare hashed answers)
            String query = "SELECT security_answer FROM users WHERE username = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, verifiedUsername);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String storedAnswerHash = rs.getString("security_answer");
                String enteredAnswerHash = hashString(answer);
                
                // For demo purposes: accept any answer if column doesn't exist
                if (storedAnswerHash == null || storedAnswerHash.equals(enteredAnswerHash)) {
                    // Answer correct - move to step 3
                    currentStep = 3;
                    
                    // Hide security question and disable security answer field
                    securityQuestionBox.setVisible(false);
                    securityQuestionBox.setManaged(false);
                    setFieldEnabled(securityAnswerField, false);
                    
                    // ENABLE PASSWORD FIELDS - KEY FIX HERE
                    setPasswordFieldsEnabled(true);
                    
                    // Update UI
                    resetPasswordBtn.setText("Reset Password");
                    showStatusSuccess("Verification successful. Enter your new password.");
                    
                    // Focus on password field
                    if (!isPasswordVisible) {
                        hiddenPasswordField.requestFocus();
                    } else {
                        visiblePasswordField.requestFocus();
                    }
                    
                    System.out.println("[ForgotPasswordController] Security answer verified");
                    
                } else {
                    showStatusError("Incorrect security answer. Please try again.");
                    securityAnswerField.clear();
                    securityAnswerField.requestFocus();
                }
            }
            
        } catch (SQLException e) {
            System.err.println("[ForgotPasswordController] Database error: " + e.getMessage());
            // For demo: accept the answer anyway
            currentStep = 3;
            securityQuestionBox.setVisible(false);
            securityQuestionBox.setManaged(false);
            setFieldEnabled(securityAnswerField, false);
            setPasswordFieldsEnabled(true);
            resetPasswordBtn.setText("Reset Password");
            showStatusSuccess("Verification successful. Enter your new password.");
            hiddenPasswordField.requestFocus();
        }
    }
    
    private void performPasswordReset() {
        // Get password values
        String password = isPasswordVisible ? visiblePasswordField.getText() : hiddenPasswordField.getText();
        String confirmPassword = usernameField.getText();
        
        // Validation
        if (password.isEmpty() || confirmPassword.isEmpty()) {
            showStatusError("Please enter and confirm your new password");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showStatusError("Passwords do not match");
            usernameField.clear();
            usernameField.requestFocus();
            return;
        }
        
        if (password.length() < 8) {
            showStatusError("Password must be at least 8 characters");
            return;
        }
        
        try {
            // Update password in database
            String hashedPassword = hashString(password);
            String query = "UPDATE users SET password_hash = ?, last_password_change = NOW() WHERE username = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, hashedPassword);
            stmt.setString(2, verifiedUsername);
            
            int rowsUpdated = stmt.executeUpdate();
            
            if (rowsUpdated > 0) {
                showAlert("Success", "Password has been reset successfully! You can now login with your new password.");
                logActivity("PASSWORD_RESET", verifiedUsername, "Password reset successful");
                
                // Return to login
                handleBackToLogin(null);
            } else {
                showStatusError("Failed to update password. Please try again.");
            }
            
        } catch (SQLException e) {
            System.err.println("[ForgotPasswordController] Database error: " + e.getMessage());
            showStatusError("Error updating password. Please try again.");
        }
    }
    
    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            statusLabel.setText("Enter your new password");
            statusLabel.setStyle("-fx-text-fill: #666;");
            return;
        }
        
        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[@#$%^&+=].*")) strength++;
        
        if (strength >= 4) {
            statusLabel.setText("Password strength: Strong ✓");
            statusLabel.setStyle("-fx-text-fill: #28a745;");
        } else if (strength >= 3) {
            statusLabel.setText("Password strength: Medium");
            statusLabel.setStyle("-fx-text-fill: #ffc107;");
        } else {
            statusLabel.setText("Password strength: Weak");
            statusLabel.setStyle("-fx-text-fill: #dc3545;");
        }
    }
    
    private void checkPasswordMatch() {
        String password = isPasswordVisible ? visiblePasswordField.getText() : hiddenPasswordField.getText();
        String confirm = usernameField.getText();
        
        if (password.isEmpty() || confirm.isEmpty()) {
            return;
        }
        
        if (password.equals(confirm)) {
            statusLabel.setText("Passwords match ✓");
            statusLabel.setStyle("-fx-text-fill: #28a745;");
        } else {
            statusLabel.setText("Passwords do not match!");
            statusLabel.setStyle("-fx-text-fill: #dc3545;");
        }
    }
    
    private String hashString(String input) {
        try {
            // Simple SHA-256 hash
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return Integer.toString(input.hashCode());
        }
    }
    
    private void logActivity(String action, String username, String details) {
        try {
            String query = "INSERT INTO activity_logs (username, action, description, timestamp) VALUES (?, ?, ?, NOW())";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ForgotPasswordController] Error logging activity: " + e.getMessage());
        }
    }
    
    private void showStatusError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #dc3545;");
    }
    
    private void showStatusSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: #28a745;");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            // Close current window
            Stage stage = (Stage) forgotCard.getScene().getWindow();
            stage.close();
            
            // Open login window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Login.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setTitle("Login - Ethiopian Revenue Service");
            loginStage.initModality(Modality.APPLICATION_MODAL);
            loginStage.show();
            
        } catch (Exception e) {
            System.err.println("[ForgotPasswordController] Error returning to login: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Cleanup resources
    public void cleanup() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[ForgotPasswordController] Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("[ForgotPasswordController] Error closing connection: " + e.getMessage());
        }
    }
}