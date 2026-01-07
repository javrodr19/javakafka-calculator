package com.calculator;

import com.calculator.client.CalculatorClient;
import com.calculator.service.CalculatorService;

/**
 * Main application entry point for the Kafka Calculator.
 * 
 * Usage:
 * java -jar kafka-calculator.jar --service # Starts the calculator service
 * java -jar kafka-calculator.jar --client # Starts the interactive client
 */
public class CalculatorApplication {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String mode = args[0].toLowerCase();

        switch (mode) {
            case "--service", "-s" -> runService();
            case "--client", "-c" -> runClient();
            case "--help", "-h" -> printUsage();
            default -> {
                System.err.println("Unknown option: " + mode);
                printUsage();
            }
        }
    }

    private static void runService() {
        System.out.println("Starting Calculator Service...");
        try (CalculatorService service = new CalculatorService()) {
            service.start();
        } catch (Exception e) {
            System.err.println("Error running service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runClient() {
        System.out.println("Starting Calculator Client...");
        try (CalculatorClient client = new CalculatorClient()) {
            client.start();
        } catch (Exception e) {
            System.err.println("Error running client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("Kafka Calculator Application");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar kafka-calculator.jar [option]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --service, -s    Start the calculator service (Kafka consumer)");
        System.out.println("  --client, -c     Start the interactive calculator client");
        System.out.println("  --help, -h       Show this help message");
    }
}
