package pe.crediya.solicitudes.sqs.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import pe.crediya.solicitudes.model.capacidad.Capacidad;
import pe.crediya.solicitudes.model.capacidad.gateways.CapacidadRepository;
import pe.crediya.solicitudes.model.estadocambiadoevent.EstadoCambiadoEvent;
import pe.crediya.solicitudes.model.estadocambiadoevent.gateways.EstadoCambiadoEventRepository;
import pe.crediya.solicitudes.sqs.sender.config.SQSSenderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log4j2
@RequiredArgsConstructor
public class SQSSender implements EstadoCambiadoEventRepository, CapacidadRepository {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    private final ObjectMapper mapper;

    public Mono<String> send(String message) {
        return Mono.fromCallable(() -> buildRequest(message))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.debug("Message sent {}", response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }

    @Override
    public Mono<Void> publicarEstadoCambiado(EstadoCambiadoEvent event) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(event))
                .map(body -> SendMessageRequest.builder()
                        .queueUrl(properties.queueUrl())
                        .messageBody(body)
                        .build())
                .flatMap(req -> Mono.fromFuture(client.sendMessage(req)))
                .doOnNext(res -> log.debug("sqs_msg_sent id={}",res.messageId()))
                .then();
    }

    @Override
    public Mono<Void> enviarEvaluacion(Capacidad capacidad) {
        return Mono.fromCallable(() -> mapper.writeValueAsString(capacidad))
                .map(body -> SendMessageRequest.builder()
                        .queueUrl(properties.capacidadQueueUrl())
                        .messageBody(body)
                        .build())
                .flatMap(req -> Mono.fromFuture(() -> client.sendMessage(req)))
                .doOnNext(res -> log.debug("Capacidad enviada a SQS id={}", res.messageId()))
                .then();
    }
}
