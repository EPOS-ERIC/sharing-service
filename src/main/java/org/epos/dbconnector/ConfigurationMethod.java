package org.epos.dbconnector;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.EntityManager;

import org.epos.dbconnector.service.DBService;
import org.epos.dbconnector.util.MappingToAndFromCommonBean;

import static org.epos.dbconnector.util.DBUtil.getFromDB;
import static org.epos.dbconnector.util.DBUtil.getOneFromDB;

public class ConfigurationMethod {
	
	private static final DBService dbService = new DBService();

	public static Configuration getConfigurationById(String id) {
		EntityManager em = dbService.getEntityManager();
		Configurations fromDB = getOneFromDB(em,
				Configurations.class,
				"configurations.findById",
				"ID", id);
		em.close();

		return fromDB != null ? MappingToAndFromCommonBean.map(fromDB) : null;
	}

	public static List<Configuration> getConfigurations() {
		EntityManager em = dbService.getEntityManager();
		List<Configurations> fromDB = getFromDB(em,
				Configurations.class,
				"configurations.findAll");
		em.close();

		return fromDB != null ? MappingToAndFromCommonBean.map(fromDB) : null;
	}

	public static void saveConfiguration(Configuration environment) {
		Objects.requireNonNull(environment, "The passed configuration is null");

		EntityManager em = dbService.getEntityManager();
		em.getTransaction().begin();

		Configurations p = new Configurations();
		if(environment.getId()!=null) p.setId(environment.getId());
		else p.setId(UUID.randomUUID().toString());

		Objects.requireNonNull(environment.getConfiguration(), "Missing configuration");

		p.setConfiguration(environment.getConfiguration());
		em.persist(p);

		em.getTransaction().commit();
		em.getEntityManagerFactory().getCache().evictAll();

		em.close();
	}

	public static boolean updateConfiguration(Configuration environment) {
		Objects.requireNonNull(environment, "The passed configuration is null");
		Objects.requireNonNull(environment.getId(), "Missing configuration ID");
		Objects.requireNonNull(environment.getConfiguration(), "Missing configuration");

		EntityManager em = dbService.getEntityManager();
		em.getTransaction().begin();

		Configurations existing = getOneFromDB(em,
				Configurations.class,
				"configurations.findById",
				"ID", environment.getId());

		if (existing == null) {
			em.getTransaction().rollback();
			em.close();
			return false;
		}

		existing.setConfiguration(environment.getConfiguration());
		em.merge(existing);

		em.getTransaction().commit();
		em.getEntityManagerFactory().getCache().evictAll();

		em.close();
		return true;
	}

	public static boolean deleteConfiguration(String id) {
		Objects.requireNonNull(id, "The passed configuration ID is null");

		EntityManager em = dbService.getEntityManager();
		em.getTransaction().begin();

		Configurations existing = getOneFromDB(em,
				Configurations.class,
				"configurations.findById",
				"ID", id);

		if (existing == null) {
			em.getTransaction().rollback();
			em.close();
			return false;
		}

		em.remove(existing);

		em.getTransaction().commit();
		em.getEntityManagerFactory().getCache().evictAll();

		em.close();
		return true;
	}

	public static boolean existsConfiguration(String id) {
		Objects.requireNonNull(id, "The passed configuration ID is null");

		EntityManager em = dbService.getEntityManager();
		Configurations existing = getOneFromDB(em,
				Configurations.class,
				"configurations.findById",
				"ID", id);
		em.close();

		return existing != null;
	}
}







