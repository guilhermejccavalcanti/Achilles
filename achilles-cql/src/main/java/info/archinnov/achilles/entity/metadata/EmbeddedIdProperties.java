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
package info.archinnov.achilles.entity.metadata;

import java.lang.reflect.Method;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Objects;
import info.archinnov.achilles.validation.Validator;

public class EmbeddedIdProperties extends AbstractComponentProperties {

    private static final Logger log  = LoggerFactory.getLogger(EmbeddedIdProperties.class);

    private final PartitionComponents partitionComponents;
	private final ClusteringComponents clusteringComponents;
	private final List<String> timeUUIDComponents;

	public EmbeddedIdProperties(PartitionComponents partitionComponents, ClusteringComponents clusteringComponents,
			List<Class<?>> componentClasses, List<String> componentNames, List<Method> componentGetters,
			List<Method> componentSetters, List<String> timeUUIDComponents) {
		super(componentClasses, componentNames, componentGetters, componentSetters);
		this.partitionComponents = partitionComponents;
		this.clusteringComponents = clusteringComponents;
		this.timeUUIDComponents = timeUUIDComponents;
	}

	void validatePartitionComponents(String className, List<Object> partitionComponents) {
		this.partitionComponents.validatePartitionComponents(className, partitionComponents);
	}

	void validateClusteringComponents(String className, List<Object> clusteringComponents) {
		this.clusteringComponents.validateClusteringComponents(className, clusteringComponents);
	}

	String getVaryingComponentNameForQuery(int fixedComponentsSize) {
        log.trace("Get varying component name for query");
		if (fixedComponentsSize > 0)
			return getComponentNames().get(fixedComponentsSize);
		else
			return getClusteringComponentNames().get(0);
	}

	Class<?> getVaryingComponentClassForQuery(int fixedComponentsSize) {
        log.trace("Get varying component class for query");
		if (fixedComponentsSize > 0)
			return getComponentClasses().get(fixedComponentsSize);
		else
			return getClusteringComponentClasses().get(0);
	}

	public boolean isCompositePartitionKey() {
		return partitionComponents.isComposite();
	}

	public boolean isClustered() {
		return clusteringComponents.isClustered();
	}

	public String getOrderingComponent() {
		return clusteringComponents.getOrderingComponent();
	}

	public String getReversedComponent() {
		return clusteringComponents.getReversedComponent();
	}

	public boolean hasReversedComponent() {
		return clusteringComponents.hasReversedComponent();
	}

	public List<String> getClusteringComponentNames() {
		return clusteringComponents.getComponentNames();
	}

	public List<Class<?>> getClusteringComponentClasses() {
		return clusteringComponents.getComponentClasses();
	}

	public List<String> getPartitionComponentNames() {
		return partitionComponents.getComponentNames();
	}

	public List<Class<?>> getPartitionComponentClasses() {
		return partitionComponents.getComponentClasses();
	}

	public List<Method> getPartitionComponentSetters() {
		return partitionComponents.getComponentSetters();
	}

	@Override
	public List<Class<?>> getComponentClasses() {
		return componentClasses;
	}

	@Override
	public List<Method> getComponentGetters() {
		return componentGetters;
	}

	@Override
	public List<Method> getComponentSetters() {
		return componentSetters;
	}

	@Override
	public List<String> getComponentNames() {
		return componentNames;
	}

	public List<String> getTimeUUIDComponents() {
		return timeUUIDComponents;
	}

	@Override
	public String toString() {

		return Objects.toStringHelper(this.getClass()).add("partitionComponents", partitionComponents)
				.add("clusteringComponents", clusteringComponents).toString();

	}

	List<Object> extractPartitionComponents(List<Object> components) {
        log.trace("Extract partition key components from {}",components);
		int partitionComponentsCount = partitionComponents.getComponentClasses().size();

		Validator.validateTrue(components.size() >= partitionComponentsCount,
				"Cannot extract composite partition key components from components list '%s'", components);
		return components.subList(0, partitionComponentsCount);
	}

	List<Object> extractClusteringComponents(List<Object> components) {
        log.trace("Extract clustering components from {}",components);
        int partitionComponentsCount = partitionComponents.getComponentClasses().size();

		Validator.validateTrue(components.size() >= partitionComponentsCount,
				"Cannot extract clustering components from components list '%s'", components);
		return components.subList(partitionComponentsCount, components.size());
	}
}
