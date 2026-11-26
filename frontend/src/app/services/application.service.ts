import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { Observable } from 'rxjs';
import type {
  ApplicationSubmission,
  ApplicationSubmissionResponse,
} from '../models/application.model';

@Injectable({
  providedIn: 'root',
})
export class ApplicationService {
  private readonly http = inject(HttpClient);

  submitApplication(
    application: ApplicationSubmission,
    cv: File,
  ): Observable<ApplicationSubmissionResponse> {
    const formData = new FormData();
    formData.append(
      'application',
      new Blob([JSON.stringify(application)], { type: 'application/json' }),
    );
    formData.append('cv', cv, cv.name);

    return this.http.post<ApplicationSubmissionResponse>('/api/applications', formData);
  }
}
