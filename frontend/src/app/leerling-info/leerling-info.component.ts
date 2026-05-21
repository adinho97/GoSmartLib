import { Component, OnInit } from "@angular/core";
import {
  InfoContentService,
  InfoContentItem,
  Sectie,
} from "../services/info-content.service";
import { AuthContextService } from "../services/auth-context.service";

type Mode = "student" | "librarian" | "super_admin";
type Scope = "school" | "global";

interface Draft {
  titel: string;
  inhoud: string;
  customSectionTitle?: string | null;
}

@Component({
  selector: "app-leerling-info",
  templateUrl: "./leerling-info.component.html",
  styleUrls: ["./leerling-info.component.css"],
  standalone: false,
})
export class LeerlingInfoComponent implements OnInit {
  readonly sections: Sectie[] = ["STAP", "FEATURE", "TIP", "FAQ", "CUSTOM"];

  items: Record<Sectie, InfoContentItem[]> = {
    STAP: [],
    FEATURE: [],
    TIP: [],
    FAQ: [],
    CUSTOM: [],
  };

  openFaqIndex: number | null = 0;

  sectionOpen: Record<Sectie, boolean> = {
    STAP: true,
    FEATURE: true,
    TIP: true,
    FAQ: true,
    CUSTOM: true,
  };
  customSectionOpen: Record<string, boolean> = {};

  customSections: { title: string; items: InfoContentItem[] }[] = [];
  newCustomSectionTitle = "";

  mode: Mode = "student";
  scopeSchoolId: number | null = null;
  scopeSchoolName = "";
  scopeMode: Scope = "global";

  editingId: number | null = null;
  editDraft: Draft = { titel: "", inhoud: "" };

  addingTo: Sectie | null = null;
  addingToCustomTitle: string | null = null;
  addDraft: Draft = { titel: "", inhoud: "" };

  saving = false;
  errorMsg = "";

  constructor(
    private infoContentService: InfoContentService,
    private authContext: AuthContextService,
  ) {}

  ngOnInit(): void {
    this.detectMode();
    this.loadAll();
  }

  private detectMode(): void {
    if (this.authContext.isAdminMode()) {
      this.mode = "super_admin";
      const raw = localStorage.getItem("adminLibrarySchoolId");
      const id = raw ? Number(raw) : NaN;
      if (Number.isFinite(id)) {
        this.scopeSchoolId = id;
        this.scopeSchoolName =
          localStorage.getItem("adminLibrarySchoolName") || "Deze school";
        this.scopeMode = "school";
      } else {
        this.scopeMode = "global";
      }
    } else if (localStorage.getItem("role") === "bibbeheerder") {
      this.mode = "librarian";
    } else {
      this.mode = "student";
    }
  }

  get hasSchoolScope(): boolean {
    return this.scopeSchoolId != null;
  }

  private currentSchoolIdParam(): number | null {
    if (this.mode === "super_admin") {
      return this.scopeMode === "school" ? this.scopeSchoolId : null;
    }
    return null;
  }

  loadAll(): void {
    const schoolId = this.currentSchoolIdParam();
    for (const sectie of this.sections) {
      this.infoContentService.getAll(sectie, schoolId).subscribe({
        next: (items) => {
          this.items[sectie] = items;
          if (sectie === "CUSTOM") {
            this.processCustomItems(items);
          }
        },
        error: () => (this.items[sectie] = []),
      });
    }
  }

  private processCustomItems(items: InfoContentItem[]): void {
    const grouped: Record<string, InfoContentItem[]> = {};
    items.forEach((item) => {
      const title = item.customSectionTitle || "Overige";
      if (!grouped[title]) grouped[title] = [];
      grouped[title].push(item);
      // Initialize toggle state to open if not already set
      if (this.customSectionOpen[title] === undefined) {
        this.customSectionOpen[title] = true;
      }
    });
    this.customSections = Object.keys(grouped).map((title) => ({
      title,
      items: grouped[title],
    }));
  }

  addCustomSection(): void {
    const title = this.newCustomSectionTitle.trim();
    if (!title || this.customSections.some((s) => s.title === title)) return;
    this.customSections.push({ title, items: [] });
    this.customSectionOpen[title] = true;
    this.newCustomSectionTitle = "";
  }

  removeCustomSection(title: string): void {
    const section = this.customSections.find((s) => s.title === title);
    if (section && section.items.length > 0) {
      this.errorMsg = "Verwijder eerst alle items uit deze rubriek.";
      return;
    }
    this.customSections = this.customSections.filter((s) => s.title !== title);
  }

  setScope(scope: Scope): void {
    if (this.mode !== "super_admin" || !this.scopeSchoolId) return;
    if (this.scopeMode === scope) return;
    this.scopeMode = scope;
    this.cancelEdit();
    this.cancelAdd();
    this.loadAll();
  }

  hasTitle(sectie: Sectie): boolean {
    return sectie === "FEATURE" || sectie === "FAQ" || sectie === "CUSTOM";
  }

  canAdd(sectie: Sectie): boolean {
    if (this.mode === "super_admin") return true;
    if (this.mode === "librarian")
      return sectie === "TIP" || sectie === "FAQ" || sectie === "CUSTOM";
    return false;
  }

  canEdit(item: InfoContentItem): boolean {
    if (this.mode === "super_admin") return true;
    if (this.mode === "librarian") return item.schoolId != null;
    return false;
  }

