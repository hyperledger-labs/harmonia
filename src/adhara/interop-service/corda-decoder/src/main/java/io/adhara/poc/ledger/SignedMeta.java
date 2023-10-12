package io.adhara.poc.ledger;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class SignedMeta {
	private Integer platformVersion;
	private Integer schemaNumber;
}
