package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import javafx.scene.control.Alert.AlertType;
import java.sql.*;
import java.io.IOException;
import java.io.File;

public class SystemSettingController {

    @FXML private Button Browsebtn;
    @FXML private Button LoadDefaultsbtn;
    @FXML private Button Savingsettingsbtn;
    @FXML private Button TestBackupbtn;
    @FXML private CheckBox auditLoggingCheck;
    @FXML private CheckBox autoBackupCheck;
    @FXML private CheckBox autoVerifyBackupCheck;
    @FXML private ComboBox<String> backupFrequencyCombo;
    @FXML private TextField backupLocationField;
    @FXML private TextField backupRetentionField;
    @FXML private TextField backupTimeField;
    @FXML private Button cancelbtn;
    @FXML private ComboBox<String> compressionLevelCombo;
    @FXML private TextField currencyField;
    @FXML private ComboBox<String> databaseBackupTypeCombo;
    @FXML private ComboBox<String> dateFormatCombo;
    @FXML private CheckBox emailBackupFailure;
    @FXML private CheckBox emailBackupSuccess;
    @FXML private CheckBox emailRestoreFailure;
    @FXML private CheckBox emailRestoreSuccess;
    @FXML private CheckBox encryptBackupCheck;
    @FXML private TextField fromEmailField;
    @FXML private ComboBox<String> languageCombo;
    @FXML private Label lastBackupLabel;
    @FXML private CheckBox loginAttemptsCheck;
    @FXML private TextField maxBackupSizeField;
    @FXML private Button newTaxpayerBtn1;
    @FXML private CheckBox notifyOnBackupCheck;
    @FXML private CheckBox notifyOnErrorCheck;
    @FXML private CheckBox passwordPolicyCheck;
    @FXML private CheckBox sessionTimeoutCheck;
    @FXML private TextField smtpPortField;
    @FXML private TextField smtpServerField;
    @FXML private Label statusLabel;
    @FXML private TextField systemNameField;
    @FXML private ComboBox<String> timeZoneCombo;
    
    private Connection connection;
    private final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "Belay2123";

    // IMPORTANT: The initialize() method MUST be public
    @FXML
    public void initialize() {
        System.out.println("SystemSettingController.initialize() called!");
        
        try {
            // Try to connect to database
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Database connected!");
            
            // Initialize UI components
            initializeComboBoxes();
            
            // Try to load settings
            try {
                loadSettingsFromDatabase();
            } catch (SQLException e) {
                System.out.println("Could not load from DB, using defaults: " + e.getMessage());
                loadDefaultSettings();
            }
            
            // Set up event handlers
            setupEventHandlers();
            
            statusLabel.setText("System settings loaded successfully!");
            statusLabel.setStyle("-fx-text-fill: green;");
            
        } catch (SQLException e) {
            System.out.println("Database connection failed, using defaults: " + e.getMessage());
            showAlert("Database Error", "Failed to connect to database. Using default settings.", AlertType.WARNING);
            
            // Load defaults even without DB
            initializeComboBoxes();
            loadDefaultSettings();
            setupEventHandlers();
            
            statusLabel.setText("Using default settings (database unavailable)");
            statusLabel.setStyle("-fx-text-fill: orange;");
        }
    }

    private void initializeComboBoxes() {
        // Language options
        languageCombo.getItems().addAll("English", "Amharic", "Oromiffa", "Tigrigna");
        languageCombo.setValue("English");
        
        // Timezone options
        timeZoneCombo.getItems().addAll("Africa/Addis_Ababa", "UTC", "GMT", "EST", "PST");
        timeZoneCombo.setValue("Africa/Addis_Ababa");
        
        // Date format options
        dateFormatCombo.getItems().addAll("YYYY-MM-DD", "MM/DD/YYYY", "DD/MM/YYYY", "YYYY/MM/DD");
        dateFormatCombo.setValue("YYYY-MM-DD");
        
        // Backup frequency
        backupFrequencyCombo.getItems().addAll("Daily", "Weekly", "Monthly", "Never");
        backupFrequencyCombo.setValue("Daily");
        
        // Database backup type
        databaseBackupTypeCombo.getItems().addAll("Full", "Incremental", "Differential");
        databaseBackupTypeCombo.setValue("Full");
        
        // Compression level
        compressionLevelCombo.getItems().addAll("None", "Low", "Medium", "High", "Maximum");
        compressionLevelCombo.setValue("Medium");
    }

