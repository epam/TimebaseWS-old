import { distinctUntilChanged, filter, map, mergeMap, switchMap, takeUntil, tap } from 'rxjs/operators';
import { Subject }                                                                from 'rxjs';

import { Injectable }              from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { HttpClient }              from '@angular/common/http';
import { select, Store }           from '@ngrx/store';
import * as StreamPropsActions     from './stream-props.actions';
import { StreamPropsActionTypes }  from './stream-props.actions';
import { PropsModel }              from '../../models/props.model';
import { TabModel }                from '../../models/tab.model';
import * as TimelineBarActions     from '../timeline-bar/timeline-bar.actions';
import { AppState }                from '../../../../core/store';
import { getTabsState }            from '../streams-tabs/streams-tabs.selectors';

@Injectable()
export class StreamPropsEffects {
  private stop_subscription$ = new Subject();
  private changed_props_state$ = new Subject();
  @Effect() getProps = this.actions$
    .pipe(
      ofType<StreamPropsActions.GetProps>(StreamPropsActionTypes.GET_PROPS),
      switchMap(() => {
        return this.appStore.pipe(
          select(getTabsState),
          filter(state => !!state.tabs.length),
          map((state) => {
            return state.tabs.find(tab => tab.active) || state.tabs[0];
          }),
          distinctUntilChanged(),
          takeUntil(this.stop_subscription$),
        );
      }),
      switchMap((activeTab: TabModel) => {
        return this.httpClient
          .get<PropsModel>(`/${encodeURIComponent(activeTab.stream)}/options`)
          .pipe(
            takeUntil(this.stop_subscription$),
            mergeMap(resp => {
              let startDate = null;
              let endDate = null;
              let props = null;
              if (resp) {
                props = resp;
                if (resp.range) {
                  startDate = resp.range.start;
                  endDate = resp.range.end;
                }
              }
              return [
                new TimelineBarActions.SetStartDate({
                  date: startDate,
                }),
                new TimelineBarActions.SetEndDate({
                  date: endDate,
                }),
                new StreamPropsActions.SetProps({
                  props: props,
                }),
              ];

            }),
          );
      }),
    );
  @Effect({dispatch: false}) stopSubscriptions = this.actions$
    .pipe(
      ofType(StreamPropsActionTypes.STOP_SUBSCRIPTIONS),
      tap(() => {
        this.stop_subscription$.next(true);
        this.stop_subscription$.complete();
        this.stop_subscription$ = new Subject();
      }),
    );

  @Effect({dispatch: false}) changeStateProps = this.actions$
    .pipe(
      ofType(StreamPropsActionTypes.CHANGE_STATE_PROPS),
      tap(() => {
        this.changed_props_state$.next(true);
        this.changed_props_state$.complete();
        this.changed_props_state$ = new Subject();
      }),
    );

  constructor(
    private actions$: Actions,
    private httpClient: HttpClient,
    private appStore: Store<AppState>,
    // private streamsStore: Store<fromStreams.FeatureState>,
  ) { }

}
