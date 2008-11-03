/**
 * Copyright (c) 2000-2008 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet.messageboards.service.impl;

import com.liferay.documentlibrary.DuplicateDirectoryException;
import com.liferay.documentlibrary.DuplicateFileException;
import com.liferay.documentlibrary.NoSuchDirectoryException;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.mail.MailMessage;
import com.liferay.portal.kernel.messaging.DestinationNames;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ObjectValuePair;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.CompanyConstants;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.model.ModelHintsUtil;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.PrefsPropsUtil;
import com.liferay.portal.util.PropsKeys;
import com.liferay.portal.util.PropsValues;
import com.liferay.portlet.blogs.model.BlogsEntry;
import com.liferay.portlet.blogs.social.BlogsActivityKeys;
import com.liferay.portlet.messageboards.MessageBodyException;
import com.liferay.portlet.messageboards.MessageSubjectException;
import com.liferay.portlet.messageboards.NoSuchDiscussionException;
import com.liferay.portlet.messageboards.NoSuchThreadException;
import com.liferay.portlet.messageboards.RequiredMessageException;
import com.liferay.portlet.messageboards.model.MBCategory;
import com.liferay.portlet.messageboards.model.MBDiscussion;
import com.liferay.portlet.messageboards.model.MBMessage;
import com.liferay.portlet.messageboards.model.MBMessageDisplay;
import com.liferay.portlet.messageboards.model.MBStatsUser;
import com.liferay.portlet.messageboards.model.MBThread;
import com.liferay.portlet.messageboards.model.MBTreeWalker;
import com.liferay.portlet.messageboards.model.impl.MBMessageDisplayImpl;
import com.liferay.portlet.messageboards.model.impl.MBMessageImpl;
import com.liferay.portlet.messageboards.model.impl.MBThreadImpl;
import com.liferay.portlet.messageboards.model.impl.MBTreeWalkerImpl;
import com.liferay.portlet.messageboards.service.base.MBMessageLocalServiceBaseImpl;
import com.liferay.portlet.messageboards.social.MBActivityKeys;
import com.liferay.portlet.messageboards.util.Indexer;
import com.liferay.portlet.messageboards.util.MBUtil;
import com.liferay.portlet.messageboards.util.MailingListThreadLocal;
import com.liferay.portlet.messageboards.util.comparator.MessageThreadComparator;
import com.liferay.portlet.messageboards.util.comparator.ThreadLastPostDateComparator;
import com.liferay.portlet.social.model.SocialActivity;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.internet.InternetAddress;

import javax.portlet.PortletPreferences;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <a href="MBMessageLocalServiceImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @author Raymond Augé
 *
 */
public class MBMessageLocalServiceImpl extends MBMessageLocalServiceBaseImpl {

	public MBMessage addDiscussionMessage(
			long userId, String userName, String subject, String body)
		throws PortalException, SystemException {

		long groupId = 0;
		String className = StringPool.BLANK;
		long classPK = 0;
		long threadId = 0;
		long parentMessageId = 0;
		ThemeDisplay themeDisplay = null;

		return addDiscussionMessage(
			userId, userName, groupId, className, classPK, threadId,
			parentMessageId, subject, body, themeDisplay);
	}

	public MBMessage addDiscussionMessage(
			long userId, String userName, long groupId, String className,
			long classPK, long threadId, long parentMessageId, String subject,
			String body)
		throws PortalException, SystemException {

		ThemeDisplay themeDisplay = null;

		MBMessage message = addDiscussionMessage(
			userId, userName, groupId, className, classPK, threadId,
			parentMessageId, subject, body, themeDisplay);

		if (parentMessageId == MBMessageImpl.DEFAULT_PARENT_MESSAGE_ID) {
			long classNameId = PortalUtil.getClassNameId(className);

			MBDiscussion discussion = mbDiscussionPersistence.fetchByC_C(
				classNameId, classPK);

			if (discussion == null) {
				long discussionId = counterLocalService.increment();

				discussion = mbDiscussionPersistence.create(
					discussionId);

				discussion.setClassNameId(PortalUtil.getClassNameId(className));
				discussion.setClassPK(classPK);
			}

			discussion.setThreadId(message.getThreadId());

			mbDiscussionPersistence.update(discussion, false);
		}

		return message;
	}

	public MBMessage addDiscussionMessage(
			long userId, String userName, long groupId, String className,
			long classPK, long threadId, long parentMessageId, String subject,
			String body, ThemeDisplay themeDisplay)
		throws PortalException, SystemException {

		long categoryId = CompanyConstants.SYSTEM;

		if (Validator.isNull(subject)) {
			subject = "N/A";
		}

		List<ObjectValuePair<String, byte[]>> files =
			new ArrayList<ObjectValuePair<String, byte[]>>();
		boolean anonymous = false;
		double priority = 0.0;
		String[] tagsEntries = null;
		PortletPreferences preferences = null;
		boolean addCommunityPermissions = true;
		boolean addGuestPermissions = true;

		mbCategoryLocalService.getSystemCategory();

		MBMessage message = addMessage(
			userId, userName, categoryId, threadId, parentMessageId, subject,
			body, files, anonymous, priority, tagsEntries, preferences,
			addCommunityPermissions, addGuestPermissions, themeDisplay);

		if ((className.equals(BlogsEntry.class.getName())) &&
			(themeDisplay != null)) {

			// Social

			BlogsEntry entry = blogsEntryPersistence.findByPrimaryKey(classPK);

			JSONObject extraData = JSONFactoryUtil.createJSONObject();

			extraData.put("messageId", message.getMessageId());

			socialActivityLocalService.addActivity(
				userId, entry.getGroupId(), BlogsEntry.class.getName(),
				classPK, BlogsActivityKeys.ADD_COMMENT, extraData.toString(),
				entry.getUserId());

			// Email

			try {
				sendBlogsCommentsEmail(userId, entry, message, themeDisplay);
			}
			catch (Exception e) {
				_log.error(e, e);
			}
		}

		return message;
	}

	public MBMessage addMessage(
			long userId, String userName, long categoryId, String subject,
			String body, List<ObjectValuePair<String, byte[]>> files,
			boolean anonymous, double priority, String[] tagsEntries,
			PortletPreferences preferences, boolean addCommunityPermissions,
			boolean addGuestPermissions, ThemeDisplay themeDisplay)
		throws PortalException, SystemException {

		return addMessage(
			userId, userName, categoryId, subject, body, files, anonymous,
			priority, tagsEntries, preferences,
			Boolean.valueOf(addCommunityPermissions),
			Boolean.valueOf(addGuestPermissions), null, null, themeDisplay);
	}

