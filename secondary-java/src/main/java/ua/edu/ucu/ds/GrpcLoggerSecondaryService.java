package ua.edu.ucu.ds;

import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.edu.ucu.*;

import java.util.concurrent.ConcurrentHashMap;

@GRpcService
public class GrpcLoggerSecondaryService extends LoggerGrpc.LoggerImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcLoggerSecondaryService.class);
    private static final ConcurrentHashMap<Integer, LogMessage> logs = new ConcurrentHashMap<>();

    public GrpcLoggerSecondaryService() {
        this.logs.put(0, LogMessage.newBuilder().setLog("Zero log from Secondary").build());
    }

    @Override
    public void appendMessage(AppendMessageRequest request, StreamObserver<AppendMessageResponse> responseObserver) {
        LogMessage log = request.getLog();

        LOGGER.info("Received LOG: "+ log.toString());
        if (logs.containsKey(log.getId())) {
            responseObserver.onNext(AppendMessageResponse.newBuilder()
                    .setResponseCode(AppendResponseCode.ERROR_LOG_WITH_ID_ALREADY_EXISTS)
                    .setResponseMessage(
                            "Idempotent operation: log with id " + log.getId() + " already exists")
                    .build());
            responseObserver.onCompleted();
        }

        logs.putIfAbsent(log.getId(), log);

        try {
            LOGGER.info("Sleep for 5 seconds: " + log.getLog());
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }

        responseObserver.onNext(AppendMessageResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void listMessages(ListMessagesRequest request, StreamObserver<ListMessagesResponse> responseObserver) {
        LOGGER.info("Return LOGS from Secondary");

        ListMessagesResponse listMessagesResponse = ListMessagesResponse.newBuilder().addAllLogs(logs.values()).build();
        responseObserver.onNext(listMessagesResponse);
        responseObserver.onCompleted();
    }
}
