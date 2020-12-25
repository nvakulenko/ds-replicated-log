package ua.edu.ucu.ds;

import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ua.edu.ucu.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static ua.edu.ucu.AppendResponseCode.*;

@Service
public class LogReplicatorService {

    @Value("${secondary.retry.attempts}")
    private Integer retryAttempts;

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMasterLoggerService.class);
    private Map<String, LoggerGrpc.LoggerBlockingStub> secondaries;
    private ConcurrentHashMap<String, List<FailureInformation>> failureStatistics;

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

        failureStatistics = new ConcurrentHashMap<>();
        failureStatistics.put("secondary-1", Collections.synchronizedList(new ArrayList<FailureInformation>()));
        failureStatistics.put("secondary-2", Collections.synchronizedList(new ArrayList<FailureInformation>()));
    }

    public Integer getSecondariesCount() {
        return secondaries.size();
    }

    public ConcurrentHashMap<String, List<FailureInformation>> getFailureStatistics() {
        return failureStatistics;
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

            List<Future<ReplicationStatus>> futures = secondaries.entrySet().stream().map(
                    secondary -> {
                        return executor.submit(() -> {
                            try {
                                return replicateLog(log, secondary);
                                // save replication status
                            } catch (Throwable e) {
                                LOGGER.error(e.getLocalizedMessage(), e);
                                return ReplicationStatus.FAILED_REPLICATION;
                            } finally {
                                countDownLatch.countDown();
                            }
                        });
                    }).collect(Collectors.toList());

            LOGGER.info("Wait for " + (writeConcern - 1) + " replicas");
            countDownLatch.await();
            LOGGER.info("Received response from " + (writeConcern - 1) + " replicas");
            long failureResponses = futures.stream()
                    .filter(in -> {
                        try {
                            return in.isDone() && ReplicationStatus.FAILED_REPLICATION.equals(in.get());
                        } catch (InterruptedException e) {
                            LOGGER.error(e.getLocalizedMessage(), e);
                            return false;
                        } catch (ExecutionException e) {
                            LOGGER.error(e.getLocalizedMessage(), e);
                            return false;
                        }
                    }).count();
            return failureResponses == 0;
        } catch (InterruptedException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
            // by fact UNKNOWN
            return false;
        }
    }

    private ReplicationStatus replicateLog(LogEntity log, Map.Entry<String, LoggerGrpc.LoggerBlockingStub> secondary) {
        int i = 0;
        while (true) {
            try {
                i++;

                if (i > 2) {
                    Thread.sleep(5000);
                }

                LOGGER.info("Replication attempt #{} to: {}, LOG: {}", i + 1, secondary.getKey(), log.getLog());
                AppendMessageResponse appendMessageResponse =
                        secondary.getValue().appendMessage(buildAppendMessageRequest(log));

                LOGGER.info("Received from secondary {} response code {} for log {}",
                        secondary.getKey(),
                        appendMessageResponse.getResponseCode(),
                        log.getId());

                if (OK.equals(appendMessageResponse.getResponseCode()) ||
                        ERROR_LOG_WITH_ID_ALREADY_EXISTS.equals(appendMessageResponse.getResponseCode())) {
                    LOGGER.info("Replicated log {} successfully to {}", log.getId(), secondary.getKey());
                    return ReplicationStatus.REPLICATED;
                } else {
                    // TODO handleErrors();
                    // connection errors - how do they look???
                    // need to test to get to know how they look
                    // logical errors
                    failureStatistics.get(secondary.getKey())
                            .add(FailureInformation.builder()
                                    .logId(log.getId())
                                    .replicationStatus(ReplicationStatus.FAILED_REPLICATION)
                                    .appendResponseCode(appendMessageResponse.getResponseCode())
                                    .attempt(i + 1)
                                    .build());
                }
            } catch (Throwable e) {
                LOGGER.error(e.getLocalizedMessage(), e);
                failureStatistics.get(secondary.getKey())
                        .add(FailureInformation.builder()
                                .logId(log.getId())
                                .replicationStatus(ReplicationStatus.FAILED_REPLICATION)
                                .failureReason(e.getLocalizedMessage())
                                .attempt(i + 1)
                                .build());
            }
        }
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
