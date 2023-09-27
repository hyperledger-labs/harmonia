package io.adhara.poc.ledger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignableData {
	private SecureHash txId; 				  // Transaction's id or root of multi-transaction Merkle tree in case of multi-transaction signing
	private SignedMeta signatureMeta; // Meta data required
}
