import { SelectableValue } from '@grafana/data';
import { Button, Input, MultiSelect, Segment, Select } from '@grafana/ui';
import { css, cx } from 'emotion';
import React, { PureComponent } from 'react';

import { ENUM_OPERATORS, NUMBER_OPERATORS, Operator, STRING_OPERATORS } from '../../utils/constants';
import { PropertyType, StreamType } from '../../utils/types';
import {
  getFilterValues,
  getSelectedFieldType,
  separateTypeAndField,
  isDecimalNumberType,
  isEnumType,
  isIntType,
  isNumberType,
  showSpecialValues,
  toOption,
  isInFilters,
} from '../../utils/utils';
import { ColoredFieldLabelComponent, FieldLabelComponent } from '../FieldLabels/FieldLabels';

const REMOVE = 'Remove';
const styles = css`
  &.gf-form-mr {
    margin-right: 4px;
    width: inherit;
  }

  &.btn-value {
    width: 100%;
    display: flex;
    justify-content: center;
  }
  &.width {
    width: 100%;
  }
`;

styles;
interface FilterProps {
  index: number;
  selectedField: string;
  selectedOperators: string;
  schema: StreamType[] | undefined;
  filterValue: string[];
  onChangeFilter: (field: string, operator: string, value: string[], index: number) => any;
  removeFilter: (index: number) => any;
}

interface FilterState {
  fields: Array<SelectableValue<string>>;
  operators: Array<SelectableValue<string>>;
  values: Array<SelectableValue<string>>;
  selectedItem: SelectableValue<string> | undefined;
  filterValue: string[];
  selectedFieldType: PropertyType | undefined;
}

export class FilterComponent extends PureComponent<FilterProps, FilterState> {
  state: FilterState = {
    fields: [],
    operators: [],
    values: [],
    selectedItem: void 0,
    filterValue: [],
    selectedFieldType: void 0,
  };

  private customValues: Array<SelectableValue<string>> = [];
  static getDerivedStateFromProps(nextProps: FilterProps, nextState: FilterState) {
    const selectedFieldType = getSelectedFieldType(nextProps.selectedField, nextProps.schema);
    const type = selectedFieldType?.dataType as PropertyType;
    const isEnum = isEnumType(type);
    const enumValues = isEnum ? selectedFieldType?.values : [];
    const values =
      showSpecialValues(nextProps.selectedOperators as Operator) || isInFilters(nextProps.selectedOperators as Operator)
        ? getFilterValues(type, enumValues as string[])
        : [];
    const selectedItem =
      (isEnum && nextProps.filterValue.length !== 0 && enumValues?.includes(nextProps.filterValue[0])) ||
      (nextProps.filterValue.length !== 0 && values.find(v => v.value === nextProps.filterValue[0]))
        ? toOption(nextProps.filterValue[0])
        : nextState.selectedItem == null
        ? values[0]
        : nextState.selectedItem;

    return {
      fields: [nextProps.selectedField, REMOVE].map(toOption),
      operators: isNumberType(type)
        ? NUMBER_OPERATORS.map(toOption)
        : isEnum
        ? ENUM_OPERATORS.map(toOption)
        : STRING_OPERATORS.map(toOption),
      selectedFieldType: type,
      values,
      selectedItem,
      filterValue: nextProps.filterValue,
    };
  }

  onChangeOperator = (value: SelectableValue<string>) => {
    if (
      isInFilters(this.props.selectedOperators as Operator) ||
      showSpecialValues(this.props.selectedOperators as Operator)
    ) {
      this.setState(state => ({
        ...state,
        filterValue: [],
        selectedItem: toOption('Value'),
      }));
      this.emitValue(this.props.selectedField, value?.value as string, []);
    } else {
      this.setState(state => ({
        ...state,
        filterValue: this.getFilterValue(),
      }));

      this.emitValue(this.props.selectedField, value?.value as string, this.getFilterValue());
    }
  };

  onChangeField = (value: SelectableValue<string>) => {
    if (value.value === REMOVE) {
      this.props.removeFilter(this.props.index);
      return;
    }
    this.emitValue(value?.value as string, this.props.selectedOperators, this.getFilterValue());
  };

  onChangeValue = (value: SelectableValue<string>) => {
    if (value == null) {
      this.setState(state => ({
        ...state,
        selectedItem: toOption('Value'),
        filterValue: [],
      }));
      this.emitValue(this.props.selectedField, this.props.selectedOperators, []);
      return;
    }
    const values =
      value?.value !== 'Value'
        ? [value?.value as string]
        : this.state.selectedItem?.value !== 'Value'
        ? []
        : this.state.filterValue;
    this.setState(state => ({
      ...state,
      selectedItem: value,
    }));

    this.emitValue(this.props.selectedField, this.props.selectedOperators, values);
  };

