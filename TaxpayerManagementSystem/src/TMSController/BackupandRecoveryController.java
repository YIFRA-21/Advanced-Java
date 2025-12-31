package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.concurrent.Task;
import java.sql.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BackupandRecoveryController {

    // FXML Fields
    @FXML private TextField backupNameField;
    @FXML private TextArea backupDescriptionField;
    @FXML private TextField backupLocationField;
    @FXML private TextField restoreFileField;
    @FXML private TextField encryptionKeyField;
    @FXML private TextField restoreKeyField;
    @FXML private ComboBox<String> scheduleCombo;
    @FXML private ComboBox<String> restoreModeCombo;
    @FXML private ComboBox<String> tablesToRestoreCombo;
    @FXML private ComboBox<String> conflictResolutionCombo;
    @FXML private Label compressLabel;
    @FXML private Label auditLabel;
    @FXML private Label taxDataLabel;
    @FXML private Label verifyLabel;
    @FXML private Label preRestoreLabel;
    @FXML private Button browseBackupBtn;
    @FXML private Button browseRestoreBtn;
    @FXML private Button generateKeyBtn;
    @FXML private Button createBackupBtn;
    @FXML private Button restoreBackupBtn;
    @FXML private Button refreshBtn;
    @FXML private Button cancelBtn;
    @FXML private Button backToDashboardBtn;
    @FXML private VBox backupListContainer;
    @FXML private VBox progressSection;
    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressPercentage;
    @FXML private Label progressStatus;
    @FXML private Label progressDetails;
    
    private Connection connection;
    private final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "Belay2123";
    
    @FXML
    public void initialize() {
        System.out.println("BackupandRecoveryController initialized");
        
        try {
            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            initializeComponents();
            
        } catch (ClassNotFoundException e) {
            showAlert("Driver Error", "MySQL JDBC Driver not found: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void initializeComponents() {
        // Initialize default backup location
        String defaultLocation = System.getProperty("user.home") + File.separator + "TMS_Backups";
        backupLocationField.setText(defaultLocation);
        
        // Create directory if it doesn't exist
        File backupDir = new File(defaultLocation);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        // Initialize combo boxes
        scheduleCombo.getItems().addAll("Manual", "Daily", "Weekly", "Monthly");
        scheduleCombo.setValue("Manual");
        
        restoreModeCombo.getItems().addAll("Full Restore", "Partial Restore", "Structure Only");
        restoreModeCombo.setValue("Full Restore");
        
        tablesToRestoreCombo.getItems().addAll("All Tables", "Taxpayers Only", "Assessments Only", "Payments Only");
        tablesToRestoreCombo.setValue("All Tables");
        
        conflictResolutionCombo.getItems().addAll("Skip", "Replace", "Merge");
        conflictResolutionCombo.setValue("Replace");
        
        // Initialize backup name with timestamp
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        backupNameField.setText("backup_" + timestamp);
        
        // Hide progress section initially
        progressSection.setVisible(false);
    }
    
    // ==================== EVENT HANDLERS ====================
    
    @FXML
    private void handleBrowseBackup(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Backup Location");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        Stage stage = (Stage) browseBackupBtn.getScene().getWindow();
        File selectedDir = directoryChooser.showDialog(stage);
        
        if (selectedDir != null) {
            backupLocationField.setText(selectedDir.getAbsolutePath());
        }
    }
    
    @FXML
    private void handleBrowseRestore(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Backup File");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("SQL Files", "*.sql"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        fileChooser.setInitialDirectory(new File(backupLocationField.getText()));
        
        Stage stage = (Stage) browseRestoreBtn.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            restoreFileField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    @FXML
    private void handleGenerateKey(ActionEvent event) {
        // Generate a random encryption key
        String key = generateRandomKey(16);
        encryptionKeyField.setText(key);
        showAlert("Key Generated", "Encryption key generated: " + key, Alert.AlertType.INFORMATION);
    }
    
    @FXML
    private void handleCreateBackup(ActionEvent event) {
        // Validate inputs
        if (backupNameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter a backup name", Alert.AlertType.ERROR);
            backupNameField.requestFocus();
            return;
        }
        
        if (backupLocationField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please select a backup location", Alert.AlertType.ERROR);
            backupLocationField.requestFocus();
            return;
        }
        
        // Start backup process
        showAlert("Backup Started", "Backup functionality will be implemented soon.", Alert.AlertType.INFORMATION);
    }
    
    @FXML
    private void handleRestoreBackup(ActionEvent event) {
        // Validate inputs
        if (restoreFileField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please select a backup file to restore", Alert.AlertType.ERROR);
            restoreFileField.requestFocus();
            return;
        }
        
        // Confirm restore
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Restore");
        confirm.setHeaderText("Database Restore Warning");
        confirm.setContentText("Restore functionality will be implemented soon.");
        
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait();
    }
    
    @FXML
    private void handleRefresh(ActionEvent event) {
        showAlert("Refresh", "Backup list will be loaded when implemented.", Alert.AlertType.INFORMATION);
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        closeWindow();
    }
    
    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Dashboard.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) backToDashboardBtn.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Dashboard");
            stage.show();
            
        } catch (Exception e) {
            showAlert("Navigation Error", "Failed to load dashboard: " + e.getMessage(), Alert.AlertType.ERROR);
            closeWindow();
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private String generateRandomKey(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            key.append(chars.charAt(index));
        }
        return key.toString();
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}