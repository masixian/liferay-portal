/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.arquillian.extension.junit.bridge.observer;

import com.liferay.arquillian.extension.junit.bridge.container.registry.LocalContainerRegistry;

import java.io.IOException;

import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ContainerDef;
import org.jboss.arquillian.config.descriptor.impl.ArquillianDescriptorImpl;
import org.jboss.arquillian.config.descriptor.impl.ContainerDefImpl;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.core.api.Injector;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.api.event.ManagerStarted;
import org.jboss.arquillian.core.spi.ServiceLoader;

/**
 * @author Matthew Tambara
 */
public class ConfigurationRegistrar {

	public void loadConfiguration(@Observes ManagerStarted managerStarted)
		throws IOException {

		_instanceProducer.set(new ArquillianDescriptorImpl(null));

		Injector injector = _injectorInstance.get();

		ContainerRegistry containerRegistry = new LocalContainerRegistry();

		ContainerDef containerDef = new ContainerDefImpl("arquillian.xml");

		containerDef.setContainerName("default");

		injector.inject(
			containerRegistry.create(
				containerDef, _serviceLoaderInstance.get()));

		_containerRegistryInstanceProducer.set(containerRegistry);
	}

	@ApplicationScoped
	@Inject
	private InstanceProducer<ContainerRegistry>
		_containerRegistryInstanceProducer;

	@Inject
	private Instance<Injector> _injectorInstance;

	@ApplicationScoped
	@Inject
	private InstanceProducer<ArquillianDescriptor> _instanceProducer;

	@Inject
	private Instance<ServiceLoader> _serviceLoaderInstance;

}