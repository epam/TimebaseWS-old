import { GridOptions, ColumnVisibleEvent, ColumnPinnedEvent, ColumnResizedEvent, ColumnMovedEvent, ColumnApi } from 'ag-grid-community';
import { CustomNoRowOverlayComponent } from './custom-no-rows-overlay.component';
import { GridStateModel } from '../../../pages/streams/models/grid.state.model';

let gridType = '';

export const defaultGridOptions: GridOptions = {
  enableColResize: true,
  suppressCopyRowsToClipboard: true,
  processCellForClipboard: (params) => {
    if (typeof params.value === 'object') {
      return JSON.stringify(params.value);
    }
    return params.value;
  },
  processHeaderForClipboard: (params) => {
    if (typeof params === 'object') {
      return JSON.stringify(params);
    }
    return params;
  },
  processCellFromClipboard: (params) => {
    if (typeof params.value === 'object') {
      return JSON.stringify(params.value);
    }
    return params.value;
  },
  // localeText: gridLocaleText,
  popupParent: document.querySelector('body'),
  onColumnPinned: (props) => saveState(props, gridType),
  onColumnResized: (props) => saveState(props, gridType),
  onColumnMoved: (props) => saveState(props, gridType),
  onColumnRowGroupChanged: (props) => saveState(props, gridType),
  onColumnValueChanged: (props) => saveState(props, gridType),
  onColumnPivotModeChanged: (props) => saveState(props, gridType),
  onColumnPivotChanged: (props) => saveState(props, gridType),
  onColumnGroupOpened: (props) => saveState(props, gridType),
  onNewColumnsLoaded: (props) => saveState(props, gridType),
  onGridColumnsChanged: (props) => saveState(props, gridType),
  onDisplayedColumnsChanged: (props) => saveState(props, gridType),
  onVirtualColumnsChanged: (props) => saveState(props, gridType),
  // onSortChanged: (props) => saveState(props, gridType),
  // onFilterChanged: (props) => saveState(props, gridType),
  onDragStopped: (props) => saveState(props, gridType),
  // onToolPanelVisibleChanged: (props) => this.saveState(props),
  components: {
    agNoRowsOverlay: CustomNoRowOverlayComponent,
  },
  onModelUpdated: (event) => {
    const rows = (event.api as any).rowRenderer.rowCompsByIndex;
    if (Array.from(Object.keys(rows)).length > 0) {
      event.api.hideOverlay();
    } else {
      if (event.api) {
        event.api.showNoRowsOverlay();
      }
    }
  },
  // frameworkComponents: {
  //   baseInputEditorComponent: CustomBaseTextEditorComponent,
  //   numberEditorComponent: CustomNumberEditorComponent,
  //   bigEditorComponent: CustomNumberEditorComponent,
  // },
};

// export function restoreState(props: GridOptions, type: string) {
// }

export function saveState(props: GridOptions, type: string) {
  if (!type) {
    return;
  }
  const columnState = props.columnApi.getColumnState();
  const columnGroupState = props.columnApi.getColumnGroupState();
  // const sortModel = props.api.getSortModel();
  // const filterModel = props.api.getFilterModel();

  localStorage.setItem('columnState' + type, JSON.stringify(columnState));
  localStorage.setItem('columnGroupState' + type, JSON.stringify(columnGroupState));

  //  localStorage.setItem('sortModel' + type, JSON.stringify(sortModel));
  // localStorage.setItem('filterModel' + type, JSON.stringify(filterModel));
}

export function getGridType(type: string) {
  return gridType = type;
}


export function getContextMenuItems(params) {
  return [
    'copy',
    'copyWithHeaders',
    'paste',
    // 'resetColumns',
    {
      name: 'Copy JSON',
      action: function () {
        if (!(params.node && params.node.data && params.node.data.$type)) return;
        const type = params.node.data.$type.replace(/\./g, '-');
        document.addEventListener('copy', (e: ClipboardEvent) => {
          e.clipboardData.setData('text/plain', (JSON.stringify(params.node.data[type])));
          e.preventDefault();
          document.removeEventListener('copy', null);
        });
        document.execCommand('copy');

      },
    },
    'separator',
    {
      name: 'Export',
      subMenu: [
        'csvExport',
        'excelExport',
        'excelXmlExport',
        /* {
           name: 'XML Export (.xml)',
           action: function () {
             params.exportMode = 'xml';
             params.api.exportDataAsExcel(params);
           },
         },*/
      ],
    },
  ];
}


