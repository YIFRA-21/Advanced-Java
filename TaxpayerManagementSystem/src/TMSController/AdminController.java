package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import java.sql.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;

public class AdminController {

    // Header button
    @FXML private Button backToDashboardBtn;
    
    // Panel buttons
    @FXML private Button manageUsersBtn;
    @FXML private Button systemSettingsBtn;
    @FXML private Button backupRecoveryBtn;
    
    // User management fields - Updated to match FXML
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button showPasswordBtn;
    @FXML private ComboBox<String> roleCombo;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> departmentCombo;
    @FXML private Button addUserBtn;
    @FXML private Button clearFormBtn;
    @FXML private Button cancelBtn;
    
    // User list/search
    @FXML private TextField userSearchField;
    @FXML private Button searchBtn;
    @FXML private TableView<User> usersTable;
    
    // Table columns - Updated to match FXML
    @FXML private TableColumn<User, Integer> userIdColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> fullNameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, String> phoneColumn;
    @FXML private TableColumn<User, String> departmentColumn;
    @FXML private TableColumn<User, String> actionsColumn;
    
    // System configuration
    @FXML private ComboBox<String> systemLanguageCombo;
    @FXML private ComboBox<String> currencyCombo;
    @FXML private TextField taxRateA;
    @FXML private TextField taxRateB;
    @FXML private Button saveConfigBtn;
    
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private Connection connection;
    private final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "Belay2123";
    private boolean passwordVisible = false;

