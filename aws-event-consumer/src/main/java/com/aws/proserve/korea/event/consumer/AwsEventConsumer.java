package com.aws.proserve.korea.event.consumer;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.TraceID;

//import lombok.extern.slf4j.Slf4j;
//import lombok.Getter;

//@Slf4j
public class AwsEventConsumer {
	
	private static Logger logger = LoggerFactory.getLogger(AwsEventConsumer.class);
	
	public static void main(String[] args) {
		new AwsEventConsumer().consume();
	}
	

	private Properties init() {
		// [2020-09-27] KSH: Read from config or properties file.
        Properties props = new Properties();

//        props.put("bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094");
//        props.put("group.id", "yoon-consumer");
//        props.put("enable.auto.commit", "true");
//        props.put("auto.offset.reset", "latest");
//        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
//        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "shkim4u-consumer");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        
        return props;
	}

	private void consume() {
		// Try with resource of KafkaConsumer.
		try (
			KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(
				init()
			);
		) {
			// 토픽리스트를 매개변수로 준다. 구독신청을 한다.
			consumer.subscribe(Arrays.asList("aws-kafka-demo-01"));
			while (true) {
				// 컨슈머는 토픽에 계속 폴링하고 있어야한다. 아니면 브로커는 컨슈머가 죽었다고 판단하고, 해당 파티션을 다른 컨슈머에게 넘긴다.
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
				for (ConsumerRecord<String, String> record : records) {
					// Tracing with XRay.
					AWSXRay.createSegment(
						"AwsEventConsumer consume",
						(a) -> {
							a.setNamespace("AWS::XRay::EventConsumer");
							
							// Retrieve trace information from the record.
							Headers headers = record.headers();
							String traceId = null;
							String parentId = null;
							try {
								Header header = headers.lastHeader("AWS_XRAY_TRACE_ID");
								if (header != null) {
									traceId = new String(header.value(), "utf-8");
								}
								
								header = headers.lastHeader("AWS_XRAY_PARENT_ID");
								if (header != null) {
									parentId = new String(header.value(), "utf-8");
								}
								
							} catch (UnsupportedEncodingException e) {
								throw new RuntimeException(e);
							}
							
							if (traceId != null) {
								a.setTraceId(TraceID.fromString(traceId));
							}
							if (parentId != null) {
								a.setParentId(parentId);
							}
							
							// Do something here.
							logger.info(
								"Topic: {}, Partition: {}, Offset: {}, Key: {}, Value: {}\n",
								record.topic(), record.partition(), record.offset(), record.key(), record.value()
							);

						}
					);
				}
				
				// 수동으로 커밋하여 메시지를 가져온 것으로 간주하는 시점을 자유롭게 조정할 수있다.
//				consumer.commitSync();
			}
		} finally {
		}
	}


}
