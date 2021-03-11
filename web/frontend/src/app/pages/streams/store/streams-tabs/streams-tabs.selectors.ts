import * as fromTabs                          from './streams-tabs.reducer';
import { createSelector }                     from '@ngrx/store';
import { StreamsState, streamsStoreSelector } from '../index';
import { TabModel }                           from '../../models/tab.model';

export const getTabsState = createSelector(
  streamsStoreSelector,
  (state: StreamsState) => state.tabs,
);

export const getTabs = createSelector(
  getTabsState,
  (state: fromTabs.State) => state.tabs,
);

export const getActiveTab = createSelector(
  getTabs,
  (tabs: TabModel[]) => tabs.find(tab => tab.active),
);

export const getActiveOrFirstTab = createSelector(
  getTabsState,
  (state: fromTabs.State) => fromTabs.getActiveOrFirstTab(state.tabs),
);

export const getActiveTabFilters = createSelector(
  getActiveOrFirstTab,
  (tab: TabModel) => tab && tab.filter ? tab.filter : null,
);

