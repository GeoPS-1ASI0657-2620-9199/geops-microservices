package com.geopslabs.geops.engagement.acceptance;

import io.cucumber.java.ParameterType;

import java.util.Arrays;
import java.util.List;

public class ParameterTypes {
    private static final String SEPARATOR = ",\\s*";

    @ParameterType("\\d+(?:,\\s*\\d+)*")
    public List<Long> longs(String values) {
        return Arrays.stream(values.split(SEPARATOR)).map(Long::valueOf).toList();
    }
}
