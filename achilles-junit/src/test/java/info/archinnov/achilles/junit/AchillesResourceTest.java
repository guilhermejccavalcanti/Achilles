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

import static info.archinnov.achilles.embedded.CassandraEmbeddedConfigParameters.DEFAULT_ACHILLES_TEST_KEYSPACE_NAME;
import static org.fest.assertions.api.Assertions.assertThat;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import info.archinnov.achilles.entity.manager.PersistenceManager;
import info.archinnov.achilles.entity.manager.PersistenceManagerFactory;
import info.archinnov.achilles.junit.AchillesTestResource.Steps;
import info.archinnov.achilles.test.integration.entity.User;

public class AchillesResourceTest {

	@Rule
	public AchillesResource resource = new AchillesResource(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME,"info.archinnov.achilles.test.integration.entity",
			Steps.AFTER_TEST, "User");

	private PersistenceManagerFactory pmf = resource.getPersistenceManagerFactory();
	private PersistenceManager manager = resource.getPersistenceManager();
	private Session session = resource.getNativeSession();

	@Test
	public void should_bootstrap_embedded_server_and_entity_manager() throws Exception {

		Long id = RandomUtils.nextLong();
		manager.persist(new User(id, "fn", "ln"));

		Row row = session.execute("SELECT * FROM User WHERE id=" + id).one();

		assertThat(row).isNotNull();

		assertThat(row.getString("firstname")).isEqualTo("fn");
		assertThat(row.getString("lastname")).isEqualTo("ln");
	}

	@Test
	public void should_create_resources_once() throws Exception {
		AchillesResource resource = new AchillesResource(DEFAULT_ACHILLES_TEST_KEYSPACE_NAME,"info.archinnov.achilles.junit.test.entity");

		assertThat(resource.getPersistenceManagerFactory()).isSameAs(pmf);
		assertThat(resource.getPersistenceManager()).isSameAs(manager);
		assertThat(resource.getNativeSession()).isSameAs(session);
	}
}
