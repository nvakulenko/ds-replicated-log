package ua.edu.ucu.ds.config;

import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua.edu.ucu.LoggerGrpc;

@Configuration
public class SecondaryConfiguration {

    @Bean(name = "secondary1")
    public LoggerGrpc.LoggerBlockingStub secondary1() {
        ManagedChannelBuilder<?> channelBuilder1 =
                ManagedChannelBuilder.forAddress("secondary-1", 6567)
                        .usePlaintext();
//                ManagedChannelBuilder.forAddress("0.0.0.0", 6567)
//                        .usePlaintext();
        return LoggerGrpc.newBlockingStub(channelBuilder1.build());
    }

    @Bean(name = "secondary2")
    public LoggerGrpc.LoggerBlockingStub secondary2() {
        ManagedChannelBuilder<?> channelBuilder2 =
                ManagedChannelBuilder.forAddress("secondary-2", 6567)
                        .usePlaintext();
        return LoggerGrpc.newBlockingStub(channelBuilder2.build());
    }
}
