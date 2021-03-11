import { Injectable, NgZone }       from '@angular/core';
import { Actions, Effect, ofType }  from '@ngrx/effects';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { StreamDescribeModel }      from '../../models/stream.describe.model';
import * as StreamsActions          from './streams.actions';
import { StreamsActionTypes }       from './streams.actions';

import { map, mergeMap, share, switchMap, takeUntil, tap } from 'rxjs/operators';
import { StreamModel }                                     from '../../models/stream.model';
import { SymbolModel }                                     from '../../models/symbol.model';
import { Observable, Subject }                             from 'rxjs';
import { Store }                                           from '@ngrx/store';
import * as fromStreams                                    from './streams.reducer';

import * as NotificationsActions from '../../../../core/modules/notifications/store/notifications.actions';

import { TranslateService }    from '@ngx-translate/core';
import { AppState }            from '../../../../core/store';
import { WSService }           from '../../../../core/services/ws.service';
import { StreamsStateModel }   from '../../models/streams.state.model';
import * as AppActions         from '../../../../core/store/app/app.actions';
import * as StreamsTabsActions from '../streams-tabs/streams-tabs.actions';


@Injectable()
export class StreamsEffects {
  // private stop_tabs_subscription$ = new Subject();
  private stop_streams_state_subscription$ = new Subject();

  constructor(
    private actions$: Actions,
    private httpClient: HttpClient,
    private streamsStore: Store<fromStreams.FeatureState>,
    private translate: TranslateService,
    private appStore: Store<AppState>,
    private wsService: WSService,
    private _ngZone: NgZone,
  ) { }

  @Effect() getStreams = this.actions$
    .pipe(
      ofType<StreamsActions.GetStreams>(StreamsActionTypes.GET_STREAMS),
      switchMap(action => {
        let req = `/streams`;
        if (action.payload.props && action.payload.props._filter && action.payload.props._filter.length) {
          req = `/streams?filter=${encodeURIComponent(action.payload.props._filter)}`;
        }
        return this.httpClient
          .get<StreamModel[]>(req)
          .pipe(
            map(resp => {
              const streams = resp.sort((a, b) => (a.key.toLowerCase() > b.key.toLowerCase()) ? 1 : ((b.key.toLowerCase() > a.key.toLowerCase()) ? -1 : 0));
              return new StreamsActions.SetStreams({streams: streams});
            }),
          );
      }),
    );

  @Effect() getSymbols = this.actions$
    .pipe(
      ofType<StreamsActions.GetSymbols>(StreamsActionTypes.GET_SYMBOLS),
      switchMap(action => {
        let req = `/${encodeURIComponent(action.payload.streamKey)}/symbols`;
        if (action.payload.props && action.payload.props._filter && action.payload.props._filter.length) {
          req = `/${encodeURIComponent(action.payload.streamKey)}/symbols?filter=${encodeURIComponent(action.payload.props._filter)}`;
        }
        return this.httpClient
          .get<SymbolModel[]>(req)
          .pipe(
            map(resp => {
              return new StreamsActions.SetSymbols({
                streamKey: action.payload.streamKey,
                symbols: resp,
              });
            }),
          );
      }),
    );

  @Effect() showStreamSymbols = this.actions$
    .pipe(
      ofType<StreamsActions.ShowStreamSymbols>(StreamsActionTypes.SHOW_STREAM_SYMBOLS),
      switchMap(action =>
        [
          new StreamsActions.SetStreamState({
            stream: action.payload.stream,
            props: {
              _shown: true,
            },
          }),
          new StreamsActions.GetSymbols({
            streamKey: action.payload.stream.key,
            props: action.payload.props,
          }),
        ]),
    );

  @Effect() truncateStream = this.actions$
    .pipe(
      ofType<StreamsActions.TruncateStream>(StreamsActionTypes.TRUNCATE_STREAM),
      switchMap(action => {
        // const params = {};
        // for (const i in action.payload.params) {
        //   if (action.payload.params.hasOwnProperty(i)) {
        //     params[i] = action.payload.params[i];
        //   }
        // }
        return this.httpClient
          .post<SymbolModel[]>(`${encodeURIComponent(action.payload.streamKey)}/truncate`, action.payload.params)
          .pipe(
            switchMap(() => this.translate.get('notification_messages')),
            mergeMap((messages) => {
              return [
                new NotificationsActions.AddNotification({
                  message: messages.stream_truncated_succeeded,
                  dismissible: true,
                  closeInterval: 2000,
                  type: 'success',
                }),
                new StreamsActions.CloseModal(),
              ];
            }),
          );
      }),
    );

