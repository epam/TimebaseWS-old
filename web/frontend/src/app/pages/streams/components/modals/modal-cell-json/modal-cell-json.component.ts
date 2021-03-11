import { Component, OnDestroy, OnInit } from '@angular/core';
import { BsModalRef }                   from 'ngx-bootstrap/modal';
import { Subject }                      from 'rxjs';

@Component({
  selector: 'app-modal-cell-json',
  templateUrl: './modal-cell-json.component.html',
  styleUrls: ['./modal-cell-json.component.scss'],
})
export class ModalCellJSONComponent implements OnInit, OnDestroy {

  public title: string;
  // public stream: string;
  public closeBtnName: string;
  // public types: any[] = [];
  public data = new Object();

  public onClear: any;
  private destroy$ = new Subject();

  constructor(public bsModalRef: BsModalRef) { }

  ngOnInit() {
  }

  copyClipboardJSON() {
    document.addEventListener('copy', (e: ClipboardEvent) => {
      e.clipboardData.setData('text/plain', (JSON.stringify(this.data)));
      e.preventDefault();
      document.removeEventListener('copy', null);
    });
    document.execCommand('copy');
  }

  clear() {
    this.onClear();
  }

  ngOnDestroy(): void {
    this.destroy$.next(true);
    this.destroy$.complete();
  }

}
