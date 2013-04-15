package info.archinnov.achilles.entity;

import static info.archinnov.achilles.entity.type.ConsistencyLevel.*;
import static info.archinnov.achilles.serializer.SerializerUtils.*;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import info.archinnov.achilles.annotations.Consistency;
import info.archinnov.achilles.annotations.Counter;
import info.archinnov.achilles.annotations.Lazy;
import info.archinnov.achilles.dao.Pair;
import info.archinnov.achilles.entity.metadata.EntityMeta;
import info.archinnov.achilles.entity.metadata.MultiKeyProperties;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.metadata.PropertyType;
import info.archinnov.achilles.entity.type.ConsistencyLevel;
import info.archinnov.achilles.entity.type.WideMap;
import info.archinnov.achilles.exception.AchillesBeanMappingException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mapping.entity.TweetMultiKey;
import me.prettyprint.cassandra.model.HColumnImpl;
import me.prettyprint.cassandra.utils.TimeUUIDUtils;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import parser.entity.CorrectMultiKey;
import parser.entity.MultiKeyIncorrectType;
import parser.entity.MultiKeyNotInstantiable;
import parser.entity.MultiKeyWithDuplicateOrder;
import parser.entity.MultiKeyWithNegativeOrder;
import parser.entity.MultiKeyWithNoAnnotation;

import com.google.common.collect.Maps;

/**
 * PropertyHelperTest
 * 
 * @author DuyHai DOAN
 * 
 */
@RunWith(MockitoJUnitRunner.class)
public class PropertyHelperTest
{
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@InjectMocks
	private PropertyHelper helper;

	@Mock
	private PropertyMeta<TweetMultiKey, String> multiKeyWideMeta;

	@Mock
	private MultiKeyProperties multiKeyProperties;

	@Test
	public void should_parse_multi_key() throws Exception
	{
		Method nameGetter = CorrectMultiKey.class.getMethod("getName");
		Method nameSetter = CorrectMultiKey.class.getMethod("setName", String.class);

		Method rankGetter = CorrectMultiKey.class.getMethod("getRank");
		Method rankSetter = CorrectMultiKey.class.getMethod("setRank", int.class);

		MultiKeyProperties props = helper.parseMultiKey(CorrectMultiKey.class);

		assertThat(props.getComponentGetters()).containsExactly(nameGetter, rankGetter);
		assertThat(props.getComponentSetters()).containsExactly(nameSetter, rankSetter);
		assertThat(props.getComponentClasses()).containsExactly(String.class, int.class);

	}

	@Test
	public void should_exception_when_multi_key_incorrect_type() throws Exception
	{
		expectedEx.expect(AchillesBeanMappingException.class);
		expectedEx
				.expectMessage("The class 'java.util.List' is not a valid key type for the MultiKey class '"
						+ MultiKeyIncorrectType.class.getCanonicalName() + "'");

		helper.parseMultiKey(MultiKeyIncorrectType.class);
	}

	@Test
	public void should_exception_when_multi_key_wrong_key_order() throws Exception
	{
		expectedEx.expect(AchillesBeanMappingException.class);
		expectedEx.expectMessage("The key orders is wrong for MultiKey class '"
				+ MultiKeyWithNegativeOrder.class.getCanonicalName() + "'");

		helper.parseMultiKey(MultiKeyWithNegativeOrder.class);
	}

	@Test
	public void should_exception_when_multi_key_has_no_annotation() throws Exception
	{
		expectedEx.expect(AchillesBeanMappingException.class);
		expectedEx.expectMessage("No field with @Key annotation found in the class '"
				+ MultiKeyWithNoAnnotation.class.getCanonicalName() + "'");

		helper.parseMultiKey(MultiKeyWithNoAnnotation.class);
	}

	@Test
	public void should_exception_when_multi_key_has_duplicate_order() throws Exception
	{
		expectedEx.expect(AchillesBeanMappingException.class);

		expectedEx.expectMessage("The order '1' is duplicated in MultiKey '"
				+ MultiKeyWithDuplicateOrder.class.getCanonicalName() + "'");

		helper.parseMultiKey(MultiKeyWithDuplicateOrder.class);
	}

	@Test
	public void should_exception_when_multi_key_not_instantiable() throws Exception
	{
		expectedEx.expect(AchillesBeanMappingException.class);
		expectedEx.expectMessage("The class '" + MultiKeyNotInstantiable.class.getCanonicalName()
				+ "' should have a public default constructor");

		helper.parseMultiKey(MultiKeyNotInstantiable.class);
	}

