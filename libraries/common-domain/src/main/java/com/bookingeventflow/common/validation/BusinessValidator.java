package com.bookingeventflow.common.validation;

@FunctionalInterface
public interface BusinessValidator<T> {

    void validate(T target);

}