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

package com.liferay.reading.time.service.base;

import com.liferay.exportimport.kernel.lar.ExportImportHelperUtil;
import com.liferay.exportimport.kernel.lar.ManifestSummary;
import com.liferay.exportimport.kernel.lar.PortletDataContext;
import com.liferay.exportimport.kernel.lar.StagedModelDataHandlerUtil;
import com.liferay.exportimport.kernel.lar.StagedModelType;
import com.liferay.portal.aop.AopService;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.SqlUpdate;
import com.liferay.portal.kernel.dao.jdbc.SqlUpdateFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DefaultActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.ExportActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.dao.orm.Property;
import com.liferay.portal.kernel.dao.orm.PropertyFactoryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.PersistedModel;
import com.liferay.portal.kernel.module.framework.service.IdentifiableOSGiService;
import com.liferay.portal.kernel.search.Indexable;
import com.liferay.portal.kernel.search.IndexableType;
import com.liferay.portal.kernel.service.BaseLocalServiceImpl;
import com.liferay.portal.kernel.service.PersistedModelLocalService;
import com.liferay.portal.kernel.transaction.Transactional;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.reading.time.model.ReadingTimeEntry;
import com.liferay.reading.time.service.ReadingTimeEntryLocalService;
import com.liferay.reading.time.service.persistence.ReadingTimeEntryPersistence;

import java.io.Serializable;

import java.util.List;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Reference;

/**
 * Provides the base implementation for the reading time entry local service.
 *
 * <p>
 * This implementation exists only as a container for the default service methods generated by ServiceBuilder. All custom service methods should be put in {@link com.liferay.reading.time.service.impl.ReadingTimeEntryLocalServiceImpl}.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.liferay.reading.time.service.impl.ReadingTimeEntryLocalServiceImpl
 * @generated
 */
