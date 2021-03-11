import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { BsModalRef }                                 from 'ngx-bootstrap/modal';
import { Store }                                      from '@ngrx/store';
import { AppState }                                   from '../../../../../core/store';
import { FormBuilder, FormGroup, Validators }         from '@angular/forms';
import { StreamModel }                                from '../../../models/stream.model';
import * as StreamsActions                            from '../../../store/streams-list/streams.actions';

@Component({
  selector: 'app-modal-rename',
  templateUrl: './modal-rename.component.html',
  styleUrls: ['./modal-rename.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalRenameComponent implements OnInit {

  public renameForm: FormGroup;
  public stream: StreamModel;
  public data: {
    stream: StreamModel,
    symbol?: string,
    name?: string,
  };

  constructor(
    public bsModalRef: BsModalRef,
    private appStore: Store<AppState>,
    private fb: FormBuilder,
  ) { }

  ngOnInit(): void {
    this.data.name = this.data.symbol ? this.data.stream.name + ' / ' + this.data.symbol : this.data.stream.name;
    this.renameForm = this.fb.group({
      'newName': [this.data.symbol ? this.data.symbol : this.data.stream.name, Validators.required],
    });
  }

  public onRenameSubmit() {
    if (this.renameForm.invalid || !this.renameForm.get('newName').value) { return; }
    if (!this.data.symbol) {
      this.appStore.dispatch(new StreamsActions.AskToRenameStream({
        streamId: this.data.stream.key,
        streamName: this.renameForm.get('newName').value,
      }));
    } else {
      this.appStore.dispatch(new StreamsActions.AskToRenameSymbol({
        streamId: this.data.stream.key,
        newSymbolName: this.renameForm.get('newName').value,
        oldSymbolName: this.data.symbol,
      }));
    }
    this.bsModalRef.hide();
  }
}