	public MBMessage addMessage(
			long userId, String userName, long categoryId, String subject,
			String body, List<ObjectValuePair<String, byte[]>> files,
			boolean anonymous, double priority, String[] tagsEntries,
			PortletPreferences preferences, String[] communityPermissions,
			String[] guestPermissions, ThemeDisplay themeDisplay)
		throws PortalException, SystemException {

		return addMessage(
			userId, userName, categoryId, subject, body, files, anonymous,
			priority, tagsEntries, preferences, null, null,
			communityPermissions, guestPermissions, themeDisplay);
	}

	public MBMessage addMessage(
			long userId, String userName, long categoryId, String subject,
			String body, List<ObjectValuePair<String, byte[]>> files,
			boolean anonymous, double priority, String[] tagsEntries,
			PortletPreferences preferences, Boolean addCommunityPermissions,
			Boolean addGuestPermissions, String[] communityPermissions,
			String[] guestPermissions, ThemeDisplay themeDisplay)
		throws PortalException, SystemException {

		long threadId = 0;
		long parentMessageId = 0;

		return addMessage(
			null, userId, userName, categoryId, threadId, parentMessageId,
			subject, body, files, anonymous, priority, tagsEntries, preferences,
			addCommunityPermissions, addGuestPermissions, communityPermissions,
			guestPermissions, themeDisplay);
	}

	public MBMessage addMessage(
			long userId, String userName, long categoryId, long threadId,
			long parentMessageId, String subject, String body,
			List<ObjectValuePair<String, byte[]>> files, boolean anonymous,
			double priority, String[] tagsEntries,
			PortletPreferences preferences, boolean addCommunityPermissions,
			boolean addGuestPermissions, ThemeDisplay themeDisplay)
		throws PortalException, SystemException {

		return addMessage(
			null, userId, userName, categoryId, threadId, parentMessageId,
			subject, body, files, anonymous, priority, tagsEntries, preferences,
			Boolean.valueOf(addCommunityPermissions),
			Boolean.valueOf(addGuestPermissions), null, null, themeDisplay);
	}

	public MBMessage addMessage(
			long userId, String userName, long categoryId, long threadId,
			long parentMessageId, String subject, String body,
			List<ObjectValuePair<String, byte[]>> files, boolean anonymous,
			double priority, String[] tagsEntries,
			PortletPreferences preferences, String[] communityPermissions,
			String[] guestPermissions, ThemeDisplay themeDisplay)
		throws PortalException, SystemException {

		return addMessage(
			null, userId, userName, categoryId, threadId, parentMessageId,
			subject, body, files, anonymous, priority, tagsEntries, preferences,
			null, null, communityPermissions, guestPermissions, themeDisplay);
	}

	public MBMessage addMessage(
			String uuid, long userId, String userName, long categoryId,
			long threadId, long parentMessageId, String subject, String body,
			List<ObjectValuePair<String, byte[]>> files, boolean anonymous,
			double priority, String[] tagsEntries,
			PortletPreferences preferences, boolean addCommunityPermissions,
			boolean addGuestPermissions, ThemeDisplay themeDisplay)
		throws PortalException, SystemException {

		return addMessage(
			uuid, userId, userName, categoryId, threadId, parentMessageId,
			subject, body, files, anonymous, priority, tagsEntries, preferences,
			Boolean.valueOf(addCommunityPermissions),
			Boolean.valueOf(addGuestPermissions), null, null, themeDisplay);
	}

