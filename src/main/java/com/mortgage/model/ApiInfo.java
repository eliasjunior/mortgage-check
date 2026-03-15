package com.mortgage.model;

import java.util.List;

public record ApiInfo(
        String application,
        String version,
        List<String> endpoints,
        String health
) {}
