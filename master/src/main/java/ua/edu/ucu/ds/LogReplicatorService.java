package ua.edu.ucu.ds;

import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.edu.ucu.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class LogReplicatorService {

    @Value("${secondary.retry.attempts}")
    private Integer retryAttempts;

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMasterLoggerService.class);
    private Map<String, LoggerGrpc.LoggerBlockingStub> secondaries;
    // <LOG, SECONDARY, REPLICATION>

    public LogReplicatorService() {
        ManagedChannelBuilder<?> channelBuilder1 =
                ManagedChannelBuilder.forAddress("secondary-1", 6567)
                        .usePlaintext();
//                ManagedChannelBuilder.forAddress("0.0.0.0", 6567)
//                        .usePlaintext();
        LoggerGrpc.LoggerBlockingStub secondary1 = LoggerGrpc.newBlockingStub(channelBuilder1.build());
        ManagedChannelBuilder<?> channelBuilder2 =
                ManagedChannelBuilder.forAddress("secondary-2", 6567)
                        .usePlaintext();
        LoggerGrpc.LoggerBlockingStub secondary2 = LoggerGrpc.newBlockingStub(channelBuilder2.build());

        secondaries = new HashMap<>(2);
        secondaries.put("secondary-1", secondary1);
        secondaries.put("secondary-2", secondary2);
    }

    public Integer getSecondariesCount() {
        return secondaries.size();
    }

    public boolean replicateLog(LogEntity log) throws IllegalArgumentException {
        try {
            int writeConcern = log.getWriteConcern();

            // check range is less then master + secondaries count
            if (writeConcern - 1 > secondaries.size()) {
                writeConcern = secondaries.size();
            }

            CountDownLatch countDownLatch = new CountDownLatch(writeConcern - 1);
            ExecutorService executor = Executors.newFixedThreadPool(secondaries.size());

            secondaries.entrySet().stream().map(
                    secondary -> {
                        return executor.submit(() -> {
                            try {
                                ReplicationStatus replicationStatus = replicateLog(log, secondary);
                                // save replication status
                            } catch (Throwable e) {
                                LOGGER.error(e.getLocalizedMessage(), e);
                            } finally {
                                countDownLatch.countDown();
                            }
                        });
                    }).collect(Collectors.toList());

            LOGGER.info("Wait for " + (writeConcern - 1) + " replicas");
            countDownLatch.await();
            LOGGER.info("Received response from " + (writeConcern - 1) + " replicas");
            return true;
        } catch (InterruptedException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            // by fact UNKNOWN
            return false;
        }
    }

    private ReplicationStatus replicateLog(LogEntity log, Map.Entry<String, LoggerGrpc.LoggerBlockingStub> secondary) {
        for (int i = 1; i < retryAttempts; i++) {
            try {
                LOGGER.info("Replication attempt #{} to: {}, LOG: {}", i, secondary.getKey(), log.getLog());
                AppendMessageResponse appendMessageResponse =
                        secondary.getValue().appendMessage(buildAppendMessageRequest(log));

                // TODO: check response errors
                if (AppendResponseCode.OK.equals(appendMessageResponse.getResponseCode())) {
                    LOGGER.info("Replicated log {} successfully to {}", log.toString(), secondary.getKey());
                    return ReplicationStatus.REPLICATED;
                } else {
                    // TODO
                    // handleErrors();
                    // connection errors - how do they look???
                    // need to test to get to know how they look
                    // logical errors
                    // 1 - retry
                }
            } catch (RuntimeException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }
        return ReplicationStatus.FAILED_REPLICATION;
    }

    private AppendMessageRequest buildAppendMessageRequest(LogEntity log) {
        return AppendMessageRequest.newBuilder()
                .setLog(LogMessage.newBuilder()
                        .setId(log.getId())
                        .setLog(log.getLog())
                        .setWriteConcern(log.getWriteConcern())
                        .build())
                .build();
    }
}
