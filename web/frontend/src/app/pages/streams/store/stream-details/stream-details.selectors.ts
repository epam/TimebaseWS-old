import { createSelector }                     from '@ngrx/store';
import { StreamsState, streamsStoreSelector } from '../index';
import { State as DetailsState }              from './stream-details.reducer';
import { getStreamsList }                     from '../streams-list/streams.selectors';
import { StreamModel }                        from '../../models/stream.model';
import { TabModel }                           from '../../models/tab.model';

export const streamsDetailsStateSelector = createSelector(
  streamsStoreSelector,
  (state: StreamsState) => state.details,
);

export const getStreamSymbols = createSelector(
  streamsDetailsStateSelector,
  (state: DetailsState) => state.symbols,
);

export const getStreamData = createSelector(
  streamsDetailsStateSelector,
  (state: DetailsState) => state.streamData,
);

export const getStreamRange = createSelector(
  streamsDetailsStateSelector,
  (state: DetailsState) => state.streamRange,
);

export const getStreamGlobalFilters = createSelector(
  streamsDetailsStateSelector,
  (state: DetailsState) => state.global_filter,
);

export const getStreamOrSymbolByID = createSelector(
  getStreamsList,
  (streams: StreamModel[], props: { streamID: string, uid: string, symbol?: string }) => {
    if (!streams || !streams.length) {
      return null;
    }
    const SYMBOL_OBJ = props.symbol ? {symbol: props.symbol} : {};
    let stream = streams.find(stream => stream.key === props.streamID);
    if (!stream) {
      return null;
    }
    stream = {...stream};
    stream['stream'] = stream.key;
    delete stream.key;
    return new TabModel({
      ...stream,
      ...SYMBOL_OBJ,
      id: props.uid,
    });
  },
);
