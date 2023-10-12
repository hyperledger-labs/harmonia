package io.adhara.poc.ledger;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;

@Data
@AllArgsConstructor
public class MerkleProof {
	private final SecureHash[] proof;
	private final byte[] flags;
	private final SecureHash[] leaves;

	public boolean equals(MerkleProof other) {
		return Arrays.equals(proof, other.proof)
			&& Arrays.equals(flags, other.flags)
			&& Arrays.equals(leaves, other.leaves);
	}
}
