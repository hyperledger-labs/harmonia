package io.adhara.poc.rest.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LedgerTransaction {
  private String networkId;              // Source ledger identification.
  private String contractAddress;        // Control contract address, or xvp contract address. e.g. 0x12345...
  private String functionName;           // Destination function in control contract, or xvp function.
  private String encodedInfo;            // Hex-encoded AMPQ/1.0-encoded signed Corda transaction, e.g. 636F7264610100000080C562000000000001D00...
  private String withHiddenAuthParams;   // Flag to indicate that authentication parameters should be added to function call data.
  private String authNetworkId;          // Auth ledger identification. Must be a ledger authorized to call performCallFromRemoteChain.
  private String authContractAddress;    // Auth contract address, or function call contract address. e.g. 0x12345...
}
