import java.util.Scanner;
public class GovernmentSchemeApplication {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        //name
        System.out.print("Enter Full Name: ");
        String fullName = sc.nextLine();

        //id
        System.out.print("Enter Government ID (Aadhaar/PAN): ");
        String govId = sc.nextLine();

        //Contact Number
        System.out.print("Enter Contact Number : ");
        String contact = sc.nextLine();

        //email
        System.out.print("Enter Email: ");
        String email = sc.nextLine();

        // Age
        System.out.print("Enter Age : ");
        String ageOrDob = sc.nextLine();

        // Address
        System.out.print("Enter Address: ");
        String address = sc.nextLine();

        System.out.print("Enter Scheme Name: ");
        String schemeName = sc.nextLine();

        // Confirmation
        System.out.println("\nApplication submitted successfully...\n");

        // Display   details
        System.out.println("---------- Application Details ----------");
        System.out.println("Full Name        : " + fullName);
        System.out.println("Government ID    : " + govId);
        System.out.println("Contact Info     : " + contact);
        System.out.println("Email            : " + email);
        System.out.println("Age              : " + ageOrDob);
        System.out.println("Address          : " + address);
        System.out.println("Scheme Name      : " + schemeName);
        System.out.println("------------------------------------------");

        sc.close();
    }

}
