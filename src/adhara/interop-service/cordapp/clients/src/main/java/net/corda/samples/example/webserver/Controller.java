package net.corda.samples.example.webserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.*;
import net.corda.core.crypto.PartialMerkleTree;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;
import net.corda.core.serialization.SerializationFactory;
import net.corda.core.serialization.SerializedBytes;
import net.corda.core.transactions.SignedTransaction;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import net.corda.samples.example.flows.*;
import net.corda.samples.example.schema.DCRSchemaV1;
import net.corda.samples.example.schema.XVPSchemaV1;
import net.corda.samples.example.states.DCRState;
import net.corda.samples.example.states.XVPState;
import net.corda.samples.example.webserver.requests.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.util.encoders.Hex;

import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;


@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    @Autowired
    private Properties properties;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();
    }

    // Helpers for filtering the network map cache.
    public String toDisplayString(X500Name name) {
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
          .stream().filter(el -> nodeInfo.isLegalIdentity(el))
          .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo) {
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo) {
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @GetMapping(value = "/status", produces = TEXT_PLAIN_VALUE)
    private String status() {
        return "200";
    }

    @GetMapping(value = "/servertime", produces = TEXT_PLAIN_VALUE)
    private String serverTime() {
        return (LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC"))).toString();
    }

    @GetMapping(value = "/addresses", produces = TEXT_PLAIN_VALUE)
    private String addresses() {
        return proxy.nodeInfo().getAddresses().toString();
    }

    @GetMapping(value = "/identities", produces = TEXT_PLAIN_VALUE)
    private String identities() {
        return proxy.nodeInfo().getLegalIdentities().toString();
    }

    @GetMapping(value = "/platformversion", produces = TEXT_PLAIN_VALUE)
    private String platformVersion() {
        return Integer.toString(proxy.nodeInfo().getPlatformVersion());
    }

    @GetMapping(value = "/peers", produces = APPLICATION_JSON_VALUE)
    public HashMap<String, List<String>> getPeers() {
        HashMap<String, List<String>> myMap = new HashMap<>();

        // Find all nodes that are not notaries, ourself, or the network map.
        Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream()
          .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        // Get their names as strings
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
          .collect(Collectors.toList());

        myMap.put("peers", nodeNames);
        return myMap;
    }

    @GetMapping(value = "/notaries", produces = TEXT_PLAIN_VALUE)
    private String notaries() {
        return proxy.notaryIdentities().toString();
    }

    @GetMapping(value = "/flows", produces = TEXT_PLAIN_VALUE)
    private String flows() {
        return proxy.registeredFlows().toString();
    }

    @GetMapping(value = "/me", produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> whoami() {
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("me", me.toString());
        return myMap;
    }

    @GetMapping(value = "/keys", produces = APPLICATION_JSON_VALUE)
    private HashMap<String, String> keys() {
        HashMap<String, String> myMap = new HashMap<>();
        myMap.put("name", me.toString());
        Party meParty = proxy.wellKnownPartyFromX500Name(CordaX500Name.parse(me.toString()));
        myMap.put("me", "0x" + Hex.toHexString(meParty.getOwningKey().getEncoded()).substring(24));
        Party notary = proxy.notaryIdentities().get(0);
        myMap.put("notary", "0x" + Hex.toHexString(notary.getOwningKey().getEncoded()).substring(24));
        return myMap;
    }

    @GetMapping(value = "/dcrs", produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<DCRState>> getDCRs(@RequestParam(required = false, name = "tradeId") String tradeId) {
        if (tradeId == null) {
          return proxy.vaultQuery(DCRState.class).getStates();
        } else {
          List<StateAndRef<DCRState>> dcrs = proxy.vaultQuery(DCRState.class).getStates().stream().filter(
            it -> it.getState().getData().getTradeId() != null && it.getState().getData().getTradeId().equals(tradeId)).collect(Collectors.toList());
          return dcrs;
        }
    }

    @GetMapping(value = "my-dcrs", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<DCRState>>> getMyDCRs() {
        List<StateAndRef<DCRState>> dcrs = proxy.vaultQuery(DCRState.class).getStates().stream().filter(
          it -> it.getState().getData().getOwner().equals(proxy.nodeInfo().getLegalIdentities().get(0))).collect(Collectors.toList());
        return ResponseEntity.ok(dcrs);
    }

    @PostMapping(value = "create-dcr", produces = APPLICATION_JSON_VALUE, headers = "Content-Type=application/json")
    public ResponseEntity<Object> createDCR(@RequestBody CreateDCRRequest createDCRRequest) throws IllegalArgumentException {
        String value = new BigInteger(createDCRRequest.getValue(), 10).toString();
        String currency = createDCRRequest.getCurrency();
        // Create a new DCR state using the parameters given.
        try {
            // Start the CreateDCRFlow. We block and waits for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(CreateDCRFlow.Initiator.class, value, currency).getReturnValue().get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("output", result.getTx().getOutput(0));
            response.put("signatures", result.getSigs().size());
            // Return the response.
            return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(response);
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception e) {
            return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Collections.singletonMap("Error", e.getMessage()));
        }
    }

    @PostMapping(value = "earmark-dcr", produces = APPLICATION_JSON_VALUE, headers = "Content-Type=application/json")
    public ResponseEntity<Object> earmarkDCR(@RequestBody EarmarkDCRRequest earmarkDCRRequest, @RequestHeader(name = "Export", required = true, defaultValue = "/tmp") String exportFolder) throws IllegalArgumentException {
        String linearId = earmarkDCRRequest.getLinearId();
        String party = earmarkDCRRequest.getPartyName();
        String tradeId = earmarkDCRRequest.getTradeId();
        // Get party objects for new owner.
        CordaX500Name partyX500Name = CordaX500Name.parse(party);
        Party otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name);
        // Create a new DCR state using the parameters given.
        try {
            // Start the EarmarkDCRFlow. We block and waits for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(EarmarkDCRFlow.Initiator.class, linearId, otherParty, tradeId).getReturnValue().get();
            if (exportFolder != null) {
                // Query the earmarked dcr state
                QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
                FieldInfo dcrAttributeTradeId = QueryCriteriaUtils.getField("tradeId", DCRSchemaV1.PersistentDCR.class);
                CriteriaExpression dcrTradeIdIndex = Builder.equal(dcrAttributeTradeId, tradeId);
                FieldInfo dcrAttributeStatus = QueryCriteriaUtils.getField("status", DCRSchemaV1.PersistentDCR.class);
                CriteriaExpression dcrStatusIndex = Builder.equal(dcrAttributeStatus, "EARMARKED");
                QueryCriteria dcrTradeIdCriteria = new QueryCriteria.VaultCustomQueryCriteria(dcrTradeIdIndex);
                QueryCriteria dcrStatusCriteria = new QueryCriteria.VaultCustomQueryCriteria(dcrStatusIndex);
                QueryCriteria dcrCriteria = generalCriteria.and(dcrTradeIdCriteria).and(dcrStatusCriteria);
                Vault.Page<ContractState> dcrResults = proxy.vaultQueryByCriteria(dcrCriteria, DCRState.class);
                if (dcrResults.getStates().isEmpty()) {
                    throw new IllegalArgumentException("Found no dcr states for trade id [" + tradeId + "]");
                }
                StateAndRef dcrInputStateAndRef = (StateAndRef) dcrResults.getStates()
                  .stream()
                  .findFirst().orElseThrow(() -> new IllegalArgumentException("No dcr state with tradeId [" + tradeId + "] was found"));
                DCRState state = (DCRState) dcrInputStateAndRef.getState().getData();
                // Process signed transaction
                ObjectMapper mapper = new ObjectMapper();
                SerializationFactory factory = SerializationFactory.Companion.getDefaultFactory();
                // Export signed transaction
                SerializedBytes<SignedTransaction> bytes = factory.serialize(result, factory.getDefaultContext());
                String dataToWrite = toHexString(bytes.getBytes());
                if (!dataToWrite.isEmpty()) {
                    // Export the notary signature
                    HashMap<String, Object> notarySignature = new HashMap<>();
                    List<TransactionSignature> signatures = result.getSigs();
                    TransactionSignature sig = signatures.get(signatures.size() - 1);
                    String platformVersion = String.format("%d", sig.getSignatureMetadata().getPlatformVersion());
                    String schemaNumber = String.format("%d", sig.getSignatureMetadata().getSchemeNumberID());
                    String signatureBytes = toHexString(sig.getBytes());
                    String signatureBy = toHexString(sig.getBy().getEncoded());
                    String transactionId = toHexString(result.getId().getBytes());
                    PartialMerkleTree pTree = sig.getPartialMerkleTree();
                    if (pTree != null) {
                        String partialRoot = pTree.getRoot().toString().replace("IncludedLeaf(hash=", "").replace(")", "");
                        notarySignature.put("partialMerkleRoot", partialRoot);
                    }
                    notarySignature.put("platformVersion", platformVersion);
                    notarySignature.put("schemaNumber", schemaNumber);
                    notarySignature.put("encodedId", transactionId.toLowerCase());
                    notarySignature.put("encodedKey", signatureBy);
                    notarySignature.put("encodedSignature", signatureBytes);
                    // Construct result
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("tradeId", tradeId);
                    map.put("senderId", Base64.getEncoder().encodeToString(state.getOwner().getName().toString().getBytes()));
                    map.put("receiverId", Base64.getEncoder().encodeToString(state.getIssuer().getName().toString().getBytes()));
                    map.put("raw", dataToWrite);
                    map.put("signature", notarySignature);
                    String mappedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
                    //System.out.println("Output: \n" + mappedJson);
                    try {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
                        LocalDateTime now = LocalDateTime.now();
                        Files.write(Paths.get(exportFolder + "/corda-tx-earmark-" + dtf.format(now) + ".json"), mappedJson.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    return ResponseEntity
                      .status(HttpStatus.BAD_REQUEST)
                      .body(Collections.singletonMap("Error", "Failed to decode event and construct proof"));
                }
            } else {
                return ResponseEntity
                  .status(HttpStatus.BAD_REQUEST)
                  .body(Collections.singletonMap("Error", "Failed to export proof"));
            }
            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("output", result.getTx().getOutput(0));
            response.put("signatures", result.getSigs().size());
            // Return the response.
            return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(response);
        } catch (Exception e) {
            return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Collections.singletonMap("Error", e.getMessage()));
        }
    }

    @PostMapping(value = "confirm-dcr", produces = APPLICATION_JSON_VALUE, headers = "Content-Type=application/json")
    public ResponseEntity<Object> confirmDCR(@RequestBody ConfirmDCRRequest confirmDCRRequest) throws IllegalArgumentException {
        String tradeId = confirmDCRRequest.getTradeId();
        String encodedInfo = confirmDCRRequest.getEncodedInfo();
        String signatureOrProof = confirmDCRRequest.getSignatureOrProof();
        // Create a new DCR state using the parameters given.
        try {
            // Start the ConfirmDCRFlow. We block and waits for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(ConfirmDCRFlow.Initiator.class, tradeId, encodedInfo, signatureOrProof).getReturnValue().get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("output", result.getTx().getOutput(0));
            response.put("signatures", result.getSigs().size());
            // Return the response.
            return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(response);
        } catch (Exception e) {
            return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Collections.singletonMap("Error", e.getMessage()));
        }
    }

    @PostMapping(value = "cancel-dcr", produces = APPLICATION_JSON_VALUE, headers = "Content-Type=application/json")
    public ResponseEntity<Object> cancelDCR(@RequestBody CancelDCRRequest cancelDCRRequest, @RequestHeader(name = "Export", required = true, defaultValue = "/tmp") String exportFolder) throws IllegalArgumentException {
        String tradeId = cancelDCRRequest.getTradeId();
        String encodedInfo = cancelDCRRequest.getEncodedInfo();
        String signatureOrProof = cancelDCRRequest.getSignatureOrProof();
        // Revert an earmarked DCR.
        try {
            // Start the CancelDCRFlow. We block and waits for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(CancelDCRFlow.Initiator.class, tradeId, encodedInfo, signatureOrProof).getReturnValue().get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("output", result.getTx().getOutput(0));
            response.put("signatures", result.getSigs().size());
            // Return the response.
            return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(response);
        } catch (Exception e) {
            return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Collections.singletonMap("Error", e.getMessage()));
        }
    }

    @GetMapping(value = "/xvps", produces = APPLICATION_JSON_VALUE)
    public List<StateAndRef<XVPState>> getXVPs(@RequestParam(required = false, name = "tradeId") String tradeId) {
        if (tradeId == null) {
            return proxy.vaultQuery(XVPState.class).getStates();
        } else {
            List<StateAndRef<XVPState>> xvps = proxy.vaultQuery(XVPState.class).getStates().stream().filter(
              it -> it.getState().getData().getTradeId() != null && it.getState().getData().getTradeId().equals(tradeId)).collect(Collectors.toList());
            return xvps;
        }
    }

    @PostMapping(value = "create-xvp", produces = APPLICATION_JSON_VALUE, headers = "Content-Type=application/json")
    public ResponseEntity<Object> createXVP(@RequestBody CreateXVPRequest createXVPRequest) throws IllegalArgumentException {
        String tradeId = createXVPRequest.getTradeId();
        String assetId = createXVPRequest.getAssetId();
        CordaX500Name fromX500Name = CordaX500Name.parse(createXVPRequest.getFrom());
        Party fromParty = proxy.wellKnownPartyFromX500Name(fromX500Name);
        CordaX500Name toX500Name = CordaX500Name.parse(createXVPRequest.getTo());
        Party toParty = proxy.wellKnownPartyFromX500Name(toX500Name);
        if (fromParty == null || toParty == null) {
            return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Collections.singletonMap("Error", "Parties could not be resolved"));
        }
        // Create a new XVP state using the parameters given.
        try {
            // Start the CreateXVPFlow. We block and waits for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(CreateXVPFlow.Initiator.class, tradeId, assetId, fromParty, toParty).getReturnValue().get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("output", result.getTx().getOutput(0));
            response.put("signatures", result.getSigs().size());
            // Return the response.
            return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(response);
            // For the purposes of this demo app, we do not differentiate by exception type.
        } catch (Exception e) {
            return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Collections.singletonMap("Error", e.getMessage()));
        }
    }

    @PostMapping(value = "cancel-xvp", produces = APPLICATION_JSON_VALUE, headers = "Content-Type=application/json")
    public ResponseEntity<Object> cancelXVP(@RequestBody CancelXVPRequest cancelXVPRequest, @RequestHeader(name = "Export", required = true, defaultValue = "/tmp") String exportFolder) throws IllegalArgumentException {
        String tradeId = cancelXVPRequest.getTradeId();
        // Create a new DCR state using the parameters given.
        try {
            // Start the CancelDCRFlow. We block and waits for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(CancelXVPFlow.Initiator.class, tradeId).getReturnValue().get();
            if (exportFolder != null) {
                // Query the cancelled xvp state
                QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);
                FieldInfo xvpAttributeTradeId = QueryCriteriaUtils.getField("tradeId", XVPSchemaV1.PersistentXVP.class);
                CriteriaExpression xvpTradeIdIndex = Builder.equal(xvpAttributeTradeId, tradeId);
                FieldInfo xvpAttributeStatus = QueryCriteriaUtils.getField("status", XVPSchemaV1.PersistentXVP.class);
                CriteriaExpression xvpStatusIndex = Builder.equal(xvpAttributeStatus, "CANCELLED");
                QueryCriteria xvpTradeIdCriteria = new QueryCriteria.VaultCustomQueryCriteria(xvpTradeIdIndex);
                QueryCriteria xvpStatusCriteria = new QueryCriteria.VaultCustomQueryCriteria(xvpStatusIndex);
                QueryCriteria xvpCriteria = generalCriteria.and(xvpTradeIdCriteria).and(xvpStatusCriteria);
                Vault.Page<ContractState> xvpResults = proxy.vaultQueryByCriteria(xvpCriteria, XVPState.class);
                if (xvpResults.getStates().isEmpty()) {
                    throw new IllegalArgumentException("Found no xvp states for trade id [" + tradeId + "]");
                }
                StateAndRef xvpInputStateAndRef = (StateAndRef) xvpResults.getStates()
                  .stream()
                  .findFirst().orElseThrow(() -> new IllegalArgumentException("No xvp state with tradeId [" + tradeId + "] was found"));
                XVPState state = (XVPState) xvpInputStateAndRef.getState().getData();
                // Process signed transaction
                ObjectMapper mapper = new ObjectMapper();
                SerializationFactory factory = SerializationFactory.Companion.getDefaultFactory();
                SerializedBytes<SignedTransaction> bytes = factory.serialize(result, factory.getDefaultContext());
                String dataToWrite = toHexString(bytes.getBytes());
                // Export signed transaction
                if (!dataToWrite.isEmpty()) {
                    // Export the notary signature
                    HashMap<String, Object> notarySignature = new HashMap<>();
                    List<TransactionSignature> signatures = result.getSigs();
                    TransactionSignature sig = signatures.get(signatures.size() - 1);
                    String platformVersion = String.format("%d", sig.getSignatureMetadata().getPlatformVersion());
                    String schemaNumber = String.format("%d", sig.getSignatureMetadata().getSchemeNumberID());
                    String signatureBytes = toHexString(sig.getBytes());
                    String signatureBy = toHexString(sig.getBy().getEncoded());
                    String transactionId = toHexString(result.getId().getBytes());
                    PartialMerkleTree pTree = sig.getPartialMerkleTree();
                    if (pTree != null) {
                        String partialRoot = pTree.getRoot().toString().replace("IncludedLeaf(hash=", "").replace(")", "");
                        notarySignature.put("partialMerkleRoot", partialRoot);
                    }
                    notarySignature.put("platformVersion", platformVersion);
                    notarySignature.put("schemaNumber", schemaNumber);
                    notarySignature.put("encodedId", transactionId);
                    notarySignature.put("encodedKey", signatureBy);
                    notarySignature.put("encodedSignature", signatureBytes);
                    // Construct resulting map
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("tradeId", tradeId);
                    map.put("receiverId", Base64.getEncoder().encodeToString(state.getSender().getName().toString().getBytes()));
                    map.put("senderId", Base64.getEncoder().encodeToString(state.getReceiver().getName().toString().getBytes()));
                    map.put("raw", dataToWrite);
                    map.put("signature", notarySignature);
                    String mappedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
                    //System.out.println("Output: \n" + mappedJson);
                    try {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
                        LocalDateTime now = LocalDateTime.now();
                        Files.write(Paths.get(exportFolder + "/corda-tx-cancel-" + dtf.format(now) + ".json"), mappedJson.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    return ResponseEntity
                      .status(HttpStatus.BAD_REQUEST)
                      .body("Failed to decode event and construct proof");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("output", result.getTx().getOutput(0));
            response.put("signatures", result.getSigs().size());
            // Return the response.
            return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(response);
        } catch (Exception e) {
            return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Collections.singletonMap("Error", e.getMessage()));
        }
    }

    @PostMapping(value = "resolve-xvp", produces = APPLICATION_JSON_VALUE, headers = "Content-Type=application/json")
    public ResponseEntity<Object> resolveXVP(@RequestBody ResolveXVPRequest resolveXVPRequest) throws IllegalArgumentException {
        String tradeId = resolveXVPRequest.getTradeId();
        // Create a new DCR state using the parameters given.
        try {
            // Start the ResolveXVPFlow. We block and waits for the flow to return.
            SignedTransaction result = proxy.startTrackedFlowDynamic(ResolveXVPFlow.Initiator.class, tradeId).getReturnValue().get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", result.getId());
            response.put("signatures", result.getSigs().size());
            // Return the response.
            return ResponseEntity
              .status(HttpStatus.CREATED)
              .body(response);
        } catch (Exception e) {
            return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(Collections.singletonMap("Error", e.getMessage()));
        }
    }

    private static final String HEXES = "0123456789ABCDEF";

    private String toHexString(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
              .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    public static byte [] fromHexString(final String s) {
        if (s == null || (s.length () % 2) == 1)
            throw new IllegalArgumentException ();
        final char [] chars = s.toCharArray ();
        final int len = chars.length;
        final byte [] data = new byte [len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit (chars[i], 16) << 4) + Character.digit (chars[i + 1], 16));
        }
        return data;
    }

    // Base58 utils with logic is copied from bitcoinj.
    protected static final char[] BASE58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();
    protected static final char ENCODED_ZERO = BASE58_CHARS[0];
    protected static final int[] INDEXES = new int[128];

    static {
        Arrays.fill(INDEXES, -1);
        for (int i = 0; i < BASE58_CHARS.length; i++) {
            INDEXES[BASE58_CHARS[i]] = i;
        }
    }

    // Encodes the given bytes as a base58 string (no checksum is appended).
    public static String toBase58String(final byte[] input) {
        if (null == input || input.length == 0) {
            return "";
        }
        final char[] encoded = new char[input.length * 2];
        final byte[] copy = Arrays.copyOf(input, input.length); // Since we modify it in-place.
        // Count leading zeros.
        int zeros = 0;
        while (zeros < input.length && input[zeros] == 0) {
            ++zeros;
        }
        int inputIndex = zeros;
        int outputIndex = encoded.length;
        while (inputIndex < copy.length) {
            encoded[--outputIndex] = BASE58_CHARS[divMod(copy, inputIndex, 256, 58)];
            if (copy[inputIndex] == 0) {
                ++inputIndex; // Optimization to skip leading zeros.
            }
        }
        // Preserve exactly as many leading encoded zeros in output as there were leading zeros in input.
        while (outputIndex < encoded.length && encoded[outputIndex] == ENCODED_ZERO) {
            ++outputIndex;
        }
        while (--zeros >= 0) {
            encoded[--outputIndex] = ENCODED_ZERO;
        }
        // Return encoded string (including encoded leading zeros).
        return new String(encoded, outputIndex, encoded.length - outputIndex);
    }

    // Divides a number, represented as an array of bytes each containing a single digit in the specified base, by the given divisor. The given number is modified in-place to contain the quotient, and the return value is the remainder.
    private static byte divMod(byte[] number, int firstDigit, int base, int divisor) {
        // Long division which accounts for the base of the input digits
        int remainder = 0;
        for (int i = firstDigit; i < number.length; i++) {
            int digit = (int) number[i] & 0xff;
            int temp = remainder * base + digit;
            number[i] = (byte) (temp / divisor);
            remainder = temp % divisor;
        }
        return (byte) remainder;
    }
}

