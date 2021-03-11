import { Component, OnDestroy, OnInit } from '@angular/core';
import { BsModalRef }                   from 'ngx-bootstrap/modal';
import { select, Store } from '@ngrx/store';
import { AppState }      from '../../../../../core/store';
import { StreamModel }   from '../../../models/stream.model';
import { Subject }                      from 'rxjs';
import * as StreamDetailsActions        from '../../../store/stream-details/stream-details.actions';
import { getStreamRange }               from '../../../store/stream-details/stream-details.selectors';
import { filter, map, take, takeUntil } from 'rxjs/operators';
import { streamsListStateSelector }     from '../../../store/streams-list/streams.selectors';
import * as StreamsActions              from '../../../store/streams-list/streams.actions';
import { StreamsEffects }               from '../../../store/streams-list/streams.effects';

@Component({
  selector: 'app-modal-truncate',
  templateUrl: './modal-truncate.component.html',
  styleUrls: ['./modal-truncate.component.scss'],
})
export class ModalTruncateComponent implements OnInit, OnDestroy {

  private destroy$ = new Subject();
  public stream: StreamModel;
  public startDate: Date;
  public endDate: Date;
  public selectedDate: Date;

  public dropdownSettingsSymbols = {
    idField: 'field',
    textField: 'field',
    allowSearchFilter: true,
    closeDropDownOnSelection: false,
    itemsShowLimit: 10,
  };
  public selectedSymbols = [];
  public symbolsList: {
    field: string,
  }[] = [];

  constructor(
    public bsModalRef: BsModalRef,
    private appStore: Store<AppState>,
    private streamsEffects: StreamsEffects,
  ) { }

  ngOnInit() {

    this.appStore.dispatch(new StreamDetailsActions.GetStreamRange({
      streamId: this.stream.key,
    }));
    this.appStore.dispatch(new StreamsActions.GetSymbols({
      streamKey: this.stream.key,
    }));
    this.appStore
      .pipe(
        select(getStreamRange),
        filter(streamRange => !!streamRange),
        take(1),
        takeUntil(this.destroy$),
      )
      .subscribe((streamRange) => {
        this.startDate = new Date(streamRange.start);
        this.endDate = new Date(streamRange.end);
        this.selectedDate = new Date(streamRange.end);
      });
    this.appStore
      .pipe(
        select(streamsListStateSelector),
        filter(streamsList => {
          const CURRENT_STREAM = streamsList.streams.find(stream => stream.key === this.stream.key);
          return !!(CURRENT_STREAM && CURRENT_STREAM._symbolsList && CURRENT_STREAM._symbolsList.length);
        }),
        map(streamsList => streamsList.streams.find(stream => stream.key === this.stream.key)._symbolsList),
        takeUntil(this.destroy$),
      )
      .subscribe(symbols => this.symbolsList = symbols.map(symbol => { return { field: symbol } }));

    this.streamsEffects.closeModal
      .pipe(
        takeUntil(this.destroy$),
      )
      .subscribe(() => this.bsModalRef.hide());
  }

  public onTruncateSubmit() {
    const params = {
      timestamp: this.selectedDate.getTime(),
    };
    if (this.selectedSymbols && this.selectedSymbols.length) {
      params['symbols'] = this.selectedSymbols.map(symbol => symbol.field);
    }
    this.appStore.dispatch(new StreamsActions.TruncateStream({
      streamKey: this.stream.key,
      params: params,
    }));
  }

  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.complete();
  }
}
