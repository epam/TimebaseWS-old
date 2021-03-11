import { Filter, Select } from '../types';
import { Operator, SpecialValue } from './constants';
import { Field, PropertyType, StreamType } from './types';

const SEPARATOR = ' : ';

export const toOption = (value: any) => ({ label: value, value });
export const toOptionWithCustomLabel = (value: any, label: any) => ({ label, value });

export const separateTypeAndField = (value: string) => value?.split(SEPARATOR);
export const getTypeWithField = (field: string, type: string) => `${type}${SEPARATOR}${field}`;

export const getListFields = (schema: StreamType[] | undefined, isFilter = false, isGroup = false) => {
  if (schema == null) {
    return [];
  }

  const listFields = [];
  for (const obj of schema) {
    const filteredFields = isFilter
      ? obj.fields.filter(
          (field: Field) =>
            field.name !== 'symbol' &&
            (isNumberType(field.fieldType.dataType) ||
              isStringType(field.fieldType.dataType) ||
              isEnumType(field.fieldType.dataType))
        )
      : isGroup
      ? obj.fields.filter(
          (field: Field) => isStringType(field.fieldType.dataType) || isEnumType(field.fieldType.dataType)
        )
      : obj.fields;
    listFields.push(...filteredFields.map((field: Field) => getTypeWithField(field.name, obj.type)));
  }

  return listFields.map(toOption);
};

export const getSelectedFieldType = (selectedField: string, schema: StreamType[] | undefined) => {
  const [type, field] = separateTypeAndField(selectedField);
  const recordType = schema == null ? null : schema.find(s => s.type === type);
  if (recordType != null) {
    const fieldObject = recordType.fields.find(f => f.name === field);
    if (fieldObject != null) {
      return fieldObject.fieldType;
    }
  }
  return;
};

export const isNumberType = (type: PropertyType) => {
  return isIntType(type) || isDecimalNumberType(type);
};

export const isIntType = (type: PropertyType) => {
  return type === PropertyType.INT || type === PropertyType.LONG;
};

export const isDecimalNumberType = (type: PropertyType) => {
  return type === PropertyType.DECIMAL64;
};

export const isStringType = (type: PropertyType) => {
  return type === PropertyType.VARCHAR;
};

export const isEnumType = (type: PropertyType) => {
  return type === PropertyType.ENUM;
};

export const showSpecialValues = (operator: Operator) => {
  return operator === Operator.EQUALS || operator === Operator.NOT_EQUAL;
};

export const isInFilters = (operator: Operator) => {
  return operator === Operator.IN || operator === Operator.NOT_IN;
};

export const getFilterValues = (type: PropertyType, enumValues: string[]) => {
  const values: string[] = [SpecialValue.NULL];
  if (isDecimalNumberType(type)) {
    values.push(SpecialValue.NAN);
    values.push(SpecialValue.MINUS_INF);
    values.push(SpecialValue.PLUS_INF);
  }
  if (isEnumType(type)) {
    values.unshift(...enumValues);
  }

  if (!isEnumType(type)) {
    values.unshift('Value');
  }
  return values.map(toOption);
};

export const getServerFilterType = (type: Operator) => {
  switch (type) {
    case Operator.EQUALS:
      return 'EQUAL';
    case Operator.NOT_EQUAL:
      return 'NOTEQUAL';
    case Operator.GREATER_THAN:
      return 'GREATER';
    case Operator.LESS_THAN:
      return 'LESS';
    case Operator.IN:
      return 'IN';
    case Operator.NOT_IN:
      return 'NOT_IN';
    case Operator.GREATER_THAN_OR_EQUALS:
      return 'NOTLESS';
    case Operator.LESS_THAN_OR_EQUALS:
      return 'NOTGREATER';
    case Operator.STARTS_WITH:
      return 'STARTS_WITH';
    case Operator.ENDS_WITH:
      return 'ENDS_WITH';
    case Operator.CONTAINS:
      return 'CONTAINS';
    case Operator.NOT_CONTAINS:
      return 'NOT_CONTAINS';
    default:
      return 'IN';
  }
};

export const getUsedFields = (filters: Filter[], selects: Select[]) => {
  const usedFields = new Set<string>();
  for (const filter of filters) {
    usedFields.add(filter.field);
  }

  for (const sel of selects) {
    if (sel.selectedField == null || sel.selectedRecordType == null) {
      continue;
    }
    usedFields.add(getTypeWithField(sel.selectedField as string, sel.selectedRecordType as string));
  }
  return Array.from(usedFields);
};

export const getErrorForFields = (
  type: string,
  field: string,
  stream: string,
  schemaFields: StreamType[] | undefined,
  serverError: string | undefined,
  isFilter = false
) => {
  const typeObject = schemaFields == null ? null : schemaFields.find(s => s.type === type);
  if (typeObject == null && type !== '') {
    return `There's no type ${type} in descriptors of ${stream}`;
  }
  const fieldObject = typeObject?.fields.find(o => o.name === field);
  if (fieldObject == null && field !== '') {
    return `There's no field ${field} in descriptors of ${stream}`;
  }
  if (
    isFilter &&
    serverError?.includes(type) &&
    serverError?.includes(field) &&
    serverError?.includes("Couldn't parse value")
  ) {
    return serverError;
  }
  return '';
};
