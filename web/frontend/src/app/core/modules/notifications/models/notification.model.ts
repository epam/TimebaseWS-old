export class NotificationModel {
  public type: string;
  public message: string;
  public closeInterval?: number;
  public dismissible: boolean;
  
  constructor({...notifications}) {
    this.type = notifications['type'] || 'info';
    this.dismissible = notifications['dismissible'] || false;
    this.message = notifications['message'];
    if (notifications['closeInterval']) this.closeInterval = notifications['closeInterval'];
  }
}