  @Effect() purgeStream = this.actions$
    .pipe(
      ofType<StreamsActions.PurgeStream>(StreamsActionTypes.PURGE_STREAM),
      switchMap(action => {
        // const params = new FormData();
        // for (const i in action.payload.params) {
        //   if (action.payload.params.hasOwnProperty(i)) {
        //     params[i] = action.payload.params[i];
        //   }
        // }
        return this.httpClient
          .post<SymbolModel[]>(`${encodeURIComponent(action.payload.streamKey)}/purge`, action.payload.params)
          .pipe(
            switchMap(() => this.translate.get('notification_messages')),
            mergeMap((messages) => {
              return [
                new NotificationsActions.AddNotification({
                  message: messages.stream_purged_succeeded,
                  dismissible: true,
                  closeInterval: 2000,
                  type: 'success',
                }),
                new StreamsActions.CloseModal(),
              ];
            }),
          );
      }),
    );
  @Effect() getStreamDescribe = this.actions$
    .pipe(
      ofType<StreamsActions.GetStreamDescribe>(StreamsActionTypes.GET_STREAM_DESCRIBE),
      switchMap(action => {
        return this.httpClient
          .get<StreamDescribeModel>(`${encodeURIComponent(action.payload.streamId)}/describe`)
          .pipe(
            map((resp) => {
              return new StreamsActions.SetStreamDescribe({describe: resp});
            }),
          );
      }),
    );

  @Effect() deleteStream = this.actions$
    .pipe(
      ofType<StreamsActions.AskToDeleteStream>(StreamsActionTypes.ASK_TO_DELETE_STREAM),
      switchMap(action => {
        return this.httpClient
          .post(`${encodeURIComponent(action.payload.streamKey)}/delete`, {})
          .pipe(
            switchMap(() => this.translate.get('notification_messages')),
            mergeMap((messages) => {
              return [
                new NotificationsActions.AddNotification({
                  message: messages.streamDeletedSucceeded,
                  dismissible: true,
                  closeInterval: 2000,
                  type: 'success',
                }),
              ];
            }),
          );
      }),
    );
  @Effect() askToRenameStream = this.actions$
    .pipe(
      ofType<StreamsActions.AskToRenameStream>(StreamsActionTypes.ASK_TO_RENAME_STREAM),
      switchMap(action => {
        const data = new FormData();
        data.append('newStreamId', action.payload.streamName);
        return this.httpClient
          .post(`${encodeURIComponent(action.payload.streamId)}/rename`, data)
          .pipe(
            switchMap(() => this.translate.get('notification_messages')),
            mergeMap((messages) => {
              return [
                new NotificationsActions.AddNotification({
                  message: messages.streamRenameSucceeded,
                  dismissible: true,
                  closeInterval: 2000,
                  type: 'success',
                }),
                // new StreamsActions.RenameStream({
                //   streamId: action.payload.streamId,
                //   streamName: action.payload.streamName,
                // }),
                // new StreamsTabsActions.RemoveStreamTabs({streamKey: action.payload.streamId}),
              ];
            }),
          );
      }),
    );

  @Effect() askToRenameSymbol = this.actions$
    .pipe(
      ofType<StreamsActions.AskToRenameSymbol>(StreamsActionTypes.ASK_TO_RENAME_SYMBOL),
      switchMap(action => {
        const data = new FormData();
        data.append('newSymbol', action.payload.newSymbolName);
        return this.httpClient
          .post(`${encodeURIComponent(action.payload.streamId)}/${encodeURIComponent(action.payload.oldSymbolName)}/rename`, data)
          .pipe(
            switchMap(() => this.translate.get('notification_messages')),
            mergeMap((messages) => {
              return [
                new NotificationsActions.AddNotification({
                  message: messages.symbolRenameSucceeded,
                  dismissible: true,
                  closeInterval: 2000,
                  type: 'success',
                }),
                // new StreamsActions.RenameSymbol({
                //   streamId: action.payload.streamId,
                //   oldSymbolName: action.payload.oldSymbolName,
                //   newSymbolName: action.payload.newSymbolName,
                // }),
                // new StreamsTabsActions.RemoveSymbolTabs({
                //   streamId: action.payload.streamId,
                //   symbolName: action.payload.oldSymbolName,
                // }),
              ];
            }),
          );
      }),
    );

  @Effect({dispatch: false}) closeModal = this.actions$
    .pipe(
      ofType<StreamsActions.CloseModal>(StreamsActionTypes.CLOSE_MODAL),
      share(),
    );


