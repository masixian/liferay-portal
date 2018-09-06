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

package com.liferay.structured.content.apio.internal.architect.resource;

import static com.liferay.portal.apio.idempotent.Idempotent.idempotent;

import com.liferay.aggregate.rating.apio.architect.identifier.AggregateRatingIdentifier;
import com.liferay.apio.architect.functional.Try;
import com.liferay.apio.architect.pagination.PageItems;
import com.liferay.apio.architect.pagination.Pagination;
import com.liferay.apio.architect.representor.Representor;
import com.liferay.apio.architect.resource.NestedCollectionResource;
import com.liferay.apio.architect.routes.ItemRoutes;
import com.liferay.apio.architect.routes.NestedCollectionRoutes;
import com.liferay.asset.kernel.AssetRendererFactoryRegistryUtil;
import com.liferay.asset.kernel.model.AssetRenderer;
import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.model.DDMFormValuesReader;
import com.liferay.asset.kernel.service.AssetTagLocalService;
import com.liferay.category.apio.architect.identifier.CategoryIdentifier;
import com.liferay.comment.apio.architect.identifier.CommentIdentifier;
import com.liferay.content.space.apio.architect.identifier.ContentSpaceIdentifier;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.dynamic.data.mapping.kernel.DDMFormFieldValue;
import com.liferay.dynamic.data.mapping.kernel.DDMFormValues;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalArticleConstants;
import com.liferay.journal.model.JournalArticleDisplay;
import com.liferay.journal.service.JournalArticleService;
import com.liferay.journal.util.JournalContent;
import com.liferay.journal.util.comparator.ArticleTitleComparator;
import com.liferay.media.object.apio.architect.identifier.MediaObjectIdentifier;
import com.liferay.person.apio.architect.identifier.PersonIdentifier;
import com.liferay.portal.apio.identifier.ClassNameClassPK;
import com.liferay.portal.apio.permission.HasPermission;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.structure.apio.architect.identifier.ContentStructureIdentifier;
import com.liferay.structured.content.apio.architect.filter.Filter;
import com.liferay.structured.content.apio.architect.filter.expression.Expression;
import com.liferay.structured.content.apio.architect.filter.expression.ExpressionVisitException;
import com.liferay.structured.content.apio.architect.identifier.StructuredContentIdentifier;
import com.liferay.structured.content.apio.architect.sort.Sort;
import com.liferay.structured.content.apio.architect.sort.SortField;
import com.liferay.structured.content.apio.architect.util.StructuredContentUtil;
import com.liferay.structured.content.apio.internal.architect.filter.ExpressionVisitorImpl;
import com.liferay.structured.content.apio.internal.architect.filter.InvalidFilterException;
import com.liferay.structured.content.apio.internal.architect.form.StructuredContentCreatorForm;
import com.liferay.structured.content.apio.internal.architect.form.StructuredContentUpdaterForm;
import com.liferay.structured.content.apio.internal.model.JournalArticleWrapper;
import com.liferay.structured.content.apio.internal.model.RenderedJournalArticle;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Provides the information necessary to expose a StructuredContent resources
 * through a web API. The resources are mapped from the internal model {@code
 * JournalArticle}.
 *
 * @author Javier Gamarra
 */
