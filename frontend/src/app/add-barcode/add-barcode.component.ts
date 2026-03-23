import { Component, OnInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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

  schools: School[] = [];
  selectedSchoolId: number | null = null;
  scanMode = false;
  isProcessing = false;
  scannedBooks: ScannedBookResult[] = [];
  manualIsbn = '';
  errorMessage = '';
  successMessage = '';

  readonly statusColors: Record<string, string> = {
    ADDED: '#4caf50',
    ALREADY_EXISTS: '#ff9800',
    NOT_FOUND: '#f44336',
    ERROR: '#f44336',
  };

  private scanSubscription: any;

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
        // Populate the manual input field so user sees what was scanned
        this.manualIsbn = barcode;
        // After a brief delay to show the ISBN, process it
        setTimeout(() => {
          void this.addManualIsbn();
        }, 200);
      });
    }
  }


  ngOnDestroy() {
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
    this.scanMode = true;
    this.errorMessage = '';
    this.barcodeService.activateScanMode();
  }

  deactivateScanMode() {
    this.scanMode = false;
    this.barcodeService.deactivateScanMode();
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

      this.errorMessage = message;
    } finally {
      this.isProcessing = false;
    }
  }

  clearSession() {
    this.scannedBooks = [];
    this.errorMessage = '';
    this.successMessage = '';
    this.deactivateScanMode();
  }

  undoLastScan() {
    if (this.scannedBooks.length > 0) {
      this.scannedBooks.shift();
    }
  }

  async addManualIsbn() {
    const trimmedIsbn = this.manualIsbn.trim();
    if (!trimmedIsbn) {
      this.errorMessage = 'Voer een ISBN-nummer in.';
      return;
    }

    this.manualIsbn = '';
    await this.processScan(trimmedIsbn);
  }

  getStatusColor(status: string): string {
    return this.statusColors[status] || '#666';
  }
}
