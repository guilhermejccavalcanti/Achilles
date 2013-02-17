package info.archinnov.achilles.wrapper;

import static info.archinnov.achilles.entity.metadata.PropertyType.EXTERNAL_JOIN_WIDE_MAP;
import static javax.persistence.CascadeType.ALL;
import static javax.persistence.CascadeType.PERSIST;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import info.archinnov.achilles.composite.factory.CompositeKeyFactory;
import info.archinnov.achilles.dao.GenericCompositeDao;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.metadata.JoinProperties;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.metadata.PropertyType;
import info.archinnov.achilles.entity.operations.EntityLoader;
import info.archinnov.achilles.entity.operations.EntityPersister;
import info.archinnov.achilles.entity.type.KeyValueIterator;
import info.archinnov.achilles.helper.CompositeHelper;
import info.archinnov.achilles.holder.KeyValue;
import info.archinnov.achilles.holder.factory.KeyValueFactory;
import info.archinnov.achilles.iterator.AchillesJoinSliceIterator;
import info.archinnov.achilles.iterator.factory.IteratorFactory;
import info.archinnov.achilles.proxy.interceptor.AchillesInterceptor;

import java.lang.reflect.Method;
import java.util.List;

import mapping.entity.UserBean;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import testBuilders.PropertyMetaTestBuilder;

