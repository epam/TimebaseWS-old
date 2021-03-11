import { CommonModule }                from '@angular/common';
import { NgModule }                    from '@angular/core';
import { FormsModule }                 from '@angular/forms';
import { TranslateModule }             from '@ngx-translate/core';
import { AgGridModule }                from 'ag-grid-angular';
import 'ag-grid-enterprise';
import { NgMultiSelectDropDownModule } from 'ng-multiselect-dropdown';
import { BsDatepickerModule }          from 'ngx-bootstrap';
import { NgxJsonViewerModule }         from 'ngx-json-viewer';
import '../../ag-grid.license';
import { TimeBarPickerComponent }      from './components/timebar-picker/time-bar-picker.component';

@NgModule({
  declarations: [TimeBarPickerComponent],
  imports: [
    CommonModule,
    TranslateModule,
    FormsModule,
    AgGridModule.withComponents([]),
    NgxJsonViewerModule,
    BsDatepickerModule.forRoot(),
    NgMultiSelectDropDownModule.forRoot(),
  ],
  exports: [
    CommonModule,
    FormsModule,
    AgGridModule,
    TranslateModule,
    NgxJsonViewerModule,
    BsDatepickerModule,
    NgMultiSelectDropDownModule,
    TimeBarPickerComponent,
    // MonacoEditorModule,
  ],

})
export class SharedModule {
}