	public MBMessage addMessage(
			String uuid, long userId, String userName, long categoryId,
			long threadId, long parentMessageId, String subject, String body,
			List<ObjectValuePair<String, byte[]>> files, boolean anonymous,
			double priority, String[] tagsEntries,
			PortletPreferences preferences, Boolean addCommunityPermissions,
			Boolean addGuestPermissions, String[] communityPermissions,
			String[] guestPermissions, ThemeDisplay themeDisplay)
		throws PortalException, SystemException {

		StopWatch stopWatch = null;

		if (_log.isDebugEnabled()) {
			stopWatch = new StopWatch();

			stopWatch.start();
		}

		// Message

		User user = userPersistence.findByPrimaryKey(userId);
		userName = user.isDefaultUser() ? userName : user.getFullName();
		MBCategory category = mbCategoryPersistence.findByPrimaryKey(
			categoryId);
		subject = ModelHintsUtil.trimString(
			MBMessage.class.getName(), "subject", subject);

		if (preferences != null) {
			if (!MBUtil.isAllowAnonymousPosting(preferences)) {
				if (anonymous || user.isDefaultUser()) {
					throw new PrincipalException();
				}
			}
		}

		if (user.isDefaultUser()) {
			anonymous = true;
		}

		Date now = new Date();

		validate(subject, body);

		long messageId = counterLocalService.increment();

		logAddMessage(messageId, stopWatch, 1);

		MBMessage message = mbMessagePersistence.create(messageId);

		message.setUuid(uuid);
		message.setCompanyId(user.getCompanyId());
		message.setUserId(user.getUserId());
		message.setUserName(userName);
		message.setCreateDate(now);
		message.setModifiedDate(now);

		// Thread

		MBMessage parentMessage = mbMessagePersistence.fetchByPrimaryKey(
			parentMessageId);

		if (parentMessage == null) {
			parentMessageId = MBMessageImpl.DEFAULT_PARENT_MESSAGE_ID;
		}

		MBThread thread = null;

		if (threadId > 0) {
			thread = mbThreadPersistence.fetchByPrimaryKey(threadId);
		}

		if ((thread == null) ||
			(parentMessageId == MBMessageImpl.DEFAULT_PARENT_MESSAGE_ID)) {

			threadId = counterLocalService.increment();

			thread = mbThreadPersistence.create(threadId);

			thread.setCategoryId(categoryId);
			thread.setRootMessageId(messageId);
		}

		thread.setMessageCount(thread.getMessageCount() + 1);

		if (anonymous) {
			thread.setLastPostByUserId(0);
		}
		else {
			thread.setLastPostByUserId(userId);
		}

		thread.setLastPostDate(now);

		if (priority != MBThreadImpl.PRIORITY_NOT_GIVEN) {
			thread.setPriority(priority);
		}

		logAddMessage(messageId, stopWatch, 2);

		// Message

		message.setCategoryId(categoryId);
		message.setThreadId(threadId);
		message.setParentMessageId(parentMessageId);
		message.setSubject(subject);
		message.setBody(body);
		message.setAttachments(!files.isEmpty());
		message.setAnonymous(anonymous);

		// Attachments

		if (files.size() > 0) {
			long companyId = message.getCompanyId();
			String portletId = CompanyConstants.SYSTEM_STRING;
			long groupId = GroupConstants.DEFAULT_PARENT_GROUP_ID;
			long repositoryId = CompanyConstants.SYSTEM;
			String dirName = message.getAttachmentsDir();

			try {
				dlService.deleteDirectory(
					companyId, portletId, repositoryId, dirName);
			}
			catch (NoSuchDirectoryException nsde) {
				if (_log.isDebugEnabled()) {
					_log.debug(nsde.getMessage());
				}
			}

			dlService.addDirectory(companyId, repositoryId, dirName);

			for (int i = 0; i < files.size(); i++) {
				ObjectValuePair<String, byte[]> ovp = files.get(i);

				String fileName = ovp.getKey();
				byte[] bytes = ovp.getValue();

				try {
					dlService.addFile(
						companyId, portletId, groupId, repositoryId,
						dirName + "/" + fileName, StringPool.BLANK,
						new String[0], bytes);
				}
				catch (DuplicateFileException dfe) {
					if (_log.isDebugEnabled()) {
						_log.debug(dfe.getMessage());
					}
				}
			}
		}

		logAddMessage(messageId, stopWatch, 3);

		// Commit

		mbThreadPersistence.update(thread, false);
		mbMessagePersistence.update(message, false);

		logAddMessage(messageId, stopWatch, 4);

		// Resources

		if (!category.isDiscussion()) {
			if (user.isDefaultUser()) {
				addMessageResources(category, message, true, true);
			}
			else if ((addCommunityPermissions != null) &&
					 (addGuestPermissions != null)) {

				addMessageResources(
					category, message, addCommunityPermissions.booleanValue(),
					addGuestPermissions.booleanValue());
			}
			else {
				addMessageResources(
					category, message, communityPermissions, guestPermissions);
			}
		}

		logAddMessage(messageId, stopWatch, 5);

		// Statistics

		if (!category.isDiscussion()) {
			mbStatsUserLocalService.updateStatsUser(
				category.getGroupId(), userId);
		}

		logAddMessage(messageId, stopWatch, 6);

		// Category

		category.setLastPostDate(now);

		mbCategoryPersistence.update(category, false);

		logAddMessage(messageId, stopWatch, 7);

		// Subscriptions

		notifySubscribers(category, message, preferences, themeDisplay, false);

		logAddMessage(messageId, stopWatch, 8);

		// Social

		if (!message.isDiscussion() && !message.isAnonymous() &&
			!user.isDefaultUser()) {

			int activityType = MBActivityKeys.ADD_MESSAGE;
			long receiverUserId = 0;

			if (parentMessage != null) {
				activityType = MBActivityKeys.REPLY_MESSAGE;
				receiverUserId = parentMessage.getUserId();
			}

			socialActivityLocalService.addActivity(
				userId, category.getGroupId(), MBMessage.class.getName(),
				messageId, activityType, StringPool.BLANK, receiverUserId);
		}

		logAddMessage(messageId, stopWatch, 9);

		// Tags

		updateTagsAsset(userId, message, tagsEntries);

		logAddMessage(messageId, stopWatch, 10);

		// Testing roll back

		/*if (true) {
			throw new SystemException("Testing roll back");
		}*/

		// Indexer

		try {
			if (!category.isDiscussion()) {
				Indexer.addMessage(
					message.getCompanyId(), category.getGroupId(),
					message.getUserId(), message.getUserName(),
					category.getCategoryId(), threadId, messageId, subject,
					body, tagsEntries, message.getExpandoBridge());
			}
		}
		catch (SearchException se) {
			_log.error("Indexing " + messageId, se);
		}

		logAddMessage(messageId, stopWatch, 11);

		return message;
	}

	public void addMessageResources(
			long categoryId, long messageId, boolean addCommunityPermissions,
			boolean addGuestPermissions)
		throws PortalException, SystemException {

		addMessageResources(
			categoryId, null, messageId, addCommunityPermissions,
			addGuestPermissions);
	}

	public void addMessageResources(
			long categoryId, String topicId, long messageId,
			boolean addCommunityPermissions, boolean addGuestPermissions)
		throws PortalException, SystemException {

		MBCategory category = mbCategoryPersistence.findByPrimaryKey(
			categoryId);
		MBMessage message = mbMessagePersistence.findByPrimaryKey(messageId);

		addMessageResources(
			category, message, addCommunityPermissions, addGuestPermissions);
	}

	public void addMessageResources(
			MBCategory category, MBMessage message,
			boolean addCommunityPermissions, boolean addGuestPermissions)
		throws PortalException, SystemException {

		resourceLocalService.addResources(
			message.getCompanyId(), category.getGroupId(), message.getUserId(),
			MBMessage.class.getName(), message.getMessageId(),
			false, addCommunityPermissions, addGuestPermissions);
	}

	public void addMessageResources(
			long categoryId, long messageId, String[] communityPermissions,
			String[] guestPermissions)
		throws PortalException, SystemException {

		addMessageResources(
			categoryId, null, messageId, communityPermissions,
			guestPermissions);
	}

	public void addMessageResources(
			long categoryId, String topicId, long messageId,
			String[] communityPermissions, String[] guestPermissions)
		throws PortalException, SystemException {

		MBCategory category = mbCategoryPersistence.findByPrimaryKey(
			categoryId);
		MBMessage message = mbMessagePersistence.findByPrimaryKey(messageId);

		addMessageResources(
			category, message, communityPermissions, guestPermissions);
	}

	public void addMessageResources(
			MBCategory category, MBMessage message,
			String[] communityPermissions, String[] guestPermissions)
		throws PortalException, SystemException {

		resourceLocalService.addModelResources(
			message.getCompanyId(), category.getGroupId(), message.getUserId(),
			MBMessage.class.getName(), message.getMessageId(),
			communityPermissions, guestPermissions);
	}

	public void deleteDiscussionMessage(long messageId)
		throws PortalException, SystemException {

		MBMessage message = mbMessagePersistence.findByPrimaryKey(messageId);

		List<MBMessage> messages = new ArrayList<MBMessage>();

		messages.add(message);

		deleteDiscussionSocialActivities(BlogsEntry.class.getName(), messages);

		deleteMessage(message);
	}

