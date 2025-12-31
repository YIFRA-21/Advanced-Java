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
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.logging.Logger;

public class AuditReportController {

    private static final Logger LOGGER = Logger.getLogger(AuditReportController.class.getName());
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    
    private Connection connection;
    private ObservableList<AuditLog> auditLogs;
    
    // FXML elements
    @FXML private Button ExportLogsbtn;
    @FXML private ComboBox<String> activityTypeCombo;
    @FXML private TableView<AuditLog> auditLogTable;
    @FXML private Button backButtonReportpage;
    @FXML private Button clearfiltersbtn;
    @FXML private Label dataChangesLabel;
    @FXML private DatePicker endDatePicker;
    @FXML private Button generatereportbtn;
    @FXML private Label loginEventsLabel;
    @FXML private ComboBox<String> moduleCombo;
    @FXML private Label securityEventsLabel;
    @FXML private DatePicker startDatePicker;
    @FXML private Label totalActivitiesLabel;
    @FXML private ComboBox<String> userCombo;
    @FXML private VBox root; // Added root reference
    
    // Additional UI elements (if present in FXML)
    @FXML private Label pageTitleLabel;
    @FXML private Label pageDescriptionLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private TextField searchField;
    
    // Audit log table columns
    @FXML private TableColumn<AuditLog, String> timestampColumn;
    @FXML private TableColumn<AuditLog, String> userColumn;
    @FXML private TableColumn<AuditLog, String> actionColumn;
    @FXML private TableColumn<AuditLog, String> moduleColumn;
    @FXML private TableColumn<AuditLog, String> descriptionColumn;
    @FXML private TableColumn<AuditLog, String> ipColumn;
    @FXML private TableColumn<AuditLog, String> statusColumn;
    
    public static class AuditLog {
        private final LocalDateTime timestamp;
        private final String userId;
        private final String action;
        private final String module;
        private final String description;
        private final String ipAddress;
        private final String status;
        private final String details;
        
