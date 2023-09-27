package io.adhara.poc.ledger;

import io.adhara.poc.amqp.codec.AMQPDefinedTypes;
import io.adhara.poc.amqp.codec.DecoderImpl;
import io.adhara.poc.amqp.codec.EncoderImpl;
import io.adhara.poc.amqp.codec.EncodingCodes;
import io.adhara.poc.amqp.types.*;
import io.adhara.poc.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Decoder {
	private static final Logger logger = LoggerFactory.getLogger(Decoder.class);
	private static final String PREFIX_CORDA = "636F726461010000";

	// Peel off AMQP/1.0-encoding layers in a recursive manner and index extracted payloads of type binary, integer and string by AMQP symbol (aka Corda fingerprint).
	// See https://www.corda.net/blog/demystifying-corda-serialisation-format/ and https://training.corda.net/corda-advanced-concepts/oracles/ for a full explanation.
	public static void parseCordaSerialization(String hexEncoded, int level, String prefix, List<Extracted<String, Object>> components) {
		String data = hexEncoded.substring(PREFIX_CORDA.length());
		byte[] bytes = Utils.fromHexString(data);

		int DEFAULT_MAX_BUFFER = 256 * 1024;
		final DecoderImpl decoder = new DecoderImpl();
		final EncoderImpl encoder = new EncoderImpl(decoder);

		AMQPDefinedTypes.registerAllTypes(decoder, encoder);
		ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_MAX_BUFFER);
		buffer.put(bytes);
		buffer.rewind();
		decoder.setByteBuffer(buffer);
		byte code = buffer.get(buffer.position());
		if (code != EncodingCodes.DESCRIBED_TYPE_INDICATOR) {
			logger.error("Expected an element of type [Described Type]");
			return;
		}
		DescribedType describedType = (DescribedType) decoder.readObject(); // Read Envelope
		Object descriptor = describedType.getDescriptor();
		if (!(descriptor instanceof UnsignedLong)) {
			logger.error("Expected an element of type [Unsigned Long]");
			return;
		}
		Object described = describedType.getDescribed();
		if (described instanceof Short) {
			logger.debug("Origin Descriptor: Unsigned long: " + descriptor + ": Described: Short: " + described);
		} else if (described instanceof List) {
			List<?> describedList = (List<?>) described;
			for (Object describedItem : describedList) {
				List<DescribedType> processedList = new ArrayList<>();
				if (describedItem instanceof List) {
					List<?> list = (List<?>) describedItem;
					for (Object listElement : list) {
						if (listElement instanceof DescribedType) {
							processedList.add((DescribedType) listElement);
						}
					}
				} else if (describedItem instanceof DescribedType) {
					processedList.add((DescribedType) describedItem);
				}
				for (DescribedType processedItem : processedList) {
					Object descriptorExtract = processedItem.getDescriptor();
					if (descriptorExtract instanceof UnsignedLong) {
						Object describedExtract = processedItem.getDescribed();
						if (describedExtract instanceof ArrayList) {
							ArrayList<?> describedExtractList = (ArrayList<?>) describedExtract;
							parseListOfDescribed(describedExtractList, level, prefix + "Schema", descriptorExtract.toString(), components);
						} else if (describedExtract instanceof Map) {
							Map<?, ?> describedExtractMap = (Map<?, ?>) describedExtract;
							if (!describedExtractMap.isEmpty()) {
								logger.warn("Unexpected non-empty map in Transform Schema: Found Map of size " + describedExtractMap.size());
								components.add(new Extracted<>(descriptorExtract.toString(), describedExtractMap));
							}
						} else {
							logger.error("Unexpected type in Described: " + describedExtract.getClass());
						}
					} else if (descriptorExtract instanceof Symbol) {
						Object describedExtract = processedItem.getDescribed();
						if (describedExtract instanceof ArrayList) {
							ArrayList<?> describedExtractList = (ArrayList<?>) describedExtract;
							parseListOfDescribed(describedExtractList, level, prefix + "Payload", descriptorExtract.toString(), components);
						} else if (describedExtract instanceof List) { // Always empty
							List<?> describedExtractList = (List<?>) describedExtract;
							if (!describedExtractList.isEmpty()) {
								logger.error("Unexpected non-empty list in Payload: Found List of size " + describedExtractList.size());
								components.add(new Extracted<>(descriptorExtract.toString(), describedExtractList));
							}
						} else {
							logger.error("Unexpected type in Described: " + describedExtract.getClass());
						}
					} else {
						logger.error("Unexpected type in Envelope: " + descriptorExtract.getClass());
					}
				}
			}
		}
		code = buffer.get(buffer.position());
		if (code != EncodingCodes.DESCRIBED_TYPE_INDICATOR) {
			logger.error("Expected an element of type [Described Type]");
		}
		logger.debug(prefix + ":");
	}

	private static void parseDescribed(Object describedItem, int level, String prefix, String fingerprint, List<Extracted<String, Object>> components) {
		String logFingerprint = "";
		if (fingerprint.length() > 0) {
			logFingerprint = ": Fingerprint: " + fingerprint;
		}
		if (describedItem instanceof DescribedType) {
			parseDescribedType((DescribedType) describedItem, level, prefix, components);
		} else if (describedItem instanceof List) {
			parseListOfDescribed((List<?>) describedItem, level, prefix + "--", fingerprint, components);
		} else if (describedItem instanceof String) {
			if (fingerprint.length() > 0) {
				components.add(new Extracted<>(fingerprint, describedItem));
			}
			logger.debug(prefix + logFingerprint + ": Described: String: " + describedItem);
		} else if (describedItem instanceof Boolean) {
			if (fingerprint.length() > 0) {
				components.add(new Extracted<>(fingerprint, describedItem));
			}
			logger.debug(prefix + logFingerprint + ": Described: Boolean: " + describedItem);
		} else if (describedItem instanceof Integer) {
			if (fingerprint.length() > 0) {
				components.add(new Extracted<>(fingerprint, describedItem));
			}
			logger.debug(prefix + logFingerprint + ": Described: Integer: " + describedItem);
		} else if (describedItem instanceof BigInteger) {
			if (fingerprint.length() > 0) {
				components.add(new Extracted<>(fingerprint, describedItem));
			}
			logger.debug(prefix + logFingerprint + ": Described: BigInteger: " + describedItem);
		} else if (describedItem instanceof BigDecimal) {
			if (fingerprint.length() > 0) {
				components.add(new Extracted<>(fingerprint, describedItem));
			}
			logger.debug(prefix + logFingerprint + ": Described: BigDecimal: " + describedItem);
		} else if (describedItem instanceof Long) {
			if (fingerprint.length() > 0) {
				components.add(new Extracted<>(fingerprint, describedItem));
			}
			logger.debug(prefix + logFingerprint + ": Described: Long: " + describedItem);
		} else if (describedItem instanceof UnsignedInteger) {
			if (fingerprint.length() > 0) {
				components.add(new Extracted<>(fingerprint, describedItem));
			}
			logger.debug(prefix + logFingerprint + ": Described: Unsigned Integer: " + describedItem);
		} else if (describedItem instanceof UUID) {
			if (fingerprint.length() > 0) {
				components.add(new Extracted<>(fingerprint, describedItem.toString()));
			}
			logger.debug(prefix + logFingerprint + ": Described: UUID: " + describedItem);
		} else if (describedItem instanceof Binary) {
			String hexEncoded = Utils.toHexString(((Binary) describedItem).getArray());
			if (fingerprint.length() > 0) {
				components.add(new Extracted<>(fingerprint, hexEncoded));
			}
			logger.debug(prefix + logFingerprint + ": Described: Binary: " + hexEncoded);
			if (hexEncoded.startsWith(PREFIX_CORDA)) {
				parseCordaSerialization(hexEncoded, level + 1, prefix + "--", components);
			}
		} else if (describedItem instanceof Symbol) {
			logger.debug(prefix + logFingerprint + ": Described: Symbol: " + describedItem);
		} else {
			if (describedItem != null) {
				logger.debug(prefix + logFingerprint + ": Described: Unknown: Class: " + describedItem.getClass());
			} else {
				if (fingerprint.length() > 0) {
					components.add(new Extracted<>(fingerprint, null));
				}
				logger.debug(prefix + logFingerprint + ": Described: Null");
			}
		}
	}

	private static void parseDescribedType(DescribedType item, int level, String prefix, List<Extracted<String, Object>> components) {
		Object descriptor = item.getDescriptor();
		if (descriptor instanceof UnsignedLong) {
			Object describedExtract = ((DescribedType) item).getDescribed();
			parseDescribed(describedExtract, level, prefix, "", components);
		} else if (descriptor instanceof Symbol) {
			String fingerprint = descriptor.toString();
			Object describedExtract = ((DescribedType) item).getDescribed();
			parseDescribed(describedExtract, level, prefix, fingerprint, components);
		} else {
			logger.debug(prefix + ": Descriptor: Unknown: " + descriptor.getClass());
		}
	}

	private static void parseListOfDescribed(List<?> list, int level, String prefix, String fingerprint, List<Extracted<String, Object>> components) {
		for (Object describedItem : list) {
			parseDescribed(describedItem, level, prefix, fingerprint, components);
		}
	}
}
