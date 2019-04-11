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

package com.liferay.portal.template;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.io.unsync.UnsyncStringWriter;
import com.liferay.portal.kernel.template.Template;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.template.TemplateException;
import com.liferay.portal.kernel.template.TemplateResource;
import com.liferay.portal.kernel.template.TemplateResourceCache;
import com.liferay.portal.kernel.util.StringBundler;

import java.io.Writer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Tina Tian
 */
public abstract class BaseTemplate implements Template {

	public BaseTemplate(
		TemplateResource templateResource,
		TemplateResource errorTemplateResource, Map<String, Object> context,
		TemplateContextHelper templateContextHelper) {

		if (templateResource == null) {
			throw new IllegalArgumentException("Template resource is null");
		}

		if (templateContextHelper == null) {
			throw new IllegalArgumentException(
				"Template context helper is null");
		}

		_templateResource = templateResource;
		_errorTemplateResource = errorTemplateResource;

		this.context = new HashMap<>();

		if (context != null) {
			for (Map.Entry<String, Object> entry : context.entrySet()) {
				put(entry.getKey(), entry.getValue());
			}
		}

		_templateContextHelper = templateContextHelper;
	}

	@Override
	public void clear() {
		context.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return context.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return context.containsValue(value);
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return context.entrySet();
	}

	@Override
	public Object get(Object key) {
		if (key == null) {
			return null;
		}

		return context.get(key);
	}

	@Override
	public Object get(String key) {
		if (key == null) {
			return null;
		}

		return context.get(key);
	}

	@Override
	public String[] getKeys() {
		Set<String> keys = context.keySet();

		return keys.toArray(new String[keys.size()]);
	}

	@Override
	public boolean isEmpty() {
		return context.isEmpty();
	}

	@Override
	public Set<String> keySet() {
		return context.keySet();
	}

	@Override
	public void prepare(HttpServletRequest request) {
		_templateContextHelper.prepare(this, request);
	}

	@Override
	public void processTemplate(Writer writer) throws TemplateException {
		if (_errorTemplateResource == null) {
			try {
				processTemplate(_templateResource, writer);

				return;
			}
			catch (Exception e) {
				throw new TemplateException(
					"Unable to process template " +
						_templateResource.getTemplateId(),
					e);
			}
		}

		Writer oldWriter = (Writer)get(TemplateConstants.WRITER);

		try {
			UnsyncStringWriter unsyncStringWriter = new UnsyncStringWriter();

			put(TemplateConstants.WRITER, unsyncStringWriter);

			processTemplate(_templateResource, unsyncStringWriter);

			StringBundler sb = unsyncStringWriter.getStringBundler();

			sb.writeTo(writer);
		}
		catch (Exception e) {
			put(TemplateConstants.WRITER, writer);

			handleException(
				_templateResource, _errorTemplateResource, e, writer);
		}
		finally {
			put(TemplateConstants.WRITER, oldWriter);
		}
	}

	@Override
	public Object put(String key, Object value) {
		if ((key == null) || (value == null)) {
			return null;
		}

		return context.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		context.putAll(map);
	}

	@Override
	public Object remove(Object key) {
		return context.remove(key);
	}

	@Override
	public int size() {
		return context.size();
	}

	@Override
	public Collection<Object> values() {
		return context.values();
	}

	protected void cacheTemplateResource(
		TemplateResourceCache templateResourceCache,
		TemplateResource templateResource) {

		TemplateResource cachedTemplateResource =
			templateResourceCache.getTemplateResource(
				templateResource.getTemplateId());

		if ((cachedTemplateResource == null) ||
			!templateResource.equals(cachedTemplateResource)) {

			templateResourceCache.put(
				templateResource.getTemplateId(), templateResource);
		}
	}

	protected String getTemplateResourceUUID(
		TemplateResource templateResource) {

		return TemplateConstants.TEMPLATE_RESOURCE_UUID_PREFIX.concat(
			StringPool.POUND
		).concat(
			templateResource.getTemplateId()
		);
	}

	protected abstract void handleException(
			TemplateResource templateResource,
			TemplateResource errorTemplateResource, Exception exception,
			Writer writer)
		throws TemplateException;

	protected abstract void processTemplate(
			TemplateResource templateResource, Writer writer)
		throws Exception;

	protected Map<String, Object> context;

	private final TemplateResource _errorTemplateResource;
	private final TemplateContextHelper _templateContextHelper;
	private final TemplateResource _templateResource;

}