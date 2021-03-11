import { ButtonCascader, CascaderOption } from '@grafana/ui';
import { css, cx } from 'emotion';
import React, { PureComponent } from 'react';

import { FIELDS_KEY, FUNCTIONS_KEY } from '../../utils/constants';
import { getId, getNewFunction } from '../../utils/functions';
import { getFunctionTitle, sortOptions } from '../../utils/options';
import {
  FieldArgumentDef,
  FunctionDeclaration,
  FunctionParameter,
  FunctionValue,
  PropertyType,
  Schema,
} from '../../utils/types';
import { getTypeWithField, separateTypeAndField, toOption, toOptionWithCustomLabel } from '../../utils/utils';
import { FunctionComponent } from './Function';
import { FunctionHeaderComponent } from './FunctionHeader';

const styles = css`
  &.blue {
    color: #61aef3;
  }
  &.green {
    color: #7bc363;
  }
  &.btn-cascader {
    padding: 0;
    margin: 0;
    svg {
      display: none;
    }
  }
  .place-holder {
    color: #8e8e8e;
  }
  .reset-offset {
    padding: 0;
    margin: 0;
  }
`;

styles;
interface FunctionFieldParameterProps {
  field: FieldArgumentDef;
  schema: Schema | undefined;
  index: number;
  description: FunctionDeclaration;
  parameterValue: FunctionParameter;
  onChangeSelectParametersValue: (parameterName: string, value: string | undefined | null | FunctionValue) => any;
  emitValue: (streamFunction: FunctionValue) => any;
}

export class FunctionFieldParameterComponent extends PureComponent<FunctionFieldParameterProps> {
  getComponent = () => {
    if (this.props.parameterValue == null || this.props.parameterValue.value == null) {
      return null;
    }
    const value = this.props.parameterValue.value;
    if (typeof value === 'string' || typeof value === 'number') {
      return null;
    }

    return (
      <FunctionComponent
        index={0}
        className={cx('reset-offset', styles)}
        schema={this.props.schema as Schema}
        description={this.props.schema?.functions.find(f => f.id === value.id) as FunctionDeclaration}
        value={value}
        onRemove={null}
        hideName={true}
        onChangeFunction={(index: number, value: FunctionValue) => {
          this.props.onChangeSelectParametersValue(this.props.parameterValue.name, value);
        }}
      />
    );
  };

  getHeader = () => {
    if (this.props.parameterValue == null || this.props.parameterValue.value == null) {
      return `select...`;
    }
    const value = this.props.parameterValue.value;
    if (typeof value === 'string' || typeof value === 'number') {
      return (
        <div className="gf-form-inline">
          <div className={cx('green', styles)}>
            {separateTypeAndField(this.props.parameterValue.value.toString())[0]}
          </div>
          &nbsp;:&nbsp;
          <div className={cx('blue', styles)}>
            {separateTypeAndField(this.props.parameterValue.value.toString())[1]}
          </div>
        </div>
      );
    }

    return (
      <FunctionHeaderComponent
        className={cx('reset-offset', styles)}
        schema={this.props.schema as Schema}
        description={this.props.schema?.functions.find(f => f.id === value.id) as FunctionDeclaration}
        value={value}
        onRemove={null}
        emitValue={this.props.emitValue}
      />
    );
  };

  onChangeButtonCascaderValue = (values: string[]) => {
    if (values[0] === FIELDS_KEY) {
      this.props.onChangeSelectParametersValue(this.props.field.name, values[values.length - 1]);
    } else {
      const id = getId(values);
      const newFunction = (this.props.schema as Schema).functions.find(f => f.id === id) as FunctionDeclaration;
      this.props.onChangeSelectParametersValue(this.props.field.name, getNewFunction(newFunction, values));
    }
  };

  render() {
    const placeHolderClass =
      this.props.parameterValue == null || this.props.parameterValue.value == null ? 'place-holder' : '';
    return (
      <div title={this.props.parameterValue?.returnTypes.join(' | ')} className={cx('gf-form-inline', styles)}>
        {this.props.parameterValue?.name}:&nbsp;
        <ButtonCascader
          className={cx(`gf-form-inline btn-cascader ${placeHolderClass}`, styles)}
          options={getFunctionParameterOptions(this.props.field, this.props.schema)}
          value={undefined}
          onChange={this.onChangeButtonCascaderValue}
        >
          {this.getHeader() as any}
        </ButtonCascader>
        {this.getComponent() as any}
        {this.props.index !== this.props.description.fields.length - 1 ||
        this.props.description.constants.length !== 0 ? (
          <div>,&nbsp;</div>
        ) : null}
      </div>
    );
  }
}

const getFunctionParameterOptions = (field: FieldArgumentDef, schema: Schema | undefined) => {
  return [
    { label: FIELDS_KEY, value: FIELDS_KEY, children: getListFieldsByTypes(field.types, schema) },
    { label: FUNCTIONS_KEY, value: FUNCTIONS_KEY, children: getListFunctionsByTypes(field.types, schema) },
  ] as CascaderOption[];
};

const getListFieldsByTypes = (types: PropertyType[], schema: Schema | undefined) => {
  if (schema == null) {
    return [];
  }
  const filteredValue = [];
  for (const streamType of schema.types) {
    const result = streamType.fields.filter(f => types.includes(f.fieldType.dataType));
    filteredValue.push(...result.map(field => getTypeWithField(field.name, streamType.type)));
  }
  return filteredValue.map(toOption);
};

const getListFunctionsByTypes = (types: PropertyType[], schema: Schema | undefined) => {
  if (schema == null) {
    return [];
  }
  const options: CascaderOption[] = [];
  const groups = new Set<string>();

  for (const func of schema.functions) {
    if (func.isAggregation && func.fields.length === 1) {
      continue;
    }
    groups.add(func.group);
  }

  groups.forEach(group => {
    const filteredValue = schema.functions.filter(f => {
      const functionTypes = f.returnFields.map(r => r.type);
      return f.group === group && !f.isAggregation && types.some(type => functionTypes.includes(type));
    });

    const values: CascaderOption[] = [];
    const names = new Set<string>(filteredValue.map(f => f.name));
    names.forEach(name => {
      const functions = filteredValue.filter(f => f.name === name);
      if (functions.length === 1) {
        const obj: CascaderOption = toOptionWithCustomLabel(functions[0].id, functions[0].name);
        if (functions[0].returnFields != null && functions[0].returnFields.length > 1) {
          const children = functions[0].returnFields.map(v => v.constantName);
          obj.children = children.map(toOption);
          obj.children.sort(sortOptions);
        } else {
          obj.title = functions[0].doc !== '' ? functions[0].doc : undefined;
        }
        values.push(obj);
      } else {
        const descriptionsFun: CascaderOption[] = [];
        for (const func of functions) {
          const obj: CascaderOption = toOptionWithCustomLabel(func.id, getFunctionTitle(func));
          obj.title = func.doc !== '' ? func.doc : undefined;
          descriptionsFun.push(obj);
        }
        values.push({
          label: name,
          value: name,
          children: descriptionsFun,
        });
      }
    });

    values.sort(sortOptions);

    options.push({
      label: group,
      value: group,
      children: values,
    });
    options.sort(sortOptions);
  });
  return options;
};
