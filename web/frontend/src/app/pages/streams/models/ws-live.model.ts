export class WSLiveModel {
    messageType?: string;
    fromTimestamp: string;
    symbols?: string[];
    types?: string[];

    constructor(obj: WSLiveModel | {}) {
        Object.assign(this, obj);
    }

}
