package io.adhara.poc.ledger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SignedData {
	private static final Logger logger = LoggerFactory.getLogger(SignedData.class);

	private String by;
	private String bytes;
	private String partialTree;
	private Integer platformVersion;
	private Integer schemaNumber;

	public boolean hasPlatformVersion() {	return platformVersion != null;	}

	public boolean hasPartialTree() {	return partialTree != null && !partialTree.isEmpty();	}

	public PartialTree getPartialTreeRoot(SecureHash root) {
		if (hasPartialTree()) {
			List<SecureHash> txLeaves = Collections.singletonList(root.rehash());
			MerkleTree txTree = MerkleTree.getMerkleTree(txLeaves);
			PartialTree pTree = null;
			try {
				PartialMerkleTree parTree = PartialMerkleTree.build(txTree, txLeaves);
				pTree = parTree.getRoot();
				if (!getPartialTree().equals(pTree.getHash().toString())) {
					logger.error("The given hash is not an included leaf in the partial tree");
				}
			} catch (Exception e) {
				logger.error("Failed to build partial tree from given hash: " + e.getMessage());
			}
			return pTree;
		}
		return null;
	}
}
