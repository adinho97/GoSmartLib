import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BrowserMultiFormatReader, IScannerControls } from '@zxing/browser';
import { BarcodeService } from '../services/barcode.service';
import { BookService } from '../services/book.service';
import { SchoolService } from '../services/school.service';
import { LoanService } from '../services/loan.service';
import { School } from '../models/school';

export interface ScannedBookResult {
  isbn: string;
  titre: string;
  status: 'ADDED' | 'ALREADY_EXISTS' | 'NOT_FOUND' | 'ERROR';
  message: string;
  copiesTotalCount?: number;
  copiesAdded?: number;
  timestamp: Date;
}

export interface PendingBarcodeBook {
  isbn: string;
  book: any;
  isAlreadyInLibrary: boolean;
  copiesTotalCount: number;
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
  
  // Modal state
  showBarcodeModal = false;
  pendingScannedBook: PendingBarcodeBook | null = null;
  pendingCopiesCount: number = 1;
  isConfirmingBarcode = false;

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
    private loanService: LoanService,
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
      // Fetch book from database first (library), then from OpenLibrary if not found
      let libraryBook = await this.bookService.getBookByIsbnFromLibrary(
        barcode,
        this.selectedSchoolId ?? undefined,
      );

      let book = libraryBook || (await this.bookService.fetchBookByIsbn(barcode));
      const isAlreadyInLibrary = libraryBook !== null;

      // Get copy count if book exists in library
      let copiesTotalCount = 0;
      if (isAlreadyInLibrary && libraryBook?.id) {
        const summary = await this.loanService.getCopySummary(libraryBook.id);
        copiesTotalCount = summary.total;
      }

      // Show modal instead of immediately adding
      this.pendingScannedBook = {
        isbn: barcode,
        book: book,
        isAlreadyInLibrary: isAlreadyInLibrary,
        copiesTotalCount: copiesTotalCount,
      };
      this.pendingCopiesCount = 1;
      this.showBarcodeModal = true;
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

  async confirmBarcodeAdd() {
    if (!this.pendingScannedBook || this.isConfirmingBarcode) return;
    if (this.pendingCopiesCount < 1) {
      this.errorMessage = 'Voer een geldig aantal exemplaren in.';
      return;
    }

    this.isConfirmingBarcode = true;
    this.errorMessage = '';

    try {
      const barcode = this.pendingScannedBook.isbn;
      const isAlreadyInLibrary = this.pendingScannedBook.isAlreadyInLibrary;

      let bookId: number | null = null;

      if (isAlreadyInLibrary) {
        // Book already exists, just add copies
        bookId = this.pendingScannedBook.book?.id;
      } else {
        // Import new book first, then add copies
        const savedBook = await this.bookService.importBookByIsbn(
          barcode,
          this.selectedSchoolId ?? undefined,
        );
        bookId = savedBook?.id;
      }

      if (bookId) {
        // Add copies
        const promises = Array.from({ length: this.pendingCopiesCount }, () =>
          this.loanService.addCopy(bookId),
        );
        await Promise.all(promises);

        // Get updated copy count
        const summary = await this.loanService.getCopySummary(bookId);
        const totalCopies = summary.total;

        // Add result to list
        this.scannedBooks.unshift({
          isbn: barcode,
          titre: this.pendingScannedBook.book?.titel || 'Onbekend',
          status: 'ADDED',
          message: `${isAlreadyInLibrary ? 'Gescand en' : 'Toegevoegd met'} ${this.pendingCopiesCount} exemplaar(en). Totaal in bibliotheek: ${totalCopies} exemplaar(en)`,
          copiesTotalCount: totalCopies,
          copiesAdded: this.pendingCopiesCount,
          timestamp: new Date(),
        });
        this.resetToFirstScannedBooksPage();

        this.successMessage = `✓ ${this.pendingScannedBook.book?.titel || 'Boek'} verwerkt`;
      }

      this.closeBarcodeModal();
    } catch (err: any) {
      this.errorMessage = 'Er ging iets mis bij het verwerken van het boek.';
    } finally {
      this.isConfirmingBarcode = false;
    }
  }

  cancelBarcodeAdd() {
    this.closeBarcodeModal();
  }

  private closeBarcodeModal() {
    this.showBarcodeModal = false;
    this.pendingScannedBook = null;
    this.pendingCopiesCount = 1;
  }

  clearSession() {
    this.scannedBooks = [];
    this.currentScannedBooksPage = 1;
    this.lastScannedIsbn = '';
    this.errorMessage = '';
    this.successMessage = '';
    this.closeBarcodeModal();
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
