/**
 *
 * Copyright (C) 2012-2013 DuyHai DOAN
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.archinnov.achilles.junit;

import static info.archinnov.achilles.embedded.CassandraEmbeddedConfigParameters.*;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableMap;
import info.archinnov.achilles.embedded.CassandraEmbeddedServer;
import info.archinnov.achilles.entity.manager.PersistenceManager;
import info.archinnov.achilles.entity.manager.PersistenceManagerFactory;
import info.archinnov.achilles.validation.Validator;

public class AchillesResource extends AchillesTestResource {

	private final PersistenceManagerFactory pmf;

	private final PersistenceManager manager;

	private final CassandraEmbeddedServer server;

	private final Session session;


	AchillesResource(String keyspaceName,String entityPackages, String... tables) {
		super(tables);

        String keyspaceToUse = StringUtils.isNotBlank(keyspaceName) ? keyspaceName: DEFAULT_ACHILLES_TEST_KEYSPACE_NAME;

        Map<String, Object> config = ImmutableMap.<String, Object> of(CLEAN_CASSANDRA_DATA_FILES, true,
				 KEYSPACE_NAME, keyspaceToUse,KEYSPACE_DURABLE_WRITE, false);

        config = addEntityPackagesIfNeeded(entityPackages, config);

        server = new CassandraEmbeddedServer(config);
		pmf = server.getPersistenceManagerFactory(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
		manager = server.getPersistenceManager(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
		session = server.getNativeSession(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
	}

    AchillesResource(String keyspaceName,String entityPackages, Steps cleanUpSteps, String... tables) {
		super(cleanUpSteps, tables);

        String keyspaceToUse = StringUtils.isNotBlank(keyspaceName) ? keyspaceName: DEFAULT_ACHILLES_TEST_KEYSPACE_NAME;

        Validator.validateNotBlank(entityPackages, "Entity packages should be provided");
		Map<String, Object> config = ImmutableMap.<String, Object> of(CLEAN_CASSANDRA_DATA_FILES, true,
				KEYSPACE_NAME, keyspaceToUse, KEYSPACE_DURABLE_WRITE, false);

        config = addEntityPackagesIfNeeded(entityPackages, config);

		server = new CassandraEmbeddedServer(config);
		pmf = server.getPersistenceManagerFactory(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
		manager = server.getPersistenceManager(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
		session = server.getNativeSession(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME);
	}

    private Map<String, Object> addEntityPackagesIfNeeded(String entityPackages, Map<String, Object> config) {
        if(StringUtils.isNotBlank(entityPackages)) {
            final Map<String, Object> newConfig = new HashMap<String, Object>();
            newConfig.putAll(config);
            newConfig.put(ENTITY_PACKAGES,entityPackages);
            config = newConfig;
        }
        return config;
    }

	/**
	 * Return a singleton PersistenceManagerFactory
	 * 
	 * @return PersistenceManagerFactory singleton
	 */
	public PersistenceManagerFactory getPersistenceManagerFactory() {
		return pmf;
	}

	/**
	 * Return a singleton PersistenceManager
	 * 
	 * @return PersistenceManager singleton
	 */
	public PersistenceManager getPersistenceManager() {
		return manager;
	}

	/**
	 * Return a native CQL3 Session
	 * 
	 * @return native CQL3 Session
	 */
	public Session getNativeSession() {
		return session;
	}

	@Override
	protected void truncateTables() {
		if (tables != null) {
			for (String table : tables) {
				server.truncateTable(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME, table);
			}
		}
	}

}
