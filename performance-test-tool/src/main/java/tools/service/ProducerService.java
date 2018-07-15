package tools.service;

import tools.model.ProducerContext;
import tools.model.ProducerReport;

public interface ProducerService {
	ProducerReport produce(ProducerContext context);
}