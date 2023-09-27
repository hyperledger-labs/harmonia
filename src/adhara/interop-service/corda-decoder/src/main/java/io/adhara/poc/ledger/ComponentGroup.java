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
@NoArgsConstructor
public class ComponentGroup {
	private static final Logger logger = LoggerFactory.getLogger(ComponentGroup.class);

	private byte[] opaqueBytes;
	private int groupIndex;
	private int internalIndex;
	private SecureHash hash;


}
