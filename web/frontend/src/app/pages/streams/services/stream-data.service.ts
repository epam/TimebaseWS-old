import { Injectable }                                      from '@angular/core';
import { HttpClient, HttpErrorResponse }                   from '@angular/common/http';
import { IServerSideDatasource, IServerSideGetRowsParams } from 'ag-grid-community';
import { select, Store }                                   from '@ngrx/store';
import { switchMap, take, takeUntil, withLatestFrom }      from 'rxjs/operators';
import { combineLatest, Subject }                          from 'rxjs';
import * as fromStreams                                    from '../store/streams-list/streams.reducer';
import { TabModel }                                        from '../models/tab.model';
import { StreamDetailsModel }    from '../models/stream.details.model';
import { AppState }              from '../../../core/store';
import { State as DetailsState } from '../store/stream-details/stream-details.reducer';
import * as TimelineBarActions                             from '../store/timeline-bar/timeline-bar.actions';
import { streamsDetailsStateSelector }                     from '../store/stream-details/stream-details.selectors';
import * as StreamDetailsActions from '../store/stream-details/stream-details.actions';
import { getActiveOrFirstTab }   from '../store/streams-tabs/streams-tabs.selectors';

@Injectable()
export class StreamDataService implements IServerSideDatasource {
  private destroy$ = new Subject();

  constructor(
    private httpClient: HttpClient,
    private streamsStore: Store<fromStreams.FeatureState>,
    private appStore: Store<AppState>,
  ) {
  }

  public getRows(params: IServerSideGetRowsParams): void {
    const request = params.request;

    this.destroy$.next(true);
    this.destroy$.complete();
    this.destroy$ = new Subject();

    // if (isRequestContainEmptyInFilter(request)) {
    //   params.successCallback([], 0);
    //   return;
    // }
    combineLatest([
      this.appStore.pipe(select(getActiveOrFirstTab)),
      this.appStore.pipe(select(streamsDetailsStateSelector)),
    ])
      .pipe(
        take(1),
        switchMap(([activeTab, streamsDetailsState]: [TabModel, DetailsState]) => {
          let params = {},
            url;
          const filter = activeTab.filter || {};
          if (activeTab.symbol) {
            url = `${encodeURIComponent(activeTab.stream)}/${encodeURIComponent(activeTab.symbol)}`;
          } else {
            url = `${encodeURIComponent(activeTab.stream)}`;
          }
          url += '/select';
          params = {
            ...params,
            'offset': request.startRow + '',
            'rows': (request.endRow - request.startRow) + '',
          };
          Object.keys(filter).forEach(key => {
            if (key != null &&
              key !== 'filter_symbols'
              && key !== 'filter_types'
              // && filter[key] !== 'tabName'
              && key !== 'filter_date_format'
              && key !== 'filter_time_format') {
              params[key] = filter[key];
            } else if (key === 'filter_symbols') {
              params['symbols'] = filter[key];
            } else if (key === 'filter_types') {
              params['types'] = filter[key];
            }
          });
          if (activeTab.reverse) {
            params['reverse'] = activeTab.reverse;
            if (!params['from']) {
              params['from'] = new Date().toISOString();
            }
          } else if (activeTab.filter && activeTab.filter.from == null) {
            delete params['from'];
          }

          this.appStore.dispatch(new TimelineBarActions.ClearLoadedDates());

          if (filter && filter['from']) {
            if (activeTab.reverse) {
              this.appStore.dispatch(new TimelineBarActions.SetLastLoadedDate({
                date: filter['from'],
              }));
            } else {
              this.appStore.dispatch(new TimelineBarActions.SetFirstLoadedDate({
                date: filter['from'],
              }));
            }
          }
          return this.httpClient
            .post<StreamDetailsModel[]>(url, params, {
              headers: {
                'customError': 'true',
              },
            });
        }),
        withLatestFrom(this.appStore.pipe(select(getActiveOrFirstTab))),
        takeUntil(this.destroy$),
      )
      .subscribe(([resp, activeTab]: [StreamDetailsModel[], TabModel]) => {
        this.appStore.dispatch(new StreamDetailsActions.SetStreamData({streamData: resp.map(streamDetails => new StreamDetailsModel(streamDetails))}));
        this.appStore.dispatch(new StreamDetailsActions.RemoveErrorMessage());
        if (resp) {
          if (resp.length) {
            const data = resp.map(streamDetails => new StreamDetailsModel(streamDetails));
            if (activeTab.reverse) {
              this.appStore.dispatch(new TimelineBarActions.SetFirstLoadedDate({
                date: resp[resp.length - 1].timestamp,
              }));
            } else {
              this.appStore.dispatch(new TimelineBarActions.SetLastLoadedDate({
                date: resp[resp.length - 1].timestamp,
              }));
            }
            params.successCallback(data, data.length < (request.endRow - request.startRow) ? request.startRow + data.length : -1);
          } else {
            params.successCallback([], 0);
          }
        } else {
          params.failCallback();
        }
      }, (error: HttpErrorResponse) => {
        this.appStore.dispatch(new StreamDetailsActions.AddErrorMessage({
          message: error.error && error.error.message ? error.error.message : error.message,
        }));
        params.successCallback([], 0);
      });
  }
}
