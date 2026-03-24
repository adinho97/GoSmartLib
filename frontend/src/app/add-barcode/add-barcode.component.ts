import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BrowserMultiFormatReader, IScannerControls } from '@zxing/browser';
import { BarcodeService } from '../services/barcode.service';
import { BookService } from '../services/book.service';
import { SchoolService } from '../services/school.service';
import { School } from '../models/school';

export interface ScannedBookResult {
  isbn: string;
  titre: string;
  status: 'ADDED' | 'ALREADY_EXISTS' | 'NOT_FOUND' | 'ERROR';
  message: string;
  timestamp: Date;
}

@Component({
  selector: 'app-add-barcode',
  standalone: true,
  imports: [CommonModule,FormsModule],
  templateUrl: './add-barcode.component.html',
  styleUrl: './add-barcode.component.css',
})
export class AddBarcodeComponent implements OnInit, OnDestroy {
  @ViewChild('scanContainer') scanContainer!: ElementRef;
  @ViewChild('cameraVideo') cameraVideo?: ElementRef<HTMLVideoElement>;

  schools: School[] = [];
  selectedSchoolId: number | null = null;
  scanMode = false;
  cameraMode = false;
  isProcessing = false; 
  isCameraDecoding = false;
  scannedBooks: ScannedBookResult[] = [];
  lastScannedIsbn = '';
  cameraErrorMessage = '';
  errorMessage = '';
  successMessage = '';

  readonly statusColors: Record<string, string> = {
    ADDED: '#4caf50',
    ALREADY_EXISTS: '#ff9800',
    NOT_FOUND: '#f44336',
    ERROR: '#f44336',
  };

  private scanSubscription: any;
  private readonly cameraCodeReader = new BrowserMultiFormatReader();
  private cameraControls: IScannerControls | null = null;
  private cameraCooldownUntil = 0;
  private readonly CAMERA_SCAN_COOLDOWN_MS = 1200;
  private readonly SCANNED_BOOKS_PAGE_SIZE = 5;
  currentScannedBooksPage = 1;

  private readonly isIosSafari =
    /iPad|iPhone|iPod/.test(navigator.userAgent) &&
    /Safari/.test(navigator.userAgent) &&
    !/CriOS|FxiOS|EdgiOS/.test(navigator.userAgent);

  constructor(
    private barcodeService: BarcodeService,
    private bookService: BookService,
    private schoolService: SchoolService,
  ) {}

  async ngOnInit() {
    await this.loadSchools();

    if (this.scanContainer) {
      this.barcodeService.setupHiddenInput(
        this.scanContainer.nativeElement
      );

      this.scanSubscription = this.barcodeService.getScans().subscribe((barcode) => {
        this.lastScannedIsbn = barcode;

        // Keep a brief delay so the scanned ISBN is visible before processing.
        setTimeout(() => {
          void this.processScan(barcode);
        }, 200);
      });
    }
  }


  ngOnDestroy() {
    this.deactivateCameraMode();
    this.barcodeService.deactivateScanMode();

    if (this.scanSubscription) {
      this.scanSubscription.unsubscribe();
    }

    this.barcodeService.cleanup();
  }

  async loadSchools() {
    try {
      this.schools = await this.schoolService.getSchools();
      const storedSchoolId = this.schoolService.getSelectedSchoolId();
      const hasStoredSchool =
        storedSchoolId !== null &&
        this.schools.some((school) => school.id === storedSchoolId);

      const fallbackSchoolId =
        this.schools.length > 0 ? this.schools[0].id : null;
      this.selectedSchoolId = hasStoredSchool
        ? storedSchoolId
        : fallbackSchoolId;

      if (this.selectedSchoolId !== null) {
        this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
      }
    } catch (err) {
      this.errorMessage = 'Kon scholen niet laden.';
    }
  }

