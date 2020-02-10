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

package com.liferay.knowledge.base.service.base;

import com.liferay.exportimport.kernel.lar.ExportImportHelperUtil;
import com.liferay.exportimport.kernel.lar.ManifestSummary;
import com.liferay.exportimport.kernel.lar.PortletDataContext;
import com.liferay.exportimport.kernel.lar.StagedModelDataHandlerUtil;
import com.liferay.exportimport.kernel.lar.StagedModelType;
import com.liferay.knowledge.base.model.KBFolder;
import com.liferay.knowledge.base.service.KBFolderLocalService;
import com.liferay.knowledge.base.service.persistence.KBArticleFinder;
import com.liferay.knowledge.base.service.persistence.KBArticlePersistence;
import com.liferay.knowledge.base.service.persistence.KBCommentPersistence;
import com.liferay.knowledge.base.service.persistence.KBFolderFinder;
import com.liferay.knowledge.base.service.persistence.KBFolderPersistence;
import com.liferay.knowledge.base.service.persistence.KBTemplatePersistence;
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

import java.io.Serializable;

import java.util.List;

import javax.sql.DataSource;

import org.osgi.service.component.annotations.Reference;

/**
 * Provides the base implementation for the kb folder local service.
 *
 * <p>
 * This implementation exists only as a container for the default service methods generated by ServiceBuilder. All custom service methods should be put in {@link com.liferay.knowledge.base.service.impl.KBFolderLocalServiceImpl}.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.liferay.knowledge.base.service.impl.KBFolderLocalServiceImpl
 * @generated
 */