  canRemove(item: InfoContentItem): boolean {
    if (this.mode === "super_admin") return true;
    if (this.mode === "librarian") {
      if (item.schoolId != null) return true;
      return item.sectie === "TIP" || item.sectie === "FAQ";
    }
    return false;
  }

  private removeKind(item: InfoContentItem): "delete" | "hide" {
    if (item.schoolId != null) return "delete";
    if (this.mode === "super_admin" && this.scopeMode === "global")
      return "delete";
    return "hide";
  }

  removeLabel(item: InfoContentItem): string {
    return this.removeKind(item) === "hide"
      ? "Verwijder voor deze school"
      : "Verwijder";
  }

  isGlobal(item: InfoContentItem): boolean {
    return item.schoolId == null;
  }

  isSectionAllGlobal(sectie: Sectie): boolean {
    const list = this.items[sectie];
    return list.length > 0 && list.every((item) => this.isGlobal(item));
  }

  toggleSection(sectie: Sectie, customTitle?: string): void {
    if (sectie === 'CUSTOM' && customTitle) {
      this.customSectionOpen[customTitle] = !this.customSectionOpen[customTitle];
      return;
    }
    this.sectionOpen[sectie] = !this.sectionOpen[sectie];
  }

  startEdit(item: InfoContentItem): void {
    if (!this.canEdit(item) || item.id == null) return;
    this.cancelAdd();
    this.editingId = item.id;
    this.editDraft = {
      titel: item.titel ?? "",
      inhoud: item.inhoud,
      customSectionTitle: item.customSectionTitle,
    };
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editDraft = { titel: "", inhoud: "" };
  }

  saveEdit(item: InfoContentItem): void {
    if (this.editingId == null || item.id !== this.editingId || item.id == null)
      return;
    if (!this.editDraft.inhoud.trim()) return;
    this.saving = true;
    this.errorMsg = "";
    this.infoContentService
      .update(item.id, {
        sectie: item.sectie,
        titel: this.editDraft.titel?.trim() || null,
        inhoud: this.editDraft.inhoud.trim(),
        sortOrder: item.sortOrder ?? 0,
        schoolId: item.schoolId ?? null,
        customSectionTitle: this.editDraft.customSectionTitle,
      })
      .subscribe({
        next: (updated) => {
          if (item.sectie === "CUSTOM") {
            this.loadAll(); // Herladen om groepering te herstellen
          } else {
            const list = this.items[item.sectie];
            const idx = list.findIndex((x) => x.id === updated.id);
            if (idx >= 0) list[idx] = updated;
          }
          this.cancelEdit();
          this.saving = false;
        },
        error: (err) => {
          this.errorMsg = err?.error?.message || "Opslaan mislukt.";
          this.saving = false;
        },
      });
  }

  startAdd(sectie: Sectie, customTitle?: string): void {
    if (!this.canAdd(sectie)) return;
    this.cancelEdit();
    this.addingTo = sectie;
    this.addingToCustomTitle = customTitle || null;
    this.addDraft = { titel: "", inhoud: "" };
  }

  cancelAdd(): void {
    this.addingTo = null;
    this.addingToCustomTitle = null;
    this.addDraft = { titel: "", inhoud: "" };
  }

  saveNew(sectie: Sectie): void {
    if (this.addingTo !== sectie) return;
    if (!this.addDraft.inhoud.trim()) return;
    this.saving = true;
    this.errorMsg = "";
    const payload: InfoContentItem = {
      sectie,
      titel: this.addDraft.titel?.trim() || null,
      inhoud: this.addDraft.inhoud.trim(),
      sortOrder: sectie === "CUSTOM" ? 0 : this.items[sectie].length,
      customSectionTitle: this.addingToCustomTitle,
      schoolId:
        this.mode === "super_admin" ? this.currentSchoolIdParam() : undefined,
    };
    this.infoContentService.create(payload).subscribe({
      next: (created) => {
        if (sectie === "CUSTOM") {
          this.loadAll(); // Refresh grouping
        } else {
          this.items[sectie] = [...this.items[sectie], created];
        }
        this.cancelAdd();
        this.saving = false;
      },
      error: (err) => {
        this.errorMsg = err?.error?.message || "Toevoegen mislukt.";
        this.saving = false;
      },
    });
  }

  remove(item: InfoContentItem): void {
    if (!this.canRemove(item) || item.id == null) return;
    const kind = this.removeKind(item);
    const msg =
      kind === "delete"
        ? "Permanent verwijderen?"
        : "Verwijderen uit deze school?";
    if (!confirm(msg)) return;
    this.errorMsg = "";
    const obs =
      kind === "delete"
        ? this.infoContentService.delete(item.id)
        : this.infoContentService.hide(
            item.id,
            this.scopeMode === "school" ? this.scopeSchoolId : undefined,
          );
    obs.subscribe({
      next: () => {
        this.items[item.sectie] = this.items[item.sectie].filter(
          (x) => x.id !== item.id,
        );
      },
      error: (err) => {
        this.errorMsg = err?.error?.message || "Verwijderen mislukt.";
      },
    });
  }

  toggleFaq(index: number): void {
    this.openFaqIndex = this.openFaqIndex === index ? null : index;
  }

  isFaqOpen(index: number): boolean {
    return this.openFaqIndex === index;
  }
}
