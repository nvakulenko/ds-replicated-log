package ua.edu.ucu.ds.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MasterConfiguration {

    @Bean
    public ManagedChannel channel() {
        ManagedChannelBuilder<?> channelBuilder =
                ManagedChannelBuilder.forAddress("secondary", 6567)
                        .usePlaintext();
        return channelBuilder.build();
    }
}
