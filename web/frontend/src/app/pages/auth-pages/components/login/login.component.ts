import { AfterContentChecked, AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';

import { Observable, Subject }            from 'rxjs';
import { AuthProviderModel }              from '../../../../models/auth-provider.model';
import { select, Store }                  from '@ngrx/store';
import { Router }                         from '@angular/router';
import { AppState }                       from '../../../../core/store';
import * as  AuthActions                  from '../../../../core/store/auth/auth.actions';
import { getAuthProvider, getIsLoggedIn } from '../../../../core/store/auth/auth.selectors';
import { filter, take, takeUntil }        from 'rxjs/operators';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent implements AfterContentChecked, OnInit, AfterViewInit, OnDestroy {
  public username: string;
  public password: string;
  @ViewChild('usernameInput', {static: false}) usernameInput: ElementRef;
  private destroy$ = new Subject<any>();
  public authState: Observable<AuthProviderModel>;

  constructor(
    private router: Router,
    private appStore: Store<AppState>,
  ) { }

  ngOnInit(): void {
    this.authState = this.appStore.pipe(
      select(getAuthProvider),
      filter(authProvider => !!authProvider),
    );

    this.authState
      .pipe(
        take(1),
        takeUntil(this.destroy$),
      )
      .subscribe(authProvider => {
        if (!authProvider.custom_provider) {
          const hash = window.location.hash.substr(1).split('&');
          let code: string;
          for (const row of hash) {
            const [key, value] = row.split('=');
            if (key === 'code') {
              code = value;
              break;
            }
          }
          if (!code) {
            this.appStore.dispatch(new AuthActions.RedirectToAuthProvider());
          } else {
            this.appStore.dispatch(new AuthActions.ProcessSingInRedirect());
          }
        }
      });

    this.appStore
      .pipe(
        select(getIsLoggedIn),
        filter((isLoggedIn) => isLoggedIn),
        take(1),
        takeUntil(this.destroy$),
      )
      .subscribe(() => {
        this.router.navigate(['/'], {replaceUrl: true});
      });

    // this.authStore
    //   .select('auth')
    //   .pipe(
    //     filter(authState => authState.logged),
    //     take(1),
    //     takeUntil(this.destroy$),
    //   )
    //   .subscribe(() => {
    //     this.router.navigate(['/']);
    //   });
  }

  ngOnDestroy() {
    this.destroy$.next(true);
    this.destroy$.complete();
  }

  ngAfterViewInit() {
    this.authState
      .pipe(
        take(1),
        takeUntil(this.destroy$),
      )
      .subscribe(authProvider => {
        if (authProvider.custom_provider) {
          this.usernameInput.nativeElement.focus();
        }
      });
  }

  public login() {
    if (this.isValid()) {
      this.appStore.dispatch(new AuthActions.TryLogIn({
        password: this.password,
        username: this.username,
      }));

    }
  }

  public isValid() {
    return this.username != null && this.username.trim().length > 0 && this.password != null && this.password.trim().length;
  }

  ngAfterContentChecked() {
    this.isValid();
  }

}
