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

import ClayTabs from '@clayui/tabs';
import {useEffect} from 'react';
import {Outlet, useNavigate} from 'react-router-dom';

import CompareRuns from '.';
import Container from '../../components/Layout/Container';
import useHeader from '../../hooks/useHeader';
import i18n from '../../i18n';
import useCompareRuns from './useCompareRuns';

const COMPARE_RUNS_ROOT_PATH = '/compare-runs';

const CompareRunsOutlet: React.FC = () => {
	const {setHeading, setTabs} = useHeader();
	const {comparableTabs, currentTab} = useCompareRuns();
	const navigate = useNavigate();

	useEffect(() => {
		setTimeout(() => {
			setHeading([
				{
					category: i18n.translate('project'),
					title: 'Liferay Portal 7.4',
				},
			]);
		});

		setTabs([]);
	}, [setHeading, setTabs]);

	return (
		<>
			<CompareRuns />

			<Container className="mt-3">
				<ClayTabs className="header-container-tabs">
					{comparableTabs &&
						comparableTabs.map((tab, index) => (
							<ClayTabs.Item
								active={tab.active}
								innerProps={{
									'aria-controls': `tabpanel-${index}`,
								}}
								key={index}
								onClick={() => navigate(tab.path)}
							>
								{tab.title}
							</ClayTabs.Item>
						))}
				</ClayTabs>

				<h5 className="mt-5">{currentTab?.title}</h5>

				<Outlet context={{COMPARE_RUNS_ROOT_PATH}} />
			</Container>
		</>
	);
};

export default CompareRunsOutlet;
