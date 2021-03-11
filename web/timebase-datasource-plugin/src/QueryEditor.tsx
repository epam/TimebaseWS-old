import { QueryEditorProps, SelectableValue } from '@grafana/data';
import { CascaderOption, Field, Segment } from '@grafana/ui';
import { css, cx } from 'emotion';
import React, { PureComponent } from 'react';
import Select from 'react-select';
import { AsyncPaginate } from 'react-select-async-paginate';

import { ALL_KEY, DataSource } from './DataSource';
import { MyDataSourceOptions, MyQuery } from './types';
import {
  AGGREGATIONS_KEY,
  DATAFRAME_KEY,
  DEFAULT_OPERATOR,
  EMPTY_SELECT,
  FIELDS_KEY,
  FUNCTIONS_KEY,
  PROPERTIES_KEY,
  REQUEST_TYPE,
} from './utils/constants';
import {
  getDefaultFunctionDescription,
  getId,
  getNewAggregationFunction,
  getNewFunction,
  getReturnFields,
  isEmptySelect,
} from './utils/functions';
import { getAggregationFunctionsOptions, getAvailableTypes, getFunctionsOptions } from './utils/options';
import { getCurrentInterval } from './utils/time-intervals';
import { FunctionDeclaration, FunctionValue, Schema } from './utils/types';
import {
  getErrorForFields,
  getListFields,
  getSelectedFieldType,
  getTypeWithField,
  getUsedFields,
  separateTypeAndField,
  toOption,
} from './utils/utils';
import { ColoredFieldLabelComponent, FieldLabelComponent } from './view/FieldLabels/FieldLabels';
import { FilterComponent } from './view/Filter/Filter';
import { FunctionComponent } from './view/Function/Function';
import { SegmentFrame } from './view/SegmentFrame/SegmentFrame';

const styles = css`
  &.width {
    width: 100%;
  }
  &.gf-form-mb {
    margin-bottom: 0;
  }
  &.gf-form-mr {
    margin-right: 4px;
  }
  &.gf-form-ml {
    margin-left: 344px;
  }
  .invalid-mess {
    margin: 0;
  }
  .select {
    div[class*='menu'] {
      .grafana-custom__option--is-selected {
        background-color: rgb(38, 132, 255) !important;
      }
      .grafana-custom__option--is-focused {
        background-color: transparent;
      }
      z-index: 10000;
      border-radius: inherit;
      background-color: rgb(11, 12, 14);
      color: #c7d0d9;
      border: 1px solid rgb(44, 50, 53);

      div[class*='option'] {
        &:hover {
          color: white;
          cursor: pointer;
        }
      }
    }
    input {
      color: #c7d0d9 !important;
    }
    div[class*='control'] {
      height: 32px;
      margin-right: 4px;
      border-radius: inherit;
      width: 220px !important;
      background-color: rgb(11, 12, 14);
      line-height: 1.5;
      font-size: 14px;
      color: #c7d0d9;
      min-height: 32px;
      flex-direction: row;
      max-width: 100%;
      align-items: center;
      cursor: default;
      display: flex;
      flex-wrap: wrap;
      justify-content: space-between;
      position: relative;
      border-radius: 2px;
      border: 1px solid rgb(44, 50, 53);
      padding: 0px 0px 0px 8px;
      div:first-child {
        padding: 0;
      }
      div[class*='singleValue'],
      div[class*='placeholder'] {
        left: -1px;
        top: 15px;
      }
      div[class*='singleValue'] {
        color: white;
      }
      div[class*='indicatorContainer'] {
        padding-bottom: 0;
        padding-top: 0;
      }
      span[class*='indicatorSeparator'] {
        display: none;
      }
    }
  }
`;

styles;
interface QueryEditorState {
  selectedStream: SelectableValue<string> | undefined;
  selectedSymbol: SelectableValue<string> | undefined;
  groupByViewOptions: Array<SelectableValue<string>>;
  schema: Schema | undefined;
  validStreamControl: boolean;
  validSymbolControl: boolean;
  hideControl: boolean;
  invalidFieldsMap: { [key: string]: boolean };
}
export class QueryEditor extends PureComponent<
  QueryEditorProps<DataSource, MyQuery, MyDataSourceOptions>,
  QueryEditorState
