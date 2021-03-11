export interface SchemaTypeModel {
  name: string;
  title: string;

  type?: string;
  nullable?: boolean;
  fields?: SchemaTypeModel[];
  hide?: boolean;
}

export interface SchemaAllTypeModel {
  isEnum: boolean;
  name: string;
  parent: string;
  title: string;
  fields: SchemaTypeModel[];
}
