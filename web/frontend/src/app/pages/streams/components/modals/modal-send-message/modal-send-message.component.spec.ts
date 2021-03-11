import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalSendMessageComponent } from './modal-send-message.component';

describe('ModalSendMessageComponent', () => {
  let component: ModalSendMessageComponent;
  let fixture: ComponentFixture<ModalSendMessageComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
        declarations: [ModalSendMessageComponent],
      })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ModalSendMessageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
