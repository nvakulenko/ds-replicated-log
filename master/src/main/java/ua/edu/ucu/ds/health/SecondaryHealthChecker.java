package ua.edu.ucu.ds.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.edu.ucu.HealthCheckRequest;
import ua.edu.ucu.HealthCheckResponse;
import ua.edu.ucu.HealthCheckStatus;
import ua.edu.ucu.LoggerGrpc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SecondaryHealthChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecondaryHealthChecker.class);
    public static final int CALLS_TO_BE_SUSPECTED = 1;
    public static final int CALLS_TO_BE_UNHEALTHY = 3;

    private Map<String, LoggerGrpc.LoggerBlockingStub> secondaries;
    // Collect pairs <secondary, number of failures>
    private ConcurrentHashMap<String, AtomicInteger> secondaryHealth;

    public SecondaryHealthChecker(LoggerGrpc.LoggerBlockingStub secondary1, LoggerGrpc.LoggerBlockingStub secondary2) {
        secondaries = new HashMap<>(2);
        secondaries.put("secondary-1", secondary1);
        secondaries.put("secondary-2", secondary2);

        secondaryHealth = new ConcurrentHashMap<>(2);
        secondaryHealth.put("secondary-1", new AtomicInteger(0));
        secondaryHealth.put("secondary-2", new AtomicInteger(0));
    }

    @Scheduled(fixedRate = 5000)
    public void checkSecondariesHealth() {
        // HEALTHY -> SUSPECTED if no response for 1 call -> UNHEALTHY if no response for 3 calls;
        LOGGER.info("Run Health Check on cluster...");
        secondaries.keySet()
                .stream()
                .forEach(secondary -> {
                    try {
                        HealthCheckResponse healthCheckResponse = secondaries.get(secondary).healthCheck(HealthCheckRequest.newBuilder().build());
                        if (HealthCheckStatus.UP.equals(healthCheckResponse.getStatus()))
                            secondaryHealth.get(secondary).set(0);
                        else {
                            secondaryHealth.get(secondary).getAndIncrement();
                            LOGGER.info("Secondary {} is down for {} attempts, marked as {}",
                                    secondary,
                                    secondaryHealth.get(secondary),
                                    getSecondaryStatus(secondary));
                        }
                    } catch (Throwable e) {
                        secondaryHealth.get(secondary).getAndIncrement();
                        LOGGER.info("Secondary {} is down for {} attempts, marked as {}",
                                secondary,
                                secondaryHealth.get(secondary),
                                getSecondaryStatus(secondary));
                    }
                });
    }

    public SecondaryHealthStatus getSecondaryStatus(String secondary) {
        Integer failureCallsToSecondary = secondaryHealth.get(secondary).get();
        if (failureCallsToSecondary == 0) return SecondaryHealthStatus.HEALTHY;
        if (failureCallsToSecondary >= CALLS_TO_BE_SUSPECTED && failureCallsToSecondary <= CALLS_TO_BE_UNHEALTHY)
            return SecondaryHealthStatus.SUSPECTED;
        else return SecondaryHealthStatus.UNHEALTHY;
    }

    public boolean isQuorumAvailable() {
        // if 2 secondaries are UNHEALTHY
        long unhealthySecondaries = secondaryHealth.values().stream()
                .filter(in -> in.get() > CALLS_TO_BE_UNHEALTHY)
                .count();
        return unhealthySecondaries == secondaries.size();
    }

}
