package net.corda.samples.example.webserver.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelDCRRequest {
	private String tradeId;
	private String encodedInfo;
	private String signatureOrProof;
}
