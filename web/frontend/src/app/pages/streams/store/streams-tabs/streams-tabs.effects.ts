import { Injectable, NgZone }      from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';

import {
  distinctUntilChanged,
  filter,
  map,
  mergeMap,
  switchMap,
  take,
  takeUntil,
  tap,
  withLatestFrom,
}                                 from 'rxjs/operators';
import { environment }            from '../../../../../environments/environment';
import { fromEvent, Subject }     from 'rxjs';
import { TabModel }               from '../../models/tab.model';
import { select, Store }          from '@ngrx/store';
import * as StreamsTabsActions    from './streams-tabs.actions';
import { StreamsTabsActionTypes } from './streams-tabs.actions';
import { AppState }               from '../../../../core/store';
import { getTabs, getTabsState }  from './streams-tabs.selectors';
import * as fromTabs              from './streams-tabs.reducer';
import { createTab }              from './streams-tabs.reducer';
import { Router }                 from '@angular/router';
import { appRoute }               from '../../../../shared/utils/routes.names';


@Injectable()
export class StreamsTabsEffects {
  private stop_tabs_subscription$ = new Subject();

  constructor(
    private actions$: Actions,
    private appStore: Store<AppState>,
    private router: Router,
    private _ngZone: NgZone,
  ) {}

  @Effect() loadTabsFromLS = this.actions$
    .pipe(
      ofType(StreamsTabsActionTypes.LOAD_TABS_FROM_LS),
      mergeMap(() => {
        const tabsFromLS = localStorage.getItem(`${environment.config.version}_gridTabs`);
        return [
          new StreamsTabsActions.SetTabs({
            tabs: tabsFromLS ? JSON.parse(tabsFromLS) : [],
          }),
          new StreamsTabsActions.StartTabsLSSync(),
        ];
      }),
    );

  @Effect() startTabsLSSync = this.actions$
    .pipe(
      ofType(StreamsTabsActionTypes.START_TABS_LS_SYNC),
      switchMap(() => {
        return fromEvent(window, 'storage')
          .pipe(
            filter((resp: StorageEvent) => resp.key === `${environment.config.version}_gridTabs`),
            map((resp: StorageEvent) => resp.newValue),
            filter((newValue: string) => !!(newValue && newValue.length)),
            distinctUntilChanged(),
            map((newValue: string) => JSON.parse(newValue)),
            map((tabs: TabModel[]) => new StreamsTabsActions.SetTabs({
              tabs: tabs,
            })),
            takeUntil(this.stop_tabs_subscription$),
          );
      }),
    );

  @Effect({dispatch: false}) stopTabsSync = this.actions$
    .pipe(
      ofType(StreamsTabsActionTypes.STOP_TABS_LS_SYNC),
      tap(() => {
        this.stop_tabs_subscription$.next(true);
        this.stop_tabs_subscription$.complete();
      }),
    );

  @Effect({dispatch: false}) saveTabsToLS = this.actions$
    .pipe(
      ofType(
        StreamsTabsActionTypes.ADD_TAB,
        StreamsTabsActionTypes.REMOVE_TAB,
        StreamsTabsActionTypes.REMOVE_STREAM_TABS,
        StreamsTabsActionTypes.REMOVE_SYMBOL_TABS,
        StreamsTabsActionTypes.SET_FILTER,
      ),
      switchMap(() => this.appStore
        .pipe(
          select(getTabsState),
          take(1),
        )),
      tap((tabsState: fromTabs.State) => {

        window.localStorage.setItem(`${environment.config.version}_gridTabs`, JSON.stringify([...tabsState.tabs].map(tab => {
          tab = new TabModel({...tab});
          delete tab.active;
          if (tab.filter) delete tab.filter.silent;
          return tab;
        })));
      }),
    );

  @Effect({dispatch: false}) createTab = this.actions$
    .pipe(
      ofType<StreamsTabsActions.CreateTab>(StreamsTabsActionTypes.CREATE_TAB),
      withLatestFrom(this.appStore.pipe(select(getTabs))),
      tap(([action, tabs]: [StreamsTabsActions.CreateTab, TabModel[]]) => {
        const params = action.payload.params, data = action.payload.data,
          tab = createTab(tabs, new TabModel({
            ...params,
            ...data,
          }));
        if (Object.keys(params).length) {
          this.router.navigate([appRoute, ...tab.linkArray]);
        }
      }),
    );
}