	public void deleteDiscussionMessages(String className, long classPK)
		throws PortalException, SystemException {

		try {
			long classNameId = PortalUtil.getClassNameId(className);

			MBDiscussion discussion = mbDiscussionPersistence.findByC_C(
				classNameId, classPK);

			List<MBMessage> messages = mbMessagePersistence.findByT_P(
				discussion.getThreadId(),
				MBMessageImpl.DEFAULT_PARENT_MESSAGE_ID, 0, 1);

			deleteDiscussionSocialActivities(
				BlogsEntry.class.getName(), messages);

			if (messages.size() > 0) {
				MBMessage message = messages.get(0);

				mbThreadLocalService.deleteThread(message.getThreadId());
			}

			mbDiscussionPersistence.remove(discussion);
		}
		catch (NoSuchDiscussionException nsde) {
			if (_log.isDebugEnabled()) {
				_log.debug(nsde.getMessage());
			}
		}
	}

	public void deleteMessage(long messageId)
		throws PortalException, SystemException {

		MBMessage message = mbMessagePersistence.findByPrimaryKey(messageId);

		deleteMessage(message);
	}

	public void deleteMessage(MBMessage message)
		throws PortalException, SystemException {

		// Indexer

		try {
			Indexer.deleteMessage(
				message.getCompanyId(), message.getMessageId());
		}
		catch (SearchException se) {
			_log.error("Deleting index " + message.getMessageId(), se);
		}

		// Attachments

		if (message.isAttachments()) {
			long companyId = message.getCompanyId();
			String portletId = CompanyConstants.SYSTEM_STRING;
			long repositoryId = CompanyConstants.SYSTEM;
			String dirName = message.getAttachmentsDir();

			try {
				dlService.deleteDirectory(
					companyId, portletId, repositoryId, dirName);
			}
			catch (NoSuchDirectoryException nsde) {
				if (_log.isDebugEnabled()) {
					_log.debug(nsde.getMessage());
				}
			}
		}

		// Thread

		int count = mbMessagePersistence.countByThreadId(message.getThreadId());

		// Message flags

		if (message.isRoot()) {
			mbMessageFlagLocalService.deleteQuestionAndAnswerFlags(
				message.getThreadId());
		}

		if (count == 1) {

			// Attachments

			long companyId = message.getCompanyId();
			String portletId = CompanyConstants.SYSTEM_STRING;
			long repositoryId = CompanyConstants.SYSTEM;
			String dirName = message.getThreadAttachmentsDir();

			try {
				dlService.deleteDirectory(
					companyId, portletId, repositoryId, dirName);
			}
			catch (NoSuchDirectoryException nsde) {
				if (_log.isDebugEnabled()) {
					_log.debug(nsde.getMessage());
				}
			}

			// Subscriptions

			subscriptionLocalService.deleteSubscriptions(
				message.getCompanyId(), MBThread.class.getName(),
				message.getThreadId());

			// Thread

			mbThreadPersistence.remove(message.getThreadId());
		}
		else if (count > 1) {
			MBThread thread = mbThreadPersistence.findByPrimaryKey(
				message.getThreadId());

			// Message is a root message

			if (thread.getRootMessageId() == message.getMessageId()) {
				List<MBMessage> childrenMessages =
					mbMessagePersistence.findByT_P(
						message.getThreadId(), message.getMessageId());

				if (childrenMessages.size() > 1) {
					throw new RequiredMessageException(
						String.valueOf(message.getMessageId()));
				}
				else if (childrenMessages.size() == 1) {
					MBMessage childMessage = childrenMessages.get(0);

					childMessage.setParentMessageId(
						MBMessageImpl.DEFAULT_PARENT_MESSAGE_ID);

					mbMessagePersistence.update(childMessage, false);

					thread.setRootMessageId(childMessage.getMessageId());

					mbThreadPersistence.update(thread, false);
				}
			}

			// Message is a child message

			else {
				List<MBMessage> childrenMessages =
					mbMessagePersistence.findByT_P(
						message.getThreadId(), message.getMessageId());

				// Message has children messages

				if (childrenMessages.size() > 0) {
					Iterator<MBMessage> itr = childrenMessages.iterator();

					while (itr.hasNext()) {
						MBMessage childMessage = itr.next();

						childMessage.setParentMessageId(
							message.getParentMessageId());

						mbMessagePersistence.update(childMessage, false);
					}
				}
			}

			// Thread

			thread.setMessageCount(count - 1);

			mbThreadPersistence.update(thread, false);
		}

		// Tags

		tagsAssetLocalService.deleteAsset(
			MBMessage.class.getName(), message.getMessageId());

		// Social

		socialActivityLocalService.deleteActivities(
			MBMessage.class.getName(), message.getMessageId());

		// Ratings

		ratingsStatsLocalService.deleteStats(
			MBMessage.class.getName(), message.getMessageId());

		// Message flags

		mbMessageFlagPersistence.removeByMessageId(message.getMessageId());

		// Resources

		if (!message.isDiscussion()) {
			resourceLocalService.deleteResource(
				message.getCompanyId(), MBMessage.class.getName(),
				ResourceConstants.SCOPE_INDIVIDUAL, message.getMessageId());
		}

		// Message

		mbMessagePersistence.remove(message);
	}

	public List<MBMessage> getCategoryMessages(
			long categoryId, int start, int end)
		throws SystemException {

		return mbMessagePersistence.findByCategoryId(categoryId, start, end);
	}

	public List<MBMessage> getCategoryMessages(
			long categoryId, int start, int end, OrderByComparator obc)
		throws SystemException {

		return mbMessagePersistence.findByCategoryId(
			categoryId, start, end, obc);
	}

	public int getCategoryMessagesCount(long categoryId)
		throws SystemException {

		return mbMessagePersistence.countByCategoryId(categoryId);
	}

	public int getCategoriesMessagesCount(List<Long> categoryIds)
		throws SystemException {

		return mbMessageFinder.countByCategoryIds(categoryIds);
	}

	public List<MBMessage> getCompanyMessages(
			long companyId, int start, int end)
		throws SystemException {

		return mbMessagePersistence.findByCompanyId(companyId, start, end);
	}

	public List<MBMessage> getCompanyMessages(
			long companyId, int start, int end, OrderByComparator obc)
		throws SystemException {

		return mbMessagePersistence.findByCompanyId(companyId, start, end, obc);
	}

	public int getCompanyMessagesCount(long companyId)
		throws SystemException {

		return mbMessagePersistence.countByCompanyId(companyId);
	}