> {
  state: QueryEditorState = {
    groupByViewOptions: [],
    schema: void 0,
    selectedStream: void 0,
    selectedSymbol: void 0,
    validStreamControl: true,
    validSymbolControl: true,
    hideControl: false,
    invalidFieldsMap: {},
  };

  componentDidMount() {
    this.props.datasource.getGroupByViewOptions().then(options => {
      this.setState({
        ...this.state,
        groupByViewOptions: options.map(toOption),
      });
    });

    if (this.props.query.selectedStream != null) {
      this.restoreView();
    }
  }

  static getDerivedStateFromProps(nextProps: QueryEditorProps<DataSource, MyQuery, MyDataSourceOptions>) {
    return {
      selectedStream: nextProps.query.selectedStream == null ? null : toOption(nextProps.query.selectedStream),
      selectedSymbol: nextProps.query.selectedSymbol == null ? null : toOption(nextProps.query.selectedSymbol),
    };
  }

  loadStreams = (search: string, loadedOptions: any[]): any => {
    return this.props.datasource.getStreams(search == null ? '' : search, loadedOptions.length).then(response => {
      return {
        options: response.list.map(toOption),
        hasMore: response.hasMore,
      };
    });
  };

  loadSymbols = (search: string, loadedOptions: any[]): any => {
    return this.props.datasource
      .getSymbols(
        search == null ? '' : search,
        (this.state.selectedStream as SelectableValue<string>).value as string,
        loadedOptions.length
      )
      .then(response => {
        if (loadedOptions.length === 0) {
          response.list.unshift(ALL_KEY);
        }

        return {
          options: response.list.map(toOption),
          hasMore: response.hasMore,
        };
      });
  };

  getSelectOptions = (index: number) => {
    if (this.props.query.selects == null || this.props.query.selects[index] == null) {
      return;
    }

    const result = [
      { label: FIELDS_KEY, value: FIELDS_KEY, children: getListFields(this.state.schema?.types, true) },
      { label: FUNCTIONS_KEY, value: FUNCTIONS_KEY, children: getFunctionsOptions(this.state.schema?.functions) },
    ];
    const select = this.props.query.selects[index];

    if ((select.selectedFunction == null || !select.selectedFunction.isAggregation) && this.state.schema != null) {
      const aggregations = getAggregationFunctionsOptions(
        this.state.schema.functions,
        select.selectedAggregations.map(v => v.id),
        getAvailableTypes(select, this.state.schema)
      );

      if (aggregations.length !== 0) {
        result.unshift({
          label: AGGREGATIONS_KEY,
          value: AGGREGATIONS_KEY,
          children: aggregations,
        });
      }
    }

    if (
      select.selectedFunction != null &&
      select.selectedFunction.returnFields != null &&
      select.selectedFunction.returnFields.length !== 0
    ) {
      const description = this.state.schema?.functions?.find(f => f.id === select.selectedFunction?.id);

      if (description != null && description.returnFields != null) {
        const names = select.selectedFunction.returnFields.map(v => v.name);
        const filteredValue = description.returnFields.filter(v => !names.includes(v.constantName as string));
        if (filteredValue.length !== 0) {
          result.unshift({
            label: PROPERTIES_KEY,
            value: PROPERTIES_KEY,
            children: filteredValue.map(v => v.constantName).map(toOption),
          });
        }
      }
    }

    return result as CascaderOption[];
  };

  onChangeStream = (selectedStream: SelectableValue<string>) => {
    if (
      this.state.selectedStream != null &&
      !(
        (this.props.query.selectedSymbol == null || this.props.query.selectedSymbol === '') &&
        this.props.query.filters.length === 0 &&
        this.props.query.selects[0].selectedRecordType != null
      )
    ) {
      const isChange = confirm('Changing the stream will reset selected configuration. Do you want to continue?');
      if (!isChange) {
        return;
      }
      this.changeStream(selectedStream);
    } else {
      this.changeStream(selectedStream);
    }
  };

  addGroup = (selectedGroup: string[]) => {
    if (this.props.query.selectedGroups == null) {
      this.props.query.selectedGroups = [];
    }

    this.props.query.selectedGroups.push(selectedGroup[0]);
    this.props.onChange({ ...this.props.query });
    this.requestData();
  };

  private changeStream = (selectedStream: SelectableValue<string>) => {
    this.setState(state => ({
      ...state,
      validStreamControl: true,
    }));
    this.recreateSymbols();
    if (selectedStream == null) {
      this.setState({
        ...this.state,
        schema: void 0,
      });

      this.props.onChange({
        ...this.props.query,
        selectedStream: null,
        selectedSymbol: null,
        selects: [{ ...EMPTY_SELECT }],
        selectedGroups: [],
        filters: [],
      });

      this.requestData();
      return;
    }

    this.props.datasource.getStreamSchema(selectedStream.value as string).then(schema => {
      this.setState({
        ...this.state,
        schema,
      });
      this.props.onChange({
        ...this.props.query,
        selectedStream: selectedStream.value as string,
        selectedSymbol: ALL_KEY,
        selects: [{ ...EMPTY_SELECT }],
        selectedGroups: [],
        filters: [],
      });

      this.requestData();
    });
  };

  private recreateSymbols = () => {
    this.setState({
      ...this.state,
      hideControl: true,
    });
    setTimeout(() => {
      this.setState({
        ...this.state,
        hideControl: false,
      });
    });
  };

  addSelectSection = (values: string[], index: number | undefined) => {
    let select = this.props.query.selects[index as number];

    if (values[0] === AGGREGATIONS_KEY) {
      const newAggregationId = values[values.length - 1];
      const description = this.state.schema?.functions.find(f => f.id === newAggregationId) as FunctionDeclaration;
      const newAggregation = getNewAggregationFunction(description);

      if (select.selectedAggregations == null) {
        select.selectedAggregations = [];
      }
      select.selectedAggregations.push(newAggregation);
    }

    if (values[0] === FIELDS_KEY) {
      const newField = values[values.length - 1];
      const [type, filed] = separateTypeAndField(newField);
      const obj = {
        selectedField: filed,
        selectedRecordType: type,
        selectedAggregations: [getDefaultFunctionDescription(this.state.schema as Schema)],
        selectedFunction: null,
      };
      if (isEmptySelect(select)) {
        select.selectedField = obj.selectedField;
        select.selectedRecordType = obj.selectedRecordType;
        select.selectedAggregations = obj.selectedAggregations;
      } else {
        this.props.query.selects.push(obj);
      }
    }

    if (values[0] === FUNCTIONS_KEY) {
      const newFuncId = getId(values);
      const declaration = this.state.schema?.functions.find(f => f.id === newFuncId) as FunctionDeclaration;
      const functionValue = getNewFunction(declaration, values);
      const obj = {
        selectedAggregations: declaration.isAggregation
          ? []
          : [getDefaultFunctionDescription(this.state.schema as Schema)],
        selectedField: null,
        selectedRecordType: null,
        selectedFunction: functionValue,
      };
      if (isEmptySelect(select)) {
        select.selectedField = obj.selectedField;
        select.selectedRecordType = obj.selectedRecordType;
        select.selectedAggregations = obj.selectedAggregations;
        select.selectedFunction = obj.selectedFunction;
      } else {
        this.props.query.selects.push(obj);
      }
    }

    if (values[0] === PROPERTIES_KEY && select.selectedFunction != null) {
      const newFuncId = values.length !== 3 ? values[2] : values[values.length - 1];
      const declaration = this.state.schema?.functions.find(f => f.id === newFuncId) as FunctionDeclaration;

      select.selectedFunction.returnFields = getReturnFields(
        declaration,
        values,
        select.selectedFunction.returnFields || []
      );
      select.selectedFunction = { ...select.selectedFunction };
    }

    this.rewriteSelects();
    this.requestData();
  };

  changeAggregation = (index: number, aggregation: FunctionValue) => {
    const select = this.props.query.selects[index];
    const objectIndex = select.selectedAggregations.findIndex(v => v.id === aggregation.id);
    select.selectedAggregations.splice(objectIndex, 1, aggregation);

    this.rewriteSelects();
    this.requestData();
  };

  removeAggregation = (item: string, index: number) => {
    const select = this.props.query.selects[index];
    select.selectedAggregations = select.selectedAggregations.filter(v => v.id !== item);
    this.rewriteSelects();
    this.requestData();
  };

  removeField = (index: number) => {
    this.props.query.selects.splice(index, 1);
    if (this.props.query.selects.length === 0) {
      this.props.query.selects.push({ ...EMPTY_SELECT });
    }
    this.rewriteSelects();
    this.requestData();
  };

  removeGroup = (index: number) => {
    this.props.query.selectedGroups.splice(index, 1);
    this.props.onChange({
      ...this.props.query,
      selectedGroups: [...this.props.query.selectedGroups],
    });
    this.requestData();
  };

  onChangeSymbol = (symbol: SelectableValue<string> | null) => {
    this.props.onChange({
      ...this.props.query,
      selectedSymbol: symbol == null ? null : (symbol.value as string),
    });
    this.setState(state => ({
      ...state,
      validSymbolControl: true,
    }));
    this.requestData();
  };

  onChangeField = (value: SelectableValue<string>, index: number) => {
    const [type, field] = separateTypeAndField(value.value as string);
    if (this.props.query.selects == null) {
      this.props.query.selects = [
        {
          selectedFunction: null,
          selectedField: field,
          selectedRecordType: type,
          selectedAggregations: [getDefaultFunctionDescription(this.state.schema as Schema)],
        },
      ];
    } else {
      const select = this.props.query.selects[index];
      const fieldType = getSelectedFieldType(value.value as string, this.state.schema?.types as any);

      const selectedAggregations = [];
      for (const aggregation of select.selectedAggregations) {
        const aggrDesription = (this.state.schema as Schema).functions.find(f => aggregation.id === f.id);
        const types = [];
        for (const f of (aggrDesription as FunctionDeclaration).fields) {
          types.push(...f.types);
        }

        if (types.includes(fieldType?.dataType as any)) {
          selectedAggregations.push(aggregation);
        }
      }

      select.selectedField = field;
      select.selectedRecordType = type;
      select.selectedAggregations = selectedAggregations;
    }

    this.rewriteSelects();
    this.requestData();
  };

  onChangeInterval = (value: SelectableValue<number>) => {
    this.props.onChange({
      ...this.props.query,
      selectedInterval: value?.value as number,
    });
    this.requestData();
  };

  onChangeGroup = (item: SelectableValue<string>, index: number) => {
    this.props.query.selectedGroups[index] = item.value as string;
    this.requestData();
  };

  private requestData = () => {
    if (
      this.props.query.selectedStream == null ||
      this.props.query.selectedSymbol == null ||
      this.props.query.selectedSymbol === ''
    ) {
      return;
    }
    this.props.onRunQuery();
  };

  addFilter = (field: string[]) => {
    if (this.props.query.filters == null) {
      this.props.query.filters = [];
    }
    this.props.query.filters.push({
      field: field[0],
      operator: DEFAULT_OPERATOR,
      values: [],
    });
    this.rewriteFilters();
    this.requestData();
  };

  removeFilter = (index: number) => {
    this.props.query.filters.splice(index, 1);
    this.rewriteFilters();
    this.requestData();
  };

  onChangeFilter = (field: string, operator: string, value: string[], index: number) => {
    const filters = this.props.query.filters[index];
    filters.field = field;
    filters.operator = operator;
    filters.values = value;

    this.rewriteFilters();
    this.requestData();
  };

  onChangeOrientation = (value: any) => {
    this.props.onChange({
      ...this.props.query,
      selectedOption: value == null ? null : (value.value as string),
    });
    this.requestData();
  };

  onChangeRequestType = (value: any) => {
    this.props.onChange({
      ...this.props.query,
      requestType: value == null ? null : (value.value as string),
    });

    this.requestData();
  };

  private rewriteSelects = () => {
    this.props.onChange({
      ...this.props.query,
      selects: [...this.props.query.selects],
    });
  };

  private rewriteFilters = () => {
    this.props.onChange({
      ...this.props.query,
      filters: [...this.props.query.filters],
    });
  };

  private restoreView = () => {
    this.props.datasource.getStreams(this.props.query.selectedStream as string, 0).then(streams => {
      const validStreamControl = streams.list.length !== 0;
      this.setState(state => ({
        ...state,
        validStreamControl,
      }));

      if (!validStreamControl) {
        return;
      }

      const symbolsTemplate =
        this.props.query.selectedSymbol != null && this.props.query.selectedSymbol !== ALL_KEY
          ? this.props.query.selectedSymbol
          : '';
      const symbols$ = this.props.datasource.getSymbols(symbolsTemplate, this.props.query.selectedStream as string, 0);
      const schema$ = this.props.datasource.getStreamSchema(this.props.query.selectedStream as string);
      const usedFields = getUsedFields(this.props.query.filters, this.props.query.selects);

      Promise.all([symbols$, schema$]).then(([symbols, schema]) => {
        const fieldsByType = getListFields(schema?.types);
        const invalidFieldsMap: any = {};
        for (const usedField of usedFields) {
          if (!fieldsByType.some(field => field.value === usedField)) {
            invalidFieldsMap[usedField] = true;
          }
        }
        this.setState(state => ({
          ...state,
          validSymbolControl:
            this.props.query.selectedSymbol == null ||
            this.props.query.selectedSymbol === '' ||
            this.props.query.selectedSymbol === ALL_KEY ||
            (symbols.list.length !== 0 && symbols.list.includes(this.props.query.selectedSymbol as string)),
          schema: schema,
          invalidFieldsMap,
        }));
      });
    });
  };

  onChangeFunctionByIndex = (index: number, streamFunction: FunctionValue) => {
    const select = this.props.query.selects[index];
    select.selectedFunction = { ...streamFunction };
    this.rewriteSelects();
    this.requestData();
  };

  getCurrentInterval = () => {
    return getCurrentInterval(
      this.props.datasource.intervals,
      this.props.query.selectedInterval,
      this.props.datasource.isChangeCurrencyInterval
    );
  };

  render() {
    const filters = this.props.query.filters == null ? [] : this.props.query.filters;
    const selects = this.props.query.selects == null ? [{ ...EMPTY_SELECT }] : this.props.query.selects;
    if (this.props.query.selects === null && this.state.schema != null) {
      selects[0].selectedAggregations = [getDefaultFunctionDescription(this.state.schema)];
    }

    return (
      <div>
        <div className={cx('gf-form-inline width', styles)}>
          <SegmentFrame title="STREAM">
            <Field
              className="invalid-mess"
              invalid={!this.state.validStreamControl}
              error={!this.state.validStreamControl ? `No stream ${this.state.selectedStream?.value}` : ''}
            >
              <div title={this.state.selectedStream?.value as string}>
                <AsyncPaginate
                  className={cx('select', styles)}
                  backspaceRemovesValue={true}
                  isClearable={true}
                  classNamePrefix="grafana-custom"
                  value={this.state.selectedStream}
                  loadOptions={this.loadStreams as any}
                  onChange={this.onChangeStream}
                />
              </div>
            </Field>

            <SegmentFrame
              className={cx('gf-form gf-form-mb', styles)}
              resetTitleWidth={true}
              title="WHERE"
              hideShadow={true}
              options={getListFields(this.state.schema?.types, true)}
              onChange={this.addFilter}
            >
              {filters.length !== 0 ? (
                <SegmentFrame resetTitleWidth={true} hideShadow={true} className={cx('gf-form gf-form-mb', styles)}>
                  <Field
                    className="invalid-mess"
                    invalid={this.state.invalidFieldsMap[filters[0].field] || this.props.datasource.queryError != null}
                    error={getErrorForFields(
                      separateTypeAndField(filters[0].field)[0],
                      separateTypeAndField(filters[0].field)[1],
                      this.state.selectedStream?.value as string,
                      this.state.schema?.types,
                      this.props.datasource.queryError,
                      true
                    )}
                  >
                    <FilterComponent
                      index={0}
                      selectedField={filters[0].field}
                      selectedOperators={filters[0].operator}
                      filterValue={filters[0].values}
                      schema={this.state.schema?.types}
                      onChangeFilter={this.onChangeFilter}
                      removeFilter={this.removeFilter}
                    />
                  </Field>
                </SegmentFrame>
              ) : null}
            </SegmentFrame>
          </SegmentFrame>
        </div>

        {filters.map((filter, index) => {
          if (index === 0) {
            return;
          }
          return (
            <div className={cx('gf-form-inline width gf-form-ml', styles)}>
              <SegmentFrame
                resetTitleWidth={true}
                hideShadow={true}
                className={cx('gf-form gf-form-mb', styles)}
                title="AND"
              >
                <Field
                  className="invalid-mess"
                  invalid={this.state.invalidFieldsMap[filter.field] || this.props.datasource.queryError != null}
                  error={getErrorForFields(
                    separateTypeAndField(filter.field)[0],
                    separateTypeAndField(filter.field)[1],
                    this.state.selectedStream?.value as string,
                    this.state.schema?.types,
                    this.props.datasource.queryError,
                    true
                  )}
                >
                  <FilterComponent
                    index={index}
                    selectedField={filter.field}
                    selectedOperators={filter.operator}
                    filterValue={filter.values}
                    schema={this.state.schema?.types}
                    onChangeFilter={this.onChangeFilter}
                    removeFilter={this.removeFilter}
                  />
                </Field>
              </SegmentFrame>
            </div>
          );
        })}
        <div className={cx('gf-form-inline width', styles)}>
          <div className="gf-form">
            <span className="gf-form-label width-8 query-keyword">SYMBOL</span>
          </div>
          <Field
            className="invalid-mess"
            invalid={!this.state.validSymbolControl}
            error={
              !this.state.validSymbolControl
                ? `No symbols [${this.state.selectedSymbol?.value}] in ${this.state.selectedStream?.value}.`
                : ''
            }
          >
            {this.state.hideControl ? (
              <div></div>
            ) : (
              <div title={this.state.selectedSymbol?.value as string}>
                <AsyncPaginate
                  className={cx('select', styles)}
                  backspaceRemovesValue={true}
                  classNamePrefix="grafana-custom"
                  isClearable={true}
                  value={this.state.selectedSymbol}
                  loadOptions={this.loadSymbols as any}
                  onChange={this.onChangeSymbol}
                />
              </div>
            )}
          </Field>
          <div className="gf-form-label gf-form-label--grow"></div>
        </div>
        <div className={cx('gf-form-inline width', styles)}>
          {selects.map((select, index) => {
            return (
              <SegmentFrame
                index={index}
                title={index === 0 ? 'SELECT' : ''}
                options={this.getSelectOptions(index)}
                onChange={this.addSelectSection}
              >
                {select.selectedField != null && select.selectedRecordType != null ? (
                  <Field
                    className="invalid-mess"
                    invalid={
                      this.state.invalidFieldsMap[getTypeWithField(select.selectedField, select.selectedRecordType)]
                    }
                    error={getErrorForFields(
                      select.selectedRecordType,
                      select.selectedField,
                      this.state.selectedStream?.value as string,
                      this.state.schema?.types,
                      this.props.datasource.queryError
                    )}
                  >
                    <Segment
                      Component={
                        <ColoredFieldLabelComponent
                          fun="field"
                          needFiledFun={true}
                          value={select.selectedField}
                          type={select.selectedRecordType}
                          index={index}
                          onRemove={this.removeField}
                        />
                      }
                      options={getListFields(this.state.schema?.types, true)}
                      onChange={(item: SelectableValue<string>) => this.onChangeField(item, index)}
                    />
                  </Field>
                ) : null}
                {select.selectedFunction != null ? (
                  <FunctionComponent
                    value={select.selectedFunction}
                    index={index}
                    schema={this.state.schema as Schema}
                    onChangeFunction={this.onChangeFunctionByIndex}
                    description={
                      this.state.schema?.functions.find(
                        f => f.id === select.selectedFunction?.id
                      ) as FunctionDeclaration
                    }
                    onRemove={this.removeField}
                  />
                ) : null}
                {select.selectedAggregations.map(aggregation => (
                  <FunctionComponent
                    index={index}
                    disableColoring={true}
                    schema={this.state.schema as Schema}
                    description={this.state.schema?.functions.find(f => f.id === aggregation.id) as FunctionDeclaration}
                    value={aggregation}
                    dependsSelect={select}
                    onRemove={index => this.removeAggregation(aggregation.id, index)}
                    onChangeFunction={this.changeAggregation}
                  />
                ))}
              </SegmentFrame>
            );
          })}
        </div>
        <div className={cx('gf-form-inline width', styles)}>
          <SegmentFrame
            title="GROUP BY"
            options={getListFields(this.state.schema?.types, false, true)}
            onChange={this.addGroup}
          >
            <Segment
              Component={<FieldLabelComponent needFiledFun={true} fun="time" value={this.getCurrentInterval()} />}
              options={this.props.datasource.intervals}
              onChange={this.onChangeInterval}
            />
            {this.props.query.selectedGroups != null
              ? this.props.query.selectedGroups.map((group, index) => {
                  return (
                    <Field
                      className="invalid-mess"
                      invalid={this.state.invalidFieldsMap[group] || this.props.datasource.queryError != null}
                      error={getErrorForFields(
                        separateTypeAndField(group)[0],
                        separateTypeAndField(group)[1],
                        this.state.selectedStream?.value as string,
                        this.state.schema?.types,
                        this.props.datasource.queryError,
                        true
                      )}
                    >
                      <Segment
                        Component={
                          <ColoredFieldLabelComponent
                            fun="field"
                            needFiledFun={true}
                            value={separateTypeAndField(group)[1]}
                            type={separateTypeAndField(group)[0]}
                            index={index}
                            onRemove={this.removeGroup}
                          />
                        }
                        options={getListFields(this.state.schema?.types, false, true)}
                        onChange={(item: SelectableValue<string>) => this.onChangeGroup(item, index)}
                      />
                    </Field>
                  );
                })
              : null}
          </SegmentFrame>
        </div>

        {this.props.query.selectedGroups == null || this.props.query.selectedGroups.length === 0 ? null : (
          <div className={cx('gf-form-inline width', styles)}>
            <SegmentFrame title="OPTION">
              <Select
                backspaceRemovesValue={true}
                isClearable={true}
                className={cx('select', styles)}
                disableDeletion={false}
                classNamePrefix="grafana-custom"
                value={toOption(
                  this.props.query.selectedOption === undefined ? 'COLUMN' : this.props.query.selectedOption
                )}
                options={this.state.groupByViewOptions as any}
                onChange={this.onChangeOrientation}
              ></Select>
            </SegmentFrame>
          </div>
        )}

        <div className={cx('gf-form-inline width', styles)}>
          <SegmentFrame title="VIEW">
            <Select
              backspaceRemovesValue={true}
              isClearable={true}
              disableDeletion={false}
              classNamePrefix="grafana-custom"
              className={cx('select', styles)}
              value={toOption(
                this.props.query.requestType === undefined ? DATAFRAME_KEY : this.props.query.requestType
              )}
              options={REQUEST_TYPE.map(toOption)}
              onChange={this.onChangeRequestType}
            ></Select>
          </SegmentFrame>
        </div>
      </div>
    );
  }
}
