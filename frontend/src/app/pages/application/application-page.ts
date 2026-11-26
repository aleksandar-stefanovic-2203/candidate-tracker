import { Component, OnInit, inject, signal } from '@angular/core';
import type { JobPosition } from '../../models/job-position.model';
import { JobPositionService } from '../../services/job-position.service';

@Component({
  selector: 'app-application-page',
  templateUrl: './application-page.html',
  styleUrl: './application-page.scss',
})
export class ApplicationPage implements OnInit {
  private readonly jobPositionService = inject(JobPositionService);

  positions = signal<JobPosition[]>([]);
  positionsLoading = signal(true);
  positionsLoadError = signal(false);

  ngOnInit(): void {
    this.jobPositionService.getActiveJobPositions().subscribe({
      next: (positions) => {
        this.positions.set(positions);
        this.positionsLoading.set(false);
      },
      error: (error: unknown) => {
        console.error('Unable to load active job positions.', error);
        this.positionsLoadError.set(true);
        this.positionsLoading.set(false);
      },
    });
  }
}
