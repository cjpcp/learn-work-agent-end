package com.example.learnworkagent.domain.approval.handler;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BusinessTypeHandlerFactory {

    private final Map<String, BusinessTypeHandler> handlers;

    public BusinessTypeHandlerFactory(List<BusinessTypeHandler> handlerList) {
        this.handlers = handlerList.stream()
                .collect(Collectors.toMap(
                        BusinessTypeHandler::getBusinessType,
                        Function.identity()
                ));
    }

    public BusinessTypeHandler getHandler(String businessType) {
        return handlers.get(businessType);
    }

    public Collection<BusinessTypeHandler> getAllHandlers() {
        return handlers.values();
    }
}