	@Test
	public void should_infer_value_class_from_list() throws Exception
	{
		@SuppressWarnings("unused")
		class Test
		{
			private List<String> friends;
		}

		Type type = Test.class.getDeclaredField("friends").getGenericType();

		Class<String> infered = helper.inferValueClassForListOrSet(type, Test.class);

		assertThat(infered).isEqualTo(String.class);
	}

	@Test
	public void should_exception_when_infering_value_type_from_raw_list() throws Exception
	{
		@SuppressWarnings(
		{
				"rawtypes",
				"unused"
		})
		class Test
		{
			private List friends;
		}

		Type type = Test.class.getDeclaredField("friends").getGenericType();

		expectedEx.expect(AchillesBeanMappingException.class);
		expectedEx.expectMessage("The type '" + type.getClass().getCanonicalName()
				+ "' of the entity 'null' should be parameterized");

		helper.inferValueClassForListOrSet(type, Test.class);

	}

	@Test
	public void should_find_lazy() throws Exception
	{

		class Test
		{
			@Lazy
			private String name;
		}

		Field field = Test.class.getDeclaredField("name");

		assertThat(helper.isLazy(field)).isTrue();
	}

	@Test
	public void should_check_counter_annotation() throws Exception
	{
		class Test
		{
			@Counter
			private String counter;
		}

		Field field = Test.class.getDeclaredField("counter");

		assertThat(helper.hasCounterAnnotation(field)).isTrue();
	}

	@Test
	public void should_check_consistency_annotation() throws Exception
	{
		class Test
		{
			@Consistency
			private String consistency;
		}

		Field field = Test.class.getDeclaredField("consistency");

		assertThat(helper.hasConsistencyAnnotation(field)).isTrue();
	}

	@Test
	public void should_not_find_counter_if_not_long_type() throws Exception
	{

	}

	@Test
	public void should_determine_composite_type_alias_for_column_family() throws Exception
	{
		EntityMeta<Long> entityMeta = new EntityMeta<Long>();
		PropertyMeta<Integer, String> propertyMeta = new PropertyMeta<Integer, String>();
		propertyMeta.setType(PropertyType.WIDE_MAP);
		propertyMeta.setKeyClass(Integer.class);
		Map<String, PropertyMeta<?, ?>> propertyMap = Maps.newHashMap();
		propertyMap.put("map", propertyMeta);
		entityMeta.setPropertyMetas(propertyMap);

		String compatatorTypeAlias = helper.determineCompatatorTypeAliasForCompositeCF(
				propertyMeta, true);

		assertThat(compatatorTypeAlias).isEqualTo("(BytesType)");
	}

	@Test
	public void should_determine_composite_type_alias_for_multikey_column_family() throws Exception
	{
		EntityMeta<Long> entityMeta = new EntityMeta<Long>();
		PropertyMeta<TweetMultiKey, String> propertyMeta = new PropertyMeta<TweetMultiKey, String>();
		propertyMeta.setType(PropertyType.WIDE_MAP);
		propertyMeta.setKeyClass(TweetMultiKey.class);
		Map<String, PropertyMeta<?, ?>> propertyMap = Maps.newHashMap();
		propertyMap.put("values", propertyMeta);
		entityMeta.setPropertyMetas(propertyMap);

		String compatatorTypeAlias = helper.determineCompatatorTypeAliasForCompositeCF(
				propertyMeta, true);

		assertThat(compatatorTypeAlias).isEqualTo("(UUIDType,UTF8Type,BytesType)");
	}

	@Test
	public void should_determine_composite_type_alias_for_column_family_check() throws Exception
	{
		EntityMeta<Long> entityMeta = new EntityMeta<Long>();
		PropertyMeta<Integer, String> propertyMeta = new PropertyMeta<Integer, String>();
		propertyMeta.setType(PropertyType.WIDE_MAP);
		propertyMeta.setKeyClass(Integer.class);
		Map<String, PropertyMeta<?, ?>> propertyMap = Maps.newHashMap();
		propertyMap.put("map", propertyMeta);
		entityMeta.setPropertyMetas(propertyMap);

		String compatatorTypeAlias = helper.determineCompatatorTypeAliasForCompositeCF(
				propertyMeta, false);

		assertThat(compatatorTypeAlias).isEqualTo(
				"CompositeType(org.apache.cassandra.db.marshal.BytesType)");
	}

