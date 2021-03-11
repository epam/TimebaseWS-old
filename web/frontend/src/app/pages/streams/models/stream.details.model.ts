export class StreamDetailsModel {
  public symbol: string;
  public timestamp: string;
  public type?: string;
  public $type: string;

  constructor(obj) {
    if (obj && obj.$type) {
      this[obj.$type.replace(/\./g, '-')] = {
        ...obj,
      };
    }
    this.symbol = obj.symbol;
    this.timestamp = obj.timestamp;
    this.$type = obj.$type;
  }
}