    private void setupEventHandlers() {
        // Auto backup checkbox enables/disables related fields
        autoBackupCheck.selectedProperty().addListener((observable, oldValue, newValue) -> {
            backupFrequencyCombo.setDisable(!newValue);
            backupTimeField.setDisable(!newValue);
            backupLocationField.setDisable(!newValue);
            backupRetentionField.setDisable(!newValue);
            maxBackupSizeField.setDisable(!newValue);
            Browsebtn.setDisable(!newValue);
            
            if (!newValue) {
                backupTimeField.setText("");
                backupRetentionField.setText("");
                backupLocationField.setText("");
                maxBackupSizeField.setText("");
            } else {
                if (backupTimeField.getText().isEmpty()) backupTimeField.setText("23:00");
                if (backupRetentionField.getText().isEmpty()) backupRetentionField.setText("30");
                if (backupLocationField.getText().isEmpty()) backupLocationField.setText(System.getProperty("user.home") + "/tax_system_backups");
                if (maxBackupSizeField.getText().isEmpty()) maxBackupSizeField.setText("1024");
            }
        });
    }

    // ========== EVENT HANDLER METHODS ==========
    // IMPORTANT: These MUST be private and match FXML onAction="#methodName"
    
    @FXML
    private void handlerclickBrowsebtn(ActionEvent event) {
        System.out.println("Browse button clicked");
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Backup Directory");
        File selectedDir = directoryChooser.showDialog(((Button) event.getSource()).getScene().getWindow());
        if (selectedDir != null) {
            backupLocationField.setText(selectedDir.getAbsolutePath());
            settingsChanged();
        }
    }

