import { Field, SegmentInput } from '@grafana/ui';
import { css, cx } from 'emotion';
import React, { PureComponent } from 'react';

import { Select } from '../../types';
import { FunctionDeclaration, FunctionValue, FunctionValueBase, PropertyType, Schema } from '../../utils/types';
import { LabelWithAliasComponent } from '../LabelWithAlias/LabelWithAlias';
import { SegmentFrame } from '../SegmentFrame/SegmentFrame';
import { FunctionFieldParameterComponent } from './FunctionFieldParameter';
import { FunctionHeaderComponent } from './FunctionHeader';

const styles = css`
  height: 32px;
  background-color: #202226;
  &.blue {
    color: #61aef3;
  }
  &.gf-form-label-mr {
    margin-right: 0 !important;
  }
  &.h-32 {
    height: 32px;
  }
  &.wrapper {
    .gf-form {
      margin: 0;
    }
  }
  &.reset-offset {
    padding: 0;
    margin: 0;
    justify-content: end;
  }
  &.gf-form-segment-input {
    padding-left: 1px;
    background-color: inherit;
    margin-right: 0;
    padding-right: 0;
  }
  &.gf-form-p {
    padding-left: 4px;
    padding-right: 4px;
  }
  &.z-index {
    div {
      z-index: 1000;
    }
  }
`;

styles;
interface FunctionProps {
  index: number;
  value: FunctionValue;
  description: FunctionDeclaration;
  schema: Schema;
  dependsSelect?: Select | null;
  disableColoring?: boolean;
  hideName?: boolean;
  className?: string;
  onRemove: null | ((index: number) => any);
  onChangeFunction: (index: number, streamFunction: FunctionValue) => any;
}

export class FunctionComponent extends PureComponent<FunctionProps> {
  state = { showAlias: false };

  static getDerivedStateFromProps(nextProps: FunctionProps) {
    return { showAlias: nextProps.value.as != null };
  }

  onChangeSelectParametersValue = (parameterName: string, value: string | undefined | null | FunctionValue) => {
    if (this.props.description == null) {
      return;
    }

    const field = this.props.description.fields.find(f => f.name === parameterName);
    const constant = this.props.description.constants.find(c => c.name === parameterName);
    let parameter = this.props.value.parameters.find(p => p.name === parameterName);

    if (parameter != null) {
      parameter.value = value !== '' ? (value as any) : null;
      if (constant != null) {
        const numberValue = Number(value);
        const max = constant.max == null ? null : Number(constant.max);
        const min = constant.min == null ? null : Number(constant.min);
        parameter.invalid =
          !isNaN(numberValue) && ((min != null && numberValue < min) || (max != null && numberValue > max));
      }

      parameter = { ...parameter };
    } else {
      parameter = {
        name: field?.name || constant?.name || '',
        returnTypes: (field?.types || [constant?.type] || []) as PropertyType[],
        value: value !== '' ? (value as any) : null,
        isConstant: constant != null,
      };

      this.props.value.parameters.push(parameter);
    }
    this.emitValue({ ...this.props.value });
  };

  getContent = () => {
    if (this.props.description == null) {
      return;
    }
    const fieldsHtml = this.props.description.fields.map((field, index) => {
      const parameterValue = this.props.value.parameters.find(parameter => parameter.name === field.name);
      if (parameterValue == null) {
        return;
      }
      return (
        <FunctionFieldParameterComponent
          description={this.props.description}
          index={index}
          field={field}
          schema={this.props.schema}
          parameterValue={{ ...parameterValue }}
          onChangeSelectParametersValue={this.onChangeSelectParametersValue}
          emitValue={this.emitValue}
        />
      );
    });

    const constHtml = this.props.description.constants.map((constants, index) => {
      const parameterValue = this.props.value.parameters.find(parameter => parameter.name === constants.name);
      const errorText = getErrorText(parameterValue?.value as number, constants.min, constants.max);

      return (
        <div className={cx('gf-form-inline wrapper', styles)}>
          <SegmentFrame className="gf-form-inline" hideShadow={true}>
            <Field className={cx('z-index', styles)} invalid={errorText != null} error={errorText}>
              <div title={constants.type} className={cx('gf-form-label reset-offset', styles)}>
                {constants.name}:&nbsp;
                <SegmentInput
                  className={cx('gf-form-segment-input', styles)}
                  placeholder={`${constants.type}: ${constants.name}`}
                  value={parameterValue?.value as number}
                  allowCustomValue
                  onChange={(text: React.ReactText) =>
                    this.onChangeSelectParametersValue(constants.name, text?.toString())
                  }
                />
              </div>
            </Field>
          </SegmentFrame>
          {index !== this.props.description.constants.length - 1 ? <div>,&nbsp;</div> : null}
        </div>
      );
    });

    return (
      <>
        {fieldsHtml}
        {constHtml}
      </>
    );
  };