  onSchoolChange(value: string) {
    this.selectedSchoolId = value ? Number(value) : null;
    if (this.selectedSchoolId !== null) {
      this.schoolService.setSelectedSchoolId(this.selectedSchoolId);
    }
  }

  activateScanMode() {
    if (this.cameraMode) {
      this.deactivateCameraMode();
    }

    this.scanMode = true;
    this.errorMessage = '';
    this.barcodeService.activateScanMode();
  }

  deactivateScanMode() {
    this.scanMode = false;
    this.barcodeService.deactivateScanMode();
  }

  async activateCameraMode() {
    if (this.scanMode) {
      this.deactivateScanMode();
    }

    this.cameraMode = true;
    this.cameraErrorMessage = '';

    await this.waitForViewRender();
    await this.startCameraDecoding();
  }

  async retryCameraMode() {
    this.cameraErrorMessage = '';
    await this.waitForViewRender();
    await this.startCameraDecoding();
  }

  deactivateCameraMode() {
    this.stopCameraDecoding();
    this.cameraMode = false;
    this.cameraErrorMessage = '';
  }

  private async startCameraDecoding() {
    const videoElement = this.cameraVideo?.nativeElement;
    if (!videoElement) {
      this.cameraErrorMessage = 'Camera-element niet gevonden.';
      return;
    }

    if (!navigator.mediaDevices?.getUserMedia) {
      this.cameraErrorMessage =
        'Deze browser ondersteunt geen camera-scanning. Gebruik een recente browser (Safari/Chrome/Edge).';
      return;
    }

    if (!window.isSecureContext) {
      this.cameraErrorMessage =
        'Camera-scanning werkt alleen op HTTPS of localhost.';
      return;
    }

    this.stopCameraDecoding();

    try {
      this.cameraControls = await this.cameraCodeReader.decodeFromVideoDevice(
        undefined,
        videoElement,
        (result) => {
          if (!result) {
            return;
          }

          const decodedValue = result.getText()?.trim();
          if (!decodedValue) {
            return;
          }

          const now = Date.now();
          if (now < this.cameraCooldownUntil) {
            return;
          }

          this.cameraCooldownUntil = now + this.CAMERA_SCAN_COOLDOWN_MS;
          this.lastScannedIsbn = decodedValue;

          void this.processScan(decodedValue);
        },
      );

      this.isCameraDecoding = true;
    } catch (err: any) {
      let errorMessage = 'Kan camera niet starten. Controleer toestemming en probeer opnieuw.';

      if (err?.name === 'NotAllowedError') {
        errorMessage = this.isIosSafari
          ? 'Camera-toestemming geweigerd. Open iOS Instellingen > Safari > Camera en sta toegang toe, herlaad daarna de pagina.'
          : 'Camera-toestemming geweigerd. Zet deze in instellingen aan.';
      } else if (err?.name === 'NotFoundError') {
        errorMessage = 'Geen camera gevonden op dit apparaat.';
      } else if (err?.name === 'TrackStartError' || err?.name === 'NotReadableError') {
        errorMessage = 'Camera wordt al door een ander programma gebruikt.';
      } else if (err?.name === 'AbortError') {
        errorMessage =
          'Camera-start onderbroken. Probeer opnieuw en controleer browserrechten.';
      }

      this.cameraErrorMessage = errorMessage;
      this.isCameraDecoding = false;
    }
  }


  private waitForViewRender(): Promise<void> {
    return new Promise((resolve) => {
      setTimeout(() => resolve(), 0);
    });
  }
  private stopCameraDecoding() {
    if (this.cameraControls) {
      this.cameraControls.stop();
      this.cameraControls = null;
    }

    const videoElement = this.cameraVideo?.nativeElement;
    if (videoElement?.srcObject) {
      const stream = videoElement.srcObject as MediaStream;
      stream.getTracks().forEach((track) => track.stop());
      videoElement.srcObject = null;
    }

    this.isCameraDecoding = false;
  }

