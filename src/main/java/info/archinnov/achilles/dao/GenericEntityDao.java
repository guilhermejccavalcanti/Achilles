package info.archinnov.achilles.dao;

import static info.archinnov.achilles.entity.metadata.PropertyType.*;
import static info.archinnov.achilles.serializer.SerializerUtils.*;
import info.archinnov.achilles.consistency.AchillesConfigurableConsistencyLevelPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.Keyspace;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.beans.AbstractComposite.ComponentEquality;
import me.prettyprint.hector.api.beans.Composite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.beans.Row;
import me.prettyprint.hector.api.beans.Rows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GenericEntityDao
 * 
 * @author DuyHai DOAN
 * 
 */
public class GenericEntityDao<K> extends AbstractDao<K, String>
{
	private static final Logger log = LoggerFactory.getLogger(GenericEntityDao.class);

	private Composite startCompositeForEagerFetch;
	private Composite endCompositeForEagerFetch;

	protected GenericEntityDao() {
		this.initComposites();
	}

	public GenericEntityDao(Cluster cluster, Keyspace keyspace, Serializer<K> keySrz, String cf,
			AchillesConfigurableConsistencyLevelPolicy consistencyPolicy)
	{
		super(cluster, keyspace);
		this.initComposites();
		keySerializer = keySrz;
		columnFamily = cf;
		columnNameSerializer = COMPOSITE_SRZ;
		valueSerializer = STRING_SRZ;
		policy = consistencyPolicy;
		log.debug(
				"Initializing GenericEntityDao for key serializer '{}', composite comparator and value serializer '{}'",
				keySrz.getComparatorType().getTypeName(), STRING_SRZ.getComparatorType()
						.getTypeName());

	}

	public List<Pair<Composite, String>> eagerFetchEntity(K key)
	{
		log.trace("Eager fetching properties for column family {} ", columnFamily);

		return this.findColumnsRange(key, startCompositeForEagerFetch, endCompositeForEagerFetch,
				false, Integer.MAX_VALUE);
	}

	public Map<K, List<Pair<Composite, String>>> eagerFetchEntities(List<K> keys)
	{
		log.trace("Eager fetching properties for multiple entities in column family {} ",
				columnFamily);

		Map<K, List<Pair<Composite, String>>> map = new HashMap<K, List<Pair<Composite, String>>>();

		Rows<K, Composite, String> rows = this.multiGetSliceRange(keys,
				startCompositeForEagerFetch, endCompositeForEagerFetch, false, Integer.MAX_VALUE);

		for (Row<K, Composite, String> row : rows)
		{
			List<Pair<Composite, String>> columns = new ArrayList<Pair<Composite, String>>();
			for (HColumn<Composite, String> column : row.getColumnSlice().getColumns())
			{
				columns.add(new Pair<Composite, String>(column.getName(), column.getValue()));
			}

			map.put(row.getKey(), columns);
		}

		return map;
	}

	private void initComposites()
	{
		startCompositeForEagerFetch = new Composite();
		startCompositeForEagerFetch.addComponent(0, START_EAGER.flag(), ComponentEquality.EQUAL);

		endCompositeForEagerFetch = new Composite();
		endCompositeForEagerFetch.addComponent(0, END_EAGER.flag(),
				ComponentEquality.GREATER_THAN_EQUAL);
	}
}
