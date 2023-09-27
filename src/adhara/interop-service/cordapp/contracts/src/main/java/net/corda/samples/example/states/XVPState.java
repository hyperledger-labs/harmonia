package net.corda.samples.example.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.samples.example.contracts.XVPContract;
import net.corda.samples.example.schema.XVPSchemaV1;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(XVPContract.class)
public class XVPState implements LinearState, QueryableState {
	private final UniqueIdentifier linearId;
	private final String tradeId;
	private final Party sender;
	private final Party receiver;
	private final String assetId;
	private String status;

	public XVPState(UniqueIdentifier linearId,
	                String tradeId,
	                String assetId,
	                Party sender,
	                Party receiver,
	                String status)
	{
		this.linearId = linearId;
		this.tradeId = tradeId;
		this.assetId = assetId;
		this.sender = sender;
		this.receiver = receiver;
		this.status = status;
	}

	public Party getSender() { return sender; }
	public Party getReceiver() { return receiver; }
	public String getTradeId() { return tradeId; }
	public String getAssetId() { return assetId; }
	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

	@Override public UniqueIdentifier getLinearId() { return linearId; }

	@Override public List<AbstractParty> getParticipants() {
		if (sender.equals(receiver))
			return Arrays.asList(sender);
		return Arrays.asList(sender, receiver);
	}

	@Override public PersistentState generateMappedObject(MappedSchema schema) {
		if (schema instanceof XVPSchemaV1) {
			return new XVPSchemaV1.PersistentXVP(
				this.linearId.getId(),
				this.tradeId,
				this.assetId,
				this.sender.getName().toString(),
				this.receiver.getName().toString(),
				this.status);
		} else {
			throw new IllegalArgumentException("Unrecognised schema $schema");
		}
	}

	@Override public Iterable<MappedSchema> supportedSchemas() {
		return Arrays.asList(new XVPSchemaV1());
	}

	@Override
	public String toString() {
		return String.format("XVPState(linearId=%s, tradeId=%s, assetId=%s, from=%s, to=%s, status=%s)", linearId, tradeId, assetId, sender, receiver, status);
	}
}
