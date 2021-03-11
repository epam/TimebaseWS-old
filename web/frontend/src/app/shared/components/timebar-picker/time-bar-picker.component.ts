import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
} from '@angular/core';
import { BsDatepickerConfig, BsDatepickerDirective } from 'ngx-bootstrap';
import { select, Store } from '@ngrx/store';
import { getStreamGlobalFilters } from '../../../pages/streams/store/stream-details/stream-details.selectors';
import { distinctUntilChanged, filter, takeUntil } from 'rxjs/operators';
import { getLocaleDateString } from '../../locale.timezone';
import { AppState }            from '../../../core/store';
import { BsModalRef }          from 'ngx-bootstrap/modal';
import { Subject } from 'rxjs';
import { getTimeZones, getTimeZoneTitle } from '../../utils/timezone.utils';
import { TimeZone } from '../../models/timezone.model';
import { HdDate } from '@deltix/hd-date';
import { formatDate } from '../../../pages/streams/components/stream-details/stream-details.component';

@Component({
  selector: 'app-time-bar-picker',
  templateUrl: './time-bar-picker.component.html',
  styleUrls: ['./time-bar-picker.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TimeBarPickerComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject();

  @Input() selectedDate: Date;
  @Output() selectedDateChange: EventEmitter<Date> = new EventEmitter<Date>();

  @Input() startDate: Date;
  @Input() endDate: Date;
  @Input() method: string;

  public format: string;
  private hd_format: string;
  public visibleSelectedDate: Date;

  public startDate_title: string;
  public endDate_title: string;
  public selectorCursorTitle: string;

  @ViewChild('dp', { static: true }) datepicker: BsDatepickerDirective;
  @ViewChild('timeBarCursor', { static: true }) timeBarCursor: ElementRef;
  @ViewChild('timeBarSelectorCursor', { static: true }) timeBarSelectorCursor: ElementRef;
  @ViewChild('timeBar', { static: true }) timeBar: ElementRef;

  public cursorLeft = 0;
  public selectorCursorLeft = 0;

  public bsConfig: Partial<BsDatepickerConfig> = {
    containerClass: 'theme-default',
  };

  public dropdownListTimeZones: {
    nameTitle: string,
    name: string,
    offset: number,
  }[] = [];
  public dropdownSettingsTimeZone = {
    singleSelection: true,
    idField: 'name',
    textField: 'nameTitle',
    allowSearchFilter: true,
    closeDropDownOnSelection: true,
  };
  public selectedTimeZone = [];
  public timeBarWidth: number;

  private avoidBSPickerTriggering = false;

  constructor(
    private appStore: Store<AppState>,
    public bsModalRef: BsModalRef,
    private cdr: ChangeDetectorRef,
    private _ngZone: NgZone,
  ) { }

  ngOnInit() {
    this.dropdownListTimeZones = getTimeZones().map(item => {
      return { nameTitle: this.getTimeZoneName(item), name: item.name, offset: item.offset };
    });
    if (!this.selectedDate) {
      this.selectedDate = new Date(this.endDate.toISOString());
      this.selectedDateChange.emit(this.selectedDate);
    }

    this.appStore
      .pipe(
        select(getStreamGlobalFilters),
        filter(global_filter => !!global_filter),
        takeUntil(this.destroy$),
        distinctUntilChanged(),
      )
      .subscribe(action => {
        let filter_date_format = getLocaleDateString();
        let filter_time_format = 'HH:mm:ss SSS';

        if (action.filter_date_format && action.filter_date_format.length) {
          filter_date_format = action.filter_date_format[0];
        }
        if (action.filter_time_format && action.filter_time_format.length) {
          filter_time_format = action.filter_time_format[0];
        }
        if (action.filter_timezone && action.filter_timezone.length) {
          this.selectedTimeZone = [action.filter_timezone[0]];
        } else {
          this.selectedTimeZone = [this.dropdownListTimeZones.find(timezone => timezone.name === Intl.DateTimeFormat().resolvedOptions().timeZone)];
        }

        this.format = filter_date_format.toUpperCase() + ' ' + filter_time_format;
        this.hd_format = filter_date_format + ' ' + filter_time_format;

        this.format = this.format.replace('tt', 'A');
        this.format = this.format.replace(/f/g, 'S');

        this.bsConfig = Object.assign({}, {
          containerClass: 'theme-default',
          dateInputFormat: this.format,
          minDate: this.getDateUsingSelectedTZ(this.startDate),
          maxDate: this.getDateUsingSelectedTZ(this.endDate),
        });


        this.startDate_title = formatDate(this.getDateUsingSelectedTZ(this.startDate).toISOString(), this.hd_format);
        this.endDate_title = formatDate(this.getDateUsingSelectedTZ(this.endDate).toISOString(), this.hd_format);

        setTimeout(() => {
          this.datepicker.setConfig();

          if (this.selectedTimeZone) {
            this.visibleSelectedDate = this.getDateUsingSelectedTZ(this.selectedDate);
          }
          this.cdr.detectChanges();
          this.cdr.markForCheck();
        }, 0);

      });

  }

  public onValueChange(newDate: Date) {
    if (this.avoidBSPickerTriggering) return;
    this.selectedDate = this.getDateWithoutSelectedTZ(newDate, this.getSelectedTZOffset());
    this.selectedDateChange.emit(this.selectedDate);
    this.manualUpdateCursorPosition();
  }

  public onTimeZoneSelected() {
    if (this.selectedTimeZone) {
      this.visibleSelectedDate = this.getDateUsingSelectedTZ(this.selectedDate);

      this.bsConfig = Object.assign({}, {
        containerClass: 'theme-default',
        dateInputFormat: this.format,
        minDate: this.getDateUsingSelectedTZ(this.startDate),
        maxDate: this.getDateUsingSelectedTZ(this.endDate),
      });

      this.startDate_title = formatDate(this.getDateUsingSelectedTZ(this.startDate).toISOString(), this.hd_format);
      this.endDate_title = formatDate(this.getDateUsingSelectedTZ(this.endDate).toISOString(), this.hd_format);

      this.datepicker.setConfig();
    }
  }

  public onTimeZoneDeSelected() {
    const tzName = Intl.DateTimeFormat().resolvedOptions().timeZone;
    this.selectedTimeZone = [this.dropdownListTimeZones.find(timezone => timezone.name === tzName)];
    this.onTimeZoneSelected();
  }

  private getTimeZoneName(item: TimeZone) {
    return getTimeZoneTitle(item);
  }

  private getSelectedTZOffset(): number {
    return this.dropdownListTimeZones.find(timezone => timezone.name === this.selectedTimeZone[0].name).offset;
  }

  private getDateUsingSelectedTZ(date: Date) {
    if (this.selectedTimeZone && this.selectedTimeZone[0] && date) {

      const localOffset = -(new HdDate()).getTimezoneOffset(),
        selectedOffset = this.getSelectedTZOffset(),
        newDate = new HdDate(date.toISOString());

      newDate.setMilliseconds(newDate.getMilliseconds() - (localOffset - selectedOffset) * 60 * 1000);

      return new Date(newDate.getEpochMillis());
    }
  }

  private getDateWithoutSelectedTZ(date: Date, offset: number): Date {
    const newDate = new Date(date.getTime()),
      localOffset = -(new HdDate()).getTimezoneOffset();
    newDate.setMilliseconds(newDate.getMilliseconds() - (offset - localOffset) * 60 * 1000);
    return newDate;
  }

  private manualUpdateCursorPosition() {
    const TIME_BAR_WIDTH = this.timeBar.nativeElement.offsetWidth,
      STREAM_TIME_RANGE = this.endDate.getTime() - this.startDate.getTime();
    this.timeBarWidth = TIME_BAR_WIDTH;
    this.cursorLeft = TIME_BAR_WIDTH / (STREAM_TIME_RANGE / (this.selectedDate.getTime() - this.startDate.getTime()));

    // If case startdate === enddate
    if (isNaN(this.cursorLeft)) {
      this.cursorLeft = this.selectorCursorLeft;
    }

  /*  if (this.cursorLeft > 15) {
      this.cursorLeft = this.cursorLeft - 15;
    }*/
  }

  public onSetDate(event: MouseEvent) {
    event.stopImmediatePropagation();
    this.cursorLeft = event.offsetX;
  /*  if (this.cursorLeft > 15) {
      this.cursorLeft = this.cursorLeft - 15;
    }*/
    const TIME_BAR_WIDTH = this.timeBar.nativeElement.offsetWidth,
      STREAM_TIME_RANGE = this.endDate.getTime() - this.startDate.getTime();
    this.selectedDate = new Date(this.startDate.getTime() + STREAM_TIME_RANGE * event.offsetX / TIME_BAR_WIDTH);
    this.selectedDateChange.emit(this.selectedDate);

    this.visibleSelectedDate = this.getDateUsingSelectedTZ(this.selectedDate);
  }

  public onMouseMove(event: MouseEvent) {
    this.selectorCursorLeft = event.offsetX;
   /* if (this.selectorCursorLeft > 15) {
      this.selectorCursorLeft = this.selectorCursorLeft - 15;
    }*/
    const TIME_BAR_WIDTH = this.timeBar.nativeElement.offsetWidth,
      STREAM_TIME_RANGE = this.endDate.getTime() - this.startDate.getTime();
    this.selectorCursorTitle = formatDate(this.getDateUsingSelectedTZ(new Date(this.startDate.getTime() + STREAM_TIME_RANGE * event.offsetX / TIME_BAR_WIDTH)).toISOString(), this.hd_format);
  }

  public ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.complete();
  }

}
