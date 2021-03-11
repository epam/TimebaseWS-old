import { AfterViewInit, Component, OnDestroy, OnInit } from '@angular/core';
import { FormGroup }                                   from '@angular/forms';
import { Observable }                                  from 'rxjs';
import { take }                                        from 'rxjs/operators';
import { StreamDescribeModel }                         from '../../../models/stream.describe.model';
import { StreamModel }                                 from '../../../models/stream.model';
import { BsModalRef }                                  from 'ngx-bootstrap/modal';
import { select, Store }                               from '@ngrx/store';
import { AppState }                                    from '../../../../../core/store';
import * as StreamsActions                             from '../../../store/streams-list/streams.actions';
import { getLastStreamDescribe }                       from '../../../store/streams-list/streams.selectors';

@Component({
  selector: 'app-modal-describe',
  templateUrl: './modal-describe.component.html',
  styleUrls: ['./modal-describe.component.scss'],
})
export class ModalDescribeComponent implements OnInit, AfterViewInit, OnDestroy {

  public renameForm: FormGroup;
  public stream: StreamModel;
  public config = {
    useBothWheelAxes: true,
  };
  public describe: Observable<StreamDescribeModel>;

  constructor(
    public bsModalRef: BsModalRef,
    private appStore: Store<AppState>,
  ) { }

  ngOnInit() {
    this.describe = this.appStore.pipe(select(getLastStreamDescribe));
  }

  ngAfterViewInit(): void {
    this.appStore.dispatch(new StreamsActions.GetStreamDescribe({streamId: this.stream.key}));
  }

  onCopy() {
    this.describe
      .pipe(take(1))
      .subscribe(describe => {

        if (typeof window.navigator.clipboard === 'undefined') {
          const textArea = window.document.createElement('textarea');
          textArea.value = describe.ddl;
          textArea.style.position = 'fixed';  // avoid scrolling to bottom
          window.document.body.appendChild(textArea);
          textArea.focus();
          textArea.select();

          try {
            window.document.execCommand('copy');
            this.bsModalRef.hide();
          } catch (err) {
            console.error('Was not possible to copy te text: ', err);
          }
          document.body.removeChild(textArea);

          return;
          // @ts-ignore
        } else if (window.navigator.permissions) {
          // @ts-ignore
          window.navigator.permissions.query({name: 'clipboard-write'}).then(result => {
            if (result.state === 'granted' || result.state === 'prompt') {
              window.navigator.clipboard.writeText(describe.ddl)
                .then(() => {
                  this.bsModalRef.hide();
                });
            }
          });
        }
      });
  }

  ngOnDestroy(): void {
    this.appStore.dispatch(new StreamsActions.SetStreamDescribe({describe: null}));
  }
}