  @Effect({dispatch: false}) setStreamStatesSubscription = this.actions$
    .pipe(
      ofType<StreamsActions.SetStreamStatesSubscription>(StreamsActionTypes.SET_STREAM_STATES_SUBSCRIPTION),
      map(action => action.payload.dbState),
      tap((new_dbState: StreamsStateModel) => {
        if (new_dbState.renamed.length) {
          new_dbState.renamed.forEach((id, idx) => {
            this.appStore.dispatch(new StreamsActions.RenameStream({
              streamId: new_dbState.renamed[idx].oldName,
              streamName: new_dbState.renamed[idx].newName,
            }));
            console.log(`Stream ${new_dbState.renamed[idx].oldName} was renamed to ${new_dbState.renamed[idx].newName}`);
            this.appStore.dispatch(new StreamsTabsActions.RemoveStreamTabs({streamKey: new_dbState.renamed[idx].oldName}));
          });
        }
        /*if (new_dbState.changed.length) {
          new_dbState.changed.forEach((id, idx) => {
            this.appStore.dispatch(new StreamsActions.RenameStream({
              streamId: new_dbState.renamed[idx].oldName,
              streamName: new_dbState.renamed[idx].newName,
            }));
            this.appStore.dispatch(new StreamsTabsActions.RemoveStreamTabs({streamKey: new_dbState.renamed[idx].oldName}));
          });
        }*/

        if (new_dbState.deleted.length) {
          new_dbState.deleted.forEach(id => {
            console.log(`Stream ${id} was deleted`);
            this.appStore.dispatch(new StreamsActions.DeleteStream({streamKey: id}));
            this.appStore.dispatch(new StreamsTabsActions.RemoveStreamTabs({streamKey: id}));
          });
        }
        if (new_dbState.added.length) {
          this.streamsStore.dispatch(new StreamsActions.GetStreams({}));
          /*if (new_dbState.changed.length) {
            new_dbState.changed.forEach((id) => {
              this.appStore.dispatch(new StreamsTabsActions.RemoveStreamTabs({streamKey: id}));
            });
          }*/
        }
      }),
    );

  @Effect() addStreamStatesSubscription = this.actions$
    .pipe(
      ofType<StreamsActions.AddStreamStatesSubscription>(StreamsActionTypes.ADD_STREAM_STATES_SUBSCRIPTION),
      switchMap(() => {
        return this._ngZone.runOutsideAngular<Observable<any>>(() => {
          return this.wsService.watch(`/topic/streams`)
            .pipe(
              map(ws_message => JSON.parse(ws_message.body)),
              takeUntil(this.stop_streams_state_subscription$),
              map((data: StreamsStateModel) => {
                if (data.renamed.length || data.deleted.length || data.added.length) { // TODO: Rewrite this part to interact with changed state
                  this.streamsStore.dispatch(new StreamsActions.SetStreamStatesSubscription({dbState: data}));
                }
                // let alert = '';
                // if (data) {
                //   if (data.added && data.added.length) {
                //     alert = data.messageType + ': Added stream(s) ' + data.added.join(', ');
                //   }
                //   if (data.deleted && data.deleted.length) {
                //     alert = data.messageType + ': Deleted stream(s) ' + data.deleted.join(', ');
                //   }
                //   if (data.changed && data.changed.length) {
                //     alert = data.messageType + ': Changed stream(s) ' + data.changed.join(', ');
                //   }
                //   if (data.renamed && data.renamed.length) {
                //     const alertTextArray = [];
                //     for (let i = 0; i < data.renamed.length; i++) {
                //       alertTextArray.push('from ' + data.renamed[i].oldname + ' on ' + data.renamed[i].newName);
                //     }
                //     alert = data.messageType + ': Renamed stream(s) ' + alertTextArray.join(', ');
                //
                //   }
                // }
                // this.appStore.dispatch(new NotificationsActions.AddNotification({
                //   message: alert,
                //   dismissible: true,
                //   closeInterval: 2000,
                //   type: 'success',
                // }));
                return {
                  type: `STREAM_STATES_MESSAGE_${data.messageType}`,
                  payload: data,
                };

              }),
            );
        });
      }),
    );


  @Effect({dispatch: false}) stopStreamStatesSubscription = this.actions$
    .pipe(
      ofType<StreamsActions.StopStreamStatesSubscription>(StreamsActionTypes.STOP_STREAM_STATES_SUBSCRIPTION),
      tap(() => {
        this.stop_streams_state_subscription$.next(true);
        this.stop_streams_state_subscription$.complete();
      }),
    );


  @Effect() downloadQSMSGFile = this.actions$
    .pipe(
      ofType<StreamsActions.DownloadQSMSGFile>(StreamsActionTypes.DOWNLOAD_QSMSG_FILE),
      switchMap(action => {
        return this.httpClient.get(`/${action.payload.streamId}/export`, {
            observe: 'response',
            responseType: 'arraybuffer',
          })
          .pipe(
            map((resp: HttpResponse<any>) => {
              const CONTENT_HEADER = (resp.headers.get('content-disposition')).replace('attachment;filename=', '');
              return new AppActions.OfferToSaveFile({
                data: resp.body,
                fileType: resp.headers.get('content-type'),
                fileName: CONTENT_HEADER,
              });
            }),
          );
      }),
    );
  @Effect() sendMessage = this.actions$
    .pipe(
      ofType<StreamsActions.SendMessage>(StreamsActionTypes.SEND_MESSAGE),
      switchMap(action => {
        return this.httpClient.post(`/${action.payload.streamId}/write`, action.payload.messages)
          .pipe(
            switchMap(() => this.translate.get('notification_messages')),
            mergeMap((messages) => {
              return [
                new NotificationsActions.AddNotification({
                  message: messages.sendMessageSucceeded,
                  dismissible: true,
                  closeInterval: 2000,
                  type: 'success',
                }),
              ];
            }),
          );
      }),
    );

}