	public MBMessageDisplay getDiscussionMessageDisplay(
			long userId, String className, long classPK)
		throws PortalException, SystemException {

		long classNameId = PortalUtil.getClassNameId(className);

		MBMessage message = null;

		MBDiscussion discussion = mbDiscussionPersistence.fetchByC_C(
			classNameId, classPK);

		if (discussion != null) {
			List<MBMessage> messages = mbMessagePersistence.findByT_P(
				discussion.getThreadId(),
				MBMessageImpl.DEFAULT_PARENT_MESSAGE_ID);

			message = messages.get(0);
		}
		else {
			String subject = String.valueOf(classPK);
			//String body = subject;

			message = addDiscussionMessage(userId, null, subject, subject);

			long discussionId = counterLocalService.increment();

			discussion = mbDiscussionPersistence.create(
				discussionId);

			discussion.setClassNameId(classNameId);
			discussion.setClassPK(classPK);
			discussion.setThreadId(message.getThreadId());

			mbDiscussionPersistence.update(discussion, false);
		}

		return getMessageDisplay(message);
	}

	public int getDiscussionMessagesCount(long classNameId, long classPK)
		throws SystemException {

		MBDiscussion discussion = mbDiscussionPersistence.fetchByC_C(
			classNameId, classPK);

		if (discussion == null) {
			return 0;
		}

		int count = mbMessagePersistence.countByThreadId(
			discussion.getThreadId());

		if (count >= 1) {
			return count - 1;
		}
		else {
			return 0;
		}
	}

	public List<MBDiscussion> getDiscussions(String className)
		throws SystemException {

		long classNameId = PortalUtil.getClassNameId(className);

		return mbDiscussionPersistence.findByClassNameId(classNameId);
	}

	public List<MBMessage> getGroupMessages(long groupId, int start, int end)
		throws SystemException {

		return mbMessageFinder.findByGroupId(groupId, start, end);
	}

	public List<MBMessage> getGroupMessages(
			long groupId, int start, int end, OrderByComparator obc)
		throws SystemException {

		return mbMessageFinder.findByGroupId(groupId, start, end, obc);
	}

	public List<MBMessage> getGroupMessages(
			long groupId, long userId, int start, int end)
		throws SystemException {

		return mbMessageFinder.findByG_U(groupId, userId, start, end);
	}

	public List<MBMessage> getGroupMessages(
			long groupId, long userId, int start, int end,
			OrderByComparator obc)
		throws SystemException {

		return mbMessageFinder.findByG_U(groupId, userId, start, end, obc);
	}

	public int getGroupMessagesCount(long groupId) throws SystemException {
		return mbMessageFinder.countByGroupId(groupId);
	}

	public int getGroupMessagesCount(long groupId, long userId)
		throws SystemException {

		return mbMessageFinder.countByG_U(groupId, userId);
	}

	public MBMessage getMessage(long messageId)
		throws PortalException, SystemException {

		return mbMessagePersistence.findByPrimaryKey(messageId);
	}

	public List<MBMessage> getMessages(String className, long classPK)
		throws SystemException {

		long classNameId = PortalUtil.getClassNameId(className);

		return mbMessageFinder.findByC_C(classNameId, classPK);
	}

	public MBMessageDisplay getMessageDisplay(long messageId)
		throws PortalException, SystemException {

		MBMessage message = getMessage(messageId);

		return getMessageDisplay(message);
	}

	public MBMessageDisplay getMessageDisplay(MBMessage message)
		throws PortalException, SystemException {

		MBCategory category = mbCategoryPersistence.findByPrimaryKey(
			message.getCategoryId());

		MBMessage parentMessage = null;

		if (message.isReply()) {
			parentMessage = mbMessagePersistence.findByPrimaryKey(
				message.getParentMessageId());
		}

		MBThread thread = mbThreadPersistence.findByPrimaryKey(
			message.getThreadId());

		thread.setViewCount(thread.getViewCount() + 1);

		mbThreadPersistence.update(thread, false);

		MBTreeWalker treeWalker = new MBTreeWalkerImpl(message);

		ThreadLastPostDateComparator comparator =
			new ThreadLastPostDateComparator(false);

		MBThread[] prevAndNextThreads =
			mbThreadPersistence.findByCategoryId_PrevAndNext(
				message.getThreadId(), message.getCategoryId(), comparator);

		MBThread previousThread = prevAndNextThreads[0];
		MBThread nextThread = prevAndNextThreads[2];

		MBThread firstThread = null;

		try {
			firstThread = mbThreadPersistence.findByCategoryId_First(
				message.getCategoryId(), comparator);
		}
		catch (NoSuchThreadException nste) {
		}

		MBThread lastThread = null;

		try {
			lastThread = mbThreadPersistence.findByCategoryId_Last(
				message.getCategoryId(), comparator);
		}
		catch (NoSuchThreadException nste) {
		}

		return new MBMessageDisplayImpl(
			message, parentMessage, category, thread, treeWalker,
			previousThread, nextThread, firstThread, lastThread);
	}

	public List<MBMessage> getNoAssetMessages() throws SystemException {
		return mbMessageFinder.findByNoAssets();
	}

	public List<MBMessage> getThreadMessages(long threadId)
		throws SystemException {

		return getThreadMessages(threadId, new MessageThreadComparator());
	}

	public List<MBMessage> getThreadMessages(
			long threadId, Comparator<MBMessage> comparator)
		throws SystemException {

		List<MBMessage> messages = mbMessagePersistence.findByThreadId(
			threadId);

		Collections.sort(messages, comparator);

		return messages;
	}

	public int getThreadMessagesCount(long threadId) throws SystemException {
		return mbMessagePersistence.countByThreadId(threadId);
	}

	public void reIndex(long messageId) throws SystemException {
		if (SearchEngineUtil.isIndexReadOnly()) {
			return;
		}

		MBMessage message = mbMessagePersistence.fetchByPrimaryKey(messageId);

		if (message == null) {
			return;
		}

		MBCategory category = mbCategoryPersistence.fetchByPrimaryKey(
			message.getCategoryId());

		long companyId = category.getCompanyId();
		long groupId = category.getGroupId();
		long userId = message.getUserId();
		String userName = message.getUserName();
		long categoryId = category.getCategoryId();
		long threadId = message.getThreadId();
		String title = message.getSubject();
		String content = message.getBody();

		String[] tagsEntries = tagsEntryLocalService.getEntryNames(
			MBMessage.class.getName(), messageId);

		try {
			Document doc = Indexer.getMessageDocument(
				companyId, groupId, userId, userName, categoryId,
				threadId, messageId, title, content, tagsEntries,
				message.getExpandoBridge());

			SearchEngineUtil.addDocument(companyId, doc);
		}
		catch (SearchException se) {
			_log.error("Reindexing " + messageId, se);
		}
	}

	public void subscribeMessage(long userId, long messageId)
		throws PortalException, SystemException {

		MBMessage message = mbMessagePersistence.findByPrimaryKey(messageId);

		subscriptionLocalService.addSubscription(
			userId, MBThread.class.getName(), message.getThreadId());
	}

