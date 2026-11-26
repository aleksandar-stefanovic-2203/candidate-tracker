import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ApplicationPage } from './application-page';

function readBlob(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        resolve(reader.result);
      } else {
        reject(new Error('Unable to read the application payload.'));
      }
    };
    reader.onerror = () => reject(reader.error ?? new Error('Unable to read the application payload.'));
    reader.readAsText(blob);
  });
}

describe('ApplicationPage', () => {
  let httpTesting: HttpTestingController;
  let navigationTarget: string | null;

  beforeEach(async () => {
    navigationTarget = null;
    await TestBed.configureTestingModule({
      imports: [ApplicationPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: Router,
          useValue: {
            navigateByUrl: (url: string) => {
              navigationTarget = url;
              return Promise.resolve(true);
            },
          },
        },
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('loads active positions and renders them as options', async () => {
    const fixture = TestBed.createComponent(ApplicationPage);
    fixture.detectChanges();

    const request = httpTesting.expectOne(
      (request) => request.url === '/api/positions' && request.params.get('active') === 'true',
    );
    expect(request.request.method).toBe('GET');

    request.flush([{ id: 7, title: 'Software Engineer', active: true }]);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#position')?.textContent).toContain('Software Engineer');
  });

  it('shows an error when active positions cannot be loaded', async () => {
    const fixture = TestBed.createComponent(ApplicationPage);
    fixture.detectChanges();

    httpTesting
      .expectOne((request) => request.url === '/api/positions')
      .flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aktivne pozicije nisu mogle da se učitaju.');
  });

  it('shows validation errors and does not submit an invalid application', () => {
    const fixture = TestBed.createComponent(ApplicationPage);
    fixture.detectChanges();
    httpTesting
      .expectOne((request) => request.url === '/api/positions' && request.params.get('active') === 'true')
      .flush([]);

    fixture.componentInstance.submitApplication();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('#name-error')?.textContent).toContain(
      'Unesite ime i prezime.',
    );
    expect(fixture.nativeElement.querySelector('#email-error')?.textContent).toContain(
      'Unesite imejl adresu.',
    );
    expect(fixture.nativeElement.querySelector('#phone-error')?.textContent).toContain(
      'Unesite broj telefona.',
    );
    expect(fixture.nativeElement.querySelector('#recommendation-error')?.textContent).toContain(
      'Unesite ko vas je preporučio.',
    );
    expect(fixture.nativeElement.querySelector('#cv-error')?.textContent).toContain(
      'Izaberite CV u PDF formatu.',
    );
    expect(fixture.nativeElement.querySelector('#consent-error')?.textContent).toContain(
      'Potvrdite saglasnost za obradu podataka.',
    );
    httpTesting.expectNone('/api/applications');
  });

  it('posts a valid application with its CV and navigates to confirmation', async () => {
    const fixture = TestBed.createComponent(ApplicationPage);
    fixture.detectChanges();
    httpTesting
      .expectOne((request) => request.url === '/api/positions' && request.params.get('active') === 'true')
      .flush([]);

    const cv = new File(['%PDF-1.7'], 'resume.pdf', { type: 'application/pdf' });
    fixture.componentInstance.applicationForm.patchValue({
      name: 'Ana Anic',
      email: 'ana@example.com',
      phone_number: '+381601234567',
      recommendation: 'Career site',
      cv,
      consent: true,
    });

    fixture.componentInstance.submitApplication();
    const request = httpTesting.expectOne('/api/applications');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeInstanceOf(FormData);
    const requestBody = request.request.body;
    if (!(requestBody instanceof FormData)) {
      throw new Error('Expected the application request to use multipart form data.');
    }
    const applicationPart = requestBody.get('application');
    expect(applicationPart).toBeInstanceOf(Blob);
    if (!(applicationPart instanceof Blob)) {
      throw new Error('Expected the application payload to be JSON.');
    }
    expect(applicationPart.type).toBe('application/json');
    const applicationJson = await readBlob(applicationPart);
    expect(applicationJson).toContain('"phone_number":"+381601234567"');
    expect(applicationJson).toContain('"job_position_id":null');
    expect(applicationJson).toContain('"consent_given_at":');
    expect(requestBody.get('cv')).toBeInstanceOf(File);

    request.flush({ id: 1, status: 'NEW', received_at: '2026-11-26T16:00:00Z' }, {
      status: 201,
      statusText: 'Created',
    });
    await fixture.whenStable();

    expect(navigationTarget).toBe('/application/confirmation');
  });
});
