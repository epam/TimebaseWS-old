import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { AppState }                                            from '../../../../core/store';
import { select, Store }                                       from '@ngrx/store';

import { Observable, Subject } from 'rxjs';
import { takeUntil, filter, distinctUntilChanged } from 'rxjs/operators';

import { timelineBarState } from '../../store/timeline-bar/timeline-bar.selectors';
import * as fromTimebaseBar from '../../store/timeline-bar/timeline-bar.reducer';
import * as FilterActions from '../../store/filter/filter.actions';

import { HdDate }              from '@deltix/hd-date';
import { getActiveOrFirstTab } from '../../store/streams-tabs/streams-tabs.selectors';
import { TabModel }            from '../../models/tab.model';
import { getStreamGlobalFilters } from '../../store/stream-details/stream-details.selectors';
import { formatHDate } from '../../../../shared/locale.timezone';

@Component({
  selector: 'app-timeline-bar',
  templateUrl: './timeline-bar.component.html',
  styleUrls: ['./timeline-bar.component.scss'],
})
export class TimelineBarComponent implements OnInit, OnDestroy {
  @ViewChild('timebarCursor', {static: true}) timebarCursor: ElementRef;
  public timelineBarState: Observable<fromTimebaseBar.State>;
  public activeTab: Observable<TabModel>;
  private timebarCursorTitle: string;
  public timebarCursorTitleVisible: string;
  private reverse: boolean;
  public cursorTop = -10;
  private destroy$ = new Subject();
  private lastCursorPos: number;
  private lastCursorPosSet: number;
  private filter_date_format = [];
  private filter_time_format = [];
  private filter_timezone = [];
  private dates: {
    start: HdDate,
    end: HdDate,
  };

  constructor(
    private appStore: Store<AppState>,
  ) { }

  ngOnInit() {
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
     /* if (this.tabFilter) {
        this.appStore.dispatch(new StreamsTabsActions.SetFilters({ filter: this.tabFilter }));
      }*/
    }
    ));

    this.timelineBarState = this.appStore.pipe(select(timelineBarState));
    this.timelineBarState
      .pipe(
        takeUntil(this.destroy$),
      )
      .subscribe((state: fromTimebaseBar.State) => {
        const top = state.top.replace('%', '');
        if (Number(top) < 0) {
          state.top = '0%';
        }
        if (Number(top) >  100) {
          state.top = '100%';
        }
        this.lastCursorPosSet = Number(top) / 100;
        if (state.startDate && state.endDate) {
          this.dates = {
            start: state.startDate,
            end: state.endDate,
          };
        }
      });

    this.activeTab = this.appStore
      .pipe(
        select(getActiveOrFirstTab),
      );

    this.appStore
      .pipe(
        select(getActiveOrFirstTab),
        filter(tab => !!tab),
        takeUntil(this.destroy$),
      )
      .subscribe((tab: TabModel) => {
        this.reverse = tab.hasOwnProperty('reverse');
      });
  }

  public onMouseMove(event: MouseEvent) {
    this.lastCursorPos = event.offsetY / (event.currentTarget as HTMLDivElement).offsetHeight;
    if (this.dates) {
      const timestampDiff = (this.dates.end.getEpochMillis() - this.dates.start.getEpochMillis()) * this.lastCursorPos;
      this.timebarCursorTitle = (new HdDate(this.dates.start.getEpochMillis() + timestampDiff)).toISOString();
      this.timebarCursorTitleVisible =  formatHDate(this.timebarCursorTitle, this.filter_date_format, this.filter_time_format, this.filter_timezone);
    }

    this.cursorTop = event.offsetY;
    // if (this.timer) {
    //   clearTimeout(this.timer);
    //   delete this.timer;
    // }
    // this.timer = setTimeout(() => {
    //   const timestamp = this.dates.end.getEpochMillis() * this.lastCursorPos;
    //   this.timebarCursorTitle = (new HdDate(timestamp)).toHdISOString();
    // }, 10);
  }


  onSetCursor() {
    if (this.dates) {
      const timestampDiff = (this.dates.end.getEpochMillis() - this.dates.start.getEpochMillis()) * this.lastCursorPosSet;
      this.timebarCursorTitle = (new HdDate(this.dates.start.getEpochMillis() + timestampDiff)).toISOString();
      this.timebarCursorTitleVisible =  formatHDate(this.timebarCursorTitle, this.filter_date_format, this.filter_time_format, this.filter_timezone);
      this.appStore.dispatch(new FilterActions.AddFilters({
        filter: {
          'from': this.timebarCursorTitle,
        },
      }));
    }

  }


  public onSetDate(event: MouseEvent) {
    event.stopPropagation();
    this.lastCursorPosSet = (this.cursorTop || event.offsetY) / (event.currentTarget as HTMLDivElement).offsetHeight;
    this.lastCursorPos = this.lastCursorPosSet;
    this.onSetCursor();

  }

  timebarPlus() {
    if (this.lastCursorPosSet <= 0.9) {
      this.lastCursorPosSet = this.lastCursorPosSet + 0.1;
    } else {
      this.lastCursorPosSet = 1;
    }
  }

  timebarMinus() {
    if (this.lastCursorPosSet >= 0.1) {
      this.lastCursorPosSet = this.lastCursorPosSet - 0.1;
    } else {
      this.lastCursorPosSet = 0;
    }
  }

  timebarTop() {
    if (!this.reverse) {
      this.timebarMinus();
    } else {
      this.timebarPlus();
    }
    this.onSetCursor();

  }

  timebarBottom() {
    if (!this.reverse) {
      this.timebarPlus();
    } else {
      this.timebarMinus();
    }
    this.onSetCursor();
  }

  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.complete();
  }
}
