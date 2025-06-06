package com.apelisser.algasensors.temperature.processing.api.controller;

import com.apelisser.algasensors.temperature.processing.common.IdGenerator;
import com.apelisser.algasensors.temperature.processing.api.model.TemperatureLogOutput;
import com.apelisser.algasensors.temperature.processing.infrastructure.rabbitmq.RabbitMQConfig;
import io.hypersistence.tsid.TSID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

@Slf4j
@RestController
@RequestMapping("/api/sensors/{sensorId}/temperatures/data")
public class TemperatureProcessingController {

    private final RabbitTemplate rabbitTemplate;

    public TemperatureProcessingController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public void process(@PathVariable TSID sensorId, @RequestBody String input) {
        if (input == null || input.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        double temperature;

        try {
            temperature = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        TemperatureLogOutput logOutput = TemperatureLogOutput.builder()
            .id(IdGenerator.generateTimeBasedUUID())
            .sensorId(sensorId)
            .value(temperature)
            .registeredAt(OffsetDateTime.now())
            .build();

        log.info(logOutput.toString());

        String exchange = RabbitMQConfig.FANOUT_EXCHANGE_NAME;
        String routingKey = "";
        Object payload = logOutput;

        rabbitTemplate.convertAndSend(exchange, routingKey, payload);
    }

}
