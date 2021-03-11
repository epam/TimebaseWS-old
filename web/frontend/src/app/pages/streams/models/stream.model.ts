export interface StreamModel {
  key: string;
  name: string;
  symbols: number;

  // all custom properties should starts from "_"
  _symbolsList?: string[];
  _active: boolean;
  _shown: boolean;
}
