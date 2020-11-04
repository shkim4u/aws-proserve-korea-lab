package com.aws.proserve.korea.event.generator;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class AwsEventGeneratorVersionInfo {

	private static String version = null;

	public static String getProgramShortName() {
		return "Event Generator";
	}
	
	public static String getProgramLongName() {
		return "Device Event Generator";
	}
	
	public static String getVersion() {
		if (version != null) return version;
		
		JarFile jarfile = null;
		version = "undefined";
		try {
			jarfile = new JarFile(
				"etokensetup.jar"
			);
			Manifest manifest = jarfile.getManifest();
			Attributes attributes = manifest.getMainAttributes();
			
			version = attributes.getValue("Implementation-Version") +
				" (" + attributes.getValue("Built-Date") + ")";
		} catch (IOException e) {
			// Read from "build.num" file.
			Properties props = new Properties();
			URL url = ClassLoader.getSystemResource("build.num");
			if (url != null) {
				try {
					props.load(url.openStream());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
	
				version = "1.0.0" + "-b" + props.getProperty("build.number");
			} else {
				version = "1.0.0" + "-b" + "?";
			}
		} finally {
			if (jarfile != null) {
				try {
					jarfile.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		// Try again for exception.
		if (version == null &&
			(version = AwsEventGeneratorVersionInfo.class.getClass().getPackage().getImplementationVersion()) == null
		) {
			version = "[Debug Version]";
		}
		
		return version;
	}
}
