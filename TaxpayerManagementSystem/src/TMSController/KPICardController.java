package TMSController;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

public class KPICardController {

    // Database connection
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    private Connection connection;
    
    // KPI types
    public enum KPIType {
        REVENUE("ጠቅላላ ገቢ", "Total Revenue", "#4CAF50", "M20,12 L20,8 L16,8 L16,12 L12,12 L16,16 L20,16 L20,12 Z"),
        TAXPAYERS("የታክስ ከፋዮች", "Active Taxpayers", "#2196F3", "M16,4 L16,8 L20,8 L20,12 L24,12 L24,16 L28,16 L28,20 L20,20 L20,16 L16,16 L16,20 L8,20 L8,16 L4,16 L4,12 L8,12 L8,8 L12,8 L12,4 Z"),
        COMPLIANCE("የማገለገል መጠን", "Compliance Rate", "#FF9800", "M12,2 L20,2 L26,8 L26,22 L6,22 L6,8 L12,2 Z M12,2 L12,8 L6,8 L12,2 Z M20,2 L20,8 L26,8 L20,2 Z"),
        COLLECTIONS("የሚገኙ ገንዘቦች", "Pending Collections", "#F44336", "M16,2 L24,2 L30,8 L30,22 L10,22 L10,8 L16,2 Z M16,2 L16,8 L10,8 L16,2 Z M24,2 L24,8 L30,8 L24,2 Z"),
        AUDIT_SCORE("የኦዲት ነጥብ", "Audit Score", "#9C27B0", "M16,4 L24,4 L28,8 L28,20 L20,20 L20,24 L12,24 L12,20 L4,20 L4,8 L8,4 Z"),
        REGIONAL_PERFORMANCE("ክልላዊ አፈፃፀም", "Regional Performance", "#00BCD4", "M16,2 L28,2 L30,4 L30,16 L28,18 L28,30 L4,30 L4,18 L2,16 L2,4 L4,2 Z");
        
        private final String amharicTitle;
        private final String englishTitle;
        private final String color;
        private final String svgPath;
        
        KPIType(String amharicTitle, String englishTitle, String color, String svgPath) {
            this.amharicTitle = amharicTitle;
            this.englishTitle = englishTitle;
            this.color = color;
            this.svgPath = svgPath;
        }
        
        public String getAmharicTitle() { return amharicTitle; }
        public String getEnglishTitle() { return englishTitle; }
        public String getColor() { return color; }
        public String getSvgPath() { return svgPath; }
    }
    
    // KPI data
    private KPIType kpiType;
    private double currentValue;
    private double previousValue;
    private String currency = "ETB";
    private String unit = "M"; // M for millions, K for thousands, % for percentage
    
    @FXML
    private Label amharicLabel;

    @FXML
    private Label iconLabel;

    @FXML
    private VBox kpiCard;

    @FXML
    private Button moreBtn;

    @FXML
    private Label titleLabel;

    @FXML
    private Label trendIcon;

    @FXML
    private Label trendText;

    @FXML
    private Label valueLabel;
    
    // Additional UI elements
    private SVGPath iconSvg;
    private Timeline valueAnimation;
    private Tooltip detailedTooltip;

    /**
     * Initialize the KPI card with specific type
     */
    public void initializeWithType(KPIType type) {
        this.kpiType = type;
        
        try {
            // Initialize database connection
            initializeDatabaseConnection();
            
            // Setup UI
            setupUIComponents();
            
            // Load KPI data
            loadKPIData();
            
            // Setup animations
            setupAnimations();
            
            // Setup tooltips
            setupTooltips();
            
            // Setup event handlers
            setupEventHandlers();
            
        } catch (SQLException e) {
            System.err.println("Error initializing KPI card: " + e.getMessage());
            setErrorState();
        }
    }
    