    @FXML
    public void initialize() {
        try {
            System.out.println("AdminController initialize() called");
            
            // Initialize database connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            // Initialize combo boxes
            initializeComboBoxes();
            
            // Initialize table columns
            initializeTableColumns();
            
            // Load users from database
            loadUsersFromDatabase();
            
            // Load system configuration
            loadSystemConfiguration();
            
            System.out.println("AdminController initialized successfully");
            
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), AlertType.ERROR);
        }
    }

    // ==================== EVENT HANDLERS ====================
    // These must match onAction="#methodName" in FXML

    @FXML
    private void handleBackToDashboard(ActionEvent event) {
  	  System.out.println("Back to Dashboard clicked");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Dashboard.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
            
        } catch (Exception e) {
        	System.out.println("Failed to navigate to dashboard: " + e.getMessage());
            showAlert("Navigation Error", 
                     "Failed to navigate to dashboard: " + e.getMessage(), 
                     Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleManageUsers(ActionEvent event) {
        System.out.println("Manage Users clicked");
        // Already on user management section
    }

    @FXML
    private void handleSystemSettings(ActionEvent event) {
        System.out.println("System Settings clicked");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/SystemSettings.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) systemSettingsBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("System Settings");
            stage.show();
            
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load system settings: " + e.getMessage(), AlertType.ERROR);
        }
    }

    @FXML
    private void handleBackupRecovery(ActionEvent event) {
        System.out.println("Backup & Recovery clicked");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/BackupRecovery.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) backupRecoveryBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Backup and Recovery");
            stage.show();
            
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load backup & recovery: " + e.getMessage(), AlertType.ERROR);
        }
    }

    @FXML
    private void handleShowPassword(ActionEvent event) {
        System.out.println("Show Password clicked");
        passwordVisible = !passwordVisible;
        
        if (passwordVisible) {
            // Show passwords
            showPasswordBtn.setText("🙈");
            String password = passwordField.getText();
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            // In a real app, you'd show the password in a visible field
        } else {
            // Hide passwords
            showPasswordBtn.setText("👁");
            passwordField.setVisible(true);
            passwordField.setManaged(true);
        }
    }

    @FXML
    private void handleAddUser(ActionEvent event) {
        System.out.println("Add User clicked");
        if (validateUserInput()) {
            try {
                String query = "INSERT INTO users (first_name, last_name, username, password, email, phone_number, role, status, department, is_active) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, firstNameField.getText().trim());
                stmt.setString(2, lastNameField.getText().trim());
                stmt.setString(3, usernameField.getText().trim());
                stmt.setString(4, hashPassword(passwordField.getText()));
                stmt.setString(5, emailField.getText().trim());
                stmt.setString(6, phoneField.getText().trim());
                stmt.setString(7, roleCombo.getValue());
                stmt.setString(8, statusCombo.getValue());
                stmt.setString(9, departmentCombo.getValue());
                stmt.setBoolean(10, "Active".equals(statusCombo.getValue()));
                
                int rows = stmt.executeUpdate();
                
                if (rows > 0) {
                    ResultSet rs = stmt.getGeneratedKeys();
                    if (rs.next()) {
                        int newUserId = rs.getInt(1);
                        
                        // Add to table
                        User newUser = new User(
                            newUserId,
                            usernameField.getText().trim(),
                            firstNameField.getText().trim() + " " + lastNameField.getText().trim(),
                            emailField.getText().trim(),
                            roleCombo.getValue(),
                            statusCombo.getValue(),
                            departmentCombo.getValue(),
                            phoneField.getText().trim()
                        );
                        
                        userList.add(newUser);
                        clearForm();
                        showAlert("Success", "User added successfully!", AlertType.INFORMATION);
                    }
                    rs.close();
                }
                stmt.close();
                
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to add user: " + e.getMessage(), AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleClearForm(ActionEvent event) {
        System.out.println("Clear Form clicked");
        clearForm();
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        System.out.println("Cancel clicked");
        clearForm();
    }

    @FXML
    private void handleSearch(ActionEvent event) {
        System.out.println("Search clicked");
        String searchTerm = userSearchField.getText().trim();
        if (!searchTerm.isEmpty()) {
            try {
                String query = "SELECT * FROM users WHERE user_id = ? OR username LIKE ? OR email LIKE ? OR first_name LIKE ? OR last_name LIKE ?";
                PreparedStatement stmt = connection.prepareStatement(query);
                stmt.setString(1, searchTerm);
                String likeTerm = "%" + searchTerm + "%";
                for (int i = 2; i <= 5; i++) {
                    stmt.setString(i, likeTerm);
                }
                
                ResultSet rs = stmt.executeQuery();
                userList.clear();
                
                while (rs.next()) {
                    User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("status"),
                        rs.getString("department"),
                        rs.getString("phone_number")
                    );
                    userList.add(user);
                }
                rs.close();
                stmt.close();
                
            } catch (SQLException e) {
                showAlert("Database Error", "Search failed: " + e.getMessage(), AlertType.ERROR);
            }
        } else {
            loadUsersFromDatabase();
        }
    }

    @FXML
    private void handleSaveConfig(ActionEvent event) {
        System.out.println("Save Configuration clicked");
        saveSystemConfiguration();
    }

    // ==================== HELPER METHODS ====================

    private void initializeComboBoxes() {
        System.out.println("Initializing combo boxes...");
        
        // Initialize role combobox
        if (roleCombo != null) {
            roleCombo.getItems().addAll("Admin", "Manager", "Officer", "Viewer");
            roleCombo.setValue("Officer");
        }
        
        // Initialize status combobox
        if (statusCombo != null) {
            statusCombo.getItems().addAll("Active", "Inactive", "Suspended");
            statusCombo.setValue("Active");
        }
        
        // Initialize department combobox
        if (departmentCombo != null) {
            departmentCombo.getItems().addAll("Administration", "Finance", "IT", "HR", "Operations", "Tax Collection");
            departmentCombo.setValue("Administration");
        }
        
        // Initialize language combobox
        if (systemLanguageCombo != null) {
            systemLanguageCombo.getItems().addAll("English", "Amharic", "Oromiffa", "Tigrigna");
            systemLanguageCombo.setValue("English");
        }
        
        // Initialize currency combobox
        if (currencyCombo != null) {
            currencyCombo.getItems().addAll("ETB", "USD", "EUR", "GBP");
            currencyCombo.setValue("ETB");
        }
    }

    private void initializeTableColumns() {
        System.out.println("Initializing table columns...");
        
        if (userIdColumn != null) {
            userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        }
        
        if (usernameColumn != null) {
            usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        }
        
        if (fullNameColumn != null) {
            fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        }
        
        if (emailColumn != null) {
            emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        }
        
        if (roleColumn != null) {
            roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        }
        
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        }
        
        if (departmentColumn != null) {
            departmentColumn.setCellValueFactory(new PropertyValueFactory<>("department"));
        }
        
        if (phoneColumn != null) {
            phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phoneNumber"));
        }
        
        // Add action buttons to Actions column
        if (actionsColumn != null) {
            actionsColumn.setCellFactory(col -> new TableCell<User, String>() {
                private final Button editBtn = new Button("Edit");
                private final Button deleteBtn = new Button("Delete");
                
                {
                    editBtn.setOnAction(e -> {
                        User user = getTableView().getItems().get(getIndex());
                        editUser(user);
                    });
                    
                    deleteBtn.setOnAction(e -> {
                        User user = getTableView().getItems().get(getIndex());
                        deleteUser(user.getUserId());
                    });
                    
                    editBtn.setStyle("-fx-background-color: #2980B9; -fx-text-fill: white; -fx-padding: 3 8;");
                    deleteBtn.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; -fx-padding: 3 8;");
                }
                
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(5, editBtn, deleteBtn);
                        setGraphic(buttons);
                    }
                }
            });
        }
        
        if (usersTable != null) {
            usersTable.setItems(userList);
        }
    }

    private void loadUsersFromDatabase() {
        try {
            System.out.println("Loading users from database...");
            
            // Check if users table exists with the right columns
            String query = "SELECT * FROM users ORDER BY user_id DESC";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            userList.clear();
            while (rs.next()) {
                User user = new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("role"),
                    rs.getString("status"),
                    rs.getString("department"),
                    rs.getString("phone_number")
                );
                userList.add(user);
            }
            rs.close();
            stmt.close();
            
            System.out.println("Loaded " + userList.size() + " users from database");
            
        } catch (SQLException e) {
            System.err.println("Failed to load users: " + e.getMessage());
            showAlert("Database Error", "Failed to load users: " + e.getMessage(), AlertType.ERROR);
        }
    }

    private void loadSystemConfiguration() {
        try {
            System.out.println("Loading system configuration...");
            
            // Check if system_config table exists
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet tables = meta.getTables(null, null, "system_config", null);
            
            if (tables.next()) {
                // Table exists, load configuration
                String query = "SELECT * FROM system_config WHERE config_id = 1";
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                
                if (rs.next()) {
                    systemLanguageCombo.setValue(rs.getString("default_language"));
                    currencyCombo.setValue(rs.getString("default_currency"));
                    taxRateA.setText(String.valueOf(rs.getDouble("tax_rate_a")));
                    taxRateB.setText(String.valueOf(rs.getDouble("tax_rate_b")));
                }
                rs.close();
                stmt.close();
            } else {
                // Table doesn't exist, use default values
                System.out.println("system_config table not found, using defaults");
                systemLanguageCombo.setValue("English");
                currencyCombo.setValue("ETB");
                taxRateA.setText("10.0");
                taxRateB.setText("15.0");
            }
            tables.close();
            
        } catch (SQLException e) {
            System.err.println("Error loading system config: " + e.getMessage());
            // Use default values
            systemLanguageCombo.setValue("English");
            currencyCombo.setValue("ETB");
            taxRateA.setText("10.0");
            taxRateB.setText("15.0");
        }
    }

    private void saveSystemConfiguration() {
        try {
            // First, ensure the table exists
            String createTable = "CREATE TABLE IF NOT EXISTS system_config (" +
                               "config_id INT PRIMARY KEY, " +
                               "default_language VARCHAR(50), " +
                               "default_currency VARCHAR(10), " +
                               "tax_rate_a DOUBLE, " +
                               "tax_rate_b DOUBLE)";
            
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(createTable);
            
            // Insert or update configuration
            String query = "INSERT INTO system_config (config_id, default_language, default_currency, tax_rate_a, tax_rate_b) " +
                         "VALUES (1, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE default_language = ?, default_currency = ?, tax_rate_a = ?, tax_rate_b = ?";
            
            PreparedStatement pstmt = connection.prepareStatement(query);
            pstmt.setString(1, systemLanguageCombo.getValue());
            pstmt.setString(2, currencyCombo.getValue());
            pstmt.setDouble(3, Double.parseDouble(taxRateA.getText()));
            pstmt.setDouble(4, Double.parseDouble(taxRateB.getText()));
            pstmt.setString(5, systemLanguageCombo.getValue());
            pstmt.setString(6, currencyCombo.getValue());
            pstmt.setDouble(7, Double.parseDouble(taxRateA.getText()));
            pstmt.setDouble(8, Double.parseDouble(taxRateB.getText()));
            
            pstmt.executeUpdate();
            pstmt.close();
            stmt.close();
            
            showAlert("Success", "System configuration saved!", AlertType.INFORMATION);
            System.out.println("System configuration saved successfully");
            
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Failed to save configuration: " + e.getMessage());
            showAlert("Error", "Failed to save configuration: " + e.getMessage(), AlertType.ERROR);
        }
    }

    private boolean validateUserInput() {
        // Validate first name
        if (firstNameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "First name is required", AlertType.ERROR);
            firstNameField.requestFocus();
            return false;
        }
        
        // Validate last name
        if (lastNameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Last name is required", AlertType.ERROR);
            lastNameField.requestFocus();
            return false;
        }
        
        // Validate username
        if (usernameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Username is required", AlertType.ERROR);
            usernameField.requestFocus();
            return false;
        }
        
        // Validate password
        if (passwordField.getText().isEmpty()) {
            showAlert("Validation Error", "Password is required", AlertType.ERROR);
            passwordField.requestFocus();
            return false;
        }
        
        if (passwordField.getText().length() < 6) {
            showAlert("Validation Error", "Password must be at least 6 characters", AlertType.ERROR);
            passwordField.requestFocus();
            return false;
        }
        
        // Validate confirm password
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showAlert("Validation Error", "Passwords do not match", AlertType.ERROR);
            confirmPasswordField.requestFocus();
            return false;
        }
        
        // Validate email
        if (!isValidEmail(emailField.getText())) {
            showAlert("Validation Error", "Invalid email format", AlertType.ERROR);
            emailField.requestFocus();
            return false;
        }
        
        // Validate role
        if (roleCombo.getValue() == null) {
            showAlert("Validation Error", "Please select a role", AlertType.ERROR);
            roleCombo.requestFocus();
            return false;
        }
        
        // Validate status
        if (statusCombo.getValue() == null) {
            showAlert("Validation Error", "Please select a status", AlertType.ERROR);
            statusCombo.requestFocus();
            return false;
        }
        
        // Validate department
        if (departmentCombo.getValue() == null) {
            showAlert("Validation Error", "Please select a department", AlertType.ERROR);
            departmentCombo.requestFocus();
            return false;
        }
        
        return true;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private String hashPassword(String password) {
        // Simple hash for demonstration - in production use BCrypt
        return Integer.toString(password.hashCode());
    }

    private void clearForm() {
        firstNameField.clear();
        lastNameField.clear();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        emailField.clear();
        phoneField.clear();
        roleCombo.setValue(null);
        statusCombo.setValue(null);
        departmentCombo.setValue(null);
        
        // Reset to default values
        roleCombo.setValue("Officer");
        statusCombo.setValue("Active");
        departmentCombo.setValue("Administration");
    }

    private void editUser(User user) {
        System.out.println("Editing user: " + user.getUsername());
        
        // Populate form with user data
        String[] names = user.getFullName().split(" ", 2);
        firstNameField.setText(names[0]);
        lastNameField.setText(names.length > 1 ? names[1] : "");
        usernameField.setText(user.getUsername());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhoneNumber());
        roleCombo.setValue(user.getRole());
        statusCombo.setValue(user.getStatus());
        departmentCombo.setValue(user.getDepartment());
        
        // Store user ID for update (you might want to add a hidden field)
        showAlert("Edit Mode", "Editing user: " + user.getFullName(), AlertType.INFORMATION);
    }

    private void deleteUser(int userId) {
        Alert confirm = new Alert(AlertType.CONFIRMATION, 
            "Are you sure you want to delete this user?\nThis action cannot be undone.", 
            ButtonType.YES, ButtonType.NO);
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    String query = "UPDATE users SET is_active = false, status = 'Inactive' WHERE user_id = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setInt(1, userId);
                    stmt.executeUpdate();
                    stmt.close();
                    
                    // Remove from table
                    userList.removeIf(user -> user.getUserId() == userId);
                    showAlert("Success", "User deactivated successfully!", AlertType.INFORMATION);
                    
                } catch (SQLException e) {
                    showAlert("Database Error", "Failed to delete user: " + e.getMessage(), AlertType.ERROR);
                }
            }
        });
    }

    private void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
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

    // User model class
    public static class User {
        private final int userId;
        private final String username;
        private final String fullName;
        private final String email;
        private final String role;
        private final String status;
        private final String department;
        private final String phoneNumber;

        public User(int userId, String username, String fullName, String email, 
                   String role, String status, String department, String phoneNumber) {
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.status = status;
            this.department = department;
            this.phoneNumber = phoneNumber;
        }

        public int getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
        public String getDepartment() { return department; }
        public String getPhoneNumber() { return phoneNumber; }
    }
}