        public AuditLog(LocalDateTime timestamp, String userId, String action, String module,
                       String description, String ipAddress, String status, String details) {
            this.timestamp = timestamp;
            this.userId = userId;
            this.action = action;
            this.module = module;
            this.description = description;
            this.ipAddress = ipAddress;
            this.status = status;
            this.details = details;
        }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getUserId() { return userId; }
        public String getAction() { return action; }
        public String getModule() { return module; }
        public String getDescription() { return description; }
        public String getIpAddress() { return ipAddress; }
        public String getStatus() { return status; }
        public String getDetails() { return details; }
        public String getFormattedTimestamp() { 
            return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); 
        }
    }
    
    @FXML
    public void initialize() {
        try {
            LOGGER.info("Initializing AuditReportController");
            
            initializeDatabase();
            setupUIComponents();
            loadComboBoxData();
            setupEventListeners();
            
            // Set default dates
            if (startDatePicker != null) {
                startDatePicker.setValue(LocalDate.now().minusDays(7));
            }
            if (endDatePicker != null) {
                endDatePicker.setValue(LocalDate.now());
            }
            
            System.out.println("AuditReportController initialized successfully");
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Initialization Error", "Error initializing controller: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    private void initializeDatabase() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        LOGGER.info("Database connected successfully for audit report");
        
        // Create audit log table if not exists
        createAuditLogTable();
    }
    
    private void createAuditLogTable() {
        String sql = "CREATE TABLE IF NOT EXISTS audit_log (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "user_id VARCHAR(50) NOT NULL," +
                    "action VARCHAR(50) NOT NULL," +
                    "module VARCHAR(50) NOT NULL," +
                    "description VARCHAR(255)," +
                    "ip_address VARCHAR(45)," +
                    "status VARCHAR(20) DEFAULT 'SUCCESS'," +
                    "details TEXT," +
                    "INDEX idx_timestamp (timestamp)," +
                    "INDEX idx_user (user_id)," +
                    "INDEX idx_action (action)," +
                    "INDEX idx_module (module)" +
                    ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            LOGGER.info("Audit log table verified/created");
        } catch (SQLException e) {
            LOGGER.warning("Could not create audit log table: " + e.getMessage());
        }
    }
    
    private void setupUIComponents() {
        try {
            // Setup table columns if they're defined in FXML
            setupTableView();
            
            // Set default activity type
            if (activityTypeCombo != null) {
                activityTypeCombo.setValue("All Activities");
            }
            
            // Set default module
            if (moduleCombo != null) {
                moduleCombo.setValue("All Modules");
            }
            
            // Setup search field
            if (searchField != null) {
                searchField.setPromptText("Search audit logs...");
            }
            
            // Setup page info if labels exist
            if (pageTitleLabel != null) {
                pageTitleLabel.setText("Audit Report");
                pageTitleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            }
            
            if (pageDescriptionLabel != null) {
                pageDescriptionLabel.setText("System audit and activity logs");
                pageDescriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            }
            
        } catch (Exception e) {
            System.err.println("Error setting up UI components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTableView() {
        try {
            // If columns are defined in FXML, setup cell value factories
            if (timestampColumn != null) {
                timestampColumn.setCellValueFactory(new PropertyValueFactory<>("formattedTimestamp"));
            }
            if (userColumn != null) {
                userColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
            }
            if (actionColumn != null) {
                actionColumn.setCellValueFactory(new PropertyValueFactory<>("action"));
            }
            if (moduleColumn != null) {
                moduleColumn.setCellValueFactory(new PropertyValueFactory<>("module"));
            }
            if (descriptionColumn != null) {
                descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            }
            if (ipColumn != null) {
                ipColumn.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
            }
            if (statusColumn != null) {
                statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            }
            
            // Add context menu for table
            if (auditLogTable != null) {
                ContextMenu contextMenu = new ContextMenu();
                MenuItem viewDetailsItem = new MenuItem("View Details");
                viewDetailsItem.setOnAction(e -> viewAuditDetails());
                MenuItem exportItem = new MenuItem("Export Selected");
                exportItem.setOnAction(e -> exportSelectedLogs());
                contextMenu.getItems().addAll(viewDetailsItem, exportItem);
                auditLogTable.setContextMenu(contextMenu);
            }
            
        } catch (Exception e) {
            System.err.println("Error setting up table view: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadComboBoxData() {
        try {
            // Activity types
            ObservableList<String> activityTypes = FXCollections.observableArrayList(
                "All Activities", "Login", "Logout", "Create", "Update", "Delete", 
                "View", "Export", "Import", "Print", "System", "Security", "Error"
            );
            if (activityTypeCombo != null) {
                activityTypeCombo.setItems(activityTypes);
            }
            
            // Modules
            ObservableList<String> modules = FXCollections.observableArrayList(
                "All Modules", "Authentication", "Taxpayer Management", "Tax Returns", 
                "Payments", "Reports", "System Administration", "User Management", 
                "Audit", "Compliance", "Notifications"
            );
            if (moduleCombo != null) {
                moduleCombo.setItems(modules);
            }
            
            // Load users in background
            Task<ObservableList<String>> loadUsersTask = new Task<ObservableList<String>>() {
                @Override
                protected ObservableList<String> call() throws Exception {
                    ObservableList<String> users = FXCollections.observableArrayList("All Users");
                    String query = "SELECT DISTINCT user_id FROM audit_log WHERE user_id IS NOT NULL ORDER BY user_id";
                    
                    try (Statement stmt = connection.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {
                        while (rs.next()) {
                            users.add(rs.getString("user_id"));
                        }
                    } catch (SQLException e) {
                        LOGGER.warning("Error loading users: " + e.getMessage());
                    }
                    return users;
                }
            };
            
            loadUsersTask.setOnSucceeded(e -> {
                if (userCombo != null) {
                    userCombo.setItems(loadUsersTask.getValue());
                    userCombo.setValue("All Users");
                }
            });
            
            Thread userThread = new Thread(loadUsersTask);
            userThread.setDaemon(true);
            userThread.start();
            
        } catch (Exception e) {
            System.err.println("Error loading combo box data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupEventListeners() {
        try {
            // Date validation
            if (startDatePicker != null && endDatePicker != null) {
                startDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && endDatePicker.getValue() != null && 
                        newVal.isAfter(endDatePicker.getValue())) {
                        showAlert("Date Error", "Start date cannot be after end date", Alert.AlertType.WARNING);
                        startDatePicker.setValue(oldVal);
                    }
                });
                
                endDatePicker.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && startDatePicker.getValue() != null && 
                        newVal.isBefore(startDatePicker.getValue())) {
                        showAlert("Date Error", "End date cannot be before start date", Alert.AlertType.WARNING);
                        endDatePicker.setValue(oldVal);
                    }
                });
            }
            
            // Search field listener
            if (searchField != null) {
                searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                    filterAuditLogs();
                });
            }
        } catch (Exception e) {
            System.err.println("Error setting up event listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== EVENT HANDLERS ====================
    
    @FXML
    void handlerclickgeneratereportbtn(ActionEvent event) {
        if (validateInputs()) {
            generateAuditReport();
        }
    }
    
    private boolean validateInputs() {
        if (startDatePicker == null || startDatePicker.getValue() == null) {
            showAlert("Validation Error", "Start date is required", Alert.AlertType.ERROR);
            if (startDatePicker != null) startDatePicker.requestFocus();
            return false;
        }
        
        if (endDatePicker == null || endDatePicker.getValue() == null) {
            showAlert("Validation Error", "End date is required", Alert.AlertType.ERROR);
            if (endDatePicker != null) endDatePicker.requestFocus();
            return false;
        }
        
        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            showAlert("Validation Error", "Start date cannot be after end date", Alert.AlertType.ERROR);
            return false;
        }
        
        return true;
    }
    
    private void generateAuditReport() {
        Task<AuditReportData> reportTask = new Task<AuditReportData>() {
            @Override
            protected AuditReportData call() throws Exception {
                LocalDate startDate = startDatePicker.getValue();
                LocalDate endDate = endDatePicker.getValue();
                String activityType = activityTypeCombo.getValue();
                String module = moduleCombo.getValue();
                String user = userCombo.getValue();
                
                return generateAuditData(startDate, endDate, activityType, module, user);
            }
            
            @Override
            protected void succeeded() {
                AuditReportData data = getValue();
                updateAuditStatistics(data);
                updateAuditTable(data);
                
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                if (generatereportbtn != null) {
                    generatereportbtn.setText("Generate Report");
                }
                
                // Enable action buttons
                if (ExportLogsbtn != null) ExportLogsbtn.setDisable(false);
                if (clearfiltersbtn != null) clearfiltersbtn.setDisable(false);
                
                LOGGER.info("Audit report generated successfully");
            }
            
            @Override
            protected void failed() {
                showAlert("Report Generation Failed", 
                         "Error: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
                if (loadingIndicator != null) {
                    loadingIndicator.setVisible(false);
                }
                if (generatereportbtn != null) {
                    generatereportbtn.setText("Generate Report");
                }
            }
        };
        
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        if (generatereportbtn != null) {
            generatereportbtn.setText("Generating...");
        }
        
        Thread reportThread = new Thread(reportTask);
        reportThread.setDaemon(true);
        reportThread.start();
    }
    
    private AuditReportData generateAuditData(LocalDate startDate, LocalDate endDate, 
                                             String activityType, String module, String user) {
        AuditReportData data = new AuditReportData();
        
        try {
            // Build query
            StringBuilder query = new StringBuilder();
            query.append("SELECT * FROM audit_log ");
            query.append("WHERE timestamp BETWEEN ? AND ? ");
            
            if (!"All Activities".equals(activityType)) {
                query.append("AND action = ? ");
            }
            if (!"All Modules".equals(module)) {
                query.append("AND module = ? ");
            }
            if (!"All Users".equals(user)) {
                query.append("AND user_id = ? ");
            }
            
            query.append("ORDER BY timestamp DESC ");
            query.append("LIMIT 1000");
            
            try (PreparedStatement pstmt = connection.prepareStatement(query.toString())) {
                pstmt.setTimestamp(1, Timestamp.valueOf(startDate.atStartOfDay()));
                pstmt.setTimestamp(2, Timestamp.valueOf(endDate.atTime(23, 59, 59)));
                
                int paramIndex = 3;
                if (!"All Activities".equals(activityType)) {
                    pstmt.setString(paramIndex++, activityType);
                }
                if (!"All Modules".equals(module)) {
                    pstmt.setString(paramIndex++, module);
                }
                if (!"All Users".equals(user)) {
                    pstmt.setString(paramIndex++, user);
                }
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    AuditLog log = new AuditLog(
                        rs.getTimestamp("timestamp").toLocalDateTime(),
                        rs.getString("user_id"),
                        rs.getString("action"),
                        rs.getString("module"),
                        rs.getString("description"),
                        rs.getString("ip_address"),
                        rs.getString("status"),
                        rs.getString("details")
                    );
                    data.auditLogs.add(log);
                    
                    // Categorize activity
                    categorizeActivity(data, log);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Error generating audit data: " + e.getMessage());
            throw new RuntimeException("Database error: " + e.getMessage());
        }
        
        return data;
    }
    
    private void categorizeActivity(AuditReportData data, AuditLog log) {
        data.totalActivities++;
        
        String action = log.getAction().toUpperCase();
        if (action.contains("LOGIN") || action.contains("LOGOUT")) {
            data.loginEvents++;
        } else if (action.contains("CREATE") || action.contains("UPDATE") || action.contains("DELETE")) {
            data.dataChanges++;
        } else if (action.contains("SECURITY") || action.contains("UNAUTHORIZED") || action.contains("PASSWORD")) {
            data.securityEvents++;
        }
    }
    
    private void updateAuditStatistics(AuditReportData data) {
        if (totalActivitiesLabel != null) totalActivitiesLabel.setText(String.valueOf(data.totalActivities));
        if (loginEventsLabel != null) loginEventsLabel.setText(String.valueOf(data.loginEvents));
        if (dataChangesLabel != null) dataChangesLabel.setText(String.valueOf(data.dataChanges));
        if (securityEventsLabel != null) securityEventsLabel.setText(String.valueOf(data.securityEvents));
    }
    
    private void updateAuditTable(AuditReportData data) {
        auditLogs = FXCollections.observableArrayList(data.auditLogs);
        if (auditLogTable != null) {
            auditLogTable.setItems(auditLogs);
        }
    }
    
    private void filterAuditLogs() {
        if (auditLogs == null || searchField == null) return;
        
        String searchText = searchField.getText().toLowerCase();
        
        ObservableList<AuditLog> filtered = FXCollections.observableArrayList();
        for (AuditLog log : auditLogs) {
            if (log.getUserId().toLowerCase().contains(searchText) ||
                log.getAction().toLowerCase().contains(searchText) ||
                log.getModule().toLowerCase().contains(searchText) ||
                (log.getDescription() != null && log.getDescription().toLowerCase().contains(searchText)) ||
                (log.getIpAddress() != null && log.getIpAddress().toLowerCase().contains(searchText))) {
                filtered.add(log);
            }
        }
        
        if (auditLogTable != null) {
            auditLogTable.setItems(filtered);
        }
        if (totalActivitiesLabel != null) {
            totalActivitiesLabel.setText(String.valueOf(filtered.size()));
        }
    }
    
    @FXML
    void handlerclickExportLogsbtn(ActionEvent event) {
        if (auditLogs == null || auditLogs.isEmpty()) {
            showAlert("Export Error", "No audit logs to export", Alert.AlertType.WARNING);
            return;
        }
        
        ChoiceDialog<String> formatDialog = new ChoiceDialog<>("CSV", "CSV", "Excel", "PDF", "JSON");
        formatDialog.setTitle("Export Format");
        formatDialog.setHeaderText("Select export format:");
        formatDialog.setContentText("Format:");
        
        Optional<String> result = formatDialog.showAndWait();
        result.ifPresent(format -> {
            Task<Boolean> exportTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    String fileName = "Audit_Logs_" + LocalDate.now() + "." + format.toLowerCase();
                    LOGGER.info("Exporting audit logs as " + format + ": " + fileName);
                    
                    // Simulate export
                    Thread.sleep(2000);
                    return true;
                }
                
                @Override
                protected void succeeded() {
                    showAlert("Export Successful", 
                             "Audit logs exported as " + format + " successfully!", 
                             Alert.AlertType.INFORMATION);
                }
                
                @Override
                protected void failed() {
                    showAlert("Export Failed", 
                             "Failed to export audit logs: " + getException().getMessage(), 
                             Alert.AlertType.ERROR);
                }
            };
            
            Thread exportThread = new Thread(exportTask);
            exportThread.setDaemon(true);
            exportThread.start();
        });
    }
    
    @FXML
    void handlerclickclearfiltersbtn(ActionEvent event) {
        try {
            if (startDatePicker != null) {
                startDatePicker.setValue(LocalDate.now().minusDays(7));
            }
            if (endDatePicker != null) {
                endDatePicker.setValue(LocalDate.now());
            }
            if (activityTypeCombo != null) {
                activityTypeCombo.setValue("All Activities");
            }
            if (moduleCombo != null) {
                moduleCombo.setValue("All Modules");
            }
            if (userCombo != null) {
                userCombo.setValue("All Users");
            }
            if (searchField != null) {
                searchField.clear();
            }
            
            if (auditLogs != null && auditLogTable != null) {
                auditLogTable.setItems(auditLogs);
            }
            if (totalActivitiesLabel != null && auditLogs != null) {
                totalActivitiesLabel.setText(String.valueOf(auditLogs.size()));
            }
            
            LOGGER.info("Filters cleared");
        } catch (Exception e) {
            System.err.println("Error clearing filters: " + e.getMessage());
        }
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
    
    
    private void viewAuditDetails() {
        if (auditLogTable == null) return;
        
        AuditLog selected = auditLogTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("View Details", "Please select an audit log entry", Alert.AlertType.WARNING);
            return;
        }
        
        StringBuilder details = new StringBuilder();
        details.append("Audit Log Details\n");
        details.append("=================\n");
        details.append("Timestamp: ").append(selected.getFormattedTimestamp()).append("\n");
        details.append("User ID: ").append(selected.getUserId()).append("\n");
        details.append("Action: ").append(selected.getAction()).append("\n");
        details.append("Module: ").append(selected.getModule()).append("\n");
        details.append("Description: ").append(selected.getDescription()).append("\n");
        details.append("IP Address: ").append(selected.getIpAddress()).append("\n");
        details.append("Status: ").append(selected.getStatus()).append("\n");
        details.append("Details: ").append(selected.getDetails()).append("\n");
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Audit Log Details");
        alert.setHeaderText("Detailed Audit Information");
        alert.setContentText(details.toString());
        alert.getDialogPane().setPrefSize(600, 400);
        alert.showAndWait();
    }
    
    private void exportSelectedLogs() {
        if (auditLogTable == null) return;
        
        ObservableList<AuditLog> selectedLogs = auditLogTable.getSelectionModel().getSelectedItems();
        if (selectedLogs.isEmpty()) {
            showAlert("Export Selected", "Please select audit logs to export", Alert.AlertType.WARNING);
            return;
        }
        
        Task<Boolean> exportTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                String fileName = "Selected_Audit_Logs_" + LocalDate.now() + ".csv";
                LOGGER.info("Exporting " + selectedLogs.size() + " selected audit logs: " + fileName);
                
                // Simulate export
                Thread.sleep(1000);
                return true;
            }
            
            @Override
            protected void succeeded() {
                showAlert("Export Successful", 
                         selectedLogs.size() + " audit logs exported successfully!", 
                         Alert.AlertType.INFORMATION);
            }
            
            @Override
            protected void failed() {
                showAlert("Export Failed", 
                         "Failed to export selected logs: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
            }
        };
        
        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
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
    
    // Data class for audit report
    private static class AuditReportData {
        int totalActivities = 0;
        int loginEvents = 0;
        int dataChanges = 0;
        int securityEvents = 0;
        ObservableList<AuditLog> auditLogs = FXCollections.observableArrayList();
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