    private void initializeDatabaseConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
    }
    
    private void setupUIComponents() {
        // Set titles
        titleLabel.setText(kpiType.getEnglishTitle());
        amharicLabel.setText(kpiType.getAmharicTitle());
        
        // Set card color scheme
        String color = kpiType.getColor();
        kpiCard.setStyle("-fx-background-color: white; -fx-border-color: " + color + "20; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        // Create and set SVG icon
        iconSvg = new SVGPath();
        iconSvg.setContent(kpiType.getSvgPath());
        iconSvg.setFill(Color.web(color));
        iconSvg.setStroke(Color.web(color + "80"));
        iconSvg.setStrokeWidth(0.5);
        
        // Add SVG to icon label (custom graphic)
        iconLabel.setGraphic(iconSvg);
        iconLabel.setStyle("-fx-background-color: " + color + "20; -fx-background-radius: 50; -fx-padding: 8;");
        
        // Style more button
        moreBtn.setStyle("-fx-background-color: " + color + "20; -fx-text-fill: " + color + "; -fx-background-radius: 4;");
        moreBtn.setOnMouseEntered(e -> moreBtn.setStyle("-fx-background-color: " + color + "40; -fx-text-fill: " + color + "; -fx-background-radius: 4;"));
        moreBtn.setOnMouseExited(e -> moreBtn.setStyle("-fx-background-color: " + color + "20; -fx-text-fill: " + color + "; -fx-background-radius: 4;"));
        
        // Set default unit based on KPI type
        switch (kpiType) {
            case REVENUE:
                unit = "M";
                currency = "ETB";
                break;
            case TAXPAYERS:
                unit = "";
                currency = "";
                break;
            case COMPLIANCE:
            case AUDIT_SCORE:
                unit = "%";
                currency = "";
                break;
            case COLLECTIONS:
                unit = "M";
                currency = "ETB";
                break;
            case REGIONAL_PERFORMANCE:
                unit = "%";
                currency = "";
                break;
        }
    }
    
    private void loadKPIData() throws SQLException {
        String query = "";
        
        switch (kpiType) {
            case REVENUE:
                query = "SELECT " +
                       "(SELECT COALESCE(SUM(amount_paid), 0) FROM payments " +
                       "WHERE payment_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)) as current_value, " +
                       "(SELECT COALESCE(SUM(amount_paid), 0) FROM payments " +
                       "WHERE payment_date >= DATE_SUB(NOW(), INTERVAL 60 DAY) " +
                       "AND payment_date < DATE_SUB(NOW(), INTERVAL 30 DAY)) as previous_value";
                break;
                
            case TAXPAYERS:
                query = "SELECT " +
                       "(SELECT COUNT(*) FROM taxpayers WHERE status = 'Active') as current_value, " +
                       "(SELECT COUNT(*) FROM taxpayers WHERE status = 'Active' " +
                       "AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)) as previous_value";
                break;
                
            case COMPLIANCE:
                query = "SELECT " +
                       "(SELECT (COUNT(*) * 100.0) / GREATEST((SELECT COUNT(*) FROM taxpayers), 1) " +
                       "FROM taxpayers WHERE compliance_status = 'Compliant') as current_value, " +
                       "(SELECT (COUNT(*) * 100.0) / GREATEST((SELECT COUNT(*) FROM taxpayers " +
                       "WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)), 1) " +
                       "FROM taxpayers WHERE compliance_status = 'Compliant' " +
                       "AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY)) as previous_value";
                break;
                
            case COLLECTIONS:
                query = "SELECT " +
                       "(SELECT COALESCE(SUM(amount_due - amount_paid), 0) FROM tax_assessments " +
                       "WHERE status = 'Pending' AND due_date < NOW()) as current_value, " +
                       "(SELECT COALESCE(SUM(amount_due - amount_paid), 0) FROM tax_assessments " +
                       "WHERE status = 'Pending' AND due_date < DATE_SUB(NOW(), INTERVAL 30 DAY)) as previous_value";
                break;
                
            case AUDIT_SCORE:
                query = "SELECT " +
                       "(SELECT COALESCE(AVG(audit_score), 0) FROM audit_logs " +
                       "WHERE audit_date >= DATE_SUB(NOW(), INTERVAL 90 DAY)) as current_value, " +
                       "(SELECT COALESCE(AVG(audit_score), 0) FROM audit_logs " +
                       "WHERE audit_date >= DATE_SUB(NOW(), INTERVAL 180 DAY) " +
                       "AND audit_date < DATE_SUB(NOW(), INTERVAL 90 DAY)) as previous_value";
                break;
                
            case REGIONAL_PERFORMANCE:
                query = "SELECT " +
                       "(SELECT COALESCE(AVG(compliance_rate), 0) FROM regional_performance " +
                       "WHERE month = DATE_FORMAT(NOW(), '%Y-%m')) as current_value, " +
                       "(SELECT COALESCE(AVG(compliance_rate), 0) FROM regional_performance " +
                       "WHERE month = DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 1 MONTH), '%Y-%m')) as previous_value";
                break;
        }
        
        if (!query.isEmpty()) {
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                currentValue = rs.getDouble("current_value");
                previousValue = rs.getDouble("previous_value");
                
                // Update UI
                updateDisplayValues();
                calculateAndDisplayTrend();
            }
        }
    }
    
    private void updateDisplayValues() {
        // Format value based on type
        String formattedValue = formatValue(currentValue);
        
        // Set value with currency and unit
        String displayValue;
        if (!currency.isEmpty() && !unit.isEmpty()) {
            displayValue = String.format("%s %s %s", currency, formattedValue, unit);
        } else if (!currency.isEmpty()) {
            displayValue = String.format("%s %s", currency, formattedValue);
        } else if (!unit.isEmpty()) {
            displayValue = String.format("%s%s", formattedValue, unit);
        } else {
            displayValue = formattedValue;
        }
        
        valueLabel.setText(displayValue);
    }
    
    private String formatValue(double value) {
        DecimalFormat formatter;
        
        switch (kpiType) {
            case REVENUE:
            case COLLECTIONS:
                // Format in millions with 1 decimal
                if (value >= 1000000) {
                    formatter = new DecimalFormat("#,##0.0");
                    return formatter.format(value / 1000000);
                } else {
                    formatter = new DecimalFormat("#,##0");
                    return formatter.format(value);
                }
                
            case COMPLIANCE:
            case AUDIT_SCORE:
            case REGIONAL_PERFORMANCE:
                // Format percentages with 1 decimal
                formatter = new DecimalFormat("#,##0.0");
                return formatter.format(value);
                
            case TAXPAYERS:
                // Format whole numbers with commas
                formatter = new DecimalFormat("#,##0");
                return formatter.format(value);
                
            default:
                return String.valueOf(value);
        }
    }
    
    private void calculateAndDisplayTrend() {
        double trendPercentage = 0;
        
        if (previousValue > 0) {
            trendPercentage = ((currentValue - previousValue) / previousValue) * 100;
        } else if (currentValue > 0) {
            trendPercentage = 100; // Infinite growth from zero
        }
        
        // Set trend text
        String trendTextStr;
        String trendIconStr;
        String trendColor;
        
        if (trendPercentage > 0) {
            trendTextStr = String.format("+%.1f%% from last month", Math.abs(trendPercentage));
            trendIconStr = "▲"; // Up arrow
            trendColor = "#4CAF50"; // Green for positive
        } else if (trendPercentage < 0) {
            trendTextStr = String.format("-%.1f%% from last month", Math.abs(trendPercentage));
            trendIconStr = "▼"; // Down arrow
            trendColor = "#F44336"; // Red for negative
        } else {
            trendTextStr = "No change from last month";
            trendIconStr = "➡"; // Right arrow
            trendColor = "#FF9800"; // Orange for neutral
        }
        
        trendText.setText(trendTextStr);
        trendIcon.setText(trendIconStr);
        trendText.setStyle("-fx-text-fill: " + trendColor + "; -fx-font-size: 11px;");
        trendIcon.setStyle("-fx-text-fill: " + trendColor + "; -fx-font-size: 10px;");
    }
    
    private void setupAnimations() {
        // Create value counting animation
        valueAnimation = new Timeline();
        valueAnimation.setCycleCount(1);
        
        // Add mouse hover animation
        kpiCard.setOnMouseEntered(e -> {
            animateCardScale(1.02);
            showDetailedTooltip();
        });
        
        kpiCard.setOnMouseExited(e -> {
            animateCardScale(1.0);
            hideDetailedTooltip();
        });
    }
    
    private void animateCardScale(double scale) {
        Timeline scaleAnimation = new Timeline(
            new KeyFrame(Duration.millis(200),
                new KeyValue(kpiCard.scaleXProperty(), scale),
                new KeyValue(kpiCard.scaleYProperty(), scale)
            )
        );
        scaleAnimation.play();
    }
    
    private void setupTooltips() {
        detailedTooltip = new Tooltip();
        detailedTooltip.setStyle("-fx-font-size: 12px; -fx-background-color: rgba(0,0,0,0.9); -fx-text-fill: white;");
        detailedTooltip.setAutoHide(true);
        detailedTooltip.setHideDelay(Duration.seconds(5));
        
        // Set tooltip text
        updateTooltipContent();
    }
    
    private void updateTooltipContent() {
        String tooltipText = String.format(
            "📊 %s\n" +
            "────────────────────\n" +
            "Current Value: %s\n" +
            "Previous Month: %s\n" +
            "Change: %s\n" +
            "Updated: %s\n" +
            "\nClick 'More' for detailed analysis",
            kpiType.getEnglishTitle(),
            formatDetailedValue(currentValue),
            formatDetailedValue(previousValue),
            calculateTrendPercentage(),
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        );
        
        detailedTooltip.setText(tooltipText);
        Tooltip.install(kpiCard, detailedTooltip);
    }
    
    private String formatDetailedValue(double value) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.US);
        
        switch (kpiType) {
            case REVENUE:
            case COLLECTIONS:
                if (value >= 1000000) {
                    return String.format("%s ETB %.2fM", currency, value / 1000000);
                } else if (value >= 1000) {
                    return String.format("%s ETB %.2fK", currency, value / 1000);
                } else {
                    return String.format("%s ETB %.0f", currency, value);
                }
                
            case COMPLIANCE:
            case AUDIT_SCORE:
            case REGIONAL_PERFORMANCE:
                return String.format("%.2f%%", value);
                
            case TAXPAYERS:
                return formatter.format(value) + " taxpayers";
                
            default:
                return String.valueOf(value);
        }
    }
    
    private String calculateTrendPercentage() {
        if (previousValue == 0) return "N/A";
        
        double percentage = ((currentValue - previousValue) / previousValue) * 100;
        String trend = percentage >= 0 ? "↑" : "↓";
        
        return String.format("%s %.2f%%", trend, Math.abs(percentage));
    }
    
    private void showDetailedTooltip() {
        updateTooltipContent();
        detailedTooltip.show(kpiCard.getScene().getWindow(),
            kpiCard.localToScreen(kpiCard.getBoundsInLocal()).getMinX(),
            kpiCard.localToScreen(kpiCard.getBoundsInLocal()).getMaxY() + 5
        );
    }
    
    private void hideDetailedTooltip() {
        detailedTooltip.hide();
    }
    
    private void setupEventHandlers() {
        // More button click handler
        moreBtn.setOnAction(this::handleMoreButtonClick);
        
        // Double click on card to refresh
        kpiCard.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                refreshKPIData();
            }
        });
    }
    
    @FXML
    private void handleMoreButtonClick(ActionEvent event) {
        try {
            // Navigate to detailed view based on KPI type
            String fxmlPath = getDetailedViewPath();
            
            if (fxmlPath != null) {
                Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
                Stage stage = new Stage();
                stage.setTitle(kpiType.getEnglishTitle() + " - Detailed View");
                stage.setScene(new Scene(root, 800, 600));
                stage.show();
                
                // Log the action
                logKPIAccess(kpiType, "Detailed view opened");
            } else {
                // Show detailed info in dialog
                showDetailedInfoDialog();
            }
            
        } catch (Exception e) {
            showAlert("Navigation Error", "Cannot open detailed view: " + e.getMessage());
        }
    }
    
    private String getDetailedViewPath() {
        switch (kpiType) {
            case REVENUE:
                return "/TMSFXML/RevenueDetails.fxml";
            case TAXPAYERS:
                return "/TMSFXML/TaxpayerDetails.fxml";
            case COMPLIANCE:
                return "/TMSFXML/ComplianceDetails.fxml";
            case COLLECTIONS:
                return "/TMSFXML/CollectionsDetails.fxml";
            case AUDIT_SCORE:
                return "/TMSFXML/AuditDetails.fxml";
            case REGIONAL_PERFORMANCE:
                return "/TMSFXML/RegionalDetails.fxml";
            default:
                return null;
        }
    }
    
    private void showDetailedInfoDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(kpiType.getEnglishTitle() + " - Detailed Information");
        alert.setHeaderText("Detailed Analysis for " + kpiType.getEnglishTitle());
        
        String content = String.format(
            "📈 %s Analysis\n\n" +
            "Current Value: %s\n" +
            "Previous Month: %s\n" +
            "Monthly Change: %s\n" +
            "Year-to-Date: %s\n" +
            "Target: %s\n" +
            "Achievement: %.1f%%\n\n" +
            "Last Updated: %s\n" +
            "Next Update: %s",
            kpiType.getEnglishTitle(),
            formatDetailedValue(currentValue),
            formatDetailedValue(previousValue),
            calculateTrendPercentage(),
            getYearToDateValue(),
            getTargetValue(),
            calculateAchievementRate(),
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        );
        
        alert.setContentText(content);
        alert.showAndWait();
    }
    
    private String getYearToDateValue() {
        try {
            String query = "";
            switch (kpiType) {
                case REVENUE:
                    query = "SELECT COALESCE(SUM(amount_paid), 0) as ytd FROM payments " +
                           "WHERE YEAR(payment_date) = YEAR(NOW())";
                    break;
                case TAXPAYERS:
                    query = "SELECT COUNT(*) as ytd FROM taxpayers " +
                           "WHERE YEAR(created_at) = YEAR(NOW())";
                    break;
                // Add other cases as needed
            }
            
            if (!query.isEmpty()) {
                PreparedStatement stmt = connection.prepareStatement(query);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return formatDetailedValue(rs.getDouble("ytd"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting YTD value: " + e.getMessage());
        }
        return "N/A";
    }
    
    private String getTargetValue() {
        // This would typically come from a targets table
        switch (kpiType) {
            case REVENUE:
                return "ETB 125.0M";
            case TAXPAYERS:
                return "10,000";
            case COMPLIANCE:
                return "85%";
            case COLLECTIONS:
                return "ETB 5.0M";
            case AUDIT_SCORE:
                return "90%";
            case REGIONAL_PERFORMANCE:
                return "80%";
            default:
                return "N/A";
        }
    }
    
    private double calculateAchievementRate() {
        double target = 0;
        
        switch (kpiType) {
            case REVENUE:
                target = 125000000; // ETB 125M
                break;
            case TAXPAYERS:
                target = 10000;
                break;
            case COMPLIANCE:
                target = 85;
                break;
            case COLLECTIONS:
                target = 5000000; // ETB 5M
                break;
            case AUDIT_SCORE:
                target = 90;
                break;
            case REGIONAL_PERFORMANCE:
                target = 80;
                break;
        }
        
        if (target > 0) {
            return (currentValue / target) * 100;
        }
        return 0;
    }
    
    public void refreshKPIData() {
        try {
            loadKPIData();
            
            // Animate value change
            animateValueChange();
            
            // Log refresh
            logKPIAccess(kpiType, "Data refreshed");
            
        } catch (SQLException e) {
            System.err.println("Error refreshing KPI data: " + e.getMessage());
        }
    }
    
    private void animateValueChange() {
        // Create counting animation
        final double startValue = Double.parseDouble(valueLabel.getText().replaceAll("[^\\d.]", ""));
        final double endValue = currentValue;
        
        valueAnimation.getKeyFrames().clear();
        valueAnimation.getKeyFrames().add(
            new KeyFrame(Duration.seconds(1.5),
                new KeyValue(valueLabel.textProperty(), formatValue(endValue))
            )
        );
        
        valueAnimation.playFromStart();
    }
    
    private void logKPIAccess(KPIType type, String action) {
        try {
            String query = "INSERT INTO kpi_access_logs (kpi_type, action, user_id, access_time) " +
                          "VALUES (?, ?, ?, NOW())";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, type.toString());
            stmt.setString(2, action);
            stmt.setString(3, "admin"); // Replace with actual user
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error logging KPI access: " + e.getMessage());
        }
    }
    
    private void setErrorState() {
        valueLabel.setText("Error");
        valueLabel.setStyle("-fx-text-fill: #dc3545;");
        trendText.setText("Data unavailable");
        trendIcon.setText("⚠");
        trendIcon.setStyle("-fx-text-fill: #dc3545;");
        
        // Disable more button
        moreBtn.setDisable(true);
        moreBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: #666666;");
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Public methods for external access
    public void setValue(double value) {
        this.currentValue = value;
        updateDisplayValues();
    }
    
    public void setPreviousValue(double value) {
        this.previousValue = value;
        calculateAndDisplayTrend();
    }
    
    public double getCurrentValue() {
        return currentValue;
    }
    
    public double getPreviousValue() {
        return previousValue;
    }
    
    public KPIType getKpiType() {
        return kpiType;
    }
    
    // Cleanup method
    public void cleanup() {
        try {
            if (valueAnimation != null) {
                valueAnimation.stop();
            }
            
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            
        } catch (SQLException e) {
            System.err.println("Error cleaning up KPI card: " + e.getMessage());
        }
    }
}