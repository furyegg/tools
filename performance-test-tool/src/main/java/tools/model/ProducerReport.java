package tools.model;

import lombok.Data;

@Data
public class ProducerReport {
	private long totalCount = 0;
	
	public void updateTotalCount(long count) {
		totalCount += count;
	}
}