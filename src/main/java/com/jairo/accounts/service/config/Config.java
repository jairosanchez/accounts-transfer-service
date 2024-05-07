package com.jairo.accounts.service.config;

import jakarta.inject.Singleton;

@Singleton
public class Config {

    private final int monitoringDelayInMillis;

    private final int numberOfMonitoringThreads;

    public Config(int monitoringDelayInMillis, int numberOfMonitoringThreads) {
        this.monitoringDelayInMillis = monitoringDelayInMillis;
        this.numberOfMonitoringThreads = numberOfMonitoringThreads;
    }

    public int getMonitoringDelayInMillis() {
        return monitoringDelayInMillis;
    }

    public int getNumberOfMonitoringThreads() {
        return numberOfMonitoringThreads;
    }
}
