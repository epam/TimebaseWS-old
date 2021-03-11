import { Injectable } from '@angular/core';
import { Actions, Effect, ofType } from '@ngrx/effects';
import { share, switchMap, map } from 'rxjs/operators';
import * as StreamQueryActions from './stream-query.actions';
import { StreamQueryActionTypes } from './stream-query.actions';
import * as StreamDetailsActions from '../stream-details/stream-details.actions';
import { StreamDetailsModel } from '../../models/stream.details.model';
import { HttpClient } from '@angular/common/http';

@Injectable()
export class StreamQueryEffects {
  @Effect() getStreamsQuery = this.actions$
    .pipe(
      ofType<StreamQueryActions.GetStreamsQuery>(StreamQueryActionTypes.GET_STREAMS_QUERY),
      switchMap(action => {
        return this.httpClient
       
          .post<StreamDetailsModel[]>(`/query`, action.payload.query)
          .pipe(
            map(resp => {
              return new StreamQueryActions.SetStreamsQuery({
                queryStreams: resp,
              });
            }),
          );
      }),
    );

    @Effect() getStreamsQueryDescribe = this.actions$
    .pipe(
      ofType<StreamQueryActions.GetStreamsQueryDescribe>(StreamQueryActionTypes.GET_STREAMS_QUERY_DESCRIBE),
      switchMap(action => {
        return this.httpClient
          .post<StreamDetailsModel[]>(`/describe`, {query: action.payload.query})
          .pipe(
            map(resp => {
              return new StreamDetailsActions.SetSchema({
                schema: resp['types'],
                schemaAll: resp['all'],
              });
            }),
          );
      }),
    );


  @Effect({ dispatch: false }) clearStreamsQuery = this.actions$
    .pipe(
      ofType<StreamQueryActions.ClearStreamsQuery>(StreamQueryActionTypes.CLEAR_STREAMS_QUERY),
      share(),
    );


  constructor(private actions$: Actions,
    private httpClient: HttpClient, ) { }

}
