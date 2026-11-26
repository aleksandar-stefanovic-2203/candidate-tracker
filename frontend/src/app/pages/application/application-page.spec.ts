import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApplicationPage } from './application-page';

describe('ApplicationPage', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplicationPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
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
});
