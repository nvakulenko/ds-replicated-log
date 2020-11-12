package ua.edu.ucu.ds;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MasterConfiguration {

    @Bean
    public ManagedChannel channel() {
        ManagedChannelBuilder<?> channelBuilder =
                ManagedChannelBuilder.forAddress("0.0.0.0", 6567)
                        .usePlaintext();
        return channelBuilder.build();
    }
}