@Component(immediate = true)
public class StructuredContentNestedCollectionResource
	implements NestedCollectionResource
		<JournalArticleWrapper, Long, StructuredContentIdentifier, Long,
		 ContentSpaceIdentifier> {

	@Override
	public NestedCollectionRoutes<JournalArticleWrapper, Long, Long>
		collectionRoutes(
			NestedCollectionRoutes.Builder<JournalArticleWrapper, Long, Long>
				builder) {

		return builder.addGetter(
			this::_getPageItems, ThemeDisplay.class, Filter.class, Sort.class
		).addCreator(
			this::_addJournalArticle, ThemeDisplay.class,
			_hasPermission.forAddingIn(ContentSpaceIdentifier.class),
			StructuredContentCreatorForm::buildForm
		).build();
	}

	@Override
	public String getName() {
		return "structured-contents";
	}

	@Override
	public ItemRoutes<JournalArticleWrapper, Long> itemRoutes(
		ItemRoutes.Builder<JournalArticleWrapper, Long> builder) {

		return builder.addGetter(
			this::_getJournalArticleWrapper, ThemeDisplay.class
		).addRemover(
			idempotent(this::_deleteJournalArticle), _hasPermission::forDeleting
		).addUpdater(
			this::_updateJournalArticle, ThemeDisplay.class,
			_hasPermission::forUpdating, StructuredContentUpdaterForm::buildForm
		).build();
	}

	@Override
	public Representor<JournalArticleWrapper> representor(
		Representor.Builder<JournalArticleWrapper, Long> builder) {

		return builder.types(
			"StructuredContent"
		).identifier(
			JournalArticle::getId
		).addBidirectionalModel(
			"contentSpace", "structuredContents", ContentSpaceIdentifier.class,
			JournalArticle::getGroupId
		).addDate(
			"dateCreated", JournalArticle::getCreateDate
		).addDate(
			"dateModified", JournalArticle::getModifiedDate
		).addDate(
			"datePublished", JournalArticle::getDisplayDate
		).addDate(
			"lastReviewed", JournalArticle::getReviewDate
		).addLinkedModel(
			"aggregateRating", AggregateRatingIdentifier.class,
			this::_createClassNameClassPK
		).addLinkedModel(
			"contentStructure", ContentStructureIdentifier.class,
			this::_getJournalArticleStructureId
		).addLinkedModel(
			"creator", PersonIdentifier.class, JournalArticle::getUserId
		).addLocalizedStringByLocale(
			"description", JournalArticleWrapper::getDescription
		).addLocalizedStringByLocale(
			"title", JournalArticle::getTitle
		).addNestedList(
			"renderedContentsByTemplate", this::_getRenderedJournalArticles,
			nestedBuilder -> nestedBuilder.types(
				"templates"
			).addLocalizedStringByLocale(
				"template", RenderedJournalArticle::getTemplateName
			).addLocalizedStringByLocale(
				"renderedContent", RenderedJournalArticle::getRenderedContent
			).build()
		).addNestedList(
			"values", this::_getJournalArticleDDMFormFieldValues,
			fieldValuesBuilder -> fieldValuesBuilder.types(
				"ContentFieldValue"
			).addLinkedModel(
				"mediaObject", MediaObjectIdentifier.class,
				ddmFormFieldValue -> Try.fromFallible(
					ddmFormFieldValue::getValue
				).map(
					value -> value.getString(LocaleUtil.getDefault())
				).map(
					string -> StructuredContentUtil.getFileEntryId(
						string, _dlAppService)
				).orElse(
					null
				)
			).addLinkedModel(
				"structuredContent", StructuredContentIdentifier.class,
				this::_getStructuredContentId
			).addLocalizedStringByLocale(
				"value", this::_getLocalizedString
			).addNested(
				"geo", this::_getGeoJSONObject,
				geoBuilder -> geoBuilder.types(
					"GeoCoordinates"
				).addNumber(
					"latitude", jsonObject -> jsonObject.getDouble("latitude")
				).addNumber(
					"longitude", jsonObject -> jsonObject.getDouble("longitude")
				).build()
			).addRelativeURL(
				"link", this::_getLink
			).addString(
				"name", DDMFormFieldValue::getName
			).build()
		).addRelatedCollection(
			"category", CategoryIdentifier.class
		).addRelatedCollection(
			"comment", CommentIdentifier.class
		).addStringList(
			"availableLanguages",
			journalArticle -> Arrays.asList(
				journalArticle.getAvailableLanguageIds())
		).addStringList(
			"keywords", this::_getJournalArticleAssetTags
		).build();
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> getFilterFieldsMap(Filter filter) {
		if (filter == null) {
			return Collections.emptyMap();
		}

		try {
			Expression expression = filter.getExpression();

			return (Map<String, Object>)expression.accept(
				new ExpressionVisitorImpl());
		}
		catch (ExpressionVisitException eve) {
			throw new InvalidFilterException(
				"Invalid filter: " + eve.getMessage(), eve);
		}
	}

	private JournalArticleWrapper _addJournalArticle(
			long contentSpaceId,
			StructuredContentCreatorForm structuredContentCreatorForm,
			ThemeDisplay themeDisplay)
		throws PortalException {

		Locale locale = themeDisplay.getLocale();

		ServiceContext serviceContext =
			structuredContentCreatorForm.getServiceContext(contentSpaceId);

		JournalArticle journalArticle = _journalArticleService.addArticle(
			contentSpaceId, 0, 0, 0, null, true,
			structuredContentCreatorForm.getTitleMap(locale),
			structuredContentCreatorForm.getDescriptionMap(locale),
			structuredContentCreatorForm.getText(),
			structuredContentCreatorForm.getStructure(),
			structuredContentCreatorForm.getTemplate(), null,
			structuredContentCreatorForm.getDisplayDateMonth(),
			structuredContentCreatorForm.getDisplayDateDay(),
			structuredContentCreatorForm.getDisplayDateYear(),
			structuredContentCreatorForm.getDisplayDateHour(),
			structuredContentCreatorForm.getDisplayDateMinute(), 0, 0, 0, 0, 0,
			true, 0, 0, 0, 0, 0, true, true, null, serviceContext);

		return new JournalArticleWrapper(journalArticle, themeDisplay);
	}

	private ClassNameClassPK _createClassNameClassPK(
		JournalArticle journalArticle) {

		return ClassNameClassPK.create(
			JournalArticle.class.getName(),
			journalArticle.getResourcePrimKey());
	}

	private void _deleteJournalArticle(long journalArticleId)
		throws PortalException {

		JournalArticle journalArticle = _journalArticleService.getArticle(
			journalArticleId);

		_journalArticleService.deleteArticle(
			journalArticle.getGroupId(), journalArticle.getArticleId(),
			journalArticle.getArticleResourceUuid(), new ServiceContext());
	}

	private List<DDMFormFieldValue> _getFormFieldValues(
		List<DDMFormFieldValue> ddmFormFieldValues) {

		Stream<DDMFormFieldValue> ddmFormFieldValueStream =
			ddmFormFieldValues.stream();

		List<DDMFormFieldValue> nestedDDMFormFieldValues =
			ddmFormFieldValueStream.flatMap(
				ddmFormFieldValue -> _getFormFieldValues(
					ddmFormFieldValue.getNestedDDMFormFieldValues()
				).stream()
			).collect(
				Collectors.toList()
			);

		nestedDDMFormFieldValues.addAll(ddmFormFieldValues);

		return nestedDDMFormFieldValues;
	}

	private JSONObject _getGeoJSONObject(DDMFormFieldValue ddmFormFieldValue) {
		return Try.fromFallible(
			ddmFormFieldValue::getValue
		).map(
			value -> value.getString(LocaleUtil.getDefault())
		).filter(
			StructuredContentUtil::isJSONObject
		).filter(
			string -> string.contains("latitude")
		).map(
			JSONFactoryUtil::createJSONObject
		).orElse(
			null
		);
	}

	private JournalArticle _getJournalArticle(JSONObject jsonObject)
		throws PortalException {

		long classPK = jsonObject.getLong("classPK");

		return _journalArticleService.getLatestArticle(classPK);
	}

	private List<String> _getJournalArticleAssetTags(
		JournalArticle journalArticle) {

		List<AssetTag> assetTags = _assetTagLocalService.getTags(
			JournalArticle.class.getName(),
			journalArticle.getResourcePrimKey());

		return ListUtil.toList(assetTags, AssetTag::getName);
	}

	private List<DDMFormFieldValue> _getJournalArticleDDMFormFieldValues(
		JournalArticleWrapper journalArticleWrapper) {

		return Try.fromFallible(
			() ->
				AssetRendererFactoryRegistryUtil.getAssetRendererFactoryByClass(
					JournalArticle.class)
		).map(
			assetRendererFactory -> assetRendererFactory.getAssetRenderer(
				journalArticleWrapper,
				AssetRendererFactory.TYPE_LATEST_APPROVED)
		).map(
			AssetRenderer::getDDMFormValuesReader
		).map(
			DDMFormValuesReader::getDDMFormValues
		).map(
			DDMFormValues::getDDMFormFieldValues
		).map(
			this::_getFormFieldValues
		).orElse(
			null
		);
	}

	private OrderByComparator<JournalArticle>
		_getJournalArticleOrderByComparator(List<SortField> sortFields) {

		OrderByComparator<JournalArticle> orderByComparator = null;

		for (SortField sortField : sortFields) {
			String fieldName = sortField.getFieldName();

			if (fieldName.equals("title")) {
				orderByComparator = new ArticleTitleComparator(
					sortField.isAscending());

				break;
			}
		}

		return orderByComparator;
	}

	private Long _getJournalArticleStructureId(
		JournalArticleWrapper journalArticleWrapper) {

		DDMStructure ddmStructure = journalArticleWrapper.getDDMStructure();

		return ddmStructure.getStructureId();
	}

	private JournalArticleWrapper _getJournalArticleWrapper(
			long journalArticleId, ThemeDisplay themeDisplay)
		throws PortalException {

		JournalArticle journalArticle = _journalArticleService.getArticle(
			journalArticleId);

		return new JournalArticleWrapper(journalArticle, themeDisplay);
	}

	private String _getLayoutLink(JSONObject jsonObject)
		throws PortalException {

		long groupId = jsonObject.getLong("groupId");
		boolean privateLayout = jsonObject.getBoolean("privateLayout");
		long layoutId = jsonObject.getLong("layoutId");

		Layout layoutByUuidAndGroupId = _layoutLocalService.getLayout(
			groupId, privateLayout, layoutId);

		return layoutByUuidAndGroupId.getFriendlyURL();
	}

	private String _getLink(DDMFormFieldValue ddmFormFieldValue) {
		return Try.fromFallible(
			ddmFormFieldValue::getValue
		).map(
			value -> value.getString(LocaleUtil.getDefault())
		).filter(
			StructuredContentUtil::isJSONObject
		).filter(
			string -> string.contains("layoutId")
		).map(
			JSONFactoryUtil::createJSONObject
		).map(
			this::_getLayoutLink
		).orElse(
			null
		);
	}

	private String _getLocalizedString(
		DDMFormFieldValue ddmFormFieldValue, Locale locale) {

		return Try.fromFallible(
			ddmFormFieldValue::getValue
		).map(
			value -> value.getString(locale)
		).filter(
			valueString -> !StructuredContentUtil.isJSONObject(valueString)
		).orElse(
			null
		);
	}

	private PageItems<JournalArticleWrapper> _getPageItems(
		Pagination pagination, long contentSpaceId, ThemeDisplay themeDisplay,
		Filter filter, Sort sort) {

		OrderByComparator<JournalArticle> orderByComparator =
			_getJournalArticleOrderByComparator(sort.getSortFields());

		Map<String, Object> filterFieldsMap = getFilterFieldsMap(filter);

		List<JournalArticleWrapper> journalArticleWrappers = Stream.of(
			_journalArticleService.search(
				themeDisplay.getCompanyId(), contentSpaceId,
				Collections.emptyList(),
				JournalArticleConstants.CLASSNAME_ID_DEFAULT, null, null,
				(String)filterFieldsMap.get("title"), null, null, new String[0],
				new String[0], null, null, WorkflowConstants.STATUS_APPROVED,
				null, true, pagination.getStartPosition(),
				pagination.getEndPosition(), orderByComparator)
		).flatMap(
			List::stream
		).map(
			journalArticle -> new JournalArticleWrapper(
				journalArticle, themeDisplay)
		).collect(
			Collectors.toList()
		);

		int count = _journalArticleService.searchCount(
			themeDisplay.getCompanyId(), contentSpaceId,
			Collections.emptyList(),
			JournalArticleConstants.CLASSNAME_ID_DEFAULT, null, null,
			(String)filterFieldsMap.get("title"), null, null, new String[0],
			new String[0], null, null, WorkflowConstants.STATUS_APPROVED, null,
			true);

		return new PageItems<>(journalArticleWrappers, count);
	}

	private String _getRenderedContent(
		JournalArticleWrapper journalArticleWrapper, DDMTemplate ddmTemplate,
		Locale locale) {

		return Try.fromFallible(
			() -> _journalContent.getDisplay(
				journalArticleWrapper.getGroupId(),
				journalArticleWrapper.getArticleId(),
				ddmTemplate.getTemplateKey(), null, locale.getLanguage(),
				journalArticleWrapper.getThemeDisplay())
		).map(
			JournalArticleDisplay::getContent
		).map(
			content -> content.replaceAll("[\\t\\n]", "")
		).orElse(
			null
		);
	}

	private List<RenderedJournalArticle> _getRenderedJournalArticles(
		JournalArticleWrapper journalArticleWrapper) {

		DDMStructure ddmStructure = journalArticleWrapper.getDDMStructure();

		return Stream.of(
			ddmStructure.getTemplates()
		).flatMap(
			List::stream
		).map(
			ddmTemplate -> RenderedJournalArticle.create(
				ddmTemplate::getName,
				locale -> _getRenderedContent(
					journalArticleWrapper, ddmTemplate, locale))
		).collect(
			Collectors.toList()
		);
	}

	private Long _getStructuredContentId(DDMFormFieldValue ddmFormFieldValue) {
		return Try.fromFallible(
			ddmFormFieldValue::getValue
		).map(
			value -> value.getString(LocaleUtil.getDefault())
		).filter(
			StructuredContentUtil::isJSONObject
		).map(
			JSONFactoryUtil::createJSONObject
		).map(
			this::_getJournalArticle
		).map(
			JournalArticle::getId
		).orElse(
			null
		);
	}

	private JournalArticleWrapper _updateJournalArticle(
			long journalArticleId,
			StructuredContentUpdaterForm structuredContentUpdaterForm,
			ThemeDisplay themeDisplay)
		throws PortalException {

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setAddGroupPermissions(true);
		serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(structuredContentUpdaterForm.getGroup());

		JournalArticle journalArticle = _journalArticleService.updateArticle(
			structuredContentUpdaterForm.getUser(),
			structuredContentUpdaterForm.getGroup(), 0,
			String.valueOf(journalArticleId),
			structuredContentUpdaterForm.getVersion(),
			structuredContentUpdaterForm.getTitleMap(),
			structuredContentUpdaterForm.getDescriptionMap(),
			structuredContentUpdaterForm.getText(), null, serviceContext);

		return new JournalArticleWrapper(journalArticle, themeDisplay);
	}

	@Reference
	private AssetTagLocalService _assetTagLocalService;

	@Reference
	private DLAppService _dlAppService;

	@Reference(
		target = "(model.class.name=com.liferay.journal.model.JournalArticle)"
	)
	private HasPermission<Long> _hasPermission;

	@Reference
	private JournalArticleService _journalArticleService;

	@Reference
	private JournalContent _journalContent;

	@Reference
	private LayoutLocalService _layoutLocalService;

}