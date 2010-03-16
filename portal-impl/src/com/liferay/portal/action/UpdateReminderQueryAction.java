/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
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

package com.liferay.portal.action;

import com.liferay.portal.NoSuchUserException;
import com.liferay.portal.UserReminderQueryException;
import com.liferay.portal.kernel.security.auth.AuthTokenUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.UserServiceUtil;
import com.liferay.portal.struts.ActionConstants;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.enterpriseadmin.util.EnterpriseAdminUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * <a href="UpdateReminderQueryAction.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 */
public class UpdateReminderQueryAction extends Action {

	public ActionForward execute(
			ActionMapping mapping, ActionForm form, HttpServletRequest request,
			HttpServletResponse response)
		throws Exception {

		String cmd = ParamUtil.getString(request, Constants.CMD);

		if (Validator.isNull(cmd)) {
			return mapping.findForward("portal.update_reminder_query");
		}

		try {
			updateReminderQuery(request, response);

			return mapping.findForward(ActionConstants.COMMON_REFERER);
		}
		catch (Exception e) {
			if (e instanceof UserReminderQueryException) {
				SessionErrors.add(request, e.getClass().getName());

				return mapping.findForward("portal.update_reminder_query");
			}
			else if (e instanceof NoSuchUserException ||
					 e instanceof PrincipalException) {

				SessionErrors.add(request, e.getClass().getName());

				return mapping.findForward("portal.error");
			}
			else {
				PortalUtil.sendError(e, request, response);

				return null;
			}
		}
	}

	protected void updateReminderQuery(
			HttpServletRequest request, HttpServletResponse response)
		throws Exception {

		long userId = PortalUtil.getUserId(request);
		String question = ParamUtil.getString(request, "reminderQueryQuestion");
		String answer = ParamUtil.getString(request, "reminderQueryAnswer");

		if (question.equals(EnterpriseAdminUtil.CUSTOM_QUESTION)) {
			question = ParamUtil.getString(
				request, "reminderQueryCustomQuestion");
		}

		AuthTokenUtil.check(request);

		UserServiceUtil.updateReminderQuery(userId, question, answer);
	}

}