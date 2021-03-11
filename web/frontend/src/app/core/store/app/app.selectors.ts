import * as fromApp                              from './app.reducer';
import { createFeatureSelector, createSelector } from '@ngrx/store';
import { RouterReducerState }                    from '@ngrx/router-store';

export const getAppState = createFeatureSelector<fromApp.State>('appSelf');


export const getAppSettings = createSelector(
  getAppState,
  (state: fromApp.State) => state.settings,
);

export const getAppVisibility = createSelector(
  getAppState,
  (state: fromApp.State) => state.app_is_visible,
);


export const getRouterState = createFeatureSelector('router');
export const getCurrentUrl = createSelector(
  getRouterState,
  (state: RouterReducerState) => {
    return state == null ? null : state.state && state.state.url;
  });