	public void unsubscribeMessage(long userId, long messageId)
		throws PortalException, SystemException {

		MBMessage message = mbMessagePersistence.findByPrimaryKey(messageId);

		subscriptionLocalService.deleteSubscription(
			userId, MBThread.class.getName(), message.getThreadId());
	}

	public MBMessage updateDiscussionMessage(
			long userId, long messageId, String subject, String body)
		throws PortalException, SystemException {

		if (Validator.isNull(subject)) {
			subject = "N/A";
		}

		List<ObjectValuePair<String, byte[]>> files =
			new ArrayList<ObjectValuePair<String, byte[]>>();
		List<String> existingFiles = new ArrayList<String>();
		double priority = 0.0;
		String[] tagsEntries = null;
		PortletPreferences preferences = null;
		ThemeDisplay themeDisplay = null;

		return updateMessage(
			userId, messageId, subject, body, files, existingFiles, priority,
			tagsEntries, preferences, themeDisplay);
	}

	public MBMessage updateMessage(
			long userId, long messageId, String subject, String body,
			List<ObjectValuePair<String, byte[]>> files,
			List<String> existingFiles, double priority, String[] tagsEntries,
			PortletPreferences preferences, ThemeDisplay themeDisplay)
		throws PortalException, SystemException {

		// Message

		MBMessage message = mbMessagePersistence.findByPrimaryKey(messageId);

		MBCategory category = message.getCategory();
		subject = ModelHintsUtil.trimString(
			MBMessage.class.getName(), "subject", subject);
		Date now = new Date();

		validate(subject, body);

		// Attachments

		long companyId = message.getCompanyId();
		String portletId = CompanyConstants.SYSTEM_STRING;
		long groupId = GroupConstants.DEFAULT_PARENT_GROUP_ID;
		long repositoryId = CompanyConstants.SYSTEM;
		String dirName = message.getAttachmentsDir();

		if (!files.isEmpty() || !existingFiles.isEmpty()) {
			try {
				dlService.addDirectory(companyId, repositoryId, dirName);
			}
			catch (DuplicateDirectoryException dde) {
			}

			String[] fileNames = dlService.getFileNames(
				companyId, repositoryId, dirName);

			for (String fileName: fileNames) {
				if (!existingFiles.contains(fileName)) {
					dlService.deleteFile(
						companyId, portletId, repositoryId, fileName);
				}
			}

			for (int i = 0; i < files.size(); i++) {
				ObjectValuePair<String, byte[]> ovp = files.get(i);

				String fileName = ovp.getKey();
				byte[] bytes = ovp.getValue();

				try {
					dlService.addFile(
						companyId, portletId, groupId, repositoryId,
						dirName + "/" + fileName, StringPool.BLANK,
						new String[0], bytes);
				}
				catch (DuplicateFileException dfe) {
				}
			}
		}
		else {
			try {
				dlService.deleteDirectory(
					companyId, portletId, repositoryId, dirName);
			}
			catch (NoSuchDirectoryException nsde) {
			}
		}

		// Message

		message.setModifiedDate(now);
		message.setSubject(subject);
		message.setBody(body);
		message.setAttachments(!files.isEmpty() || !existingFiles.isEmpty());

		mbMessagePersistence.update(message, false);

		// Thread

		MBThread thread = mbThreadPersistence.findByPrimaryKey(
			message.getThreadId());

		if (priority != MBThreadImpl.PRIORITY_NOT_GIVEN) {
			thread.setPriority(priority);
		}

		mbThreadPersistence.update(thread, false);

		// Category

		category.setLastPostDate(now);

		mbCategoryPersistence.update(category, false);

		// Subscriptions

		notifySubscribers(category, message, preferences, themeDisplay, true);

		// Tags

		updateTagsAsset(userId, message, tagsEntries);

		// Indexer

		try {
			if (!category.isDiscussion()) {
				Indexer.updateMessage(
					message.getCompanyId(), category.getGroupId(),
					message.getUserId(), message.getUserName(),
					category.getCategoryId(), message.getThreadId(), messageId,
					subject, body, tagsEntries, message.getExpandoBridge());
			}
		}
		catch (SearchException se) {
			_log.error("Indexing " + messageId, se);
		}

		return message;
	}

	public MBMessage updateMessage(
			long messageId, Date createDate, Date modifiedDate)
		throws PortalException, SystemException {

		// Message

		MBMessage message = mbMessagePersistence.findByPrimaryKey(messageId);

		message.setCreateDate(createDate);
		message.setModifiedDate(modifiedDate);

		mbMessagePersistence.update(message, false);

		// Thread

		MBThread thread = mbThreadPersistence.findByPrimaryKey(
			message.getThreadId());

		if (message.isAnonymous()) {
			thread.setLastPostByUserId(0);
		}
		else {
			thread.setLastPostByUserId(message.getUserId());
		}

		thread.setLastPostDate(modifiedDate);

		mbThreadPersistence.update(thread, false);

		// Category

		MBCategory category = mbCategoryPersistence.findByPrimaryKey(
			message.getCategoryId());

		category.setLastPostDate(modifiedDate);

		mbCategoryPersistence.update(category, false);

		// Statistics

		MBStatsUser statsUser = mbStatsUserPersistence.fetchByG_U(
			category.getGroupId(), message.getUserId());

		if (statsUser != null) {
			statsUser.setLastPostDate(modifiedDate);

			mbStatsUserPersistence.update(statsUser, false);
		}

		return message;
	}

	public MBMessage updateMessage(long messageId, String body)
		throws PortalException, SystemException {

		MBMessage message = mbMessagePersistence.findByPrimaryKey(messageId);

		message.setBody(body);

		mbMessagePersistence.update(message, false);

		return message;
	}

	public void updateTagsAsset(
			long userId, MBMessage message, String[] tagsEntries)
		throws PortalException, SystemException {

		if (message.isDiscussion()) {
			return;
		}

		tagsAssetLocalService.updateAsset(
			userId, message.getCategory().getGroupId(),
			MBMessage.class.getName(), message.getMessageId(), tagsEntries,
			null, null, null, null, ContentTypes.TEXT_HTML,
			message.getSubject(), null, null, null, 0, 0, null, false);
	}

