package info.archinnov.achilles.entity.operations;

import info.archinnov.achilles.entity.context.PersistenceContext;
import info.archinnov.achilles.proxy.interceptor.AchillesInterceptor;
import info.archinnov.achilles.proxy.interceptor.JpaEntityInterceptor;
import info.archinnov.achilles.proxy.interceptor.JpaEntityInterceptorBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;

/**
 * EntityProxifier
 * 
 * @author DuyHai DOAN
 * 
 */
public class EntityProxifier
{

	public <ID> Class<?> deriveBaseClass(Object entity)
	{
		Class<?> baseClass = entity.getClass();
		if (isProxy(entity))
		{
			AchillesInterceptor<ID> interceptor = this.getInterceptor(entity);
			baseClass = interceptor.getTarget().getClass();
		}

		return baseClass;
	}

	@SuppressWarnings("unchecked")
	public <T, ID> T buildProxy(T entity, PersistenceContext<ID> context)
	{
		if (entity == null)
		{
			return null;
		}

		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(entity.getClass());

		enhancer.setCallback(JpaEntityInterceptorBuilder.builder(context, entity).build());

		return (T) enhancer.create();
	}

	@SuppressWarnings("unchecked")
	public <T, ID> T getRealObject(T proxy)
	{
		Factory factory = (Factory) proxy;
		JpaEntityInterceptor<ID, T> interceptor = (JpaEntityInterceptor<ID, T>) factory
				.getCallback(0);
		return (T) interceptor.getTarget();
	}

	public boolean isProxy(Object entity)
	{
		return Factory.class.isAssignableFrom(entity.getClass());
	}

	@SuppressWarnings("unchecked")
	public <T, ID> JpaEntityInterceptor<ID, T> getInterceptor(T proxy)
	{
		Factory factory = (Factory) proxy;
		JpaEntityInterceptor<ID, T> interceptor = (JpaEntityInterceptor<ID, T>) factory
				.getCallback(0);
		return interceptor;
	}

	public <T> void ensureProxy(T proxy)
	{
		if (!this.isProxy(proxy))
		{
			throw new IllegalStateException("The entity '" + proxy + "' is not in 'managed' state.");
		}
	}

	public <T> T unproxy(T proxy)
	{
		if (proxy != null)
		{

			if (this.isProxy(proxy))
			{
				return this.getRealObject(proxy);
			}
			else
			{
				return proxy;
			}
		}
		else
		{
			return null;
		}
	}

	public <K, V> Entry<K, V> unproxy(Entry<K, V> entry)
	{
		V value = entry.getValue();
		if (this.isProxy(value))
		{
			value = this.getRealObject(value);
			entry.setValue(value);
		}
		return entry;
	}

	public <T> Collection<T> unproxy(Collection<T> proxies)
	{

		Collection<T> result = new ArrayList<T>();
		for (T proxy : proxies)
		{
			result.add(this.unproxy(proxy));
		}
		return result;
	}

	public <T> List<T> unproxy(List<T> proxies)
	{
		List<T> result = new ArrayList<T>();
		for (T proxy : proxies)
		{
			result.add(this.unproxy(proxy));
		}

		return result;
	}

	public <T> Set<T> unproxy(Set<T> proxies)
	{
		Set<T> result = new HashSet<T>();
		for (T proxy : proxies)
		{
			result.add(this.unproxy(proxy));
		}

		return result;
	}
}
