package io.adhara.poc.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication; 

@SpringBootApplication 
public class DecoderService {
    private static final Logger logger = LoggerFactory.getLogger(DecoderService.class);

    public static void main(String[] args) {
        SpringApplication.run(DecoderService.class, args);
    }
}
