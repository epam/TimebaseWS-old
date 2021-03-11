import { HttpClient }                            from '@angular/common/http';
import { Injectable, NgZone }                    from '@angular/core';
import { Actions, Effect, ofType }               from '@ngrx/effects';
import { Store }                                 from '@ngrx/store';
import { fromEvent, Observable, of, Subject }    from 'rxjs';
import { map, share, switchMap, takeUntil, tap } from 'rxjs/operators';
import { WSService }                             from '../../services/ws.service';
import * as AppActions                           from './app.actions';
import { AppActionTypes }                        from './app.actions';
import { AppState }                              from '../index';
import { AppSettingsModel }                      from '../../../shared/models/app.settings.model';
import { TranslateService }                      from '@ngx-translate/core';
import * as NotificationsActions                 from '../../modules/notifications/store/notifications.actions';
import * as AuthActions                          from '../auth/auth.actions';

@Injectable()
export class AppEffects {

  private stop_system_subscription$ = new Subject();

  constructor(
    private actions$: Actions,
    private appStore: Store<AppState>,
    private translate: TranslateService,
    private httpClient: HttpClient,
    private wsService: WSService,
    private _ngZone: NgZone,
  ) {}

  @Effect() getAppVer = this.actions$
    .pipe(
      ofType(AppActionTypes.GET_APP_VER),
      switchMap(() => {
        return this.httpClient
          .get('/config/version')
          .pipe(
            switchMap(appVer => {
              return of(new AppActions.SetAppVer(appVer['version']));
            }),
          );
      }),
    );


  @Effect() getAppSettings = this.actions$
    .pipe(
      ofType(AppActionTypes.GET_APP_SETTINGS),
      switchMap(() => {
        return this.httpClient
          .get<AppSettingsModel>('/settings')
          .pipe(
            switchMap(appSettings => {
              return of(new AppActions.SetAppSettings({settings: appSettings}));
            }),
          );
      }),
    );

  @Effect() addSystemSubscription = this.actions$
    .pipe(
      ofType(AppActionTypes.ADD_SYSTEM_SUBSCRIPTION),
      switchMap(() => {
        return this._ngZone.runOutsideAngular<Observable<any>>(() => {
          return this.wsService.watch(`/topic/system`)
            .pipe(
              map(ws_message => JSON.parse(ws_message.body)),
              takeUntil(this.stop_system_subscription$),
              map((message: any/*SystemMessageModel*/) => {
                return {
                  type: `SYSTEM_MESSAGE_${message.message_type}`,
                  payload: message.message,
                };
              }),
            );
        });
      }),
    );

  @Effect({dispatch: false}) systemSubscriptionIsTriggered = this.actions$
    .pipe(
      ofType(AppActionTypes.SYSTEM_SUBSCRIPTION_IS_TRIGGERED),
      share(),
    );

  @Effect({dispatch: false}) stopSystemSubscription = this.actions$
    .pipe(
      ofType(AppActionTypes.STOP_SYSTEM_SUBSCRIPTION),
      tap(() => {
        this.stop_system_subscription$.next(true);
        this.stop_system_subscription$.complete();
      }),
    );

  @Effect() getCurrencies = this.actions$
    .pipe(
      ofType(AppActionTypes.GET_CURRENCIES),
      switchMap(() => {
        return this.httpClient
          .get('/currencies')

          .pipe(
            map((resp: Array<string>) => {
              return new AppActions.SetCurrencies({
                currencies: resp,
              });
            }),
          );
      }),
    );

  @Effect({}) startListeningVisibilityChange = this.actions$
    .pipe(
      ofType(AppActionTypes.START_LISTENING_VISIBILITY_CHANGE),
      tap(() => {
        this.appStore.dispatch(new AppActions.ChangeDocVisibility({
          visible: window.document.visibilityState !== 'hidden',
        }));
      }),
      switchMap(() => {
        return fromEvent(window, 'visibilitychange')
          .pipe(
            takeUntil(this.stop_system_subscription$),
            map(() => {
              return new AppActions.ChangeDocVisibility({
                visible: window.document.visibilityState !== 'hidden',
              });
            }),
          );
      }),
    );

  @Effect() showLoginAlert = this.actions$.pipe(
    ofType<AppActions.ShowLoginAlert>(AppActionTypes.SHOW_LOGIN_ALERT),
    // switchMap(() => this.appStore.pipe(select(getAppState))),
    // filter(appState => !appState.preventRequests),
    // take(1),
    tap(() => this.appStore.dispatch(new AppActions.PreventHTTPRequests())),
    switchMap(() => this.translate.get('notification_messages.auth_error_notification')),
    map((notification_message) => {
      return new NotificationsActions.AddWarn({
        dismissible: true,
        message: notification_message,
        closeAction: () => {
          this.appStore.dispatch(new AuthActions.LogOut());
          // this.appStore.dispatch(new AuthActions.RedirectToAuthProvider());
        },
      });
    }),
  );

  @Effect({dispatch: false}) offerToSaveFile = this.actions$
    .pipe(
      ofType<AppActions.OfferToSaveFile>(AppActionTypes.OFFER_TO_SAVE_FILE),
      tap(action => {
        const blob = new Blob([action.payload.data], {type: action.payload.fileType.toString()});
        const a = window.document.createElement('a');
        const blobURL = window.URL.createObjectURL(blob);
        if (action.payload.fileName) a.download = action.payload.fileName;
        a.href = blobURL;
        a.click();
      }),
    );
}
