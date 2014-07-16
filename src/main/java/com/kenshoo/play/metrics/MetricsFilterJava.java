package com.kenshoo.play.metrics;

import com.codahale.metrics.MetricRegistry;
import play.Logger;

import java.lang.reflect.InvocationTargetException;


public class MetricsFilterJava extends MetricsFilter {

    @Override
    public MetricRegistry registry() {
        Object defaultRegistry;
        try {
            //this is due to 'default' is a reserved word in Java
            defaultRegistry = MetricsRegistry.class.getMethod("default").invoke(null);
            return (MetricRegistry) defaultRegistry;
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            Logger.error("Error getting default registry", e);
            return new MetricRegistry();
        }
    }
}