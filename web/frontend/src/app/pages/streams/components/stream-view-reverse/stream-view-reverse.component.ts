import { ActivatedRoute, Data }                                                          from '@angular/router';
import { Component, OnDestroy, OnInit, ViewChild, ChangeDetectionStrategy }              from '@angular/core';
import { Observable, Subject, Subscription }                                             from 'rxjs';
import { StreamDetailsModel }                                                            from '../../models/stream.details.model';
import { TabModel }                                                                      from '../../models/tab.model';
import { select, Store }                                                                 from '@ngrx/store';
import * as fromStreams
                                                                                         from '../../store/streams-list/streams.reducer';
import { filter, take, takeUntil, withLatestFrom, distinctUntilChanged, switchMap, map } from 'rxjs/operators';
import * as fromStreamDetails
                                                                                         from '../../store/stream-details/stream-details.reducer';
import * as StreamDetailsActions
                                                                                         from '../../store/stream-details/stream-details.actions';
import * as StreamPropsActions
                                                                                         from '../../store/stream-props/stream-props.actions';
import { StreamDetailsEffects }                                                          from '../../store/stream-details/stream-details.effects';
import { generateGridConfig }                                                            from '../../../../shared/utils/grid/config.generator';
import { AgGridNg2 }                                                                     from 'ag-grid-angular';
import {
  GridOptions,
  GridReadyEvent,
  Column,
  ColumnResizedEvent,
  ColumnVisibleEvent,
  ColumnMovedEvent,
  ColumnPinnedEvent,
  ColumnApi,
}                                                                                        from 'ag-grid-community';
import {
  defaultGridOptions,
  getContextMenuItems,
  columnIsResized,
  columnIsVisible,
  columnIsMoved,
  columnIsPinned,
  gridStateLSInit,
  setMaxColumnWidth,
}                                                                                        from '../../../../shared/utils/grid/config.defaults';
import { StreamDataService }                                                             from '../../services/stream-data.service';
import { BsModalRef, BsModalService }                                                    from 'ngx-bootstrap/modal';
import { HdDate }                                                                        from '@deltix/hd-date';
import { ModalCellJSONComponent }                                                        from '../modals/modal-cell-json/modal-cell-json.component';
import { AppState }                                                                      from '../../../../core/store';
import { streamProps }                                                                   from '../../store/stream-props/stream-props.selectors';
import * as FilterActions
                                                                                         from '../../store/filter/filter.actions';
import * as TimelineBarActions
                                                                                         from '../../store/timeline-bar/timeline-bar.actions';
import { WebsocketService }                                                              from '../../../../core/services/websocket.service';
import {
  streamsDetailsStateSelector,
  getStreamData,
  getStreamGlobalFilters,
  getStreamOrSymbolByID,
}                                                                                        from '../../store/stream-details/stream-details.selectors';
import * as StreamsTabsActions
                                                                                         from '../../store/streams-tabs/streams-tabs.actions';
import {
  getActiveOrFirstTab,
  getActiveTabFilters,
  getTabs,
}                                                                                        from '../../store/streams-tabs/streams-tabs.selectors';
import { FilterModel }                                                                   from '../../models/filter.model';
import { streamsListStateSelector }                                                      from '../../store/streams-list/streams.selectors';
import { formatHDate }                                                                   from '../../../../shared/locale.timezone';
import { GridStateModel }                                                                from '../../models/grid.state.model';
import { ModalSendMessageComponent }                                                     from '../modals/modal-send-message/modal-send-message.component';

const now = new HdDate();

export const toUtc = (date: any) => {
  const newDate = new HdDate(date);
  newDate.setMilliseconds(newDate.getMilliseconds() + now.getTimezoneOffset() * 60 * 1000);
  return newDate;
};

export const fromUtc = (date: any) => {
  const newDate = new HdDate(date);
  newDate.setMilliseconds(newDate.getMilliseconds() - now.getTimezoneOffset() * 60 * 1000);
  return newDate;
};

