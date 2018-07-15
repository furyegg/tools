package tools.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@Data
public class ScheduleConfig {
	@Value("${request.producer.schedule.period}")
	private long schedulePeriod;
	
	@Value("${request.producer.schedule.period.unit}")
	private String scheduleTimeUnit;
	
	public TimeUnit getScheduleTimeUnit() {
		try {
			return TimeUnit.valueOf(scheduleTimeUnit);
		} catch (Exception e) {
			String msg = String.format("Unsupported time unit: %s. Supported time units: %s",
					scheduleTimeUnit,
					"MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS");
			throw new IllegalArgumentException(msg);
		}
	}
}