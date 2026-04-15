package com.opensearch.utility.command.document.ports.inbound;

public interface DocumentBatchEventListenerPort<T> {

    void processEvent(T event);
}
