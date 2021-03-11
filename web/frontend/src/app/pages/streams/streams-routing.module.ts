import { NgModule }                                   from '@angular/core';
import { RouterModule, Routes }                       from '@angular/router';
import { appRoute, streamRouteName, symbolRouteName } from '../../shared/utils/routes.names';
import { StreamDetailsComponent }                     from './components/stream-details/stream-details.component';
import { StreamsLayoutComponent }                     from './components/streams-layout/streams-layout.component';
import { TabsRouterProxyComponent }                   from './components/tabs-router-proxy/tabs-router-proxy.component';


const routes: Routes = [
  {
    path: '',
    component: StreamsLayoutComponent,
    children: [
      {
        path: streamRouteName,
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: appRoute,
          },

          /* proxy */
          {
            path: 'live/:stream',
            component: TabsRouterProxyComponent,
            data: {
              live: true,
            },
          },
          {
            path: 'reverse/:stream',
            component: TabsRouterProxyComponent,
            data: {
              reverse: true,
            },
          },
          {
            path: 'view/:stream',
            component: TabsRouterProxyComponent,
            data: {
              view: true,
            },
          },
          {
            path: 'chart/:stream',
            component: TabsRouterProxyComponent,
            data: {
              chart: true,
            },
          },
          {
            path: 'schema/:stream',
            component: TabsRouterProxyComponent,
            data: {
              schema: true,
            },
          },
          {
            path: 'query/:stream',
            component: TabsRouterProxyComponent,
            data: {
              query: true,
            },
          },

          /* components */
          {
            path: 'live/:stream/:id',
            component: StreamDetailsComponent,
            data: {
              live: true,
            },
          },
          {
            path: 'reverse/:stream/:id',
            component: StreamDetailsComponent,
            data: {
              reverse: true,
            },
          },
          {
            path: 'view/:stream/:id',
            component: StreamDetailsComponent,
            data: {
              view: true,
            },
          },
          {
            path: 'schema/:stream/:id',
            component: StreamDetailsComponent,
            data: {
              schema: true,
            },
          },
          {
            path: 'query/:stream/:id',
            component: StreamDetailsComponent,
            data: {
              query: true,
            },
          },

          /* common */
          {
            path: ':stream',
            pathMatch: 'full',
            component: TabsRouterProxyComponent,
          },
          {
            path: ':stream/:id',
            component: StreamDetailsComponent,
          },
          // {
          //   path: 'schema/:stream/:id',
          //   component: StreamDetailsComponent,
          //   data: {
          //     schema: true,
          //   },
          // },
          // {
          //   path: ':stream',
          //   pathMatch: 'full',
          //   component: TabsRouterProxyComponent,
          // },
          // {
          //   path: ':stream/:id',
          //   component: StreamDetailsComponent,
          // },
        ],
      },
      {
        path: symbolRouteName,
        children: [
          {
            path: '',
            pathMatch: 'full',
            redirectTo: appRoute,
          },


          /* proxy */
          {
            path: 'live/:stream/:symbol',
            component: TabsRouterProxyComponent,
            data: {
              live: true,
            },
          },
          {
            path: 'reverse/:stream/:symbol',
            component: TabsRouterProxyComponent,
            data: {
              reverse: true,
            },
          },
          {
            path: 'chart/:stream/:symbol',
            component: TabsRouterProxyComponent,
            data: {
              chart: true,
            },
          },
          {
            path: 'view/:stream/:symbol',
            component: TabsRouterProxyComponent,
            data: {
              view: true,
            },
          },
          {
            path: 'query/:stream/:symbol',
            component: TabsRouterProxyComponent,
            data: {
              query: true,
            },
          },

          /* components */
          {
            path: 'live/:stream/:symbol/:id',
            component: StreamDetailsComponent,
            data: {
              live: true,
            },
          },
          {
            path: 'reverse/:stream/:symbol/:id',
            component: StreamDetailsComponent,
            data: {
              reverse: true,
            },
          },
          {
            path: 'schema/:stream/:symbol/:id',
            component: StreamDetailsComponent,
            data: {
              schema: true,
            },
          },
          {
            path: 'view/:stream/:symbol/:id',
            component: StreamDetailsComponent,
            data: {
              view: true,
            },
          },
          {
            path: 'query/:stream/:symbol/:id',
            component: StreamDetailsComponent,
            data: {
              query: true,
            },
          },

          /* common */
          {
            path: ':stream/:symbol',
            pathMatch: 'full',
            component: TabsRouterProxyComponent,
          },
          {
            path: ':stream/:symbol/:id',
            component: StreamDetailsComponent,
          },
        ],
      },
    ],
  },

];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class StreamsRoutingModule {
}
