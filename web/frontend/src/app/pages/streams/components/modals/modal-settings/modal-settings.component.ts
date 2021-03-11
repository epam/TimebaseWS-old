import { Component, OnDestroy, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { select, Store } from '@ngrx/store';
import { takeUntil } from 'rxjs/operators';
import { Subject }                                    from 'rxjs';
import { dateFormatsSupported, timeFormatsSupported } from '../../../../../shared/locale.timezone';
import { AppState }                                   from '../../../../../core/store';
import * as fromStreamDetails                         from '../../../store/stream-details/stream-details.reducer';
import { getTimeZoneTitle, getTimeZones }             from '../../../../../shared/utils/timezone.utils';
import { TimeZone }                                   from '../../../../../shared/models/timezone.model';

@Component({
  selector: 'app-modal-settings',
  templateUrl: './modal-settings.component.html',
  styleUrls: ['./modal-settings.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalSettingsComponent implements OnInit, OnDestroy {

  public title: string;
  public stream: string;
  public closeBtnName: string;
  public dropdownListDateFormats = [];
  public selectedDateFormat = [];
  public dropdownSettingsDateFormat = {};
  public dropdownListTimeFormats = [];
  public selectedTimeFormat = [];
  public dropdownSettingsTimeFormat = {};
  public dropdownListTimeZones = [];
  public selectedTimeZone = [];
  public dropdownSettingsTimeZone = {};
  public onFilter: any;
  public onClear: any;
  private destroy$ = new Subject();


  constructor(public bsModalRef: BsModalRef,
    ///  private streamDetailsStore: Store<fromStreamDetails.FeatureState>,
    private appStore: Store<AppState>,
    // private modalService: BsModalService,
    //  private streamDetailsEffects: StreamDetailsEffects,
    private streamDetailsStore: Store<fromStreamDetails.FeatureState>,
  ) { }

  ngOnInit() {
    this.dropdownListDateFormats = [...dateFormatsSupported].map(item => {
      return { name: item };
    });
    this.dropdownListTimeFormats = [...timeFormatsSupported].map(item => {
      return { name: item };
    });
    this.dropdownListTimeZones = getTimeZones().map(item => {
      return { nameTitle: this.getTimeZoneName(item), name: item.name, offset: item.offset };
    });
    this.dropdownSettingsDateFormat = {
      singleSelection: true,
      idField: 'name',
      textField: 'name',
    };

    this.dropdownSettingsTimeFormat = {
      singleSelection: true,
      idField: 'name',
      textField: 'name',
    };

    this.dropdownSettingsTimeZone = {
      singleSelection: true,
      idField: 'name',
      textField: 'nameTitle',
      allowSearchFilter: true,
    };

    /*
   this.appStore
   .pipe(
     select(getActiveTabFilters),
     filter((filter) => !!filter),
     takeUntil(this.destroy$),
   )
   .subscribe((filter: FilterModel) => {
     if (filter.filter_date_format && filter.filter_date_format.length) {
       this.selectedDateFormat = filter.filter_date_format.map(item => {
         return { name: item };
       });
     } else {
       this.selectedDateFormat = [];
     }
     if (filter.filter_time_format && filter.filter_time_format.length) {
       this.selectedTimeFormat = filter.filter_time_format.map(item => {
         return { name: item };
       });
     } else {
       this.selectedTimeFormat = [];
     }
   });
 */
    this.streamDetailsStore
      .pipe(
        select('streamDetails'),
        // filter((state: fromStreamDetails.State) => !!state.filter),
        takeUntil(this.destroy$),
      )
      .subscribe((state => {
        if (state.global_filter && state.global_filter.filter_date_format && state.global_filter.filter_date_format.length) {
          this.selectedDateFormat = state.global_filter.filter_date_format.map(item => {
            return { name: item };
          });
        } else {
          this.selectedDateFormat = [];
        }
        if (state.global_filter && state.global_filter.filter_time_format && state.global_filter.filter_time_format.length) {
          this.selectedTimeFormat = state.global_filter.filter_time_format.map(item => {
            return { name: item };
          });
        } else {
          this.selectedTimeFormat = [];
        }
        if (state.global_filter && state.global_filter.filter_timezone && state.global_filter.filter_timezone.length) {
          this.selectedTimeZone = state.global_filter.filter_timezone;
        } else {
          this.selectedTimeZone = [this.dropdownListTimeZones.find(timezone => timezone.name === Intl.DateTimeFormat().resolvedOptions().timeZone)];
        }
      }
      ));

  }

  getTimeZoneName(item: TimeZone) {
    return getTimeZoneTitle(item);
  }

  globalSettingsFilter() {
    let filter_date_format = [];
    let filter_time_format = [];
    let filter_timezone = [];
    if (this.selectedDateFormat && this.selectedDateFormat.length) {
      filter_date_format = [... this.selectedDateFormat.map(item => {
        return item['name'];
      })];
    }
    if (this.selectedTimeFormat && this.selectedTimeFormat.length) {
      filter_time_format = [... this.selectedTimeFormat.map(item => {
        return item['name'];
      })];
    }
    if (this.selectedTimeZone && this.selectedTimeZone.length) {
    /*  filter_timezone = [... this.selectedTimeZone.map(item => {
        return item['name'];
      })];*/
      filter_timezone = this.selectedTimeZone;
    }
    this.onFilter({
      filter_date_format: filter_date_format,
      filter_time_format: filter_time_format,
      filter_timezone: filter_timezone,
    });

  }

  clear() {
    this.selectedDateFormat = [];
    this.selectedTimeFormat = [];
    this.onClear();
  }

  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.complete();
  }

}
