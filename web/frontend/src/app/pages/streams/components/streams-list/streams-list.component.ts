import { AfterViewInit, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { FormBuilder, FormControl, FormGroup }                    from '@angular/forms';
import { Router }                                   from '@angular/router';
import { select, Store }                            from '@ngrx/store';
import { TranslateService }                         from '@ngx-translate/core';
import { BsModalRef, BsModalService }               from 'ngx-bootstrap';
import { ContextMenuComponent, ContextMenuService } from 'ngx-contextmenu';
import { PerfectScrollbarConfigInterface }          from 'ngx-perfect-scrollbar';
import { Observable, Subject }                      from 'rxjs';
import { map, take, takeUntil }                     from 'rxjs/operators';
import { AppState }                                 from '../../../../core/store';
import { appRoute }                                 from '../../../../shared/utils/routes.names';
import { StreamModel }                              from '../../models/stream.model';
import { SymbolModel }                              from '../../models/symbol.model';

import * as StreamsActions           from '../../store/streams-list/streams.actions';
import * as fromStreams              from '../../store/streams-list/streams.reducer';
import { getActiveOrFirstTab }       from '../../store/streams-tabs/streams-tabs.selectors';
import { ModalDescribeComponent }    from '../modals/modal-describe/modal-describe.component';
import { ModalPurgeComponent }       from '../modals/modal-purge/modal-purge.component';
import { ModalRenameComponent }      from '../modals/modal-rename/modal-rename.component';
import { ModalSendMessageComponent } from '../modals/modal-send-message/modal-send-message.component';
import { ModalTruncateComponent }    from '../modals/modal-truncate/modal-truncate.component';

@Component({
  selector: 'app-streams-list',
  templateUrl: './streams-list.component.html',
  styleUrls: ['./streams-list.component.scss'],
  // changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StreamsListComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild(ContextMenuComponent, {static: true}) public basicMenu: ContextMenuComponent;
  @ViewChild('modalTemplate', {static: true}) modalTemplate;
  public deleteModalRef: BsModalRef;
  public deleteModalData: { stream: StreamModel };

  public appRoute = appRoute;
  public menusmall = false;
  public streams: StreamModel[];
  public showClear: boolean;
  private expandArray = [];
  public openedSymbolsListStream: StreamModel;
  public streamsState: Observable<fromStreams.State>;
  public loader: boolean;
  public loaderInit: boolean;
  public config: PerfectScrollbarConfigInterface = {};
  private destroy$ = new Subject<any>();
  public searchForm: FormGroup;
  public openNewTabForm: FormGroup;
  private bsModalRef: BsModalRef;
  private activeTabType: string;

  constructor(
    private streamsStore: Store<fromStreams.FeatureState>,
    private contextMenuService: ContextMenuService,
    private modalService: BsModalService,
    private fb: FormBuilder,
    private translate: TranslateService,
    private appStore: Store<AppState>,
    private router: Router,
  ) {
  }

  ngOnInit() {
    this.menusmall = !JSON.parse(localStorage.getItem('toggleMenu'));
    this.toggleMenu();
    this.streamsStore.dispatch(new StreamsActions.GetStreams({}));
    this.streamsStore.dispatch(new StreamsActions.AddStreamStatesSubscription());
    this.streamsState = this.streamsStore.pipe(select('streams'));
    this.loaderInit = true;
    this.appStore
      .pipe(
        select(getActiveOrFirstTab),
        // filter((activeTab: TabModel) => !!(activeTab && activeTab.type)),
        takeUntil(this.destroy$),
      )
      .subscribe(activeTab => {
        if (activeTab && activeTab.type) {
          this.activeTabType = activeTab.type;
        } else {
          this.activeTabType = null;
        }
      });

    this.streamsState.pipe(
      map(data => data.streams),
      takeUntil(this.destroy$),
    ).subscribe((streams) => {
      if (!this.searchForm) {
        this.searchForm = this.fb.group({
          'search': new FormControl(),
        });
      }

      if (!this.openNewTabForm) {
        this.openNewTabForm = this.fb.group({
          'openNewTab': new FormControl(),
        });
      }


      if (streams && streams.length) {
        this.loaderInit = false;
        this.streams = [...streams];
        if (this.streams.find(stream => stream._shown)) {
          this.loader = false;
        }
        this.expandArray = this.streams.filter(stream => stream._shown);
      } else if (streams && !streams.length) {
        this.loaderInit = false;
        this.loader = false;
      }
    });
    const openNewTab = JSON.parse(localStorage.getItem('openNewTab'));
    if (openNewTab) {
      this.openNewTabForm.get('openNewTab').setValue(openNewTab);
      this.streamsStore.dispatch(new StreamsActions.SetNavigationState({_openNewTab: openNewTab}));
    }
  }

  getChilds(event: MouseEvent, stream: StreamModel) {
    event.preventDefault();
    event.stopImmediatePropagation();
    event.stopPropagation();
    this.onCloseContextMenu();

    if (!(stream._symbolsList && stream._symbolsList.length)) {
      const searchInputVal = this.searchForm.get('search').value;
      if (searchInputVal && searchInputVal.length) {
        this.streamsStore.dispatch(new StreamsActions.ShowStreamSymbols({
          stream: stream,
          props: {_filter: searchInputVal},
        }));
      } else {
        this.streamsStore.dispatch(new StreamsActions.ShowStreamSymbols({stream: stream}));
      }
      this.loader = true;
    } else {
      this.streamsStore.dispatch(new StreamsActions.SetStreamState({
        stream: stream,
        props: {
          _shown: !stream._shown,
        },
      }));
    }
    this.expandArray = [];
  }

  toggleMenu() {
    this.menusmall = !this.menusmall;
    localStorage.setItem('toggleMenu', JSON.stringify(this.menusmall));
    const body = document.getElementsByTagName('body')[0];
    if (this.menusmall) {
      body.classList.add('body-menu-small');
    } else {
      body.classList.remove('body-menu-small');
    }
  }

  collapseAll() {
    if (this.expandArray && this.expandArray.length) {
      for (const stream of this.expandArray) {
        stream._shown = false;
      }
    }
  }

  streamsTrack(index: number, item: StreamModel) {
    return item.key; // or item.id
  }

  symbolsTrack(index, item) {
    return item; // or item.id
  }

  onShowContextMenu($event, streamProps: {
    stream: StreamModel,
    symbol?: SymbolModel,
  }) {
    this.contextMenuService.show.next({
      // Optional - if unspecified, all context menu components will open
      contextMenu: this.basicMenu,
      event: $event,
      item: streamProps,
    });

    $event.preventDefault();
    $event.stopImmediatePropagation();
    $event.stopPropagation();
  }

  onCloseContextMenu() {
    this.contextMenuService.closeAllContextMenus({
      eventType: 'cancel',
    });
  }

  changeOpenNewTab() {
    const openNewTab = this.openNewTabForm.get('openNewTab').value;
    this.streamsStore.dispatch(new StreamsActions.SetNavigationState({_openNewTab: openNewTab}));
    localStorage.setItem('openNewTab', JSON.stringify(openNewTab));
  }

  onSearch(event: any) {
    if (event.keyCode === 13) {
      this.getStreamsSearch();
    }
  }

  getStreamsSearch() {
    this.streamsStore.dispatch(new StreamsActions.GetStreams({props: {_filter: this.searchForm.get('search').value}}));
  }

  onChangeSearch() {
    this.showClear = !!this.searchForm.get('search').value.length;
  }

  onClearSearch() {
    this.searchForm.get('search').setValue('');
    this.showClear = !!this.searchForm.get('search').value.length;
    this.streamsStore.dispatch(new StreamsActions.GetStreams({}));
  }

  public onShowTruncateModal(item: { stream: StreamModel }) {
    if (!(item && item.stream && item.stream.key)) return;

    this.onCloseContextMenu();
    this.translate.get('titles')
      .pipe(
        take(1),
        takeUntil(this.destroy$),
      )
      .subscribe((/*messages*/) => {
        const initialState = {
          stream: item.stream,
        };

        this.bsModalRef = this.modalService.show(ModalTruncateComponent, {
          initialState: initialState,
          ignoreBackdropClick: true,
        });

      });

  }

  public onShowPurgeModal(item: { stream: StreamModel }) {
    if (!(item && item.stream && item.stream.key)) return;

    const initialState = {
      stream: item.stream,
    };
    this.onCloseContextMenu();
    this.bsModalRef = this.modalService.show(ModalPurgeComponent, {
      initialState: initialState,
      ignoreBackdropClick: true,
    });

  }

  public onShowSendMessage(item: { stream: StreamModel }) {
    if (!(item && item.stream && item.stream.key)) return;

    const initialState = {
      stream: item.stream,
    };
    this.onCloseContextMenu();
    this.bsModalRef = this.modalService.show(ModalSendMessageComponent, {
      initialState: initialState,
      ignoreBackdropClick: true,
      class: 'modal-message',
    });

  }

  public onExportQSMSGFile(item: { stream: StreamModel }) {
    if (!(item && item.stream && item.stream.key)) return;
    this.onCloseContextMenu();
    this.appStore.dispatch(new StreamsActions.DownloadQSMSGFile({streamId: item.stream.key}));
  }

  public onAskToDeleteStream(item: { stream: StreamModel }) {
    if (!(item && item.stream && item.stream.key)) return;
    this.onCloseContextMenu();

    this.deleteModalData = item;
    this.deleteModalRef = this.modalService.show(this.modalTemplate, {
      class: 'modal-small',
    });
  }

  public onShowEditNameModal(item: { stream: StreamModel, symbol?: string }) {
    if (!(item && item.stream && item.stream.key)) return;
    this.onCloseContextMenu();

    const initialState = {
      data: item,
    };

    this.deleteModalData = item;
    this.deleteModalRef = this.modalService.show(ModalRenameComponent, {
      // class: 'bg-dark modal-small',
      initialState: initialState,
      ignoreBackdropClick: true,
    });
  }

  public onShowDescribe(item: { stream: StreamModel }) {
    if (!(item && item.stream && item.stream.key)) return;
    this.onCloseContextMenu();

    const initialState = {
      stream: item.stream,
    };

    this.deleteModalData = item;
    this.deleteModalRef = this.modalService.show(ModalDescribeComponent, {
      // class: 'bg-dark modal-small',
      initialState: initialState,
      ignoreBackdropClick: true,
    });
  }

  public onDeleteStream(stream: StreamModel) {
    if (!(stream && stream.key)) return;
    this.appStore.dispatch(new StreamsActions.AskToDeleteStream({streamKey: stream.key}));
    this.deleteModalRef.hide();
  }

  public onCheckNavigationOptions(event: MouseEvent, streamKey: string, symbol?: string) {
    event.preventDefault();
    // event.stopImmediatePropagation();
    // event.stopPropagation();
    if ((symbol && this.activeTabType === 'schema') ||
      (!symbol && this.activeTabType === 'chart')) {
      return;
    }
    if (this.activeTabType &&
      !this.openNewTabForm.get('openNewTab').value &&
      this.activeTabType !== 'query'
    ) {
      let route: string[] = [appRoute];
      route.push(symbol ? 'symbol' : 'stream');
      route = [...route, this.activeTabType, streamKey];
      if (symbol) route.push(symbol);

      this.router.navigate(route);
    } else {
      this.router.navigate(!symbol ? [appRoute, 'stream', streamKey] : [appRoute, 'symbol', streamKey, symbol]);
    }
  }

  ngOnDestroy(): void {
    this.streamsStore.dispatch(new StreamsActions.StopStreamStatesSubscription());
    this.destroy$.next(true);
    this.destroy$.complete();
    // document.removeEventListener('click', this.onCloseContextMenu.bind(this));
  }

  ngAfterViewInit(): void {
    // document.addEventListener('click', this.onCloseContextMenu.bind(this));
  }

  /*navigate(type: string, streamKey: string, symbol?: string) {
   if (symbol) {
   this.router.navigate([appRoute, type, streamKey, symbol]);
   } else {
   this.router.navigate([appRoute, type, streamKey]);
   }
   }*/
}