	@Test
	public void should_determine_composite_type_alias_for_multikey_column_family_check()
			throws Exception
	{
		EntityMeta<Long> entityMeta = new EntityMeta<Long>();
		PropertyMeta<TweetMultiKey, String> propertyMeta = new PropertyMeta<TweetMultiKey, String>();
		propertyMeta.setType(PropertyType.WIDE_MAP);
		propertyMeta.setKeyClass(TweetMultiKey.class);
		Map<String, PropertyMeta<?, ?>> propertyMap = Maps.newHashMap();
		propertyMap.put("values", propertyMeta);
		entityMeta.setPropertyMetas(propertyMap);

		String compatatorTypeAlias = helper.determineCompatatorTypeAliasForCompositeCF(
				propertyMeta, false);

		assertThat(compatatorTypeAlias)
				.isEqualTo(
						"CompositeType(org.apache.cassandra.db.marshal.UUIDType,org.apache.cassandra.db.marshal.UTF8Type,org.apache.cassandra.db.marshal.BytesType)");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void should_build_multikey_for_composite() throws Exception
	{
		Method authorSetter = TweetMultiKey.class.getDeclaredMethod("setAuthor", String.class);
		Method idSetter = TweetMultiKey.class.getDeclaredMethod("setId", UUID.class);
		Method retweetCountSetter = TweetMultiKey.class.getDeclaredMethod("setRetweetCount",
				int.class);

		UUID uuid1 = TimeUUIDUtils.getUniqueTimeUUIDinMillis();

		when(multiKeyWideMeta.getMultiKeyProperties()).thenReturn(multiKeyProperties);

		HColumn<Composite, String> hCol1 = buildHColumn(buildComposite("author1", uuid1, 11),
				"val1");

		when(multiKeyWideMeta.getKeyClass()).thenReturn(TweetMultiKey.class);

		when(multiKeyProperties.getComponentSerializers()).thenReturn(
				Arrays.asList((Serializer<?>) STRING_SRZ, UUID_SRZ, INT_SRZ));
		when(multiKeyProperties.getComponentSetters()).thenReturn(
				Arrays.asList(authorSetter, idSetter, retweetCountSetter));

		TweetMultiKey multiKey = helper.buildMultiKeyFromComposite(multiKeyWideMeta, hCol1.getName()
				.getComponents());

		assertThat(multiKey.getAuthor()).isEqualTo("author1");
		assertThat(multiKey.getId()).isEqualTo(uuid1);
		assertThat(multiKey.getRetweetCount()).isEqualTo(11);
	}

	@Test
	public void should_find_any_any_consistency_level() throws Exception
	{
		class Test
		{
			@Consistency(read = ANY, write = LOCAL_QUORUM)
			private WideMap<Integer, String> field;
		}

		Pair<ConsistencyLevel, ConsistencyLevel> levels = helper.findConsistencyLevels(Test.class
				.getDeclaredField("field"));

		assertThat(levels.left).isEqualTo(ANY);
		assertThat(levels.right).isEqualTo(LOCAL_QUORUM);
	}

	@Test
	public void should_find_quorum_consistency_level_by_default() throws Exception
	{
		class Test
		{
			@SuppressWarnings("unused")
			private WideMap<Integer, String> field;
		}

		Pair<ConsistencyLevel, ConsistencyLevel> levels = helper.findConsistencyLevels(Test.class
				.getDeclaredField("field"));

		assertThat(levels.left).isEqualTo(ConsistencyLevel.QUORUM);
		assertThat(levels.right).isEqualTo(ConsistencyLevel.QUORUM);
	}

	@Test
	public void should_return_true_when_type_supported() throws Exception
	{
		assertThat(PropertyHelper.isSupportedType(Long.class)).isTrue();
	}

	private Composite buildComposite(String author, UUID uuid, int retweetCount)
	{
		Composite composite = new Composite();
		composite.setComponent(0, author, STRING_SRZ, STRING_SRZ.getComparatorType().getTypeName());
		composite.setComponent(1, uuid, UUID_SRZ, UUID_SRZ.getComparatorType().getTypeName());
		composite.setComponent(2, retweetCount, INT_SRZ, INT_SRZ.getComparatorType().getTypeName());

		return composite;
	}

	private HColumn<Composite, String> buildHColumn(Composite comp, String value)
	{
		HColumn<Composite, String> hColumn = new HColumnImpl<Composite, String>(COMPOSITE_SRZ,
				STRING_SRZ);

		hColumn.setName(comp);
		hColumn.setValue(value);
		return hColumn;
	}
}