@Component({
  selector: 'app-stream-view-reverse',
  templateUrl: './stream-view-reverse.component.html',
  styleUrls: ['./stream-view-reverse.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StreamViewReverseComponent implements OnInit, OnDestroy {
  public closedProps: boolean;
  public bsModalRef: BsModalRef;
  public schema = [];
  public symbols = [];
  public streamName: string;
  public tabName: string;
  public streamDetails: Observable<fromStreamDetails.State>;
  public activeTab: Observable<TabModel>;
  public live: boolean;
  // private reverse: boolean;
  private tabFilter;
  // private typeName = '';
  private isOpenInNewTab: boolean;
  private columnsIdVisible = [];
  private filter_date_format = [];
  private filter_time_format = [];
  private filter_timezone = [];
  private gridStateLS: GridStateModel = {visibleArray: [], pinnedArray: [], resizedArray: [], movedArray: []};
  private rowData;

  @ViewChild('streamDetailsGrid', {static: true}) agGrid: AgGridNg2;
  public gridOptions: GridOptions;

  public websocketSub: Subscription;
  private destroy$ = new Subject();
  private readyApi: GridOptions;
  private streamId: string;
  private gridDefaults: GridOptions = {
    ...defaultGridOptions,
    rowBuffer: 10,
    enableFilter: true,
    // enableCellChangeFlash: true,
    enableSorting: true,
    suppressRowClickSelection: false,
    rowSelection: 'multiple',
    defaultColDef: {
      filter: false,
      sortable: false,
    },
    enableRangeSelection: true,
    getContextMenuItems: (params => {
      return [
        ...getContextMenuItems(params),

        'separator',
        {
          name: 'Send Message',
          action: () => {
            if (!this.streamId) return;
            const initialState = {
              stream: {
                key: this.streamId,
                name: this.streamName,
              },
              formData: (params.node.data as StreamDetailsModel), // (params.node.data as StreamDetailsModel).$type, //
            };
            this.bsModalRef = this.modalService.show(ModalSendMessageComponent, {
              initialState: initialState,
              ignoreBackdropClick: true,
              class: 'modal-message',
            });
          },
        },
      ];
    }),
    rowModelType: 'serverSide',
    // cacheBlockSize: 5,
    infiniteInitialRowCount: 1,
    maxConcurrentDatasourceRequests: 1,
    enableServerSideSorting: true,
    enableServerSideFilter: true,
    gridAutoHeight: false,
    stopEditingWhenGridLosesFocus: true,
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
            this.gridStateLS = {visibleArray: [], pinnedArray: [], resizedArray: [], movedArray: []};
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
    onModelUpdated: (params) => {
      params.columnApi.autoSizeAllColumns();
      setMaxColumnWidth(params.columnApi);
      if (this.gridStateLS.resizedArray.length) {
        for (const item of this.gridStateLS.resizedArray) {
          params.columnApi.setColumnWidth(item.colId, item.actualWidth, true);
        }
      }
    },


  };

  constructor(
    private appStore: Store<AppState>,
    private route: ActivatedRoute,
    private streamsStore: Store<fromStreams.FeatureState>,
    private streamDetailsStore: Store<fromStreamDetails.FeatureState>,
    private streamDetailsEffects: StreamDetailsEffects,
    private dataSource: StreamDataService,
    private modalService: BsModalService,
    private wsService: WebsocketService,
    //  private streamPropsStore: Store<fromStreamProps.FeatureState>,
  ) { }

  ngOnInit() {
    this.streamDetails = this.streamDetailsStore.pipe(select(streamsDetailsStateSelector));

    this.activeTab = this.appStore.pipe(select(getActiveOrFirstTab));

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
          if (this.tabFilter) {
            this.appStore.dispatch(new StreamsTabsActions.SetFilters({filter: this.tabFilter}));
          }
        }
      ));


    this.appStore
      .pipe(
        select(streamsListStateSelector),
        filter((_openNewTab) => !!_openNewTab),
        takeUntil(this.destroy$),
      )
      .subscribe((data: any) => {
        this.isOpenInNewTab = data._openNewTab;
      });

    this.appStore
      .pipe(
        select(getTabs),
        filter((tabs) => !!tabs),
        take(1),
        takeUntil(this.destroy$),
      )
      .subscribe((tabs: TabModel[]) => {
        this.route.params
          .pipe(
            withLatestFrom(this.route.data),
            filter(([params, data]: [{ stream: string, id: string, symbol?: string }, Data]) => !!params.stream),
            switchMap(([params, data]: [{ stream: string, id: string, symbol?: string }, Data]) => this.appStore
              .pipe(
                select(getStreamOrSymbolByID, {streamID: params.stream, symbol: params.symbol, uid: params.id}),
                filter((tabModel: TabModel) => !!tabModel),
                take(1),
                map((tabModel: TabModel) => {
                  return [
                    tabModel,
                    data,
                    tabs,
                  ];
                }),
              )),
            takeUntil(this.destroy$),
          )
          .subscribe(([tabModel, data, tabs]: [TabModel, Data, TabModel[]]) => {
            this.streamDetailsStore.dispatch(new StreamDetailsActions.GetSymbols({streamId: tabModel.stream}));
            this.streamId = tabModel.stream;
            this.live = data.hasOwnProperty('live');
            //  this.reverse = data.hasOwnProperty('reverse');
            this.streamName = tabModel.name;
            if (!tabModel.stream) return;
            const tab: TabModel = new TabModel({
              ...tabModel,
              ...data,
              active: true,
            });
            // Key is streamName for LocalStorage  Grid State
            this.tabName = tabModel.stream;

            const prevActTab = JSON.parse(localStorage.getItem('prevActTab'));
            const tabsItemEquilPrev = tabs.find(item => item.id === tab.id);
            let position = -1;
            if (!this.isOpenInNewTab && prevActTab && prevActTab.id) {
              position = tabs.map(e => e.id).indexOf(prevActTab.id);
            }

            this.streamsStore.dispatch(new StreamsTabsActions.AddTab({
              tab: tab,
              position: position,
            }));

            if (localStorage.getItem('prevActTab') && prevActTab.id !== tab.id && !this.isOpenInNewTab) {
              // Don't remember why is it
              prevActTab['live'] = false;

              if (!tabsItemEquilPrev) {
                this.streamsStore.dispatch(new StreamsTabsActions.RemoveTab({
                  tab: prevActTab,
                }));
              }
            }

            localStorage.setItem('prevActTab', JSON.stringify(tab));
          });

        this.appStore
          .pipe(
            select(getActiveTabFilters),
            filter((filter) => !!filter),
            take(1),
            takeUntil(this.destroy$),
          )
          .subscribe((filter: FilterModel) => {
            this.appStore.dispatch(new TimelineBarActions.ClearLoadedDates());
            this.appStore.dispatch(new FilterActions.SetFilters({
              filter: {...filter} || {},
            }));

            this.streamsStore.dispatch(new StreamDetailsActions.CleanStreamData());
          });

      });

    this.appStore.pipe(select(streamProps)).subscribe(() => this.gridOptions = {
      ...this.gridDefaults,

    });

    // TODO: Why is second 'setSchema' ?
    this.streamDetailsEffects
      .setSchema
      .pipe(
        // take(1),
        takeUntil(this.destroy$),
      )
      .subscribe(action => {
        if (action.payload.schema && action.payload.schema.length) {
          this.schema = [...action.payload.schema];
        }
      });
  }

  cleanWebsocketSubscription() {
    this.wsService.close();
  }

  closedPropsEmit($event) {
    this.closedProps = $event;
  }

  public onHideErrorMessage() {
    this.appStore.dispatch(new StreamDetailsActions.RemoveErrorMessage());
  }

  private gridIsReady(readyEvent: GridReadyEvent) {

    this.readyApi = {...readyEvent};


    this.appStore
      .pipe(
        select(getActiveTabFilters),
        filter((filter) => !!filter),
        takeUntil(this.destroy$),
      )
      .subscribe((filter: FilterModel) => {
        this.tabFilter = {...filter};
        this.columnsIdVisible = [];
        readyEvent.api.setServerSideDatasource(this.dataSource);
      });

    this.streamDetailsEffects
      .setSchema
      .pipe(
        // take(1),
        takeUntil(this.destroy$),
      )
      .subscribe(action => {
        if (!action.payload.schema) {
          return;
        }
        if (action.payload.schema && action.payload.schema.length) {
          this.schema = [...action.payload.schema];
        }

        const hideAllColumns = true; // https://gitlab.deltixhub.com/Deltix/QuantServer/TimebaseWS/issues/106
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
            headerTooltip: 'Symbol',
          },
          {
            headerName: 'Timestamp',
            field: 'timestamp',
            pinned: 'left',
            filter: false,
            sortable: false,
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
        // this.columns = props;
        readyEvent.api.setColumnDefs(null);
        readyEvent.api.setColumnDefs(props);
      });
    this.columnsIdVisible = [];
    this.streamsStore.dispatch(new StreamDetailsActions.SubscribeTabChanges());
    this.appStore
      .pipe(
        select(getStreamData),
        filter((data) => !!data),
        takeUntil(this.destroy$),
      )
      .subscribe((data: any) => {
        this.rowData = data;


        this.columnsVisibleData(readyEvent.columnApi, data);
      });
  }

  columnsVisibleData(columnApi: ColumnApi, data: any) {
    const cols: Column[] = columnApi.getAllColumns();
    // TODO: sometimes if > 1000 columns don't work getAllColumns() - is NULL , need another method
    if (cols && cols.length) {
      for (let i = 0; i < cols.length; i++) {
        const colIdArr = cols[i]['colId'].split('.');

        if (colIdArr.length === 2) {
          if (this.columnsIdVisible.find(item => item === cols[i]['colId'])) {
            return;
          }
          if (data.find(item => {
            if (item.hasOwnProperty(colIdArr[0])) {
              return item[colIdArr[0]][colIdArr[1]];
            }
          })) {
            columnApi.setColumnVisible(cols[i]['colId'], true);
            this.columnsIdVisible.push(cols[i]['colId']);
          }
        }
      }
    }
    this.gridStateLS = gridStateLSInit(columnApi, this.tabName, this.gridStateLS);
  }

  openModalCellJSON(params: any) {
    const initialState = {
      title: 'Viewer of JSON Data',
      data: params.value,
    };
    this.bsModalRef = this.modalService.show(ModalCellJSONComponent, {initialState});
  }

  ngOnDestroy(): void {
    this.cleanWebsocketSubscription();
    this.destroy$.next(true);
    this.destroy$.complete();
    this.streamsStore.dispatch(new StreamDetailsActions.StopSubscriptions());
  }

}




