import { DataQuery, DataSourceJsonData } from '@grafana/data';

import { FunctionValue } from './utils/types';

export interface MyQuery extends DataQuery {
  selectedStream: string | null;
  selectedSymbol: string | null;
  selectedInterval: number;
  selects: Select[];
  filters: Filter[];
  selectedGroups: string[];
  selectedOption: string | null;
  requestType: string | null;
}

export interface Select {
  selectedRecordType: string | null;
  selectedField: string | null;
  selectedFunction: FunctionValue | null;
  selectedAggregations: FunctionValue[];
}

export interface Filter {
  field: string;
  values: string[];
  operator: string;
}

export interface MyDataSourceOptions extends DataSourceJsonData {
  timebaseUrl?: string;
  timebaseUser?: string;
}