    @FXML
    private void handlerclickLoadDefaultsbtn(ActionEvent event) {
        System.out.println("Load Defaults button clicked");
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Load Defaults");
        confirm.setHeaderText("Load Default Settings");
        confirm.setContentText("Are you sure you want to load default settings?\nCurrent settings will be lost.");
        
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                loadDefaultSettings();
                statusLabel.setText("Default settings loaded!");
                statusLabel.setStyle("-fx-text-fill: green;");
            }
        });
    }

    @FXML
    private void handlerclickSavingsettingsbtn(ActionEvent event) {
        System.out.println("Save Settings button clicked");
        if (validateSettings()) {
            try {
                saveSettingsToDatabase();
            } catch (Exception e) {
                showAlert("Error", "Could not save to database: " + e.getMessage(), AlertType.ERROR);
                statusLabel.setText("Settings saved locally (database error)");
                statusLabel.setStyle("-fx-text-fill: orange;");
            }
        }
    }

    @FXML
    private void handlerclickTestBackupbtn(ActionEvent event) {
        System.out.println("Test Backup button clicked");
        testBackupConfiguration();
    }

    @FXML
    private void handlerclickauditLoggingCheck(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickautoBackupCheck(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickautoVerifyBackupCheck(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickbackupFrequencyCombo(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickcancelbtn(ActionEvent event) {
        System.out.println("Cancel button clicked");
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Cancel");
        confirm.setHeaderText("Discard Changes");
        confirm.setContentText("Are you sure you want to discard all unsaved changes?");
        
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    loadSettingsFromDatabase();
                    statusLabel.setText("Changes cancelled");
                    statusLabel.setStyle("-fx-text-fill: orange;");
                } catch (SQLException e) {
                    statusLabel.setText("Could not reload settings");
                    statusLabel.setStyle("-fx-text-fill: red;");
                }
            }
        });
    }

    @FXML
    private void handlerclickcompressionLevelCombo(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickdatabaseBackupTypeCombo(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickdateFormatCombo(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickemailBackupFailure(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickemailBackupSuccess(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickemailRestoreFailure(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickemailRestoreSuccess(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickencryptBackupCheck(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclicklanguageCombo(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickloginAttemptsCheck(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclicknewTaxpayerBtn1(ActionEvent event) {
        System.out.println("Back to Admin button clicked");
        navigateBackToAdmin();
    }

    @FXML
    private void handlerclicknotifyOnBackupCheck(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclicknotifyOnErrorCheck(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclickpasswordPolicyCheck(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclicksessionTimeoutCheck(ActionEvent event) {
        settingsChanged();
    }

    @FXML
    private void handlerclicktimeZoneCombo(ActionEvent event) {
        settingsChanged();
    }

    // ========== HELPER METHODS ==========
    
    private void loadSettingsFromDatabase() throws SQLException {
        // Create settings table if it doesn't exist
        createSettingsTable();
        
        String query = "SELECT * FROM system_settings WHERE setting_id = 1";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        
        if (rs.next()) {
            // General Settings
            systemNameField.setText(rs.getString("system_name"));
            languageCombo.setValue(rs.getString("default_language"));
            timeZoneCombo.setValue(rs.getString("time_zone"));
            dateFormatCombo.setValue(rs.getString("date_format"));
            currencyField.setText(rs.getString("currency"));
            
            // Security Settings
            passwordPolicyCheck.setSelected(rs.getBoolean("enable_password_policy"));
            sessionTimeoutCheck.setSelected(rs.getBoolean("enable_session_timeout"));
            loginAttemptsCheck.setSelected(rs.getBoolean("limit_login_attempts"));
            auditLoggingCheck.setSelected(rs.getBoolean("enable_audit_logging"));
            
            // Backup Settings
            autoBackupCheck.setSelected(rs.getBoolean("auto_backup_enabled"));
            backupFrequencyCombo.setValue(rs.getString("backup_frequency"));
            backupTimeField.setText(rs.getString("backup_time"));
            backupRetentionField.setText(rs.getString("backup_retention"));
            backupLocationField.setText(rs.getString("backup_location"));
            maxBackupSizeField.setText(rs.getString("max_backup_size"));
            
            // Recovery Settings
            autoVerifyBackupCheck.setSelected(rs.getBoolean("auto_verify_backups"));
            encryptBackupCheck.setSelected(rs.getBoolean("encrypt_backup_files"));
            notifyOnBackupCheck.setSelected(rs.getBoolean("notify_on_backup_completion"));
            notifyOnErrorCheck.setSelected(rs.getBoolean("notify_on_backup_error"));
            
            // Database Settings
            databaseBackupTypeCombo.setValue(rs.getString("backup_type"));
            compressionLevelCombo.setValue(rs.getString("compression_level"));
            
            // Notification Settings
            emailBackupSuccess.setSelected(rs.getBoolean("notify_backup_success"));
            emailBackupFailure.setSelected(rs.getBoolean("notify_backup_failure"));
            emailRestoreSuccess.setSelected(rs.getBoolean("notify_restore_success"));
            emailRestoreFailure.setSelected(rs.getBoolean("notify_restore_failure"));
            
            // Email Configuration
            smtpServerField.setText(rs.getString("smtp_server"));
            smtpPortField.setText(rs.getString("smtp_port"));
            fromEmailField.setText(rs.getString("from_email"));
            
            // Load last backup info
            loadLastBackupInfo();
            
            // Update UI states based on loaded settings
            updateEmailFieldsState();
            
            // Enable/disable backup fields based on auto backup setting
            backupFrequencyCombo.setDisable(!autoBackupCheck.isSelected());
            backupTimeField.setDisable(!autoBackupCheck.isSelected());
            backupLocationField.setDisable(!autoBackupCheck.isSelected());
            backupRetentionField.setDisable(!autoBackupCheck.isSelected());
            maxBackupSizeField.setDisable(!autoBackupCheck.isSelected());
            Browsebtn.setDisable(!autoBackupCheck.isSelected());
            
            System.out.println("Settings loaded from database successfully!");
        } else {
            System.out.println("No saved settings found, loading defaults");
            loadDefaultSettings();
        }
        rs.close();
        stmt.close();
    }

    private void createSettingsTable() {
        try {
            String query = "CREATE TABLE IF NOT EXISTS system_settings (" +
                         "setting_id INT PRIMARY KEY, " +
                         "system_name VARCHAR(255), " +
                         "default_language VARCHAR(50), " +
                         "time_zone VARCHAR(100), " +
                         "date_format VARCHAR(50), " +
                         "currency VARCHAR(10), " +
                         "enable_password_policy BOOLEAN DEFAULT 1, " +
                         "enable_session_timeout BOOLEAN DEFAULT 1, " +
                         "limit_login_attempts BOOLEAN DEFAULT 1, " +
                         "enable_audit_logging BOOLEAN DEFAULT 1, " +
                         "auto_backup_enabled BOOLEAN DEFAULT 0, " +
                         "backup_frequency VARCHAR(50), " +
                         "backup_time VARCHAR(10), " +
                         "backup_retention VARCHAR(10), " +
                         "backup_location VARCHAR(500), " +
                         "max_backup_size VARCHAR(20), " +
                         "auto_verify_backups BOOLEAN DEFAULT 1, " +
                         "encrypt_backup_files BOOLEAN DEFAULT 0, " +
                         "notify_on_backup_completion BOOLEAN DEFAULT 1, " +
                         "notify_on_backup_error BOOLEAN DEFAULT 1, " +
                         "backup_type VARCHAR(50), " +
                         "compression_level VARCHAR(50), " +
                         "notify_backup_success BOOLEAN DEFAULT 1, " +
                         "notify_backup_failure BOOLEAN DEFAULT 1, " +
                         "notify_restore_success BOOLEAN DEFAULT 0, " +
                         "notify_restore_failure BOOLEAN DEFAULT 1, " +
                         "smtp_server VARCHAR(255), " +
                         "smtp_port VARCHAR(10), " +
                         "from_email VARCHAR(255), " +
                         "last_backup_time TIMESTAMP NULL, " +
                         "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)";
            
            Statement stmt = connection.createStatement();
            stmt.execute(query);
            
            // Also create backup_history table if it doesn't exist
            String backupHistoryQuery = "CREATE TABLE IF NOT EXISTS backup_history (" +
                                      "backup_id INT AUTO_INCREMENT PRIMARY KEY, " +
                                      "backup_type VARCHAR(50), " +
                                      "file_path VARCHAR(500), " +
                                      "file_size VARCHAR(20), " +
                                      "status VARCHAR(50), " +
                                      "error_message TEXT, " +
                                      "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(backupHistoryQuery);
            
            stmt.close();
            System.out.println("Database tables created successfully!");
            
        } catch (SQLException e) {
            System.out.println("Note: Could not create database tables: " + e.getMessage());
        }
    }

    private void loadDefaultSettings() {
        System.out.println("Loading default settings...");
        
        // General Settings
        systemNameField.setText("Tax Management System");
        languageCombo.setValue("English");
        timeZoneCombo.setValue("Africa/Addis_Ababa");
        dateFormatCombo.setValue("YYYY-MM-DD");
        currencyField.setText("ETB");
        
        // Security Settings
        passwordPolicyCheck.setSelected(true);
        sessionTimeoutCheck.setSelected(true);
        loginAttemptsCheck.setSelected(true);
        auditLoggingCheck.setSelected(true);
        
        // Backup Settings
        autoBackupCheck.setSelected(false);
        backupFrequencyCombo.setValue("Daily");
        backupTimeField.setText("23:00");
        backupRetentionField.setText("30");
        backupLocationField.setText(System.getProperty("user.home") + "/tax_system_backups");
        maxBackupSizeField.setText("1024");
        
        // Recovery Settings
        autoVerifyBackupCheck.setSelected(true);
        encryptBackupCheck.setSelected(false);
        notifyOnBackupCheck.setSelected(true);
        notifyOnErrorCheck.setSelected(true);
        
        // Database Settings
        databaseBackupTypeCombo.setValue("Full");
        compressionLevelCombo.setValue("Medium");
        
        // Notification Settings
        emailBackupSuccess.setSelected(true);
        emailBackupFailure.setSelected(true);
        emailRestoreSuccess.setSelected(false);
        emailRestoreFailure.setSelected(true);
        
        // Email Configuration
        smtpServerField.setText("smtp.gmail.com");
        smtpPortField.setText("587");
        fromEmailField.setText("noreply@taxsystem.et");
        
        // Enable/disable backup fields based on auto backup setting
        backupFrequencyCombo.setDisable(!autoBackupCheck.isSelected());
        backupTimeField.setDisable(!autoBackupCheck.isSelected());
        backupLocationField.setDisable(!autoBackupCheck.isSelected());
        backupRetentionField.setDisable(!autoBackupCheck.isSelected());
        maxBackupSizeField.setDisable(!autoBackupCheck.isSelected());
        Browsebtn.setDisable(!autoBackupCheck.isSelected());
        
        // Update email fields state
        updateEmailFieldsState();
        
        lastBackupLabel.setText("Last backup: Never");
    }

    private void updateEmailFieldsState() {
        boolean emailEnabled = emailBackupSuccess.isSelected() || 
                               emailBackupFailure.isSelected() || 
                               emailRestoreSuccess.isSelected() || 
                               emailRestoreFailure.isSelected();
        
        smtpServerField.setDisable(!emailEnabled);
        smtpPortField.setDisable(!emailEnabled);
        fromEmailField.setDisable(!emailEnabled);
    }

    private void loadLastBackupInfo() {
        try {
            String query = "SELECT MAX(created_at) as last_backup FROM backup_history WHERE status = 'Completed'";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            if (rs.next() && rs.getTimestamp("last_backup") != null) {
                lastBackupLabel.setText("Last Backup: " + rs.getTimestamp("last_backup"));
            } else {
                lastBackupLabel.setText("Last Backup: Never");
            }
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            lastBackupLabel.setText("Last Backup: Unknown");
        }
    }

    private boolean validateSettings() {
        // Validate system name
        if (systemNameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "System name is required", AlertType.ERROR);
            systemNameField.requestFocus();
            return false;
        }
        
        // Validate currency
        if (currencyField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Currency is required", AlertType.ERROR);
            currencyField.requestFocus();
            return false;
        }
        
        // Validate backup settings if auto backup is enabled
        if (autoBackupCheck.isSelected()) {
            if (backupLocationField.getText().trim().isEmpty()) {
                showAlert("Validation Error", "Backup location is required when auto backup is enabled", AlertType.ERROR);
                backupLocationField.requestFocus();
                return false;
            }
            
            // Validate backup retention (should be a number)
            try {
                int retention = Integer.parseInt(backupRetentionField.getText());
                if (retention <= 0) {
                    showAlert("Validation Error", "Backup retention must be a positive number", AlertType.ERROR);
                    backupRetentionField.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert("Validation Error", "Backup retention must be a valid number", AlertType.ERROR);
                backupRetentionField.requestFocus();
                return false;
            }
            
            // Validate backup time format (HH:MM)
            String backupTime = backupTimeField.getText().trim();
            if (!backupTime.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                showAlert("Validation Error", "Backup time must be in HH:MM format (24-hour)", AlertType.ERROR);
                backupTimeField.requestFocus();
                return false;
            }
            
            // Validate max backup size
            try {
                int maxSize = Integer.parseInt(maxBackupSizeField.getText());
                if (maxSize <= 0) {
                    showAlert("Validation Error", "Maximum backup size must be a positive number", AlertType.ERROR);
                    maxBackupSizeField.requestFocus();
                    return false;
                }
            } catch (NumberFormatException e) {
                showAlert("Validation Error", "Maximum backup size must be a valid number", AlertType.ERROR);
                maxBackupSizeField.requestFocus();
                return false;
            }
        }
        
        return true;
    }

    private void saveSettingsToDatabase() {
        try {
            // First, validate the connection
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            }
            
            String query = "INSERT INTO system_settings (setting_id, system_name, default_language, time_zone, " +
                         "date_format, currency, enable_password_policy, enable_session_timeout, limit_login_attempts, " +
                         "enable_audit_logging, auto_backup_enabled, backup_frequency, backup_time, backup_retention, " +
                         "backup_location, max_backup_size, auto_verify_backups, encrypt_backup_files, " +
                         "notify_on_backup_completion, notify_on_backup_error, backup_type, compression_level, " +
                         "notify_backup_success, notify_backup_failure, notify_restore_success, notify_restore_failure, " +
                         "smtp_server, smtp_port, from_email) " +
                         "VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE system_name = VALUES(system_name), default_language = VALUES(default_language), " +
                         "time_zone = VALUES(time_zone), date_format = VALUES(date_format), currency = VALUES(currency), " +
                         "enable_password_policy = VALUES(enable_password_policy), enable_session_timeout = VALUES(enable_session_timeout), " +
                         "limit_login_attempts = VALUES(limit_login_attempts), enable_audit_logging = VALUES(enable_audit_logging), " +
                         "auto_backup_enabled = VALUES(auto_backup_enabled), backup_frequency = VALUES(backup_frequency), " +
                         "backup_time = VALUES(backup_time), backup_retention = VALUES(backup_retention), " +
                         "backup_location = VALUES(backup_location), max_backup_size = VALUES(max_backup_size), " +
                         "auto_verify_backups = VALUES(auto_verify_backups), encrypt_backup_files = VALUES(encrypt_backup_files), " +
                         "notify_on_backup_completion = VALUES(notify_on_backup_completion), " +
                         "notify_on_backup_error = VALUES(notify_on_backup_error), backup_type = VALUES(backup_type), " +
                         "compression_level = VALUES(compression_level), notify_backup_success = VALUES(notify_backup_success), " +
                         "notify_backup_failure = VALUES(notify_backup_failure), notify_restore_success = VALUES(notify_restore_success), " +
                         "notify_restore_failure = VALUES(notify_restore_failure), smtp_server = VALUES(smtp_server), " +
                         "smtp_port = VALUES(smtp_port), from_email = VALUES(from_email), updated_at = CURRENT_TIMESTAMP";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            
            // Set all parameters
            int paramIndex = 1;
            stmt.setString(paramIndex++, systemNameField.getText());
            stmt.setString(paramIndex++, languageCombo.getValue() != null ? languageCombo.getValue() : "English");
            stmt.setString(paramIndex++, timeZoneCombo.getValue() != null ? timeZoneCombo.getValue() : "Africa/Addis_Ababa");
            stmt.setString(paramIndex++, dateFormatCombo.getValue() != null ? dateFormatCombo.getValue() : "YYYY-MM-DD");
            stmt.setString(paramIndex++, currencyField.getText());
            
            stmt.setBoolean(paramIndex++, passwordPolicyCheck.isSelected());
            stmt.setBoolean(paramIndex++, sessionTimeoutCheck.isSelected());
            stmt.setBoolean(paramIndex++, loginAttemptsCheck.isSelected());
            stmt.setBoolean(paramIndex++, auditLoggingCheck.isSelected());
            
            stmt.setBoolean(paramIndex++, autoBackupCheck.isSelected());
            stmt.setString(paramIndex++, backupFrequencyCombo.getValue() != null ? backupFrequencyCombo.getValue() : "Daily");
            stmt.setString(paramIndex++, backupTimeField.getText());
            stmt.setString(paramIndex++, backupRetentionField.getText());
            stmt.setString(paramIndex++, backupLocationField.getText());
            stmt.setString(paramIndex++, maxBackupSizeField.getText());
            
            stmt.setBoolean(paramIndex++, autoVerifyBackupCheck.isSelected());
            stmt.setBoolean(paramIndex++, encryptBackupCheck.isSelected());
            stmt.setBoolean(paramIndex++, notifyOnBackupCheck.isSelected());
            stmt.setBoolean(paramIndex++, notifyOnErrorCheck.isSelected());
            
            stmt.setString(paramIndex++, databaseBackupTypeCombo.getValue() != null ? databaseBackupTypeCombo.getValue() : "Full");
            stmt.setString(paramIndex++, compressionLevelCombo.getValue() != null ? compressionLevelCombo.getValue() : "Medium");
            
            stmt.setBoolean(paramIndex++, emailBackupSuccess.isSelected());
            stmt.setBoolean(paramIndex++, emailBackupFailure.isSelected());
            stmt.setBoolean(paramIndex++, emailRestoreSuccess.isSelected());
            stmt.setBoolean(paramIndex++, emailRestoreFailure.isSelected());
            
            stmt.setString(paramIndex++, smtpServerField.getText());
            stmt.setString(paramIndex++, smtpPortField.getText());
            stmt.setString(paramIndex++, fromEmailField.getText());
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            if (rowsAffected > 0) {
                statusLabel.setText("Settings saved successfully!");
                statusLabel.setStyle("-fx-text-fill: green;");
                
                // Log the backup configuration
                if (autoBackupCheck.isSelected()) {
                    logBackupConfiguration();
                }
            }
            
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save settings: " + e.getMessage(), e);
        }
    }
    
    private void logBackupConfiguration() {
        try {
            String query = "INSERT INTO backup_history (backup_type, status, created_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, "Configuration Updated");
            stmt.setString(2, "Settings Saved");
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Note: Could not log backup configuration: " + e.getMessage());
        }
    }

    private void testBackupConfiguration() {
        try {
            if (autoBackupCheck.isSelected()) {
                // Check if backup location is writable
                String backupPath = backupLocationField.getText().trim();
                if (backupPath.isEmpty()) {
                    showAlert("Test Failed", "Backup location is not set", AlertType.WARNING);
                    return;
                }
                
                File backupDir = new File(backupPath);
                
                // Test directory creation if it doesn't exist
                if (!backupDir.exists()) {
                    if (backupDir.mkdirs()) {
                        showAlert("Test Successful", "Backup directory created successfully at:\n" + backupPath, AlertType.INFORMATION);
                    } else {
                        showAlert("Test Failed", "Failed to create backup directory.\nCheck permissions for path:\n" + backupPath, AlertType.ERROR);
                        return;
                    }
                }
                
                // Test if directory is writable
                File testFile = new File(backupDir, "test_backup_" + System.currentTimeMillis() + ".txt");
                try {
                    if (testFile.createNewFile()) {
                        // Write test content
                        java.nio.file.Files.write(testFile.toPath(), "Test backup file - can be deleted".getBytes());
                        testFile.delete();
                        showAlert("Test Successful", "Backup location is writable:\n" + backupPath, AlertType.INFORMATION);
                        
                        // Log test success
                        logBackupTest("Success", "Backup location test passed");
                        
                    } else {
                        showAlert("Test Failed", "Cannot create files in backup location.\nCheck permissions for:\n" + backupPath, AlertType.ERROR);
                        logBackupTest("Failed", "Cannot create files in backup location");
                    }
                } catch (Exception e) {
                    showAlert("Test Failed", "Backup location is not writable:\n" + e.getMessage(), AlertType.ERROR);
                    logBackupTest("Failed", "Backup location not writable: " + e.getMessage());
                }
                
            } else {
                showAlert("Test Skipped", "Auto backup is disabled.\nEnable it to test backup configuration.", AlertType.INFORMATION);
            }
            
        } catch (Exception e) {
            showAlert("Test Error", "Failed to test backup configuration:\n" + e.getMessage(), AlertType.ERROR);
            logBackupTest("Error", "Exception during backup test: " + e.getMessage());
        }
    }
    
    private void logBackupTest(String status, String message) {
        try {
            String query = "INSERT INTO backup_history (backup_type, status, error_message) VALUES (?, ?, ?)";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, "Configuration Test");
            stmt.setString(2, status);
            stmt.setString(3, message);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            System.out.println("Note: Could not log backup test: " + e.getMessage());
        }
    }

    private void settingsChanged() {
        if (!statusLabel.getText().equals("Unsaved changes")) {
            statusLabel.setText("Unsaved changes");
            statusLabel.setStyle("-fx-text-fill: orange;");
        }
    }

    private void navigateBackToAdmin() {
        // Check if there are unsaved changes
        if (statusLabel.getText().equals("Unsaved changes")) {
            Alert confirm = new Alert(AlertType.CONFIRMATION);
            confirm.setTitle("Unsaved Changes");
            confirm.setHeaderText("You have unsaved changes");
            confirm.setContentText("Do you want to save changes before leaving?");
            
            confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    if (validateSettings()) {
                        saveSettingsToDatabase();
                        performNavigation();
                    }
                } else if (response == ButtonType.NO) {
                    performNavigation();
                }
                // If CANCEL, do nothing
            });
        } else {
            performNavigation();
        }
    }

    private void performNavigation() {
        try {
            // Close database connection
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            
            // Try multiple possible paths for Admin.fxml
            String[] possiblePaths = {
                "/TMSFXML/Admin.fxml",
                "/Admin.fxml",
                "Admin.fxml",
                "../TMSFXML/Admin.fxml",
                "../Admin.fxml"
            };
            
            Parent adminRoot = null;
            FXMLLoader loader = null;
            
            for (String path : possiblePaths) {
                try {
                    System.out.println("Trying to load Admin.fxml from: " + path);
                    loader = new FXMLLoader(getClass().getResource(path));
                    adminRoot = loader.load();
                    System.out.println("Successfully loaded from: " + path);
                    break;
                } catch (Exception e) {
                    System.out.println("Failed to load from " + path + ": " + e.getMessage());
                }
            }
            
            if (adminRoot == null) {
                throw new IOException("Could not find Admin.fxml in any location");
            }
            
            Stage currentStage = (Stage) newTaxpayerBtn1.getScene().getWindow();
            Scene adminScene = new Scene(adminRoot);
            currentStage.setScene(adminScene);
            currentStage.setTitle("Admin Panel");
            currentStage.centerOnScreen();
            currentStage.show();
            
        } catch (IOException e) {
            System.out.println("Navigation error: " + e.getMessage());
            showAlert("Navigation Error", "Could not load Admin page. Please check if Admin.fxml exists.", AlertType.ERROR);
        } catch (SQLException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}