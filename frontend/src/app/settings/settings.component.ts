import { Component, OnDestroy, OnInit, ChangeDetectorRef } from "@angular/core";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
import { SettingsService, SchoolSettings } from "../services/settings.service";
import { SchoolService } from "../services/school.service";
import {
  AdminGenreService,
  Genre as ApiGenre,
} from "../services/admin-genre.service";
import { AdminTagService, Tag } from "../services/admin-tag.service";
import { UiToastService } from "../services/ui-toast.service";
import { forkJoin, from } from "rxjs";
import { catchError, of } from 'rxjs';

interface Message {
  id: string;
  title: string;
  body: string;
  accent: "info" | "warn" | "good" | "brand";
  enabled: boolean;
  startsAt: string;
  endsAt: string;
}

interface DayHours {
  open: boolean;
  from: string;
  to: string;
}

interface Genre {
  id: string;
  naam: string;
  count: number;
  subs: { id: number; naam: string }[];
}

interface ReadingLevel {
  id: string;
  code: string;
  name: string;
  desc: string;
  active: boolean;
  isDefault: boolean;
}

interface AccentOption {
  id: "info" | "warn" | "good" | "brand";
  label: string;
}

interface DayDef {
  id: keyof DefaultHours;
  label: string;
  short: string;
}

interface DefaultHours {
  mon: DayHours;
  tue: DayHours;
  wed: DayHours;
  thu: DayHours;
  fri: DayHours;
  sat: DayHours;
  sun: DayHours;
}

interface NavItem {
  id: string;
  label: string;
}

interface ConfirmTarget {
  title: string;
  body: string;
  confirmLabel: string;
  onConfirm: () => void;
}

const ROTATE_MS = 20000;
const NETWORK_DEFAULT_LOAN_DAYS = 14;

