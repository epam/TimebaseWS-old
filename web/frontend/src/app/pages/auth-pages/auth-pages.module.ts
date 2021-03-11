import { NgModule }       from '@angular/core';
import { LoginComponent } from './components/login/login.component';
import { SharedModule }   from '../../shared/shared.module';
import { RouterModule }   from '@angular/router';
import { LoginGuard }     from '../../core/services/guards/login.guard';

@NgModule({
  declarations: [LoginComponent],
  imports: [
    SharedModule,
    RouterModule.forChild([{
      path: '',
      canActivate: [LoginGuard],
      children: [
        {
          path: '',
          pathMatch: 'full',
          redirectTo: 'login',
        },
        {
          path: 'login',
          component: LoginComponent,
        },
      ],
    }]),
  ],
  exports: [
    RouterModule,
  ],
})
export class AuthPagesModule {
}
