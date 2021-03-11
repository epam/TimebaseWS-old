import { Injectable }                                                        from '@angular/core';
import { HttpEvent, HttpHandler, HttpHeaders, HttpInterceptor, HttpRequest } from '@angular/common/http';
import * as NotificationsActions
                                                                             from '../../modules/notifications/store/notifications.actions';
import { catchError, filter, switchMap, take }                               from 'rxjs/operators';
import { TranslateService }                                                  from '@ngx-translate/core';
import { select, Store }                                                     from '@ngrx/store';

import { Observable, throwError } from 'rxjs';
import { AppState }               from '../../store';
import { getAppState }            from '../../store/app/app.selectors';

@Injectable()
export class RequestDefaultErrorInterceptor implements HttpInterceptor {
  constructor(
    private translate: TranslateService,
    private appStore: Store<AppState>,
  ) {}

  private handleError(error) {
    if (error && error.status && error.status !== 401) {
      return this.translate.get('notification_messages')
        .pipe(
          take(1),
          switchMap(messages => {
            this.appStore.dispatch(new NotificationsActions.AddAlert({
              message: error && error.error && error.error.message ? `${error.error.message}` /* messages.network_error + (error && error.error && error.error.message ? `<br /> ${error.error.message}`*/ : messages.network_error,
              dismissible: true,
              closeInterval: 5000,
            }));
            return throwError(error);
          }),
        );
    } else {
      return throwError(error);
    }
  }

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const customError = req.headers.get('customError'),
      headers = {};

    req.headers.keys().forEach(key => {
      headers[key] = req.headers.get(key);
    });

    delete headers['customError'];

    if (!customError && !headers['ignoreApiPrefix']) {
      return this.appStore
        .pipe(
          select(getAppState),
          filter(appState => !appState.preventRequests),
          take(1),
          switchMap(() => {
            return next
              .handle(req.clone({headers: new HttpHeaders(headers)}))
              .pipe(
                catchError(this.handleError.bind(this)),
              );
          }),
        );
    } else {
      // delete headers['customError'];
      return next
        .handle(req.clone({headers: new HttpHeaders(headers)}));
    }
  }
}
