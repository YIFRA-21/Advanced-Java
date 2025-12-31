package TMSController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert.AlertType;
import java.sql.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AssessmentFormController {

    // ... [All your @FXML annotations remain the same] ...
    @FXML private TableColumn<Assessment, String> Actions;
    @FXML private VBox AssessmentDetails;
    @FXML private TableColumn<Assessment, Integer> AssessmentID;
    @FXML private VBox CrateAssessment;
    @FXML private HBox DateorStatus;
    @FXML private Button Filterbtn;
    @FXML private VBox FinalizeAssessment;
    @FXML private VBox SelectTaxpayer;
    @FXML private TableColumn<Assessment, String> Status;
    @FXML private TableColumn<Assessment, Double> TaxAmount;
    @FXML private VBox TaxCalculation;
    @FXML private TableColumn<Assessment, Double> Taxincome;
    @FXML private TableColumn<Assessment, String> Taxpayer;
    @FXML private Label approvedAssessmentsLabel;
    @FXML private DatePicker assessmentDatePicker;
    @FXML private TextArea assessmentNotesField;
    @FXML private ComboBox<String> assessmentPeriodCombo;
    @FXML private ComboBox<String> assessmentStatusFilter;
    @FXML private TabPane assessmentTabs;
    @FXML private ComboBox<String> assessmentYearCombo;
    @FXML private TableView<Assessment> assessmentsTable;
    @FXML private Button calculateTaxBtn;
    @FXML private Label calculatedTaxLabel;
    @FXML private Button cancelAssessmentBtn;
    @FXML private DatePicker dueDatePicker;
    @FXML private Button newAssessmentBtn;
    @FXML private Button newTaxpayerBtn1;
    @FXML private Label pendingAssessmentsLabel;
    @FXML private TableColumn<Assessment, String> period;
    @FXML private Button saveAssessmentBtn;
    @FXML private Button searchTaxpayerBtn;
    @FXML private VBox selectedTaxpayerBox;
    @FXML private Label totalAssessmentsLabel;
    @FXML private Label totalTaxAmountLabel;
    @FXML private TextField txtdeductionsField;
    @FXML private TextField txtgrossIncomeField;
    @FXML private TextField txtsearchTaxpayerField;
    @FXML private TextField txttaxRateField;
    @FXML private TextField txttaxableIncomeField;
    @FXML private Tab viewAssessment;
    @FXML private DatePicker viewEndDate;
    @FXML private DatePicker viewStartDate;
    @FXML private TableColumn<Assessment, Integer> year;
    
    private ObservableList<Taxpayer> taxpayerList = FXCollections.observableArrayList();
    private ObservableList<Assessment> assessmentList = FXCollections.observableArrayList();
    private Taxpayer selectedTaxpayer = null;
    private Connection connection;
    private final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private final String DB_USER = "root";
    private final String DB_PASSWORD = "Belay2123";
    
    @FXML
    public void initialize() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Database connected successfully!");
            
            // Check and create missing columns if needed
            checkAndCreateMissingColumns();
            
            initializeComponents();
            loadDashboardStats();
            loadAssessments();
            
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            showAlert("Database Error", 
                     "Failed to connect to database: " + e.getMessage() + 
                     "\n\nPlease ensure:\n1. MySQL is running\n2. Database 'tax_management_db' exists\n3. Username/password is correct", 
                     AlertType.ERROR);
        }
    }
    
    /**
     * Check if required columns exist and create them if missing
     */
    private void checkAndCreateMissingColumns() {
        try {
            // Check taxpayers table columns
            DatabaseMetaData meta = connection.getMetaData();
            
            // Check first_name in taxpayers
            ResultSet columns = meta.getColumns(null, null, "taxpayers", "first_name");
            if (!columns.next()) {
                System.out.println("Column 'first_name' not found in taxpayers table. Creating it...");
                Statement stmt = connection.createStatement();
                stmt.executeUpdate("ALTER TABLE taxpayers ADD COLUMN first_name VARCHAR(100)");
                stmt.close();
                System.out.println("Column 'first_name' added to taxpayers table.");
            }
            columns.close();
            
            // Check last_name in taxpayers
            columns = meta.getColumns(null, null, "taxpayers", "last_name");
            if (!columns.next()) {
                System.out.println("Column 'last_name' not found in taxpayers table. Creating it...");
                Statement stmt = connection.createStatement();
                stmt.executeUpdate("ALTER TABLE taxpayers ADD COLUMN last_name VARCHAR(100)");
                stmt.close();
                System.out.println("Column 'last_name' added to taxpayers table.");
            }
            columns.close();
            
            // Check business_name in taxpayers
            columns = meta.getColumns(null, null, "taxpayers", "business_name");
            if (!columns.next()) {
                System.out.println("Column 'business_name' not found in taxpayers table. Creating it...");
                Statement stmt = connection.createStatement();
                stmt.executeUpdate("ALTER TABLE taxpayers ADD COLUMN business_name VARCHAR(200)");
                stmt.close();
                System.out.println("Column 'business_name' added to taxpayers table.");
            }
            columns.close();
            
            // Check calculated_tax in tax_assessments
            columns = meta.getColumns(null, null, "tax_assessments", "calculated_tax");
            if (!columns.next()) {
                System.out.println("Column 'calculated_tax' not found in tax_assessments table. Creating it...");
                Statement stmt = connection.createStatement();
                stmt.executeUpdate("ALTER TABLE tax_assessments ADD COLUMN calculated_tax DECIMAL(15,2) DEFAULT 0");
                stmt.close();
                System.out.println("Column 'calculated_tax' added to tax_assessments table.");
            }
            columns.close();
            
        } catch (SQLException e) {
            System.err.println("Error checking/creating columns: " + e.getMessage());
        }
    }
    
    private void initializeComponents() {
        // Initialize combo boxes
        initializeComboBoxes();
        
        // Initialize date pickers with default values
        assessmentDatePicker.setValue(LocalDate.now());
        dueDatePicker.setValue(LocalDate.now().plusDays(30));
        viewStartDate.setValue(LocalDate.now().minusMonths(1));
        viewEndDate.setValue(LocalDate.now());
        
        // Initialize table columns
        initializeTableColumns();
        
        // Set default tab
        assessmentTabs.getSelectionModel().select(0);
        
        // Set up listeners
        setupListeners();
    }
    
    private void initializeComboBoxes() {
        // Assessment year (current year and 5 years back)
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear - 5; i <= currentYear + 1; i++) {
            assessmentYearCombo.getItems().add(String.valueOf(i));
        }
        assessmentYearCombo.setValue(String.valueOf(currentYear));
        
        // Assessment period
        assessmentPeriodCombo.getItems().addAll("Q1", "Q2", "Q3", "Q4", "Annual");
        assessmentPeriodCombo.setValue("Annual");
        
        // Status filter
        assessmentStatusFilter.getItems().addAll("All", "Pending", "Approved", "Rejected", "Paid");
        assessmentStatusFilter.setValue("All");
    }
    
    private void initializeTableColumns() {
        // Assessment table columns
        AssessmentID.setCellValueFactory(new PropertyValueFactory<>("assessmentId"));
        Taxpayer.setCellValueFactory(new PropertyValueFactory<>("taxpayerName"));
        year.setCellValueFactory(new PropertyValueFactory<>("assessmentYear"));
        period.setCellValueFactory(new PropertyValueFactory<>("assessmentPeriod"));
        Taxincome.setCellValueFactory(new PropertyValueFactory<>("taxableIncome"));
        TaxAmount.setCellValueFactory(new PropertyValueFactory<>("calculatedTax"));
        Status.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Actions column with buttons
        Actions.setCellFactory(col -> new TableCell<Assessment, String>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final Button approveBtn = new Button("Approve");
            private final Button deleteBtn = new Button("Delete");
            
            {
                viewBtn.setOnAction(e -> {
                    Assessment assessment = getTableView().getItems().get(getIndex());
                    viewAssessment(assessment);
                });
                
                editBtn.setOnAction(e -> {
                    Assessment assessment = getTableView().getItems().get(getIndex());
                    editAssessment(assessment);
                });
                
                approveBtn.setOnAction(e -> {
                    Assessment assessment = getTableView().getItems().get(getIndex());
                    approveAssessment(assessment.getAssessmentId());
                });
                
                deleteBtn.setOnAction(e -> {
                    Assessment assessment = getTableView().getItems().get(getIndex());
                    deleteAssessment(assessment.getAssessmentId());
                });
                
                // Style buttons
                viewBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 10px;");
                editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10px;");
                approveBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 10px;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 10px;");
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Assessment assessment = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5, viewBtn, editBtn);
                    
                    // Show approve button only for pending assessments
                    if ("Pending".equals(assessment.getStatus())) {
                        buttons.getChildren().add(approveBtn);
                    }
                    
                    buttons.getChildren().add(deleteBtn);
                    setGraphic(buttons);
                }
            }
        });
        
        assessmentsTable.setItems(assessmentList);
    }
    
    private void setupListeners() {
        // Auto-calculate taxable income when gross income or deductions change
        txtgrossIncomeField.textProperty().addListener((observable, oldValue, newValue) -> {
            calculateTaxableIncome();
        });
        
        txtdeductionsField.textProperty().addListener((observable, oldValue, newValue) -> {
            calculateTaxableIncome();
        });
        
        // Auto-calculate tax when taxable income or tax rate changes
        txttaxableIncomeField.textProperty().addListener((observable, oldValue, newValue) -> {
            calculateTax();
        });
        
        txttaxRateField.textProperty().addListener((observable, oldValue, newValue) -> {
            calculateTax();
        });
    }
    
    // ==================== EVENT HANDLERS ====================
    
    @FXML
    private void handleNewAssessment(ActionEvent event) {
        assessmentTabs.getSelectionModel().select(0);
        clearAssessmentForm();
    }
    
    @FXML
    private void handleBackToDashboard(ActionEvent event) {
        try {
            // Try multiple possible paths for Dashboard.fxml
            String[] possiblePaths = {
                "/TMSFXML/Dashboard.fxml",
                "/Dashboard.fxml",
                "../TMSFXML/Dashboard.fxml",
                "../Dashboard.fxml",
                "Dashboard.fxml"
            };
            
            FXMLLoader loader = null;
            Parent dashboardRoot = null;
            
            for (String path : possiblePaths) {
                try {
                    loader = new FXMLLoader(getClass().getResource(path));
                    dashboardRoot = loader.load();
                    System.out.println("Successfully loaded dashboard from: " + path);
                    break;
                } catch (Exception e) {
                    System.err.println("Failed to load from " + path + ": " + e.getMessage());
                }
            }
            
            if (dashboardRoot == null) {
                throw new IOException("Could not find Dashboard.fxml in any location");
            }
            
            Stage currentStage = (Stage) newTaxpayerBtn1.getScene().getWindow();
            Scene dashboardScene = new Scene(dashboardRoot);
            
            currentStage.setScene(dashboardScene);
            currentStage.setTitle("Dashboard");
            currentStage.show();
            
        } catch (IOException e) {
            System.err.println("Navigation error: " + e.getMessage());
            showAlert("Navigation Error", 
                     "Failed to load Dashboard: " + e.getMessage() +
                     "\n\nPlease ensure Dashboard.fxml exists in the TMSFXML folder.", 
                     AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleSearchTaxpayerField(ActionEvent event) {
        searchTaxpayer();
    }
    
    @FXML
    private void handleSearchTaxpayer(ActionEvent event) {
        searchTaxpayer();
    }
    
    @FXML
    private void handleAssessmentYearCombo(ActionEvent event) {
        // Year selection changed
    }
    
    @FXML
    private void handleAssessmentPeriodCombo(ActionEvent event) {
        // Period selection changed
    }
    
    @FXML
    private void handleCalculateTax(ActionEvent event) {
        calculateTax();
    }
    
    @FXML
    private void handleAssessmentDatePicker(ActionEvent event) {
        if (assessmentDatePicker.getValue() != null) {
            dueDatePicker.setValue(assessmentDatePicker.getValue().plusDays(30));
        }
    }
    
    @FXML
    private void handleDueDatePicker(ActionEvent event) {
        // Due date selection changed
    }
    
    @FXML
    private void handleSaveAssessment(ActionEvent event) {
        saveAssessment();
    }
    
    @FXML
    private void handleCancelAssessment(ActionEvent event) {
        clearAssessmentForm();
    }
    
    @FXML
    private void handleViewStartDate(ActionEvent event) {
        loadAssessments();
    }
    
    @FXML
    private void handleViewEndDate(ActionEvent event) {
        loadAssessments();
    }
    
    @FXML
    private void handleAssessmentStatusFilter(ActionEvent event) {
        loadAssessments();
    }
    
    @FXML
    private void handleFilterAssessments(ActionEvent event) {
        loadAssessments();
    }
    
    // ==================== HELPER METHODS ====================
    
    private void searchTaxpayer() {
        String searchTerm = txtsearchTaxpayerField.getText().trim();
        if (searchTerm.isEmpty()) {
            showAlert("Search Error", "Please enter TIN or name to search", AlertType.WARNING);
            return;
        }
        
        try {
            // Use COALESCE to handle null columns
            String query = "SELECT *, " +
                         "COALESCE(first_name, '') as first_name, " +
                         "COALESCE(last_name, '') as last_name, " +
                         "COALESCE(business_name, '') as business_name " +
                         "FROM taxpayers " +
                         "WHERE tin LIKE ? OR first_name LIKE ? OR last_name LIKE ? OR business_name LIKE ?";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            String likeTerm = "%" + searchTerm + "%";
            stmt.setString(1, likeTerm);
            stmt.setString(2, likeTerm);
            stmt.setString(3, likeTerm);
            stmt.setString(4, likeTerm);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                selectedTaxpayer = new Taxpayer(
                    rs.getInt("taxpayer_id"),
                    rs.getString("tin"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("business_name"),
                    rs.getString("phone_number"),
                    rs.getString("email")
                );
                
                // Display selected taxpayer
                selectedTaxpayerBox.getChildren().clear();
                
                Label taxpayerInfo = new Label();
                taxpayerInfo.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 14;");
                
                String displayText = "TIN: " + selectedTaxpayer.getTin();
                if (!selectedTaxpayer.getFirstName().isEmpty() || !selectedTaxpayer.getLastName().isEmpty()) {
                    displayText += "\nName: " + selectedTaxpayer.getFullName();
                }
                if (selectedTaxpayer.getBusinessName() != null && !selectedTaxpayer.getBusinessName().isEmpty()) {
                    displayText += "\nBusiness: " + selectedTaxpayer.getBusinessName();
                }
                
                taxpayerInfo.setText(displayText);
                selectedTaxpayerBox.getChildren().add(taxpayerInfo);
                selectedTaxpayerBox.setVisible(true);
                
                showAlert("Taxpayer Found", "Taxpayer selected: " + selectedTaxpayer.getFullName(), AlertType.INFORMATION);
            } else {
                showAlert("Not Found", "No taxpayer found with: " + searchTerm, AlertType.WARNING);
                selectedTaxpayerBox.setVisible(false);
                selectedTaxpayer = null;
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Search error: " + e.getMessage());
            showAlert("Database Error", "Search failed: " + e.getMessage(), AlertType.ERROR);
        }
    }
    
    private void calculateTaxableIncome() {
        try {
            double grossIncome = txtgrossIncomeField.getText().isEmpty() ? 0 : 
                               Double.parseDouble(txtgrossIncomeField.getText());
            double deductions = txtdeductionsField.getText().isEmpty() ? 0 : 
                              Double.parseDouble(txtdeductionsField.getText());
            
            double taxableIncome = grossIncome - deductions;
            if (taxableIncome < 0) taxableIncome = 0;
            
            txttaxableIncomeField.setText(String.format("%.2f", taxableIncome));
            
            calculateTaxRate(taxableIncome);
            
        } catch (NumberFormatException e) {
            txttaxableIncomeField.setText("0.00");
        }
    }
    
    private void calculateTaxRate(double taxableIncome) {
        double taxRate = 0.0;
        
        if (taxableIncome <= 600) {
            taxRate = 0;
        } else if (taxableIncome <= 1650) {
            taxRate = 10;
        } else if (taxableIncome <= 3200) {
            taxRate = 15;
        } else if (taxableIncome <= 5250) {
            taxRate = 20;
        } else if (taxableIncome <= 7800) {
            taxRate = 25;
        } else if (taxableIncome <= 10900) {
            taxRate = 30;
        } else {
            taxRate = 35;
        }
        
        txttaxRateField.setText(String.format("%.0f", taxRate));
    }
    
    private void calculateTax() {
        try {
            double taxableIncome = txttaxableIncomeField.getText().isEmpty() ? 0 : 
                                 Double.parseDouble(txttaxableIncomeField.getText());
            double taxRate = txttaxRateField.getText().isEmpty() ? 0 : 
                           Double.parseDouble(txttaxRateField.getText());
            
            double calculatedTax = (taxableIncome * taxRate) / 100;
            calculatedTaxLabel.setText(String.format("ETB %.2f", calculatedTax));
            
        } catch (NumberFormatException e) {
            calculatedTaxLabel.setText("ETB 0.00");
        }
    }
    
    private void saveAssessment() {
        if (!validateAssessment()) {
            return;
        }
        
        try {
            String query = "INSERT INTO tax_assessments (taxpayer_id, assessment_year, assessment_period, " +
                         "gross_income, allowable_deductions, taxable_income, tax_rate, calculated_tax, " +
                         "assessment_date, due_date, notes, status) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Pending')";
            
            PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            
            stmt.setInt(1, selectedTaxpayer.getTaxpayerId());
            stmt.setInt(2, Integer.parseInt(assessmentYearCombo.getValue()));
            stmt.setString(3, assessmentPeriodCombo.getValue());
            stmt.setDouble(4, Double.parseDouble(txtgrossIncomeField.getText()));
            stmt.setDouble(5, Double.parseDouble(txtdeductionsField.getText()));
            stmt.setDouble(6, Double.parseDouble(txttaxableIncomeField.getText()));
            stmt.setDouble(7, Double.parseDouble(txttaxRateField.getText()));
            
            String taxText = calculatedTaxLabel.getText().replace("ETB", "").trim();
            double calculatedTax = Double.parseDouble(taxText);
            stmt.setDouble(8, calculatedTax);
            
            stmt.setDate(9, Date.valueOf(assessmentDatePicker.getValue()));
            stmt.setDate(10, Date.valueOf(dueDatePicker.getValue()));
            stmt.setString(11, assessmentNotesField.getText());
            
            int rows = stmt.executeUpdate();
            
            if (rows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int newAssessmentId = rs.getInt(1);
                    
                    Assessment newAssessment = new Assessment(
                        newAssessmentId,
                        selectedTaxpayer.getFullName(),
                        Integer.parseInt(assessmentYearCombo.getValue()),
                        assessmentPeriodCombo.getValue(),
                        Double.parseDouble(txttaxableIncomeField.getText()),
                        calculatedTax,
                        "Pending"
                    );
                    
                    assessmentList.add(0, newAssessment);
                    loadDashboardStats();
                    clearAssessmentForm();
                    
                    showAlert("Success", "Assessment saved successfully! Assessment ID: " + newAssessmentId, AlertType.INFORMATION);
                }
                rs.close();
            }
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Save assessment error: " + e.getMessage());
            showAlert("Database Error", "Failed to save assessment: " + e.getMessage(), AlertType.ERROR);
        }
    }
    
    private boolean validateAssessment() {
        if (selectedTaxpayer == null) {
            showAlert("Validation Error", "Please select a taxpayer", AlertType.ERROR);
            return false;
        }
        
        try {
            double grossIncome = Double.parseDouble(txtgrossIncomeField.getText());
            if (grossIncome < 0) {
                showAlert("Validation Error", "Gross income cannot be negative", AlertType.ERROR);
                txtgrossIncomeField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter valid gross income", AlertType.ERROR);
            txtgrossIncomeField.requestFocus();
            return false;
        }
        
        try {
            double deductions = Double.parseDouble(txtdeductionsField.getText());
            if (deductions < 0) {
                showAlert("Validation Error", "Deductions cannot be negative", AlertType.ERROR);
                txtdeductionsField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter valid deductions", AlertType.ERROR);
            txtdeductionsField.requestFocus();
            return false;
        }
        
        try {
            double taxRate = Double.parseDouble(txttaxRateField.getText());
            if (taxRate < 0 || taxRate > 100) {
                showAlert("Validation Error", "Tax rate must be between 0 and 100", AlertType.ERROR);
                txttaxRateField.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter valid tax rate", AlertType.ERROR);
            txttaxRateField.requestFocus();
            return false;
        }
        
        if (assessmentDatePicker.getValue() == null) {
            showAlert("Validation Error", "Please select assessment date", AlertType.ERROR);
            assessmentDatePicker.requestFocus();
            return false;
        }
        
        if (dueDatePicker.getValue() == null) {
            showAlert("Validation Error", "Please select due date", AlertType.ERROR);
            dueDatePicker.requestFocus();
            return false;
        }
        
        if (dueDatePicker.getValue().isBefore(assessmentDatePicker.getValue())) {
            showAlert("Validation Error", "Due date cannot be before assessment date", AlertType.ERROR);
            dueDatePicker.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void loadAssessments() {
        try {
            // Use COALESCE to handle potentially missing columns
            StringBuilder query = new StringBuilder(
                "SELECT a.*, " +
                "COALESCE(t.first_name, '') as first_name, " +
                "COALESCE(t.last_name, '') as last_name, " +
                "COALESCE(t.business_name, '') as business_name, " +
                "COALESCE(a.calculated_tax, 0) as calculated_tax " +
                "FROM tax_assessments a " +
                "LEFT JOIN taxpayers t ON a.taxpayer_id = t.taxpayer_id " +
                "WHERE 1=1"
            );
            
            // Build WHERE clause
            if (viewStartDate.getValue() != null) {
                query.append(" AND a.assessment_date >= ?");
            }
            
            if (viewEndDate.getValue() != null) {
                query.append(" AND a.assessment_date <= ?");
            }
            
            String statusFilter = assessmentStatusFilter.getValue();
            if (!"All".equals(statusFilter)) {
                query.append(" AND a.status = ?");
            }
            
            query.append(" ORDER BY a.assessment_date DESC");
            
            PreparedStatement stmt = connection.prepareStatement(query.toString());
            
            int paramIndex = 1;
            if (viewStartDate.getValue() != null) {
                stmt.setDate(paramIndex++, Date.valueOf(viewStartDate.getValue()));
            }
            if (viewEndDate.getValue() != null) {
                stmt.setDate(paramIndex++, Date.valueOf(viewEndDate.getValue()));
            }
            if (!"All".equals(statusFilter)) {
                stmt.setString(paramIndex, statusFilter);
            }
            
            ResultSet rs = stmt.executeQuery();
            
            assessmentList.clear();
            while (rs.next()) {
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String businessName = rs.getString("business_name");
                
                String taxpayerName = "";
                if (!firstName.isEmpty() || !lastName.isEmpty()) {
                    taxpayerName = firstName + " " + lastName;
                }
                
                if (businessName != null && !businessName.isEmpty()) {
                    if (!taxpayerName.isEmpty()) {
                        taxpayerName += " (" + businessName + ")";
                    } else {
                        taxpayerName = businessName;
                    }
                }
                
                // If still empty, use a placeholder
                if (taxpayerName.isEmpty()) {
                    taxpayerName = "Taxpayer ID: " + rs.getInt("taxpayer_id");
                }
                
                Assessment assessment = new Assessment(
                    rs.getInt("assessment_id"),
                    taxpayerName,
                    rs.getInt("assessment_year"),
                    rs.getString("assessment_period"),
                    rs.getDouble("taxable_income"),
                    rs.getDouble("calculated_tax"),
                    rs.getString("status")
                );
                assessmentList.add(assessment);
            }
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Load assessments error: " + e.getMessage());
            showAlert("Database Error", "Failed to load assessments: " + e.getMessage(), AlertType.ERROR);
        }
    }
    
    private void loadDashboardStats() {
        try {
            Statement stmt = connection.createStatement();
            
            // Total assessments
            String totalQuery = "SELECT COUNT(*) as total FROM tax_assessments";
            ResultSet rs = stmt.executeQuery(totalQuery);
            if (rs.next()) {
                totalAssessmentsLabel.setText(String.valueOf(rs.getInt("total")));
            }
            
            // Pending assessments
            String pendingQuery = "SELECT COUNT(*) as pending FROM tax_assessments WHERE status = 'Pending'";
            rs = stmt.executeQuery(pendingQuery);
            if (rs.next()) {
                pendingAssessmentsLabel.setText(String.valueOf(rs.getInt("pending")));
            }
            
            // Approved assessments
            String approvedQuery = "SELECT COUNT(*) as approved FROM tax_assessments WHERE status = 'Approved'";
            rs = stmt.executeQuery(approvedQuery);
            if (rs.next()) {
                approvedAssessmentsLabel.setText(String.valueOf(rs.getInt("approved")));
            }
            
            // Total tax amount - use COALESCE to handle null
            String taxQuery = "SELECT COALESCE(SUM(calculated_tax), 0) as total_tax " +
                            "FROM tax_assessments WHERE status IN ('Approved', 'Paid')";
            rs = stmt.executeQuery(taxQuery);
            if (rs.next()) {
                double totalTax = rs.getDouble("total_tax");
                totalTaxAmountLabel.setText(String.format("ETB %.2f", totalTax));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Load dashboard stats error: " + e.getMessage());
            // Set default values
            totalAssessmentsLabel.setText("0");
            pendingAssessmentsLabel.setText("0");
            approvedAssessmentsLabel.setText("0");
            totalTaxAmountLabel.setText("ETB 0.00");
        }
    }
    
    private void viewAssessment(Assessment assessment) {
        try {
            String query = "SELECT a.*, " +
                         "COALESCE(t.first_name, '') as first_name, " +
                         "COALESCE(t.last_name, '') as last_name, " +
                         "COALESCE(t.tin, '') as tin, " +
                         "COALESCE(t.business_name, '') as business_name, " +
                         "COALESCE(a.calculated_tax, 0) as calculated_tax " +
                         "FROM tax_assessments a " +
                         "LEFT JOIN taxpayers t ON a.taxpayer_id = t.taxpayer_id " +
                         "WHERE a.assessment_id = ?";
            
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, assessment.getAssessmentId());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Alert dialog = new Alert(AlertType.INFORMATION);
                dialog.setTitle("Assessment Details");
                dialog.setHeaderText("Assessment ID: " + rs.getInt("assessment_id"));
                
                StringBuilder content = new StringBuilder();
                content.append("Taxpayer: ").append(rs.getString("first_name")).append(" ").append(rs.getString("last_name")).append("\n");
                content.append("TIN: ").append(rs.getString("tin")).append("\n");
                content.append("Business: ").append(rs.getString("business_name")).append("\n");
                content.append("Year: ").append(rs.getInt("assessment_year")).append("\n");
                content.append("Period: ").append(rs.getString("assessment_period")).append("\n");
                content.append("Gross Income: ETB ").append(String.format("%.2f", rs.getDouble("gross_income"))).append("\n");
                content.append("Deductions: ETB ").append(String.format("%.2f", rs.getDouble("allowable_deductions"))).append("\n");
                content.append("Taxable Income: ETB ").append(String.format("%.2f", rs.getDouble("taxable_income"))).append("\n");
                content.append("Tax Rate: ").append(rs.getDouble("tax_rate")).append("%\n");
                content.append("Calculated Tax: ETB ").append(String.format("%.2f", rs.getDouble("calculated_tax"))).append("\n");
                content.append("Assessment Date: ").append(rs.getDate("assessment_date")).append("\n");
                content.append("Due Date: ").append(rs.getDate("due_date")).append("\n");
                content.append("Status: ").append(rs.getString("status")).append("\n");
                content.append("Notes: ").append(rs.getString("notes") != null ? rs.getString("notes") : "N/A");
                
                dialog.setContentText(content.toString());
                dialog.showAndWait();
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("View assessment error: " + e.getMessage());
            showAlert("Database Error", "Failed to load assessment details: " + e.getMessage(), AlertType.ERROR);
        }
    }
    
    private void editAssessment(Assessment assessment) {
        try {
            String query = "SELECT * FROM tax_assessments WHERE assessment_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, assessment.getAssessmentId());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                assessmentTabs.getSelectionModel().select(0);
                
                // Load the taxpayer
                String taxpayerQuery = "SELECT *, " +
                                     "COALESCE(first_name, '') as first_name, " +
                                     "COALESCE(last_name, '') as last_name, " +
                                     "COALESCE(business_name, '') as business_name " +
                                     "FROM taxpayers WHERE taxpayer_id = ?";
                PreparedStatement taxpayerStmt = connection.prepareStatement(taxpayerQuery);
                taxpayerStmt.setInt(1, rs.getInt("taxpayer_id"));
                ResultSet taxpayerRs = taxpayerStmt.executeQuery();
                
                if (taxpayerRs.next()) {
                    selectedTaxpayer = new Taxpayer(
                        taxpayerRs.getInt("taxpayer_id"),
                        taxpayerRs.getString("tin"),
                        taxpayerRs.getString("first_name"),
                        taxpayerRs.getString("last_name"),
                        taxpayerRs.getString("business_name"),
                        taxpayerRs.getString("phone_number"),
                        taxpayerRs.getString("email")
                    );
                    
                    selectedTaxpayerBox.getChildren().clear();
                    Label taxpayerInfo = new Label();
                    taxpayerInfo.setText("TIN: " + selectedTaxpayer.getTin() + 
                                       "\nName: " + selectedTaxpayer.getFullName());
                    selectedTaxpayerBox.getChildren().add(taxpayerInfo);
                    selectedTaxpayerBox.setVisible(true);
                }
                taxpayerRs.close();
                taxpayerStmt.close();
                
                // Load assessment data
                assessmentYearCombo.setValue(String.valueOf(rs.getInt("assessment_year")));
                assessmentPeriodCombo.setValue(rs.getString("assessment_period"));
                txtgrossIncomeField.setText(String.format("%.2f", rs.getDouble("gross_income")));
                txtdeductionsField.setText(String.format("%.2f", rs.getDouble("allowable_deductions")));
                txttaxableIncomeField.setText(String.format("%.2f", rs.getDouble("taxable_income")));
                txttaxRateField.setText(String.format("%.0f", rs.getDouble("tax_rate")));
                calculatedTaxLabel.setText(String.format("ETB %.2f", rs.getDouble("calculated_tax")));
                assessmentDatePicker.setValue(rs.getDate("assessment_date").toLocalDate());
                dueDatePicker.setValue(rs.getDate("due_date").toLocalDate());
                assessmentNotesField.setText(rs.getString("notes"));
                
                saveAssessmentBtn.setText("Update Assessment");
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Edit assessment error: " + e.getMessage());
            showAlert("Database Error", "Failed to load assessment for editing: " + e.getMessage(), AlertType.ERROR);
        }
    }
    
    private void approveAssessment(int assessmentId) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Approval");
        confirm.setHeaderText("Approve Assessment");
        confirm.setContentText("Are you sure you want to approve this assessment?");
        
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    String query = "UPDATE tax_assessments SET status = 'Approved' WHERE assessment_id = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setInt(1, assessmentId);
                    stmt.executeUpdate();
                    stmt.close();
                    
                    loadAssessments();
                    loadDashboardStats();
                    
                    showAlert("Success", "Assessment approved successfully!", AlertType.INFORMATION);
                    
                } catch (SQLException e) {
                    System.err.println("Approve assessment error: " + e.getMessage());
                    showAlert("Database Error", "Failed to approve assessment: " + e.getMessage(), AlertType.ERROR);
                }
            }
        });
    }
    
    private void deleteAssessment(int assessmentId) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Assessment");
        confirm.setContentText("Are you sure you want to delete this assessment?");
        
        confirm.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    String query = "DELETE FROM tax_assessments WHERE assessment_id = ?";
                    PreparedStatement stmt = connection.prepareStatement(query);
                    stmt.setInt(1, assessmentId);
                    stmt.executeUpdate();
                    stmt.close();
                    
                    assessmentList.removeIf(a -> a.getAssessmentId() == assessmentId);
                    loadDashboardStats();
                    
                    showAlert("Success", "Assessment deleted successfully!", AlertType.INFORMATION);
                    
                } catch (SQLException e) {
                    System.err.println("Delete assessment error: " + e.getMessage());
                    showAlert("Database Error", "Failed to delete assessment: " + e.getMessage(), AlertType.ERROR);
                }
            }
        });
    }
    
    private void clearAssessmentForm() {
        selectedTaxpayer = null;
        selectedTaxpayerBox.getChildren().clear();
        selectedTaxpayerBox.setVisible(false);
        txtsearchTaxpayerField.clear();
        txtgrossIncomeField.setText("0.00");
        txtdeductionsField.setText("0.00");
        txttaxableIncomeField.setText("0.00");
        txttaxRateField.setText("0");
        calculatedTaxLabel.setText("ETB 0.00");
        assessmentDatePicker.setValue(LocalDate.now());
        dueDatePicker.setValue(LocalDate.now().plusDays(30));
        assessmentNotesField.clear();
        saveAssessmentBtn.setText("Save Assessment");
    }
    
    private void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // ==================== MODEL CLASSES ====================
    
    public static class Taxpayer {
        private final int taxpayerId;
        private final String tin;
        private final String firstName;
        private final String lastName;
        private final String businessName;
        private final String phoneNumber;
        private final String email;
        
        public Taxpayer(int taxpayerId, String tin, String firstName, String lastName, 
                       String businessName, String phoneNumber, String email) {
            this.taxpayerId = taxpayerId;
            this.tin = tin;
            this.firstName = firstName;
            this.lastName = lastName;
            this.businessName = businessName;
            this.phoneNumber = phoneNumber;
            this.email = email;
        }
        
        public int getTaxpayerId() { return taxpayerId; }
        public String getTin() { return tin; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getFullName() { 
            return (firstName + " " + lastName).trim(); 
        }
        public String getBusinessName() { return businessName; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getEmail() { return email; }
    }
    
    public static class Assessment {
        private final int assessmentId;
        private final String taxpayerName;
        private final int assessmentYear;
        private final String assessmentPeriod;
        private final double taxableIncome;
        private final double calculatedTax;
        private final String status;
        
        public Assessment(int assessmentId, String taxpayerName, int assessmentYear, 
                         String assessmentPeriod, double taxableIncome, 
                         double calculatedTax, String status) {
            this.assessmentId = assessmentId;
            this.taxpayerName = taxpayerName;
            this.assessmentYear = assessmentYear;
            this.assessmentPeriod = assessmentPeriod;
            this.taxableIncome = taxableIncome;
            this.calculatedTax = calculatedTax;
            this.status = status;
        }
        
        public int getAssessmentId() { return assessmentId; }
        public String getTaxpayerName() { return taxpayerName; }
        public int getAssessmentYear() { return assessmentYear; }
        public String getAssessmentPeriod() { return assessmentPeriod; }
        public double getTaxableIncome() { return taxableIncome; }
        public double getCalculatedTax() { return calculatedTax; }
        public String getStatus() { return status; }
    }
}