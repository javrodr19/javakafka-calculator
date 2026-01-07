package com.calculator.service;

import com.calculator.config.KafkaConfig;
import com.calculator.model.CalculationRequest;
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

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Kafka consumer service that processes calculation requests and publishes
 * results.
 */
public class CalculatorService implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(CalculatorService.class);

    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> producer;
    private final Calculator calculator;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running;

    public CalculatorService() {
        this.consumer = createConsumer();
        this.producer = createProducer();
        this.calculator = new Calculator();
        this.objectMapper = new ObjectMapper();
        this.running = new AtomicBoolean(true);
    }

    private KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaConfig.SERVICE_GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<>(props);
    }

    private KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    /**
     * Starts the service and begins processing calculation requests.
     */
    public void start() {
        consumer.subscribe(Collections.singletonList(KafkaConfig.REQUESTS_TOPIC));
        logger.info("Calculator Service started. Waiting for requests on topic: {}",
                KafkaConfig.REQUESTS_TOPIC);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Calculator Service...");
            running.set(false);
        }));

        while (running.get()) {
            try {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    processRecord(record);
                }
            } catch (Exception e) {
                logger.error("Error processing records", e);
            }
        }
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        try {
            logger.info("Received request: {}", record.value());

            CalculationRequest request = objectMapper.readValue(
                    record.value(), CalculationRequest.class);

            CalculationResult result = calculator.calculate(request);

            String resultJson = objectMapper.writeValueAsString(result);
            producer.send(new ProducerRecord<>(
                    KafkaConfig.RESULTS_TOPIC,
                    request.getRequestId(),
                    resultJson));
            producer.flush();

            logger.info("Sent result: {}", result);

        } catch (Exception e) {
            logger.error("Error processing record: {}", record.value(), e);
        }
    }

    @Override
    public void close() {
        running.set(false);
        consumer.close();
        producer.close();
        logger.info("Calculator Service closed.");
    }
}