/**
 * JoinExternalWideMapWrapperTest
 * 
 * @author DuyHai DOAN
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class JoinExternalWideMapWrapperTest
{
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@InjectMocks
	private JoinExternalWideMapWrapper<Long, Long, Integer, UserBean> wrapper;

	@Mock
	private GenericCompositeDao<Long, Long> dao;

	@Mock
	private PropertyMeta<Integer, UserBean> externalJoinWideMapMeta;

	@Mock
	private CompositeKeyFactory compositeKeyFactory;

	@Mock
	private EntityPersister persister;

	@Mock
	private EntityLoader loader;

	@Mock
	private CompositeHelper helper;

	@Mock
	private KeyValueFactory keyValueFactory;

	@Mock
	private IteratorFactory iteratorFactory;

	@Mock
	private AchillesInterceptor interceptor;

	@Mock
	private Mutator<Long> mutator;

	private Long id = 7425L;

	@Before
	public void setUp()
	{
		ReflectionTestUtils.setField(wrapper, "id", id);
		ReflectionTestUtils.setField(wrapper, "persister", persister);
		ReflectionTestUtils.setField(wrapper, "loader", loader);
		ReflectionTestUtils.setField(wrapper, "helper", helper);
		ReflectionTestUtils.setField(wrapper, "compositeKeyFactory", compositeKeyFactory);
		ReflectionTestUtils.setField(wrapper, "keyValueFactory", keyValueFactory);
		ReflectionTestUtils.setField(wrapper, "iteratorFactory", iteratorFactory);

	}

	@Test
	public void should_get_value() throws Exception
	{
		Long joinId = 1235L;
		int key = 4567;
		UserBean userBean = new UserBean();
		Composite comp = new Composite();

		EntityMeta<Long> joinEntityMeta = new EntityMeta<Long>();

		joinEntityMeta.setIdMeta(PropertyMetaTestBuilder //
				.noClass(Void.class, Long.class) //
				.type(EXTERNAL_JOIN_WIDE_MAP) //
				.build());
		JoinProperties joinProperties = new JoinProperties();
		joinProperties.setEntityMeta(joinEntityMeta);

		when(externalJoinWideMapMeta.getValueClass()).thenReturn(UserBean.class);
		when(externalJoinWideMapMeta.getJoinProperties()).thenReturn(
				(JoinProperties) joinProperties);

		when(compositeKeyFactory.createBaseComposite(externalJoinWideMapMeta, key))
				.thenReturn(comp);
		when(dao.getValue(id, comp)).thenReturn(joinId);
		when(loader.loadJoinEntity(UserBean.class, joinId, joinEntityMeta)).thenReturn(userBean);

		UserBean expected = wrapper.get(key);

		assertThat(expected).isSameAs(userBean);
	}

	@Test
	public void should_insert_value_and_entity_when_insertable() throws Exception
	{

		JoinProperties joinProperties = prepareJoinProperties();
		joinProperties.addCascadeType(PERSIST);

		int key = 4567;
		UserBean userBean = new UserBean();
		long userId = 475L;
		userBean.setUserId(userId);
		Composite comp = new Composite();

		when(externalJoinWideMapMeta.getJoinProperties()).thenReturn(
				(JoinProperties) joinProperties);
		when(compositeKeyFactory.createBaseComposite(externalJoinWideMapMeta, key))
				.thenReturn(comp);
		when(persister.cascadePersistOrEnsureExists(userBean, joinProperties)).thenReturn(userId);
		when(interceptor.isBatchMode()).thenReturn(false);
		wrapper.insert(key, userBean);

		verify(dao).setValue(id, comp, userId);
	}

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	@Test
	public void should_insert_value_in_batch_mode_and_entity_when_insertable() throws Exception
	{

		JoinProperties joinProperties = prepareJoinProperties();
		joinProperties.addCascadeType(PERSIST);

		int key = 4567;
		UserBean userBean = new UserBean();
		long userId = 475L;
		userBean.setUserId(userId);
		Composite comp = new Composite();

		when(externalJoinWideMapMeta.getJoinProperties()).thenReturn(
				(JoinProperties) joinProperties);
		when(compositeKeyFactory.createBaseComposite(externalJoinWideMapMeta, key))
				.thenReturn(comp);
		when(persister.cascadePersistOrEnsureExists(userBean, joinProperties)).thenReturn(userId);
		when(interceptor.isBatchMode()).thenReturn(true);
		when(interceptor.getMutator()).thenReturn((Mutator) mutator);
		wrapper.insert(key, userBean);

		verify(dao).setValueBatch(id, comp, userId, mutator);
	}

	@Test(expected = IllegalArgumentException.class)
	public void should_exception_when_trying_to_persist_null_entity() throws Exception
	{
		int key = 4567;
		wrapper.insert(key, null);
	}

	@Test
	public void should_insert_value_and_entity_with_ttl() throws Exception
	{
		JoinProperties joinProperties = prepareJoinProperties();
		joinProperties.addCascadeType(ALL);

		int key = 4567;
		UserBean userBean = new UserBean();
		long userId = 475L;
		userBean.setUserId(userId);
		Composite comp = new Composite();

		when(externalJoinWideMapMeta.getJoinProperties()).thenReturn(
				(JoinProperties) joinProperties);
		when(compositeKeyFactory.createBaseComposite(externalJoinWideMapMeta, key))
				.thenReturn(comp);
		when(persister.cascadePersistOrEnsureExists(userBean, joinProperties)).thenReturn(userId);
		when(interceptor.isBatchMode()).thenReturn(false);
		wrapper.insert(key, userBean, 150);

		verify(dao).setValue(id, comp, userId, 150);
	}

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	@Test
	public void should_insert_value_in_batch_mode_and_entity_with_ttl() throws Exception
	{
		JoinProperties joinProperties = prepareJoinProperties();
		joinProperties.addCascadeType(ALL);

		int key = 4567;
		UserBean userBean = new UserBean();
		long userId = 475L;
		userBean.setUserId(userId);
		Composite comp = new Composite();

		when(externalJoinWideMapMeta.getJoinProperties()).thenReturn(
				(JoinProperties) joinProperties);
		when(compositeKeyFactory.createBaseComposite(externalJoinWideMapMeta, key))
				.thenReturn(comp);
		when(persister.cascadePersistOrEnsureExists(userBean, joinProperties)).thenReturn(userId);
		when(interceptor.isBatchMode()).thenReturn(true);
		when(interceptor.getMutator()).thenReturn((Mutator) mutator);

		wrapper.insert(key, userBean, 150);

		verify(dao).setValueBatch(id, comp, userId, 150, mutator);
	}

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	@Test
	public void should_find_keyvalue_range() throws Exception
	{
		int start = 7, end = 5, count = 10;
		boolean reverse = true, inclusiveStart = false, inclusiveEnd = true;
		Composite startComp = new Composite(), endComp = new Composite();

		when(
				compositeKeyFactory.createForQuery(externalJoinWideMapMeta, start, inclusiveStart,
						end, inclusiveEnd, reverse)).thenReturn(new Composite[]
		{
				startComp,
				endComp
		});
		List<HColumn<Composite, ?>> hColumns = mock(List.class);
		when(dao.findRawColumnsRange(id, startComp, endComp, reverse, count)).thenReturn(
				(List) hColumns);
		List<KeyValue<Integer, UserBean>> values = mock(List.class);
		when(keyValueFactory.createJoinKeyValueListForComposite(externalJoinWideMapMeta, hColumns))
				.thenReturn(values);

		List<KeyValue<Integer, UserBean>> expected = wrapper.find(start, inclusiveStart, end,
				inclusiveEnd, reverse, count);

		verify(helper).checkBounds(externalJoinWideMapMeta, start, end, reverse);
		assertThat(expected).isSameAs(values);
	}

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	@Test
	public void should_find_values_range() throws Exception
	{
		int start = 7, end = 5, count = 10;
		boolean reverse = true, inclusiveStart = false, inclusiveEnd = true;
		Composite startComp = new Composite(), endComp = new Composite();

		when(
				compositeKeyFactory.createForQuery(externalJoinWideMapMeta, start, inclusiveStart,
						end, inclusiveEnd, reverse)).thenReturn(new Composite[]
		{
				startComp,
				endComp
		});
		List<HColumn<Composite, ?>> hColumns = mock(List.class);
		when(dao.findRawColumnsRange(id, startComp, endComp, reverse, count)).thenReturn(
				(List) hColumns);
		List<UserBean> values = mock(List.class);
		when(keyValueFactory.createJoinValueListForComposite(externalJoinWideMapMeta, hColumns))
				.thenReturn(values);

		List<UserBean> expected = wrapper.findValues(start, inclusiveStart, end, inclusiveEnd,
				reverse, count);

		verify(helper).checkBounds(externalJoinWideMapMeta, start, end, reverse);
		assertThat(expected).isSameAs(values);
	}

	@SuppressWarnings(
	{
			"unchecked",
			"rawtypes"
	})
	@Test
	public void should_find_keys_range() throws Exception
	{
		int start = 7, end = 5, count = 10;
		boolean reverse = true, inclusiveStart = false, inclusiveEnd = true;
		Composite startComp = new Composite(), endComp = new Composite();

		when(
				compositeKeyFactory.createForQuery(externalJoinWideMapMeta, start, inclusiveStart,
						end, inclusiveEnd, reverse)).thenReturn(new Composite[]
		{
				startComp,
				endComp
		});
		List<HColumn<Composite, ?>> hColumns = mock(List.class);
		when(dao.findRawColumnsRange(id, startComp, endComp, reverse, count)).thenReturn(
				(List) hColumns);
		List<Integer> values = mock(List.class);
		when(keyValueFactory.createKeyListForComposite(externalJoinWideMapMeta, hColumns))
				.thenReturn(values);

		List<Integer> expected = wrapper.findKeys(start, inclusiveStart, end, inclusiveEnd,
				reverse, count);

		verify(helper).checkBounds(externalJoinWideMapMeta, start, end, reverse);
		assertThat(expected).isSameAs(values);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_get_iterator() throws Exception
	{
		int start = 7, end = 5, count = 10;
		boolean reverse = true, inclusiveStart = false, inclusiveEnd = true;
		Composite startComp = new Composite(), endComp = new Composite();

		when(
				compositeKeyFactory.createForQuery(externalJoinWideMapMeta, start, inclusiveStart,
						end, inclusiveEnd, reverse)).thenReturn(new Composite[]
		{
				startComp,
				endComp
		});

		AchillesJoinSliceIterator<Long, Composite, Long, Integer, UserBean> iterator = mock(AchillesJoinSliceIterator.class);
		when(
				dao.getJoinColumnsIterator(externalJoinWideMapMeta, id, startComp, endComp,
						reverse, count)).thenReturn(iterator);

		KeyValueIterator<Integer, UserBean> keyValueIterator = mock(KeyValueIterator.class);
		when(iteratorFactory.createKeyValueIteratorForComposite(iterator, externalJoinWideMapMeta))
				.thenReturn(keyValueIterator);

		KeyValueIterator<Integer, UserBean> expected = wrapper.iterator(start, inclusiveStart, end,
				inclusiveEnd, reverse, count);

		assertThat(expected).isSameAs(keyValueIterator);
	}

	@Test
	public void should_remove() throws Exception
	{
		int key = 4567;
		Composite comp = new Composite();

		when(compositeKeyFactory.createBaseComposite(externalJoinWideMapMeta, key))
				.thenReturn(comp);

		wrapper.remove(key);

		verify(dao).removeColumn(id, comp);
	}

	@Test
	public void should_remove_range() throws Exception
	{

		int start = 7, end = 5;
		boolean inclusiveStart = false, inclusiveEnd = true;
		Composite startComp = new Composite(), endComp = new Composite();

		when(
				compositeKeyFactory.createForQuery(externalJoinWideMapMeta, start, inclusiveStart,
						end, inclusiveEnd, false)).thenReturn(new Composite[]
		{
				startComp,
				endComp
		});

		wrapper.remove(start, inclusiveStart, end, inclusiveEnd);

		verify(helper).checkBounds(externalJoinWideMapMeta, start, end, false);
		verify(dao).removeColumnRange(id, startComp, endComp);

	}

	@Test
	public void should_remove_first() throws Exception
	{
		wrapper.removeFirst(15);

		verify(dao).removeColumnRange(id, null, null, false, 15);
	}

	@Test
	public void should_remove_last() throws Exception
	{
		wrapper.removeLast(9);

		verify(dao).removeColumnRange(id, null, null, true, 9);
	}

	private JoinProperties prepareJoinProperties() throws Exception
	{
		EntityMeta<Long> joinEntityMeta = new EntityMeta<Long>();
		joinEntityMeta.setClassName("canonicalClassName");

		Method idGetter = UserBean.class.getDeclaredMethod("getUserId");
		PropertyMeta<Void, Long> idMeta = new PropertyMeta<Void, Long>();
		idMeta.setType(PropertyType.SIMPLE);
		idMeta.setGetter(idGetter);
		joinEntityMeta.setIdMeta(idMeta);
		JoinProperties joinProperties = new JoinProperties();
		joinProperties.setEntityMeta(joinEntityMeta);

		return joinProperties;
	}
}
