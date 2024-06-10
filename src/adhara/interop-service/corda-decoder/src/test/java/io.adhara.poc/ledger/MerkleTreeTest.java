package io.adhara.poc.ledger;

import org.junit.Test;
import org.junit.BeforeClass;
import org.web3j.utils.Numeric;

import java.util.Arrays;
import java.util.List;

public class MerkleTreeTest {

	List<SecureHash> txLeaves =
		Arrays.asList(
			new SecureHash(Numeric.hexStringToByteArray("0xA1CC0888DB4A7929C3C8089E5EB93E0207EE500C1DA49DF621DFD4671EFE67F0"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x9BE638965BFD84229960AF602AA414C548A08AEECCFDB8F3BDFF70439976D36E"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0BC1C1F77397E822E622C079A5DA52C5269C62919A35DB9E5F14F47E13F21FF3"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xD3BA49D2572B288C03EC26B2DE83FEFA89EDEB2964C3D3D6F50272212C144F4B"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x823576854AE3575BAD3F36CF6BE562C27C7E6E83F7F37E0CA3063C6C2F73B52E"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x9C1C3793A00629405E7660BEA3D2D62F65162A6A16E2A88B91E89E6F9D8391AD"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xB2FA4970666B850C1234ED618CC5E8A042C5C0C049048DD0797914AFEAEA924F"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256)
		);

	SecureHash root = new SecureHash(Numeric.hexStringToByteArray("3E1DE1CB6F3BF92BD1E427C0AED40DECE749BCAC5BC1227E301D81621E2546FB"), SecureHash.SHA_256);

	List<SecureHash> txLeaves2 =
		Arrays.asList(
			new SecureHash(Numeric.hexStringToByteArray("0x2F248BBBE40D5BC1EFCF85CAF7EA0C226006CD4CED5461277CC0060F44DAF773"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x60601A16F49D0C6E8BD14415F466F8BD4B0981F58A15BDB06A1FA1E3DC49553F"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x5DB25DB06EACEEE9A49228A902C2C1BA0910AECD149E8F1DF7FC6D368A85903F"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xDDC9A03FD435B5F2DD529315B28F2FD48F10CEF8F40D9584F9F20E5C1C56AD2B"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xD610601AB516421FF10B2EA9C2E5CBAFE46224BCCCF1A70BA8E05415E26745B3"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x3BCE67FBCD315211CC82B6A93B21AFFD925FA418C9989437A768A4B581DE04EB"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xBCA23985B853901B98F0C324D96D21AA5D27441B19570E24F9646E828B1F42B4"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x57AE3AF22AB50A6723E0F56C4954E744215F1824515CF8CB0A620D9B10A0DA92"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x8FA1FA34541EB9CA3781A96874D5F5F59E561F0A06265F8742B095B4032AC818"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x0000000000000000000000000000000000000000000000000000000000000000"), SecureHash.SHA_256)
		);

	SecureHash root2 = new SecureHash(Numeric.hexStringToByteArray("AA7AF2ABDAA2E37785B23AEC36F73A3280FF1326595D0F976532FF8246D8536C"), SecureHash.SHA_256);

	@BeforeClass
	public static void setup() throws Exception {
	}

	@Test
	public void multiMerkleProofWithOneLeaf() throws Exception {
		MerkleTree txTree = MerkleTree.getMerkleTree(txLeaves2);
		assert txTree != null;

		MerkleProof result = MerkleTree.generateMultiProof(txLeaves2,	Arrays.asList(
			new SecureHash(Numeric.hexStringToByteArray("0x60601A16F49D0C6E8BD14415F466F8BD4B0981F58A15BDB06A1FA1E3DC49553F"), SecureHash.SHA_256)
		));
		boolean verifiedResult = MerkleTree.verifyMultiProof(root2, result.getProof(), result.getFlags(), result.getLeaves());
		assert verifiedResult;
		PartialMerkleTree parTree = PartialMerkleTree.build(txTree, Arrays.asList(
			new SecureHash(Numeric.hexStringToByteArray("0x60601A16F49D0C6E8BD14415F466F8BD4B0981F58A15BDB06A1FA1E3DC49553F"), SecureHash.SHA_256)
		));
		MerkleProof proof = parTree.generateMultiProof();
		boolean verifiedProof = MerkleTree.verifyMultiProof(root2, proof.getProof(), proof.getFlags(), proof.getLeaves());
		assert verifiedProof;

		assert result.equals(proof);
	}

	@Test
	public void multiMerkleProofWithTwoLeaves() throws Exception {
		MerkleTree txTree = MerkleTree.getMerkleTree(txLeaves2);
		assert txTree != null;

		MerkleProof result = MerkleTree.generateMultiProof(txLeaves2,	Arrays.asList(
			new SecureHash(Numeric.hexStringToByteArray("0xD610601AB516421FF10B2EA9C2E5CBAFE46224BCCCF1A70BA8E05415E26745B3"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x3BCE67FBCD315211CC82B6A93B21AFFD925FA418C9989437A768A4B581DE04EB"), SecureHash.SHA_256)
		));
		boolean verifiedResult = MerkleTree.verifyMultiProof(root2, result.getProof(), result.getFlags(), result.getLeaves());
		assert verifiedResult;
		PartialMerkleTree parTree = PartialMerkleTree.build(txTree, Arrays.asList(
			new SecureHash(Numeric.hexStringToByteArray("0xD610601AB516421FF10B2EA9C2E5CBAFE46224BCCCF1A70BA8E05415E26745B3"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x3BCE67FBCD315211CC82B6A93B21AFFD925FA418C9989437A768A4B581DE04EB"), SecureHash.SHA_256)
		));
		MerkleProof proof = parTree.generateMultiProof();
		boolean verifiedProof = MerkleTree.verifyMultiProof(root2, proof.getProof(), proof.getFlags(), proof.getLeaves());
		assert verifiedProof;

		assert result.equals(proof);
	}


	@Test
	public void multiMerkleProofWithJoinedLeaves() throws Exception {
		MerkleTree txTree = MerkleTree.getMerkleTree(txLeaves);
		assert txTree != null;

		MerkleProof result = MerkleTree.generateMultiProof(txLeaves,	Arrays.asList(
			new SecureHash(Numeric.hexStringToByteArray("0x0BC1C1F77397E822E622C079A5DA52C5269C62919A35DB9E5F14F47E13F21FF3"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xD3BA49D2572B288C03EC26B2DE83FEFA89EDEB2964C3D3D6F50272212C144F4B"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x9C1C3793A00629405E7660BEA3D2D62F65162A6A16E2A88B91E89E6F9D8391AD"), SecureHash.SHA_256)
		));
		boolean verifiedResult = MerkleTree.verifyMultiProof(root, result.getProof(), result.getFlags(), result.getLeaves());
		assert verifiedResult;
		PartialMerkleTree parTree = PartialMerkleTree.build(txTree, Arrays.asList(
			new SecureHash(Numeric.hexStringToByteArray("0x0BC1C1F77397E822E622C079A5DA52C5269C62919A35DB9E5F14F47E13F21FF3"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xD3BA49D2572B288C03EC26B2DE83FEFA89EDEB2964C3D3D6F50272212C144F4B"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x9C1C3793A00629405E7660BEA3D2D62F65162A6A16E2A88B91E89E6F9D8391AD"), SecureHash.SHA_256)
		));
		MerkleProof proof = parTree.generateMultiProof();
		boolean verifiedProof = MerkleTree.verifyMultiProof(root, proof.getProof(), proof.getFlags(), proof.getLeaves());
		assert verifiedProof;

		assert result.equals(proof);
	}

	@Test
	public void multiMerkleProofWithSeparateLeaves() throws Exception {

		MerkleTree txTree = MerkleTree.getMerkleTree(txLeaves);
		assert txTree != null;

		MerkleProof result = MerkleTree.generateMultiProof(txLeaves,	Arrays.asList(
			new SecureHash(Numeric.hexStringToByteArray("0x0BC1C1F77397E822E622C079A5DA52C5269C62919A35DB9E5F14F47E13F21FF3"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x823576854AE3575BAD3F36CF6BE562C27C7E6E83F7F37E0CA3063C6C2F73B52E"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xB2FA4970666B850C1234ED618CC5E8A042C5C0C049048DD0797914AFEAEA924F"), SecureHash.SHA_256)
		));
		boolean verifiedResult = MerkleTree.verifyMultiProof(root, result.getProof(), result.getFlags(), result.getLeaves());
		assert verifiedResult;

		PartialMerkleTree parTree = PartialMerkleTree.build(txTree, Arrays.asList(
			new SecureHash(Numeric.hexStringToByteArray("0x0BC1C1F77397E822E622C079A5DA52C5269C62919A35DB9E5F14F47E13F21FF3"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0x823576854AE3575BAD3F36CF6BE562C27C7E6E83F7F37E0CA3063C6C2F73B52E"), SecureHash.SHA_256),
			new SecureHash(Numeric.hexStringToByteArray("0xB2FA4970666B850C1234ED618CC5E8A042C5C0C049048DD0797914AFEAEA924F"), SecureHash.SHA_256)
		));
		MerkleProof proof = parTree.generateMultiProof();
		boolean verifiedProof = MerkleTree.verifyMultiProof(root, proof.getProof(), proof.getFlags(), proof.getLeaves());
		assert verifiedProof;

		assert result.equals(proof);
	}
}