  onKeyPress = (e: any) => {
    if (
      e.key === 'Backspace' ||
      e.key === 'Delete' ||
      (e.key === '-' && (e.target.value == null || (!e.target.value.includes('-') && e.target.selectionStart === 0)))
    ) {
      return;
    }
    if (isIntType(this.state.selectedFieldType as PropertyType) && isNaN(e.key)) {
      e.preventDefault();
      return;
    } else if (
      isDecimalNumberType(this.state.selectedFieldType as PropertyType) &&
      isNaN(e.key) &&
      (e.key !== '.' || (e.key === '.' && e.target.value.includes('.')))
    ) {
      e.preventDefault();
      return;
    }

    e.target.focus();
  };

  onChangeInputValue = (event: any) => {
    const va = event.target.value as string;

    this.setState(state => ({
      ...state,
      filterValue: [va],
    }));
    this.emitValue(this.props.selectedField, this.props.selectedOperators, [event.target.value as string]);
  };

  onChangeMultiSelect = (items: Array<SelectableValue<string>>) => {
    this.setState(state => ({
      ...state,
      filterValue: items.map(item => item.value) as string[],
    }));

    this.emitValue(this.props.selectedField, this.props.selectedOperators, items.map(item => item.value) as string[]);
  };

  onCreateOption = (v: string) => {
    this.customValues.push(toOption(v));
    this.state.filterValue.push(v);
    this.setState(state => ({ ...state }));
    this.emitValue(this.props.selectedField, this.props.selectedOperators, this.state.filterValue);
  };

  filter = (v: SelectableValue<string>, searchQuery: string) => {
    if (v.label?.includes('Create')) {
      return true;
    }
    if (searchQuery != null && searchQuery !== '') {
      return false;
    }
    return this.state.values.find(value => value.value === v.value) != null;
  };

  getContent = () => {
    if (isInFilters(this.props.selectedOperators as Operator)) {
      const values = this.state.values.filter(v => v.value !== 'Value');
      values.push(...this.customValues);

      return (
        <MultiSelect
          options={values}
          className={cx('gf-form-mr width-22', styles)}
          value={this.state.filterValue.map(toOption)}
          allowCustomValue
          onKeyDown={this.onKeyPress}
          filterOption={this.filter}
          getOptionLabel={v => v?.label as string}
          onChange={this.onChangeMultiSelect}
          onCreateOption={this.onCreateOption}
        />
      );
    }

    return (
      <Input
        width={15}
        value={this.state.filterValue[0]}
        onKeyDown={this.onKeyPress}
        onChange={this.onChangeInputValue}
      />
    );
  };

  private emitValue = (selectedField: string, selectedOperators: string, value: string[]) => {
    this.props.onChangeFilter(selectedField, selectedOperators, value, this.props.index);
  };

  private getFilterValue = () => {
    if (this.state.selectedItem?.value !== 'Value') {
      return [this.state.selectedItem?.value as string];
    }
    return this.state.filterValue;
  };

  private getContentForItems = (otherProps: any) => {
    return otherProps.value?.value === 'Value' ? (
      <Input
        {...otherProps}
        value={this.state.filterValue[0] == null ? '' : this.state.filterValue[0]}
        className={cx('width', styles)}
        onKeyDown={this.onKeyPress}
        onChange={this.onChangeInputValue}
      />
    ) : (
      <Button {...otherProps} variant="secondary" className={cx('btn-value', styles)}>
        {otherProps.value?.value}
      </Button>
    );
  };

  render() {
    const [type, field] = separateTypeAndField(this.props.selectedField);
    return (
      <div className="gf-form-inline">
        <Segment
          Component={<ColoredFieldLabelComponent value={field} type={type} needFiledFun={false} fun={''} />}
          options={this.state.fields}
          className="width-9"
          onChange={this.onChangeField}
        />
        <Segment
          Component={<FieldLabelComponent value={this.props.selectedOperators} />}
          options={this.state.operators}
          className="width-3"
          onChange={this.onChangeOperator}
        />
        {showSpecialValues(this.props.selectedOperators as Operator) ? (
          <Select
            width={15}
            className={cx('gf-form-mr', styles)}
            options={this.state.values}
            value={this.state.selectedItem}
            onChange={this.onChangeValue}
            backspaceRemovesValue={true}
            isClearable={true}
            renderControl={this.getContentForItems}
          />
        ) : (
          this.getContent()
        )}
      </div>
    );
  }
}
