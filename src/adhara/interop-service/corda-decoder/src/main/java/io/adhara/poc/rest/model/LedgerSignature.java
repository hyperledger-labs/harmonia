package io.adhara.poc.rest.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerSignature {
  private String networkId;           // Source ledger identification.
  private String contractAddress;        // Destination xvp contract address.
  private String functionName;           // Destination function in xvp contract to invoke.
  private String senderId;               // Sender identification given as remote account id, e.g. X500 name, or local account id, e.g. BIC.
  private String receiverId;             // Receiver identification given as remote account id, e.g. X500 name, or local account id, e.g. BIC.
  private String encodedId;              // Hex-encoded id of the Corda trade transaction on the Corda network.
  private String encodedKey;             // Hex-encoded public key of the notary that will be finalising the Corda securities trade transaction on the Corda network.
  private String encodedSignature;       // Hex-encoded notary signature of the finalised Corda securities trade transaction on the Corda network.
  private String partialMerkleRoot;      // Corda serialization of a partial Merkle tree as required when multi-transaction signing is utilised.
  private String platformVersion;        // Integer representing the Corda platform version used to create the encoded signature.
  private String schemaNumber;           // Integer representing the Corda signature scheme used to create the encoded signature.
  private String withHiddenAuthParams;   // Flag to indicate that authentication parameters should be added to function call data.
  private String authNetworkId;       // Auth ledger identification. Must be a ledger authorized to call performCallFromRemoteChain.
  private String authContractAddress;    // Auth contract address, or function call contract address. e.g. 0x12345...
}
