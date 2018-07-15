package tools.model;

import lombok.Data;
import tools.producer.Producer;

@Data
public class ProducerContext {
	private Producer producer;
	private long sendPeriod;
	private long sendTimeUnit;
	private long totalCount;
}