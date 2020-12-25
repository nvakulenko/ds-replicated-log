package ua.edu.ucu.ds;

import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.edu.ucu.*;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

@GRpcService
public class GrpcLoggerSecondaryService extends LoggerGrpc.LoggerImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcLoggerSecondaryService.class);
    private static final ConcurrentSkipListMap<Integer, LogMessage> logs = new ConcurrentSkipListMap<>();
    private static final AtomicInteger secondaryOrder = new AtomicInteger(0);

    public GrpcLoggerSecondaryService() {
        this.logs.put(0, LogMessage.newBuilder().setLog("Zero log from Secondary").build());
    }

    @Override
    public void appendMessage(AppendMessageRequest request, StreamObserver<AppendMessageResponse> responseObserver) {
        LogMessage log = request.getLog();
        LOGGER.info("Received LOG: "+ log.toString());

        // deduplication: each log should be present on secondary exactly ones
        if (logs.containsKey(log.getId())) {
            LOGGER.error("LOG with id "+ log.getId() + " is already stored on secondary");
            responseObserver.onNext(AppendMessageResponse.newBuilder()
                    .setResponseCode(AppendResponseCode.ERROR_LOG_WITH_ID_ALREADY_EXISTS)
                    .setResponseMessage(
                            "Idempotent operation: log with id " + log.getId() + " already exists")
                    .build());
            responseObserver.onCompleted();
            return;
        }

        // deduplication: each log should be present on secondary exactly ones
        logs.putIfAbsent(log.getId(), log);

        // generate random int up to 5 seconds
        Random random = new Random();
        int randomSleep = random.nextInt(5 - 1) + 1;

        try {
            LOGGER.info("Sleep randomly generated " + randomSleep + " seconds: " + log.getLog());
            Thread.sleep(randomSleep);
        } catch (InterruptedException e) {
            LOGGER.error(e.getLocalizedMessage(), e);
        }

        if (randomSleep % 2 == 0) {
            // randomly generated internal server error
            responseObserver.onError(new InternalError("Randomly generated error"));
        } else {
            responseObserver.onNext(AppendMessageResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void listMessages(ListMessagesRequest request, StreamObserver<ListMessagesResponse> responseObserver) {
        LOGGER.info("Return LOGS from Secondary");

        // TODO: total ordering:
        // If secondary has received messages [msg1, msg2, msg4],
        // it shouldn’t display the message ‘msg4’ until the ‘msg3’ will be received

        // filter logs
        List<LogMessage> totalOrderResult = new ArrayList<>();
        int totalOrder = 0;

        while (logs.containsKey(totalOrder)) {
            totalOrderResult.add(logs.get(totalOrder));
            totalOrder++;
        }

        ListMessagesResponse listMessagesResponse =
                ListMessagesResponse.newBuilder().addAllLogs(totalOrderResult).build();
        responseObserver.onNext(listMessagesResponse);
        responseObserver.onCompleted();
    }
}
