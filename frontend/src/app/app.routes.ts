import { Routes } from '@angular/router';
import { ApplicationPage } from './pages/application/application-page';
import { ApplicationConfirmationPage } from './pages/application-confirmation/application-confirmation-page';

export const routes: Routes = [
  { path: '',
    component: ApplicationPage,
    title: 'Prijava za posao | Manage IT Out' },
  { 
    path: 'application',
    component: ApplicationPage,
    title: 'Prijava za posao | Manage IT Out' },
  {
    path: 'application/confirmation',
    component: ApplicationConfirmationPage,
    title: 'Potvrda prijave',
  },
];
