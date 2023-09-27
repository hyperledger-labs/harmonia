package net.corda.samples.example.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.samples.example.contracts.DCRContract;
import net.corda.samples.example.schema.DCRSchemaV1;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(DCRContract.class)
public class DCRState implements LinearState, QueryableState {
    private final String value;
    private final String currency;
    private final Party issuer;
    private Party owner;
    private final UniqueIdentifier linearId;
    private String tradeId;
    private String status;
    private String proof;

    public DCRState(String value,
                    String currency,
                    Party owner,
                    Party issuer,
                    UniqueIdentifier linearId,
                    String tradeId,
                    String proof,
                    String status)
    {
        this.value = value;
        this.currency = currency;
        this.owner = owner;
        this.issuer = issuer;
        this.linearId = linearId;
        this.tradeId = tradeId;
        this.proof = proof;
        this.status = status;
    }

    public String getValue() { return value; }
    public String getCurrency() { return currency; }
    public Party getOwner() { return owner; }
    public void setOwner(Party owner) { this.owner = owner; }
    public Party getIssuer() { return issuer; }
    public String getTradeId() { return tradeId; }
    public void setTradeId(String tradeId) { this.tradeId = tradeId; }
    public String getProof() { return proof; }
    public void setProof(String proof) { this.proof = proof; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override public UniqueIdentifier getLinearId() { return linearId; }

    @Override public List<AbstractParty> getParticipants() {
        if (issuer.equals(owner))
          return Arrays.asList(owner);
        return Arrays.asList(owner, issuer);
    }

    @Override public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof DCRSchemaV1) {
            return new DCRSchemaV1.PersistentDCR(
                    this.owner.getName().toString(),
                    this.issuer.getName().toString(),
                    this.value,
                    this.currency,
                    this.linearId.getId(),
                    this.tradeId,
                    this.proof,
                    this.status);
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return Arrays.asList(new DCRSchemaV1());
    }

    @Override
    public String toString() {
        return String.format("DCRState(linearId=%s, value=%s, currency=%s, owner=%s, issuer=%s, tradeId=%s, proof=%s, status=%s)", linearId, value, currency, owner, issuer, tradeId, proof != null ? "true" : "false", status);
    }
}
