package tools.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.config.ScheduleConfig;
import tools.model.ProducerContext;
import tools.model.ProducerReport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

@Service
@Slf4j
public class ProducerServiceImpl implements ProducerService {
	private ScheduledExecutorService executor;
	
	@Autowired
	private ScheduleConfig scheduleConfig;
	
	@Data
	private static class ProducerFeature {
		private CompletableFuture<Long> completableFuture;
		
		public ProducerFeature() {
			this.completableFuture = new CompletableFuture<>();
		}
	}
	
	@Override
	public ProducerReport produce(ProducerContext context) {
		ProducerReport.ProducerReportBuilder builder = ProducerReport.builder();
		int batchSize = getBatchSize(context);
		ProducerFeature pf = new ProducerFeature();
		
		Runnable action = () -> {
			CompletableFuture<Long> cf = pf.getCompletableFuture();
			
			try {
				Long count = cf.get();
				if (count >= context.getTotalCount()) {
					cf.complete(count);
				}
			} catch (InterruptedException | ExecutionException e) {
				// TODO:....
			}
			
			if (batchSize > 1) {
				StreamEx.generate(() -> 1)
						.limit(batchSize)
						.parallel()
						.map(n -> CompletableFuture.supplyAsync(() -> context.getProducer().produce() ? 1 : 0))
						.forEach(pcf -> cf.thenCombineAsync(pcf, (n1, n2) -> n1 + n2));
			} else {
				cf.thenCombineAsync(
						CompletableFuture.supplyAsync(() -> context.getProducer().produce() ? 1 : 0),
						(n1, n2) -> n1 + n2);
			}
		};
		
		ScheduledFuture<?> sf = executor.scheduleAtFixedRate(
				action,
				0,
				scheduleConfig.getSchedulePeriod(),
				scheduleConfig.getScheduleTimeUnit());
		
		pf.getCompletableFuture().whenComplete(
				(n, e) -> builder.totalCount(n));
		
		return builder.build();
	}
	
	private int getBatchSize(ProducerContext context) {
		if (context.getSendPeriod() < scheduleConfig.getSchedulePeriod()) {
			return Double.valueOf(Math.ceil(1.0 * context.getTotalCount() / context.getSendPeriod())).intValue();
		}
		return 1;
	}
	
	@PostConstruct
	private void init() {
		log.info("PostConstruct");
		executor = Executors.newScheduledThreadPool(2);
	}
	
	@PreDestroy
	private void destroy() {
		log.info("PreDestroy");
	}
}