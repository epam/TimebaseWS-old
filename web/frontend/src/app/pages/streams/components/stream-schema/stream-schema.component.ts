import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { combineLatest, Observable, Subject }                               from 'rxjs';
import { TabModel }                                                         from '../../models/tab.model';
import { select, Store }                                                    from '@ngrx/store';
import * as fromStreams
                                                                            from '../../store/streams-list/streams.reducer';
import * as fromStreamsSchema
                                                                            from '../../store/stream-schema/stream-schema.reducer';
import { distinctUntilChanged, filter, take, takeUntil, tap }               from 'rxjs/operators';
import * as StreamDetailsActions
                                                                            from '../../store/stream-details/stream-details.actions';
import { StreamDetailsEffects }                                             from '../../store/stream-details/stream-details.effects';
import * as StreamSchemaActions
                                                                            from '../../store/stream-schema/stream-schema.actions';
import { GridOptions, GridReadyEvent, SelectionChangedEvent }               from 'ag-grid-community';
import { defaultGridOptions }          from '../../../../shared/utils/grid/config.defaults';
import { AppState }                    from '../../../../core/store';
import { streamsDetailsStateSelector } from '../../store/stream-details/stream-details.selectors';
import { getActiveOrFirstTab }                                              from '../../store/streams-tabs/streams-tabs.selectors';
import * as fromStreamProps
                                                                            from '../../store/stream-props/stream-props.reducer';
import { State as DetailsState }                                            from '../../store/stream-details/stream-details.reducer';
import * as StreamPropsActions
                                                                            from '../../store/stream-props/stream-props.actions';
import { AgGridNg2 }                                                        from 'ag-grid-angular';

@Component({
  selector: 'app-stream-schema',
  templateUrl: './stream-schema.component.html',
  styleUrls: ['./stream-schema.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StreamSchemaComponent implements OnInit, OnDestroy {

  public schemaAll = [];
  private schemaFields = [];
  private selectedRow = {};
  private destroy$ = new Subject();
  private propsState: Observable<fromStreamProps.State>;
  public gridOptions: GridOptions;
  private readyApi: GridOptions;
  @ViewChild('streamDetailsSchema', {static: true}) agGrid: AgGridNg2;
  private gridDefaults: GridOptions = {
    ...defaultGridOptions,
    rowBuffer: 10,
    enableFilter: true,
    enableCellChangeFlash: true,
    enableSorting: true,

    suppressRowClickSelection: false,
    deltaRowDataMode: true,
    gridAutoHeight: false,
    suppressNoRowsOverlay: true,
    treeData: true,
    groupDefaultExpanded: -1,
    getDataPath: (data: any) => this.getDataPath(data),
    autoGroupColumnDef: {
      sortable: true,
      filter: 'agTextColumnFilter',
      headerName: 'Name',
    },
    // gridAutoHeight: true,
    rowSelection: 'single',
    onGridReady: (readyEvent: GridReadyEvent) => this.gridIsReady(readyEvent),
    getRowNodeId: data => {
      return data.name;
    },
    onCellDoubleClicked: (/*params*/) => {
      this.appStore.dispatch(new StreamPropsActions.ChangeStateProps({
        opened: true,
      }));
    },
    onSelectionChanged: (event: SelectionChangedEvent) => {
      this.selectedRow = event.api.getSelectedRows()[0];
      if (this.selectedRow && this.selectedRow['fields']) {
        this.schemaFields = [...this.selectedRow['fields']];
      } else {
        this.schemaFields = [];
      }

      if (this.selectedRow) {
        this.streamsSchemaStore.dispatch(new StreamSchemaActions.GetSchemaFields({
          selectedRowIsEnum: this.selectedRow['isEnum'],
          selectedRowFields: this.schemaFields,
          selectedRowName: this.selectedRow['name'],
        }));
      }

    },
  };

  constructor(
    private appStore: Store<AppState>,
    private streamsStore: Store<fromStreams.FeatureState>,
    private streamsSchemaStore: Store<fromStreamsSchema.FeatureState>,
    private streamDetailsEffects: StreamDetailsEffects,
    private streamPropsStore: Store<fromStreamProps.FeatureState>,
  ) { }

  ngOnInit() {
    this.gridOptions = {
      ...this.gridDefaults,
    };
    this.appStore.dispatch(new StreamPropsActions.ChangeStateProps({
      opened: true,
    }));
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
              filter((tab: TabModel) => tab && tab.schema),
              tap((tab) => {
                this.streamsStore.dispatch(new StreamDetailsActions.GetSchema({streamId: tab.stream}));
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
              if (this.readyApi && this.readyApi.api) {
                // this.readyApi.api.setRowData([]);
              }
            }),
            takeUntil(this.destroy$),
          )
          .subscribe(() => {
          });
      });

  }

  private gridIsReady(readyEvent: GridReadyEvent) {
    this.readyApi = {...readyEvent};

    this.streamDetailsEffects
      .setSchema
      .pipe(
        takeUntil(this.destroy$),
      )
      .subscribe(action => {
        if (!action.payload.schema) {
          return;
        }
        if (action.payload.schemaAll && action.payload.schemaAll.length) {
          this.schemaAll = [...action.payload.schemaAll];
        }

        const props = [
          {
            headerName: 'Use',
            field: 'isTypesData',
            filter: false,
            sortable: false,
            cellRenderer: params => {
              // return `<input disabled type='checkbox' ${params.value ? 'checked' : ''} />`;
              let checkboxSpanDisabled = `<span class="ag-selection-checkbox" unselectable="on"><span class="ag-icon ag-icon-checkbox-checked ag-hidden" unselectable="on"></span><span class="ag-icon ag-icon-checkbox-unchecked" unselectable="on"></span><span class="ag-icon ag-icon-checkbox-indeterminate ag-hidden" unselectable="on"></span></span>`;
              if (params.value) {
                checkboxSpanDisabled = `<span class="ag-selection-checkbox" unselectable="on"><span class="ag-icon ag-icon-checkbox-checked" unselectable="on"></span><span class="ag-icon ag-icon-checkbox-unchecked ag-hidden" unselectable="on"></span><span class="ag-icon ag-icon-checkbox-indeterminate ag-hidden" unselectable="on"></span></span>`;
              }
              return checkboxSpanDisabled;
            },

            pinned: 'left',
          },
          {

            headerName: 'Title',
            field: 'title',
            filter: true,
            sortable: true,
            width: 100,
          },
        ];
        // ...generateGridConfig(action.payload.schemaAll)];

        readyEvent.api.setColumnDefs(null);
        readyEvent.api.setColumnDefs(props);
        const rowData = [...action.payload.schemaAll];
        const types = [...action.payload.schema];
        rowData['isTypesData'] = false;
        for (const item of rowData) {
          if (types.find(type => type.name === item.name)) {
            item['isTypesData'] = true;
          }
        }

        for (const row of rowData) {
          if (row['isEnum']) {
            row['hierarchy'] = ['Enumerators', row.name];
          } else {
            row['hierarchy'] = ['Types', row.name];
          }
        }

        rowData.sort((prev, next) => prev['isEnum'] - next['isEnum']);
        readyEvent.api.setRowData(rowData);
        this.readyApi.columnApi.autoSizeAllColumns();
      });
  }

  getDataPath = function (data) {
    return data.hierarchy;
  };


  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.complete();

  }

}
