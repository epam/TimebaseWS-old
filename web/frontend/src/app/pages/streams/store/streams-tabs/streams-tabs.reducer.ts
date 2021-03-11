import { TabModel }                                   from '../../models/tab.model';
import { StreamsTabsActions, StreamsTabsActionTypes } from './streams-tabs.actions';


export interface State {
  tabs: TabModel[];
}


export const initialState: State = {
  tabs: [],
};

export function getActiveOrFirstTab(tabs: TabModel[]): TabModel {
  return tabs.find(tab => tab.active) || tabs[0] || null;
}

// tslint:disable-next-line:no-bitwise
export const uuid = () => `f${(~~(Math.random() * 1e8)).toString(16)}`;

export function createTab(existedTabs: TabModel[], tab: TabModel): TabModel {
  const sameTabs = findTabs(existedTabs, tab);
  if (sameTabs.length) {
    sameTabs.sort((a, b) => {
      if (a.id > b.id) return 1;
      if (a.id < b.id) return -1;
    });

    return new TabModel({
      ...tab,
      id: uuid(),
    });
  }
  return new TabModel({
    ...tab,
    id: uuid(),
  });
}

export function findTabs(existedTabs: TabModel[], tab: TabModel): TabModel[] {
  const tabs = [];
  existedTabs.forEach(existedTab => {
    if (tab.symbol) {
      if (tab.symbol === existedTab.symbol && tab.stream === existedTab.stream) tabs.push(existedTab);
    } else {
      if (tab.stream === existedTab.stream) tabs.push(existedTab);
    }
  });
  return tabs;
}

export function reducer(state = initialState, action: StreamsTabsActions): State {
  let tabs: TabModel[], selectedTab: TabModel, selectedTabIndex: number;
  let newTabsList = false, i;
  switch (action.type) {
    case StreamsTabsActionTypes.SET_TABS:
      if (state.tabs && action.payload && action.payload.tabs && state.tabs.length === action.payload.tabs.length) {
        for (i = 0; i < action.payload.tabs.length; i++) {
          if (!state.tabs.find(tab => tab.id === action.payload.tabs[i].id)) {
            newTabsList = true;
            break;
          }
        }
        if (newTabsList) {

          return {
            ...state,
          };
        }
      }

      selectedTab = getActiveOrFirstTab((state.tabs || []));
      if (selectedTab) {

        selectedTabIndex = action.payload.tabs.findIndex(tab => {
          if (selectedTab.symbol) {
            return selectedTab.symbol === tab.symbol && selectedTab.stream === tab.stream && selectedTab.id === tab.id;
          } else {
            return selectedTab.stream === tab.stream && selectedTab.id === tab.id;
          }
        });
        if (selectedTabIndex > -1 && state.tabs.length) action.payload.tabs[selectedTabIndex].active = true;
      }
      let tabsList = [];
      if (action.payload.tabs) {
        tabsList = action.payload.tabs.map(tab => new TabModel({...tab}));
      }
      return {
        ...state,
        // tabs: action.payload.tabs.map(tab => new TabModel({...tab})) || [],
        tabs: tabsList,
      };
    case StreamsTabsActionTypes.ADD_TAB:
      tabs = [...(state.tabs || [])].map(tab => {
        delete tab.active;
        return tab;
      });

      selectedTabIndex = tabs.findIndex(tab => {
        const action_tab = action.payload.tab;
        if (action_tab.symbol) {
          return action_tab.symbol === tab.symbol && action_tab.stream === tab.stream && action_tab.id === tab.id;
        } else {
          return action_tab.stream === tab.stream && action_tab.id === tab.id;
        }
      });
      if (selectedTabIndex === -1) {
        if (action.payload.position > -1) {
          tabs.splice(action.payload.position, 0, (new TabModel({
            ...action.payload.tab,
          })));
        } else {
          tabs.push(new TabModel({
            ...action.payload.tab,
          }));
        }
      } else {
        tabs[selectedTabIndex].active = true;
      }

      return {
        ...state,
        tabs: tabs,
      };

    case StreamsTabsActionTypes.REMOVE_TAB:
      tabs = [...state.tabs];
      selectedTabIndex = tabs.findIndex(tab => {
        const action_tab = action.payload.tab;
        if (action_tab.symbol) {
          return action_tab.symbol === tab.symbol && action_tab.stream === tab.stream && action_tab.id === tab.id;
        } else {
          return action_tab.stream === tab.stream && action_tab.id === tab.id;
        }
      });
      if (selectedTabIndex > -1) {
        tabs.splice(selectedTabIndex, 1);
        /* if (action.payload.tab.active && tabs.length) {
         tabs[selectedTabIndex < tabs.length ? selectedTabIndex : tabs.length - 1].active = true;
         }*/
      }
      return {
        ...state,
        tabs: tabs,
      };

    case StreamsTabsActionTypes.REMOVE_STREAM_TABS:
      console.log(`Stream ${action.payload.streamKey} was deleted`);
      tabs = [...state.tabs];
      if (action.payload.streamKey) {
        tabs = tabs.filter(tab => tab.stream !== action.payload.streamKey);
      }
      return {
        ...state,
        tabs: tabs,
      };

    case StreamsTabsActionTypes.REMOVE_SYMBOL_TABS:
      tabs = [...state.tabs];
      if (action.payload.streamId) {
        tabs = tabs.filter(tab => {
          if (tab.symbol) {
            return tab.stream !== action.payload.streamId || tab.symbol !== action.payload.symbolName;
          }
          return true;
        });
      }
      return {
        ...state,
        tabs: tabs,
      };

    case StreamsTabsActionTypes.SET_FILTER:
      tabs = [...state.tabs];
      selectedTab = getActiveOrFirstTab((state.tabs));
      if (selectedTab) {
        selectedTabIndex = tabs.findIndex(tab => {
          if (selectedTab.symbol) {
            return selectedTab.symbol === tab.symbol && selectedTab.stream === tab.stream && selectedTab.id === tab.id;
          } else {
            return selectedTab.stream === tab.stream && selectedTab.id === tab.id;
          }
        });
        if (selectedTabIndex > -1) {
          tabs[selectedTabIndex] = new TabModel({
            ...tabs[selectedTabIndex],
            filter: action.payload.filter,
          });
        }
      }
      return {
        ...state,
        tabs: tabs,
      };
    default:
      return state;
  }
}
