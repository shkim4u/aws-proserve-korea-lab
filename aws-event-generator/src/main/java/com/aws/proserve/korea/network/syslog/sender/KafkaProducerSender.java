package com.aws.proserve.korea.network.syslog.sender;

public class KafkaProducerSender extends AbstractKafkaSender {

	@Override
	public void sendMessage(String topic, String key, String value) {
//		final ProducerRecord<String, String> record = new ProducerRecord<String, String>(
//				topic,
//				key,
//				revalue
//			);
//			kafkaProducer.send(
//				record,
//				(metadata, exception) -> {
//	                if (metadata != null){
//	                    System.out.println("Record: -> "+ record.key()+" | "+ record.value());
//	                }
//	                else{
//	                    System.out.println("Error Sending Record -> " + record.value());
//	                }
//				}
//			);
	}

}
