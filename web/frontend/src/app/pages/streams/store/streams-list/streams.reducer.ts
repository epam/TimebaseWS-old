import { StreamDescribeModel }                from '../../models/stream.describe.model';
import { StreamsActions, StreamsActionTypes } from './streams.actions';
import { AppState }                           from '../../../../core/store';
import { StreamModel }                        from '../../models/stream.model';
import { StreamsStateModel }                  from '../../models/streams.state.model';


export interface FeatureState extends AppState {
  streams: State;
  _openNewTab: boolean;
  dbState: StreamsStateModel;
}

export interface State {
  streams: StreamModel[];
  _openNewTab: boolean;
  dbState: StreamsStateModel;
  lasStreamDescribe: StreamDescribeModel;
  // tabs: TabModel[];
}

export const initialState: State = {
  streams: null,
  _openNewTab: false,
  dbState: null,
  // tabs: null,
  lasStreamDescribe: null,
};

const streamSorter = (stream1: StreamModel, stream2: StreamModel): number => {
  const STREAM_NAME_1 = (stream1.name ? stream1.name : stream1.key).toLocaleLowerCase(),
    STREAM_NAME_2 = (stream2.name ? stream2.name : stream2.key).toLocaleLowerCase();
  if (STREAM_NAME_1 > STREAM_NAME_2) {
      return 1;
  } else if (STREAM_NAME_1 < STREAM_NAME_2) {
      return -1;
  }
  return 0;
};

export function reducer(state = initialState, action: StreamsActions): State {
  let selectedStream, selectedStreamIndex/*, tabs: TabModel[], selectedTab: TabModel, selectedTabIndex: number*/;
  switch (action.type) {
    case StreamsActionTypes.SET_STREAMS:
      return {
        ...state,
        streams: action.payload.streams.sort(streamSorter),
      };

    case StreamsActionTypes.SET_STREAM_DESCRIBE:
      return {
        ...state,
        lasStreamDescribe: action.payload.describe,
      };

    case StreamsActionTypes.DELETE_STREAM:
      selectedStreamIndex = state.streams.findIndex(stream => stream.key === action.payload.streamKey);
      if (selectedStreamIndex > -1) {
        state.streams.splice(selectedStreamIndex, 1);
      }
      return {
        ...state,
        streams: [...state.streams]/*.sort(streamSorter)*/,
      };

    case StreamsActionTypes.RENAME_STREAM:
      selectedStreamIndex = state.streams.findIndex(stream => stream.key === action.payload.streamId);
      if (selectedStreamIndex > -1) {
        state.streams[selectedStreamIndex] = {
          ...state.streams[selectedStreamIndex],
          key: action.payload.streamName,
          name: action.payload.streamName,
        };
      }
      return {
        ...state,
        streams: [...state.streams].sort(streamSorter),
      };

    case StreamsActionTypes.RENAME_SYMBOL:
      selectedStreamIndex = state.streams.findIndex(stream => stream.key === action.payload.streamId);
      if (selectedStreamIndex > -1 &&
        state.streams[selectedStreamIndex]._symbolsList &&
        state.streams[selectedStreamIndex]._symbolsList.length) {
        const symbolIndex = state.streams[selectedStreamIndex]._symbolsList.findIndex(symbol => symbol === action.payload.oldSymbolName);
        if (symbolIndex > -1) {
          state.streams[selectedStreamIndex]._symbolsList.splice(symbolIndex, 1, action.payload.newSymbolName);
          state.streams[selectedStreamIndex] = {
            ...state.streams[selectedStreamIndex],
            _symbolsList: [...state.streams[selectedStreamIndex]._symbolsList],
          };
        }
      }
      return {
        ...state,
        streams: [...state.streams].sort(streamSorter),
      };

    case StreamsActionTypes.SET_SYMBOLS:
      selectedStream = state.streams.find(stream => stream.key === action.payload.streamKey);
      if (selectedStream) selectedStream._symbolsList = [...action.payload.symbols];
      return {
        ...state,
        streams: [...state.streams],
      };

    case StreamsActionTypes.SET_STREAM_STATE:
      selectedStreamIndex = state.streams.findIndex(stream => stream.key === action.payload.stream.key);
      if (selectedStreamIndex > -1) {
        state.streams[selectedStreamIndex] = {
          ...state.streams[selectedStreamIndex],
          ...action.payload.props,
        };
      }
      return {
        ...state,
        streams: [...state.streams],
      };

    case StreamsActionTypes.SET_NAVIGATION_STATE:
      return {
        ...state,

        _openNewTab: action.payload._openNewTab,
      };

      case StreamsActionTypes.SET_STREAM_STATES_SUBSCRIPTION:
        return {
          ...state,
          dbState: action.payload.dbState,
        };

    default:
      return state;
  }
}
