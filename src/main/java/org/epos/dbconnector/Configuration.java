package org.epos.dbconnector;

public class Configuration {
	
	private String id;
	private String configuration;
	
	public Configuration() {
		super();
	}
	
	public Configuration(String id, String configuration) {
		super();
		this.id = id;
		this.configuration = configuration;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getConfiguration() {
		return configuration;
	}

	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}
	
	

}
