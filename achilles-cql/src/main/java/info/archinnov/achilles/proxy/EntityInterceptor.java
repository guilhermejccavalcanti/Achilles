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
package info.archinnov.achilles.proxy;

import info.archinnov.achilles.context.PersistenceContext;
import info.archinnov.achilles.entity.metadata.PropertyMeta;
import info.archinnov.achilles.entity.operations.EntityLoader;
import info.archinnov.achilles.proxy.wrapper.CounterWrapper;
import info.archinnov.achilles.proxy.wrapper.builder.ListWrapperBuilder;
import info.archinnov.achilles.proxy.wrapper.builder.MapWrapperBuilder;
import info.archinnov.achilles.proxy.wrapper.builder.SetWrapperBuilder;
import info.archinnov.achilles.type.Counter;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityInterceptor<T> implements MethodInterceptor, Serializable {

	private static final long serialVersionUID = 1L;

	private static final transient Logger log = LoggerFactory.getLogger(EntityInterceptor.class);

	private transient EntityLoader loader = new EntityLoader();

	private T target;
	private transient Object primaryKey;
	private transient Method idGetter;
	private transient Method idSetter;
	private transient Map<Method, PropertyMeta> getterMetas;
	private transient Map<Method, PropertyMeta> setterMetas;
	private transient Map<Method, PropertyMeta> dirtyMap;
	private transient Set<Method> alreadyLoaded;
	private transient PersistenceContext context;

	public Object getTarget() {
		return this.target;
	}

	@Override
	public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
		log.trace("Method {} called for entity of class {}", method.getName(), target.getClass().getCanonicalName());

		if (idGetter.equals(method)) {
			return primaryKey;
		} else if (idSetter.equals(method)) {
			throw new IllegalAccessException("Cannot change primary key value for existing entity ");
		}

		Object result;
		if (this.getterMetas.containsKey(method)) {
			result = interceptGetter(method, args, proxy);
		} else if (this.setterMetas.containsKey(method)) {
			result = interceptSetter(method, args, proxy);
		} else {
			result = proxy.invoke(target, args);
		}
		return result;
	}

	private Object interceptGetter(Method method, Object[] args, MethodProxy proxy) throws Throwable {
		Object result = null;
		PropertyMeta propertyMeta = this.getterMetas.get(method);

		// Load fields into target object
		if (!propertyMeta.isCounter() && !this.alreadyLoaded.contains(method)) {
			log.trace("Loading property {}", propertyMeta.getPropertyName());

			loader.loadPropertyIntoObject(context, target, propertyMeta);
			alreadyLoaded.add(method);
		}

		log.trace("Invoking getter {} on real object", method.getName());
		Object rawValue = proxy.invoke(target, args);

		// Build proxy when necessary
		switch (propertyMeta.type()) {
		case COUNTER:
			log.trace("Build counter wrapper for property {} of entity of class {} ", propertyMeta.getPropertyName(),
					propertyMeta.getEntityClassName());
			result = buildCounterWrapper(propertyMeta);
			break;
		case LIST:
		case LAZY_LIST:
			if (rawValue != null) {
				log.trace("Build list wrapper for property {} of entity of class {} ", propertyMeta.getPropertyName(),
						propertyMeta.getEntityClassName());

				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) rawValue;
				result = ListWrapperBuilder.builder(context, list).dirtyMap(dirtyMap).setter(propertyMeta.getSetter())
						.propertyMeta(this.getPropertyMetaByProperty(method)).build();
			}
			break;
		case SET:
		case LAZY_SET:
			if (rawValue != null) {
				log.trace("Build set wrapper for property {} of entity of class {} ", propertyMeta.getPropertyName(),
						propertyMeta.getEntityClassName());

				@SuppressWarnings("unchecked")
				Set<Object> set = (Set<Object>) rawValue;
				result = SetWrapperBuilder.builder(context, set).dirtyMap(dirtyMap).setter(propertyMeta.getSetter())
						.propertyMeta(this.getPropertyMetaByProperty(method)).build();
			}
			break;
		case MAP:
		case LAZY_MAP:
			if (rawValue != null) {
				log.trace("Build map wrapper for property {} of entity of class {} ", propertyMeta.getPropertyName(),
						propertyMeta.getEntityClassName());

				@SuppressWarnings("unchecked")
				Map<Object, Object> map = (Map<Object, Object>) rawValue;
				result = MapWrapperBuilder
						//
						.builder(context, map).dirtyMap(dirtyMap).setter(propertyMeta.getSetter())
						.propertyMeta(this.getPropertyMetaByProperty(method)).build();
			}
			break;
		default:
			log.trace("Return un-mapped raw value {} for property {} of entity of class {} ",
					propertyMeta.getPropertyName(), propertyMeta.getEntityClassName());

			result = rawValue;
			break;
		}
		return result;
	}

	private Object interceptSetter(Method method, Object[] args, MethodProxy proxy) throws Throwable {
		PropertyMeta propertyMeta = this.setterMetas.get(method);
		Object result;

		switch (propertyMeta.type()) {
		case COUNTER:
			throw new UnsupportedOperationException(
					"Cannot set value directly to a Counter type. Please call the getter first to get handle on the wrapper");
		default:
			break;
		}

		if (propertyMeta.type().isLazy()) {
			this.alreadyLoaded.add(propertyMeta.getGetter());
		}
		log.trace("Flagging property {}", propertyMeta.getPropertyName());

		dirtyMap.put(method, propertyMeta);
		result = proxy.invoke(target, args);
		return result;
	}

	public Object writeReplace() {
		System.out.println("Write replace called");
		return this.target;
	}

	public Map<Method, PropertyMeta> getDirtyMap() {
		return dirtyMap;
	}

	public Set<Method> getAlreadyLoaded() {
		return alreadyLoaded;
	}

	public Object getPrimaryKey() {
		return primaryKey;
	}

	public void setTarget(T target) {
		this.target = target;
	}

	void setPrimaryKey(Object key) {
		this.primaryKey = key;
	}

	void setIdGetter(Method idGetter) {
		this.idGetter = idGetter;
	}

	void setIdSetter(Method idSetter) {
		this.idSetter = idSetter;
	}

	void setGetterMetas(Map<Method, PropertyMeta> getterMetas) {
		this.getterMetas = getterMetas;
	}

	void setSetterMetas(Map<Method, PropertyMeta> setterMetas) {
		this.setterMetas = setterMetas;
	}

	void setDirtyMap(Map<Method, PropertyMeta> dirtyMap) {
		this.dirtyMap = dirtyMap;
	}

	void setAlreadyLoaded(Set<Method> lazyLoaded) {
		this.alreadyLoaded = lazyLoaded;
	}

	public PersistenceContext getContext() {
		return context;
	}

	public void setContext(PersistenceContext context) {
		this.context = context;
	}

	private PropertyMeta getPropertyMetaByProperty(Method method) {
		return getterMetas.get(method);
	}

	protected Counter buildCounterWrapper(PropertyMeta propertyMeta) {
		return new CounterWrapper(context, propertyMeta);
	}

}
