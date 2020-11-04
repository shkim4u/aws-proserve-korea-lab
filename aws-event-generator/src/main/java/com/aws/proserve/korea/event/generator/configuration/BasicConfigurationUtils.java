package com.aws.proserve.korea.event.generator.configuration;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.configuration.PropertiesConfiguration;
//import org.apache.log4j.Logger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BasicConfigurationUtils {

//	static Logger logger = Logger.getLogger(BasicConfigurationUtils.class);
	static Logger logger = LoggerFactory.getLogger(BasicConfigurationUtils.class);
	
	public static void setSortedProperties(
		PropertiesConfiguration propertiesConfiguration,
		Properties properties,
		boolean sort
	) {
		if (propertiesConfiguration != null &&
			properties != null
		) {
			Enumeration<Object> keysEnum = properties.keys();
			
			Vector<String> keyList = new Vector<String>();
			while (keysEnum.hasMoreElements()) {
				keyList.add((String)keysEnum.nextElement());
			}
			
			if (sort) {
				// Sort.
				Collections.sort(keyList);
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("\n<<<<<<<<<< Parameters >>>>>>>>>>");

			// Set properties to configuration object.
			for (Iterator<String> iter = keyList.iterator(); iter.hasNext(); ) {
				String key = iter.next();
				String value = properties.getProperty(key);
				sb.append("\n\t* " + key + " = " + value);
				
				propertiesConfiguration.setProperty(key, value);
			}
			
			logger.info(sb.toString());
		}		
	}

}
