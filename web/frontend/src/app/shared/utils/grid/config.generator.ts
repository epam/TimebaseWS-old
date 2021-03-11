import { SchemaTypeModel } from '../../models/schema.type.model';
import { formatHDate } from '../../locale.timezone';

export function generateGridConfig(rawData: SchemaTypeModel[], parentKey = '', hide: boolean, filter_date_format?: string[], filter_time_format?: string[], ) {
  const currencies = JSON.parse(localStorage.getItem('currencies'));
  // console.log(currencies);
  return rawData.map(SchemaTypeModel => {
    const column = {
      headerName: SchemaTypeModel.title || SchemaTypeModel.name,
      field: parentKey + SchemaTypeModel.name.replace(/\./g, '-'),
      filter: false,
      sortable: false,
      //  width: 130,
      resizable: true,
      headerTooltip: SchemaTypeModel.title || SchemaTypeModel.name,
      //  hide: hide || SchemaTypeModel.hide, window.location.protocol === 'https:' ? 'wss' : 'ws'
      hide: hide ? hide : SchemaTypeModel.hide, // https://gitlab.deltixhub.com/Deltix/QuantServer/TimebaseWS/issues/106
      valueFormatter: (params) => {
        if ((SchemaTypeModel.name === 'currencyCode' || SchemaTypeModel.name === 'baseCurrency') && params.value && currencies.length) {
          const currency = currencies.find(item => item.numericCode === params.value);
          if (currency && currency.alphabeticCode) {
            return params.value + ' (' + currency.alphabeticCode + ')';
          } else {
            return params.value;
          }
        }
        if (typeof params.value === 'number') {
          if (String(params.value).indexOf('e') !== -1) {
            const exponent = parseInt(String(params.value).split('-')[1], 10);
            return params.value.toFixed(exponent);
          }
        }
        if (SchemaTypeModel.type === 'TIMESTAMP' && params.value) {
          return formatHDate(params.value, filter_date_format, filter_time_format);
        }
        if (params.value && typeof params.value === 'object') return JSON.stringify(params.value);
        return params.value;
      },
    };

    if (SchemaTypeModel.fields) {
      column['children'] = generateGridConfig(SchemaTypeModel.fields, column.field + '.', hide, filter_date_format, filter_time_format);
      column['marryChildren'] = true;
      //   column['hide'] = String(hide) && String(hide).length ? hide : SchemaTypeModel.hide;
    }
    return column;
  });
}