  removeFunctionReturnField = (item: string) => {
    const values = (this.props.value.returnFields as FunctionValueBase[]).filter(v => v.name !== item);
    if (values.length === 0) {
      if (this.props.onRemove != null) {
        this.props.onRemove(this.props.index);
      }
    } else {
      this.props.value.returnFields = [...values];
      this.emitValue({ ...this.props.value });
    }
  };

  changeFunctionValueAlias = (index: number, aggregation: string, alias: string | null) => {
    const val = (this.props.value.returnFields as FunctionValueBase[])[index];
    if (val != null) {
      val.as = alias;
    }
    this.emitValue({ ...this.props.value });
  };

  changeFunctionAlias = (alias: React.ReactText) => {
    this.emitValue({ ...this.props.value, as: alias.toString() });
  };

  emitValue = (functionValue: FunctionValue) => {
    this.props.onChangeFunction(this.props.index, functionValue);
  };

  render() {
    return (
      <div className={`gf-form-label ${this.props.className != null ? this.props.className : ''}`}>
        <FunctionHeaderComponent
          className={this.props.className}
          hideName={this.props.hideName}
          description={this.props.description}
          value={this.props.value}
          schema={this.props.schema}
          dependsSelect={this.props.dependsSelect}
          disableColoring={this.props.disableColoring}
          onRemove={() => {
            if (this.props.onRemove != null) {
              this.props.onRemove(this.props.index);
            }
          }}
          emitValue={this.emitValue}
        />
        <div>(</div>
        {this.getContent()}
        <div>)</div>
        <ReturnFieldComponent
          value={this.props.value}
          removeFunctionReturnField={this.removeFunctionReturnField}
          changeFunctionValueAlias={this.changeFunctionValueAlias}
        />

        {this.state.showAlias ? (
          <div className={cx('gf-form-inline h-32', styles)}>
            <div className={cx('gf-form-label gf-form-label-mr gf-form-p', styles)}> as </div>
            <SegmentFrame className="gf-form-inline" hideShadow={true}>
              <SegmentInput
                className={cx('gf-form-segment-input', styles)}
                value={this.props.value.as as string}
                onChange={this.changeFunctionAlias}
              />
            </SegmentFrame>
          </div>
        ) : null}
      </div>
    );
  }
}

const getErrorText = (value: number, min?: string | undefined, max?: string | undefined) => {
  if (value != null && isNaN(value)) {
    return 'Invalid value';
  }

  if (value != null && min != null && Number(min) > value) {
    return `Min value ${min}`;
  }

  if (value != null && max != null && Number(max) < value) {
    return `Max value ${max}`;
  }
  return;
};

export const ReturnFieldComponent = ({ value, removeFunctionReturnField, changeFunctionValueAlias }: any) =>
  value.returnFields != null
    ? value.returnFields.map((value: any, index: number) => {
        return (
          <LabelWithAliasComponent
            index={index}
            label={value.name}
            alias={value.as}
            doc={''}
            className={cx('blue', styles)}
            additionalText="property"
            onRemove={removeFunctionReturnField}
            onChangeAlias={changeFunctionValueAlias}
          />
        );
      })
    : null;