public abstract class KBFolderLocalServiceBaseImpl
	extends BaseLocalServiceImpl
	implements AopService, IdentifiableOSGiService, KBFolderLocalService {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. Use <code>KBFolderLocalService</code> via injection or a <code>org.osgi.util.tracker.ServiceTracker</code> or use <code>com.liferay.knowledge.base.service.KBFolderLocalServiceUtil</code>.
	 */

	/**
	 * Adds the kb folder to the database. Also notifies the appropriate model listeners.
	 *
	 * @param kbFolder the kb folder
	 * @return the kb folder that was added
	 */
	@Indexable(type = IndexableType.REINDEX)
	@Override
	public KBFolder addKBFolder(KBFolder kbFolder) {
		kbFolder.setNew(true);

		return kbFolderPersistence.update(kbFolder);
	}

	/**
	 * Creates a new kb folder with the primary key. Does not add the kb folder to the database.
	 *
	 * @param kbFolderId the primary key for the new kb folder
	 * @return the new kb folder
	 */
	@Override
	@Transactional(enabled = false)
	public KBFolder createKBFolder(long kbFolderId) {
		return kbFolderPersistence.create(kbFolderId);
	}

	/**
	 * Deletes the kb folder with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param kbFolderId the primary key of the kb folder
	 * @return the kb folder that was removed
	 * @throws PortalException if a kb folder with the primary key could not be found
	 */
	@Indexable(type = IndexableType.DELETE)
	@Override
	public KBFolder deleteKBFolder(long kbFolderId) throws PortalException {
		return kbFolderPersistence.remove(kbFolderId);
	}

	/**
	 * Deletes the kb folder from the database. Also notifies the appropriate model listeners.
	 *
	 * @param kbFolder the kb folder
	 * @return the kb folder that was removed
	 */
	@Indexable(type = IndexableType.DELETE)
	@Override
	public KBFolder deleteKBFolder(KBFolder kbFolder) {
		return kbFolderPersistence.remove(kbFolder);
	}

	@Override
	public DynamicQuery dynamicQuery() {
		Class<?> clazz = getClass();

		return DynamicQueryFactoryUtil.forClass(
			KBFolder.class, clazz.getClassLoader());
	}

	/**
	 * Performs a dynamic query on the database and returns the matching rows.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the matching rows
	 */
	@Override
	public <T> List<T> dynamicQuery(DynamicQuery dynamicQuery) {
		return kbFolderPersistence.findWithDynamicQuery(dynamicQuery);
	}

	/**
	 * Performs a dynamic query on the database and returns a range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.knowledge.base.model.impl.KBFolderModelImpl</code>.
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

		return kbFolderPersistence.findWithDynamicQuery(
			dynamicQuery, start, end);
	}

	/**
	 * Performs a dynamic query on the database and returns an ordered range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.knowledge.base.model.impl.KBFolderModelImpl</code>.
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

		return kbFolderPersistence.findWithDynamicQuery(
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
		return kbFolderPersistence.countWithDynamicQuery(dynamicQuery);
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

		return kbFolderPersistence.countWithDynamicQuery(
			dynamicQuery, projection);
	}

	@Override
	public KBFolder fetchKBFolder(long kbFolderId) {
		return kbFolderPersistence.fetchByPrimaryKey(kbFolderId);
	}

	/**
	 * Returns the kb folder matching the UUID and group.
	 *
	 * @param uuid the kb folder's UUID
	 * @param groupId the primary key of the group
	 * @return the matching kb folder, or <code>null</code> if a matching kb folder could not be found
	 */
	@Override
	public KBFolder fetchKBFolderByUuidAndGroupId(String uuid, long groupId) {
		return kbFolderPersistence.fetchByUUID_G(uuid, groupId);
	}

	/**
	 * Returns the kb folder with the primary key.
	 *
	 * @param kbFolderId the primary key of the kb folder
	 * @return the kb folder
	 * @throws PortalException if a kb folder with the primary key could not be found
	 */
	@Override
	public KBFolder getKBFolder(long kbFolderId) throws PortalException {
		return kbFolderPersistence.findByPrimaryKey(kbFolderId);
	}

	@Override
	public ActionableDynamicQuery getActionableDynamicQuery() {
		ActionableDynamicQuery actionableDynamicQuery =
			new DefaultActionableDynamicQuery();

		actionableDynamicQuery.setBaseLocalService(kbFolderLocalService);
		actionableDynamicQuery.setClassLoader(getClassLoader());
		actionableDynamicQuery.setModelClass(KBFolder.class);

		actionableDynamicQuery.setPrimaryKeyPropertyName("kbFolderId");

		return actionableDynamicQuery;
	}

	@Override
	public IndexableActionableDynamicQuery
		getIndexableActionableDynamicQuery() {

		IndexableActionableDynamicQuery indexableActionableDynamicQuery =
			new IndexableActionableDynamicQuery();

		indexableActionableDynamicQuery.setBaseLocalService(
			kbFolderLocalService);
		indexableActionableDynamicQuery.setClassLoader(getClassLoader());
		indexableActionableDynamicQuery.setModelClass(KBFolder.class);

		indexableActionableDynamicQuery.setPrimaryKeyPropertyName("kbFolderId");

		return indexableActionableDynamicQuery;
	}

	protected void initActionableDynamicQuery(
		ActionableDynamicQuery actionableDynamicQuery) {

		actionableDynamicQuery.setBaseLocalService(kbFolderLocalService);
		actionableDynamicQuery.setClassLoader(getClassLoader());
		actionableDynamicQuery.setModelClass(KBFolder.class);

		actionableDynamicQuery.setPrimaryKeyPropertyName("kbFolderId");
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
				}

			});

		exportActionableDynamicQuery.setCompanyId(
			portletDataContext.getCompanyId());

		exportActionableDynamicQuery.setGroupId(
			portletDataContext.getScopeGroupId());

		exportActionableDynamicQuery.setPerformActionMethod(
			new ActionableDynamicQuery.PerformActionMethod<KBFolder>() {

				@Override
				public void performAction(KBFolder kbFolder)
					throws PortalException {

					StagedModelDataHandlerUtil.exportStagedModel(
						portletDataContext, kbFolder);
				}

			});
		exportActionableDynamicQuery.setStagedModelType(
			new StagedModelType(
				PortalUtil.getClassNameId(KBFolder.class.getName())));

		return exportActionableDynamicQuery;
	}

	/**
	 * @throws PortalException
	 */
	public PersistedModel createPersistedModel(Serializable primaryKeyObj)
		throws PortalException {

		return kbFolderPersistence.create(primaryKeyObj);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public PersistedModel deletePersistedModel(PersistedModel persistedModel)
		throws PortalException {

		return kbFolderLocalService.deleteKBFolder((KBFolder)persistedModel);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public PersistedModel getPersistedModel(Serializable primaryKeyObj)
		throws PortalException {

		return kbFolderPersistence.findByPrimaryKey(primaryKeyObj);
	}

	/**
	 * Returns all the kb folders matching the UUID and company.
	 *
	 * @param uuid the UUID of the kb folders
	 * @param companyId the primary key of the company
	 * @return the matching kb folders, or an empty list if no matches were found
	 */
	@Override
	public List<KBFolder> getKBFoldersByUuidAndCompanyId(
		String uuid, long companyId) {

		return kbFolderPersistence.findByUuid_C(uuid, companyId);
	}

	/**
	 * Returns a range of kb folders matching the UUID and company.
	 *
	 * @param uuid the UUID of the kb folders
	 * @param companyId the primary key of the company
	 * @param start the lower bound of the range of kb folders
	 * @param end the upper bound of the range of kb folders (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the range of matching kb folders, or an empty list if no matches were found
	 */
	@Override
	public List<KBFolder> getKBFoldersByUuidAndCompanyId(
		String uuid, long companyId, int start, int end,
		OrderByComparator<KBFolder> orderByComparator) {

		return kbFolderPersistence.findByUuid_C(
			uuid, companyId, start, end, orderByComparator);
	}

	/**
	 * Returns the kb folder matching the UUID and group.
	 *
	 * @param uuid the kb folder's UUID
	 * @param groupId the primary key of the group
	 * @return the matching kb folder
	 * @throws PortalException if a matching kb folder could not be found
	 */
	@Override
	public KBFolder getKBFolderByUuidAndGroupId(String uuid, long groupId)
		throws PortalException {

		return kbFolderPersistence.findByUUID_G(uuid, groupId);
	}

	/**
	 * Returns a range of all the kb folders.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.knowledge.base.model.impl.KBFolderModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of kb folders
	 * @param end the upper bound of the range of kb folders (not inclusive)
	 * @return the range of kb folders
	 */
	@Override
	public List<KBFolder> getKBFolders(int start, int end) {
		return kbFolderPersistence.findAll(start, end);
	}

	/**
	 * Returns the number of kb folders.
	 *
	 * @return the number of kb folders
	 */
	@Override
	public int getKBFoldersCount() {
		return kbFolderPersistence.countAll();
	}

	/**
	 * Updates the kb folder in the database or adds it if it does not yet exist. Also notifies the appropriate model listeners.
	 *
	 * @param kbFolder the kb folder
	 * @return the kb folder that was updated
	 */
	@Indexable(type = IndexableType.REINDEX)
	@Override
	public KBFolder updateKBFolder(KBFolder kbFolder) {
		return kbFolderPersistence.update(kbFolder);
	}

	@Override
	public Class<?>[] getAopInterfaces() {
		return new Class<?>[] {
			KBFolderLocalService.class, IdentifiableOSGiService.class,
			PersistedModelLocalService.class
		};
	}

	@Override
	public void setAopProxy(Object aopProxy) {
		kbFolderLocalService = (KBFolderLocalService)aopProxy;
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	@Override
	public String getOSGiServiceIdentifier() {
		return KBFolderLocalService.class.getName();
	}

	protected Class<?> getModelClass() {
		return KBFolder.class;
	}

	protected String getModelClassName() {
		return KBFolder.class.getName();
	}

	/**
	 * Performs a SQL query.
	 *
	 * @param sql the sql query
	 */
	protected void runSQL(String sql) {
		try {
			DataSource dataSource = kbFolderPersistence.getDataSource();

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

	@Reference
	protected KBArticlePersistence kbArticlePersistence;

	@Reference
	protected KBArticleFinder kbArticleFinder;

	@Reference
	protected KBCommentPersistence kbCommentPersistence;

	protected KBFolderLocalService kbFolderLocalService;

	@Reference
	protected KBFolderPersistence kbFolderPersistence;

	@Reference
	protected KBFolderFinder kbFolderFinder;

	@Reference
	protected KBTemplatePersistence kbTemplatePersistence;

	@Reference
	protected com.liferay.counter.kernel.service.CounterLocalService
		counterLocalService;

	@Reference
	protected com.liferay.portal.kernel.service.ClassNameLocalService
		classNameLocalService;

	@Reference
	protected com.liferay.portal.kernel.service.ResourceLocalService
		resourceLocalService;

	@Reference
	protected com.liferay.portal.kernel.service.UserLocalService
		userLocalService;

	@Reference
	protected com.liferay.expando.kernel.service.ExpandoRowLocalService
		expandoRowLocalService;

}