	protected void deleteDiscussionSocialActivities(
			String className, List<MBMessage> messages)
		throws PortalException, SystemException {

		if (messages.size() == 0) {
			return;
		}

		MBMessage message = messages.get(0);

		MBDiscussion discussion = mbDiscussionPersistence.findByThreadId(
			message.getThreadId());

		long classNameId = PortalUtil.getClassNameId(className);
		long classPK = discussion.getClassPK();

		if (discussion.getClassNameId() != classNameId) {
			return;
		}

		Set<Long> messageIds = new HashSet<Long>();

		for (MBMessage curMessage : messages) {
			messageIds.add(curMessage.getMessageId());
		}

		List<SocialActivity> socialActivities =
			socialActivityLocalService.getActivities(
				0, className, classPK, QueryUtil.ALL_POS, QueryUtil.ALL_POS);

		for (SocialActivity socialActivity : socialActivities) {
			if (Validator.isNull(socialActivity.getExtraData())) {
				continue;
			}

			JSONObject extraData = JSONFactoryUtil.createJSONObject(
				socialActivity.getExtraData());

			long extraDataMessageId = extraData.getLong("messageId");

			if (messageIds.contains(extraDataMessageId)) {
				socialActivityLocalService.deleteActivity(
					socialActivity.getActivityId());
			}
		}
	}

	protected void logAddMessage(
		long messageId, StopWatch stopWatch, int block) {

		if (_log.isDebugEnabled()) {
			if ((messageId != 1) && ((messageId % 10) != 0)) {
				return;
			}

			_log.debug(
				"Adding message block " + block + " for " + messageId +
					" takes " + stopWatch.getTime() + " ms");
		}
	}

	protected void notifySubscribers(
			MBCategory category, MBMessage message,
			PortletPreferences preferences, ThemeDisplay themeDisplay,
			boolean update)
		throws PortalException, SystemException {

		if (category.isDiscussion()) {
			return;
		}

		if (preferences == null) {
			long ownerId = category.getGroupId();
			int ownerType = PortletKeys.PREFS_OWNER_TYPE_GROUP;
			long plid = PortletKeys.PREFS_PLID_SHARED;
			String portletId = PortletKeys.MESSAGE_BOARDS;
			String defaultPreferences = null;

			preferences = portletPreferencesLocalService.getPreferences(
				category.getCompanyId(), ownerId, ownerType, plid, portletId,
				defaultPreferences);
		}

		if (!update && MBUtil.getEmailMessageAddedEnabled(preferences)) {
		}
		else if (update && MBUtil.getEmailMessageUpdatedEnabled(preferences)) {
		}
		else {
			return;
		}

		Company company = companyPersistence.findByPrimaryKey(
			message.getCompanyId());

		Group group = groupPersistence.findByPrimaryKey(category.getGroupId());

		User user = userPersistence.findByPrimaryKey(message.getUserId());

		List<Long> categoryIds = new ArrayList<Long>();

		categoryIds.add(category.getCategoryId());
		categoryIds.addAll(category.getAncestorCategoryIds());

		String messageURL = StringPool.BLANK;

		if (themeDisplay != null) {
			String portalURL = PortalUtil.getPortalURL(themeDisplay);
			String layoutURL = PortalUtil.getLayoutURL(themeDisplay);

			messageURL =
				portalURL + layoutURL + "/-/message_boards/message/" +
					message.getMessageId();
		}

		String portletName = PortalUtil.getPortletTitle(
			PortletKeys.MESSAGE_BOARDS, user);

		String fromName = MBUtil.getEmailFromName(preferences);
		String fromAddress = MBUtil.getEmailFromAddress(preferences);

		String mailingListAddress = StringPool.BLANK;

		if (PropsValues.POP_SERVER_NOTIFICATIONS_ENABLED) {
			mailingListAddress = MBUtil.getMailingListAddress(
				message.getCategoryId(), message.getMessageId(),
				company.getMx(), fromAddress);
		}

		String replyToAddress = mailingListAddress;
		String mailId = MBUtil.getMailId(
			company.getMx(), message.getCategoryId(), message.getMessageId());

		fromName = StringUtil.replace(
			fromName,
			new String[] {
				"[$COMPANY_ID$]",
				"[$COMPANY_MX$]",
				"[$COMPANY_NAME$]",
				"[$COMMUNITY_NAME$]",
				"[$MAILING_LIST_ADDRESS$]",
				"[$MESSAGE_USER_ADDRESS$]",
				"[$MESSAGE_USER_NAME$]",
				"[$PORTLET_NAME$]"
			},
			new String[] {
				String.valueOf(company.getCompanyId()),
				company.getMx(),
				company.getName(),
				group.getName(),
				mailingListAddress,
				user.getEmailAddress(),
				user.getFullName(),
				portletName
			});

		fromAddress = StringUtil.replace(
			fromAddress,
			new String[] {
				"[$COMPANY_ID$]",
				"[$COMPANY_MX$]",
				"[$COMPANY_NAME$]",
				"[$COMMUNITY_NAME$]",
				"[$MAILING_LIST_ADDRESS$]",
				"[$MESSAGE_USER_ADDRESS$]",
				"[$MESSAGE_USER_NAME$]",
				"[$PORTLET_NAME$]"
			},
			new String[] {
				String.valueOf(company.getCompanyId()),
				company.getMx(),
				company.getName(),
				group.getName(),
				mailingListAddress,
				user.getEmailAddress(),
				user.getFullName(),
				portletName
			});

		String subjectPrefix = null;
		String body = null;
		String signature = null;
		boolean htmlFormat = MBUtil.getEmailHtmlFormat(preferences);

		if (update) {
			subjectPrefix = MBUtil.getEmailMessageUpdatedSubjectPrefix(
				preferences);
			body = MBUtil.getEmailMessageUpdatedBody(preferences);
			signature = MBUtil.getEmailMessageUpdatedSignature(preferences);
		}
		else {
			subjectPrefix = MBUtil.getEmailMessageAddedSubjectPrefix(
				preferences);
			body = MBUtil.getEmailMessageAddedBody(preferences);
			signature = MBUtil.getEmailMessageAddedSignature(preferences);
		}

		if (Validator.isNotNull(signature)) {
			body +=  "\n--\n" + signature;
		}

		subjectPrefix = StringUtil.replace(
			subjectPrefix,
			new String[] {
				"[$CATEGORY_NAME$]",
				"[$COMPANY_ID$]",
				"[$COMPANY_MX$]",
				"[$COMPANY_NAME$]",
				"[$COMMUNITY_NAME$]",
				"[$FROM_ADDRESS$]",
				"[$FROM_NAME$]",
				"[$MAILING_LIST_ADDRESS$]",
				"[$MESSAGE_BODY$]",
				"[$MESSAGE_ID$]",
				"[$MESSAGE_SUBJECT$]",
				"[$MESSAGE_USER_ADDRESS$]",
				"[$MESSAGE_USER_NAME$]",
				"[$PORTAL_URL$]",
				"[$PORTLET_NAME$]"
			},
			new String[] {
				category.getName(),
				String.valueOf(company.getCompanyId()),
				company.getMx(),
				company.getName(),
				group.getName(),
				fromAddress,
				fromName,
				mailingListAddress,
				message.getBody(),
				String.valueOf(message.getMessageId()),
				message.getSubject(),
				user.getEmailAddress(),
				user.getFullName(),
				company.getVirtualHost(),
				portletName
			});

		body = StringUtil.replace(
			body,
			new String[] {
				"[$CATEGORY_NAME$]",
				"[$COMPANY_ID$]",
				"[$COMPANY_MX$]",
				"[$COMPANY_NAME$]",
				"[$COMMUNITY_NAME$]",
				"[$FROM_ADDRESS$]",
				"[$FROM_NAME$]",
				"[$MAILING_LIST_ADDRESS$]",
				"[$MESSAGE_BODY$]",
				"[$MESSAGE_ID$]",
				"[$MESSAGE_SUBJECT$]",
				"[$MESSAGE_URL$]",
				"[$MESSAGE_USER_ADDRESS$]",
				"[$MESSAGE_USER_NAME$]",
				"[$PORTAL_URL$]",
				"[$PORTLET_NAME$]"
			},
			new String[] {
				category.getName(),
				String.valueOf(company.getCompanyId()),
				company.getMx(),
				company.getName(),
				group.getName(),
				fromAddress,
				fromName,
				mailingListAddress,
				message.getBody(),
				String.valueOf(message.getMessageId()),
				message.getSubject(),
				messageURL,
				user.getEmailAddress(),
				user.getFullName(),
				company.getVirtualHost(),
				portletName
			});

		String subject = message.getSubject();

		if (subject.indexOf(subjectPrefix) == -1) {
			subject = subjectPrefix.trim() + " " + subject.trim();
		}

		String inReplyTo = null;

		if (message.getParentMessageId() !=
				MBMessageImpl.DEFAULT_PARENT_MESSAGE_ID) {

			inReplyTo = MBUtil.getMailId(
				company.getMx(), message.getCategoryId(),
				message.getParentMessageId());
		}

		com.liferay.portal.kernel.messaging.Message messagingObj =
			new com.liferay.portal.kernel.messaging.Message();

		messagingObj.put("companyId", message.getCompanyId());
		messagingObj.put("userId", message.getUserId());
		messagingObj.put("categoryIds", StringUtil.merge(categoryIds));
		messagingObj.put("threadId", message.getThreadId());
		messagingObj.put("fromName", fromName);
		messagingObj.put("fromAddress", fromAddress);
		messagingObj.put("subject", subject);
		messagingObj.put("body", body);
		messagingObj.put("replyToAddress", replyToAddress);
		messagingObj.put("mailId", mailId);
		messagingObj.put("inReplyTo", inReplyTo);
		messagingObj.put("htmlFormat", htmlFormat);
		messagingObj.put(
			"sourceMailingList", MailingListThreadLocal.isSourceMailingList());

		MessageBusUtil.sendMessage(
			DestinationNames.MESSAGE_BOARDS, messagingObj);
	}

