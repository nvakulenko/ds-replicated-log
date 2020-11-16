package ua.edu.ucu.ds.grpc;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ua.edu.ucu.*;
import ua.edu.ucu.ds.service.LogReplicatorService;

import java.util.ArrayList;
import java.util.List;

@GRpcService
public class GrpcMasterLoggerService extends LoggerGrpc.LoggerImplBase {

    @Autowired
    private LogReplicatorService replicatorService;

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMasterLoggerService.class);
    private final List<LogMessage> logs = new ArrayList<>();

    public GrpcMasterLoggerService(ManagedChannel channel) {
        this.logs.add(LogMessage.newBuilder().setLog("Logs from Master").build());
    }

    @Override
    public void appendMessage(AppendMessageRequest request, StreamObserver<AppendMessageResponse> responseObserver) {
        LogMessage log = request.getLog();

        LOGGER.info("Received LOG:" + log.getLog());
        logs.add(log);

        replicatorService.replicateLog(request);

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
