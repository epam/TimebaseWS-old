import { Action } from '@ngrx/store';
import { StreamDetailsModel } from '../../models/stream.details.model';
import { StreamQueryModel } from '../../models/query.model';

export enum StreamQueryActionTypes {
  GET_STREAMS_QUERY = '[Streams] Get Streams Query',
  SET_STREAMS_QUERY = '[Streams] Set Streams Query',
  GET_STREAMS_QUERY_DESCRIBE = '[Streams] Get Streams Query Describe',
  CLEAR_STREAMS_QUERY = '[Streams] Clear Streams Query',
}


export class GetStreamsQuery implements Action {
  readonly type = StreamQueryActionTypes.GET_STREAMS_QUERY;

  constructor(public payload: {
    query: StreamQueryModel;
  }) { }
}

export class SetStreamsQuery implements Action {
  readonly type = StreamQueryActionTypes.SET_STREAMS_QUERY;

  constructor(public payload: {
    queryStreams: StreamDetailsModel[],
  }) { }
}


export class GetStreamsQueryDescribe implements Action {
  readonly type = StreamQueryActionTypes.GET_STREAMS_QUERY_DESCRIBE;

  constructor(public payload: {
    query: string;
  }) { }
}


export class ClearStreamsQuery implements Action {
  readonly type = StreamQueryActionTypes.CLEAR_STREAMS_QUERY;
}

export type StreamQueryActions = 
  GetStreamsQuery |
  SetStreamsQuery |
  GetStreamsQueryDescribe |
  ClearStreamsQuery;
