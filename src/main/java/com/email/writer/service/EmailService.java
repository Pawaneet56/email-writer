package com.email.writer.service;

import com.email.writer.domain.EmailRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.html.HtmlParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
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
        Map<String,Object> requestBody=createRequestBody(prompt);
        String url=apiUrl+"?key="+apiKey;
        String response=webClient.post()
                .uri(url).
                header("Content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve().bodyToMono(String.class).block();
        response=extractResponse(response);
        return response;
    }
    private Map<String,Object> createRequestBody(String prompt){
        return Map.of(
                "contents",new Object[]{
                        Map.of("parts",new Object[]{
                                Map.of("text",prompt)
                        })
                }
        );
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

    public String generateSummary(InputStream inputStream, String fileType) throws TikaException, IOException, SAXException {
        String text=extractText(inputStream,fileType);
        String prompt="Generate summary for the following text in less than 200 words\n";
        prompt=prompt.concat(text);
        Map<String,Object> requestBody=createRequestBody(prompt);
        String url=apiUrl+"?key="+apiKey;
        String response=webClient.post()
                .uri(url).
                header("Content-Type","application/json")
                .bodyValue(requestBody)
                .retrieve().bodyToMono(String.class).block();
        response=extractResponse(response);
        return response;
    }
    public String extractText(InputStream inputStream, String fileType) throws IOException, TikaException, SAXException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        switch (fileType) {
            case "text/plain":  // TXT files
            case "application/pdf":  // PDF files
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":  // DOCX files
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation":  // PPTX files
                parser.parse(inputStream, handler, metadata, context);
                return handler.toString();
            case "text/html":  // HTML files
                HtmlParser htmlParser = new HtmlParser();
                htmlParser.parse(inputStream, handler, metadata, context);
                return handler.toString();
            default:
                return "Unsupported file type: " + fileType;
        }
    }
}
