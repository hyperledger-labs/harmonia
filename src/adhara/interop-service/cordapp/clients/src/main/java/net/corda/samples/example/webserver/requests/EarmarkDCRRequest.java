package net.corda.samples.example.webserver.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EarmarkDCRRequest {
	private String linearId;
	private String partyName;
	private String tradeId;
}
