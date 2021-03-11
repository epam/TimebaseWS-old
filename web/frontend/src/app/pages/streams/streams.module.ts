import { NgModule }                                     from '@angular/core';
import { ReactiveFormsModule }                          from '@angular/forms';
import { AutocompleteModule }                           from '@deltix/ng-autocomplete';
import { EffectsModule }                                from '@ngrx/effects';
import { StoreModule }                                  from '@ngrx/store';
import { AngularSplitModule }                           from 'angular-split';
import { ModalModule, TimepickerModule, TooltipModule } from 'ngx-bootstrap';
import { ContextMenuModule }                            from 'ngx-contextmenu';
import { MonacoEditorModule }                           from 'ngx-monaco-editor';
import {
  PERFECT_SCROLLBAR_CONFIG,
  PerfectScrollbarConfigInterface,
  PerfectScrollbarModule,
}                                                       from 'ngx-perfect-scrollbar';
import { SharedModule }                                 from '../../shared/shared.module';
import { FiltersPanelComponent }                        from './components/filters-panel/filters-panel.component';
import { ModalCellJSONComponent }                       from './components/modals/modal-cell-json/modal-cell-json.component';
import { ModalDescribeComponent }                       from './components/modals/modal-describe/modal-describe.component';
// import { SmartDateTimePickerModule }                                        from '@deltix/ng-smart-date-time-picker';
import { ModalFilterComponent }                         from './components/modals/modal-filter/modal-filter.component';
import { ModalPurgeComponent }                          from './components/modals/modal-purge/modal-purge.component';
import { ModalRenameComponent }                         from './components/modals/modal-rename/modal-rename.component';
import { FormControlComponent }                         from './components/modals/modal-send-message/form-controls/form-control.component';
import { FormGroupComponent }                           from './components/modals/modal-send-message/form-controls/form-group.component';
import { ModalSendMessageComponent }                    from './components/modals/modal-send-message/modal-send-message.component';
import { ModalSettingsComponent }                       from './components/modals/modal-settings/modal-settings.component';
import { ModalTruncateComponent }                       from './components/modals/modal-truncate/modal-truncate.component';
import { StreamDetailsComponent }                       from './components/stream-details/stream-details.component';
import { StreamQueryComponent }                         from './components/stream-query/stream-query.component';
import { StreamSchemaComponent }                        from './components/stream-schema/stream-schema.component';
import { StreamViewReverseComponent }                   from './components/stream-view-reverse/stream-view-reverse.component';
import { StreamsGridLiveComponent }                     from './components/streams-grid-live/streams-grid-live.component';
import { StreamsLayoutComponent }                       from './components/streams-layout/streams-layout.component';
import { StreamsListComponent }                         from './components/streams-list/streams-list.component';
import { StreamsPropsComponent }                        from './components/streams-props/streams-props.component';
import { StreamsTabsComponent }                         from './components/streams-tabs/streams-tabs.component';
import { TabsRouterProxyComponent }                     from './components/tabs-router-proxy/tabs-router-proxy.component';
import { TimelineBarComponent }                         from './components/timeline-bar/timeline-bar.component';
import { ChartDataService }                             from './services/chart-data.service';
import { SchemaDataService }                            from './services/schema-data.service';
import { SendMessagePopupService }                      from './services/send-message-popup.service';
import { StreamDataService }                            from './services/stream-data.service';
import { reducers }                                     from './store';
import { FilterEffects }                                from './store/filter/filter.effects';
import { StreamDetailsEffects }                         from './store/stream-details/stream-details.effects';
import * as fromStreamDetails                           from './store/stream-details/stream-details.reducer';
import { StreamPropsEffects }                           from './store/stream-props/stream-props.effects';
import * as fromStreamProps                             from './store/stream-props/stream-props.reducer';
import { StreamQueryEffects }                           from './store/stream-query/stream-query.effects';
import * as fromStreamQuery                             from './store/stream-query/stream-query.reducer';
import { StreamsEffects }                               from './store/streams-list/streams.effects';
import * as fromStreams                                 from './store/streams-list/streams.reducer';
import { StreamsTabsEffects }                           from './store/streams-tabs/streams-tabs.effects';
import { TimelineBarEffects }                           from './store/timeline-bar/timeline-bar.effects';
import { StreamsRoutingModule }                         from './streams-routing.module';


const DEFAULT_PERFECT_SCROLLBAR_CONFIG: PerfectScrollbarConfigInterface = {
  suppressScrollX: false,
  suppressScrollY: false,
};

@NgModule({
  declarations: [
    StreamsLayoutComponent,
    StreamsListComponent,
    StreamDetailsComponent,
    StreamsTabsComponent,
    StreamsPropsComponent,
    ModalFilterComponent,
    ModalSettingsComponent,
    ModalCellJSONComponent,
    TimelineBarComponent,
    StreamsGridLiveComponent,
    TabsRouterProxyComponent,
    StreamSchemaComponent,
    StreamQueryComponent,
    FiltersPanelComponent,
    StreamViewReverseComponent,
    ModalTruncateComponent,
    ModalPurgeComponent,
    ModalRenameComponent,
    ModalSendMessageComponent,
    ModalDescribeComponent,
    FormGroupComponent,
    FormControlComponent,
  ],
  imports: [
    SharedModule,
    //  SmartDateTimePickerModule,
    AutocompleteModule,
    ReactiveFormsModule,
    StreamsRoutingModule,
    StoreModule.forFeature('streams-store', reducers),
    StoreModule.forFeature('streams', fromStreams.reducer),
    StoreModule.forFeature('streamDetails', fromStreamDetails.reducer),
    StoreModule.forFeature('streamProps', fromStreamProps.reducer),
    StoreModule.forFeature('streamQuery', fromStreamQuery.reducer),
    EffectsModule.forFeature([StreamsEffects, StreamDetailsEffects, StreamPropsEffects, TimelineBarEffects, FilterEffects, StreamsTabsEffects, StreamQueryEffects]),
    PerfectScrollbarModule,
    AngularSplitModule,
    TimepickerModule.forRoot(),
    ModalModule.forRoot(),
    TooltipModule.forRoot(),
    ContextMenuModule,
    MonacoEditorModule,
  ],
  entryComponents: [
    ModalFilterComponent,
    ModalCellJSONComponent,
    ModalSettingsComponent,
    ModalTruncateComponent,
    ModalPurgeComponent,
    ModalRenameComponent,
    ModalSendMessageComponent,
    ModalDescribeComponent,
  ],
  providers: [
    StreamDataService,
    ChartDataService,
    SchemaDataService,
    {
      provide: PERFECT_SCROLLBAR_CONFIG,
      useValue: DEFAULT_PERFECT_SCROLLBAR_CONFIG,
    },
    SendMessagePopupService,
  ],
})

export class StreamsModule {
}
