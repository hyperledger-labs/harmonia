package net.corda.samples.example.webserver.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmDCRRequest {
	private String tradeId;
	private String networkId;
	private String sourceNetworkId;
	private String encodedInfo;
	private String signatureOrProof;
}
