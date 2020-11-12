package ua.edu.ucu.ds;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ua.edu.ucu.*;

import java.util.ArrayList;
import java.util.List;

@GRpcService
public class GrpcMasterLoggerService extends LoggerGrpc.LoggerImplBase {
    @Autowired
    private ManagedChannel channel;
    private LoggerGrpc.LoggerBlockingStub loggerBlockingStub;

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMasterLoggerService.class);
    private final List<LogMessage> logs = new ArrayList<>();

    public GrpcMasterLoggerService(ManagedChannel channel) {
        this.channel = channel;
        this.loggerBlockingStub = LoggerGrpc.newBlockingStub(channel);
        this.logs.add(LogMessage.newBuilder().setLog("Logs from Master").build());
    }

    @Override
    public void appendMessage(AppendMessageRequest request, StreamObserver<AppendMessageResponse> responseObserver) {
        LogMessage log = request.getLog();

        LOGGER.info("Received LOG:" + log.getLog());
        logs.add(log);

        LOGGER.info("Replicate LOG to secondary:" + log.getLog());
        // Replica services
        loggerBlockingStub.appendMessage(request);

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
