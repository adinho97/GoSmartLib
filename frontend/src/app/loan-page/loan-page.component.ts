import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
} from "@angular/core";
import { BookService } from "../services/book.service";
import {
  LoanService,
  Loan,
  BookCopyInfo,
  ReturnCondition,
  ReturnLoanRequest,
} from "../services/loan.service";
import { SchoolService } from "../services/school.service";
import { ExperienceService } from "../services/experience.service";
import axios from "axios";
import { BrowserMultiFormatReader, IScannerControls } from "@zxing/browser";
import { BarcodeService } from "../services/barcode.service";

type BookOption = {
  id: number;
  titel: string;
  auteur: string;
  cover: string;
  availableCopies: number;
  totalCopies: number;
  scannedBarcode?: string;
};

type CopySelectionState = {
  book: BookOption;
  copies: BookCopyInfo[];
  selectedCopyId: number | null;
};

type Leerling = {
  sub: string;
  displayName: string;
};

type Step = "leerling" | "boeken" | "bevestiging";

@Component({
  selector: "app-loan-page",
  templateUrl: "./loan-page.component.html",
  styleUrls: ["./loan-page.component.css"],
  standalone: false,
})
export class LoanPageComponent implements OnInit, OnDestroy {
  step: Step = "leerling";

  // Scanning properties
  @ViewChild("scanContainer", { static: true }) scanContainer!: ElementRef;
  @ViewChild("cameraVideo") cameraVideo?: ElementRef<HTMLVideoElement>;
  cameraMode = false;
  isProcessingScan = false;
  cameraErrorMessage = "";
  isCameraDecoding = false;
  private cameraControls: IScannerControls | null = null;
  private readonly cameraCodeReader = new BrowserMultiFormatReader();
  private cameraCooldownUntil = 0;
  private scanSubscription: any;

  // Stap 1
  leerlingen: Leerling[] = [];
  filteredLeerlingen: Leerling[] = [];
  leerlingSearch = "";
  selectedLeerling: Leerling | null = null;
  leerlingenLoading = false;
  leerlingError = "";

  // Stap 2
  books: BookOption[] = [];
  filteredBooks: BookOption[] = [];
  bookPage = 1;
  readonly bookPageSize = 5;
  bookTotalCount = 0;
  searchQuery = "";
  selectedBooks: BookOption[] = [];
  activeLoans: Loan[] = [];
  loanHistory: Loan[] = [];
  showHistory = false;
  readonly historyPageSize = 5;
  currentHistoryPage = 1;

  // Stap 3
  dueDate = "";
  today = new Date().toISOString().split("T")[0];
  defaultDueDate = (() => {
    const d = new Date();
    d.setDate(d.getDate() + 14);
    return d.toISOString().split("T")[0];
  })();

  isLoading = true;
  isLoaning = false;
  successMessage = "";
  errorMessage = "";

  editingLoanId: number | null = null;
  tempDueDate: string = "";
  returnDialogOpen = false;
  returnDialogLoan: Loan | null = null;
  returnCondition: ReturnCondition = "GOOD";
  returnLostBook = false;
  isReturningLoan = false;

  bookNotFoundDialogOpen = false;
  scannedBarcodeNotFound = "";

  bookUnavailableDialogOpen = false;
  scanConfirmationOpen = false;
  pendingScannedBook: BookOption | null = null;

  copySelectionOpen = false;
  copySelectionState: CopySelectionState | null = null;
  isResolvingCopySelection = false;
  private copySelectionResolve: ((copyId: number | null) => void) | null = null;
  private booksLoadRequestId = 0;

  readonly role = localStorage.getItem("role") || "";
  private readonly currentUserSub =
    localStorage.getItem("sub") || localStorage.getItem("userId") || "";
  private readonly CAMERA_SCAN_COOLDOWN_MS = 1200;
  private readonly isIosSafari =
    /iPad|iPhone|iPod/.test(navigator.userAgent) &&
    /Safari/.test(navigator.userAgent) &&
    !/CriOS|FxiOS|EdgiOS/.test(navigator.userAgent);

