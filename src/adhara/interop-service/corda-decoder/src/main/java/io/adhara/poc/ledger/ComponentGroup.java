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
public class ComponentGroup {
	private final byte[] opaqueBytes;
	private final int groupIndex;
	private final int internalIndex;
	private final SecureHash hash;
}