export function columnIsVisible(visibleEvent: ColumnVisibleEvent, tabName: string, gridStateLS: GridStateModel) {
  if (visibleEvent.source === 'toolPanelUi') {
    for (const col of visibleEvent.columns) {
      const item = gridStateLS.visibleArray.find(item => item.colId === col['colId']);
      if (gridStateLS.visibleArray.length && item) {
        item.visible = visibleEvent.visible;
      } else {
        gridStateLS.visibleArray.push({ colId: col['colId'], visible: visibleEvent.visible });
      }
    }
    localStorage.setItem('gridStateLS' + tabName, JSON.stringify(gridStateLS));
  }
  if (visibleEvent.source === 'columnMenu') {
    const item = gridStateLS.visibleArray.find(item => item.colId === visibleEvent.column['colId']);
    if (gridStateLS.visibleArray.length && item) {
      item.visible = visibleEvent.visible;
    } else {
      gridStateLS.visibleArray.push({ colId: visibleEvent.column['colId'], visible: visibleEvent.visible });
    }
    localStorage.setItem('gridStateLS' + tabName, JSON.stringify(gridStateLS));
  }
}

export function columnIsPinned(pinnedEvent: ColumnPinnedEvent, tabName: string, gridStateLS: GridStateModel) {
  if (pinnedEvent.source === 'contextMenu') {
    const item = gridStateLS.pinnedArray.find(item => item.colId === pinnedEvent.column['colId']);
    if (gridStateLS.pinnedArray.length && item) {
      item.pinned = pinnedEvent.pinned;
    } else {
      gridStateLS.pinnedArray.push({ colId: pinnedEvent.column['colId'], pinned: pinnedEvent.pinned });
    }
    localStorage.setItem('gridStateLS' + tabName, JSON.stringify(gridStateLS));
  }
}

export function columnIsResized(resizedEvent: ColumnResizedEvent, tabName: string, gridStateLS: GridStateModel) {
  if (resizedEvent.finished && resizedEvent.source === 'uiColumnDragged') {
    const item = gridStateLS.resizedArray.find(item => item.colId === resizedEvent.column['colId']);
    if (gridStateLS.resizedArray.length && item) {
      item.actualWidth = resizedEvent.column.getActualWidth();
    } else {
      gridStateLS.resizedArray.push({ colId: resizedEvent.column['colId'], actualWidth: resizedEvent.column.getActualWidth() });
    }
    localStorage.setItem('gridStateLS' + tabName, JSON.stringify(gridStateLS));
  }
}

export function columnIsMoved(movedEvent: ColumnMovedEvent, tabName: string, gridStateLS: GridStateModel) {
  if (movedEvent.source === 'uiColumnDragged') {
    const columnsArray = movedEvent.columnApi.getAllColumns();
    let prevColumnId = '';
    if (movedEvent.toIndex > 0) {
      prevColumnId = columnsArray[movedEvent.toIndex - 1].getColId();
    }

    const item = gridStateLS.movedArray.find(item => item.colId === movedEvent.column['colId']);
    if (gridStateLS.movedArray.length && item) {
      item.toIndex = movedEvent.toIndex;
      item.colIdPrev = prevColumnId;
    } else {
      gridStateLS.movedArray.push({ colId: movedEvent.column['colId'], toIndex: movedEvent.toIndex, colIdPrev: prevColumnId });
    }
    localStorage.setItem('gridStateLS' + tabName, JSON.stringify(gridStateLS));
  }
}

export function gridStateLSInit(columnApi: ColumnApi, tabName: string, gridStateLS: GridStateModel) {
  if (localStorage.getItem('gridStateLS' + tabName)) {
    const allColumns = columnApi.getAllColumns();
    if (allColumns && allColumns.length) {
      gridStateLS = JSON.parse(localStorage.getItem('gridStateLS' + tabName));
      if (gridStateLS.visibleArray.length) {
        for (const item of gridStateLS.visibleArray) {
          const COLUMN = columnApi ? columnApi.getColumn(item.colId) : null;
          if (COLUMN) COLUMN.setVisible(item.visible);
        }
      }
      if (gridStateLS.pinnedArray.length) {
        for (const item of gridStateLS.pinnedArray) {
          columnApi.setColumnPinned(item.colId, item.pinned);
        }
      }
      if (gridStateLS.movedArray.length) {
        for (const item of gridStateLS.movedArray) {
          if (item.colIdPrev && item.colIdPrev.length) {
            const index = allColumns.findIndex(el => el['colId'] === item.colIdPrev);
            if (index > -1) {
              columnApi.moveColumn(item.colId, index + 1);
            }
          } else {
            columnApi.moveColumn(item.colId, item.toIndex);
          }
        }
      }
    }
  }
  return gridStateLS;
}


export function setMaxColumnWidth(columnApi: ColumnApi) {
  const displCols = columnApi.getAllDisplayedColumns();
  if (displCols && displCols.length) {
    for (const col of displCols) {
      if (col.getActualWidth() > 500) {
        columnApi.setColumnWidth(col, 500, true);
      }
    }
  }
}
