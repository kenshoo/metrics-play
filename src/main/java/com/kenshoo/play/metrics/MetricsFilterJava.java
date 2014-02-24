package com.kenshoo.play.metrics;

import java.lang.reflect.InvocationTargetException;

import play.Logger;

import com.codahale.metrics.MetricRegistry;
import com.kenshoo.play.metrics.MetricsFilter;
import com.kenshoo.play.metrics.MetricsRegistry;
import play.api.mvc.EssentialAction;


public class MetricsFilterJava extends MetricsFilter {

    @Override
    public MetricRegistry registry() {
        Object defaultRegistry;
        try {
            defaultRegistry = MetricsRegistry.class.getMethod("default").invoke(null);
            return (MetricRegistry) defaultRegistry;
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            Logger.error("Error getting default registry", e);
            e.printStackTrace();
            return new MetricRegistry();
        }
    }
    
}