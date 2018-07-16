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
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
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
		Instant start = Instant.now();
		log.info("start to produce requests...");
		ProducerReport report = new ProducerReport();
		int batchSize = getBatchSize(context);
		ProducerFeature pf = new ProducerFeature();
		
		Runnable action = () -> {
			Instant batchStart = Instant.now();
			CompletableFuture<Long> cf = pf.getCompletableFuture();
			
			Long totalCount = report.getTotalCount();
			if (totalCount >= context.getTotalCount()) {
				cf.complete(totalCount);
			}
			
			CompletableFuture<Long> newCF;
			if (batchSize > 1) {
				newCF = StreamEx.generate(() -> 1)
						.limit(batchSize)
						.parallel()
						.map(n -> CompletableFuture.supplyAsync(() -> context.getProducer().produce() ? 1L : 0L))
						.foldLeft(cf, (acc, item) -> acc.thenComposeAsync(n -> item).thenApply(count -> {
							report.updateTotalCount(count);
							printProgress(batchSize, batchStart, Instant.now());
							return count;
						}));
			} else {
				newCF = cf.thenComposeAsync(n -> CompletableFuture.supplyAsync(() -> context.getProducer().produce() ? 1L : 0L)
						.thenApply(count -> {
							report.updateTotalCount(count);
							printProgress(batchSize, batchStart, Instant.now());
							return count;
						}));
			}
			pf.setCompletableFuture(cf.thenCompose(n -> newCF));
		};
		
		ScheduledFuture<?> sf = executor.scheduleAtFixedRate(
				action,
				0,
				scheduleConfig.getSchedulePeriod(),
				scheduleConfig.getScheduleTimeUnit());
		
		pf.getCompletableFuture().whenComplete((n, e) -> {
			sf.cancel(true);
		});
		
		pf.getCompletableFuture().join();
		log.info("end to produce requests, cost: {}", Duration.between(start, Instant.now()));
		return report;
	}
	
	private void printProgress(int batchSize, Instant start, Instant end) {
		System.out.println(String.format("produced %d request in %d %s", batchSize, Duration.between(start, end)));
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
		executor.shutdownNow();
	}
}