  get totalScannedBooksPages(): number {
    return Math.max(
      1,
      Math.ceil(this.scannedBooks.length / this.SCANNED_BOOKS_PAGE_SIZE),
    );
  }

  get paginatedScannedBooks(): ScannedBookResult[] {
    const startIndex =
      (this.currentScannedBooksPage - 1) * this.SCANNED_BOOKS_PAGE_SIZE;
    return this.scannedBooks.slice(
      startIndex,
      startIndex + this.SCANNED_BOOKS_PAGE_SIZE,
    );
  }

  get scannedBooksPageNumbers(): number[] {
    return Array.from(
      { length: this.totalScannedBooksPages },
      (_, index) => index + 1,
    );
  }

  goToScannedBooksPage(page: number) {
    if (page < 1 || page > this.totalScannedBooksPages) {
      return;
    }

    this.currentScannedBooksPage = page;
  }

  goToPreviousScannedBooksPage() {
    this.goToScannedBooksPage(this.currentScannedBooksPage - 1);
  }

  goToNextScannedBooksPage() {
    this.goToScannedBooksPage(this.currentScannedBooksPage + 1);
  }

  private resetToFirstScannedBooksPage() {
    this.currentScannedBooksPage = 1;
  }

  private normalizeScannedBooksPage() {
    if (this.currentScannedBooksPage > this.totalScannedBooksPages) {
      this.currentScannedBooksPage = this.totalScannedBooksPages;
    }
  }

  async processScan(barcode: string) {
    if (this.isProcessing) return;
    this.isProcessing = true;
    this.errorMessage = '';

    try {
      const book = await this.bookService.fetchBookByIsbn(barcode);

      const alreadyExists = await this.bookService.isBookInLibrary(
        barcode,
        this.selectedSchoolId ?? undefined,
      );

      if (alreadyExists) {
        this.scannedBooks.unshift({
          isbn: barcode,
          titre: book?.titel || 'Onbekend',
          status: 'ALREADY_EXISTS',
          message: 'Reeds in bibliotheek',
          timestamp: new Date(),
        });
        this.resetToFirstScannedBooksPage();
        return;
      }

      const savedBook = await this.bookService.importBookByIsbn(
        barcode,
        this.selectedSchoolId ?? undefined,
      );

      this.scannedBooks.unshift({
        isbn: barcode,
        titre: savedBook?.titel || book?.titel || 'Onbekend',
        status: 'ADDED',
        message: 'Toegevoegd aan bibliotheek',
        timestamp: new Date(),
      });
      this.resetToFirstScannedBooksPage();

      this.successMessage = `✓ ${savedBook?.titel || 'Boek'} toegevoegd`;
    } catch (err: any) {
      let status: ScannedBookResult['status'] = 'ERROR';
      let message = 'Er ging iets mis';

      if (err?.response?.status === 400) {
        status = 'ERROR';
        message = 'Ongeldig ISBN-formaat';
      } else if (err?.response?.status === 404) {
        status = 'NOT_FOUND';
        message = 'Boek niet gevonden';
      } else {
        message = err?.response?.data?.message || message;
      }

      this.scannedBooks.unshift({
        isbn: barcode,
        titre: '',
        status: status,
        message: message,
        timestamp: new Date(),
      });
      this.resetToFirstScannedBooksPage();

      this.errorMessage = message;
    } finally {
      this.isProcessing = false;
    }
  }

  clearSession() {
    this.scannedBooks = [];
    this.currentScannedBooksPage = 1;
    this.lastScannedIsbn = '';
    this.errorMessage = '';
    this.successMessage = '';
    this.deactivateScanMode();
    this.deactivateCameraMode();
  }

  undoLastScan() {
    if (this.scannedBooks.length > 0) {
      this.scannedBooks.shift();
      this.normalizeScannedBooksPage();
    }
  }

  getStatusColor(status: string): string {
    return this.statusColors[status] || '#666';
  }
}
