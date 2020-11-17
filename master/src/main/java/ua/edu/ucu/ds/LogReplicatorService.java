package ua.edu.ucu.ds;

import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ua.edu.ucu.*;

@Service
public class LogReplicatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMasterLoggerService.class);
    private LoggerGrpc.LoggerBlockingStub secondary1;
    private LoggerGrpc.LoggerBlockingStub secondary2;

    public LogReplicatorService() {
        ManagedChannelBuilder<?> channelBuilder1 =
                ManagedChannelBuilder.forAddress("secondary-1", 6567)
                        .usePlaintext();
        this.secondary1 = LoggerGrpc.newBlockingStub(channelBuilder1.build());

        ManagedChannelBuilder<?> channelBuilder2 =
                ManagedChannelBuilder.forAddress("secondary-2", 6567)
                        .usePlaintext();
        this.secondary2 = LoggerGrpc.newBlockingStub(channelBuilder2.build());
    }

    public void replicateLog(AppendMessageRequest request) {
        LOGGER.info("Replicate LOG to secondary-1: " + request.getLog());
        secondary1.appendMessage(request);

        LOGGER.info("Replicate LOG to secondary-2: " + request.getLog());
        secondary2.appendMessage(request);
    }
}
