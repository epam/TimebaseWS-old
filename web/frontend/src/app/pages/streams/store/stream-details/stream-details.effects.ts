import { Injectable }                                                                    from '@angular/core';
import { Actions, Effect, ofType }                                                       from '@ngrx/effects';
import { HttpClient }                                                                    from '@angular/common/http';
import { select, Store }                                                                 from '@ngrx/store';
import { distinctUntilChanged, filter, map, mergeMap, share, switchMap, takeUntil, tap } from 'rxjs/operators';
import { TabModel }                                                                      from '../../models/tab.model';
import * as StreamDetailsActions
                                                                                         from './stream-details.actions';
import { StreamDetailsActionTypes }                                                      from './stream-details.actions';
import { Subject }                                                                       from 'rxjs';
import * as FilterActions
                                                                                         from '../filter/filter.actions';
import { getTabsState }                                                                  from '../streams-tabs/streams-tabs.selectors';
import { AppState }                                                                      from '../../../../core/store';

@Injectable()
export class StreamDetailsEffects {
  @Effect({dispatch: false}) setSchema = this.actions$
    .pipe(
      ofType<StreamDetailsActions.SetSchema>(StreamDetailsActionTypes.SET_SCHEMA),
      share(),
    );
  @Effect({dispatch: false}) cleanStreamData = this.actions$
    .pipe(
      ofType<StreamDetailsActions.CleanStreamData>(StreamDetailsActionTypes.CLEAN_STREAM_DATA),
      share(),
    );

  @Effect({dispatch: false}) setSymbols = this.actions$
    .pipe(
      ofType<StreamDetailsActions.SetSymbols>(StreamDetailsActionTypes.SET_SYMBOLS),
      share(),
    );

  @Effect({dispatch: false}) saveGlobalFilterState = this.actions$
    .pipe(
      ofType<StreamDetailsActions.SaveGlobalFilterState>(StreamDetailsActionTypes.SAVE_GLOBAL_FILTER_STATE),
      map(action => {
        if (action.payload.global_filter) {
          localStorage.setItem('global_filter', JSON.stringify(action.payload.global_filter));
        }
        return false;
      }),
      share(),
    );
  @Effect({dispatch: false}) setGlobalFilterState = this.actions$
    .pipe(
      ofType<StreamDetailsActions.SetGlobalFilterState>(StreamDetailsActionTypes.SET_GLOBAL_FILTER_STATE),
      share(),
    );

  @Effect({dispatch: false}) clearGlobalFilterState = this.actions$
    .pipe(
      ofType<StreamDetailsActions.ClearGlobalFilterState>(StreamDetailsActionTypes.CLEAR_GLOBAL_FILTER_STATE),
      map(() => {
          localStorage.setItem('global_filter', '');

          return false;
        },
      ),
      share(),
    );

  private stop_subscription$ = new Subject();
  @Effect() getSchema = this.actions$
    .pipe(
      ofType<StreamDetailsActions.GetSchema>(StreamDetailsActionTypes.GET_SCHEMA),
      map(action => action.payload.streamId),
      // distinctUntilChanged(),
      switchMap(streamId => {
        return this.httpClient
          .get(`/${encodeURIComponent(streamId)}/schema`)
          .pipe(
            takeUntil(this.stop_subscription$),
            map((resp) => {
              let schemaTypes = [];
              let schemaAll = [];

              if (resp) {
                if (resp['types']) {
                  schemaTypes = resp['types'];
                }
                if (resp['all']) {
                  schemaAll = resp['all'];
                }
              }
              return new StreamDetailsActions.SetSchema({
                schema: schemaTypes,
                schemaAll: schemaAll,
              });
            }),
          );
      }),
    );
  @Effect({dispatch: false}) stopSubscriptions = this.actions$
    .pipe(
      ofType(StreamDetailsActionTypes.STOP_SUBSCRIPTIONS),
      tap(() => {
        this.stop_subscription$.next(true);
        this.stop_subscription$.complete();
        this.stop_subscription$ = new Subject();
      }),
    );
  @Effect() getSymbols = this.actions$
    .pipe(
      ofType<StreamDetailsActions.GetSymbols>(StreamDetailsActionTypes.GET_SYMBOLS),
      map(action => action.payload.streamId),
      // distinctUntilChanged(),
      switchMap(streamId => {
        return this.httpClient
          .get(`/${encodeURIComponent(streamId)}/symbols`)
          .pipe(
            takeUntil(this.stop_subscription$),
            map((resp: Array<string>) => {
              return new StreamDetailsActions.SetSymbols({
                symbols: resp,
              });
            }),
          );
      }),
    );
  private tabs_activated: boolean;
  @Effect() subscribeTabChanges = this.actions$
    .pipe(
      ofType<StreamDetailsActions.SubscribeTabChanges>(StreamDetailsActionTypes.SUBSCRIBE_TAB_CHANGES),
      switchMap(() => {
        return this.appStore.pipe(
          select(getTabsState),
          filter(state => !!state.tabs.length),
          map((state) => {
            return state.tabs.find(tab => tab.active) || state.tabs[0];
          }),
          distinctUntilChanged(),
          mergeMap((activeTab: TabModel) => {
            const reset_tabs = [];
            if (this.tabs_activated) {
              reset_tabs.push(new FilterActions.ResetState());
            } else {
              this.tabs_activated = true;
            }
            return [
              ...reset_tabs,
              new StreamDetailsActions.CleanStreamData(),
              new StreamDetailsActions.GetSchema({
                streamId: activeTab.stream,
              }),
              // new StreamDetailsActions.GetStreamData({
              //   activeTab: activeTab,
              // }),
            ];
          }),
          takeUntil(this.stop_subscription$),
        );
      }),
    );

  @Effect() getStreamRange = this.actions$
    .pipe(
      ofType<StreamDetailsActions.GetStreamRange>(StreamDetailsActionTypes.GET_STREAM_RANGE),
      // distinctUntilChanged(),
      switchMap((action) => {
        let symbolQueryString = '';
        if (action.payload.symbol) {
          symbolQueryString = `?symbols=${encodeURIComponent(action.payload.symbol)}`;
        }
        return this.httpClient
          .get<{ end: string, start: string }>(`/${encodeURIComponent(action.payload.streamId)}/range${symbolQueryString}`)
          .pipe(
            map((streamRange) => {
              return new StreamDetailsActions.SetStreamRange({
                streamRange: streamRange,
              });
            }),
          );
      }),
    );


  constructor(
    private actions$: Actions,
    private httpClient: HttpClient,
    private appStore: Store<AppState>,
  ) { }


}
