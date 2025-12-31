package TMSController;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Logger;

public class SplashScreenController {

    private static final Logger LOGGER = Logger.getLogger(SplashScreenController.class.getName());
    
    // Database connection details (optional)
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    
    // FXML Elements from your simplified FXML
    @FXML private VBox splashLayout;
    @FXML private ImageView logoImageView;
    @FXML private ProgressIndicator progressIndicator;
    
    // Internal variables
    private Connection databaseConnection;
    private boolean databaseInitialized = false;
    
    @FXML
    public void initialize() {
        LOGGER.info("SplashScreenController initialized");
        
        try {
            // Setup UI
            setupUI();
            
            // Start splash screen sequence
            startSplashSequence();
            
        } catch (Exception e) {
            LOGGER.severe("Error in initialize: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: proceed to login if splash fails
            Platform.runLater(() -> {
                try {
                    proceedToLogin();
                } catch (Exception ex) {
                    LOGGER.severe("Failed to proceed to login: " + ex.getMessage());
                    showErrorAndExit();
                }
            });
        }
    }
    
    private void setupUI() {
        LOGGER.info("Setting up UI components...");
        
        try {
            // Set initial opacity for fade-in effect
            if (splashLayout != null) {
                splashLayout.setOpacity(0);
            }
            
            // Configure logo - with fallback
            if (logoImageView != null) {
                try {
                    // Try to load logo from common locations
                    Image logo = loadLogoImage();
                    if (logo != null) {
                        logoImageView.setImage(logo);
                        LOGGER.info("Logo image loaded successfully");
                    } else {
                        LOGGER.info("No logo image found, using default styling");
                        setupLogoPlaceholder();
                    }
                } catch (Exception e) {
                    LOGGER.warning("Failed to load logo image: " + e.getMessage());
                    setupLogoPlaceholder();
                }
            }
            
            // Configure progress indicator
            if (progressIndicator != null) {
                progressIndicator.setProgress(0);
                progressIndicator.setStyle("-fx-progress-color: white;");
                
                // Add pulsating effect
                addPulsatingEffect();
            }
            
            LOGGER.info("UI setup completed");
            
        } catch (Exception e) {
            LOGGER.severe("Error in setupUI: " + e.getMessage());
            throw e;
        }
    }
    
    private Image loadLogoImage() {
        // Try multiple possible logo paths
        String[] possiblePaths = {
            "/images/ers_logo.png",
            "/images/logo.png",
            "/application/image/tax logo.jpg",
            "/TMSFXML/images/ers_logo.png",
            "/resources/images/ers_logo.png",
            "/images/tax_logo.jpg"
        };
        
        for (String path : possiblePaths) {
            try {
                Image image = new Image(getClass().getResourceAsStream(path));
                if (image != null && !image.isError()) {
                    LOGGER.info("Logo found at: " + path);
                    return image;
                }
            } catch (Exception e) {
                // Try next path
                continue;
            }
        }
        
        LOGGER.info("No logo image found in any of the searched paths");
        return null;
    }
    
    private void setupLogoPlaceholder() {
        if (logoImageView != null) {
            // Create a simple placeholder with Ethiopian flag colors
            logoImageView.setStyle(
                "-fx-background-color: linear-gradient(to right, #078930 33%, #FCDD09 33%, #FCDD09 66%, #DA121A 66%); " +
                "-fx-border-color: white; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 75; " +  // Half of 150 for circle
                "-fx-background-radius: 75; " +  // Half of 150 for circle
                "-fx-min-width: 150; " +
                "-fx-min-height: 150;"
            );
        }
    }
    
    private void addPulsatingEffect() {
        if (progressIndicator != null) {
            Timeline pulseTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressIndicator.opacityProperty(), 1.0)),
                new KeyFrame(Duration.seconds(1), new KeyValue(progressIndicator.opacityProperty(), 0.5)),
                new KeyFrame(Duration.seconds(2), new KeyValue(progressIndicator.opacityProperty(), 1.0))
            );
            pulseTimeline.setCycleCount(Timeline.INDEFINITE);
            pulseTimeline.play();
        }
    }
    
    private void startSplashSequence() {
        LOGGER.info("Starting splash sequence");
        
        try {
            // Create fade-in animation
            if (splashLayout == null) {
                LOGGER.severe("splashLayout is null, cannot start animation");
                beginStartupTasks();
                return;
            }
            
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.5), splashLayout);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            
            fadeIn.setOnFinished(event -> {
                LOGGER.info("Fade-in animation completed");
                beginStartupTasks();
            });
            
            fadeIn.play();
            
        } catch (Exception e) {
            LOGGER.severe("Error starting splash sequence: " + e.getMessage());
            beginStartupTasks(); // Skip animations if they fail
        }
    }
    
    private void beginStartupTasks() {
        LOGGER.info("Beginning startup tasks");
        
        Task<Void> startupTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    updateProgress(0, 3);
                    Thread.sleep(300);
                    
                    // Task 1: Initialize Database (optional)
                    updateProgress(1, 3);
                    initializeDatabase();
                    Thread.sleep(500);
                    
                    // Task 2: System Check (simulated)
                    updateProgress(2, 3);
                    performSystemCheck();
                    Thread.sleep(500);
                    
                    // Task 3: Final Preparation
                    updateProgress(3, 3);
                    prepareApplication();
                    Thread.sleep(300);
                    
                } catch (InterruptedException e) {
                    LOGGER.info("Startup task interrupted");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    LOGGER.warning("Startup task error: " + e.getMessage());
                }
                return null;
            }
            
            @Override
            protected void succeeded() {
                LOGGER.info("Startup tasks completed successfully");
                transitionToLogin();
            }
            
            @Override
            protected void failed() {
                LOGGER.severe("Startup tasks failed: " + getException().getMessage());
                transitionToLogin(); // Still try to proceed
            }
        };
        
        // Bind progress to progress indicator
        if (progressIndicator != null) {
            progressIndicator.progressProperty().bind(startupTask.progressProperty());
        }
        
        // Start the startup task in a separate thread
        Thread startupThread = new Thread(startupTask);
        startupThread.setDaemon(true);
        startupThread.start();
    }
    
    private void initializeDatabase() {
        try {
            LOGGER.info("Attempting database connection...");
            
            // Load MySQL driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Establish connection with timeout
            DriverManager.setLoginTimeout(3);
            databaseConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            // Test connection
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                databaseInitialized = true;
                LOGGER.info("Database connection established successfully");
            }
            
        } catch (ClassNotFoundException e) {
            LOGGER.warning("MySQL JDBC Driver not found: " + e.getMessage());
            databaseInitialized = false;
        } catch (Exception e) {
            LOGGER.warning("Failed to connect to database: " + e.getMessage());
            databaseInitialized = false;
        }
    }
    
    private void performSystemCheck() {
        try {
            LOGGER.info("Performing system check...");
            
            // Simple system check
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsage = (double) usedMemory / maxMemory * 100;
            
            LOGGER.info(String.format("Memory usage: %.1f%%", memoryUsage));
            
        } catch (Exception e) {
            LOGGER.warning("System check failed: " + e.getMessage());
        }
    }
    
    private void prepareApplication() {
        try {
            LOGGER.info("Preparing application...");
            // Simulated preparation
            Thread.sleep(300);
            
        } catch (Exception e) {
            LOGGER.warning("Failed to prepare application: " + e.getMessage());
        }
    }
    
    private void transitionToLogin() {
        LOGGER.info("Transitioning to login screen...");
        
        Platform.runLater(() -> {
            try {
                // Create fade-out animation
                if (splashLayout != null) {
                    FadeTransition fadeOut = new FadeTransition(Duration.seconds(1), splashLayout);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    
                    fadeOut.setOnFinished(event -> {
                        proceedToLogin();
                    });
                    
                    fadeOut.play();
                } else {
                    proceedToLogin();
                }
                
            } catch (Exception e) {
                LOGGER.severe("Error in transition animation: " + e.getMessage());
                proceedToLogin();
            }
        });
    }
    
    private void proceedToLogin() {
        try {
            LOGGER.info("Proceeding to login screen...");
            
            // Close database connection if it exists
            closeDatabaseConnection();
            
            // Load login screen
            Stage currentStage = getCurrentStage();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/TMSFXML/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            
            currentStage.setScene(scene);
            currentStage.setTitle("ERS Tax Management System - Login");
            currentStage.centerOnScreen();
            currentStage.show();
            
            LOGGER.info("Successfully loaded login screen");
            
        } catch (Exception e) {
            LOGGER.severe("Failed to load login screen: " + e.getMessage());
            e.printStackTrace();
            showErrorAndExit();
        }
    }
    
    private Stage getCurrentStage() {
        if (splashLayout != null && splashLayout.getScene() != null) {
            return (Stage) splashLayout.getScene().getWindow();
        } else {
            LOGGER.warning("Could not get current stage, creating new one");
            return new Stage();
        }
    }
    
    private void showErrorAndExit() {
        Platform.runLater(() -> {
            try {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Application Error");
                alert.setHeaderText("Unable to Start Application");
                alert.setContentText("The application cannot start. Please check:\n" +
                                   "1. JavaFX is installed\n" +
                                   "2. FXML files are in correct location\n" +
                                   "3. Database is running (if required)\n\n" +
                                   "Application will now exit.");
                
                alert.showAndWait();
                
            } catch (Exception e) {
                LOGGER.severe("Critical error showing alert: " + e.getMessage());
            } finally {
                Platform.exit();
                System.exit(1);
            }
        });
    }
    
    private void closeDatabaseConnection() {
        try {
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                databaseConnection.close();
                LOGGER.info("Database connection closed");
            }
        } catch (Exception e) {
            LOGGER.warning("Error closing database connection: " + e.getMessage());
        }
    }
    
    // Public method to skip splash screen (for testing)
    public void skipSplashScreen() {
        LOGGER.info("Skipping splash screen");
        transitionToLogin();
    }
    
    // Public method to get database status
    public boolean isDatabaseInitialized() {
        return databaseInitialized;
    }
    
    // Cleanup method
    public void cleanup() {
        LOGGER.info("Cleaning up SplashScreenController");
        closeDatabaseConnection();
    }
}