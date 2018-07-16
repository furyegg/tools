package tools.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.producer.Producer;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProducerContext {
	private Producer producer;
	private long sendPeriod;
	private long sendTimeUnit;
	private long totalCount;
}