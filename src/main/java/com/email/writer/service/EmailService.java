package com.email.writer.service;

import com.email.writer.domain.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class EmailService {
    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String apiUrl;
    @Value("${gemini.api.key}")
    private String apiKey;

    public EmailService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest){
        String prompt=createPrompt(emailRequest);
        Map<String,Object> requestBody=Map.of(
                "contents",new Object[]{
                        Map.of("parts",new Object[]{
                            Map.of("text",prompt)
                })
                 }
        );
        String url=apiUrl+"?key="+apiKey;
//        String url=  UriComponentsBuilder.fromPath(apiUrl).queryParam("key",apiKey).toUriString();
        String response=webClient.post()
                .uri(url).
                header("Content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve().bodyToMono(String.class).block();
        response=extractResponse(response);
        return response;
    }

    private String extractResponse(String response) {
        try{
            ObjectMapper mapper=new ObjectMapper();
            JsonNode rootNode=mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content").path("parts")
                    .get(0).path("text").asText();
        }catch (Exception e){
            throw  new RuntimeException("Error processing request : ,{e.getMessage()}");
        }

    }

    private String createPrompt(EmailRequest emailRequest) {
        StringBuilder prompt=new StringBuilder();
        prompt.append("Generate a professional email reply for the following email content. Please don't generate a subject line ");
        if(emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty()){
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone. ");
        }
        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
