export const FUNCTIONS_KEY = 'Functions';
export const AGGREGATIONS_KEY = 'Aggregations';
export const FIELDS_KEY = 'Fields';
export const PROPERTIES_KEY = 'Properties';

export enum Operator {
  EQUALS = '=',
  NOT_EQUAL = '!=',
  GREATER_THAN = '>',
  GREATER_THAN_OR_EQUALS = '>=',
  LESS_THAN = '<',
  LESS_THAN_OR_EQUALS = '<=',
  STARTS_WITH = 'Starts with',
  ENDS_WITH = 'Ends with',
  CONTAINS = 'Contains',
  NOT_CONTAINS = 'Not contains',
  IN = 'In',
  NOT_IN = 'Not in',
}

export enum SpecialValue {
  NULL = 'Null',
  NAN = 'NaN',
  PLUS_INF = '+Infinity',
  MINUS_INF = '-Infinity',
}

export const STRING_OPERATORS = [
  Operator.EQUALS,
  Operator.NOT_EQUAL,
  Operator.STARTS_WITH,
  Operator.ENDS_WITH,
  Operator.CONTAINS,
  Operator.NOT_CONTAINS,
  Operator.IN,
  Operator.NOT_IN,
];

export const NUMBER_OPERATORS = [
  Operator.EQUALS,
  Operator.NOT_EQUAL,
  Operator.GREATER_THAN,
  Operator.GREATER_THAN_OR_EQUALS,
  Operator.LESS_THAN,
  Operator.LESS_THAN_OR_EQUALS,
  Operator.IN,
  Operator.NOT_IN,
];

export const ENUM_OPERATORS = [Operator.EQUALS, Operator.NOT_EQUAL, Operator.IN, Operator.NOT_IN];
export const DEFAULT_OPERATOR = Operator.EQUALS;
export const DEFAULT_FUNCTION = 'last';
export const DATAFRAME_KEY = 'DATAFRAME';
export const REQUEST_TYPE = [DATAFRAME_KEY, 'TIMESERIES'];

export const EMPTY_SELECT = {
  selectedFunction: null,
  selectedRecordType: null,
  selectedField: null,
  selectedAggregations: [],
};