@Component({
  selector: "app-settings",
  templateUrl: "./settings.component.html",
  styleUrls: ["./settings.component.css"],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class SettingsComponent implements OnInit, OnDestroy {
  schoolName = "Laden...";
  private schoolId: number | null = null;

  readonly nav: NavItem[] = [
    { id: "motd", label: "Berichten" },
    { id: "hours", label: "Openingsuren" },
    { id: "loanterm", label: "Uitleentermijn" },
    { id: "genres", label: "Genres" },
    { id: "tags", label: "Thema's (Tags)" },
    { id: "levels", label: "Leesniveaus" },
  ];

  readonly accents: AccentOption[] = [
    { id: "info", label: "Info" },
    { id: "warn", label: "Belangrijk" },
    { id: "good", label: "Positief" },
    { id: "brand", label: "Merk" },
  ];

  readonly days: DayDef[] = [
    { id: "mon", label: "Maandag", short: "MA" },
    { id: "tue", label: "Dinsdag", short: "DI" },
    { id: "wed", label: "Woensdag", short: "WO" },
    { id: "thu", label: "Donderdag", short: "DO" },
    { id: "fri", label: "Vrijdag", short: "VR" },
    { id: "sat", label: "Zaterdag", short: "ZA" },
    { id: "sun", label: "Zondag", short: "ZO" },
  ];

  readonly loanPresets = [
    { days: 7, label: "1 week" },
    { days: 14, label: "2 weken" },
    { days: 21, label: "3 weken" },
    { days: 28, label: "4 weken" },
  ];

  readonly networkLoanDays = NETWORK_DEFAULT_LOAN_DAYS;
  readonly minLoanDays = 1;
  readonly maxLoanDays = 90;

  activeAnchor = "motd";

  messages: Message[] = [];
  hours: DefaultHours = this.defaultHours();
  loanDays = 7;
  genres: Genre[] = [];
  tags: Tag[] = [];
  levels: ReadingLevel[] = [];

  isLoading = true;
  private initialState = {
    messages: "",
    hours: "",
    loanDays: 0,
    levels: "",
  };

  dirty = false;
  savedAt = "Vandaag, 10:42";

  // Messages section UI state
  editingMessageId: string | null = null;
  addingMessage = false;
  messageDraft: Message | null = null;
  previewIdx = 0;
  private rotateTimer: ReturnType<typeof setInterval> | null = null;

  // Genres section UI state
  genreQuery = "";
  editingGenreId: string | null = null;
  editingGenreName = "";
  addingSubFor: string | null = null;
  subDraft = "";
  newGenreName = "";
  newTagName = "";

  confirmTarget: ConfirmTarget | null = null;

  constructor(
    private settingsService: SettingsService,
    public schoolService: SchoolService,
    private genreService: AdminGenreService,
    private tagService: AdminTagService,
    private toastService: UiToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.schoolId = this.schoolService.getSelectedSchoolId();
    if (!this.schoolId) {
      this.toastService.error("Geen school geselecteerd.");
      return;
    }

    this.loadAllData();
    this.startRotation();
  }

  private loadAllData(): void {
    if (!this.schoolId) return;
    this.isLoading = true;

    forkJoin({
      schoolInfo: this.settingsService.getSchoolName(this.schoolId),
      settings: this.settingsService.getSettings(this.schoolId),
      loanDays: from(this.schoolService.getDefaultLoanDays(this.schoolId)),
      apiGenres: this.genreService.getAll().pipe(catchError(() => of([]))),
      apiTags: this.tagService.getAll().pipe(catchError(() => of([]))),
    }).subscribe({
      next: (res) => {
        this.schoolName = res.schoolInfo.name;
        this.messages = res.settings.messages ?? [];

        // Merge de opgeslagen uren met de defaults zodat alle 7 dagen altijd aanwezig zijn in de UI
        const savedHours = res.settings.hours as any;
        const defaults = this.defaultHours();
        this.hours = {
          mon: savedHours?.mon ?? defaults.mon,
          tue: savedHours?.tue ?? defaults.tue,
          wed: savedHours?.wed ?? defaults.wed,
          thu: savedHours?.thu ?? defaults.thu,
          fri: savedHours?.fri ?? defaults.fri,
          sat: savedHours?.sat ?? defaults.sat,
          sun: savedHours?.sun ?? defaults.sun,
        };

        // Gebruik de default niveaus (A-D) als de database nog geen niveaus bevat
        this.levels = (res.settings.levels && res.settings.levels.length > 0)
          ? res.settings.levels
          : this.defaultLevels();

        this.loanDays = res.loanDays;
        this.tags = res.apiTags;
        this.genres = res.apiGenres.map(g => this.mapApiGenre(g));
        this.snapshotInitial();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.toastService.error("Instellingen konden niet geladen worden.");
        this.isLoading = false;
      },
    });
  }

  private mapApiGenre(g: ApiGenre): Genre {
    return {
      id: String(g.id),
      naam: g.naam,
      count: 0,
      subs: g.subgenres.map((s) => ({ id: s.id, naam: s.naam })),
    };
  }

  ngOnDestroy(): void {
    this.stopRotation();
  }

  private defaultHours(): DefaultHours {
    // Default structure for new schools
    return {
      mon: { open: true, from: "08:30", to: "16:30" },
      tue: { open: true, from: "08:30", to: "16:30" },
      wed: { open: true, from: "08:30", to: "12:30" },
      thu: { open: true, from: "08:30", to: "16:30" },
      fri: { open: true, from: "08:30", to: "15:00" },
      sat: { open: false, from: "10:00", to: "12:00" },
      sun: { open: false, from: "10:00", to: "12:00" },
    };
  }

  private defaultLevels(): ReadingLevel[] {
    return [
      { id: 'l1', code: 'A', name: 'Niveau A', desc: 'Beginnende lezers', active: true, isDefault: true },
      { id: 'l2', code: 'B', name: 'Niveau B', desc: 'Gevorderde lezers', active: true, isDefault: false },
      { id: 'l3', code: 'C', name: 'Niveau C', desc: 'Ervaren lezers', active: true, isDefault: false },
      { id: 'l4', code: 'D', name: 'Niveau D', desc: 'Top lezers', active: true, isDefault: false },
    ];
  }

  markDirty(): void {
    const d =
      JSON.stringify(this.messages) !== this.initialState.messages ||
      JSON.stringify(this.hours) !== this.initialState.hours ||
      this.loanDays !== this.initialState.loanDays ||
      JSON.stringify(this.levels) !== this.initialState.levels;
    this.dirty = d;
  }

  /* ── Dirty tracking & save ───────────────── */
  private snapshotInitial(): void {
    this.initialState = {
      messages: JSON.stringify(this.messages),
      hours: JSON.stringify(this.hours),
      loanDays: this.loanDays,
      levels: JSON.stringify(this.levels),
    };
    this.dirty = false;
  }

  saveAll(): void {
    if (!this.schoolId) return;

    const settingsPayload: SchoolSettings = {
      messages: this.messages,
      hours: this.hours,
      levels: this.levels,
    };

    this.toastService.info("Wijzigingen opslaan...");

    forkJoin({
      settings: this.settingsService.saveSettings(
        this.schoolId,
        settingsPayload,
      ),
      loanDays: from(
        this.schoolService.updateDefaultLoanDays(this.schoolId, this.loanDays),
      ),
    }).subscribe({
      next: () => {
        const now = new Date();
        const hh = String(now.getHours()).padStart(2, "0");
        const mm = String(now.getMinutes()).padStart(2, "0");
        this.savedAt = `Vandaag, ${hh}:${mm}`;
        this.snapshotInitial();
        this.toastService.success("Instellingen succesvol bijgewerkt.");
        this.cdr.detectChanges();
      },
      error: () => {
        this.toastService.error("Fout bij het opslaan van instellingen.");
      },
    });
  }

  discardAll(): void {
    this.messages = JSON.parse(this.initialState.messages);
    this.hours = JSON.parse(this.initialState.hours);
    this.loanDays = this.initialState.loanDays;
    this.levels = JSON.parse(this.initialState.levels);
    this.dirty = false;
  }

  /* ── Anchor navigation ──────────────────── */
  scrollTo(id: string): void {
    this.activeAnchor = id;
    const el = document.getElementById(id);
    if (el) {
      window.scrollTo({
        top: el.getBoundingClientRect().top + window.scrollY - 80,
        behavior: "smooth",
      });
    }
  }

  /* ── Messages ───────────────────────────── */
  get enabledMessages(): Message[] {
    return this.messages.filter((m) => m.enabled);
  }

  get currentPreview(): Message | null {
    const list = this.enabledMessages;
    if (list.length === 0) return null;
    const idx = Math.min(this.previewIdx, list.length - 1);
    return list[idx];
  }

  setPreviewIdx(i: number): void {
    this.previewIdx = i;
  }

  private startRotation(): void {
    this.stopRotation();
    this.rotateTimer = setInterval(() => {
      const n = this.enabledMessages.length;
      if (n < 2) return;
      this.previewIdx = (this.previewIdx + 1) % n;
    }, ROTATE_MS);
  }

  private stopRotation(): void {
    if (this.rotateTimer) {
      clearInterval(this.rotateTimer);
      this.rotateTimer = null;
    }
  }

  accentLabel(accent: Message["accent"]): string {
    return this.accents.find((a) => a.id === accent)?.label ?? "";
  }

  charsLeft(): number {
    if (!this.messageDraft) return 220;
    return 220 - (this.messageDraft.body?.length ?? 0);
  }

  startEditMessage(m: Message): void {
    this.addingMessage = false;
    this.editingMessageId = m.id;
    this.messageDraft = { ...m };
  }

  startAddMessage(): void {
    this.editingMessageId = null;
    this.addingMessage = true;
    const today = new Date().toISOString().slice(0, 10);
    this.messageDraft = {
      id: `m-${Date.now()}`,
      title: "",
      body: "",
      accent: "info",
      enabled: true,
      startsAt: today,
      endsAt: today,
    };
  }

  cancelMessage(): void {
    this.editingMessageId = null;
    this.addingMessage = false;
    this.messageDraft = null;
  }

  saveMessage(): void {
    if (!this.messageDraft) return;
    const draft = this.messageDraft;
    if (!draft.title.trim() || !draft.body.trim()) return;
    if (this.addingMessage) {
      this.messages = [...this.messages, draft];
    } else {
      this.messages = this.messages.map((m) =>
        m.id === this.editingMessageId ? { ...m, ...draft } : m,
      );
    }
    this.cancelMessage();
    this.markDirty();
  }

  toggleMessage(id: string, enabled: boolean): void {
    this.messages = this.messages.map((m) =>
      m.id === id ? { ...m, enabled } : m,
    );
    this.markDirty();
  }

  requestDeleteMessage(m: Message): void {
    this.confirmTarget = {
      title: "Bericht verwijderen?",
      body: `"${m.title || "Naamloos bericht"}" wordt verwijderd. Deze actie kan niet ongedaan gemaakt worden.`,
      confirmLabel: "Verwijderen",
      onConfirm: () => {
        this.messages = this.messages.filter((x) => x.id !== m.id);
        this.markDirty();
      },
    };
  }

  setMessageAccent(accent: AccentOption["id"]): void {
    if (this.messageDraft) {
      this.messageDraft = { ...this.messageDraft, accent };
    }
  }

  onMessageBody(value: string): void {
    if (this.messageDraft) {
      this.messageDraft = { ...this.messageDraft, body: value.slice(0, 220) };
    }
  }

  /* ── Hours ──────────────────────────────── */
  setDayOpen(id: keyof DefaultHours, open: boolean): void {
    this.hours = { ...this.hours, [id]: { ...this.hours[id], open } };
    this.markDirty();
  }

  setDayFrom(id: keyof DefaultHours, from: string): void {
    this.hours = { ...this.hours, [id]: { ...this.hours[id], from } };
    this.markDirty();
  }

  setDayTo(id: keyof DefaultHours, to: string): void {
    this.hours = { ...this.hours, [id]: { ...this.hours[id], to } };
    this.markDirty();
  }

  applyHoursPreset(preset: "school-week" | "lunch-only" | "closed"): void {
    const next: DefaultHours = { ...this.hours };
    this.days.forEach((d) => {
      const weekday = d.id !== "sat" && d.id !== "sun";
      if (preset === "school-week") {
        next[d.id] = { open: weekday, from: "08:30", to: "16:30" };
      } else if (preset === "lunch-only") {
        next[d.id] = { open: weekday, from: "12:00", to: "13:30" };
      } else if (preset === "closed") {
        next[d.id] = { ...next[d.id], open: false };
      }
    });
    this.hours = next;
    this.markDirty();
  }

  /* ── Loan term ──────────────────────────── */
  incLoan(delta: number): void {
    this.loanDays = Math.max(
      this.minLoanDays,
      Math.min(this.maxLoanDays, this.loanDays + delta),
    );
    this.markDirty();
  }

  setLoanDays(value: number): void {
    if (Number.isNaN(value)) return;
    this.loanDays = Math.max(
      this.minLoanDays,
      Math.min(this.maxLoanDays, value),
    );
    this.markDirty();
  }

  get deltaVsNet(): number {
    return this.loanDays - this.networkLoanDays;
  }

  get deltaClass(): string {
    const d = this.deltaVsNet;
    if (d === 0) return "is-equal";
    if (d < 0) return "is-stricter";
    return "is-looser";
  }

  get deltaCopy(): string {
    const d = this.deltaVsNet;
    if (d === 0) return "Gelijk aan het GO!-netwerk.";
    if (d < 0)
      return `${Math.abs(d)} dagen strenger dan het netwerk (${this.networkLoanDays} dagen).`;
    return `${d} dagen ruimer dan het netwerk (${this.networkLoanDays} dagen).`;
  }

  /* ── Tags (Thema's) ─────────────────────── */
  addTag(): void {
    const v = this.newTagName.trim();
    if (!v) return;
    this.tagService.create(v).subscribe({
      next: (t) => {
        this.tags = [...this.tags, t];
        this.newTagName = "";
        this.toastService.success(`Thema "${v}" toegevoegd.`);
      },
      error: () => this.toastService.error("Fout bij het toevoegen van thema."),
    });
  }

  requestDeleteTag(t: Tag): void {
    this.confirmTarget = {
      title: `Thema "${t.naam}" verwijderen?`,
      body: `Dit thema wordt verwijderd uit de lijst. Boeken die dit label hebben, behouden de tekst maar het label is niet langer beschikbaar voor nieuwe boeken.`,
      confirmLabel: "Verwijderen",
      onConfirm: () => {
        this.tagService.delete(t.id).subscribe({
          next: () => (this.tags = this.tags.filter((x) => x.id !== t.id)),
          error: () =>
            this.toastService.error("Fout bij het verwijderen van thema."),
        });
      },
    };
  }

  /* ── Genres ─────────────────────────────── */
  get visibleGenres(): Genre[] {
    const q = this.genreQuery.trim().toLowerCase();
    if (!q) return this.genres;
    return this.genres.filter(
      (g) =>
        g.naam.toLowerCase().includes(q) ||
        g.subs.some((s) => s.naam.toLowerCase().includes(q)),
    );
  }

  startEditGenre(g: Genre): void {
    this.editingGenreId = g.id;
    this.editingGenreName = g.naam;
  }

  cancelEditGenre(): void {
    this.editingGenreId = null;
    this.editingGenreName = "";
  }

  saveEditGenre(g: Genre): void {
    const naam = this.editingGenreName.trim();
    if (!naam) return;

    this.genreService.update(Number(g.id), naam).subscribe({
      next: () => {
        this.genres = this.genres.map((x) =>
          x.id === g.id ? { ...x, naam } : x,
        );
        this.cancelEditGenre();
        this.toastService.success(`Genre "${naam}" bijgewerkt.`);
      },
      error: () => this.toastService.error("Fout bij het bijwerken van genre."),
    });
  }

  startAddSub(g: Genre): void {
    this.addingSubFor = g.id;
    this.subDraft = "";
  }

  commitSub(g: Genre): void {
    const v = this.subDraft.trim();
    this.addingSubFor = null;
    this.subDraft = "";
    if (!v) return;
    if (g.subs.some((s) => s.naam.toLowerCase() === v.toLowerCase())) return;

    this.genreService.createSubgenre(Number(g.id), v).subscribe({
      next: (updatedGenre) => {
        this.genres = this.genres.map((x) =>
          x.id === g.id ? this.mapApiGenre(updatedGenre) : x,
        );
        this.toastService.success(`Subgenre "${v}" toegevoegd.`);
      },
      error: () =>
        this.toastService.error("Fout bij het toevoegen van subgenre."),
    });
  }

  cancelSub(): void {
    this.addingSubFor = null;
    this.subDraft = "";
  }

  removeSub(g: Genre, subId: number): void {
    this.genreService.deleteSubgenre(Number(g.id), subId).subscribe({
      next: () => {
        this.genres = this.genres.map((x) =>
          x.id === g.id
            ? { ...x, subs: x.subs.filter((s) => s.id !== subId) }
            : x,
        );
      },
      error: () =>
        this.toastService.error("Fout bij het verwijderen van subgenre."),
    });
  }

  addGenre(): void {
    const v = this.newGenreName.trim();
    if (!v) return;

    this.genreService.create(v).subscribe({
      next: (newApiGenre) => {
        this.genres = [...this.genres, this.mapApiGenre(newApiGenre)];
        this.newGenreName = "";
        this.toastService.success(`Genre "${v}" aangemaakt.`);
        this.cdr.detectChanges();
      },
      error: () => this.toastService.error("Fout bij het aanmaken van genre."),
    });
  }

  requestDeleteGenre(g: Genre): void {
    this.confirmTarget = {
      title: `Genre "${g.naam}" verwijderen?`,
      body:
        g.count > 0
          ? `Er zijn nog ${g.count} boeken aan dit genre gekoppeld. Die boeken behouden hun titel maar verliezen hun genre-label. Deze actie kan niet ongedaan gemaakt worden.`
          : "Dit genre bevat geen boeken. Verwijderen kan niet ongedaan gemaakt worden.",
      confirmLabel: "Verwijderen",
      onConfirm: () => {
        this.genreService.delete(Number(g.id)).subscribe({
          next: () => {
            this.genres = this.genres.filter((x) => x.id !== g.id);
            this.toastService.success(`Genre "${g.naam}" verwijderd.`);
          },
          error: () =>
            this.toastService.error("Fout bij het verwijderen van genre."),
        });
      },
    };
  }

  clearGenreQuery(): void {
    this.genreQuery = "";
  }

  /* ── Levels ─────────────────────────────── */
  get activeLevelCount(): number {
    return this.levels.filter((l) => l.active).length;
  }

  updateLevel(id: string, patch: Partial<ReadingLevel>): void {
    this.levels = this.levels.map((l) =>
      l.id === id ? { ...l, ...patch } : l,
    );
    this.markDirty();
  }

  setLevelCode(id: string, code: string): void {
    this.updateLevel(id, { code: code.slice(0, 6) });
  }

  toggleLevelActive(level: ReadingLevel, active: boolean): void {
    this.updateLevel(level.id, { active });
    if (!active && level.isDefault) {
      const next = this.levels.find((x) => x.id !== level.id && x.active);
      if (next) this.setDefaultLevel(next.id);
    }
  }

  setDefaultLevel(id: string): void {
    this.levels = this.levels.map((l) => ({ ...l, isDefault: l.id === id }));
    this.markDirty();
  }

  levelCardClass(l: ReadingLevel): string {
    const cls = ["lvl-card"];
    cls.push(l.active ? "active" : "inactive");
    if (l.isDefault) cls.push("default");
    return cls.join(" ");
  }

  /* ── Modal ──────────────────────────────── */
  closeConfirm(): void {
    this.confirmTarget = null;
  }

  acceptConfirm(): void {
    if (this.confirmTarget) {
      this.confirmTarget.onConfirm();
      this.confirmTarget = null;
    }
  }

  /* ── Helpers for templates ──────────────── */
  trackById<T extends { id: string }>(_: number, item: T): string {
    return item.id;
  }

  trackByDayId = (_: number, d: DayDef): string => d.id;

  trackBySub = (_: number, s: any): any => s.id || s;

  trackByAccent = (_: number, a: AccentOption): string => a.id;
}