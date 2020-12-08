package ua.edu.ucu.ds;

import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ua.edu.ucu.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@GRpcService
public class GrpcMasterLoggerService extends LoggerGrpc.LoggerImplBase {

    @Autowired
    private LogReplicatorService replicatorService;

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMasterLoggerService.class);
    private final List<LogEntity> logEntities = new ArrayList<>();
    private static final AtomicInteger counter = new AtomicInteger();

    public GrpcMasterLoggerService() {
        this.logEntities.add(LogEntity.builder()
                .id(0)
                .log("Logs from Master")
                .writeConcern(0)
                .build());
    }

    @Override
    public void appendMessage(AppendMessageRequest request, StreamObserver<AppendMessageResponse> responseObserver) {
        LogMessage log = request.getLog();
        LOGGER.info("Received LOG: " + log.getLog());

        Integer secondariesCount = replicatorService.getSecondariesCount();
        if (log.getWriteConcern() > secondariesCount + 1) {
            LOGGER.error("Write concern is more then secondaries count. Received: " +
                    log.getWriteConcern() + "; available secondaries:  " + secondariesCount);
            responseObserver.onNext(
                    AppendMessageResponse.newBuilder()
                            .setResponseCode(AppendResponseCode.ERROR_WRITECONCERN)
                            .setResponseMessage("Write concern is more then secondaries count")
                            .build());
            responseObserver.onCompleted();
            return;
        }

        // generate unique id and set
        LogEntity copyLog = LogEntity.builder()
                .id(counter.incrementAndGet())
                .log(log.getLog())
                .writeConcern(log.getWriteConcern())
                .build();

        logEntities.add(copyLog);
        boolean replicationResult = replicatorService.replicateLog(copyLog);

        responseObserver.onNext(
                AppendMessageResponse.newBuilder()
                        .setResponseCode(AppendResponseCode.OK)
                        .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listMessages(ListMessagesRequest request, StreamObserver<ListMessagesResponse> responseObserver) {
        ListMessagesResponse listMessagesResponse = ListMessagesResponse.newBuilder()
                .addAllLogs(logEntities.stream()
                        .map(in -> LogMessage.newBuilder()
                                .setId(in.getId())
                                .setLog(in.getLog())
                                .setWriteConcern(in.getWriteConcern())
                                .build())
                        .collect(Collectors.toList()))
                .build();
        responseObserver.onNext(listMessagesResponse);
        responseObserver.onCompleted();
    }
}
