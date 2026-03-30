import { Injectable, NgZone } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class BarcodeService {
  private hiddenInput: HTMLInputElement | null = null;
  private scanSubject = new Subject<string>();
  private lastScanTime = 0;
  private readonly DEBOUNCE_MS = 300; // Prevent duplicate scans
  private isSetup = false;
  private keydownListener: ((e: KeyboardEvent) => void) | null = null;

  constructor(private ngZone: NgZone) {}

  setupHiddenInput(container: HTMLElement): void {
    if (this.isSetup && this.hiddenInput && this.hiddenInput.parentElement) {
      return;
    }

    this.cleanup();

    this.hiddenInput = document.createElement('input');
    this.hiddenInput.type = 'text';
    this.hiddenInput.className = 'barcode-scanner-input';
    this.hiddenInput.style.position = 'absolute';
    this.hiddenInput.style.left = '-9999px';
    this.hiddenInput.style.top = '-9999px';
    this.hiddenInput.style.opacity = '0';
    this.hiddenInput.style.pointerEvents = 'none';
    this.hiddenInput.style.width = '1px';
    this.hiddenInput.style.height = '1px';
    this.hiddenInput.setAttribute('aria-hidden', 'true');
    this.hiddenInput.setAttribute('tabindex', '-1');

    container.appendChild(this.hiddenInput);

    this.keydownListener = (e) => this.handleKeydown(e);

    this.ngZone.runOutsideAngular(() => {
      this.hiddenInput!.addEventListener('keydown', this.keydownListener!);
    });

    this.isSetup = true;
  }

  activateScanMode(): void {
    if (!this.hiddenInput || !this.isSetup) {
      console.warn(
        'BarcodeService: setupHiddenInput() must be called before activateScanMode()'
      );
      return;
    }
    this.hiddenInput.focus();
    this.hiddenInput.value = '';
  }


  deactivateScanMode(): void {
    if (this.hiddenInput) {
      this.hiddenInput.blur();
      this.hiddenInput.value = '';
    }
  }


  getScans(): Observable<string> {
    return this.scanSubject.asObservable().pipe(
      debounceTime(this.DEBOUNCE_MS)
    );
  }


  isScanModeActive(): boolean {
    return this.hiddenInput === document.activeElement;
  }


  cleanup(): void {
    if (this.hiddenInput) {
      if (this.keydownListener) {
        this.hiddenInput.removeEventListener('keydown', this.keydownListener);
        this.keydownListener = null;
      }

      if (this.hiddenInput.parentElement) {
        this.hiddenInput.parentElement.removeChild(this.hiddenInput);
      }

      this.hiddenInput = null;
    }

    this.isSetup = false;
  }


  destroy(): void {
    this.cleanup();
    this.scanSubject.complete();
  }


  private handleKeydown(event: KeyboardEvent): void {
    if (event.key !== 'Enter') {
      return;
    }

    event.preventDefault();

    const barcode = this.hiddenInput!.value.trim();

    if (barcode.length > 0) {
      const now = Date.now();
      if (now - this.lastScanTime > this.DEBOUNCE_MS) {
        this.lastScanTime = now;

        this.ngZone.run(() => {
          this.scanSubject.next(barcode);
        });

        this.hiddenInput!.value = '';
      }
    }
  }
}   
