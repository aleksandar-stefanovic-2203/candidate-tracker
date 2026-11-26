import { HttpErrorResponse } from '@angular/common/http';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ValidationErrors,
  Validators,
} from '@angular/forms';

const MAX_CV_SIZE_BYTES = 5 * 1024 * 1024;

const notBlankValidator = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'string' && control.value.trim().length === 0 ? { blank: true } : null;

const cvFileValidator = (control: AbstractControl): ValidationErrors | null => {
  const file: unknown = control.value;
  if (typeof File === 'undefined' || !(file instanceof File)) {
    return null;
  }
  if (!file.name.toLowerCase().endsWith('.pdf')) {
    return { invalidPdf: true };
  }
  if (file.size > MAX_CV_SIZE_BYTES) {
    return { maxFileSize: true };
  }
  return null;
};

export type ApplicationField =
  | 'name'
  | 'email'
  | 'phone_number'
  | 'job_position_id'
  | 'recommendation'
  | 'message'
  | 'cv'
  | 'consent';

export class ApplicationValidation {
  readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, notBlankValidator, Validators.minLength(2), Validators.maxLength(100)],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, notBlankValidator, Validators.email, Validators.maxLength(254)],
    }),
    phone_number: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, notBlankValidator, Validators.minLength(6), Validators.maxLength(20)],
    }),
    job_position_id: new FormControl('', { nonNullable: true }),
    recommendation: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, notBlankValidator, Validators.minLength(2), Validators.maxLength(100)],
    }),
    message: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(1000)],
    }),
    cv: new FormControl<File | null>(null, [Validators.required, cvFileValidator]),
    consent: new FormControl(false, {
      nonNullable: true,
      validators: [Validators.requiredTrue],
    }),
  });

  fieldError(field: ApplicationField): string | null {
    const control = this.form.controls[field];
    if (!control.touched || !control.errors) {
      return null;
    }

    if (control.hasError('required') || control.hasError('blank')) {
      return {
        name: 'Unesite ime i prezime.',
        email: 'Unesite imejl adresu.',
        phone_number: 'Unesite broj telefona.',
        job_position_id: '',
        recommendation: 'Unesite ko vas je preporučio.',
        message: '',
        cv: 'Izaberite CV u PDF formatu.',
        consent: 'Potvrdite saglasnost za obradu podataka.',
      }[field];
    }
    if (control.hasError('email')) {
      return 'Unesite ispravnu imejl adresu.';
    }
    if (control.hasError('minlength')) {
      const minimum = control.getError('minlength')?.requiredLength;
      if (typeof minimum !== 'number') {
        return 'Unesite ispravnu vrednost.';
      }
      const fieldNames: Partial<Record<ApplicationField, string>> = {
        name: 'Ime i prezime',
        phone_number: 'Broj telefona',
        recommendation: 'Odgovor',
      };
      return `${fieldNames[field] ?? 'Polje'} mora imati najmanje ${minimum} karaktera.`;
    }
    if (control.hasError('maxlength')) {
      const maximum = control.getError('maxlength')?.requiredLength;
      if (typeof maximum !== 'number') {
        return 'Unesite ispravnu vrednost.';
      }
      const fieldNames: Partial<Record<ApplicationField, string>> = {
        name: 'Ime i prezime',
        email: 'Imejl adresa',
        phone_number: 'Broj telefona',
        recommendation: 'Odgovor',
        message: 'Poruka',
      };
      return `${fieldNames[field] ?? 'Polje'} ne sme biti duže od ${maximum} karaktera.`;
    }
    if (control.hasError('invalidPdf')) {
      return 'CV mora biti PDF fajl.';
    }
    if (control.hasError('maxFileSize')) {
      return 'CV ne sme biti veći od 5 MB.';
    }
    if (control.hasError('positionUnavailable')) {
      return 'Izabrana pozicija više nije dostupna. Izaberite drugu poziciju.';
    }

    return 'Unesite ispravnu vrednost.';
  }

  handleSubmissionError(error: unknown, hasSelectedPosition: boolean): string | null {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 413) {
        this.setFieldError('cv', { maxFileSize: true });
        return null;
      }
      if (error.status === 415) {
        this.setFieldError('cv', { invalidPdf: true });
        return null;
      }
      if (error.status === 404 && hasSelectedPosition) {
        this.setFieldError('job_position_id', { positionUnavailable: true });
        return null;
      }
    }

    return 'Prijava nije mogla da se sačuva. Proverite podatke i pokušajte ponovo.';
  }

  private setFieldError(field: ApplicationField, error: ValidationErrors): void {
    const control = this.form.controls[field];
    control.setErrors(error);
    control.markAsTouched();
  }
}
