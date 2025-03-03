package org.niit_project.backend.config;

import io.getstream.services.framework.StreamSDKClient;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StreamClientConfig {

    @Bean
    public StreamSDKClient client(){
        var env = Dotenv.load();

        return new StreamSDKClient(env.get("STREAM_API_KEY"), env.get("STREAM_API_SECRET"));
    }
}
