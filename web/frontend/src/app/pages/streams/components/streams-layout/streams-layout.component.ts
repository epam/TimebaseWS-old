import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store }                        from '@ngrx/store';
import * as StreamsTabsActions          from '../../store/streams-tabs/streams-tabs.actions';
import { AppState }                     from '../../../../core/store';

@Component({
  selector: 'app-streams-layout',
  templateUrl: './streams-layout.component.html',
  styleUrls: ['./streams-layout.component.scss'],
})
export class StreamsLayoutComponent implements OnInit, OnDestroy {

  constructor(
    private appStore: Store<AppState>,
  ) { }

  ngOnInit() {
    this.appStore.dispatch(new StreamsTabsActions.LoadTabsFromLS());
  }

  ngOnDestroy(): void {
    this.appStore.dispatch(new StreamsTabsActions.StopTabsSync());
  }

}
