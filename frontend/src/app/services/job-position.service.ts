import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { Observable } from 'rxjs';
import type { JobPosition } from '../models/job-position.model';

@Injectable({
  providedIn: 'root',
})
export class JobPositionService {
  private readonly http = inject(HttpClient);

  getActiveJobPositions(): Observable<JobPosition[]> {
    return this.http.get<JobPosition[]>('/api/positions', { params: { active: 'true' } });
  }
}
