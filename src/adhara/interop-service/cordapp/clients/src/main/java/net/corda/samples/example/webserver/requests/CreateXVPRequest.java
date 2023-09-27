package net.corda.samples.example.webserver.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateXVPRequest {
	private String tradeId;
	private String assetId;
	private String from;
	private String to;
}
