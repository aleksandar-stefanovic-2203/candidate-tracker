import { Component, OnInit, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import type { ApplicationSubmission } from '../../models/application.model';
import type { JobPosition } from '../../models/job-position.model';
import { ApplicationService } from '../../services/application.service';
import { JobPositionService } from '../../services/job-position.service';
import { ApplicationValidation } from '../../validation/application-validation';

@Component({
  selector: 'app-application-page',
  imports: [ReactiveFormsModule],
  templateUrl: './application-page.html',
  styleUrl: './application-page.scss',
})
export class ApplicationPage implements OnInit {
  private readonly applicationService = inject(ApplicationService);
  private readonly jobPositionService = inject(JobPositionService);
  private readonly router = inject(Router);
  readonly validation = new ApplicationValidation();
  readonly applicationForm = this.validation.form;

  positions = signal<JobPosition[]>([]);
  positionsLoading = signal(true);
  positionsLoadError = signal(false);
  isSubmitting = signal(false);
  submitError = signal<string | null>(null);

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

  onCvChange(event: Event): void {
    const input = event.currentTarget;
    if (!(input instanceof HTMLInputElement)) {
      return;
    }

    const control = this.applicationForm.controls.cv;
    control.setValue(input.files?.item(0) ?? null);
    control.markAsTouched();
  }

  submitApplication(): void {
    this.submitError.set(null);
    if (this.isSubmitting()) {
      return;
    }
    if (this.applicationForm.invalid) {
      this.applicationForm.markAllAsTouched();
      return;
    }

    const form = this.applicationForm.getRawValue();
    if (!form.cv) {
      this.applicationForm.controls.cv.markAsTouched();
      return;
    }

    const application: ApplicationSubmission = {
      name: form.name,
      email: form.email,
      phone_number: form.phone_number,
      job_position_id: form.job_position_id ? Number(form.job_position_id) : null,
      recommendation: form.recommendation,
      message: form.message || null,
      consent_given_at: new Date().toISOString(),
    };

    this.isSubmitting.set(true);
    this.applicationService.submitApplication(application, form.cv).subscribe({
      next: () => this.handleSubmissionSuccess(),
      error: (error: unknown) => this.handleSubmissionError(error),
    });
  }

  private handleSubmissionSuccess(): void {
    this.isSubmitting.set(false);
    void this.router.navigateByUrl('/application/confirmation').catch((error: unknown) => {
      console.error('Application was saved but confirmation navigation failed.', error);
      this.submitError.set('Prijava je sačuvana, ali stranica potvrde nije mogla da se otvori.');
    });
  }

  private handleSubmissionError(error: unknown): void {
    console.error('Unable to submit application.', error);
    this.isSubmitting.set(false);
    const message = this.validation.handleSubmissionError(
      error,
      Boolean(this.applicationForm.controls.job_position_id.value),
    );
    if (message) {
      this.submitError.set(message);
    }
  }
}