  constructor(
    private bookService: BookService,
    private loanService: LoanService,
    private schoolService: SchoolService,
    private experienceService: ExperienceService,
    private barcodeService: BarcodeService,
  ) {}

  async ngOnInit() {
    if (this.scanContainer) {
      this.barcodeService.setupHiddenInput(this.scanContainer.nativeElement);
    }

    await Promise.all([this.loadBooks(), this.loadLeerlingen()]);
    this.dueDate = this.defaultDueDate;
    // Setup hardware scanner listener
    this.scanSubscription = this.barcodeService
      .getScans()
      .subscribe((barcode) => {
        // Add a brief delay to ensure visual feedback, consistent with AddBarcodeComponent
        setTimeout(() => {
          void this.processScan(barcode);
        }, 200);
      });
  }

  ngOnDestroy() {
    this.stopCameraDecoding();
    this.barcodeService.deactivateScanMode();
    if (this.scanSubscription) {
      this.scanSubscription.unsubscribe();
    }
    this.barcodeService.cleanup();
  }

  private async getDisplayNameForSub(sub: string): Promise<string> {
    try {
      const profile = await axios.get(
        `/api/users/${encodeURIComponent(sub)}/profile`,
      );
      const userInfo = profile.data as any;

      const fullname =
        userInfo.fullname ||
        `${userInfo.name || ""} ${userInfo.surname || ""}`.trim();
      return (
        (
          fullname ||
          userInfo.name ||
          userInfo.givenName ||
          userInfo.given_name ||
          userInfo.familyName ||
          userInfo.sub ||
          ""
        ).trim() || sub
      );
    } catch {
      return sub;
    }
  }

  async loadLeerlingen() {
    this.leerlingenLoading = true;
    try {
      const res = await axios.get("/api/gebruikers/leerlingen");
      const students = (res.data || []) as Array<{ sub: string }>;
      const enriched = await Promise.all(
        students.map(async (l) => ({
          sub: l.sub,
          displayName: await this.getDisplayNameForSub(l.sub),
        })),
      );

      this.leerlingen = enriched;
      this.filteredLeerlingen = [...this.leerlingen];
    } catch (err) {
      console.error("loadLeerlingen error", err);
      this.leerlingError = "Leerlingen laden mislukt.";
    } finally {
      this.leerlingenLoading = false;
    }
  }

  onLeerlingSearch() {
    const q = this.leerlingSearch.trim().toLowerCase();
    this.filteredLeerlingen = q
      ? this.leerlingen.filter(
          (l) =>
            l.displayName.toLowerCase().includes(q) ||
            l.sub.toLowerCase().includes(q),
        )
      : [...this.leerlingen];
  }

  selectLeerling(leerling: Leerling) {
    this.selectedLeerling = leerling;
    this.leerlingError = "";
  }

  confirmLeerling() {
    if (!this.selectedLeerling) {
      this.leerlingError = "Selecteer een leerling.";
      return;
    }
    this.step = "boeken";
    this.showHistory = false;
    this.currentHistoryPage = 1;
    this.loadActiveLoansForUser();
    this.barcodeService.activateScanMode(); // Activate hardware scanner when moving to books step
    this.loadLoanHistoryForUser();
  }

  async loadActiveLoansForUser() {
    if (!this.selectedLeerling) {
      this.activeLoans = [];
      return;
    }
    try {
      this.activeLoans = await this.loanService.getActiveLoans(
        this.selectedLeerling.sub,
      );
    } catch {
      this.activeLoans = [];
    }
  }

  async loadLoanHistoryForUser() {
    if (!this.selectedLeerling) {
      this.loanHistory = [];
      return;
    }
    try {
      this.loanHistory = await this.loanService.getLoanHistory(
        this.selectedLeerling.sub,
      );
      this.currentHistoryPage = 1;
    } catch {
      this.loanHistory = [];
    }
  }

  get totalHistoryPages(): number {
    return Math.max(
      1,
      Math.ceil(this.loanHistory.length / this.historyPageSize),
    );
  }

  get historyPageNumbers(): number[] {
    return Array.from({ length: this.totalHistoryPages }, (_, i) => i + 1);
  }

