package com.calculator.client;

import com.calculator.config.KafkaConfig;
import com.calculator.model.CalculationRequest;
import com.calculator.model.CalculationRequest.Operation;
import com.calculator.model.CalculationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interactive calculator client that sends requests via Kafka and displays
 * results.
 */
public class CalculatorClient implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorClient.class);
    private static final Pattern CALC_PATTERN = Pattern.compile(
            "^\\s*(-?\\d+\\.?\\d*)\\s*([+\\-*/])\\s*(-?\\d+\\.?\\d*)\\s*$");

    private final KafkaProducer<String, String> producer;
    private final KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final Map<String, Boolean> pendingRequests;
    private final AtomicBoolean running;
    private final ExecutorService resultListener;

    public CalculatorClient() {
        this.producer = createProducer();
        this.consumer = createConsumer();
        this.objectMapper = new ObjectMapper();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.running = new AtomicBoolean(true);
        this.resultListener = Executors.newSingleThreadExecutor();
    }

    private KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAP_SERVERS);
        // Unique group ID for each client instance
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                KafkaConfig.CLIENT_GROUP_PREFIX + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        return new KafkaConsumer<>(props);
    }

    /**
     * Starts the interactive calculator client.
     */
    public void start() {
        // Start result listener thread
        consumer.subscribe(Collections.singletonList(KafkaConfig.RESULTS_TOPIC));
        resultListener.submit(this::listenForResults);

        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║        KAFKA CALCULATOR CLIENT             ║");
        System.out.println("╠════════════════════════════════════════════╣");
        System.out.println("║  Enter calculations like: 5 + 3            ║");
        System.out.println("║  Supported operations: + - * /             ║");
        System.out.println("║  Type 'exit' to quit                       ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while (running.get()) {
                System.out.print("calc> ");
                line = reader.readLine();

                if (line == null || line.trim().equalsIgnoreCase("exit")) {
                    System.out.println("Goodbye!");
                    break;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                processInput(line.trim());
            }
        } catch (Exception e) {
            logger.error("Error in client", e);
        }
    }

    private void processInput(String input) {
        Matcher matcher = CALC_PATTERN.matcher(input);

        if (!matcher.matches()) {
            System.out.println("Invalid input. Use format: number operator number");
            System.out.println("Example: 5 + 3");
            return;
        }

        try {
            double operand1 = Double.parseDouble(matcher.group(1));
            String operator = matcher.group(2);
            double operand2 = Double.parseDouble(matcher.group(3));

            Operation operation = switch (operator) {
                case "+" -> Operation.ADD;
                case "-" -> Operation.SUBTRACT;
                case "*" -> Operation.MULTIPLY;
                case "/" -> Operation.DIVIDE;
                default -> throw new IllegalArgumentException("Unknown operator: " + operator);
            };

            String requestId = UUID.randomUUID().toString();
            CalculationRequest request = new CalculationRequest(
                    requestId, operand1, operand2, operation);

            pendingRequests.put(requestId, true);

            String requestJson = objectMapper.writeValueAsString(request);
            producer.send(new ProducerRecord<>(
                    KafkaConfig.REQUESTS_TOPIC,
                    requestId,
                    requestJson));
            producer.flush();

            logger.debug("Sent request: {}", request);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void listenForResults() {
        while (running.get()) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    processResult(record);
                }
            } catch (Exception e) {
                if (running.get()) {
                    logger.error("Error listening for results", e);
                }
            }
        }
    }

    private void processResult(ConsumerRecord<String, String> record) {
        try {
            CalculationResult result = objectMapper.readValue(
                    record.value(), CalculationResult.class);

            // Only display results for our requests
            if (pendingRequests.remove(result.getRequestId()) != null) {
                if (result.isSuccess()) {
                    System.out.println("= " + formatResult(result.getResult()));
                } else {
                    System.out.println("Error: " + result.getErrorMessage());
                }
                System.out.print("calc> ");
            }
        } catch (Exception e) {
            logger.error("Error processing result", e);
        }
    }

    private String formatResult(double result) {
        if (result == Math.floor(result) && !Double.isInfinite(result)) {
            return String.valueOf((long) result);
        }
        return String.valueOf(result);
    }

    @Override
    public void close() {
        running.set(false);
        resultListener.shutdown();
        consumer.close();
        producer.close();
    }
}
