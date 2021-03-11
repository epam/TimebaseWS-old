export interface ChartModel {
  effectiveWindow: {
    end: string;
    start: string;
  };
  lines: {[key: string]: ChartRawLine};
  name: string;
}

export interface ChartRawLine {
  aggregationSizeMs: number;
  newWindowSizeMs: number;
  points: {
    time: number;
    value: string;
  }[];
}

