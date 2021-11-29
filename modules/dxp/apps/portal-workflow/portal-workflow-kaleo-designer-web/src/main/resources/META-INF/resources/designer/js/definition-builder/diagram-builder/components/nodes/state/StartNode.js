/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 */

import PropTypes from 'prop-types';
import React, {useContext} from 'react';

import {DefinitionBuilderContext} from '../../../../DefinitionBuilderContext';
import BaseNode from '../BaseNode';

export default function StartNode({
	data: {description, label} = {},
	descriptionSidebar,
	id,
	...otherProps
}) {
	const {defaultLanguageId} = useContext(DefinitionBuilderContext);

	if (!label || !label[defaultLanguageId]) {
		const defaultLanguageId = themeDisplay.getLanguageId();

		label = {
			[defaultLanguageId]: Liferay.Language.get('start'),
		};
	}

	return (
		<BaseNode
			className="start-node"
			description={description}
			descriptionSidebar={descriptionSidebar}
			icon="play"
			id={id}
			label={label}
			type="start"
			{...otherProps}
		/>
	);
}

StartNode.propTypes = {
	data: PropTypes.object,
	descriptionSidebar: PropTypes.string,
	id: PropTypes.string.isRequired,
};
