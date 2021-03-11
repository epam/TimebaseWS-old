import {
  AppEvents,
  ArrayVector,
  DataQueryRequest,
  DataQueryResponse,
  DataSourceApi,
  DataSourceInstanceSettings,
} from '@grafana/data';
import { BackendSrvRequest, getBackendSrv, SystemJS } from '@grafana/runtime';

import { MyDataSourceOptions, MyQuery } from './types';
import { DATAFRAME_KEY } from './utils/constants';
import { getFilters, getFunctions, getInterval } from './utils/query';
import { getIntervals } from './utils/time-intervals';
import { Schema } from './utils/types';
import { separateTypeAndField } from './utils/utils';

const HEADERS = { 'Content-Type': 'application/json' };
const PREFIX = '/grafana/v0';
export const ALL_KEY = 'ALL()';

export class DataSource extends DataSourceApi<MyQuery, MyDataSourceOptions> {
  intervals: any;
  currencyInterval: any;
  isChangeCurrencyInterval = false;
  queryError: string | undefined;
  private appEvents: any;
  private url: string | undefined;

  constructor(instanceSettings: DataSourceInstanceSettings<MyDataSourceOptions>) {
    super(instanceSettings);
    this.url = instanceSettings.url;

    if (typeof instanceSettings.basicAuth === 'string' && instanceSettings.basicAuth.length > 0) {
      (HEADERS as any)['Authorization'] = instanceSettings.basicAuth;
    }

    SystemJS.load('app/core/app_events').then((appEvents: any) => {
      this.appEvents = appEvents;
    });
  }

  async query(options: DataQueryRequest<MyQuery>): Promise<DataQueryResponse> {
    this.isChangeCurrencyInterval = isChangeInterval(this.currencyInterval, options.range);

    this.currencyInterval = options.range;

    if (options.targets.some((target: any) => target.selectedStream == null)) {
      return { data: [] };
    }

    options.targets = options.targets.map((target: any) => {
      const interval = getInterval(target.selectedInterval, this.isChangeCurrencyInterval);
      if (
        this.isChangeCurrencyInterval &&
        !(
          interval.intervalType === 'MAX_DATA_POINTS' &&
          (isNaN(target.selectedInterval) ||
            this.intervals.find((interval: any) => interval.value === target.selectedInterval) == null)
        )
      ) {
        this.createAlert();
      }

      return {
        refId: target.refId,
        stream: target.selectedStream,
        queryType: 'CUSTOM',
        view: target.requestType == null ? DATAFRAME_KEY : target.requestType,
        symbols:
          target.selectedSymbol != null && target.selectedSymbol !== '' && target.selectedSymbol !== ALL_KEY
            ? [target.selectedSymbol]
            : [],
        hide: target.hide,
        types: [],
        functions: getFunctions(target.selects),
        interval,
        filters: getFilters(target.filters),
        groupBy:
          target.selectedGroups == null
            ? []
            : target.selectedGroups.map((group: string) => {
                const [type, field] = separateTypeAndField(group);
                return { type, name: field };
              }),
        groupByView:
          target.selectedGroups == null ? null : target.selectedOption == null ? 'COLUMN' : target.selectedOption,
      } as any;
    });
    this.intervals = getIntervals(options.maxDataPoints as any, options.range);
    const request$ = this.sendRequest('POST', '/queries/select', options);
    request$
      .then(() => {
        this.queryError = '';
      })
      .catch((er: { data: { message: string } }) => {
        this.queryError = er.data.message;
      });

    return request$.then(result => {
      for (const dataFrame of result.data) {
        if (dataFrame.fields != null) {
          for (const field of dataFrame.fields) {
            field.values = new ArrayVector(field.values);
          }
        }
      }
      return result;
    });
  }

  async testDatasource() {
    return getBackendSrv()
      .datasourceRequest({
        url: this.getUrl('/'),
        method: 'GET',
        headers: HEADERS,
      })
      .then((response: { status: number }) => {
        if (response.status === 200) {
          return { status: 'success', message: 'Data source is working', title: 'Success' };
        }

        return;
      })
      .catch((er: { data: { error_description: string } }) => {
        return { status: 'failed', message: er.data.error_description, title: 'Error' };
      });
  }

  getGroupByViewOptions() {
    return this.sendRequest('GET', '/groupByViewOptions').then(result => result.data);
  }

  getChartTypes(): Promise<string[]> {
    return this.sendRequest('POST', '/types', {}).then(result => result.data);
  }

  getStreams(template: string, offset: number): Promise<{ list: string[]; hasMore: boolean }> {
    return this.sendRequest('GET', `/streams?template=${template}&offset=${offset}&limit=50`).then(
      result => result.data
    );
  }

  getSymbols(template: string, stream: string, offset: number): Promise<{ list: string[]; hasMore: boolean }> {
    return this.sendRequest('GET', `/symbols?template=${template}&stream=${stream}&offset=${offset}&limit=50`).then(
      result => result.data
    );
  }

  getAggregations(): Promise<string[]> {
    return this.sendRequest('GET', '/aggregationTypes').then(result => result.data);
  }

  getStreamSchema(stream: string): Promise<Schema> {
    return this.sendRequest('GET', `/schema?stream=${stream}`).then(result => result.data);
  }

  private createAlert() {
    this.appEvents.emit(AppEvents.alertWarning, ['Selected grouping by time is reset due to time interval change.']);
  }

  private sendRequest(method: string, postfix: string, body?: any): Promise<any> {
    const option: BackendSrvRequest = {
      url: this.getUrl(postfix),
      method,
      headers: HEADERS,
    };

    if (body != null) {
      option.data = body;
    }
    return getBackendSrv().datasourceRequest(option);
  }

  private getUrl(postfix: string): string {
    return `${this.url}${PREFIX}${postfix}`;
  }
}

const isChangeInterval = (currencyInterval: any, range: any) => {
  if (currencyInterval == null) {
    return false;
  }
  let equalTo = false;
  if (typeof range.raw.to === 'string' && typeof currencyInterval.raw.to === 'string') {
    equalTo = currencyInterval.raw.to === range.raw.to;
  } else if (typeof range.raw.to !== 'string' && typeof currencyInterval.raw.to !== 'string') {
    equalTo = currencyInterval.raw.to.isSame(range.raw.to);
  }
  let equalFrom = false;
  if (typeof range.raw.from === 'string' && typeof currencyInterval.raw.from === 'string') {
    equalFrom = currencyInterval.raw.from === range.raw.from;
  } else if (typeof range.raw.from !== 'string' && typeof currencyInterval.raw.from !== 'string') {
    equalFrom = currencyInterval.raw.from.isSame(range.raw.from);
  }
  return !equalTo || !equalFrom;
};