	protected void sendBlogsCommentsEmail(
			long userId, BlogsEntry entry, MBMessage message,
			ThemeDisplay themeDisplay)
		throws IOException, PortalException, SystemException {

		long companyId = message.getCompanyId();

		if (!PrefsPropsUtil.getBoolean(
				companyId, PropsKeys.BLOGS_EMAIL_COMMENTS_ADDED_ENABLED)) {

			return;
		}

		String portalURL = PortalUtil.getPortalURL(themeDisplay);
		String layoutURL = PortalUtil.getLayoutURL(themeDisplay);

		String blogsEntryURL =
			portalURL + layoutURL + "/-/blogs/" + entry.getUrlTitle();

		User blogsUser = userPersistence.findByPrimaryKey(entry.getUserId());
		User commentsUser = userPersistence.findByPrimaryKey(userId);

		String fromName = PrefsPropsUtil.getString(
			companyId, PropsKeys.ADMIN_EMAIL_FROM_NAME);
		String fromAddress = PrefsPropsUtil.getString(
			companyId, PropsKeys.ADMIN_EMAIL_FROM_ADDRESS);

		String toName = blogsUser.getFullName();
		String toAddress = blogsUser.getEmailAddress();

		String subject = PrefsPropsUtil.getContent(
			companyId, PropsKeys.BLOGS_EMAIL_COMMENTS_ADDED_SUBJECT);
		String body = PrefsPropsUtil.getContent(
			companyId, PropsKeys.BLOGS_EMAIL_COMMENTS_ADDED_BODY);

		subject = StringUtil.replace(
			subject,
			new String[] {
				"[$BLOGS_COMMENTS_USER_ADDRESS$]",
				"[$BLOGS_COMMENTS_USER_NAME$]",
				"[$BLOGS_ENTRY_URL$]",
				"[$FROM_ADDRESS$]",
				"[$FROM_NAME$]",
				"[$TO_ADDRESS$]",
				"[$TO_NAME$]"
			},
			new String[] {
				commentsUser.getEmailAddress(),
				commentsUser.getFullName(),
				blogsEntryURL,
				fromAddress,
				fromName,
				toAddress,
				toName
			});

		body = StringUtil.replace(
			body,
			new String[] {
				"[$BLOGS_COMMENTS_USER_ADDRESS$]",
				"[$BLOGS_COMMENTS_USER_NAME$]",
				"[$BLOGS_ENTRY_URL$]",
				"[$FROM_ADDRESS$]",
				"[$FROM_NAME$]",
				"[$TO_ADDRESS$]",
				"[$TO_NAME$]"
			},
			new String[] {
				commentsUser.getEmailAddress(),
				commentsUser.getFullName(),
				blogsEntryURL,
				fromAddress,
				fromName,
				toAddress,
				toName
			});

		InternetAddress from = new InternetAddress(fromAddress, fromName);

		InternetAddress to = new InternetAddress(toAddress, toName);

		MailMessage mailMessage = new MailMessage(
			from, to, subject, body, true);

		mailService.sendEmail(mailMessage);
	}

	protected void validate(String subject, String body)
		throws PortalException {

		if (Validator.isNull(subject)) {
			throw new MessageSubjectException();
		}

		if (Validator.isNull(body)) {
			throw new MessageBodyException();
		}
	}

	private static Log _log =
		LogFactory.getLog(MBMessageLocalServiceImpl.class);

}