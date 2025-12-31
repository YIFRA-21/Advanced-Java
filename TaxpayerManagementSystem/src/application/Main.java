package application;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Main extends Application {
    
    private static Stage primaryStage;
    private static Map<String, Parent> screens = new HashMap<>();
    private static Map<String, Object> controllers = new HashMap<>();
    
    private static final String APP_TITLE = "Taxpayer Management System";
    
    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            primaryStage.setTitle(APP_TITLE);
            
            // Set application icon
            setApplicationIcon();
            
            // Show splash screen first
            showSplashScreen();
            
            // Load all screens
            loadScreens();
            
        } catch(Exception e) {
            showErrorScreen("Startup Error", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setApplicationIcon() {
        try {
            InputStream iconStream = getClass().getResourceAsStream("/application/image/tax logo.jpg");
            if (iconStream != null) {
                Image icon = new Image(iconStream);
                primaryStage.getIcons().add(icon);
            }
        } catch (Exception e) {
            // Icon is optional
        }
    }
    
    private void showSplashScreen() {
        VBox splashLayout = new VBox(20);
        splashLayout.setAlignment(Pos.CENTER);
        splashLayout.setPadding(new Insets(40));
        splashLayout.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #3498db);");
        
        Label appTitle = new Label(APP_TITLE);
        appTitle.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        Label loadingLabel = new Label("Initializing Taxpayer Management System...");
        loadingLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        
        ProgressIndicator progress = new ProgressIndicator();
        progress.setStyle("-fx-progress-color: white;");
        
        splashLayout.getChildren().addAll(appTitle, loadingLabel, progress);
        
        Scene splashScene = new Scene(splashLayout, 600, 400);
        primaryStage.setScene(splashScene);
        primaryStage.show();
    }
    
    private void loadScreens() {
        // Load only essential screens first
        String[] essentialScreens = {
            "Login", "Login.fxml",
            "Dashboard", "Dashboard.fxml",
            "ForgotPassword", "ForgotPassword.fxml",
            "SplashScreen", "SplashScreen.fxml"
        };
        
        // Load essential screens first
        for (int i = 0; i < essentialScreens.length; i += 2) {
            String screenName = essentialScreens[i];
            String fxmlFile = essentialScreens[i + 1];
            
            try {
                Parent screen = loadScreen(screenName, fxmlFile);
                screens.put(screenName, screen);
            } catch (Exception e) {
                System.err.println("Failed to load " + screenName + ": " + e.getMessage());
            }
        }
        
        // Switch to login screen
        switchToScreen("Login", "Login");
    }
    
    private Parent loadScreen(String screenName, String fxmlFile) throws Exception {
        URL fxmlUrl = getClass().getResource("/TMSFXML/" + fxmlFile);
        
        if (fxmlUrl == null) {
            throw new RuntimeException("FXML file not found: /TMSFXML/" + fxmlFile);
        }
        
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        
        // Get controller if it exists
        Object controller = loader.getController();
        if (controller != null) {
            controllers.put(screenName, controller);
        }
        
        return root;
    }
    
    public static void switchToScreen(String screenName, String title) {
        try {
            if (!screens.containsKey(screenName)) {
                // Load screen if not already loaded
                URL fxmlUrl = Main.class.getResource("/TMSFXML/" + getFxmlFile(screenName));
                if (fxmlUrl == null) {
                    throw new Exception("Screen '" + screenName + "' not found.");
                }
                
                Parent screenRoot = FXMLLoader.load(fxmlUrl);
                screens.put(screenName, screenRoot);
            }
            
            Parent screenRoot = screens.get(screenName);
            Scene scene = new Scene(screenRoot, 1200, 800);
            
            // Apply CSS
            try {
                URL cssUrl = Main.class.getResource("/application/Style.css");
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                }
            } catch (Exception e) {
                // CSS optional
            }
            
            primaryStage.setScene(scene);
            primaryStage.setTitle(APP_TITLE + " - " + title);
            
            if (screenName.equals("Dashboard") || screenName.equals("Admin")) {
                primaryStage.setMaximized(true);
            } else {
                primaryStage.setMaximized(false);
                primaryStage.setWidth(1200);
                primaryStage.setHeight(800);
                primaryStage.centerOnScreen();
            }
            
        } catch (Exception e) {
            showErrorAlert("Navigation Error", "Cannot open " + screenName + ": " + e.getMessage());
        }
    }
    
    private static String getFxmlFile(String screenName) {
        // Map screen names to FXML files
        switch(screenName) {
            case "Login": return "Login.fxml";
            case "Dashboard": return "Dashboard.fxml";
            case "ForgotPassword": return "ForgotPassword.fxml";
            case "SplashScreen": return "SplashScreen.fxml";
            case "Admin": return "Admin.fxml";
            case "TaxpayerManagement": return "TaxpayerManagement.fxml";
            case "TaxpayerForm": return "TaxpayerForm.fxml";
            case "AssessmentForm": return "AssessmentForm.fxml";
            case "PaymentProcessing": return "PaymentProcessing.fxml";
            case "NewPayment": return "newpayment.fxml";
            case "Audit": return "Audit.fxml";
            case "AuditReport": return "AuditReport.fxml";
            case "Reports": return "Reports.fxml";
            case "RevenueReport": return "RevenueReport.fxml";
            case "ComplianceReport": return "ComplianceReport.fxml";
            case "TaxpayerReport": return "TaxpayerReport.fxml";
            case "SystemSettings": return "SystemSettings.fxml";
            case "BackupRecovery": return "BackupRecovery.fxml";
            case "ExportLogs": return "ExportLogs.fxml";
            case "KPICard": return "KPICard.fxml";
            case "SignUp": return "Signup.fxml";
            default: return screenName + ".fxml";
        }
    }
    
    private void showErrorScreen(String title, String message) {
        VBox errorLayout = new VBox(20);
        errorLayout.setAlignment(Pos.CENTER);
        errorLayout.setPadding(new Insets(40));
        
        Label errorTitle = new Label("⚠ " + title);
        errorTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: red;");
        
        Label errorMessage = new Label(message);
        errorMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-wrap-text: true;");
        
        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> primaryStage.close());
        
        errorLayout.getChildren().addAll(errorTitle, errorMessage, closeBtn);
        
        Scene scene = new Scene(errorLayout, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void showInfoAlert(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public static void showErrorAlert(String title, String message) {
        javafx.application.Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public static Object getController(String screenName) {
        return controllers.get(screenName);
    }
    
    public static boolean hasScreen(String screenName) {
        return screens.containsKey(screenName);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}