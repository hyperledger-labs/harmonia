package net.corda.samples.example.schema;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.UUID;
//4.6 changes
import org.hibernate.annotations.Type;
import javax.annotation.Nullable;

public class DCRSchemaV1 extends MappedSchema {
    public DCRSchemaV1() {
        super(DCRSchema.class, 1, Arrays.asList(PersistentDCR.class));
    }

    @Nullable
    @Override
    public String getMigrationResource() {
        return "dcr.changelog-master";
    }

    @Entity
    @Table(name = "dcr_states")
    public static class PersistentDCR extends PersistentState {
        @Column(name = "owner") private final String owner;
        @Column(name = "issuer") private final String issuer;
        @Column(name = "value") private final String value;
        @Column(name = "currency") private final String currency;
        @Column(name = "linear_id") @Type (type = "uuid-char") private final UUID linearId;
        @Column(name = "trade_id") private final String tradeId;
        @Column(name = "proof") private final String proof;
        @Column(name = "status") private final String status;

        public PersistentDCR(String owner, String issuer, String value, String currency, UUID linearId, String tradeId, String proof, String status) {
            this.owner = owner;
            this.issuer = issuer;
            this.value = value;
            this.currency = currency;
            this.linearId = linearId;
            this.tradeId = tradeId;
            this.proof = proof;
            this.status = status;
        }

        // Default constructor required by hibernate.
        public PersistentDCR() {
            this.owner = null;
            this.issuer = null;
            this.value = null;
            this.currency = null;
            this.linearId = null;
            this.tradeId = null;
            this.proof = null;
            this.status = null;
        }

        public String getOwner() {
            return owner;
        }

        public String getIssuer() {
            return issuer;
        }

        public String getValue() {
            return value;
        }

        public String getCurrency() {
            return currency;
        }

        public UUID getId() {
            return linearId;
        }

        public String getTradeId() {
            return tradeId;
        }

        public String getProof() {
            return proof;
        }

        public String getStatus() {
            return status;
        }
    }
}