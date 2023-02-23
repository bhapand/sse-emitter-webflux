package io.kairos.sseemitterwebflux.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@Slf4j
public class MaintenanceController implements DisposableBean {
    private final EmitterProcessor<ServerSentEvent<String>> emitter = EmitterProcessor.create();

    @GetMapping(value = "/pollEmitter", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @CrossOrigin
    public Flux<ServerSentEvent<String>> stream() {
        Flux<ServerSentEvent<String>> flux = sendMessage("");

        return flux.doFinally(signalType -> {
                    if (signalType == SignalType.ON_COMPLETE) {
                        log.info("SSE stream completed");
                    }
                })
                .timeout(Duration.ofMinutes(5))
                .doOnError(error -> log.error("SSE stream error", error))
                .doOnCancel(() -> log.info("SSE stream cancelled"))
                .log();
    }

    public Flux<ServerSentEvent<String>> sendMessage(String message) {
        return Flux.interval(Duration.ofSeconds(1))
                .map(i -> ServerSentEvent.builder(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss"))).build())
                .doOnNext(emitter::onNext);
    }

    @Override
    public void destroy() {
        emitter.onComplete();
    }
}
