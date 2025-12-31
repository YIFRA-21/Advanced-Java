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
import javafx.concurrent.Task;
import javafx.application.Platform;
import java.sql.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ExportLogsController {

    // FXML Fields
    @FXML private Button browseBtn;
    @FXML private Button estimateBtn;
    @FXML private Button exportLogsBtn;
    @FXML private Button previewBtn;
    @FXML private ComboBox<String> actionTypeFilter;
    @FXML private Button cancelBtn;
    @FXML private ToggleGroup contentGroup;
    @FXML private DatePicker endDatePicker;
    @FXML private Label estimatedRecordsLabel;
    @FXML private Label exportFileLabel;
    @FXML private Label exportSizeLabel;
    @FXML private TextField fileNameField;
    @FXML private ToggleGroup formatGroup;
    @FXML private Label formatLabel;
    @FXML private TextField ipAddressFilter;
    @FXML private ComboBox<String> moduleFilter;
    @FXML private Label progressLabel;
    @FXML private Label progressPercentage;
    @FXML private VBox progressSection;
    @FXML private Label progressStatus;
    @FXML private TextField saveLocationField;
    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> userFilter;
    
    // Radio buttons
    @FXML private RadioButton allLogsRadio;
    @FXML private RadioButton filteredLogsRadio;
    @FXML private RadioButton customSelectionRadio;
    @FXML private RadioButton excelRadio;
    @FXML private RadioButton pdfRadio;
    @FXML private RadioButton csvRadio;
    @FXML private RadioButton jsonRadio;
    
    // Navigation button
    @FXML private Button backToAuditPageBtn;
    
    private Connection connection;
    private final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private final String DB_USER = "belay";
    private final String DB_PASSWORD = "Belay2123";
    
    @FXML
    public void initialize() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            initializeComponents();
            
            System.out.println("ExportLogsController initialized successfully");
            
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            showAlert("Database Error", "Failed to connect to database: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void initializeComponents() {
        // Initialize date pickers
        startDatePicker.setValue(LocalDate.now().minusDays(30));
        endDatePicker.setValue(LocalDate.now());
        
        // Initialize default file name
        String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileNameField.setText("audit_logs_" + timestamp);
        
        // Initialize filter dropdowns
        initializeFilterDropdowns();
        
        // Set default radio button selections
        if (excelRadio != null) {
            excelRadio.setSelected(true);
        }
        if (allLogsRadio != null) {
            allLogsRadio.setSelected(true);
        }
        
        // Hide progress section initially
        progressSection.setVisible(false);
        
        // Set default save location
        String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
        saveLocationField.setText(downloadsPath);
        
        // Auto-update format label when format changes
        if (formatGroup != null) {
            formatGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    RadioButton selected = (RadioButton) newValue;
                    formatLabel.setText(selected.getText());
                }
            });
        }
        
        // Update format label initially
        if (excelRadio != null && excelRadio.isSelected()) {
            formatLabel.setText("Excel (.xlsx)");
        }
    }
    
    private void initializeFilterDropdowns() {
        // User filter
        userFilter.getItems().add("All Users");
        loadUserFilterOptions();
        userFilter.setValue("All Users");
        
        // Action type filter
        actionTypeFilter.getItems().add("All Actions");
        loadActionTypeFilterOptions();
        actionTypeFilter.setValue("All Actions");
        
        // Module filter
        moduleFilter.getItems().add("All Modules");
        loadModuleFilterOptions();
        moduleFilter.setValue("All Modules");
        
        // Status filter
        statusFilter.getItems().addAll("All Status", "Success", "Failed", "Warning");
        statusFilter.setValue("All Status");
    }
    
    private void loadUserFilterOptions() {
        try {
            String query = "SELECT DISTINCT username FROM audit_log WHERE username IS NOT NULL AND username != '' ORDER BY username";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                String username = rs.getString("username");
                if (username != null && !username.trim().isEmpty()) {
                    userFilter.getItems().add(username);
                }
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            // Create table if it doesn't exist
            createAuditLogTable();
            System.out.println("Note: Could not load user filter options: " + e.getMessage());
        }
    }
    
    private void createAuditLogTable() {
        try {
            String query = "CREATE TABLE IF NOT EXISTS audit_log (" +
                         "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "username VARCHAR(100), " +
                         "action VARCHAR(200), " +
                         "module VARCHAR(100), " +
                         "description TEXT, " +
                         "ip_address VARCHAR(45), " +
                         "status VARCHAR(50)" +
                         ")";
            
            Statement stmt = connection.createStatement();
            stmt.execute(query);
            stmt.close();
            
            System.out.println("Audit log table created successfully");
            
        } catch (SQLException e) {
            System.err.println("Failed to create audit_log table: " + e.getMessage());
        }
    }
    
    private void loadActionTypeFilterOptions() {
        try {
            String query = "SELECT DISTINCT action FROM audit_log WHERE action IS NOT NULL AND action != '' ORDER BY action";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                String action = rs.getString("action");
                if (action != null && !action.trim().isEmpty()) {
                    actionTypeFilter.getItems().add(action);
                }
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.out.println("Note: Could not load action type filter options: " + e.getMessage());
        }
    }
    
    private void loadModuleFilterOptions() {
        try {
            String query = "SELECT DISTINCT module FROM audit_log WHERE module IS NOT NULL AND module != '' ORDER BY module";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                String module = rs.getString("module");
                if (module != null && !module.trim().isEmpty()) {
                    moduleFilter.getItems().add(module);
                }
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.out.println("Note: Could not load module filter options: " + e.getMessage());
        }
    }
    
    // ==================== EVENT HANDLER METHODS ====================
    
    @FXML
    public void handleBrowse(ActionEvent event) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Save Location");
        File selectedDir = directoryChooser.showDialog(((Button) event.getSource()).getScene().getWindow());
        if (selectedDir != null) {
            saveLocationField.setText(selectedDir.getAbsolutePath());
        }
    }
    
    @FXML
    public void handleEstimate(ActionEvent event) {
        estimateExport();
    }
    
    @FXML
    public void handleExportLogs(ActionEvent event) {
        exportLogs();
    }
    
    @FXML
    public void handlePreview(ActionEvent event) {
        previewLogs();
    }
    
    @FXML
    public void handleCancel(ActionEvent event) {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    public void handleBackToAuditPage(ActionEvent event) {
        navigateBackToAudit();
    }
    
    // ==================== BUSINESS LOGIC METHODS ====================
    
    private void estimateExport() {
        try {
            int recordCount = getFilteredRecordCount();
            
            // Calculate estimated file size (rough estimation)
            double estimatedSize = (recordCount * 500.0) / (1024 * 1024); // 500 bytes per record
            
            // Update labels
            estimatedRecordsLabel.setText(recordCount + " records");
            exportSizeLabel.setText(String.format("%.2f MB", estimatedSize));
            
            // Get selected format
            RadioButton selectedFormat = (RadioButton) formatGroup.getSelectedToggle();
            if (selectedFormat != null) {
                formatLabel.setText(selectedFormat.getText());
            }
            
            // Update export file name preview
            String fileName = fileNameField.getText().trim();
            if (fileName.isEmpty()) {
                fileName = "audit_logs_export";
            }
            exportFileLabel.setText("File: " + fileName + getFileExtension());
            
        } catch (SQLException e) {
            showAlert("Estimation Error", "Failed to estimate export: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private String getFileExtension() {
        RadioButton selectedFormat = (RadioButton) formatGroup.getSelectedToggle();
        if (selectedFormat != null) {
            String format = selectedFormat.getText().toLowerCase();
            if (format.contains("excel") || format.contains("xlsx")) return ".xlsx";
            if (format.contains("pdf")) return ".pdf";
            if (format.contains("csv")) return ".csv";
            if (format.contains("json")) return ".json";
        }
        return ".csv";
    }
    
    private int getFilteredRecordCount() throws SQLException {
        StringBuilder query = new StringBuilder("SELECT COUNT(*) as total FROM audit_log WHERE 1=1");
        List<Object> params = new ArrayList<>();
        applyFiltersToQuery(query, params);
        
        PreparedStatement pstmt = connection.prepareStatement(query.toString());
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
        
        ResultSet rs = pstmt.executeQuery();
        int count = 0;
        if (rs.next()) {
            count = rs.getInt("total");
        }
        
        rs.close();
        pstmt.close();
        return count;
    }
    
    private void previewLogs() {
        try {
            StringBuilder query = new StringBuilder("SELECT * FROM audit_log WHERE 1=1");
            List<Object> params = new ArrayList<>();
            applyFiltersToQuery(query, params);
            query.append(" ORDER BY created_at DESC LIMIT 100");
            
            PreparedStatement pstmt = connection.prepareStatement(query.toString());
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = pstmt.executeQuery();
            
            StringBuilder preview = new StringBuilder();
            preview.append("Preview (First 100 records):\n\n");
            
            int count = 0;
            while (rs.next() && count < 10) { // Show first 10 for preview
                preview.append("Timestamp: ").append(rs.getTimestamp("created_at")).append("\n");
                preview.append("User: ").append(rs.getString("username")).append("\n");
                preview.append("Action: ").append(rs.getString("action")).append("\n");
                preview.append("Module: ").append(rs.getString("module")).append("\n");
                preview.append("IP: ").append(rs.getString("ip_address")).append("\n");
                preview.append("Status: ").append(rs.getString("status")).append("\n");
                preview.append("---\n");
                count++;
            }
            
            if (count == 0) {
                preview.append("No records found with current filters.");
            }
            
            rs.close();
            pstmt.close();
            
            TextArea previewArea = new TextArea(preview.toString());
            previewArea.setEditable(false);
            previewArea.setWrapText(true);
            
            Alert previewDialog = new Alert(Alert.AlertType.INFORMATION);
            previewDialog.setTitle("Export Preview");
            previewDialog.setHeaderText("Preview of logs to be exported");
            previewDialog.getDialogPane().setContent(previewArea);
            previewDialog.getDialogPane().setPrefSize(600, 400);
            previewDialog.showAndWait();
            
        } catch (SQLException e) {
            showAlert("Preview Error", "Failed to generate preview: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void exportLogs() {
        // Validate inputs
        if (fileNameField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter a file name", Alert.AlertType.ERROR);
            fileNameField.requestFocus();
            return;
        }
        
        if (saveLocationField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please select a save location", Alert.AlertType.ERROR);
            saveLocationField.requestFocus();
            return;
        }
        
        File saveDir = new File(saveLocationField.getText());
        if (!saveDir.exists() || !saveDir.isDirectory()) {
            showAlert("Validation Error", "Invalid save location. Please select an existing directory.", Alert.AlertType.ERROR);
            saveLocationField.requestFocus();
            return;
        }
        
        if (!saveDir.canWrite()) {
            showAlert("Permission Error", "Cannot write to selected directory. Please choose a different location.", Alert.AlertType.ERROR);
            saveLocationField.requestFocus();
            return;
        }
        
        // Get export parameters
        String fileName = fileNameField.getText().trim();
        String fileExtension = getFileExtension();
        String fullFilePath = saveDir.getAbsolutePath() + File.separator + fileName + fileExtension;
        
        // Check if file already exists
        File outputFile = new File(fullFilePath);
        if (outputFile.exists()) {
            Alert overwriteConfirm = new Alert(Alert.AlertType.CONFIRMATION);
            overwriteConfirm.setTitle("File Exists");
            overwriteConfirm.setHeaderText("File already exists: " + outputFile.getName());
            overwriteConfirm.setContentText("Do you want to overwrite it?");
            
            overwriteConfirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            overwriteConfirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    startExport(fullFilePath);
                }
            });
        } else {
            startExport(fullFilePath);
        }
    }
    
    private void startExport(String filePath) {
        Task<Void> exportTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateProgress(0, 100);
                updateMessage("Preparing export...");
                
                // Get filtered data
                updateProgress(10, 100);
                updateMessage("Fetching data from database...");
                List<Map<String, Object>> auditData = fetchAuditData();
                
                if (auditData.isEmpty()) {
                    updateMessage("No data to export");
                    return null;
                }
                
                // Export based on format
                updateProgress(30, 100);
                updateMessage("Exporting data...");
                
                String format = formatLabel.getText().toLowerCase();
                if (format.contains("csv") || format.contains(".csv")) {
                    exportToCSV(auditData, filePath);
                } else if (format.contains("json") || format.contains(".json")) {
                    exportToJSON(auditData, filePath);
                } else {
                    // For Excel/PDF, export as CSV for now (you can implement proper Excel/PDF export later)
                    exportToCSV(auditData, filePath.replace(".xlsx", ".csv").replace(".pdf", ".csv"));
                }
                
                updateProgress(100, 100);
                updateMessage("Export completed successfully!");
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                progressSection.setVisible(false);
                showAlert("Export Successful", 
                         "Audit logs exported successfully to:\n" + filePath, 
                         Alert.AlertType.INFORMATION);
            }
            
            @Override
            protected void failed() {
                progressSection.setVisible(false);
                showAlert("Export Failed", 
                         "Failed to export audit logs: " + getException().getMessage(), 
                         Alert.AlertType.ERROR);
            }
        };
        
        // Bind progress to UI
        exportTask.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            int percent = (int) (newProgress.doubleValue() * 100);
            progressPercentage.setText(percent + "%");
        });
        
        exportTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            progressStatus.setText(newMsg);
        });
        
        // Show progress section
        progressSection.setVisible(true);
        progressLabel.setText("Exporting audit logs...");
        exportFileLabel.setText("Saving to: " + filePath);
        
        // Start export task in background thread
        new Thread(exportTask).start();
    }
    
    private List<Map<String, Object>> fetchAuditData() throws SQLException {
        StringBuilder query = new StringBuilder("SELECT * FROM audit_log WHERE 1=1");
        List<Object> params = new ArrayList<>();
        applyFiltersToQuery(query, params);
        query.append(" ORDER BY created_at DESC");
        
        PreparedStatement pstmt = connection.prepareStatement(query.toString());
        for (int i = 0; i < params.size(); i++) {
            pstmt.setObject(i + 1, params.get(i));
        }
        
        ResultSet rs = pstmt.executeQuery();
        
        List<Map<String, Object>> data = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("log_id", rs.getInt("log_id"));
            row.put("timestamp", rs.getTimestamp("created_at"));
            row.put("username", rs.getString("username"));
            row.put("action", rs.getString("action"));
            row.put("module", rs.getString("module"));
            row.put("description", rs.getString("description"));
            row.put("ip_address", rs.getString("ip_address"));
            row.put("status", rs.getString("status"));
            data.add(row);
        }
        
        rs.close();
        pstmt.close();
        return data;
    }
    
    private void applyFiltersToQuery(StringBuilder query, List<Object> params) {
        // Date range
        if (startDatePicker.getValue() != null) {
            query.append(" AND DATE(created_at) >= ?");
            params.add(startDatePicker.getValue());
        }
        
        if (endDatePicker.getValue() != null) {
            query.append(" AND DATE(created_at) <= ?");
            params.add(endDatePicker.getValue());
        }
        
        // User filter
        String user = userFilter.getValue();
        if (user != null && !"All Users".equals(user) && !user.trim().isEmpty()) {
            query.append(" AND username = ?");
            params.add(user);
        }
        
        // Action type filter
        String action = actionTypeFilter.getValue();
        if (action != null && !"All Actions".equals(action) && !action.trim().isEmpty()) {
            query.append(" AND action = ?");
            params.add(action);
        }
        
        // Module filter
        String module = moduleFilter.getValue();
        if (module != null && !"All Modules".equals(module) && !module.trim().isEmpty()) {
            query.append(" AND module = ?");
            params.add(module);
        }
        
        // Status filter
        String status = statusFilter.getValue();
        if (status != null && !"All Status".equals(status) && !status.trim().isEmpty()) {
            query.append(" AND status = ?");
            params.add(status);
        }
        
        // IP address filter
        String ip = ipAddressFilter.getText().trim();
        if (!ip.isEmpty()) {
            query.append(" AND ip_address LIKE ?");
            params.add("%" + ip + "%");
        }
    }
    
    private void exportToCSV(List<Map<String, Object>> data, String filePath) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Write UTF-8 BOM for Excel compatibility
            writer.write('\uFEFF');
            
            // Write header
            writer.println("Log ID,Timestamp,User,Action,Module,Description,IP Address,Status");
            
            // Write data
            for (Map<String, Object> rowData : data) {
                writer.print(rowData.get("log_id"));
                writer.print(",");
                writer.print(escapeCsv(rowData.get("timestamp").toString()));
                writer.print(",");
                writer.print(escapeCsv((String) rowData.get("username")));
                writer.print(",");
                writer.print(escapeCsv((String) rowData.get("action")));
                writer.print(",");
                writer.print(escapeCsv((String) rowData.get("module")));
                writer.print(",");
                writer.print(escapeCsv((String) rowData.get("description")));
                writer.print(",");
                writer.print(escapeCsv((String) rowData.get("ip_address")));
                writer.print(",");
                writer.print(escapeCsv((String) rowData.get("status")));
                writer.println();
            }
        }
    }
    
    private void exportToJSON(List<Map<String, Object>> data, String filePath) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            writer.println("[");
            for (int i = 0; i < data.size(); i++) {
                Map<String, Object> row = data.get(i);
                writer.println("  {");
                writer.println("    \"log_id\": " + row.get("log_id") + ",");
                writer.println("    \"timestamp\": \"" + row.get("timestamp") + "\",");
                writer.println("    \"username\": \"" + escapeJson((String) row.get("username")) + "\",");
                writer.println("    \"action\": \"" + escapeJson((String) row.get("action")) + "\",");
                writer.println("    \"module\": \"" + escapeJson((String) row.get("module")) + "\",");
                writer.println("    \"description\": \"" + escapeJson((String) row.get("description")) + "\",");
                writer.println("    \"ip_address\": \"" + row.get("ip_address") + "\",");
                writer.println("    \"status\": \"" + escapeJson((String) row.get("status")) + "\"");
                writer.print("  }");
                if (i < data.size() - 1) {
                    writer.println(",");
                } else {
                    writer.println();
                }
            }
            writer.println("]");
        }
    }
    
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    private String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
    
    private void navigateBackToAudit() {
        try {
            // Close current window
            Stage currentStage = (Stage) cancelBtn.getScene().getWindow();
            currentStage.close();
            
            // Load Audit.fxml (adjust the path as needed)
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Audit.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Audit Logs");
            stage.show();
            
        } catch (Exception e) {
            System.err.println("Error loading Audit.fxml: " + e.getMessage());
            // If Audit.fxml doesn't exist, just close the window
            Stage currentStage = (Stage) cancelBtn.getScene().getWindow();
            currentStage.close();
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    // Clean up resources
    public void shutdown() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
}