  get pagedLoanHistory(): Loan[] {
    const start = (this.currentHistoryPage - 1) * this.historyPageSize;
    return this.loanHistory.slice(start, start + this.historyPageSize);
  }

  goToHistoryPage(page: number) {
    this.currentHistoryPage = page;
  }

  async loadBooks() {
    const requestId = ++this.booksLoadRequestId;
    this.isLoading = true;
    try {
      const schoolId = this.schoolService.getSelectedSchoolId() ?? undefined;
      const pageData = await this.bookService.getBooksPage(
        this.bookPage - 1,
        this.bookPageSize,
        this.searchQuery,
        schoolId,
      );
      if (requestId !== this.booksLoadRequestId) {
        return;
      }
      this.bookTotalCount = pageData.total;

      const filteredItems = pageData.items.filter(
        (b: any) =>
          (b.genre || "").toLowerCase() !== "didactiek" ||
          this.role !== "leerling",
      );

      // Fetch copy summaries for each book in the paged results
      this.books = await Promise.all(
        filteredItems.map(async (b: any) => {
          const summary = await this.loanService.getCopySummary(b.id);
          return {
            id: b.id,
            titel: b.titel,
            auteur: b.auteur,
            cover: b.cover || "",
            availableCopies: summary.available,
            totalCopies: summary.total,
          };
        }),
      );
      this.filteredBooks = [...this.books];
    } catch {
      if (requestId !== this.booksLoadRequestId) {
        return;
      }
      this.errorMessage = "Boeken laden mislukt.";
    } finally {
      if (requestId === this.booksLoadRequestId) {
        this.isLoading = false;
      }
    }
  }

  get totalBookPages(): number {
    return Math.max(1, Math.ceil(this.bookTotalCount / this.bookPageSize));
  }

  get bookPageNumbers(): number[] {
    return Array.from({ length: this.totalBookPages }, (_, i) => i + 1);
  }

  async goToBookPage(page: number) {
    if (page < 1 || page > this.totalBookPages || page === this.bookPage) {
      return;
    }
    this.bookPage = page;
    await this.loadBooks();
  }

  async goToPreviousBookPage() {
    await this.goToBookPage(this.bookPage - 1);
  }

  async goToNextBookPage() {
    await this.goToBookPage(this.bookPage + 1);
  }

  async onSearch() {
    this.bookPage = 1;
    await this.loadBooks();
  }

  isSelected(book: BookOption): boolean {
    return this.selectedBooks.some((b) => b.id === book.id);
  }

  toggleBook(book: BookOption) {
    if (book.availableCopies === 0) return;
    if (this.isSelected(book)) {
      this.selectedBooks = this.selectedBooks.filter((b) => b.id !== book.id);
    } else {
      this.selectedBooks = [...this.selectedBooks, book];
    }
  }

  proceedToConfirm() {
    if (this.selectedBooks.length === 0) {
      this.errorMessage = "Selecteer minstens één boek.";
      return;
    }
    this.errorMessage = "";
    this.step = "bevestiging";
  }

  async loanBooks() {
    if (!this.dueDate || !this.selectedLeerling) return;
    this.isLoaning = true;
    this.errorMessage = "";
    this.successMessage = "";
    try {
      const shouldAwardLoanXp =
        !!this.currentUserSub &&
        this.selectedLeerling.sub === this.currentUserSub;

      for (const book of this.selectedBooks) {
        this.barcodeService.deactivateScanMode();
        const copyId = await this.resolveCopyForLoan(book);
        await this.loanService.createLoan(
          book.id,
          this.selectedLeerling.sub,
          this.dueDate,
          copyId ?? undefined,
        );
        if (shouldAwardLoanXp) {
          this.experienceService.addExperienceForLoaningBook();
        }
      }
      this.successMessage = `${this.selectedBooks.length} boek(en) uitgeleend aan ${this.selectedLeerling.displayName}.`;
      this.selectedBooks = [];
      this.step = "leerling";
      this.selectedLeerling = null;
      this.leerlingSearch = "";
      this.bookPage = 1;
      this.bookTotalCount = 0;
      this.searchQuery = "";
      this.filteredLeerlingen = [...this.leerlingen];
      this.dueDate = this.defaultDueDate;
      await this.loadBooks();
    } catch (e: any) {
      this.errorMessage =
        e?.response?.status === 409
          ? "Een of meer boeken zijn niet meer beschikbaar."
          : "Uitlenen mislukt. Probeer opnieuw.";
    } finally {
      this.isLoaning = false;
    }
  }

