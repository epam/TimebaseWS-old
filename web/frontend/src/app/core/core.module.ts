import { NgModule }                    from '@angular/core';
import { SharedModule }                from '../shared/shared.module';
import { WSService }                   from './services/ws.service';
import { WebsocketService }    from './services/websocket.service';
import { AppInitGuard }                from './services/guards/app-init.guard';
import { NotificationsModule }         from './modules/notifications/notifications.module';
import { StoreModule }                 from '@ngrx/store';
import { metaReducers, reducers }      from './store';
import { EffectsModule }               from '@ngrx/effects';
import { AppEffects }                  from './store/app/app.effects';
import { environment }                 from '../../environments/environment';
import { StoreDevtoolsModule }         from '@ngrx/store-devtools';
import { AuthEffects }                 from './store/auth/auth.effects';
import { InterceptorsModule }          from './services/interceptors/interceptors.module';
import { StoreRouterConnectingModule } from '@ngrx/router-store';
import { AuthGuard }                   from './services/guards/auth.guard';
import { LoginGuard }                  from './services/guards/login.guard';


@NgModule({
  declarations: [],
  imports: [
    SharedModule,
    NotificationsModule,
    InterceptorsModule,

    StoreModule.forRoot(reducers, {metaReducers}),
    EffectsModule.forRoot([AppEffects]),
    EffectsModule.forFeature([AuthEffects]),
    StoreRouterConnectingModule.forRoot(),

    !environment.production ? StoreDevtoolsModule.instrument() : [],
  ],
  providers: [
    WSService,
    WebsocketService,
    AppInitGuard,
    LoginGuard,
    AuthGuard,
  ],
  exports: [
    NotificationsModule,
    StoreModule,
    StoreRouterConnectingModule,
    InterceptorsModule,
    // LoginGuard,
    // AuthGuard,
  ],
})
export class CoreModule {
}
