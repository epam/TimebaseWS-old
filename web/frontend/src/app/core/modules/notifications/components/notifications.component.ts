import { Component, OnInit }     from '@angular/core';
import { select, Store }         from '@ngrx/store';
import { Observable }            from 'rxjs';
import { NotificationModel }     from '../models/notification.model';
import * as NotificationsActions from '../store/notifications.actions';
import * as fromNotifications    from '../store/notifications.reducer';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.scss'],
})
export class NotificationsComponent implements OnInit {
  public notificationsState: Observable<fromNotifications.State>;
  
  constructor(
    private notificationsStore: Store<fromNotifications.FeatureState>,
  ) { }
  
  ngOnInit() {
    this.notificationsState = this.notificationsStore.pipe(select('notifications'));
  }
  
  onClosed(notification: NotificationModel, index: number) {
    let actionName = '';
    switch (notification.type) {
      case 'alert':
        actionName = 'RemoveAlert';
        break;
      case 'warn':
        actionName = 'RemoveWarn';
        break;
      default:
        actionName = 'RemoveNotification';
        break;
    }
    this.notificationsStore.dispatch(new NotificationsActions[actionName](index));
  }
}
