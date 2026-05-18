import { Component, OnInit } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { firstValueFrom } from "rxjs";
import { BibbeheerderService } from "../services/bibbeheerder.service";
import { AdminUserListItem } from "../models/admin-school";
import { formatUserInfoDisplayName } from "../utils/name-utils";

@Component({
  selector: "app-teacher-promotion",
  templateUrl: "./teacher-promotion.component.html",
  styleUrls: ["./teacher-promotion.component.css"],
  standalone: false,
})
export class TeacherPromotionComponent implements OnInit {
  leerkrachten: AdminUserListItem[] = [];
  loading = true;
  loadError = "";

  confirmTarget: AdminUserListItem | null = null;
  isPromoting = false;
  promoteError = "";

  constructor(
    private readonly bibbeheerderService: BibbeheerderService,
    private readonly http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.loadLeerkrachten();
  }

  private loadLeerkrachten(): void {
    this.loading = true;
    this.loadError = "";
    this.bibbeheerderService.getLeerkrachten().subscribe({
      next: (users) => {
        this.leerkrachten = users;
        this.loading = false;
      },
      error: (err) => {
        this.loadError = err?.error?.message || "Leerkrachten laden mislukt.";
        this.loading = false;
      },
    });
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

    this.bibbeheerderService
      .promoteLeerkracht(this.confirmTarget.id)
      .subscribe({
        next: () => {
          this.leerkrachten = this.leerkrachten.filter(
            (u) => u.id !== this.confirmTarget!.id,
          );
          this.confirmTarget = null;
          this.isPromoting = false;
        },
        error: (err) => {
          this.isPromoting = false;
          if (err?.error?.code === "INVALID_ROLE_TRANSITION") {
            this.promoteError = "Deze leerkracht is al bibbeheerder.";
            this.confirmTarget = null;
            this.loadLeerkrachten();
          } else {
            this.promoteError =
              err?.error?.message || "Promoveren mislukt. Probeer opnieuw.";
          }
        },
      });
  }
}