  private async resolveCopyForLoan(book: BookOption): Promise<number | null> {
    // If a specific GO-number was scanned, automatically find and select that copy
    if (book.scannedBarcode) {
      const copies = await this.loanService.getCopiesForBook(book.id);
      const match = (copies as any[]).find(c => c.goNumber === book.scannedBarcode);
      if (match) return match.id;
    }

    if (book.availableCopies <= 1) {
      return null;
    }

    const copies = await this.loanService.getCopiesForBook(book.id);
    const lendableCopies = copies
      .filter(
        (copy) => copy.status === "AVAILABLE" || copy.status === "DAMAGED",
      )
      .sort((a, b) => a.id - b.id);

    if (lendableCopies.length <= 1) {
      return lendableCopies[0]?.id ?? null;
    }

    return this.openCopySelectionDialog(book, lendableCopies);
  }

  private openCopySelectionDialog(
    book: BookOption,
    copies: BookCopyInfo[],
  ): Promise<number | null> {
    this.copySelectionState = {
      book,
      copies,
      selectedCopyId: copies[0]?.id ?? null,
    };
    this.copySelectionOpen = true;

    return new Promise<number | null>((resolve) => {
      this.copySelectionResolve = resolve;
    });
  }

  closeCopySelectionDialog() {
    this.copySelectionOpen = false;
    this.copySelectionState = null;
    if (this.copySelectionResolve) {
      this.copySelectionResolve(null);
      this.copySelectionResolve = null;
    }
  }

  confirmCopySelection() {
    if (
      !this.copySelectionState?.selectedCopyId ||
      !this.copySelectionResolve
    ) {
      return;
    }

    const selectedCopyId = this.copySelectionState.selectedCopyId;
    this.copySelectionOpen = false;
    this.copySelectionResolve(selectedCopyId);
    this.copySelectionResolve = null;
    this.copySelectionState = null;
  }

  getCopyConditionLabel(condition: BookCopyInfo["condition"]): string {
    if (condition === "MODERATE") return "Matig";
    if (condition === "BAD") return "Slecht";
    return "Goed";
  }

  getCopyStatusLabel(status: BookCopyInfo["status"]): string {
    if (status === "DAMAGED") return "Beschadigd";
    if (status === "LOST") return "Verloren";
    if (status === "LOANED") return "Uitgeleend";
    return "Beschikbaar";
  }

  async openReturnDialog(loan: Loan, event?: MouseEvent) {
    event?.stopPropagation();
    event?.preventDefault();
    this.returnDialogLoan = loan;
    this.returnLostBook = false;
    this.errorMessage = "";
    this.returnCondition = await this.resolveReturnConditionForLoan(loan);
    this.returnDialogOpen = true;
  }

  private async resolveReturnConditionForLoan(
    loan: Loan,
  ): Promise<ReturnCondition> {
    try {
      const copies = await this.loanService.getCopiesForBook(loan.bookId);
      const copy = copies.find((item) => item.id === loan.copyId);
      if (!copy) {
        return "GOOD";
      }

      if (copy.condition === "MODERATE") {
        return "MODERATE";
      }

      if (copy.condition === "BAD") {
        return "BAD";
      }

      return "GOOD";
    } catch {
      return "GOOD";
    }
  }

  closeReturnDialog() {
    this.returnDialogOpen = false;
    this.returnDialogLoan = null;
    this.returnCondition = "GOOD";
    this.returnLostBook = false;
    this.isReturningLoan = false;
  }

  closeBookNotFoundDialog() {
    this.bookNotFoundDialogOpen = false;
    this.scannedBarcodeNotFound = "";
  }

