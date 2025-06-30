package org.epos.dbconnector.util;

import org.epos.dbconnector.Configuration;
import org.epos.dbconnector.Configurations;

import java.util.ArrayList;
import java.util.List;

public class MappingToAndFromCommonBean {

	public static Configuration map(Configurations pu) {
		if (pu == null) return null;
        Configuration e = new Configuration(pu.getId(), pu.getConfiguration());
		return e;
	}

	public static List<Configuration> map(List<Configurations> pu) {
		if (pu == null) return null;
		List<Configuration> configurations = new ArrayList<Configuration>();
		for(Configurations c : pu) {
			Configuration e = new Configuration(c.getId(), c.getConfiguration());
			configurations.add(e);
		}
		return configurations;
	}

}
