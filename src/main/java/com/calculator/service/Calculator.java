package com.calculator.service;

import com.calculator.model.CalculationRequest;
import com.calculator.model.CalculationResult;

/**
 * Core calculator logic for performing arithmetic operations.
 */
public class Calculator {

    /**
     * Performs the calculation based on the request.
     *
     * @param request the calculation request
     * @return the calculation result
     */
    public CalculationResult calculate(CalculationRequest request) {
        try {
            double result = switch (request.getOperation()) {
                case ADD -> request.getOperand1() + request.getOperand2();
                case SUBTRACT -> request.getOperand1() - request.getOperand2();
                case MULTIPLY -> request.getOperand1() * request.getOperand2();
                case DIVIDE -> divide(request.getOperand1(), request.getOperand2());
            };
            return CalculationResult.success(request.getRequestId(), result);
        } catch (ArithmeticException e) {
            return CalculationResult.error(request.getRequestId(), e.getMessage());
        }
    }

    private double divide(double operand1, double operand2) {
        if (operand2 == 0) {
            throw new ArithmeticException("Division by zero");
        }
        return operand1 / operand2;
    }
}
