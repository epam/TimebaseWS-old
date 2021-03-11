import { Component, OnDestroy, OnInit, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { combineLatest, Subject, Observable } from 'rxjs';
import { select, Store } from '@ngrx/store';
import * as fromStreams from '../../store/streams-list/streams.reducer';
import { filter, takeUntil, tap, distinctUntilChanged, take } from 'rxjs/operators';
import * as fromStreamDetails from '../../store/stream-details/stream-details.reducer';
import { State as DetailsState } from '../../store/stream-details/stream-details.reducer';
import * as StreamDetailsActions from '../../store/stream-details/stream-details.actions';
import { StreamDetailsEffects } from '../../store/stream-details/stream-details.effects';
import { generateGridConfig } from '../../../../shared/utils/grid/config.generator';
import { AgGridNg2 } from 'ag-grid-angular';
import { GridOptions, GridReadyEvent, Column, ColumnResizedEvent, ColumnVisibleEvent, ColumnMovedEvent, ColumnPinnedEvent } from 'ag-grid-community';
import { defaultGridOptions, getContextMenuItems, setMaxColumnWidth, columnIsResized, columnIsVisible, columnIsMoved, columnIsPinned, gridStateLSInit } from '../../../../shared/utils/grid/config.defaults';
import { BsModalService, BsModalRef } from 'ngx-bootstrap/modal';
import { ModalCellJSONComponent }                              from '../modals/modal-cell-json/modal-cell-json.component';
import { AppState }                                            from '../../../../core/store';
import { streamsDetailsStateSelector, getStreamGlobalFilters } from '../../store/stream-details/stream-details.selectors';
import { getActiveOrFirstTab }                                 from '../../store/streams-tabs/streams-tabs.selectors';
import { TabModel }                                            from '../../models/tab.model';
import * as fromStreamProps from '../../store/stream-props/stream-props.reducer';
import * as StreamPropsActions from '../../store/stream-props/stream-props.actions';
import { StreamDetailsModel } from '../../models/stream.details.model';
import { FormBuilder, FormGroup, FormControl } from '@angular/forms';
import * as StreamQueryActions from '../../store/stream-query/stream-query.actions';
import { StreamQueryModel } from '../../models/query.model';
import * as fromStreamQuery from '../../store/stream-query/stream-query.reducer';
import { StreamQueryEffects } from '../../store/stream-query/stream-query.effects';
import { formatHDate } from '../../../../shared/locale.timezone';
import { GridStateModel } from '../../models/grid.state.model';

@Component({
  selector: 'app-stream-query',
  templateUrl: './stream-query.component.html',
  styleUrls: ['./stream-query.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StreamQueryComponent implements OnInit, OnDestroy {
  public schema = [];
  public rowData = new Map();
  @ViewChild('streamDetailsGridQuery', { static: true }) agGrid: AgGridNg2;

  public bsModalRef: BsModalRef;
  public gridOptions: GridOptions;
  private destroy$ = new Subject();
  private readyApi: GridOptions;
  private propsState: Observable<fromStreamProps.State>;
  public streamsQueryState: Observable<fromStreamQuery.State>;
  public queryForm: FormGroup;
  public queryObj: StreamQueryModel;
  private filter_date_format = [];
  private filter_time_format = [];
  private filter_timezone = [];
  private gridStateLS: GridStateModel = { visibleArray: [], pinnedArray: [], resizedArray: [], movedArray: [] };
  private columnsIdVisible = [];
  public tabName: string;

  private gridDefaults: GridOptions = {
    ...defaultGridOptions,
    rowBuffer: 10,
    enableFilter: true,
    enableCellChangeFlash: true,
    enableSorting: true,
    suppressRowClickSelection: false,
    deltaRowDataMode: false,
    rowSelection: 'single',
    gridAutoHeight: false,
    suppressNoRowsOverlay: true,
    getContextMenuItems: getContextMenuItems.bind(this),
    onCellDoubleClicked: (params) => {
      if (params.value && typeof params.value === 'object') {
        return this.openModalCellJSON(params);
      } else {
        this.appStore.dispatch(new StreamPropsActions.ChangeStateProps({
          opened: true,
        }));
      }
    },
    onGridReady: (readyEvent: GridReadyEvent) => this.gridIsReady(readyEvent),
    onColumnResized: (resizedEvent: ColumnResizedEvent) => columnIsResized(resizedEvent, this.tabName, this.gridStateLS),
    onColumnVisible: (visibleEvent: ColumnVisibleEvent) => columnIsVisible(visibleEvent, this.tabName, this.gridStateLS),
    onColumnMoved: (movedEvent: ColumnMovedEvent) => columnIsMoved(movedEvent, this.tabName, this.gridStateLS),
    onColumnPinned: (pinnedEvent: ColumnPinnedEvent) => columnIsPinned(pinnedEvent, this.tabName, this.gridStateLS),
    getMainMenuItems: (params) => {
      //  const items = [...params.defaultItems];
      return [
        'pinSubMenu',
        'separator',
        // 'autoSizeThis',
        {
          name: 'Autosize This Column',
          action: () => {
            params.columnApi.autoSizeColumn(params.column);
            if (params.column.getActualWidth() > 500) {
              params.columnApi.setColumnWidth(params.column, 500, true);
            }
            const filtered = this.gridStateLS.resizedArray.filter(item => item.colId !== params.column.getColId());
            this.gridStateLS.resizedArray = [...filtered];
            localStorage.setItem('gridStateLS' + this.tabName, JSON.stringify(this.gridStateLS));
          },
        },
        // 'autoSizeAll',
        {
          name: 'Autosize All Columns',
          action: () => {
            params.columnApi.autoSizeAllColumns();
            setMaxColumnWidth(params.columnApi);
            this.gridStateLS.resizedArray = [];
            localStorage.setItem('gridStateLS' + this.tabName, JSON.stringify(this.gridStateLS));
          },
        },
        'separator',
        'separator',
        {
          name: 'Reset Columns',
          action: () => {
            this.gridStateLS = { visibleArray: [], pinnedArray: [], resizedArray: [], movedArray: [] };
            localStorage.removeItem('gridStateLS' + this.tabName);
            if (this.rowData) {
              params.columnApi.resetColumnState();
              this.columnsIdVisible = [];
              this.columnsVisibleData(params.columnApi, this.rowData);
              setTimeout(() => {
                params.columnApi.autoSizeAllColumns();
              }, 100);
            }
          },
        }];
    },
  };

  constructor(
    private appStore: Store<AppState>,
    private streamsStore: Store<fromStreams.FeatureState>,
    private streamDetailsStore: Store<fromStreamDetails.FeatureState>,
    private streamQueryStore: Store<fromStreamQuery.FeatureState>,
    private streamDetailsEffects: StreamDetailsEffects,

    private streamQueryEffects: StreamQueryEffects,
    private modalService: BsModalService,
    private streamPropsStore: Store<fromStreamProps.FeatureState>,
    private fb: FormBuilder,
  ) { }

  ngOnInit() {
    this.gridOptions = {
      ...this.gridDefaults,
    };

    this.appStore
      .pipe(
        select(getStreamGlobalFilters),
        filter(global_filter => !!global_filter),
        takeUntil(this.destroy$),
        distinctUntilChanged(),
      )
      .subscribe((action => {
        if (action.filter_date_format && action.filter_date_format.length) {
          this.filter_date_format = [...action.filter_date_format];
        } else {
          this.filter_date_format = [];
        }
        if (action.filter_time_format && action.filter_time_format.length) {
          this.filter_time_format = [...action.filter_time_format];
        } else {
          this.filter_time_format = [];
        }
        if (action.filter_timezone && action.filter_timezone.length) {
          this.filter_timezone = [...action.filter_timezone];
        } else {
          this.filter_timezone = [];
        }
        if (this.queryObj && this.queryObj.rows) {
          this.streamsStore.dispatch(new StreamQueryActions.GetStreamsQuery({ query: this.queryObj }));
          this.streamsStore.dispatch(new StreamQueryActions.GetStreamsQueryDescribe({ query: this.queryObj.query }));
        }
      }
      ));

    this.queryObj = new StreamQueryModel({});

    if (!this.queryForm) {
      this.queryForm = this.fb.group({
        'rows': new FormControl(),
        'queryTextarea': new FormControl(),
      });
    }

    this.queryForm.get('rows').setValue(100);
    this.queryForm.get('queryTextarea').setValue('');
    if (localStorage.getItem('qqlLast')) {
      this.queryForm.get('queryTextarea').setValue(JSON.parse(localStorage.getItem('qqlLast')));
    }

    this.propsState = this.streamPropsStore.pipe(select('streamProps'));
    this.propsState
      .pipe(
        filter((props: any) => props && props.props),
        distinctUntilChanged(),
        take(1),
        takeUntil(this.destroy$),
      )
      .subscribe((/*props*/) => {
        combineLatest([
          this.appStore
            .pipe(
              select(getActiveOrFirstTab),
              filter((tab: TabModel) => tab && tab.query),
              tap((tab) => {
                this.streamsStore.dispatch(new StreamDetailsActions.GetSchema({ streamId: tab.stream }));
              }),
            ),
          this.appStore
            .pipe(
              select(streamsDetailsStateSelector),
              filter((state: DetailsState) => !!state.schema),
              // distinctUntilChanged((p: DetailsState, q: DetailsState) => p.schema === q.schema || p.filter_types === q.filter_types || p.filter_symbols === q.filter_symbols),
            ),
        ])
          .pipe(
            tap(() => {
              //   this.readyApi.api.setRowData(null);
            }),
            takeUntil(this.destroy$),
          )
          .subscribe(([tab, action]: [TabModel, DetailsState]) => {
          });
      });
  }

  private gridIsReady(readyEvent: GridReadyEvent) {
    this.readyApi = { ...readyEvent };

    this.streamDetailsEffects
      .setSchema
      .pipe(
        takeUntil(this.destroy$),
      )
      .subscribe(action => {

        if (action.payload.schema && action.payload.schema.length) {
          this.schema = [...action.payload.schema];
        }
        const hideAllColumns = true;
        const props = [
          {
            headerName: '', minWidth: 30, maxWidth: 30, width: 30,
            // headerCheckboxSelection: true,
            headerCheckboxSelectionFilteredOnly: true,
            checkboxSelection: true,
            filter: false,
            sortable: false,
            pinned: 'left',
          },
          {
            headerName: 'Symbol',
            field: 'symbol',
            pinned: 'left',
            filter: false,
            sortable: false,
            width: 100,
            headerTooltip: 'Symbol',
          },
          {
            headerName: 'Timestamp',
            field: 'timestamp',
            pinned: 'left',
            filter: false,
            sortable: false,
            width: 160,
            headerTooltip: 'Timestamp',
            cellRenderer: (data) => {
              return formatHDate(data.value, this.filter_date_format, this.filter_time_format, this.filter_timezone);
            },
          },
          {
            headerName: 'Type',
            field: '$type',
            pinned: 'left',
            filter: false,
            sortable: false,
            headerTooltip: 'Type',
            hide: true,
          },
          ...generateGridConfig(action.payload.schema, '', hideAllColumns, this.filter_date_format, this.filter_time_format)];
        readyEvent.api.setColumnDefs(null);
        readyEvent.api.setColumnDefs(props);
        this.streamsQueryState = this.streamQueryStore.pipe(select('streamQuery'));
        this.streamsQueryState
          .pipe(
            filter(streamQuery => !!streamQuery),
            distinctUntilChanged(),
            takeUntil(this.destroy$),
          )
          .subscribe(streamQuery => {
            readyEvent.api.setRowData([]);
            const newRowData = [];
            this.columnsIdVisible = [];


            if (streamQuery.streamQuery && streamQuery.streamQuery.length) {
              streamQuery.streamQuery.forEach(row => {
                const rowStreamModel = new StreamDetailsModel(row);
                newRowData.push(rowStreamModel);
              });
            }
            readyEvent.api.setRowData(newRowData);
            this.columnsVisibleData(readyEvent.columnApi, newRowData);
            this.gridStateLS = gridStateLSInit(readyEvent.columnApi, this.tabName, this.gridStateLS);

            this.readyApi.columnApi.autoSizeAllColumns();

            setMaxColumnWidth(readyEvent.columnApi);
            if (this.gridStateLS.resizedArray.length) {
              for (const item of this.gridStateLS.resizedArray) {
                readyEvent.columnApi.setColumnWidth(item.colId, item.actualWidth, true);
              }
            }
          });
      });
  }

  columnsVisibleData(columnApi, rowData) {
    const cols: Column[] = columnApi.getAllColumns();
    if (cols && cols.length) {
      for (let i = 0; i < cols.length; i++) {
        const colIdArr = cols[i]['colId'].split('.');

        if (colIdArr.length === 2) {
          if (this.columnsIdVisible.find(item => item === cols[i]['colId'])) {
            return;
          }
          if (rowData.find(item => {
            if (item.hasOwnProperty(colIdArr[0])) {
              return item[colIdArr[0]][colIdArr[1]];
            }
          })) {
            columnApi.setColumnVisible(cols[i]['colId'], true);
            this.columnsIdVisible.push(cols[i]['colId']);
          } else {
            columnApi.setColumnVisible(cols[i]['colId'], false);
          }
        }
      }
    }
  }

  openModalCellJSON(params: any) {
    const initialState = {
      title: 'Viewer of JSON Data',
      data: params.value,
    };
    this.bsModalRef = this.modalService.show(ModalCellJSONComponent, { initialState });
  }


  changeQQLText() {
    localStorage.setItem('qqlLast', JSON.stringify(this.queryForm.get('queryTextarea').value || ''));
  }

  sendQueryCtrlEnter(event: any) {
    if ((event.keyCode === 10 || event.keyCode === 13) && event.ctrlKey) {
      this.sendQuery();
    }
  }

  sendQuery() {
    let req = this.queryForm.get('queryTextarea').value;
    if (req && req.length) {
      req = req.trim();
      req = req.replace(/\s+/g, ' ');
      const streamArr = req.split(' ');
      const index = req.toLowerCase().split(' ').indexOf('from');
      if (index > -1) {
        this.tabName = streamArr[index + 1];
      }
    }
    this.queryObj.rows = this.queryForm.get('rows').value;
    this.queryObj.query = req;
    this.queryObj.offset = 0;
    /* this.queryObj.reverse = false;
      this.queryObj.types = null;
      this.queryObj.symbols = null;*/
    this.streamsStore.dispatch(new StreamQueryActions.GetStreamsQuery({ query: this.queryObj }));
    this.streamsStore.dispatch(new StreamQueryActions.GetStreamsQueryDescribe({ query: this.queryObj.query }));
  }

  ngOnDestroy(): void {
    this.streamsStore.dispatch(new StreamQueryActions.ClearStreamsQuery());
    this.destroy$.next(true);
    this.destroy$.complete();
    this.streamsStore.dispatch(new StreamDetailsActions.StopSubscriptions());
  }

}
