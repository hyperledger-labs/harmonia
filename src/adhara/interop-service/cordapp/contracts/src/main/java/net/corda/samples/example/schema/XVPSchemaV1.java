package net.corda.samples.example.schema;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.UUID;

import org.hibernate.annotations.Type;
import javax.annotation.Nullable;

public class XVPSchemaV1 extends MappedSchema {
    public XVPSchemaV1() {
        super(XVPSchema.class, 1, Arrays.asList(PersistentXVP.class));
    }

    @Nullable
    @Override
    public String getMigrationResource() {
        return "xvp.changelog-master";
    }

    @Entity
    @Table(name = "xvp_states")
    public static class PersistentXVP extends PersistentState {
        @Column(name = "linear_id") @Type (type = "uuid-char") private final UUID linearId;
        @Column(name = "trade_id") private final String tradeId;
        @Column(name = "asset_id") private final String assetId;
        @Column(name = "sender") private final String sender;
        @Column(name = "receiver") private final String receiver;
        @Column(name = "status") private final String status;

        public PersistentXVP(UUID linearId, String tradeId, String assetId, String sender, String receiver, String status) {
            this.linearId = linearId;
            this.tradeId = tradeId;
            this.assetId = assetId;
            this.sender = sender;
            this.receiver = receiver;
            this.status = status;
        }

        // Default constructor required by hibernate.
        public PersistentXVP() {
            this.linearId = null;
            this.tradeId = null;
            this.assetId = null;
            this.sender = null;
            this.receiver = null;
            this.status = null;
        }

        public String getSender() {
            return sender;
        }

        public String getReceiver() {
          return receiver;
        }

        public UUID getId() {
            return linearId;
        }

        public String getTradeId() {
            return tradeId;
        }

        public String getAssetId() {
            return assetId;
        }

        public String getStatus() {
            return status;
        }
    }
}
