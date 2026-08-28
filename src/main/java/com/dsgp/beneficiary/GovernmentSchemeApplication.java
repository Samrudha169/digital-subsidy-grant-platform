package com.dsgp.beneficiary;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Scanner;
import java.util.regex.Pattern;

@SpringBootApplication
public class GovernmentSchemeApplication implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public GovernmentSchemeApplication(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(GovernmentSchemeApplication.class, args);
    }

    @Override
    public void run(String... args) {

        Scanner sc = new Scanner(System.in);

        System.out.println("==========================================");
        System.out.println(" Government Scheme Application");
        System.out.println("==========================================");
        System.out.println("Database connected successfully!\n");

        // Full Name
        String fullName;

        while (true) {
            System.out.print("Enter Full Name: ");
            fullName = sc.nextLine().trim();

            if (fullName.matches("[A-Za-z ]+")) {
                break;
            }

            System.out.println(
                    "Invalid input! Name should contain only letters and spaces. Try again.\n"
            );
        }

        // Government ID
        String govId;

        while (true) {
            System.out.print(
                    "Enter Government ID (12-digit Aadhaar or 10-char PAN): "
            );

            govId = sc.nextLine().trim();

            boolean validAadhaar = govId.matches("\\d{12}");
            boolean validPan = govId.matches("[A-Za-z0-9]{10}");

            if (validAadhaar || validPan) {
                break;
            }

            System.out.println(
                    "Invalid ID! Enter a 12-digit Aadhaar number or a 10-character PAN. Try again.\n"
            );
        }

        // Contact Number
        String contact;

        while (true) {
            System.out.print("Enter Contact Number (10 digits): ");
            contact = sc.nextLine().trim();

            if (contact.matches("\\d{10}")) {
                break;
            }

            System.out.println(
                    "Invalid number! Enter exactly 10 digits. Try again.\n"
            );
        }

        // Email
        String email;

        String emailRegex =
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        while (true) {
            System.out.print("Enter Email: ");
            email = sc.nextLine().trim();

            if (Pattern.matches(emailRegex, email)) {
                break;
            }

            System.out.println(
                    "Invalid email format! Example: name@example.com. Try again.\n"
            );
        }

        // Age
        int age = 0;

        while (true) {
            System.out.print("Enter Age: ");
            String ageInput = sc.nextLine().trim();

            if (ageInput.matches("\\d+")) {

                age = Integer.parseInt(ageInput);

                if (age > 0 && age <= 120) {
                    break;
                }

                System.out.println(
                        "Invalid age! Enter a realistic age (1-120). Try again.\n"
                );

            } else {
                System.out.println(
                        "Invalid input! Age must contain numbers only. Try again.\n"
                );
            }
        }

        // Address
        String address;

        while (true) {
            System.out.print("Enter Address: ");
            address = sc.nextLine().trim();

            if (!address.isEmpty()) {
                break;
            }

            System.out.println(
                    "Address cannot be empty. Try again.\n"
            );
        }

        // Scheme Name
        String schemeName;

        while (true) {
            System.out.print("Enter Scheme Name: ");
            schemeName = sc.nextLine().trim();

            if (!schemeName.isEmpty()
                    && schemeName.matches("[A-Za-z0-9 .-]+")) {
                break;
            }

            System.out.println(
                    "Invalid scheme name! Use only letters, numbers and spaces. Try again.\n"
            );
        }

        // Save to Database
        String sql = """
               INSERT INTO beneficiary
               (full_name, gov_id, contact, email, age, address, scheme_name)
               VALUES (?, ?, ?, ?, ?, ?, ?)
               """;

        try {

            jdbcTemplate.update(
                    sql,
                    fullName,
                    govId,
                    contact,
                    email,
                    age,
                    address,
                    schemeName
            );

            System.out.println(
                    "\nData saved to database successfully!"
            );

        } catch (Exception e) {

            System.out.println(
                    "\nFailed to save data to database!"
            );

            e.printStackTrace();

            sc.close();
            return;
        }

        // Confirmation
        System.out.println(
                "\nApplication submitted successfully...\n"
        );

        // Display details
        System.out.println(
                "---------- Application Details ----------"
        );

        System.out.println("Full Name        : " + fullName);
        System.out.println("Government ID    : " + govId);
        System.out.println("Contact Number   : " + contact);
        System.out.println("Email            : " + email);
        System.out.println("Age              : " + age);
        System.out.println("Address          : " + address);
        System.out.println("Scheme Name      : " + schemeName);

        System.out.println(
                "-------------------------------------------"
        );

        sc.close();
    }
}
