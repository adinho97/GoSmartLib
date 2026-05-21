import { Component, OnDestroy } from "@angular/core";
import { firstValueFrom } from "rxjs";
import { BibbeheerderService } from "../services/bibbeheerder.service";
import { AdminUserListItem } from "../models/admin-school";

@Component({
  selector: "app-teacher-promotion",
  templateUrl: "./teacher-promotion.component.html",
  styleUrls: ["./teacher-promotion.component.css"],
  standalone: false,
})
export class TeacherPromotionComponent implements OnDestroy {
  searchQuery = "";
  results: AdminUserListItem[] = [];
  searching = false;
  searchError = "";
  hasSearched = false;

  private searchDebounce: ReturnType<typeof setTimeout> | null = null;
  private activeSearchToken = 0;

  confirmTarget: AdminUserListItem | null = null;
  isPromoting = false;
  promoteError = "";

  successMessage = "";
  private successTimeout: ReturnType<typeof setTimeout> | null = null;

  constructor(private readonly bibbeheerderService: BibbeheerderService) {}

  ngOnDestroy(): void {
    if (this.searchDebounce !== null) clearTimeout(this.searchDebounce);
    if (this.successTimeout !== null) clearTimeout(this.successTimeout);
  }

  onSearch(): void {
    if (this.searchDebounce !== null) clearTimeout(this.searchDebounce);
    const q = this.searchQuery.trim();
    if (!q) {
      this.results = [];
      this.searching = false;
      this.hasSearched = false;
      this.activeSearchToken++;
      return;
    }
    this.searching = true;
    this.searchDebounce = setTimeout(() => this.runSearch(q), 300);
  }

  private async runSearch(query: string): Promise<void> {
    const token = ++this.activeSearchToken;
    try {
      const results = await firstValueFrom(
        this.bibbeheerderService.searchLeerkrachten(query),
      );
      if (token !== this.activeSearchToken) return;
      this.results = results || [];
      this.searchError = "";
    } catch (err: any) {
      if (token !== this.activeSearchToken) return;
      this.results = [];
      this.searchError = err?.error?.message || "Zoeken mislukt.";
    } finally {
      if (token === this.activeSearchToken) {
        this.searching = false;
        this.hasSearched = true;
      }
    }
  }

  openConfirm(user: AdminUserListItem): void {
    this.confirmTarget = user;
    this.promoteError = "";
  }

  cancelConfirm(): void {
    this.confirmTarget = null;
    this.promoteError = "";
  }

  confirmPromote(): void {
    if (this.isPromoting || !this.confirmTarget) return;
    this.isPromoting = true;
    this.promoteError = "";
    const target = this.confirmTarget;

    this.bibbeheerderService.promoteLeerkracht(target.id).subscribe({
      next: () => {
        this.results = this.results.filter((u) => u.id !== target.id);
        this.confirmTarget = null;
        this.isPromoting = false;
        this.showSuccess(
          `${target.displayName || target.sub} is nu bibbeheerder.`,
        );
      },
      error: (err) => {
        this.isPromoting = false;
        if (err?.error?.code === "INVALID_ROLE_TRANSITION") {
          this.promoteError = "Deze leerkracht is al bibbeheerder.";
          this.confirmTarget = null;
          this.results = this.results.filter((u) => u.id !== target.id);
        } else {
          this.promoteError =
            err?.error?.message || "Promoveren mislukt. Probeer opnieuw.";
        }
      },
    });
  }

  private showSuccess(message: string): void {
    this.successMessage = message;
    if (this.successTimeout !== null) clearTimeout(this.successTimeout);
    this.successTimeout = setTimeout(() => {
      this.successMessage = "";
      this.successTimeout = null;
    }, 3500);
  }
}
