import { HttpClient }          from '@angular/common/http';
import { Injectable }          from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map, takeUntil }                      from 'rxjs/operators';
import { SchemaAllTypeModel, SchemaTypeModel } from '../../../shared/models/schema.type.model';

export interface SimpleColumnModel {
  field: string;
  headerName: string;
  headerTooltip: string;
  children?: SimpleColumnModel[];
  required: boolean;
  dataType?: string;
  controlType?: string;
  controlCollection?: {
    key: string,
    title: string,
  }[];
  changeEvent?: (any) => void;
  rendered?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class SchemaDataService {

  private $destroy = new Subject();

  constructor(
    private httpClient: HttpClient,
  ) { }

  public getSchema(streamId: string): Observable<[SchemaAllTypeModel[], SimpleColumnModel[]]> {
    return this.httpClient
      .get<{
        types: SchemaTypeModel[];
        all: SchemaAllTypeModel[];
      }>(`/${encodeURIComponent(streamId)}/schema`)
      .pipe(
        takeUntil(this.$destroy),
        map((resp) => {
          let schemaTypes: SchemaTypeModel[] = [];
          let schemaAll: SchemaAllTypeModel[] = [];

          if (resp) {
            if (resp['types']) {
              schemaTypes = resp['types'];
            }
            if (resp['all']) {
              schemaAll = resp['all'];
            }
          }
          return [schemaAll, this.getGridConfig(schemaTypes)];
        }),
      );
  }

  public getGridConfig(schema: SchemaTypeModel[]) {
    return this.generateConfig(schema, '');
  }

  // public getSimplifiedConfig(gridConfig: []): SimpleColumnModel[] {
  //   return gridConfig.map(columnConfig => {
  //     const SIMPLE_COLUMN: SimpleColumnModel = {
  //       field: columnConfig['field'],
  //       headerName: columnConfig['headerName'],
  //       headerTooltip: columnConfig['headerTooltip'],
  //     };
  //     if (columnConfig['children']) {
  //       SIMPLE_COLUMN['children'] = this.getSimplifiedConfig(columnConfig['children']);
  //     }
  //     return SIMPLE_COLUMN;
  //   });
  // }

  public generateConfig(rawData: SchemaTypeModel[], parentKey = ''): SimpleColumnModel[] {
    if (!rawData) {
      return [];
    }
    return rawData.map(SchemaTypeModel => {
      const column: SimpleColumnModel = {
        headerName: SchemaTypeModel.title || SchemaTypeModel.name,
        field: /*parentKey + */SchemaTypeModel.name.replace(/\./g, '-'),
        headerTooltip: SchemaTypeModel.title || SchemaTypeModel.name,
        required: typeof SchemaTypeModel.nullable === 'boolean' ? !SchemaTypeModel.nullable : false,
        dataType: SchemaTypeModel.type,
      };
      if (column.dataType === 'BOOLEAN') {
        column.controlType = 'checkbox';
      }
      if (column.dataType === 'TIMESTAMP') {
        column.controlType = 'dateTime';
      }
      if (SchemaTypeModel.fields) {
        column['children'] = this.generateConfig(SchemaTypeModel.fields, column.field + '.');
      }
      return column;
    });
  }

  public getSymbols(streamKey) {
    return this.httpClient
      .get<string[]>(`/${encodeURIComponent(streamKey)}/symbols`);
  }

  public destroy() {
    this.$destroy.next(true);
    this.$destroy.complete();
    this.$destroy = new Subject();
  }

}
