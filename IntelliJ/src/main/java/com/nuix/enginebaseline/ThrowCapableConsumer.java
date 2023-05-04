package com.nuix.enginebaseline;

@FunctionalInterface
public interface ThrowCapableConsumer<T> {
    void accept(T obj) throws Exception;
}
