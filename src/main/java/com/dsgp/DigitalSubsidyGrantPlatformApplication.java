package com.dsgp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Digital Subsidy and Grant Administration Platform.
 *
 * <p>This application is structured as a feature-based, multi-module Spring Boot
 * application aligned with the system architecture:
 *
 * <ul>
 *   <li>Beneficiary &amp; Scheme Master Data</li>
 *   <li>Eligibility Scoring &amp; Verification Workflow</li>
 *   <li>Staged Disbursement &amp; Compliance Tracking</li>
 *   <li>Fund Utilization &amp; Regional Analytics</li>
 *   <li>Security, Integration &amp; Deployment</li>
 * </ul>
 *
 * <p><strong>Current phase:</strong> Architecture &amp; Project Structure Setup.
 * Business modules will be implemented in subsequent phases.
 */
@SpringBootApplication
public class DigitalSubsidyGrantPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalSubsidyGrantPlatformApplication.class, args);
    }
}
