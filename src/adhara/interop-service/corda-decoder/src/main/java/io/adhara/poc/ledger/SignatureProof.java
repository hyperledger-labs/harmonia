package io.adhara.poc.ledger;

import io.adhara.poc.utils.Utils;
import lombok.Data;
import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class SignatureProof {
	private static final Logger logger = LoggerFactory.getLogger(SignatureProof.class);

	// Using fingerprint to extract deserialized data is NOT maintainable in the sense that the schema can change. In a production implementation we will pull this from the included schema.
	private static final String DESCRIPTOR_SIGNED_TRANSACTION = "net.corda:zToILi8Cg+z9QG52DsFT9g==";
	private static final String DESCRIPTOR_SIGNATURE_METADATA = "net.corda:IzFt8cRKytsJq3vQ+yjsGg==";
	private static final String DESCRIPTOR_TRANSACTION_SIGNATURE = "net.corda:JDgI4T6c+qDdhNXY0kFjiQ==";
	private static final String DESCRIPTOR_SECURE_HASH = "net.corda:7YZSUU3tC6YvtX33Klo9Jg==";
	private static final String DESCRIPTOR_PARTIAL_TREE = "net.corda:QZFO4s8ng/jneOx6aC95/Q==";
	private static final String DESCRIPTOR_PARTY = "net.corda:ngdwbt6kRT0l5nn16uf87A==";
	private static final String DESCRIPTOR_PUBLIC_KEY = "net.corda:java.security.PublicKey";
	private static final String DESCRIPTOR_WIRE_TRANSACTION = "net.corda:tfE4ru/0RkQp8D2wkDqzRQ=="; // net.corda.core.transactions.WireTransaction
	private static final String DESCRIPTOR_SALT = "net.corda:1skUfBacU1AgmLX8M1z83A==";             // net.corda.core.contracts.PrivacySalt
	private static final String DESCRIPTOR_COMPONENT_GROUP = "net.corda:HneSPA89MGhpizVLE3wcOg==";  // net.corda.core.transactions.ComponentGroup
	private static final String DESCRIPTOR_SERIALIZED_BYTES = "net.corda:LY55YUDjxO84OlwSwUzvSA=="; // net.corda.core.serialization.SerializedBytes
	private static final String DESCRIPTOR_LIDS = "net.corda:rniw7B2Mqi7zlkPpKmJ77A==";
	private static final String DESCRIPTOR_CONTRACT = "net.corda:Q0zUGN/K6wwwyuIlNf3Raw==";

  private static final String DESCRIPTOR_HOMESTEAD_DCR = "net.corda:DldW9yS4tBOze6qv6U4QTA==";
  private static final String DESCRIPTOR_HOMESTEAD_XVP = "net.corda:9GdANdKRptKFtq6zQDfG+A==";

  private static final String DESCRIPTOR_3RDPARTY_COMMAND = "net.corda:xW8RtS06zbreNWxv92ly4w=="; // "net.corda:MelVjkkQJGuWcRt/sNVptA==";
	private static final String DESCRIPTOR_3RDPARTY_AMOUNT = "net.corda:EX8RRuprLshI1m51O4333A==";
	private static final int LID_3RDPARTY_TRADEID_INDEX = 2;

	private static final int NONCE_SIZE = 8;
	public static final int COMPONENT_GROUP_OUTPUTS = 1;
	public static final int COMPONENT_GROUP_COMMANDS = 2;
	public static final int COMPONENT_GROUP_NOTARY = 4;
	public static final int COMPONENT_GROUP_SIGNERS = 6;

	private String raw;
	private List<SignatureData> contents = new ArrayList<>();
	private String contract;
	private String event;
	private String id;
	private String command;
	private String amount;
	private String currency;
	private List<String> parties;
	private SecureHash root;
	private SecureHash hash;
	private String salt;
	private MerkleProof proof;

	//private SecureHash outputsComponentHash;
	private String[] outputsComponentGroup;
	//private SecureHash commandsComponentHash;
	private String[] commandsComponentGroup;
	//private SecureHash signersComponentHash;
	private String[] signersComponentGroup;
	//private SecureHash notaryComponentHash;
	private String[] notaryComponentGroup;

	public SignatureProof()	{
	}

  public SignatureProof(String encoded, List<Extracted<String, Object>> components) throws Exception {
		raw = encoded;
		// These are Corda specific fingerprints, hard-coded for signed wire transactions
		byte[] privacySalt = parseHexByFingerprint(components, DESCRIPTOR_SALT);
		List<SignedData> signatures = parseSignatures(components, DESCRIPTOR_PUBLIC_KEY, DESCRIPTOR_TRANSACTION_SIGNATURE, DESCRIPTOR_SIGNATURE_METADATA, DESCRIPTOR_SECURE_HASH);
		Map<Integer, List<ComponentGroup>> componentGroups = parseComponentGroups(components, DESCRIPTOR_COMPONENT_GROUP, DESCRIPTOR_SERIALIZED_BYTES, privacySalt);
		raw = parseStringByFingerprint(components, DESCRIPTOR_WIRE_TRANSACTION);
		salt = Utils.toHexString(privacySalt);
		logger.debug("Extracted salt: " + salt);
		logger.debug("Extracted " + signatures.size() + " signatures: " + signatures);
		logger.debug("Extracted wire transaction: " + raw);
		SecureHash ones = SecureHash.getOnes(SecureHash.SHA_256);
		List<SecureHash> componentLeaves = new ArrayList<>();
		List<SecureHash> componentHashes = new ArrayList<>();
		for (int ordinal = 0; ordinal < 9; ordinal++) {
			List<ComponentGroup> group = componentGroups.getOrDefault(ordinal, null);
			if (group != null && group.size() > 0) {
				List<SecureHash> paddedLeaves = group.stream().map(ComponentGroup::getHash).collect(Collectors.toList());
				MerkleTree componentGroup = MerkleTree.getMerkleTree(paddedLeaves);
				if (componentGroup == null)
					throw new IllegalArgumentException("Failed to compute component group Merkle tree");
				if (ordinal == COMPONENT_GROUP_OUTPUTS) {
					outputsComponentGroup = new String[group.size()];
					for (int g = 0; g < group.size(); g++) {
						outputsComponentGroup[g] = Utils.toHexString(group.get(g).getOpaqueBytes()).replaceFirst("636F726461010000", "");
						logger.debug("Extracted " + g + "th output component group: " + outputsComponentGroup[g]);
					}
					componentHashes.add(componentGroup.getHash());
				} else if (ordinal == COMPONENT_GROUP_COMMANDS) {
					commandsComponentGroup = new String[group.size()];
					for (int g = 0; g < group.size(); g++) {
						commandsComponentGroup[g] = Utils.toHexString(group.get(g).getOpaqueBytes()).replaceFirst("636F726461010000", "");
						logger.debug("Extracted " + g + "th command component group: " + commandsComponentGroup[g]);
					}
					componentHashes.add(componentGroup.getHash());
				} else if (ordinal == COMPONENT_GROUP_NOTARY) {
					notaryComponentGroup = new String[group.size()];
					for (int g = 0; g < group.size(); g++) {
						notaryComponentGroup[g] = Utils.toHexString(group.get(g).getOpaqueBytes()).replaceFirst("636F726461010000", "");
						logger.debug("Extracted " + g + "th notary component group: " + notaryComponentGroup[g]);
					}
					componentHashes.add(componentGroup.getHash());
				} else if (ordinal == COMPONENT_GROUP_SIGNERS) {
					signersComponentGroup = new String[group.size()];
					for (int g = 0; g < group.size(); g++) {
						signersComponentGroup[g] = Utils.toHexString(group.get(g).getOpaqueBytes()).replaceFirst("636F726461010000", "");
						logger.debug("Extracted " + g + "th signer component group: " + signersComponentGroup[g]);
					}
					componentHashes.add(componentGroup.getHash());
				}
				componentLeaves.add(componentGroup.getHash());
			} else {
				componentLeaves.add(ones);
			}
		}

		MerkleTree componentTree = MerkleTree.getMerkleTree(componentLeaves);
 	  assert componentTree != null;
		root = componentTree.getHash();
		logger.info("Recovered transaction root: " + root.toString());

		List<SecureHash> paddedLeaves = MerkleTree.getPaddedLeaves(componentLeaves);
		proof = MerkleTree.generateMultiProof(paddedLeaves, componentHashes); //Collections.singletonList(outputsComponentHash));
		//logger.debug("Leaves: ");
		//logLeaves(paddedLeaves);
		//logger.debug("Proof: ");
		//logProof(proof);
		if (!MerkleTree.verifyMultiProof(root, proof.getProof(), proof.getFlags(), proof.getLeaves())) {
			throw new IllegalArgumentException("Proof did not verify correctly");
		}
		List<SecureHash> txLeaves = Collections.singletonList(root.rehash());
		MerkleTree txTree = MerkleTree.getMerkleTree(txLeaves);
		PartialMerkleTree parTree = PartialMerkleTree.build(txTree, txLeaves);
		for (SignedData signature : signatures) {
			if (signature.hasPartialTree() && !signature.getPartialTree().equals(parTree.getRoot().getHash().toString())) {
				throw new IllegalArgumentException("Partial trees are not fully supported yet");
			}
			SignedMeta signedMeta = new SignedMeta(signature.getPlatformVersion(), signature.getSchemaNumber());
			String signableData = signature.hasPartialTree() ? getSignableData(root, signature.getPartialTreeRoot(root), signedMeta) : getSignableData(root, null, signedMeta);
			String signatureMeta = String.format("%08X%08X%s", signature.getPlatformVersion(), signature.getSchemaNumber(), signature.hasPartialTree() ? signature.getPartialTree() : root.toString());
			contents.add(new SignatureData(signature.getBy(), signature.getBytes(), signableData, signatureMeta,null));
		}
		logSignatures();

		// These are commonly used Corda fingerprints, not guaranteed to be there
		contract = parseStringByFingerprint(components, DESCRIPTOR_CONTRACT);
		List<Object> linearIds = parseOrderedListByFingerprint(components, DESCRIPTOR_LIDS); // 128 bits => 16 bytes
		parties = parseCordaPartiesByFingerprint(components);

		// These are 3rd party specific fingerprints, hard-coded for DCRs
		command = parseStringByFingerprint(components, DESCRIPTOR_3RDPARTY_COMMAND);
		amount = parseCurrencyAmountByFingerprint(components, DESCRIPTOR_3RDPARTY_AMOUNT);
		if (amount != null) {
			String[] split = amount.split(":");
			amount = split[0];
			currency = split[1];
		}
		id = linearIds.size() > LID_3RDPARTY_TRADEID_INDEX && linearIds.get(LID_3RDPARTY_TRADEID_INDEX) != null ? linearIds.get(LID_3RDPARTY_TRADEID_INDEX).toString() : "";

		// These are PoC specific fingerprints, hardcoded for DCRs
		List<Object> homesteadDCRFields = parseOrderedListByFingerprint(components, DESCRIPTOR_HOMESTEAD_DCR);
    if (!homesteadDCRFields.isEmpty()) {
			currency = (String) homesteadDCRFields.get(0);
			command = (String) homesteadDCRFields.get(2);
			id = (String) homesteadDCRFields.get(3);
			amount = (String) homesteadDCRFields.get(4);
		}
		List<Object> homesteadXVPFields = parseOrderedListByFingerprint(components, DESCRIPTOR_HOMESTEAD_XVP);
		if (!homesteadXVPFields.isEmpty()) {
			command = (String) homesteadXVPFields.get(1);
			id = (String) homesteadXVPFields.get(2);
		}

		event = String.format("%s:%s:%s:%s", contract != null ? contract : "", command != null ? command : "", id != null ? id : "", amount != null ? amount : "");
		logger.info("Recovered event: " + event);
	}

	public boolean verify() throws Exception {
		for (SignatureData signature : contents) {
			byte[] compressedKey = SignatureData.getCompressedPublicKey(signature.getBy(), signature.getSignatureScheme());
			String publicKey = String.format("%064x", new BigInteger(1, compressedKey)).toUpperCase();
			if (!verifySignature(signature.getBy(), signature.getBytes(), signature.getData())) {
				logger.info("Transaction signature for public key [" + publicKey + "] is not valid");
				return false;
			} else {
				logger.info("Transaction signature for public key [" + publicKey + "] is valid");
			}
		}
		return true;
	}

	public static boolean verify(SecureHash root, SignedData signedData) throws Exception {
		SignatureProof base = new SignatureProof();
		SignedMeta signedMeta = new SignedMeta(signedData.getPlatformVersion(), signedData.getSchemaNumber());
		String signableData = base.getSignableData(root, signedData.getPartialTreeRoot(root), signedMeta);
		String signatureMeta = String.format("%08X%08X%s", signedData.getPlatformVersion(), signedData.getSchemaNumber(), signedData.hasPartialTree() ? signedData.getPartialTree() : root.toString());
		SignatureData signature = new SignatureData(signedData.getBy(), signedData.getBytes(), signableData, signatureMeta, null);
		byte[] compressedKey = SignatureData.getCompressedPublicKey(signature.getBy(), signedData.getSchemaNumber());
		String publicKey = String.format("%064x", new BigInteger(1, compressedKey)).toUpperCase();
		if (!base.verifySignature(signature.getBy(), signature.getBytes(), signature.getData())) {
			logger.info("Transaction signature for public key [" + publicKey + "] is not valid");
			return false;
		} else {
			logger.info("Transaction signature for public key [" + publicKey + "] is valid");
		}
		return true;
	}

	private void logLeaves(List<SecureHash> leaves) {
		for (SecureHash leaf : leaves) {
			logger.debug(leaf.toString());
		}
	}

	private void logSignatures() throws InvalidKeySpecException {
		logger.debug("Recovered " + contents.size() + " signatures:");
		int i = 0;
		for (SignatureData signature : contents) {
			logger.debug("  [" +i+ "] by  : " + String.format("%064x", new BigInteger(1, signature.getPublicKey())).toUpperCase());
			logger.debug("  [" +i+ "] sigR: " + String.format("%064x", new BigInteger(1, signature.getSignatureR())).toUpperCase());
			logger.debug("  [" +i+ "] sigS: " + String.format("%064x", new BigInteger(1, signature.getSignatureS())).toUpperCase());
			logger.debug("  [" +i+ "] sigV: " + String.format("%02x", new BigInteger(1, signature.getSignatureV())).toUpperCase());
			logger.debug("  [" +i+ "] meta: " + signature.getMeta().toUpperCase());
			logger.debug("  [" +i+ "] data: " + signature.getData());
			i++;
		}
	}

	private void logProof(MerkleProof proof) {
		logger.debug("Leaves:");
		for (SecureHash leaf : proof.getLeaves()) {
			logger.debug(leaf.toString());
		}
		logger.debug("Flags:");
		for (byte flag : proof.getFlags()) {
			logger.debug(String.format("%02x", flag).toUpperCase());
		}
		logger.debug("Witness:");
		for (SecureHash witness : proof.getProof()) {
			logger.debug(witness.toString());
		}
	}

	public static Signatures getSignatures(SecureHash root, SignedData signedData) throws InvalidKeySpecException, NoSuchAlgorithmException {
		SignatureProof base = new SignatureProof();
		SignedMeta signedMeta = new SignedMeta(signedData.getPlatformVersion(), signedData.getSchemaNumber());
		String signableData = base.getSignableData(root, signedData.getPartialTreeRoot(root), signedMeta);
		String signatureMeta = String.format("%08X%08X%s", signedData.getPlatformVersion(), signedData.getSchemaNumber(), signedData.hasPartialTree() ? signedData.getPartialTree() : root.toString());
		SignatureData signature = new SignatureData(signedData.getBy(), signedData.getBytes(), signableData, signatureMeta, null);
		List<Signature> sigList = new ArrayList<>();
		Signature sig = new Signature();
		sig.by = new Uint256(new BigInteger(1, signature.getPublicKey()));
		sig.sigR = new Uint256(new BigInteger(1, signature.getSignatureR()));
		sig.sigS = new Uint256(new BigInteger(1, signature.getSignatureS()));
		sig.sigV = new Uint256(new BigInteger(1, signature.getSignatureV()));
		sig.meta = new DynamicBytes(Numeric.hexStringToByteArray(signature.getMeta().toUpperCase()));
		sigList.add(sig);
		Signatures signatures = new Signatures();
		signatures.typ = new Uint256(3);
		signatures.signatures = new DynamicArray<>(Signature.class, sigList);
		return signatures;
	}

	public Signatures getSignatures() throws InvalidKeySpecException, NoSuchAlgorithmException {
		List<Signature> sigList = new ArrayList<>();
		for (SignatureData signature : contents) {
			Signature sig = new Signature();
			sig.by = new Uint256(new BigInteger(1, signature.getPublicKey()));
			sig.sigR = new Uint256(new BigInteger(1, signature.getSignatureR()));
			sig.sigS = new Uint256(new BigInteger(1, signature.getSignatureS()));
			sig.sigV = new Uint256(new BigInteger(1, signature.getSignatureV()));
			sig.meta = new DynamicBytes(Numeric.hexStringToByteArray(signature.getMeta().toUpperCase()));
			sigList.add(sig);
		}
		Signatures signatures = new Signatures();
		signatures.typ = new Uint256(3);
		signatures.signatures = new DynamicArray<>(Signature.class, sigList);
		return signatures;
	}

	private String getSignableData(SecureHash txId, PartialTree partialTreeRoot, SignedMeta sigMeta) {
		SecureHash id = getOriginallySignedHash(txId, partialTreeRoot);
		// Note that this will only work for small integers < 255, anything bigger we need to add six bytes and use integer (code=71) encoding
		String prefix = "636F7264610100000080C562000000000001D0000003D00000000300A3226E65742E636F7264613A4D5941396A726E4E646C5161615830366F45736D78413D3DC0770200A3226E65742E636F7264613A497A46743863524B7974734A713376512B796A7347673D3D";
		String meta = "C00502" + "54" + String.format("%02X", sigMeta.getPlatformVersion()) + "54" + String.format("%02X", sigMeta.getSchemaNumber());
		String postfix = "00A3226E65742E636F7264613A37595A535555337443365976745833334B6C6F394A673D3DC02301A020";
		String value = id.toString();
		String schema = "0080C562000000000002D00000031200000001D000000309000000040080C562000000000005C0E405A1226E65742E636F7264612E636F72652E63727970746F2E5369676E61626C654461746140450080C562000000000003C02602A3226E65742E636F7264613A4D5941396A726E4E646C5161615830366F45736D78413D3D40C089020080C562000000000004C04207A1117369676E61747572654D65746164617461A1276E65742E636F7264612E636F72652E63727970746F2E5369676E61747572654D6574616461746145404041420080C562000000000004C02E07A10474784964A1206E65742E636F7264612E636F72652E63727970746F2E5365637572654861736845404041420080C562000000000005C0B405A1276E65742E636F7264612E636F72652E63727970746F2E5369676E61747572654D6574616461746140450080C562000000000003C02602A3226E65742E636F7264613A497A46743863524B7974734A713376512B796A7347673D3D40C054020080C562000000000004C01E07A10F706C6174666F726D56657273696F6EA103696E7445A101304041420080C562000000000004C01D07A10E736368656D654E756D6265724944A103696E7445A101304041420080C562000000000005C0BB05A1206E65742E636F7264612E636F72652E63727970746F2E5365637572654861736840450080C562000000000003C02602A3226E65742E636F7264613A62373950654D424C73487875324132337944595261413D3D40C062030080C562000000000004C01507A1056279746573A10662696E61727945404041420080C562000000000004C01507A1066F6666736574A103696E7445A101304041420080C562000000000004C01307A10473697A65A103696E7445A101304041420080C562000000000005C08205A1276E65742E636F7264612E636F72652E63727970746F2E536563757265486173682453484132353640450080C562000000000003C02602A3226E65742E636F7264613A37595A535555337443365976745833334B6C6F394A673D3D40C022010080C562000000000004C01507A1056279746573A10662696E61727945404041420080C562000000000009C10100";
		return prefix + meta + postfix + value + schema;
	}

	private SecureHash getOriginallySignedHash(SecureHash txId, PartialTree partialTreeRoot) {
		if (partialTreeRoot != null) {
			List<SecureHash> usedHashes = new ArrayList<>();
			SecureHash root = PartialMerkleTree.rootAndUsedHashes(partialTreeRoot, usedHashes);
			if (!usedHashes.contains(txId.rehash())) {
				logger.error("Transaction with id " + txId + " is not a leaf in the provided partial tree");
			}
			return root;
		} else {
			return txId;
		}
	}

	private boolean verifySignature(String pubKey, String signature, String message) throws Exception {
		final PublicKey key = parsePublicKey(Utils.fromHexString(pubKey));
		logger.debug("Verifying message: " + message);
		try {
			final EdDSAEngine engine = new EdDSAEngine(SecureHash.getDigestFor(SecureHash.SHA_512));
			engine.initVerify(key);
			engine.update(Utils.fromHexString(message));
			return engine.verify(Utils.fromHexString(signature));
		} catch (Exception e) {
			final java.security.Signature ecdsaVerify = java.security.Signature.getInstance("SHA256withECDSA");
			ecdsaVerify.initVerify(key);
			ecdsaVerify.update(Utils.fromHexString(message));
			return ecdsaVerify.verify(Utils.fromHexString(signature));
		}
	}

	private PublicKey parsePublicKey(byte[] encodedKeyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
		final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKeyBytes);
		logger.debug("Parsing " + keySpec.getFormat() + " public key");
		try {
			EdDSAPublicKey key = new EdDSAPublicKey(keySpec);
			return key;
		} catch (Exception e) {
			KeyFactory keyFactory = KeyFactory.getInstance("EC");
			PublicKey key = keyFactory.generatePublic(keySpec);
			return key;
		}
	}

	private byte[] parseHexByFingerprint(List<Extracted<String, Object>> components, String fingerprint) {
		for (Extracted<String, Object> item : components) {
			if (item.getValue() == null) {
				continue;
			}
			if (item.getKey().equals(fingerprint)) {
				return Utils.fromHexString((String) item.getValue());
			}
		}
		return null;
	}

	private void logExtracted(List<Extracted<String, Object>> components) {
		for (Extracted<String, Object> item : components) {
			if (item.getValue() == null) {
				continue;
			}
			logger.debug("Fingerprint: " + item.getKey() + ": " + item.getValue());
		}
	}

	private String parseStringByFingerprint(List<Extracted<String, Object>> components, String fingerprint) {
		for (Extracted<String, Object> item : components) {
			if (item.getValue() == null) {
				continue;
			}
			if (item.getKey().equals(fingerprint)) {
				return (String) item.getValue();
			}
		}
		return null;
	}

	private List<Object> parseOrderedListByFingerprint(List<Extracted<String, Object>> components, String fingerprint) {
		List<Object> result = new ArrayList<>();
		for (Extracted<String, Object> item : components) {
			if (item.getKey().equals(fingerprint)) {
				result.add(item.getValue());
			}
		}
		return result;
	}

	private List<Object> parseOrderedListByFingerprintWithAdditional(List<Extracted<String, Object>> components, String fingerprint, int increment, String additional) {
		List<Object> result = new ArrayList<>();
		int i = 0;
		for (Extracted<String, Object> item : components) {
			if (item.getKey().equals(fingerprint)) {
				result.add(item.getValue());
				i++;
			} else if (i == increment && item.getKey().equals(additional)) {
				result.add(item.getValue());
				i = 0;
			}
		}
		return result;
	}

	private String generateRFC1779DistinguishedName(List<String> names) {
		if (names.size() == 1) {
			return names.get(0);
		}
		StringBuilder sb = new StringBuilder(48);
		for (int i=names.size()-1; i >= 0; i--) {
			if (i != names.size()-1) {
				sb.append(", ");
			}
			sb.append(names.get(i));
		}
		return sb.toString();
	}

	private List<String> generateX500Names(HashMap<String, String> map) {
		List<String> list = new ArrayList<String>();
		list.add("C="+map.get("country"));
		if (map.get("state") != null) list.add("ST="+map.get("state"));
		list.add("L="+map.get("locality"));
		list.add("O="+map.get("organisation"));
		if (map.get("organisationUnit") != null) list.add("OU="+map.get("organisationUnit"));
		if (map.get("commonName") != null) list.add("CN="+map.get("commonName"));
		return list;
	}

	private List<String> parseCordaPartiesByFingerprint(List<Extracted<String, Object>> components) {
		int size = 6;
		List<Object> partyObjectFields = parseOrderedListByFingerprintWithAdditional(components, DESCRIPTOR_PARTY, size, DESCRIPTOR_PUBLIC_KEY);
		List<String> result = new ArrayList<>();
		int j = size+1;
		if (!partyObjectFields.isEmpty()) {
			logger.debug("Parties:");
			for (int i=0; i<partyObjectFields.size()/j; i++) {
				HashMap<String,String> partyMap = new HashMap<>();
				partyMap.put("commonName", (String)partyObjectFields.get(i*j+0));
				partyMap.put("country", (String)partyObjectFields.get(i*j+1));
				partyMap.put("locality", (String)partyObjectFields.get(i*j+2));
				partyMap.put("organisation", (String)partyObjectFields.get(i*j+3));
				partyMap.put("organisationUnit", (String)partyObjectFields.get(i*j+4));
				partyMap.put("state", (String)partyObjectFields.get(i*j+5));
				String x500Name = generateRFC1779DistinguishedName(generateX500Names(partyMap));
				result.add(x500Name);
				String publicKey = (String)partyObjectFields.get(i*j+6);
				logger.debug("  [" +i+ "] " + KeyData.getCompressedPublicKey(SignatureData.ED25519SignatureSchemeId, publicKey) + ": " + x500Name + ": " + Base64.getEncoder().encodeToString(x500Name.getBytes()));
			}
		}
		return result;
	}

	private String parseCurrencyAmountByFingerprint(List<Extracted<String, Object>> components, String fingerprint) {
		for (int i = 0; i < components.size(); i++) {
			Extracted<String, Object> item = components.get(i);
			if (item.getValue() == null) {
				continue;
			}
			if (item.getKey().equals(fingerprint)) {
				int j = i + 1;
				String currency = null;
				String value = null;
				do {
					Extracted<String, Object> subItem = components.get(j);
					if (subItem.getKey().equals("net.corda:java.util.Currency")) {
						currency = (String) subItem.getValue();
					} else if (subItem.getKey().equals("net.corda:java.math.BigDecimal")) {
						value = (String) subItem.getValue();
					}
					j++;
				} while (j < components.size() && (currency == null || value == null));
				return value + ":" + currency;
			}
		}
		return null;
	}

	private Integer parseNextIntegerByFingerprint(List<Extracted<String, Object>> components, int index, String fingerprint) {
		Integer value = null;
		for (int i = index; i < components.size(); i++) {
			Extracted<String, Object> item = components.get(i);
			if (item.getValue() == null) {
				continue;
			}
			if (item.getKey().equals(fingerprint)) {
				value = (Integer) item.getValue();
				break;
			}
		}
		return value;
	}

	// Parse transaction component groups from a list of extracted components
	private Map<Integer, List<ComponentGroup>> parseComponentGroups(List<Extracted<String, Object>> components, String groupIndexFingerprint, String groupItemFingerprint, byte[] privacySalt) {
		Map<Integer, List<ComponentGroup>> map = new HashMap<>();
		for (int i = 0; i < components.size(); i++) {
			Extracted<String, Object> item = components.get(i);
			if (item.getValue() == null) {
				continue;
			}
			if (item.getKey().equals(groupItemFingerprint)) {
				byte[] opaque = Utils.fromHexString((String) item.getValue());
				int componentGroupIndex = parseNextIntegerByFingerprint(components, i + 1, groupIndexFingerprint);
				List<ComponentGroup> items = map.getOrDefault(componentGroupIndex, new ArrayList<>());
				int internalIndex = items.size();
				byte[] nonce = componentHash(opaque, privacySalt, componentGroupIndex, internalIndex);
				SecureHash hash = new SecureHash(componentHash(nonce, opaque), SecureHash.SHA_256);
				items.add(new ComponentGroup(opaque, componentGroupIndex, internalIndex, hash));
				map.put(componentGroupIndex, items);
			}
		}
		return map;
	}

	// Parse signatures from a list of extracted components
	private List<SignedData> parseSignatures(List<Extracted<String, Object>> components, String keyFingerprint, String signatureFingerprint, String metaFingerprint, String partialFingerprint) {
		List<SignedData> list = new ArrayList<>();
		SignedData signature = new SignedData();
		for (Extracted<String, Object> item : components) {
			if (item.getValue() == null) {
				continue;
			}
			if (item.getKey().equals(keyFingerprint)) {
				String value = (String) item.getValue();
				signature.setBy(value);
			} else if (item.getKey().equals(signatureFingerprint)) {
				String value = (String) item.getValue();
				signature.setBytes(value);
			} else if (item.getKey().equals(partialFingerprint)) {
				String value = (String) item.getValue();
				signature.setPartialTree(value);
			} else if (item.getKey().equals(metaFingerprint)) {
				Integer value = (Integer) item.getValue();
				if (!signature.hasPlatformVersion()) {
					signature.setPlatformVersion(value);
				} else {
					signature.setSchemaNumber(value);
					list.add(signature);
					signature = new SignedData();
				}
			}
		}
		return list;
	}

	// Computes the hash of each serialised component to be used as Merkle tree leaf. The resultant output (leaf) is calculated using the service's hash algorithm, thus HASH(HASH(nonce || serializedComponent)) for SHA256.
	private byte[] componentHash(byte[] opaque, byte[] privacySalt, int componentGroupIndex, int internalIndex) {
		logger.debug("Computing component hash for group index " + componentGroupIndex + " and internalIndex " + internalIndex);
		return componentHash(computeNonce(privacySalt, componentGroupIndex, internalIndex), opaque);
	}

	// Returns HASH(HASH(nonce || serializedComponent)) for SHA256.
	private byte[] componentHash(byte[] nonce, byte[] opaque) {
		logger.debug("Computing component hash for nonce " + Utils.toHexString(nonce) + " and opaque " + Utils.toHexString(opaque));
		byte[] data = Utils.concatBytes(nonce, opaque);
		return SecureHash.getDoubleHashFor(data, SecureHash.SHA_256).getBytes();
	}

	// Returns HASH(HASH(privacySalt || groupIndex || internalIndex)) for SHA256.
	private byte[] computeNonce(byte[] privacySalt, int groupIndex, int internalIndex) {
		logger.debug("Computing component nonce for group index " + groupIndex + " and internalIndex " + internalIndex);
		byte[] data = Utils.concatBytes(privacySalt, ByteBuffer.allocate(NONCE_SIZE).putInt(groupIndex).putInt(internalIndex).array());
		return SecureHash.getDoubleHashFor(data, SecureHash.SHA_256).getBytes();
	}

	@Data
	public static class EncodedInfo {
		public Uint256 networkId; // The ledger identification.
		public Address crosschainControlContract; // The 160-bit Ethereum address of the cross-chain control contract.
		public Bytes32 eventSig; // The event function signature.
		public DynamicBytes eventData; // The event data.
	}

	@Data
	public static class Signature extends DynamicStruct {
		public Uint256 by; // The 256-bit ECDSA/EDDSA compressed public key as hex-encoded integer.
		public Uint256 sigR; // The 256-bit ECDSA/EDDSA signature's R value as hex-encoded integer.
		public Uint256 sigS; // The 256-bit ECDSA/EDDSA signature's S value as hex-encoded integer.
		public Uint256 sigV; // The 8-bit ECDSA signature's V value as hex-encoded integer.
		public DynamicBytes meta; // The metadata as hex-encoded bytes.

		public DynamicStruct asDynamicStruct()
		{
			return new DynamicStruct(by, sigR, sigS, sigV, meta);
		}
	}

	@Data
	public static class Signatures extends DynamicStruct {
		public Uint256 typ; // For Ethereum SECP256K1 signatures, use 0x0001, and for other signatures, use 0x0003.
		public DynamicArray<Signature> signatures; // The array of signatures.
	}

	@Data
	public static class ComponentData extends DynamicStruct {
		public Uint8 groupIndex;
		public Uint8 internalIndex;
		public DynamicBytes encodedBytes;

		public DynamicStruct asDynamicStruct()
		{
			return new DynamicStruct(groupIndex, internalIndex, encodedBytes);
		}
	}
}
