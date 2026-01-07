package com.calculator.config;

/**
 * Kafka configuration constants.
 */
public final class KafkaConfig {

    private KafkaConfig() {
        // Utility class
    }

    /**
     * Kafka bootstrap servers address.
     */
    public static final String BOOTSTRAP_SERVERS = "localhost:9092";

    /**
     * Topic for calculation requests.
     */
    public static final String REQUESTS_TOPIC = "calculator-requests";

    /**
     * Topic for calculation results.
     */
    public static final String RESULTS_TOPIC = "calculator-results";

    /**
     * Consumer group ID for the calculator service.
     */
    public static final String SERVICE_GROUP_ID = "calculator-service-group";

    /**
     * Consumer group ID prefix for clients (each client gets unique group).
     */
    public static final String CLIENT_GROUP_PREFIX = "calculator-client-";
}
