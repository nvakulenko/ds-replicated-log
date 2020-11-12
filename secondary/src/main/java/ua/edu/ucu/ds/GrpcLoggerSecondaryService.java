package ua.edu.ucu.ds;

import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.edu.ucu.*;

import java.util.ArrayList;
import java.util.List;

@GRpcService
public class GrpcLoggerSecondaryService extends LoggerGrpc.LoggerImplBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcLoggerSecondaryService.class);
    private final List<LogMessage> logs = new ArrayList<>();

    public GrpcLoggerSecondaryService() {
        this.logs.add(LogMessage.newBuilder().setLog("Logs from Secondary").build());
    }

    @Override
    public void appendMessage(AppendMessageRequest request, StreamObserver<AppendMessageResponse> responseObserver) {
        LogMessage log = request.getLog();

        LOGGER.info("Received LOG:" + log.getLog());
        logs.add(log);

        responseObserver.onNext(AppendMessageResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void listMessages(ListMessagesRequest request, StreamObserver<ListMessagesResponse> responseObserver) {
        ListMessagesResponse listMessagesResponse = ListMessagesResponse.newBuilder().addAllLogs(logs).build();

        responseObserver.onNext(listMessagesResponse);
        responseObserver.onCompleted();
    }
}
