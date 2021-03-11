import { FilterModel }                      from './filter.model';
import { streamRouteName, symbolRouteName } from '../../../shared/utils/routes.names';

export class TabModel {
  public stream: string;
  public symbol?: string;
  public id?: string;

  public name?: string;

  public active?: boolean;
  public live?: boolean;
  public reverse?: boolean;
  public view?: boolean;
  public schema?: boolean;
  public chart?: boolean;
  public query?: boolean;

  public filter: FilterModel = {};

  constructor(obj: {} | TabModel) {
    // Object.assign(this, obj);
    if (obj['stream']) {
      this.stream = obj['stream'];
    }
    if (obj['symbol']) {
      this.symbol = obj['symbol'];
    }
    if (obj['id']) {
      this.id = obj['id'];
    }
    if (obj['name']) {
      this.name = obj['name'];
    }
    if (obj['active']) {
      this.active = obj['active'];
    }
    if (obj['live']) {
      this.live = obj['live'];
    }
    if (obj['reverse']) {
      this.reverse = obj['reverse'];
    }
    if (obj['view']) {
      this.view = obj['view'];
    }
    if (obj['schema']) {
      this.schema = obj['schema'];
    }
    if (obj['chart']) {
      this.chart = obj['chart'];
    }
    if (obj['query']) {
      this.query = obj['query'];
    }
    if (obj['filter']) {
      this.filter = obj['filter'];
    }
  }

  public get title(): string {
    let title = this.name || this.stream;
    if (this.symbol) title += ' / ' + this.symbol;
    return title;
  }

  public get type(): string {
    switch (true) {
      case this.live:
        return 'live';
      case this.reverse:
        return 'reverse';
      case this.schema:
        return 'schema';
      case this.chart:
        return 'chart';
      case this.query:
        return 'query';
      case this.view:
      default:
        return 'view';
    }
  }

  public get linkArray(): string[] {
    const link_array = [this.symbol ? symbolRouteName : streamRouteName];
    switch (true) {
      case this.live:
        link_array.push('live');
        break;
      case this.reverse:
        link_array.push('reverse');
        break;
      case this.view:
        link_array.push('view');
        break;
      case this.schema:
        link_array.push('schema');
        break;
      case this.chart:
        link_array.push('chart');
        break;
      case this.query:
        link_array.push('query');
        break;
    }

    if (this.stream) link_array.push(this.stream);
    if (this.symbol) link_array.push(this.symbol);
    if (this.id + '') link_array.push(this.id + '');
    return link_array;
  }
}

