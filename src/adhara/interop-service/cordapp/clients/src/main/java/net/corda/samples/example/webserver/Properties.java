package net.corda.samples.example.webserver;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class Properties {
	private String config;

	public String getConfig() {
		return config;
	}
	public void setConfig(String config) {
		this.config = config;
	}
}
