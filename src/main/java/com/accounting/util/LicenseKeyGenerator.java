package com.accounting.util;

/**
 * Utility to generate license keys for customers.
 * Run this class to generate a license key for a company.
 * 
 * Usage: java LicenseKeyGenerator "COMPANY_NAME"
 */
public class LicenseKeyGenerator {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("=".repeat(60));
            System.out.println("LICENSE KEY GENERATOR");
            System.out.println("=".repeat(60));
            System.out.println();
            System.out.println("Usage: java LicenseKeyGenerator \"COMPANY_NAME\"");
            System.out.println();
            System.out.println("Example:");
            System.out.println("  java LicenseKeyGenerator \"ABC Company\"");
            System.out.println();
            System.out.println("Or run without arguments and enter company name:");
            System.out.print("\nEnter company name: ");
            
            try {
                java.util.Scanner scanner = new java.util.Scanner(System.in);
                String companyName = scanner.nextLine().trim();
                if (companyName.isEmpty()) {
                    System.out.println("Error: Company name cannot be empty");
                    return;
                }
                generateAndPrint(companyName);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        } else {
            String companyName = args[0].trim();
            generateAndPrint(companyName);
        }
    }

    private static void generateAndPrint(String companyName) {
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("Generating license key for: " + companyName);
        System.out.println("=".repeat(60));
        
        String licenseKey = LicenseManager.generateLicenseKey(companyName);
        
        if (licenseKey != null) {
            System.out.println();
            System.out.println("LICENSE KEY (copy everything below):");
            System.out.println("-".repeat(60));
            System.out.println(licenseKey);
            System.out.println("-".repeat(60));
            System.out.println();
            System.out.println("✓ License key generated successfully!");
            System.out.println("✓ This is a PERMANENT license (never expires)");
            System.out.println();
            System.out.println("Instructions:");
            System.out.println("1. Copy the entire license key above");
            System.out.println("2. Send it to the customer");
            System.out.println("3. Customer should paste it in: Settings > License > Activate");
            System.out.println();
        } else {
            System.err.println("ERROR: Failed to generate license key");
        }
    }
}
