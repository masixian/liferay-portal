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

package com.liferay.depot.web.internal.asset.model;

import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.asset.kernel.model.ClassTypeReader;
import com.liferay.depot.service.DepotEntryLocalService;
import com.liferay.depot.web.internal.application.controller.DepotApplicationController;
import com.liferay.osgi.util.ServiceTrackerFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextThreadLocal;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.GroupThreadLocal;
import com.liferay.portal.kernel.util.HashMapDictionary;

import java.util.Dictionary;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * @author Alejandro Tardín
 */
@Component(
	immediate = true, service = DepotAssetRendererFactoryController.class
)
public class DepotAssetRendererFactoryController {

	@Activate
	protected void activate(BundleContext bundleContext) {
		_serviceTracker = ServiceTrackerFactory.open(
			bundleContext, AssetRendererFactory.class,
			new DepotAssetRendererFactoryServiceTrackerCustomizer(
				bundleContext, _serviceRegistrations));
	}

	@Deactivate
	protected void deactivate() {
		_serviceTracker.close();

		for (ServiceRegistration<AssetRendererFactory> serviceRegistration :
				_serviceRegistrations.values()) {

			try {
				serviceRegistration.unregister();
			}
			catch (IllegalStateException illegalStateException) {
				_log.error(illegalStateException, illegalStateException);
			}
		}

		_serviceRegistrations.clear();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DepotAssetRendererFactoryController.class);

	@Reference
	private DepotApplicationController _depotApplicationController;

	@Reference
	private DepotEntryLocalService _depotEntryLocalService;

	@Reference
	private GroupLocalService _groupLocalService;

	private final Map
		<ServiceReference<AssetRendererFactory>,
		 ServiceRegistration<AssetRendererFactory>> _serviceRegistrations =
			new ConcurrentHashMap<>();
	private ServiceTracker<AssetRendererFactory, AssetRendererFactory>
		_serviceTracker;

	private class ControlledDepotAssetRendererFactoryWrapper
		extends DepotAssetRendererFactoryWrapper {

		public ControlledDepotAssetRendererFactoryWrapper(
			AssetRendererFactory assetRendererFactory) {

			_assetRendererFactory = assetRendererFactory;
		}

		@Override
		public ClassTypeReader getClassTypeReader() {
			if (isSelectable()) {
				return new DepotClassTypeReader(
					super.getClassTypeReader(), _depotEntryLocalService);
			}

			return super.getClassTypeReader();
		}

		@Override
		public boolean isSelectable() {
			Group group = _getGroup();

			if ((group != null) &&
				(group.getType() == GroupConstants.TYPE_DEPOT) &&
				!_depotApplicationController.isClassNameEnabled(
					getClassName(), group.getGroupId())) {

				return false;
			}

			return _assetRendererFactory.isSelectable();
		}

		@Override
		protected AssetRendererFactory getAssetRendererFactory() {
			return _assetRendererFactory;
		}

		private Group _getGroup() {
			ServiceContext serviceContext =
				ServiceContextThreadLocal.getServiceContext();

			if (serviceContext == null) {
				return _groupLocalService.fetchGroup(
					GroupThreadLocal.getGroupId());
			}

			ThemeDisplay themeDisplay = serviceContext.getThemeDisplay();

			if (themeDisplay != null) {
				return themeDisplay.getScopeGroup();
			}

			return _groupLocalService.fetchGroup(
				serviceContext.getScopeGroupId());
		}

		private final AssetRendererFactory _assetRendererFactory;

	}

	private class DepotAssetRendererFactoryServiceTrackerCustomizer
		implements ServiceTrackerCustomizer
			<AssetRendererFactory, AssetRendererFactory> {

		public DepotAssetRendererFactoryServiceTrackerCustomizer(
			BundleContext bundleContext,
			Map
				<ServiceReference<AssetRendererFactory>,
				 ServiceRegistration<AssetRendererFactory>>
					serviceRegistrations) {

			_bundleContext = bundleContext;
			_serviceRegistrations = serviceRegistrations;
		}

		@Override
		public AssetRendererFactory addingService(
			ServiceReference<AssetRendererFactory> serviceReference) {

			AssetRendererFactory assetRendererFactory =
				_bundleContext.getService(serviceReference);

			if (assetRendererFactory instanceof
					ControlledDepotAssetRendererFactoryWrapper) {

				return assetRendererFactory;
			}

			Dictionary<String, Object> assetRendererFactoryProperties =
				new HashMapDictionary<>();

			for (String key : serviceReference.getPropertyKeys()) {
				assetRendererFactoryProperties.put(
					key, serviceReference.getProperty(key));
			}

			assetRendererFactoryProperties.put(
				"service.ranking", Integer.MAX_VALUE);

			AssetRendererFactory wrappedAssetRendererFactoryWrapper =
				new ControlledDepotAssetRendererFactoryWrapper(
					assetRendererFactory);

			ServiceRegistration<AssetRendererFactory> serviceRegistration =
				_bundleContext.registerService(
					AssetRendererFactory.class,
					wrappedAssetRendererFactoryWrapper,
					assetRendererFactoryProperties);

			_serviceRegistrations.put(serviceReference, serviceRegistration);

			return wrappedAssetRendererFactoryWrapper;
		}

		@Override
		public void modifiedService(
			ServiceReference<AssetRendererFactory> serviceReference,
			AssetRendererFactory assetRendererFactory) {

			removedService(serviceReference, assetRendererFactory);

			addingService(serviceReference);
		}

		@Override
		public void removedService(
			ServiceReference<AssetRendererFactory> serviceReference,
			AssetRendererFactory assetRendererFactory) {

			ServiceRegistration<AssetRendererFactory> serviceRegistration =
				_serviceRegistrations.remove(serviceReference);

			if (serviceRegistration != null) {
				serviceRegistration.unregister();
			}
		}

		private final BundleContext _bundleContext;
		private final Map
			<ServiceReference<AssetRendererFactory>,
			 ServiceRegistration<AssetRendererFactory>> _serviceRegistrations;

	}

}