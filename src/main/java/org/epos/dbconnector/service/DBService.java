package org.epos.dbconnector.service;


import jakarta.persistence.EntityManager;

public class DBService {
    public DBService() {
    }

    public EntityManager getEntityManager() {
        return EntityManagerFactoryProvider.getInstance().createEntityManager();
    }

}
