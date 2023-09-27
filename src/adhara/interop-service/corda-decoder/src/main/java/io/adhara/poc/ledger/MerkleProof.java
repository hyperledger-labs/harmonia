package io.adhara.poc.ledger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MerkleProof {
	SecureHash[] proof;
	byte[] flags;
	SecureHash[] leaves;

	public void print() {
		System.out.println("Leaves");
		for (SecureHash p : leaves) {
			System.out.println(p.toString());
		}
		System.out.println("Proof");
		for (SecureHash p : proof) {
			System.out.println(p.toString());
		}
		System.out.println("Flags");
		for (byte b : flags) {
			System.out.println(b);
		}
	}

	public boolean equals(MerkleProof other) {
		if (proof.length != other.proof.length || flags.length != other.flags.length || leaves.length != other.leaves.length)
			return false;
		for (int p=0; p<proof.length; p++) {
			if (!proof[p].equals(other.proof[p]))
				return false;
		}
		for (int f=0; f<flags.length; f++) {
			if (flags[f] != other.flags[f])
				return false;
		}
		for (int l=0; l<leaves.length; l++) {
			if (!leaves[l].equals(other.leaves[l]))
				return false;
		}
		return true;
	}
}
