package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert.AlertType;
import java.sql.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AuditController {

    @FXML private ComboBox<String> actionTypeFilter;
    @FXML private Button applyFilterBtn;
    @FXML private DatePicker auditEndDate;
    @FXML private Pagination auditPagination;
    @FXML private DatePicker auditStartDate;
    @FXML private TableView<AuditLog> auditTable;
    @FXML private Button clearOldLogsBtn;
    @FXML private TableColumn<AuditLog, String> columonAction;
    @FXML private TableColumn<AuditLog, String> columonDetails;
    @FXML private TableColumn<AuditLog, String> columonIpAdress;
    @FXML private TableColumn<AuditLog, String> columonModule;
    @FXML private TableColumn<AuditLog, String> columonTimestamp;
    @FXML private TableColumn<AuditLog, String> columonUser;
    @FXML private TableColumn<AuditLog, String> columonstatus;
    @FXML private TableColumn<AuditLog, String> columonView;
    @FXML private Button exportAuditBtn;
    @FXML private Label logCountLabel;
    @FXML private Label loginLogsLabel;
    @FXML private Button newTaxpayerBtn1;
    @FXML private Button refreshAuditBtn;
    @FXML private Button resetFilterBtn;
    @FXML private Label securityLogsLabel;
    @FXML private Label todaysLogsLabel;
    @FXML private Label totalLogsLabel;
    @FXML private TextField txtipAddressFilter;
    @FXML private ComboBox<String> txtuserFilter;
    @FXML private VBox root;  // Added root reference
    
    private ObservableList<AuditLog> auditLogList = FXCollections.observableArrayList();
    private ObservableList<String> userList = FXCollections.observableArrayList();
    private ObservableList<String> actionTypeList = FXCollections.observableArrayList();
    private Connection connection;
    private final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "Belay2123";
    private final int PAGE_SIZE = 50;
    
    @FXML
    public void initialize() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            initializeComponents();
            loadStatistics();
            loadAuditLogs();
            
            System.out.println("AuditController initialized successfully");
            
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Initialization Error", "Error initializing controller: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    private void initializeComponents() {
        try {
            // Initialize date pickers
            if (auditStartDate != null) {
                auditStartDate.setValue(LocalDate.now().minusDays(7));
            }
            if (auditEndDate != null) {
                auditEndDate.setValue(LocalDate.now());
            }
            
            // Initialize filter dropdowns
            initializeFilterDropdowns();
            
            // Initialize table columns
            initializeTableColumns();
            
            // Initialize pagination
            initializePagination();
            
            // Load initial data
            loadUserFilterOptions();
            loadActionTypeFilterOptions();
            
        } catch (Exception e) {
            System.err.println("Error initializing components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializeFilterDropdowns() {
        try {
            // User filter
            if (txtuserFilter != null) {
                txtuserFilter.getItems().add("All Users");
                txtuserFilter.setValue("All Users");
            }
            
            // Action type filter
            if (actionTypeFilter != null) {
                actionTypeFilter.getItems().add("All Actions");
                actionTypeFilter.setValue("All Actions");
            }
        } catch (Exception e) {
            System.err.println("Error initializing filter dropdowns: " + e.getMessage());
        }
    }
    
    private void initializeTableColumns() {
        try {
            // Set column factories
            if (columonTimestamp != null) {
                columonTimestamp.setCellValueFactory(new PropertyValueFactory<>("formattedTimestamp"));
            }
            if (columonUser != null) {
                columonUser.setCellValueFactory(new PropertyValueFactory<>("username"));
            }
            if (columonAction != null) {
                columonAction.setCellValueFactory(new PropertyValueFactory<>("action"));
            }
            if (columonModule != null) {
                columonModule.setCellValueFactory(new PropertyValueFactory<>("module"));
            }
            if (columonDetails != null) {
                columonDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
            }
            if (columonIpAdress != null) {
                columonIpAdress.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
            }
            if (columonstatus != null) {
                columonstatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            }
            
            // View column with button
            if (auditTable != null) {
                TableColumn<AuditLog, Void> viewColumn = new TableColumn<>("View");
                viewColumn.setPrefWidth(80);
                
                viewColumn.setCellFactory(col -> new TableCell<AuditLog, Void>() {
                    private final Button viewBtn = new Button("View");
                    
                    {
                        viewBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3 8 3 8;");
                        viewBtn.setOnAction(e -> {
                            AuditLog log = getTableView().getItems().get(getIndex());
                            if (log != null) {
                                viewLogDetails(log);
                            }
                        });
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(viewBtn);
                        }
                    }
                });
                
                // Add the column to the table if not already there
                if (!auditTable.getColumns().contains(viewColumn)) {
                    auditTable.getColumns().add(viewColumn);
                }
                
                auditTable.setItems(auditLogList);
            }
            
        } catch (Exception e) {
            System.err.println("Error initializing table columns: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void initializePagination() {
        try {
            if (auditPagination != null) {
                auditPagination.setPageCount(1);
                auditPagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
                    loadAuditLogs();
                });
            }
        } catch (Exception e) {
            System.err.println("Error initializing pagination: " + e.getMessage());
        }
    }
    
    private void loadUserFilterOptions() {
        try {
            if (txtuserFilter == null) return;
            
            String query = "SELECT DISTINCT username FROM audit_log WHERE username IS NOT NULL AND username != '' ORDER BY username";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            txtuserFilter.getItems().clear();
            txtuserFilter.getItems().add("All Users");
            
            while (rs.next()) {
                txtuserFilter.getItems().add(rs.getString("username"));
            }
            
            txtuserFilter.setValue("All Users");
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            // Table might not exist yet
            System.out.println("Note: audit_log table might not exist yet: " + e.getMessage());
            createAuditLogTable();
        }
    }
    
    private void loadActionTypeFilterOptions() {
        try {
            if (actionTypeFilter == null) return;
            
            String query = "SELECT DISTINCT action FROM audit_log WHERE action IS NOT NULL AND action != '' ORDER BY action";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            actionTypeFilter.getItems().clear();
            actionTypeFilter.getItems().add("All Actions");
            
            while (rs.next()) {
                actionTypeFilter.getItems().add(rs.getString("action"));
            }
            
            actionTypeFilter.setValue("All Actions");
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            // Table might not exist yet
            System.out.println("Note: Could not load action types: " + e.getMessage());
        }
    }
    
    @FXML
    void handelerclickexportAuditBtn(ActionEvent event) {
        navigateToExportLogs();
    }
    
    @FXML
    void handlerclickactionTypeFilter(ActionEvent event) {
        // Action type filter changed - no action needed until apply is clicked
    }
    
    @FXML
    void handlerclickapplyFilterBtn(ActionEvent event) {
        loadAuditLogs();
    }
    
    @FXML
    void handlerclickauditStartDate(ActionEvent event) {
        // Start date changed - no action needed until apply is clicked
    }
    
    @FXML
    void handlerclickclearOldLogsBtn(ActionEvent event) {
        clearOldLogs();
    }
    
    @FXML
    void handlerclicknewTaxpayerBtn1(ActionEvent event) {
        navigateToDashboard();
    }
    
    @FXML
    void handlerclickrefreshAuditBtn(ActionEvent event) {
        refreshAuditData();
    }
    
    @FXML
    void handlerclickresetFilterBtn(ActionEvent event) {
        resetFilters();
    }
    
    @FXML
    void handlerclicktxtuserFilter(ActionEvent event) {
        // User filter changed - no action needed until apply is clicked
    }
    
    private void loadStatistics() {
        try {
            // Create audit_log table if it doesn't exist
            createAuditLogTable();
            
            // Total logs
            String totalQuery = "SELECT COUNT(*) as total FROM audit_log";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(totalQuery);
            if (rs.next() && totalLogsLabel != null) {
                totalLogsLabel.setText(String.format("%,d", rs.getInt("total")));
            }
            
            // Today's logs
            String todayQuery = "SELECT COUNT(*) as today FROM audit_log WHERE DATE(created_at) = CURDATE()";
            rs = stmt.executeQuery(todayQuery);
            if (rs.next() && todaysLogsLabel != null) {
                todaysLogsLabel.setText(String.format("%,d", rs.getInt("today")));
            }
            
            // Login logs (assuming login action)
            String loginQuery = "SELECT COUNT(*) as login FROM audit_log WHERE action LIKE '%login%' OR action LIKE '%logout%' OR action LIKE '%LOGIN%' OR action LIKE '%LOGOUT%'";
            rs = stmt.executeQuery(loginQuery);
            if (rs.next() && loginLogsLabel != null) {
                loginLogsLabel.setText(String.format("%,d", rs.getInt("login")));
            }
            
            // Security events (failed logins, unauthorized access)
            String securityQuery = "SELECT COUNT(*) as security FROM audit_log WHERE action LIKE '%failed%' OR action LIKE '%unauthorized%' OR action LIKE '%violation%' OR action LIKE '%FAILED%' OR action LIKE '%UNAUTHORIZED%'";
            rs = stmt.executeQuery(securityQuery);
            if (rs.next() && securityLogsLabel != null) {
                securityLogsLabel.setText(String.format("%,d", rs.getInt("security")));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error loading statistics: " + e.getMessage());
        }
    }
    
    private void loadAuditLogs() {
        try {
            if (auditTable == null) return;
            
            StringBuilder query = new StringBuilder("SELECT * FROM audit_log WHERE 1=1");
            
            // Apply date filters
            if (auditStartDate != null && auditStartDate.getValue() != null) {
                query.append(" AND DATE(created_at) >= '").append(auditStartDate.getValue()).append("'");
            }
            
            if (auditEndDate != null && auditEndDate.getValue() != null) {
                query.append(" AND DATE(created_at) <= '").append(auditEndDate.getValue()).append("'");
            }
            
            // Apply user filter
            if (txtuserFilter != null) {
                String userFilter = txtuserFilter.getValue();
                if (userFilter != null && !"All Users".equals(userFilter) && !userFilter.trim().isEmpty()) {
                    query.append(" AND username = '").append(userFilter).append("'");
                }
            }
            
            // Apply action type filter
            if (actionTypeFilter != null) {
                String actionFilter = actionTypeFilter.getValue();
                if (actionFilter != null && !"All Actions".equals(actionFilter) && !actionFilter.trim().isEmpty()) {
                    query.append(" AND action = '").append(actionFilter).append("'");
                }
            }
            
            // Apply IP address filter
            if (txtipAddressFilter != null) {
                String ipFilter = txtipAddressFilter.getText().trim();
                if (!ipFilter.isEmpty()) {
                    query.append(" AND ip_address LIKE '%").append(ipFilter).append("%'");
                }
            }
            
            // Count total records
            String countQuery = "SELECT COUNT(*) as total FROM (" + query.toString() + ") as filtered";
            Statement countStmt = connection.createStatement();
            ResultSet countRs = countStmt.executeQuery(countQuery);
            
            int totalRecords = 0;
            if (countRs.next()) {
                totalRecords = countRs.getInt("total");
            }
            countRs.close();
            countStmt.close();
            
            // Update pagination
            if (auditPagination != null) {
                int totalPages = (int) Math.ceil((double) totalRecords / PAGE_SIZE);
                auditPagination.setPageCount(Math.max(1, totalPages));
                
                // Apply pagination
                int currentPage = auditPagination.getCurrentPageIndex();
                int offset = currentPage * PAGE_SIZE;
                query.append(" ORDER BY created_at DESC LIMIT ").append(PAGE_SIZE).append(" OFFSET ").append(offset);
            } else {
                query.append(" ORDER BY created_at DESC LIMIT ").append(PAGE_SIZE);
            }
            
            // Execute main query
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query.toString());
            
            auditLogList.clear();
            while (rs.next()) {
                AuditLog log = new AuditLog(
                    rs.getInt("log_id"),
                    rs.getTimestamp("created_at"),
                    rs.getString("username"),
                    rs.getString("action"),
                    rs.getString("module"),
                    rs.getString("description"),
                    rs.getString("ip_address"),
                    rs.getString("user_agent")
                );
                auditLogList.add(log);
            }
            
            // Update labels
            if (logCountLabel != null) {
                logCountLabel.setText("Showing " + auditLogList.size() + " of " + totalRecords + " logs");
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error loading audit logs: " + e.getMessage());
            showAlert("Database Error", "Failed to load audit logs: " + e.getMessage(), AlertType.ERROR);
        }
    }
    
    private void createAuditLogTable() {
        try {
            String query = "CREATE TABLE IF NOT EXISTS audit_log (" +
                         "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "user_id INT, " +
                         "username VARCHAR(50), " +
                         "action VARCHAR(100) NOT NULL, " +
                         "module VARCHAR(50) NOT NULL, " +
                         "description TEXT, " +
                         "ip_address VARCHAR(45), " +
                         "user_agent TEXT, " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "INDEX idx_username (username), " +
                         "INDEX idx_action (action), " +
                         "INDEX idx_module (module), " +
                         "INDEX idx_created_at (created_at))";
            
            Statement stmt = connection.createStatement();
            stmt.execute(query);
            stmt.close();
            
            System.out.println("Audit log table created/verified successfully");
            
        } catch (SQLException e) {
            System.err.println("Error creating audit log table: " + e.getMessage());
        }
    }
    
    private void refreshAuditData() {
        loadStatistics();
        loadAuditLogs();
        loadUserFilterOptions();
        loadActionTypeFilterOptions();
    }
    
    private void resetFilters() {
        try {
            if (auditStartDate != null) {
                auditStartDate.setValue(LocalDate.now().minusDays(7));
            }
            if (auditEndDate != null) {
                auditEndDate.setValue(LocalDate.now());
            }
            if (txtuserFilter != null) {
                txtuserFilter.setValue("All Users");
            }
            if (actionTypeFilter != null) {
                actionTypeFilter.setValue("All Actions");
            }
            if (txtipAddressFilter != null) {
                txtipAddressFilter.clear();
            }
            
            loadAuditLogs();
        } catch (Exception e) {
            System.err.println("Error resetting filters: " + e.getMessage());
        }
    }
    
    private void clearOldLogs() {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Clear Old Logs");
        confirm.setHeaderText("Clear Audit Logs Older Than 90 Days");
        confirm.setContentText("Are you sure you want to delete audit logs older than 90 days? This action cannot be undone.");
        
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    String query = "DELETE FROM audit_log WHERE created_at < DATE_SUB(NOW(), INTERVAL 90 DAY)";
                    Statement stmt = connection.createStatement();
                    int deletedCount = stmt.executeUpdate(query);
                    stmt.close();
                    
                    // Refresh data
                    refreshAuditData();
                    
                    showAlert("Success", "Deleted " + deletedCount + " old audit logs", AlertType.INFORMATION);
                    
                } catch (SQLException e) {
                    showAlert("Database Error", "Failed to clear old logs: " + e.getMessage(), AlertType.ERROR);
                }
            }
        });
    }
    
    private void viewLogDetails(AuditLog log) {
        Alert dialog = new Alert(AlertType.INFORMATION);
        dialog.setTitle("Audit Log Details");
        dialog.setHeaderText("Log ID: " + log.getLogId());
        
        StringBuilder content = new StringBuilder();
        content.append("Timestamp: ").append(log.getFormattedTimestamp()).append("\n\n");
        content.append("User: ").append(log.getUsername() != null ? log.getUsername() : "System").append("\n\n");
        content.append("Action: ").append(log.getAction()).append("\n\n");
        content.append("Module: ").append(log.getModule()).append("\n\n");
        content.append("Details: ").append(log.getDetails() != null ? log.getDetails() : "N/A").append("\n\n");
        content.append("IP Address: ").append(log.getIpAddress() != null ? log.getIpAddress() : "N/A").append("\n\n");
        content.append("User Agent: ").append(log.getUserAgent() != null ? log.getUserAgent() : "N/A");
        
        dialog.setContentText(content.toString());
        dialog.getDialogPane().setPrefSize(500, 400);
        dialog.showAndWait();
    }
    
    private void navigateToExportLogs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/ExportLogs.fxml"));
            Parent exportRoot = loader.load();
            
            Stage currentStage = (Stage) exportAuditBtn.getScene().getWindow();
            Scene exportScene = new Scene(exportRoot);
            
            currentStage.setScene(exportScene);
            currentStage.setTitle("Export Audit Logs");
            currentStage.show();
            
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load Export Logs page: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }
    
    private void navigateToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Dashboard.fxml"));
            Parent dashboardRoot = loader.load();
            
            Stage currentStage = (Stage) newTaxpayerBtn1.getScene().getWindow();
            Scene dashboardScene = new Scene(dashboardRoot);
            
            currentStage.setScene(dashboardScene);
            currentStage.setTitle("Dashboard");
            currentStage.show();
            
        } catch (IOException e) {
            showAlert("Navigation Error", "Failed to load Dashboard: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
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
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
    
    // Audit Log model class
    public static class AuditLog {
        private final int logId;
        private final Timestamp timestamp;
        private final String username;
        private final String action;
        private final String module;
        private final String details;
        private final String ipAddress;
        private final String userAgent;
        
        public AuditLog(int logId, Timestamp timestamp, String username, String action, 
                       String module, String details, String ipAddress, String userAgent) {
            this.logId = logId;
            this.timestamp = timestamp;
            this.username = username;
            this.action = action;
            this.module = module;
            this.details = details;
            this.ipAddress = ipAddress;
            this.userAgent = userAgent;
        }
        
        public int getLogId() { return logId; }
        public Timestamp getTimestamp() { return timestamp; }
        public String getFormattedTimestamp() {
            if (timestamp == null) return "N/A";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return timestamp.toLocalDateTime().format(formatter);
        }
        public String getUsername() { return username != null ? username : "System"; }
        public String getAction() { return action != null ? action : "Unknown"; }
        public String getModule() { return module != null ? module : "Unknown"; }
        public String getDetails() { return details != null ? details : ""; }
        public String getIpAddress() { return ipAddress != null ? ipAddress : ""; }
        public String getUserAgent() { return userAgent != null ? userAgent : ""; }
        public String getStatus() { 
            if (action != null && (action.toLowerCase().contains("failed") || 
                action.toLowerCase().contains("error") || 
                action.toLowerCase().contains("denied"))) {
                return "Failed";
            }
            return "Success";
        }
    }
}