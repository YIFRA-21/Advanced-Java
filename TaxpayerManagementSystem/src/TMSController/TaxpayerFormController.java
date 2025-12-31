package TMSController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class TaxpayerFormController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(TaxpayerFormController.class.getName());
    
    // Database connection
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    
    private Connection connection;
    
    // FXML Elements
    @FXML private Label formSubtitle;
    @FXML private Label formModeLabel;
    @FXML private TextField tinField;
    @FXML private TextField taxpayerNameField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private DatePicker registrationDatePicker;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> regionCombo;
    @FXML private TextField woredaField;
    @FXML private TextArea addressField;
    @FXML private ComboBox<String> businessTypeCombo;
    @FXML private TextField annualTurnoverField;
    @FXML private TextField licenseNoField;
    @FXML private DatePicker licenseDatePicker;
    @FXML private RadioButton activeStatusRadio;
    @FXML private RadioButton inactiveStatusRadio;
    @FXML private RadioButton suspendedStatusRadio;
    @FXML private TextArea notesField;
    
    private ToggleGroup statusToggleGroup = new ToggleGroup();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LOGGER.info("Initializing TaxpayerFormController");
        
        try {
            // Initialize database connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            LOGGER.info("Database connected successfully for taxpayer form");
            
            // Setup UI components
            setupUIComponents();
            
            // Setup toggle group for radio buttons
            activeStatusRadio.setToggleGroup(statusToggleGroup);
            inactiveStatusRadio.setToggleGroup(statusToggleGroup);
            suspendedStatusRadio.setToggleGroup(statusToggleGroup);
            activeStatusRadio.setSelected(true);
            
            // Set default dates
            registrationDatePicker.setValue(LocalDate.now());
            licenseDatePicker.setValue(LocalDate.now());
            
            // Load combo box data
            loadComboBoxData();
            
        } catch (SQLException e) {
            LOGGER.severe("Initialization error: " + e.getMessage());
            showAlert("Database Error", 
                     "Failed to initialize database connection: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
        }
    }
    
    private void setupUIComponents() {
        // Set prompt texts
        tinField.setPromptText("Enter 10-digit TIN");
        taxpayerNameField.setPromptText("Enter full name");
        phoneField.setPromptText("09XXXXXXXXX");
        emailField.setPromptText("example@domain.com");
        woredaField.setPromptText("Enter sub-city/woreda");
        addressField.setPromptText("Enter complete address");
        annualTurnoverField.setPromptText("0.00");
        licenseNoField.setPromptText("Enter license number");
        notesField.setPromptText("Enter any additional notes");
    }
    
    private void loadComboBoxData() {
        // Load categories
        ObservableList<String> categories = FXCollections.observableArrayList(
            "Individual", "Business", "Corporate", "Government"
        );
        categoryCombo.setItems(categories);
        categoryCombo.getSelectionModel().selectFirst();
        
        // Load regions
        ObservableList<String> regions = FXCollections.observableArrayList(
            "Addis Ababa", "Oromia", "Amhara", "Tigray", "SNNPR", "Somali", 
            "Afar", "Benishangul-Gumuz", "Gambela", "Harari", "Dire Dawa"
        );
        regionCombo.setItems(regions);
        regionCombo.getSelectionModel().selectFirst();
        
        // Load business types
        ObservableList<String> businessTypes = FXCollections.observableArrayList(
            "Manufacturing", "Service", "Trade", "Agriculture", "Construction",
            "Transportation", "Healthcare", "Education", "Tourism", "Technology"
        );
        businessTypeCombo.setItems(businessTypes);
        businessTypeCombo.getSelectionModel().selectFirst();
    }
    
    @FXML
    private void handleBackButton(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/TMSFXML/TaxpayerManagement.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            LOGGER.severe("Failed to navigate back: " + e.getMessage());
            showAlert("Navigation Error", 
                     "Failed to navigate to taxpayer management", 
                     Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handlerclicksaveBtn(ActionEvent event) {
        if (validateForm()) {
            saveTaxpayer();
        }
    }
    
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        
        // Validate TIN (10 digits)
        String tin = tinField.getText().trim();
        if (tin.isEmpty()) {
            errors.append("• TIN is required\n");
        } else if (!tin.matches("^[0-9]{10}$")) {
            errors.append("• TIN must be 10 digits\n");
        }
        
        // Validate name
        if (taxpayerNameField.getText().trim().isEmpty()) {
            errors.append("• Taxpayer name is required\n");
        }
        
        // Validate registration date
        if (registrationDatePicker.getValue() == null) {
            errors.append("• Registration date is required\n");
        }
        
        // Validate email format if provided
        String email = emailField.getText().trim();
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.append("• Invalid email format\n");
        }
        
        // Validate phone format if provided
        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^0[0-9]{9}$")) {
            errors.append("• Phone must be 10 digits starting with 0\n");
        }
        
        // Validate turnover if provided
        String turnover = annualTurnoverField.getText().trim();
        if (!turnover.isEmpty()) {
            try {
                double value = Double.parseDouble(turnover);
                if (value < 0) {
                    errors.append("• Annual turnover cannot be negative\n");
                }
            } catch (NumberFormatException e) {
                errors.append("• Annual turnover must be a valid number\n");
            }
        }
        
        if (errors.length() > 0) {
            showAlert("Validation Errors", errors.toString(), Alert.AlertType.ERROR);
            return false;
        }
        
        return true;
    }
    
    private void saveTaxpayer() {
        try {
            // Check if taxpayers table exists, create if not
            createTaxpayersTableIfNotExists();
            
            // Insert taxpayer
            String sql = "INSERT INTO taxpayers (tin, taxpayer_name, category, registration_date, " +
                       "phone_number, email, region, subcity_woreda, full_address, business_type, " +
                       "annual_turnover, business_license_no, license_issue_date, status, notes, " +
                       "created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
            
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, tinField.getText().trim());
                pstmt.setString(2, taxpayerNameField.getText().trim());
                pstmt.setString(3, categoryCombo.getValue());
                pstmt.setDate(4, Date.valueOf(registrationDatePicker.getValue()));
                
                pstmt.setString(5, phoneField.getText().trim().isEmpty() ? null : phoneField.getText().trim());
                pstmt.setString(6, emailField.getText().trim().isEmpty() ? null : emailField.getText().trim());
                pstmt.setString(7, regionCombo.getValue());
                pstmt.setString(8, woredaField.getText().trim().isEmpty() ? null : woredaField.getText().trim());
                pstmt.setString(9, addressField.getText().trim().isEmpty() ? null : addressField.getText().trim());
                pstmt.setString(10, businessTypeCombo.getValue());
                
                if (!annualTurnoverField.getText().trim().isEmpty()) {
                    pstmt.setDouble(11, Double.parseDouble(annualTurnoverField.getText().trim()));
                } else {
                    pstmt.setNull(11, Types.DOUBLE);
                }
                
                pstmt.setString(12, licenseNoField.getText().trim().isEmpty() ? null : licenseNoField.getText().trim());
                
                if (licenseDatePicker.getValue() != null) {
                    pstmt.setDate(13, Date.valueOf(licenseDatePicker.getValue()));
                } else {
                    pstmt.setNull(13, Types.DATE);
                }
                
                RadioButton selected = (RadioButton) statusToggleGroup.getSelectedToggle();
                pstmt.setString(14, selected.getText().toUpperCase());
                pstmt.setString(15, notesField.getText().trim().isEmpty() ? null : notesField.getText().trim());
                
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    showAlert("Success", "Taxpayer saved successfully!", Alert.AlertType.INFORMATION);
                    clearForm();
                } else {
                    showAlert("Error", "Failed to save taxpayer", Alert.AlertType.ERROR);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Save failed: " + e.getMessage());
            showAlert("Database Error", "Failed to save taxpayer: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void createTaxpayersTableIfNotExists() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS taxpayers (" +
                              "id INT AUTO_INCREMENT PRIMARY KEY," +
                              "tin VARCHAR(20) UNIQUE NOT NULL," +
                              "taxpayer_name VARCHAR(200) NOT NULL," +
                              "category VARCHAR(50)," +
                              "registration_date DATE," +
                              "phone_number VARCHAR(20)," +
                              "email VARCHAR(100)," +
                              "region VARCHAR(50)," +
                              "subcity_woreda VARCHAR(100)," +
                              "full_address TEXT," +
                              "business_type VARCHAR(100)," +
                              "annual_turnover DECIMAL(15,2)," +
                              "business_license_no VARCHAR(50)," +
                              "license_issue_date DATE," +
                              "status VARCHAR(20) DEFAULT 'ACTIVE'," +
                              "notes TEXT," +
                              "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                              "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                              ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            LOGGER.info("Taxpayers table checked/created successfully");
        }
    }
    
    @FXML
    private void handlerclickclearBtn(ActionEvent event) {
        clearForm();
    }
    
    @FXML
    private void handlerclickcancelBtn(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
    
    private void clearForm() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Clear Form");
        confirm.setHeaderText("Clear all fields?");
        confirm.setContentText("Are you sure you want to clear all form fields?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            tinField.clear();
            taxpayerNameField.clear();
            categoryCombo.getSelectionModel().selectFirst();
            registrationDatePicker.setValue(LocalDate.now());
            phoneField.clear();
            emailField.clear();
            regionCombo.getSelectionModel().selectFirst();
            woredaField.clear();
            addressField.clear();
            businessTypeCombo.getSelectionModel().selectFirst();
            annualTurnoverField.clear();
            licenseNoField.clear();
            licenseDatePicker.setValue(LocalDate.now());
            notesField.clear();
            activeStatusRadio.setSelected(true);
            
            tinField.requestFocus();
        }
    }
    
    private void showAlert(String title, String content, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    public void cleanup() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed");
            }
        } catch (SQLException e) {
            LOGGER.warning("Error closing connection: " + e.getMessage());
        }
    }
}