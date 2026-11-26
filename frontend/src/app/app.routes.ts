import { Routes } from '@angular/router';
import { ApplicationPage } from './pages/application/application-page';
import { ApplicationConfirmationPage } from './pages/application-confirmation/application-confirmation-page';

export const routes: Routes = [
  { path: '', component: ApplicationPage },
  { path: 'application', component: ApplicationPage },
  { path: 'application/confirmation', component: ApplicationConfirmationPage },
];
