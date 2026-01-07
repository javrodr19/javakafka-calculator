package com.calculator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the result of a calculation sent via Kafka.
 */
public class CalculationResult {

    private final String requestId;
    private final double result;
    private final boolean success;
    private final String errorMessage;

    @JsonCreator
    public CalculationResult(
            @JsonProperty("requestId") String requestId,
            @JsonProperty("result") double result,
            @JsonProperty("success") boolean success,
            @JsonProperty("errorMessage") String errorMessage) {
        this.requestId = requestId;
        this.result = result;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    /**
     * Creates a successful result.
     */
    public static CalculationResult success(String requestId, double result) {
        return new CalculationResult(requestId, result, true, null);
    }

    /**
     * Creates a failed result with an error message.
     */
    public static CalculationResult error(String requestId, String errorMessage) {
        return new CalculationResult(requestId, 0, false, errorMessage);
    }

    public String getRequestId() {
        return requestId;
    }

    public double getResult() {
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        if (success) {
            return String.format("CalculationResult{id=%s, result=%s}", requestId, result);
        } else {
            return String.format("CalculationResult{id=%s, error=%s}", requestId, errorMessage);
        }
    }
}