  get returnConditionLabel(): string {
    if (this.returnLostBook) {
      return "verloren";
    }

    if (this.returnCondition === "MODERATE") {
      return "matig";
    }

    if (this.returnCondition === "BAD") {
      return "slecht";
    }

    return "goed";
  }

  async confirmReturnLoan() {
    if (!this.returnDialogLoan || this.isReturningLoan) {
      return;
    }

    const request: ReturnLoanRequest = {
      condition: this.returnCondition,
      lost: this.returnLostBook,
    };

    this.isReturningLoan = true;
    try {
      const loan = this.returnDialogLoan;
      await this.loanService.returnLoan(loan.id, request);
      this.activeLoans = this.activeLoans.filter((l) => l.id !== loan.id);
      this.successMessage = request.lost
        ? `Boek "${loan.bookTitel}" als verloren gemeld.`
        : request.condition === "MODERATE"
          ? `Boek "${loan.bookTitel}" teruggebracht als matig.`
          : request.condition === "BAD"
            ? `Boek "${loan.bookTitel}" teruggebracht als slecht.`
            : `Boek "${loan.bookTitel}" teruggebracht.`;
      this.closeReturnDialog();
      await this.loadLoanHistoryForUser();
      await this.loadBooks();
    } catch {
      this.errorMessage = "Terugbrengen mislukt.";
    } finally {
      this.isReturningLoan = false;
    }
  }

  isOverdue(dueDate: string): boolean {
    return dueDate < this.today;
  }

  goBack() {
    if (this.step === "boeken") {
      this.barcodeService.deactivateScanMode();
      this.step = "leerling";
      this.selectedBooks = [];
      this.loanHistory = [];
      this.activeLoans = [];
      this.errorMessage = "";
    } else if (this.step === "bevestiging") {
      this.step = "boeken";
      this.errorMessage = "";
    }
  }

  startEditing(loan: Loan) {
    this.editingLoanId = loan.id;
    this.tempDueDate = new Date(loan.dueDate).toISOString().split("T")[0];
  }

  cancelEdit() {
    this.editingLoanId = null;
    this.tempDueDate = "";
  }

  async saveDueDate(loan: Loan) {
    try {
      await this.loanService.updateLoanDueDate(loan.id, this.tempDueDate);

      loan.dueDate = this.tempDueDate;
      this.editingLoanId = null;
      this.successMessage = `Deadline voor "${loan.bookTitel}" bijgewerkt.`;

      setTimeout(() => (this.successMessage = ""), 3000);
    } catch (error) {
      console.error("Error updating due date:", error);
      this.errorMessage = "Bijwerken deadline mislukt.";
      setTimeout(() => (this.errorMessage = ""), 3000);
    }
  }

  // --- Barcode Scanning Logic ---
  async activateCameraMode() {
    this.barcodeService.deactivateScanMode(); // Deactivate hardware scanner when camera is active
    this.cameraMode = true;
    this.cameraErrorMessage = "";
    this.errorMessage = "";

    // Brief delay to allow the video element to be rendered in the DOM
    setTimeout(async () => {
      await this.waitForViewRender();
      await this.startCameraDecoding(); // Start camera after view is rendered
    }, 50);
  }

  private waitForViewRender(): Promise<void> {
    return new Promise((resolve) => {
      setTimeout(() => resolve(), 0);
    });
  }

  deactivateCameraMode() {
    this.cameraCooldownUntil = 0; // Reset cooldown
    this.stopCameraDecoding();
    this.cameraMode = false;
    this.cameraErrorMessage = "";
  }

