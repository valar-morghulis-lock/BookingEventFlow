package com.bookingeventflow.common.validation.constraints;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.temporal.Temporal;
import java.lang.reflect.Field;

public class ValidDateRangeValidator
        implements ConstraintValidator<ValidDateRange, Object> {

    private String startField;
    private String endField;

    @Override
    public void initialize(ValidDateRange annotation) {
        this.startField = annotation.start();
        this.endField = annotation.end();
    }

    @Override
    public boolean isValid(
            Object value,
            ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        try {
            Field start = value.getClass().getDeclaredField(startField);
            Field end = value.getClass().getDeclaredField(endField);

            start.setAccessible(true);
            end.setAccessible(true);

            Object startValue = start.get(value);
            Object endValue = end.get(value);

            if (startValue == null || endValue == null) {
                return true;
            }

            if (!(startValue instanceof Temporal)
                    || !(endValue instanceof Temporal)) {
                return false;
            }

            return ((Temporal) startValue)
                    .getClass()
                    .isInstance(endValue)
                    && compare(startValue, endValue) < 0;

        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compare(Object start, Object end) {
        return ((Comparable) start).compareTo(end);
    }
}