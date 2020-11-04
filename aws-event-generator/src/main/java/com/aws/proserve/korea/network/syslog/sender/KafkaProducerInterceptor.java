package com.aws.proserve.korea.network.syslog.sender;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Segment;

public class KafkaProducerInterceptor implements ProducerInterceptor<String, String> {

	@Override
	public void configure(Map<String, ?> configs) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ProducerRecord<String, String> onSend(ProducerRecord<String, String> record) {
		AWSXRay.createSubsegment("# AwsEventGenerator onSend interceptor", (a) -> {
			// Get current trace and parent ID.
			Segment segment = a.getParentSegment();
			String traceId = segment.getTraceId().toString();
			String parentId = segment.getParentId();
			
			// Add trace information to the header.
			try {
				Headers headers = record.headers();
				
				if (traceId != null) {
					headers.add("AWS_XRAY_TRACE_ID", traceId.getBytes("utf-8"));
				}
				
				// Add parent ID as its ID.
				headers.add("AWS_XRAY_PARENT_ID", a.getId().getBytes("utf-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			
		});
		
		
		return record;
	}

	@Override
	public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}
	
}
