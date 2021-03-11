/// <reference lib="webworker" />

import { ChartModel, ChartRawLine } from '../models/chart.model';

addEventListener('message', ({data}) => {
  let [chart_rawData, chart_raw_newData, viewportDiff_changed, current_viewport, current_data_windowSize, streamRange, tailsMultiplier] = data;
  if (viewportDiff_changed) {
    chart_rawData = chart_raw_newData;
  } else if (chart_rawData &&
    chart_rawData.lines &&
    Object.keys(chart_rawData.lines).length) {
    for (const i in chart_rawData.lines) {
      if (chart_rawData.lines.hasOwnProperty(i)) {
        const new_line = (chart_raw_newData && chart_raw_newData.lines && typeof chart_raw_newData.lines[i] === 'object') ? chart_raw_newData.lines[i] : {points: []};
        const merged_points = [];

        for (const point of [...chart_rawData.lines[i].points, ...new_line.points]) {
          if (!merged_points.find(line_point => !!(line_point.time === point.time && line_point.value === point.value))) {
            merged_points.push(point);
          }
        }

        chart_rawData.lines[i] = {
          ...chart_rawData.lines[i],
          ...new_line,
          points: merged_points.sort((point_a, point_b) => point_a.time - point_b.time),
        };
      }
    }

    let newLines;

    [newLines, current_data_windowSize] = clearChartData(chart_rawData.lines, current_viewport, streamRange, tailsMultiplier);

    chart_rawData = {
      ...chart_rawData,
      ...chart_raw_newData,
      lines: newLines,
    };

  }

  const [requestViewPort, chart_data, nextRequestViewportDiff] = getChartData([chart_rawData]);

  postMessage([chart_data, chart_rawData, requestViewPort, nextRequestViewportDiff, current_data_windowSize, current_viewport, streamRange, tailsMultiplier]);
});

function getChartData(resp: ChartModel[], update?: boolean): [
  {
    end: string;
    start: string;
  },
  any[],
  number
] {
  const data = [];
  data.push({
    type: 'scatter',
    showInLegend: true,
    legendText: '',
    color: 'rgba(0,0,0,0)',
    dataPoints: [{
      x: 0,
      y: 0,
    }],
  });
  const chart = resp[0],
    maxLevels = getMaxLevels(chart.lines);
  let next_newWindowSizeMs: number;
  for (const lineKey in chart.lines) {
    if (chart.lines.hasOwnProperty(lineKey)) {
      const dataPoints = [],
        line = chart.lines[lineKey],
        pointsLength = line.points.length,
        points = line.points;
      if (!next_newWindowSizeMs || (next_newWindowSizeMs >= line.newWindowSizeMs)) {
        next_newWindowSizeMs = line.newWindowSizeMs;
      }
      points.forEach((point, index) => {
        index++;
        const y = parseFloat(point.value),
          newPoints = [{
            x: point.time,
            y: y,
          }];
        if (lineKey !== 'TRADES' && index < pointsLength && points[index]) {
          newPoints[1] = {
            x: points[index].time,
            y: y,
          };
          newPoints[2] = {
            x: newPoints[1].x,
            y: null,
          };
          dataPoints.push(...newPoints);
        } else if (lineKey === 'TRADES') {
          dataPoints.push(...newPoints);
        }
      });

      data.push(lineKey === 'TRADES' ? {
        type: 'scatter',
        dataPoints: dataPoints,
        markerType: 'cross',
        color: '#ffc70b', // #ffc70b
        markerSize: 10,
        _lineKey: lineKey,
      } : {
        type: 'stepLine',
        color: getLineColor(lineKey, maxLevels/*, update*/), // todo: uncomment 'update' for debug colors
        connectNullData: false,
        markerType: 'none',
        dataPoints: dataPoints,
        _lineKey: lineKey,
      });
    }
  }
  return [chart.effectiveWindow, [...data], next_newWindowSizeMs];
}

function getMaxLevels(lines: {}): {
  askMaxLvl: number,
  bidMaxLvl: number,
} {
  let askMaxLvl = 0, bidMaxLvl = 0;
  for (const lineKey in lines) {
    if (lines.hasOwnProperty(lineKey)) {
      const lineLvl = parseInt(lineKey.replace(/\D/g, ''), 10);
      if (/ASK/.test(lineKey)) {
        askMaxLvl = !isNaN(lineLvl) && askMaxLvl < lineLvl ? lineLvl : askMaxLvl;
      } else {
        bidMaxLvl = !isNaN(lineLvl) && bidMaxLvl < lineLvl ? lineLvl : bidMaxLvl;
      }
    }
  }
  return {
    askMaxLvl: askMaxLvl,
    bidMaxLvl: bidMaxLvl,
  };
}

function getLineColor(lineKey: string, maxLevels: {
  askMaxLvl: number,
  bidMaxLvl: number,
}, update?: boolean) {
  const currentLvl = parseInt(lineKey.replace(/\D/g, ''), 10);
  let lvlStep = 70;

  if (/ASK/.test(lineKey)) {
    if (maxLevels.askMaxLvl > 0) lvlStep = (255 - lvlStep) / maxLevels.askMaxLvl;
    // '#f70063';
    return update ? `rgb(${255 - lvlStep * currentLvl}, 10, 10)` : `rgb(0, ${255 - lvlStep * currentLvl}, 0)`;
  } else {
    if (maxLevels.bidMaxLvl > 0) lvlStep = (255 - lvlStep) / maxLevels.bidMaxLvl;
    return update ? `rgb(10, 10, ${255 - lvlStep * currentLvl})` : `rgb(${255 - lvlStep * currentLvl}, 0, 0`;
  }
}


function clearChartData(
  raw_lines: { [key: string]: ChartRawLine },
  current_viewport,
  streamRange,
  tailsMultiplier,
) {
  let startDate = current_viewport.start,
    endDate = current_viewport.end;
  const TIME_DIFF = endDate - startDate;

  const newLines: { [key: string]: ChartRawLine } = {},
    currentDataWindowSize: {
      start?: string,
      end?: string,
    } = {};

  startDate -= (TIME_DIFF * tailsMultiplier);
  endDate += (TIME_DIFF * tailsMultiplier);

  if (startDate < streamRange.start) startDate = streamRange.start;
  if (endDate > streamRange.end) endDate = streamRange.end;

  if (Object.keys(raw_lines).length) {
    for (const LINE_KEY in raw_lines) {
      if (raw_lines.hasOwnProperty(LINE_KEY)) {
        newLines[LINE_KEY] = {
          ...raw_lines[LINE_KEY],
          points: raw_lines[LINE_KEY].points.filter(point => !!(point.time >= startDate && point.time <= endDate)),
        };
      }
    }
  }

  currentDataWindowSize.start = (new Date(startDate)).toISOString();
  currentDataWindowSize.end = (new Date(endDate)).toISOString();

  return [newLines, currentDataWindowSize];
}