public abstract class ReadingTimeEntryLocalServiceBaseImpl
	extends BaseLocalServiceImpl
	implements AopService, IdentifiableOSGiService,
			   ReadingTimeEntryLocalService {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. Use <code>ReadingTimeEntryLocalService</code> via injection or a <code>org.osgi.util.tracker.ServiceTracker</code> or use <code>com.liferay.reading.time.service.ReadingTimeEntryLocalServiceUtil</code>.
	 */

	/**
	 * Adds the reading time entry to the database. Also notifies the appropriate model listeners.
	 *
	 * @param readingTimeEntry the reading time entry
	 * @return the reading time entry that was added
	 */
	@Indexable(type = IndexableType.REINDEX)
	@Override
	public ReadingTimeEntry addReadingTimeEntry(
		ReadingTimeEntry readingTimeEntry) {

		readingTimeEntry.setNew(true);

		return readingTimeEntryPersistence.update(readingTimeEntry);
	}

	/**
	 * Creates a new reading time entry with the primary key. Does not add the reading time entry to the database.
	 *
	 * @param readingTimeEntryId the primary key for the new reading time entry
	 * @return the new reading time entry
	 */
	@Override
	@Transactional(enabled = false)
	public ReadingTimeEntry createReadingTimeEntry(long readingTimeEntryId) {
		return readingTimeEntryPersistence.create(readingTimeEntryId);
	}

	/**
	 * Deletes the reading time entry with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param readingTimeEntryId the primary key of the reading time entry
	 * @return the reading time entry that was removed
	 * @throws PortalException if a reading time entry with the primary key could not be found
	 */
	@Indexable(type = IndexableType.DELETE)
	@Override
	public ReadingTimeEntry deleteReadingTimeEntry(long readingTimeEntryId)
		throws PortalException {

		return readingTimeEntryPersistence.remove(readingTimeEntryId);
	}

	/**
	 * Deletes the reading time entry from the database. Also notifies the appropriate model listeners.
	 *
	 * @param readingTimeEntry the reading time entry
	 * @return the reading time entry that was removed
	 */
	@Indexable(type = IndexableType.DELETE)
	@Override
	public ReadingTimeEntry deleteReadingTimeEntry(
		ReadingTimeEntry readingTimeEntry) {

		return readingTimeEntryPersistence.remove(readingTimeEntry);
	}

	@Override
	public DynamicQuery dynamicQuery() {
		Class<?> clazz = getClass();

		return DynamicQueryFactoryUtil.forClass(
			ReadingTimeEntry.class, clazz.getClassLoader());
	}

	/**
	 * Performs a dynamic query on the database and returns the matching rows.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the matching rows
	 */
	@Override
	public <T> List<T> dynamicQuery(DynamicQuery dynamicQuery) {
		return readingTimeEntryPersistence.findWithDynamicQuery(dynamicQuery);
	}

	/**
	 * Performs a dynamic query on the database and returns a range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.reading.time.model.impl.ReadingTimeEntryModelImpl</code>.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @return the range of matching rows
	 */
	@Override
	public <T> List<T> dynamicQuery(
		DynamicQuery dynamicQuery, int start, int end) {

		return readingTimeEntryPersistence.findWithDynamicQuery(
			dynamicQuery, start, end);
	}

	/**
	 * Performs a dynamic query on the database and returns an ordered range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.reading.time.model.impl.ReadingTimeEntryModelImpl</code>.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching rows
	 */
	@Override
	public <T> List<T> dynamicQuery(
		DynamicQuery dynamicQuery, int start, int end,
		OrderByComparator<T> orderByComparator) {

		return readingTimeEntryPersistence.findWithDynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the number of rows matching the dynamic query
	 */
	@Override
	public long dynamicQueryCount(DynamicQuery dynamicQuery) {
		return readingTimeEntryPersistence.countWithDynamicQuery(dynamicQuery);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @param projection the projection to apply to the query
	 * @return the number of rows matching the dynamic query
	 */
	@Override
	public long dynamicQueryCount(
		DynamicQuery dynamicQuery, Projection projection) {

		return readingTimeEntryPersistence.countWithDynamicQuery(
			dynamicQuery, projection);
	}

	@Override
	public ReadingTimeEntry fetchReadingTimeEntry(long readingTimeEntryId) {
		return readingTimeEntryPersistence.fetchByPrimaryKey(
			readingTimeEntryId);
	}

	/**
	 * Returns the reading time entry matching the UUID and group.
	 *
	 * @param uuid the reading time entry's UUID
	 * @param groupId the primary key of the group
	 * @return the matching reading time entry, or <code>null</code> if a matching reading time entry could not be found
	 */
	@Override
	public ReadingTimeEntry fetchReadingTimeEntryByUuidAndGroupId(
		String uuid, long groupId) {

		return readingTimeEntryPersistence.fetchByUUID_G(uuid, groupId);
	}

	/**
	 * Returns the reading time entry with the primary key.
	 *
	 * @param readingTimeEntryId the primary key of the reading time entry
	 * @return the reading time entry
	 * @throws PortalException if a reading time entry with the primary key could not be found
	 */
	@Override
	public ReadingTimeEntry getReadingTimeEntry(long readingTimeEntryId)
		throws PortalException {

		return readingTimeEntryPersistence.findByPrimaryKey(readingTimeEntryId);
	}

	@Override
	public ActionableDynamicQuery getActionableDynamicQuery() {
		ActionableDynamicQuery actionableDynamicQuery =
			new DefaultActionableDynamicQuery();

		actionableDynamicQuery.setBaseLocalService(
			readingTimeEntryLocalService);
		actionableDynamicQuery.setClassLoader(getClassLoader());
		actionableDynamicQuery.setModelClass(ReadingTimeEntry.class);

		actionableDynamicQuery.setPrimaryKeyPropertyName("readingTimeEntryId");

		return actionableDynamicQuery;
	}

	@Override
	public IndexableActionableDynamicQuery
		getIndexableActionableDynamicQuery() {

		IndexableActionableDynamicQuery indexableActionableDynamicQuery =
			new IndexableActionableDynamicQuery();

		indexableActionableDynamicQuery.setBaseLocalService(
			readingTimeEntryLocalService);
		indexableActionableDynamicQuery.setClassLoader(getClassLoader());
		indexableActionableDynamicQuery.setModelClass(ReadingTimeEntry.class);

		indexableActionableDynamicQuery.setPrimaryKeyPropertyName(
			"readingTimeEntryId");

		return indexableActionableDynamicQuery;
	}

	protected void initActionableDynamicQuery(
		ActionableDynamicQuery actionableDynamicQuery) {

		actionableDynamicQuery.setBaseLocalService(
			readingTimeEntryLocalService);
		actionableDynamicQuery.setClassLoader(getClassLoader());
		actionableDynamicQuery.setModelClass(ReadingTimeEntry.class);

		actionableDynamicQuery.setPrimaryKeyPropertyName("readingTimeEntryId");
	}

	@Override
	public ExportActionableDynamicQuery getExportActionableDynamicQuery(
		final PortletDataContext portletDataContext) {

		final ExportActionableDynamicQuery exportActionableDynamicQuery =
			new ExportActionableDynamicQuery() {

				@Override
				public long performCount() throws PortalException {
					ManifestSummary manifestSummary =
						portletDataContext.getManifestSummary();

					StagedModelType stagedModelType = getStagedModelType();

					long modelAdditionCount = super.performCount();

					manifestSummary.addModelAdditionCount(
						stagedModelType, modelAdditionCount);

					long modelDeletionCount =
						ExportImportHelperUtil.getModelDeletionCount(
							portletDataContext, stagedModelType);

					manifestSummary.addModelDeletionCount(
						stagedModelType, modelDeletionCount);

					return modelAdditionCount;
				}

			};

		initActionableDynamicQuery(exportActionableDynamicQuery);

		exportActionableDynamicQuery.setAddCriteriaMethod(
			new ActionableDynamicQuery.AddCriteriaMethod() {

				@Override
				public void addCriteria(DynamicQuery dynamicQuery) {
					portletDataContext.addDateRangeCriteria(
						dynamicQuery, "modifiedDate");

					StagedModelType stagedModelType =
						exportActionableDynamicQuery.getStagedModelType();

					long referrerClassNameId =
						stagedModelType.getReferrerClassNameId();

					Property classNameIdProperty = PropertyFactoryUtil.forName(
						"classNameId");

					if ((referrerClassNameId !=
							StagedModelType.REFERRER_CLASS_NAME_ID_ALL) &&
						(referrerClassNameId !=
							StagedModelType.REFERRER_CLASS_NAME_ID_ANY)) {

						dynamicQuery.add(
							classNameIdProperty.eq(
								stagedModelType.getReferrerClassNameId()));
					}
					else if (referrerClassNameId ==
								StagedModelType.REFERRER_CLASS_NAME_ID_ANY) {

						dynamicQuery.add(classNameIdProperty.isNotNull());
					}
				}

			});

		exportActionableDynamicQuery.setCompanyId(
			portletDataContext.getCompanyId());

		exportActionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod<ReadingTimeEntry>() {

				@Override
				public void performAction(ReadingTimeEntry readingTimeEntry)
					throws PortalException {

					StagedModelDataHandlerUtil.exportStagedModel(
						portletDataContext, readingTimeEntry);
				}

			});
		exportActionableDynamicQuery.setStagedModelType(
			new StagedModelType(
				PortalUtil.getClassNameId(ReadingTimeEntry.class.getName()),
				StagedModelType.REFERRER_CLASS_NAME_ID_ALL));

		return exportActionableDynamicQuery;
	}

	/**
	 * @throws PortalException
	 */
	public PersistedModel createPersistedModel(Serializable primaryKeyObj)
		throws PortalException {

		return readingTimeEntryPersistence.create(primaryKeyObj);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public PersistedModel deletePersistedModel(PersistedModel persistedModel)
		throws PortalException {

		return readingTimeEntryLocalService.deleteReadingTimeEntry(
			(ReadingTimeEntry)persistedModel);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public PersistedModel getPersistedModel(Serializable primaryKeyObj)
		throws PortalException {

		return readingTimeEntryPersistence.findByPrimaryKey(primaryKeyObj);
	}

	/**
	 * Returns all the reading time entries matching the UUID and company.
	 *
	 * @param uuid the UUID of the reading time entries
	 * @param companyId the primary key of the company
	 * @return the matching reading time entries, or an empty list if no matches were found
	 */
	@Override
	public List<ReadingTimeEntry> getReadingTimeEntriesByUuidAndCompanyId(
		String uuid, long companyId) {

		return readingTimeEntryPersistence.findByUuid_C(uuid, companyId);
	}

	/**
	 * Returns a range of reading time entries matching the UUID and company.
	 *
	 * @param uuid the UUID of the reading time entries
	 * @param companyId the primary key of the company
	 * @param start the lower bound of the range of reading time entries
	 * @param end the upper bound of the range of reading time entries (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the range of matching reading time entries, or an empty list if no matches were found
	 */
	@Override
	public List<ReadingTimeEntry> getReadingTimeEntriesByUuidAndCompanyId(
		String uuid, long companyId, int start, int end,
		OrderByComparator<ReadingTimeEntry> orderByComparator) {

		return readingTimeEntryPersistence.findByUuid_C(
			uuid, companyId, start, end, orderByComparator);
	}

	/**
	 * Returns the reading time entry matching the UUID and group.
	 *
	 * @param uuid the reading time entry's UUID
	 * @param groupId the primary key of the group
	 * @return the matching reading time entry
	 * @throws PortalException if a matching reading time entry could not be found
	 */
	@Override
	public ReadingTimeEntry getReadingTimeEntryByUuidAndGroupId(
			String uuid, long groupId)
		throws PortalException {

		return readingTimeEntryPersistence.findByUUID_G(uuid, groupId);
	}

	/**
	 * Returns a range of all the reading time entries.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.reading.time.model.impl.ReadingTimeEntryModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of reading time entries
	 * @param end the upper bound of the range of reading time entries (not inclusive)
	 * @return the range of reading time entries
	 */
	@Override
	public List<ReadingTimeEntry> getReadingTimeEntries(int start, int end) {
		return readingTimeEntryPersistence.findAll(start, end);
	}

	/**
	 * Returns the number of reading time entries.
	 *
	 * @return the number of reading time entries
	 */
	@Override
	public int getReadingTimeEntriesCount() {
		return readingTimeEntryPersistence.countAll();
	}

	/**
	 * Updates the reading time entry in the database or adds it if it does not yet exist. Also notifies the appropriate model listeners.
	 *
	 * @param readingTimeEntry the reading time entry
	 * @return the reading time entry that was updated
	 */
	@Indexable(type = IndexableType.REINDEX)
	@Override
	public ReadingTimeEntry updateReadingTimeEntry(
		ReadingTimeEntry readingTimeEntry) {

		return readingTimeEntryPersistence.update(readingTimeEntry);
	}

	@Override
	public Class<?>[] getAopInterfaces() {
		return new Class<?>[] {
			ReadingTimeEntryLocalService.class, IdentifiableOSGiService.class,
			PersistedModelLocalService.class
		};
	}

	@Override
	public void setAopProxy(Object aopProxy) {
		readingTimeEntryLocalService = (ReadingTimeEntryLocalService)aopProxy;
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	@Override
	public String getOSGiServiceIdentifier() {
		return ReadingTimeEntryLocalService.class.getName();
	}

	protected Class<?> getModelClass() {
		return ReadingTimeEntry.class;
	}

	protected String getModelClassName() {
		return ReadingTimeEntry.class.getName();
	}

	/**
	 * Performs a SQL query.
	 *
	 * @param sql the sql query
	 */
	protected void runSQL(String sql) {
		try {
			DataSource dataSource = readingTimeEntryPersistence.getDataSource();

			DB db = DBManagerUtil.getDB();

			sql = db.buildSQL(sql);
			sql = PortalUtil.transformSQL(sql);

			SqlUpdate sqlUpdate = SqlUpdateFactoryUtil.getSqlUpdate(
				dataSource, sql);

			sqlUpdate.update();
		}
		catch (Exception exception) {
			throw new SystemException(exception);
		}
	}

	protected ReadingTimeEntryLocalService readingTimeEntryLocalService;

	@Reference
	protected ReadingTimeEntryPersistence readingTimeEntryPersistence;

	@Reference
	protected com.liferay.counter.kernel.service.CounterLocalService
		counterLocalService;

}