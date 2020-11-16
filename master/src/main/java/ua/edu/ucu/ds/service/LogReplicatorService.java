package ua.edu.ucu.ds.service;

import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.edu.ucu.*;
import ua.edu.ucu.ds.grpc.GrpcMasterLoggerService;

@Service
public class LogReplicatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMasterLoggerService.class);

    @Autowired
    private ManagedChannel channel;
    private LoggerGrpc.LoggerBlockingStub loggerBlockingStub;

    public LogReplicatorService(ManagedChannel channel) {
        this.channel = channel;
        this.loggerBlockingStub = LoggerGrpc.newBlockingStub(channel);
    }

    public void replicateLog(AppendMessageRequest request) {
        LOGGER.info("Replicate LOG to secondary:" + request.getLog());
        // Replica services
        loggerBlockingStub.appendMessage(request);
    }

    public ListMessagesResponse getMessages() {
       return loggerBlockingStub.listMessages(ListMessagesRequest.newBuilder().build());
    }
}
