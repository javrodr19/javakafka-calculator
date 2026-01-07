package com.calculator.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a calculation request sent via Kafka.
 */
public class CalculationRequest {
    
    public enum Operation {
        ADD, SUBTRACT, MULTIPLY, DIVIDE
    }
    
    private final String requestId;
    private final double operand1;
    private final double operand2;
    private final Operation operation;
    
    @JsonCreator
    public CalculationRequest(
            @JsonProperty("requestId") String requestId,
            @JsonProperty("operand1") double operand1,
            @JsonProperty("operand2") double operand2,
            @JsonProperty("operation") Operation operation) {
        this.requestId = requestId;
        this.operand1 = operand1;
        this.operand2 = operand2;
        this.operation = operation;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public double getOperand1() {
        return operand1;
    }
    
    public double getOperand2() {
        return operand2;
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    @Override
    public String toString() {
        return String.format("CalculationRequest{id=%s, %s %s %s}", 
            requestId, operand1, operation, operand2);
    }
}
