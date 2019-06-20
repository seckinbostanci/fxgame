package com.seckinbostanci;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@EnableKafka
@Component
public class KafkaUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaUtils.class);

    @Value("${app.kafka.partition.number:1}")
    private int kafkaNumPartitions;

    @Value("${app.kafka.replication.factor:1}")
    private short kafkaReplicationFactor;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String kafkaHosts;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private String prefix;

    private AdminClient kafkaAdminClient;

    @PostConstruct
    public void initTopics() {

        prefix = "usdtry_quote";

        if (kafkaAdminClient == null) {
            kafkaAdminClient = AdminClient.create(kafkaAdmin.getConfig());
        }

        ListTopicsResult ltr = kafkaAdminClient.listTopics();
        try {
            Set<String> existingTopicNames = ltr.names().get();
            Collection<NewTopic> newTopics = new ArrayList<NewTopic>();

            if (!existingTopicNames.contains(prefix)) {
                newTopics.add(new NewTopic(prefix, kafkaNumPartitions, kafkaReplicationFactor));
            }

            CreateTopicsResult createTopicsResult = kafkaAdminClient.createTopics(newTopics);
            createTopicsResult.all().get();
            //TODO: remove obsolete topics
        } catch (ExecutionException | InterruptedException e) {
            LOGGER.error("Cannot list existing topics. ", e);
        }
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaHosts.split(",")[0]);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new KafkaTemplate<String, Object>(new DefaultKafkaProducerFactory<String, Object>(props));
    }

    public void produceEvent(StockmarketData stockmarketData) {
        try {
            String realTopic = prefix;
            LOGGER.info("Sending message: {} with topic: {}", stockmarketData, realTopic);
            ListenableFuture<SendResult<String, Object>> res = kafkaTemplate.send(realTopic, stockmarketData);
            res.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Cannot send device events. ", e);
        }
    }

}