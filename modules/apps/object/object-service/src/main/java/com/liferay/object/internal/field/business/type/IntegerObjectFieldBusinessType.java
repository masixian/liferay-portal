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

package com.liferay.object.internal.field.business.type;

import com.liferay.dynamic.data.mapping.form.field.type.constants.DDMFormFieldTypeConstants;
import com.liferay.dynamic.data.mapping.storage.constants.FieldConstants;
import com.liferay.object.constants.ObjectFieldConstants;
import com.liferay.object.field.business.type.ObjectFieldBusinessType;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

/**
 * @author Marcela Cunha
 */
@Component(
	immediate = true,
	property = "object.field.business.type.name=" + ObjectFieldConstants.BUSINESS_TYPE_INTEGER,
	service = {
		IntegerObjectFieldBusinessType.class, ObjectFieldBusinessType.class
	}
)
public class IntegerObjectFieldBusinessType implements ObjectFieldBusinessType {

	@Override
	public String getDBType() {
		return ObjectFieldConstants.DB_TYPE_INTEGER;
	}

	@Override
	public String getDDMFormFieldTypeName() {
		return DDMFormFieldTypeConstants.NUMERIC;
	}

	@Override
	public String getDescription(Locale locale) {
		return LanguageUtil.get(
			ResourceBundleUtil.getModuleAndPortalResourceBundle(
				locale, getClass()),
			"add-an-integer-up-to-nine-digits");
	}

	@Override
	public String getLabel(Locale locale) {
		return LanguageUtil.get(
			ResourceBundleUtil.getModuleAndPortalResourceBundle(
				locale, getClass()),
			"integer");
	}

	@Override
	public String getName() {
		return ObjectFieldConstants.BUSINESS_TYPE_INTEGER;
	}

	@Override
	public Map<String, Object> getProperties() {
		return HashMapBuilder.<String, Object>put(
			FieldConstants.DATA_TYPE, FieldConstants.INTEGER
		).build();
	}

}