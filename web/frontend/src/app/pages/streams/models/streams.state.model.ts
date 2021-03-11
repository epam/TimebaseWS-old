export interface StreamsStateModel {
  messageType: string;
  id: number;
  added: string[];
  deleted: string[];
  changed: string[];
  renamed?: { oldName: string, newName: string }[];
}
