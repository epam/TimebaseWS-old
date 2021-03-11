import { Injectable }                   from '@angular/core';
import { HttpClient }                   from '@angular/common/http';
import { select, Store }                from '@ngrx/store';
import { filter, map, switchMap, take } from 'rxjs/operators';
import * as fromStreams                 from '../store/streams-list/streams.reducer';
import { TabModel }            from '../models/tab.model';
import { AppState }            from '../../../core/store';
import { getActiveOrFirstTab } from '../store/streams-tabs/streams-tabs.selectors';
import { ChartModel }       from '../models/chart.model';
import { getAppSettings }   from '../../../core/store/app/app.selectors';
import { AppSettingsModel } from '../../../shared/models/app.settings.model';
import { of }                           from 'rxjs';


@Injectable()
export class ChartDataService {

  constructor(
    private httpClient: HttpClient,
    private streamsStore: Store<fromStreams.FeatureState>,
    private appStore: Store<AppState>,
  ) {
  }

  public getNewData(startDate: string, endDate: string, symbols: string[], isTail = false, pointsRate = 1) {
    return this.appStore
      .pipe(
        select(getActiveOrFirstTab),
        take(1),
        switchMap(activeTab => this.appStore
          .select(getAppSettings)
          .pipe(
            filter(settings => !!settings),
            take(1),
            map(settings => [activeTab, settings]),
          )),
        switchMap(([activeTab, settings]: [TabModel, AppSettingsModel]) => {
          const maxPoints = this.getMaxPointsCount(settings, isTail, pointsRate, activeTab.filter['levels']);
          if (maxPoints >= 10) {
            return this.httpClient
              .get<ChartModel[]>(`charting/${encodeURIComponent(activeTab.stream)}`, {
                params: {
                  startTime: startDate,
                  endTime: endDate,
                  symbols: symbols,
                  levels: activeTab.filter['levels'] + '',
                  maxPoints: maxPoints.toString(),
                },
                headers: {
                  customError: 'true',
                },
              });
          } else {
            return of(true);
          }
        }),
      );
  }

  private getMaxPointsCount(settings: AppSettingsModel, isTail, pointsRate: number, levels: number): number {
    let maxPointsCount: number;
    if (isTail) {
      maxPointsCount = (settings.chartMaxPoints - settings.chartMaxVisiblePoints) / 2;
    } else {
      pointsRate = 1;
      maxPointsCount = settings.chartMaxVisiblePoints;
    }

    return Math.round((maxPointsCount / levels) * pointsRate);
  }
}