  private async startCameraDecoding() {
    const videoElement = this.cameraVideo?.nativeElement;
    if (!videoElement) {
      this.cameraErrorMessage = "Camera-element niet gevonden.";
      return;
    }

    if (!navigator.mediaDevices?.getUserMedia) {
      this.cameraErrorMessage =
        "Deze browser ondersteunt geen camera-scanning. Gebruik een recente browser (Safari/Chrome/Edge).";
      return;
    }

    if (!window.isSecureContext) {
      this.cameraErrorMessage =
        "Camera-scanning werkt alleen op HTTPS of localhost.";
      return;
    }

    this.stopCameraDecoding(); // Stop any existing camera stream

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
          void this.processScan(decodedValue);
        },
      );
      this.isCameraDecoding = true;
    } catch (err: any) {
      // More detailed error handling from AddBarcodeComponent
      let errorMessage =
        "Kan camera niet starten. Controleer toestemming en probeer opnieuw.";

      if (err?.name === "NotAllowedError") {
        errorMessage = this.isIosSafari
          ? "Camera-toestemming geweigerd. Open iOS Instellingen > Safari > Camera en sta toegang toe, herlaad daarna de pagina."
          : "Camera-toestemming geweigerd. Zet deze in instellingen aan.";
      } else if (err?.name === "NotFoundError") {
        errorMessage = "Geen camera gevonden op dit apparaat.";
      } else if (
        err?.name === "TrackStartError" ||
        err?.name === "NotReadableError"
      ) {
        errorMessage = "Camera wordt al door een ander programma gebruikt.";
      } else if (err?.name === "AbortError") {
        errorMessage =
          "Camera-start onderbroken. Probeer opnieuw en controleer browserrechten.";
      }
      this.cameraErrorMessage = errorMessage;
      console.error("Camera decoding error:", err);
    }
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

  async processScan(barcode: string) {
    if (
      this.isProcessingScan || 
      this.bookNotFoundDialogOpen || 
      this.bookUnavailableDialogOpen || 
      this.scanConfirmationOpen
    ) return;
    this.isProcessingScan = true;
    this.errorMessage = "";
    this.successMessage = "";

    try {
      const schoolId = this.schoolService.getSelectedSchoolId() ?? undefined;

      // 1. Try finding the book by GO-number in the library first (unique copy)
      let bookFoundByGo = null;
      try {
        bookFoundByGo = await this.bookService.getBookByGoNumberFromLibrary(barcode, schoolId);
      } catch (e) {
        // Ignore 404/errors here to allow fallback to ISBN search
        console.debug("Book not found by GO-number, trying ISBN...");
      }
      let book = bookFoundByGo;

      // 2. If not found, try searching by ISBN in the library
      if (!book) {
        book = await this.bookService.getBookByIsbnFromLibrary(
          barcode,
          schoolId,
        );
      }

      if (book) {
        // Fetch copy summary for the scanned book
        const summary = await this.loanService.getCopySummary(book.id);

        this.pendingScannedBook = {
          id: book.id,
          titel: book.titel,
          auteur: book.auteur,
          cover: book.cover || "",
          availableCopies: summary.available,
          totalCopies: summary.total,
          scannedBarcode: bookFoundByGo ? barcode : undefined
        };

        if (summary.available <= 0) {
          this.bookUnavailableDialogOpen = true;
        } else {
          this.scanConfirmationOpen = true;
        }
      } else {
        this.scannedBarcodeNotFound = barcode;
        this.bookNotFoundDialogOpen = true;
      }
    } catch (err: any) {
      this.errorMessage = "Er ging iets mis bij het zoeken naar het boek.";
    } finally {
      this.isProcessingScan = false;
    }
  }

  confirmScannedBook() {
    if (this.pendingScannedBook) {
      if (this.pendingScannedBook.availableCopies > 0) {
        if (!this.isSelected(this.pendingScannedBook)) {
          this.toggleBook(this.pendingScannedBook);
        }
        this.successMessage = `Boek "${this.pendingScannedBook.titel}" toegevoegd aan selectie.`;
        setTimeout(() => (this.successMessage = ""), 3000);
      } else {
        this.errorMessage = "Dit boek heeft geen beschikbare exemplaren.";
        setTimeout(() => (this.errorMessage = ""), 3000);
      }
    }
    this.closeScanConfirmation();
  }

  cancelScannedBook() {
    this.closeScanConfirmation();
  }

  private closeScanConfirmation() {
    this.scanConfirmationOpen = false;
    this.pendingScannedBook = null;
  }

  closeBookUnavailableDialog() {
    this.bookUnavailableDialogOpen = false;
    this.pendingScannedBook = null;
  }
}
