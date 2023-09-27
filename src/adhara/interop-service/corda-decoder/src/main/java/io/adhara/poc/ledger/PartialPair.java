package io.adhara.poc.ledger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartialPair {
	private Boolean hasIncludedLeaf;
	private PartialTree tree;
}
