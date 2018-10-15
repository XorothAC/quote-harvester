package com.finplant.cryptoharvester;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Settings {
	private static final Logger LOG = LoggerFactory.getLogger(Settings.class);
	
	@JsonProperty("db")
	private Map<String, String> db;
	@JsonProperty("flush_period_s")
	private int flush_period_s;
	@JsonProperty("instruments")
	private List<Instrument> instruments;
	
	public Map<String, String> getDb() {
		return db;
	}

	public int getFlushPeriodS() {
		return flush_period_s;
	}

	public List<Instrument> getInstruments() {
		return instruments;
	}

	public static Settings readYaml() {
	    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
	    File file = new File(Paths.get(".").toAbsolutePath().normalize().toString()+"/settings.yml");
	    LOG.info("YAML settings file: "+file.toString());
	    try {
			return mapper.readValue(file, Settings.class);
		} catch (Exception e) {
			ErrorHandler.logError("YAML settings file error: ", e);
		}
	    return null;
	}
	
	static class Instrument {
		private String name;
		private String instrument;
		private List<String> depends;
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getInstrument() {
			return instrument;
		}
		
		public void setInstrument(String instrument) {
			this.instrument = instrument;
		}
		
		public List<String> getDepends() {
			return depends;
		}
		
		public void setDepends(List<String> depends) {
			this.depends = depends;
		}
	}
}