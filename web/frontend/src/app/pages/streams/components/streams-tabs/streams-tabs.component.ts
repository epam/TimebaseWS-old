import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { select, Store } from '@ngrx/store';
import { distinctUntilChanged, filter, map, takeUntil } from 'rxjs/operators';
import { Observable, Subject } from 'rxjs';

import {
  PerfectScrollbarComponent,
  PerfectScrollbarConfigInterface,
  PerfectScrollbarDirective,
} from 'ngx-perfect-scrollbar';

import * as fromTabs from '../../store/streams-tabs/streams-tabs.reducer';
import * as  StreamsTabsActions from '../../store/streams-tabs/streams-tabs.actions';
import { getTabs, getTabsState } from '../../store/streams-tabs/streams-tabs.selectors';

import { StreamModel } from '../../models/stream.model';
import { TabModel } from '../../models/tab.model';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { AppState }                       from '../../../../core/store';
import { appRoute }                       from '../../../../shared/utils/routes.names';

import * as fromStreamProps           from '../../store/stream-props/stream-props.reducer';
import * as StreamPropsActions        from '../../store/stream-props/stream-props.actions';
import * as fromStreams               from '../../store/streams-list/streams.reducer';
import { ModalSettingsComponent }     from '../modals/modal-settings/modal-settings.component';
import { BsModalService, BsModalRef } from 'ngx-bootstrap';
import * as fromStreamDetails         from '../../store/stream-details/stream-details.reducer';
import * as StreamDetailsActions      from '../../store/stream-details/stream-details.actions';
import { getStreamGlobalFilters }     from '../../store/stream-details/stream-details.selectors';
import * as AuthActions               from '../../../../core/store/auth/auth.actions';

@Component({
  selector: 'app-streams-tabs',
  templateUrl: './streams-tabs.component.html',
  styleUrls: ['./streams-tabs.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StreamsTabsComponent implements OnInit, OnDestroy {
  public tabsState: Observable<fromTabs.State>;
  public propsState: Observable<fromStreams.State>;
  public config: PerfectScrollbarConfigInterface = {};
  public openTabsList: boolean;
  public live: boolean;
  @ViewChild(PerfectScrollbarComponent, { static: true }) componentRef?: PerfectScrollbarComponent;
  @ViewChild(PerfectScrollbarDirective, { static: true }) directiveRef?: PerfectScrollbarDirective;
  private destroy$ = new Subject();
  private psXReachEnd: boolean;
  private psXReachStart: boolean;
  private scrollXvalue: number;
  public tabs: TabModel[];
  public openedProps: boolean;
  private bsModalRef: BsModalRef;
  public filteredGlobalSettings: boolean;

  constructor(
    private appStore: Store<AppState>,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private activatedRoute: ActivatedRoute,
    private streamPropsStore: Store<fromStreamProps.FeatureState>,
    private modalService: BsModalService,
    private streamDetailsStore: Store<fromStreamDetails.FeatureState>,
  ) { }

  ngOnInit() {
    this.appStore
      .pipe(
        select(getTabs),
        filter((tabs) => !!tabs),
        takeUntil(this.destroy$),
      )
      .subscribe(tabs => {
        this.tabs = tabs;
        if (this.componentRef && this.componentRef.directiveRef) {
          this.componentRef.directiveRef.update();
        }
      });
    this.streamDetailsStore.dispatch(new StreamDetailsActions.SetGlobalFilterState());

    this.appStore
      .pipe(
        select(getStreamGlobalFilters),
        filter(global_filter => !!global_filter),
        takeUntil(this.destroy$),
        distinctUntilChanged(),
      )
      .subscribe(action => {
        this.filteredGlobalSettings = !!((action.filter_date_format && action.filter_date_format.length) || (action.filter_time_format && action.filter_time_format.length)
          || (action.filter_timezone && action.filter_timezone.length));
        this.cdr.markForCheck();

      });

    this.streamPropsStore.pipe(
      select('streamProps'),
      // select('streamProps'),
      // map((state: fromStreamProps.State) => state.props),
      filter(props => !!props),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    )
      .subscribe(props => {
        this.openedProps = !props['opened'];
        this.cdr.markForCheck();
      });


    this.tabsState = this.appStore.pipe(select(getTabsState));
    this.activatedRoute.queryParams.subscribe(
      (params: Params) => {
        this.live = params.hasOwnProperty('live');
      },
    );
    let lastLength = 0;
    this.tabsState
      .pipe(
        filter(state => !!state.tabs),
        map(state => state.tabs),
        filter(tabs => {
          let pass = false;
          if (lastLength > tabs.length) {
            pass = true;
          }
          lastLength = tabs.length;
          return pass;
        }),
        takeUntil(this.destroy$),
      )
      .subscribe((tabs: TabModel[]) => {
        if (tabs && tabs.length) {
          const tab = tabs.find(tab => tab.active) || tabs[0];
          this.router.navigate([`/${appRoute}`, ...tab.linkArray], { replaceUrl: true });
        } else {
          this.openTabsList = false;
          this.router.navigate([`/${appRoute}`]);
          this.cdr.markForCheck();
        }

      });
  }

  streamsTrack(index: number, item: StreamModel) {
    return item.key; // or item.id
  }

  scrollToX(x: number): void {
    this.componentRef.directiveRef.scrollTo(x, 500);
  }

  onScrollEvent(event: any): void {
    this.scrollXvalue = event.target.scrollLeft;
    this.psXReachStart = false;
    this.psXReachEnd = false;
    if (event.type === 'ps-x-reach-start') {
      this.psXReachStart = true;
    }
    if (event.type === 'ps-x-reach-end') {
      this.psXReachEnd = true;
    }
    if (!event.target.className.includes('ps--active-x')) {
      this.psXReachStart = true;
      this.psXReachEnd = true;
    }
    // this.cdr.detectChanges();
  }

  closeTab(tab: TabModel) {
    this.appStore.dispatch(new StreamsTabsActions.RemoveTab({
      tab: tab,
    }));
  }

  openList() {

  }

  toggleDetails() {
    this.appStore.dispatch(new StreamPropsActions.ChangeStateProps({
      opened: this.openedProps,
    }));
  }

  closeAllTabs() {
    this.openTabsList = false;
    for (const tab of this.tabs) {
      this.appStore.dispatch(new StreamsTabsActions.RemoveTab({
        tab: tab,
      }));
    }
  }

  ngOnDestroy(): void {
    localStorage.setItem('prevActTab', null);
    this.destroy$.next(true);
    this.destroy$.complete();
  }

  openGlobalSettings() {
    const initialState = {
      title: 'Global Settings',
    };

    this.bsModalRef = this.modalService.show(ModalSettingsComponent, { initialState });
    this.bsModalRef.content.onFilter = (data) => {
      this.streamDetailsStore.dispatch(new StreamDetailsActions.SaveGlobalFilterState({ global_filter: data }));
    };
    this.bsModalRef.content.onClear = () => {
      this.filteredGlobalSettings = false;
      this.streamDetailsStore.dispatch(new StreamDetailsActions.ClearGlobalFilterState());
    };
    this.bsModalRef.content.closeBtnName = 'Close';
  }

  public onLogOut() {
    this.appStore.dispatch(new AuthActions.LogOut());
  }
}
