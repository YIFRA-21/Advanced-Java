package TMSController;


import java.sql.*;

public class DatabaseFixer {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/tax_management_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Belay2123";
    
    public static void main(String[] args) {
        System.out.println("=== FIXING DATABASE SCHEMA ===");
        fixAllIssues();
    }
    
    private static void fixAllIssues() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("✓ Connected to database");
            
            // Fix 1: Add missing columns to users table
            fixUsersTable(conn);
            
            // Fix 2: Add missing columns to taxpayers table
            fixTaxpayersTable(conn);
            
            // Fix 3: Find and fix activity/log tables
            fixActivityTables(conn);
            
            System.out.println("\n=== ALL DATABASE FIXES COMPLETED ===");
            
        } catch (SQLException e) {
            System.err.println("✗ Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void fixUsersTable(Connection conn) {
        System.out.println("\n1. Fixing 'users' table...");
        
        String[] userFixes = {
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login DATETIME",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS login_attempts INT DEFAULT 0",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked BOOLEAN DEFAULT FALSE",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
        };
        
        executeUpdates(conn, userFixes, "users");
        
        // Update existing records
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE users SET is_active = TRUE WHERE is_active IS NULL");
            System.out.println("  ✓ Updated existing user records");
        } catch (SQLException e) {
            System.out.println("  ⚠ Could not update user records: " + e.getMessage());
        }
    }
    
    private static void fixTaxpayersTable(Connection conn) {
        System.out.println("\n2. Fixing 'taxpayers' table...");
        
        String[] taxpayerFixes = {
            "ALTER TABLE taxpayers ADD COLUMN IF NOT EXISTS compliance_status VARCHAR(50) DEFAULT 'Compliant'",
            "ALTER TABLE taxpayers ADD COLUMN IF NOT EXISTS assessment_status VARCHAR(50) DEFAULT 'Pending'",
            "ALTER TABLE taxpayers ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE",
            "ALTER TABLE taxpayers ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP",
            "ALTER TABLE taxpayers ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
        };
        
        executeUpdates(conn, taxpayerFixes, "taxpayers");
        
        // Update existing records
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE taxpayers SET compliance_status = 'Compliant' WHERE compliance_status IS NULL");
            stmt.executeUpdate("UPDATE taxpayers SET is_active = TRUE WHERE is_active IS NULL");
            System.out.println("  ✓ Updated existing taxpayer records");
        } catch (SQLException e) {
            System.out.println("  ⚠ Could not update taxpayer records: " + e.getMessage());
        }
    }
    
    private static void fixActivityTables(Connection conn) {
        System.out.println("\n3. Finding and fixing activity/log tables...");
        
        // Common activity table names
        String[] possibleActivityTables = {
            "user_activity", "activity_log", "system_log", "audit_log", 
            "logs", "user_logs", "system_logs", "audit_trail", "login_log"
        };
        
        for (String table : possibleActivityTables) {
            if (tableExists(conn, table)) {
                System.out.println("  Found table: " + table);
                fixActivityTable(conn, table);
            }
        }
    }
    
    private static void fixActivityTable(Connection conn, String tableName) {
        String[] activityFixes = {
            "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS description TEXT",
            "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45)",
            "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS user_agent TEXT",
            "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
        };
        
        executeUpdates(conn, activityFixes, tableName);
    }
    
    private static void executeUpdates(Connection conn, String[] sqlStatements, String tableName) {
        for (String sql : sqlStatements) {
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("  ✓ " + sql);
            } catch (SQLException e) {
                System.out.println("  ⚠ Skipped (column might already exist): " + e.getMessage());
            }
        }
    }
    
    private static boolean tableExists(Connection conn, String tableName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, tableName, new String[]{"TABLE"});
            return tables.next();
        } catch (SQLException e) {
            return false;
        }
    }
}