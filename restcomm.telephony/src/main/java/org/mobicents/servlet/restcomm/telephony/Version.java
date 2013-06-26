package org.mobicents.servlet.restcomm.telephony;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {

	private String restcomm_version;

	private static Version instance;

	private Version() {
		Properties configProp = new Properties();
		InputStream in = this.getClass().getResourceAsStream("org.mobicents.servlet.restcomm.telephony");
        try {
            configProp.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
		this.restcomm_version = configProp.getProperty("restcomm.version");
	}

	public static Version getInstance(){
		if(instance == null)
			instance = new Version();
		return instance;
	}

	public String getRestcomm_version() {
		return restcomm_version;
	}
}