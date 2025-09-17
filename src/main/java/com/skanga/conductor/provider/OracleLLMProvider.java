package com.skanga.conductor.provider;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.skanga.conductor.retry.RetryPolicy;
import dev.langchain4j.community.model.oracle.oci.genai.OciGenAiChatModel;

import java.io.IOException;

public class OracleLLMProvider extends AbstractLLMProvider {

    private final OciGenAiChatModel model;


    public OracleLLMProvider(String compartmentId, String modelName) throws IOException {
        super("oracle");
        // Create an authentication provider
        // Uses the default configuration profile (~/.oci/config)
        AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(ConfigFileReader.parseDefault());
        this.model = OciGenAiChatModel.builder()
                .compartmentId(compartmentId)
                .modelName(modelName)
                .authProvider(provider)
                .build();
    }

    public OracleLLMProvider(String compartmentId, String modelName, RetryPolicy retryPolicy) throws IOException {
        super("oracle", retryPolicy);
        // Create an authentication provider
        // Uses the default configuration profile (~/.oci/config)
        AuthenticationDetailsProvider provider = new ConfigFileAuthenticationDetailsProvider(ConfigFileReader.parseDefault());
        this.model = OciGenAiChatModel.builder()
                .compartmentId(compartmentId)
                .modelName(modelName)
                .authProvider(provider)
                .build();
    }

    @Override
    protected String generateInternal(String prompt) throws Exception {
        return model.chat(prompt);
    }
}