package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DashboardController {

    // Database connection
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    private Connection connection;

    // FXML file paths - FIXED: Updated paths to match actual FXML files
    private static final Map<String, String> FXML_PATHS = Map.of(
        "dashboard", "/TMSFXML/Dashboard.fxml",
        "taxpayer", "/TMSFXML/TaxpayerManagement.fxml",
        "assessment", "/TMSFXML/AssessmentForm.fxml",
        "payment", "/TMSFXML/Payment.fxml", // Changed from PaymentProcessing.fxml to Payment.fxml
        "reports", "/TMSFXML/Reports.fxml",
        "audit", "/TMSFXML/Audit.fxml",
        "admin", "/TMSFXML/Admin.fxml",
        "PaymentProcessing","/TMSFXML/PaymentProcessing.fxml",
        "newTaxpayer", "/TMSFXML/TaxpayerForm.fxml",
        "newPayment", "/TMSFXML/newpayment.fxml"
    );

    @FXML
    private Label activeUsersLabel;

    @FXML
    private Button adminBtn;

    @FXML
    private VBox alertsBox;

    @FXML
    private Button assessmentBtn;

    @FXML
    private Button auditBtn;

    @FXML
    private Label auditScoreValue;

    @FXML
    private Label avatarText;

    @FXML
    private Label collectionsValue;

    @FXML
    private LineChart<String, Number> complianceLineChart;

    @FXML
    private Label complianceTrendLabel;

    @FXML
    private Label complianceValue;

    @FXML
    private Label cpuUsageLabel;

    @FXML
    private Button dashboardBtn;

    @FXML
    private Label dateLabel;

    @FXML
    private Label dbConnectionsLabel;

    @FXML
    private HBox kpiRow1;

    @FXML
    private HBox kpiRow2;

    @FXML
    private Label lastSyncLabel;

    @FXML
    private Button logoutBtn;

    @FXML
    private VBox mainContent;

    @FXML
    private ScrollPane mainContentScroll;

    @FXML
    private Label mainTitle;

    @FXML
    private Label memoryUsageLabel;

    @FXML
    private VBox menuBox;

    @FXML
    private Button newPaymentBtn;

    @FXML
    private Button newTaxpayerBtn;

    @FXML
    private Button paymentBtn;

    @FXML
    private Label pendingNotificationsLabel;

    @FXML
    private Label pendingTasksValue;

    @FXML
    private VBox quickActionsBox;

    @FXML
    private Button quickReportBtn;

    @FXML
    private VBox recentActivitiesBox;

    @FXML
    private Button refreshBtn;

    @FXML
    private BarChart<String, Number> regionalBarChart;

    @FXML
    private Button reportsBtn;

    @FXML
    private BarChart<String, Number> revenueBarChart;

    @FXML
    private Label revenueValue;

    @FXML
    private BorderPane rootPane;

    @FXML
    private VBox sidebar;

    @FXML
    private Label sidebarUserName;

    @FXML
    private HBox statusBar;

    @FXML
    private Button taxpayerBtn;

    @FXML
    private PieChart taxpayerPieChart;

    @FXML
    private Label taxpayersValue;

    @FXML
    private HBox topNav;

    @FXML
    private Label uptimeLabel;

    @FXML
    private HBox userInfoBox;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label userRoleLabel;

    // Additional UI elements
    @FXML
    private ProgressBar complianceProgressBar;
    
    @FXML
    private Label complianceTargetLabel;
    
    @FXML
    private Label topRegionLabel;

    @FXML
    void initialize() {
        System.out.println("DashboardController initializing...");
        System.out.println("Current FXML paths: " + FXML_PATHS);
        
        try {
            // Initialize database connection
            initializeDatabaseConnection();
            
            // Setup current date
            updateDateTime();
            
            // Load user info
            loadUserInfo();
            
            // Load dashboard data
            loadDashboardData();
            
            // Initialize charts
            initializeCharts();
            
            // Setup system monitoring
            startSystemMonitoring();
            
            // Setup refresh listener
            setupAutoRefresh();
            
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            showAlert("Database Error", "Cannot connect to database: " + e.getMessage());
            
            // Set default values for offline mode
            setDefaultValues();
        }
    }

    private void initializeDatabaseConnection() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        System.out.println("Database connection established successfully.");
    }

    private void updateDateTime() {
        dateLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")));
    }

    private void loadUserInfo() {
        try {
            String query = "SELECT username, role FROM users WHERE is_active = 1 ORDER BY last_login DESC LIMIT 1";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String username = rs.getString("username");
                String role = rs.getString("role");
                
                userNameLabel.setText(username);
                userRoleLabel.setText(role.toUpperCase());
                sidebarUserName.setText(username);
                
                // Set avatar text (first letter of username)
                if (username != null && !username.isEmpty()) {
                    avatarText.setText(username.substring(0, 1).toUpperCase());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading user info: " + e.getMessage());
            // Set default values
            setDefaultUserInfo();
        }
    }

    private void setDefaultUserInfo() {
        userNameLabel.setText("Admin");
        userRoleLabel.setText("ADMIN");
        sidebarUserName.setText("Admin");
        avatarText.setText("A");
    }

    private void loadDashboardData() {
        try {
            // Load KPI data
            loadKPIData();
            
            // Load charts data
            loadRevenueData();
            loadComplianceData();
            loadTaxpayerDistribution();
            loadRegionalPerformance();
            
            // Load system metrics
            loadSystemMetrics();
            
            // Load recent activities
            loadRecentActivities();
            
            // Load alerts
            loadSystemAlerts();
            
        } catch (SQLException e) {
            System.err.println("Error loading dashboard data: " + e.getMessage());
            setDefaultDashboardData();
        }
    }

    private void loadKPIData() throws SQLException {
        // Total Revenue
        String revenueQuery = "SELECT COALESCE(SUM(amount_paid), 0) as total_revenue " +
                            "FROM payments WHERE payment_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        PreparedStatement revenueStmt = connection.prepareStatement(revenueQuery);
        ResultSet revenueRs = revenueStmt.executeQuery();
        if (revenueRs.next()) {
            double revenue = revenueRs.getDouble("total_revenue");
            revenueValue.setText(String.format("ETB %.1fM", revenue / 1000000));
        }

        // Active Taxpayers
        String taxpayerQuery = "SELECT COUNT(*) as total_taxpayers FROM taxpayers WHERE status = 'Active'";
        PreparedStatement taxpayerStmt = connection.prepareStatement(taxpayerQuery);
        ResultSet taxpayerRs = taxpayerStmt.executeQuery();
        if (taxpayerRs.next()) {
            int taxpayers = taxpayerRs.getInt("total_taxpayers");
            taxpayersValue.setText(String.format("%d", taxpayers));
        }

        // Compliance Rate
        String complianceQuery = "SELECT " +
                               "(SELECT COUNT(*) FROM taxpayers WHERE compliance_status = 'Compliant') * 100.0 / " +
                               "GREATEST(COUNT(*), 1) as compliance_rate " +
                               "FROM taxpayers";
        PreparedStatement complianceStmt = connection.prepareStatement(complianceQuery);
        ResultSet complianceRs = complianceStmt.executeQuery();
        if (complianceRs.next()) {
            double complianceRate = complianceRs.getDouble("compliance_rate");
            complianceValue.setText(String.format("%.1f%%", complianceRate));
            
            // Update progress bar and target
            complianceProgressBar.setProgress(complianceRate / 100);
            complianceTargetLabel.setText(String.format("Current: %.1f%% | Target: 85.0%%", complianceRate));
            complianceTrendLabel.setText(String.format("Current: %.1f%% | Target: 85.0%%", complianceRate));
        }

        // Collections
        String collectionsQuery = "SELECT COALESCE(SUM(amount_paid), 0) as total_collections " +
                                "FROM payments WHERE payment_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
        PreparedStatement collectionsStmt = connection.prepareStatement(collectionsQuery);
        ResultSet collectionsRs = collectionsStmt.executeQuery();
        if (collectionsRs.next()) {
            double collections = collectionsRs.getDouble("total_collections");
            collectionsValue.setText(String.format("ETB %.1fM", collections / 1000000));
        }

        // Audit Score (simulated if table doesn't exist)
        try {
            String auditQuery = "SELECT AVG(audit_score) as avg_audit_score FROM audit_logs WHERE audit_date >= DATE_SUB(NOW(), INTERVAL 90 DAY)";
            PreparedStatement auditStmt = connection.prepareStatement(auditQuery);
            ResultSet auditRs = auditStmt.executeQuery();
            if (auditRs.next()) {
                double auditScore = auditRs.getDouble("avg_audit_score");
                auditScoreValue.setText(String.format("%.1f/10", auditScore / 10));
            } else {
                auditScoreValue.setText("8.5/10");
            }
        } catch (SQLException e) {
            auditScoreValue.setText("8.5/10");
        }

        // Pending Tasks
        try {
            String tasksQuery = "SELECT COUNT(*) as pending_tasks FROM tasks WHERE status = 'Pending'";
            PreparedStatement tasksStmt = connection.prepareStatement(tasksQuery);
            ResultSet tasksRs = tasksStmt.executeQuery();
            if (tasksRs.next()) {
                int pendingTasks = tasksRs.getInt("pending_tasks");
                pendingTasksValue.setText(String.format("%d", pendingTasks));
            } else {
                pendingTasksValue.setText("0");
            }
        } catch (SQLException e) {
            pendingTasksValue.setText("0");
        }
    }

    private void loadRevenueData() throws SQLException {
        revenueBarChart.getData().clear();
        
        // Sample data for demonstration
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        double[] actualRevenue = {45.2, 48.7, 52.1, 49.8, 56.3, 61.2};
        double[] targetRevenue = {50.0, 52.0, 54.0, 56.0, 58.0, 60.0};
        
        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual Revenue");
        
        XYChart.Series<String, Number> targetSeries = new XYChart.Series<>();
        targetSeries.setName("Target Revenue");
        
        for (int i = 0; i < months.length; i++) {
            actualSeries.getData().add(new XYChart.Data<>(months[i], actualRevenue[i]));
            targetSeries.getData().add(new XYChart.Data<>(months[i], targetRevenue[i]));
        }
        
        revenueBarChart.getData().addAll(actualSeries, targetSeries);
    }

    private void loadComplianceData() throws SQLException {
        complianceLineChart.getData().clear();
        
        // Sample data for demonstration
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        double[] complianceRates = {78.5, 79.2, 80.1, 81.5, 82.3, 83.0};
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Compliance Rate");
        
        for (int i = 0; i < months.length; i++) {
            series.getData().add(new XYChart.Data<>(months[i], complianceRates[i]));
        }
        
        complianceLineChart.getData().add(series);
    }

    private void loadTaxpayerDistribution() throws SQLException {
        taxpayerPieChart.getData().clear();
        
        // Sample data for demonstration
        taxpayerPieChart.getData().add(new PieChart.Data("Individual", 4500));
        taxpayerPieChart.getData().add(new PieChart.Data("Business", 3200));
        taxpayerPieChart.getData().add(new PieChart.Data("Corporate", 1800));
        taxpayerPieChart.getData().add(new PieChart.Data("Non-Profit", 500));
    }

    private void loadRegionalPerformance() throws SQLException {
        regionalBarChart.getData().clear();
        
        // Sample data for demonstration
        String[] regions = {"Addis Ababa", "Oromia", "Amhara", "SNNP", "Tigray"};
        double[] revenues = {125.4, 89.7, 76.3, 65.2, 42.8};
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Regional Revenue (ETB Millions)");
        
        double maxRevenue = 0;
        String topRegion = "Addis Ababa";
        
        for (int i = 0; i < regions.length; i++) {
            series.getData().add(new XYChart.Data<>(regions[i], revenues[i]));
            if (revenues[i] > maxRevenue) {
                maxRevenue = revenues[i];
                topRegion = regions[i];
            }
        }
        
        regionalBarChart.getData().add(series);
        topRegionLabel.setText(String.format("Top Region: %s (ETB %.1fM)", topRegion, maxRevenue));
    }

    private void loadSystemMetrics() throws SQLException {
        // Active users
        try {
            String usersQuery = "SELECT COUNT(*) as active_users FROM users WHERE is_active = 1";
            PreparedStatement usersStmt = connection.prepareStatement(usersQuery);
            ResultSet usersRs = usersStmt.executeQuery();
            if (usersRs.next()) {
                activeUsersLabel.setText(String.valueOf(usersRs.getInt("active_users")));
            } else {
                activeUsersLabel.setText("1");
            }
        } catch (SQLException e) {
            activeUsersLabel.setText("1");
        }

        // Database connections (simulated)
        dbConnectionsLabel.setText("5 active");

        // Update timestamps
        lastSyncLabel.setText("Last sync: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        uptimeLabel.setText("7 days 14:25:36"); // Simulated uptime
    }

    private void loadRecentActivities() {
        recentActivitiesBox.getChildren().clear();
        
        // Add sample activities
        String[] activities = {
            "🟢 System initialized successfully",
            "💰 Payment processed: ETB 15,250.00",
            "👤 New taxpayer registered: ABC Corporation",
            "📊 Monthly report generated",
            "⚙ System maintenance completed"
        };
        
        String[] times = {"Just now", "5 min ago", "15 min ago", "1 hour ago", "2 hours ago"};
        
        for (int i = 0; i < activities.length; i++) {
            HBox activityBox = new HBox(10);
            activityBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            
            Label icon = new Label("•");
            icon.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 16;");
            
            Label activity = new Label(activities[i]);
            activity.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14; -fx-text-fill: #5D6D7E;");
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            Label time = new Label(times[i]);
            time.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12; -fx-text-fill: #95A5A6;");
            
            activityBox.getChildren().addAll(icon, activity, spacer, time);
            recentActivitiesBox.getChildren().add(activityBox);
        }
    }

    private void loadSystemAlerts() {
        alertsBox.getChildren().clear();
        
        // Add sample alert
        Label noAlertsLabel = new Label("No active alerts. All systems are operational.");
        noAlertsLabel.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14; -fx-text-fill: #27AE60; " +
                              "-fx-font-weight: bold; -fx-background-color: #E8F8F5; -fx-padding: 10; " +
                              "-fx-background-radius: 8;");
        alertsBox.getChildren().add(noAlertsLabel);
    }

    private void initializeCharts() {
        // Setup revenue bar chart
        revenueBarChart.setLegendVisible(false);
        
        // Setup compliance line chart
        complianceLineChart.setLegendVisible(false);
        
        // Setup regional bar chart
        regionalBarChart.setLegendVisible(false);
        
        // Setup taxpayer pie chart
        taxpayerPieChart.setLegendVisible(true);
    }

    private void startSystemMonitoring() {
        // Simulate system metrics
        cpuUsageLabel.setText("24.5%");
        memoryUsageLabel.setText("68.2%");
        pendingNotificationsLabel.setText("3");
    }

    private void setupAutoRefresh() {
        // Auto-refresh every 60 seconds
        Thread refreshThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000);
                    javafx.application.Platform.runLater(() -> {
                        refreshDashboard(null);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    private void setDefaultValues() {
        setDefaultUserInfo();
        setDefaultDashboardData();
    }

    private void setDefaultDashboardData() {
        // Set default KPI values
        revenueValue.setText("ETB 0.0M");
        taxpayersValue.setText("0");
        complianceValue.setText("0.0%");
        complianceProgressBar.setProgress(0);
        complianceTargetLabel.setText("Current: 0.0% | Target: 85.0%");
        complianceTrendLabel.setText("Current: 0.0% | Target: 85.0%");
        collectionsValue.setText("ETB 0.0M");
        auditScoreValue.setText("8.5/10");
        pendingTasksValue.setText("0");
        
        // Set default system metrics
        activeUsersLabel.setText("1");
        dbConnectionsLabel.setText("0 active");
        cpuUsageLabel.setText("0.0%");
        memoryUsageLabel.setText("0.0%");
        pendingNotificationsLabel.setText("0");
        uptimeLabel.setText("0 days 0:00:00");
        
        // Update sync time
        lastSyncLabel.setText("Last sync: Never");
        
        // Load default charts data
        loadDefaultCharts();
    }

    private void loadDefaultCharts() {
        // Default revenue data
        revenueBarChart.getData().clear();
        XYChart.Series<String, Number> defaultRevenue = new XYChart.Series<>();
        defaultRevenue.setName("Revenue");
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun"};
        for (String month : months) {
            defaultRevenue.getData().add(new XYChart.Data<>(month, 0));
        }
        revenueBarChart.getData().add(defaultRevenue);
        
        // Default compliance data
        complianceLineChart.getData().clear();
        XYChart.Series<String, Number> defaultCompliance = new XYChart.Series<>();
        defaultCompliance.setName("Compliance");
        for (String month : months) {
            defaultCompliance.getData().add(new XYChart.Data<>(month, 0));
        }
        complianceLineChart.getData().add(defaultCompliance);
        
        // Default taxpayer distribution
        taxpayerPieChart.getData().clear();
        taxpayerPieChart.getData().add(new PieChart.Data("No Data", 1));
        
        // Default regional performance
        regionalBarChart.getData().clear();
        XYChart.Series<String, Number> defaultRegional = new XYChart.Series<>();
        defaultRegional.setName("Regional");
        defaultRegional.getData().add(new XYChart.Data<>("No Data", 0));
        regionalBarChart.getData().add(defaultRegional);
        topRegionLabel.setText("Top Region: N/A (ETB 0.0M)");
    }

    @FXML
    void handlerclickadminBtn(ActionEvent event) {
        navigateTo("admin");
    }

    @FXML
    void handlerclickassessmentBtn(ActionEvent event) {
        navigateTo("assessment");
    }

    @FXML
    void handlerclickauditBtn(ActionEvent event) {
        navigateTo("audit");
    }

    @FXML
    void handlerclickdashboardBtn(ActionEvent event) {
        navigateTo("dashboard");
    }

    @FXML
    void handlerclicklogoutBtn(ActionEvent event) {
        try {
            // Close database connection
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
            
            // Navigate to login screen
            Parent root = FXMLLoader.load(getClass().getResource("/TMSFXML/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
            
        } catch (Exception e) {
            showAlert("Logout Error", "Error during logout: " + e.getMessage());
        }
    }

    @FXML
    void handlerclicknewPaymentBtn(ActionEvent event) {
        navigateTo("newPayment");
    }

    @FXML
    void handlerclicknewTaxpayerBtn(ActionEvent event) {
        navigateTo("newTaxpayer");
    }

    @FXML
    void handlerclickpaymentBtn(ActionEvent event) {
        navigateTo("PaymentProcessing");
    }

    @FXML
    void handlerclickquickReportBtn(ActionEvent event) {
        String report = String.format(
            "Quick System Report\n" +
            "Generated: %s\n\n" +
            "Dashboard Metrics:\n" +
            "• Total Revenue: %s\n" +
            "• Active Taxpayers: %s\n" +
            "• Compliance Rate: %s\n" +
            "• Pending Tasks: %s\n" +
            "• Total Collections: %s\n" +
            "• Audit Score: %s\n\n" +
            "System Status: ✅ All systems operational",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            revenueValue.getText(),
            taxpayersValue.getText(),
            complianceValue.getText(),
            pendingTasksValue.getText(),
            collectionsValue.getText(),
            auditScoreValue.getText()
        );
        
        showAlert("Quick Report", report);
    }

    @FXML
    void handlerclickrefreshBtn(ActionEvent event) {
        refreshDashboard(event);
    }

    @FXML
    void handlerclickreportsBtn(ActionEvent event) {
        navigateTo("reports");
    }

    @FXML
    void handlerclicktaxpayerBtn(ActionEvent event) {
        navigateTo("taxpayer");
    }

    private void navigateTo(String screen) {
        try {
            String fxmlPath = FXML_PATHS.get(screen);
            if (fxmlPath == null) {
                throw new IllegalArgumentException("Unknown screen: " + screen);
            }
            
            // Check if we're already on this screen
            if (screen.equals("dashboard")) {
                // Just refresh current dashboard
                refreshDashboard(null);
                return;
            }
            
            // Load the new FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            // Get the current stage
            Stage stage = (Stage) rootPane.getScene().getWindow();
            
            // Create new scene
            Scene scene = new Scene(root);
            
            // Set the new scene
            stage.setScene(scene);
            stage.show();
            
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid screen: " + screen);
            showAlert("Navigation Error", "Screen '" + screen + "' is not configured. Available screens: " + FXML_PATHS.keySet());
        } catch (IOException e) {
            System.err.println("Error loading FXML for screen " + screen + ": " + e.getMessage());
            showAlert("Navigation Error", "Cannot load screen '" + screen + "'. File may not exist: " + FXML_PATHS.get(screen));
        } catch (Exception e) {
            System.err.println("Unexpected error navigating to " + screen + ": " + e.getMessage());
            e.printStackTrace();
            showAlert("Navigation Error", "Unexpected error: " + e.getMessage());
        }
    }

    private void refreshDashboard(ActionEvent event) {
        try {
            System.out.println("Refreshing dashboard...");
            updateDateTime();
            
            // Re-establish database connection if needed
            if (connection == null || connection.isClosed()) {
                try {
                    initializeDatabaseConnection();
                } catch (SQLException e) {
                    System.err.println("Cannot re-establish database connection: " + e.getMessage());
                }
            }
            
            loadDashboardData();
            
            // Show refresh notification
            lastSyncLabel.setText("Last sync: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            
            System.out.println("Dashboard refreshed successfully.");
            
        } catch (Exception e) {
            System.err.println("Error refreshing dashboard: " + e.getMessage());
            showAlert("Refresh Error", "Cannot refresh dashboard: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
                System.out.println("Database connection closed in cleanup.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
    
    // Method to set stage for cleanup on close
    public void setStage(Stage stage) {
        stage.setOnCloseRequest(event -> {
            cleanup();
        });
    }
}