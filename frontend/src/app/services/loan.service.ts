// src/app/services/loan.service.ts
import { Injectable } from "@angular/core";
import { AuthContextService } from "./auth-context.service";
import axios from "axios";

export interface Loan {
  id: number;
  copyId: number;
  bookId: number;
  bookTitel: string;
  bookCover: string;
  userSub: string;
  userDisplayName?: string;
  loanedAt: string;
  dueDate: string;
  returnedAt: string | null;
}

export interface CreateLoanRequest {
  bookId: number;
  userSub: string;
  dueDate: string;
  copyId?: number;
}

export type ReturnCondition = "GOOD" | "MODERATE" | "BAD";

export interface ReturnLoanRequest {
  condition: ReturnCondition;
  lost: boolean;
}

export interface BookCopyInfo {
  id: number;
  status: "AVAILABLE" | "LOANED" | "DAMAGED" | "LOST";
  condition: "GOOD" | "MODERATE" | "BAD";
}

export interface UpdateBookCopyStateRequest {
  status: "AVAILABLE" | "DAMAGED" | "LOST";
  condition: "GOOD" | "MODERATE" | "BAD";
}

export interface WorsenedReturn {
  loanId: number;
  copyId: number;
  copyNumber: number | null;
  bookId: number;
  bookTitel: string;
  bookCover: string;
  userSub: string;
  userDisplayName?: string;
  loanedAt: string;
  returnedAt: string;
  loanedCondition: "GOOD" | "MODERATE" | "BAD" | null;
  returnedCondition: "GOOD" | "MODERATE" | "BAD" | null;
  returnedStatus: "AVAILABLE" | "LOANED" | "DAMAGED" | "LOST" | null;
}

export interface BookStateOverview {
  bookId: number;
  bookTitel: string;
  bookCover: string;
  totalCopies: number;
  availableCopies: number;
  loanedCopies: number;
  damagedCopies: number;
  lostCopies: number;
  goodConditionCopies: number;
  moderateConditionCopies: number;
  badConditionCopies: number;
}

export interface LostCopyOverview {
  copyId: number;
  copyNumber: number | null;
  bookId: number;
  bookTitel: string;
  bookCover: string;
  condition: "GOOD" | "MODERATE" | "BAD";
}

export interface LoanConditionOverview {
  worsenedReturns: WorsenedReturn[];
  bookStates: BookStateOverview[];
  lostCopies: LostCopyOverview[];
}

@Injectable({ providedIn: "root" })
export class LoanService {
  private api = "/api/uitleningen";
  private copyApi = "/api/exemplaren";

  constructor(private authContext: AuthContextService) {}

  private headers() {
    return {
      headers: {
        "X-User-Role": this.authContext.getEffectiveRole(),
        "X-User-Sub": this.authContext.getEffectiveSub(),
      },
    };
  }

  async createLoan(
    bookId: number,
    userSub: string,
    dueDate: string,
    copyId?: number,
  ): Promise<Loan> {
    const payload: CreateLoanRequest = { bookId, userSub, dueDate };
    if (copyId !== undefined) {
      payload.copyId = copyId;
    }
    const res = await axios.post(this.api, payload, this.headers());
    return res.data;
  }

  async createLoans(requests: CreateLoanRequest[]): Promise<Loan[]> {
    const res = await axios.post(`${this.api}/bulk`, requests, this.headers());
    return res.data;
  }

  async returnLoan(
    loanId: number,
    request: ReturnLoanRequest = { condition: "GOOD", lost: false },
  ): Promise<Loan> {
    const url = `${this.api}/${loanId}/teruggeven`;
    const res = await axios.put(url, request, this.headers());
    return res.data;
  }

  async getActiveLoans(userSub: string): Promise<Loan[]> {
    const res = await axios.get(
      `${this.api}/gebruiker/${userSub}`,
      this.headers(),
    );
    return res.data;
  }

  async getLoanHistory(userSub: string): Promise<Loan[]> {
    const res = await axios.get(
      `${this.api}/gebruiker/${userSub}/historiek`,
      this.headers(),
    );
    return res.data;
  }

  async getMyActiveLoans(): Promise<Loan[]> {
    const res = await axios.get(`${this.api}/mijn`, this.headers());
    return res.data;
  }

  async getMyLoanHistory(): Promise<Loan[]> {
    const res = await axios.get(`${this.api}/mijn/historiek`, this.headers());
    return res.data;
  }

  async getLoansForBook(bookId: number): Promise<Loan[]> {
    const res = await axios.get(`${this.api}/boek/${bookId}`, this.headers());
    return res.data;
  }

  async getConditionOverview(): Promise<LoanConditionOverview> {
    const res = await axios.get(
      `${this.api}/inspectie/conditie`,
      this.headers(),
    );
    return res.data as LoanConditionOverview;
  }

  async getAllActiveLoans(): Promise<Loan[]> {
    const res = await axios.get(`${this.api}/all-active`, this.headers());
    return res.data;
  }

  async getCopySummary(
    bookId: number,
  ): Promise<{ total: number; available: number }> {
    try {
      const res = await axios.get(`${this.copyApi}/boek/${bookId}/summary`);
      console.log("copySummary response:", res.data); // tijdelijk
      return res.data;
    } catch (err) {
      console.error("getCopySummary fout:", err); // tijdelijk
      return { total: 0, available: 0 };
    }
  }

  async getCopiesForBook(bookId: number): Promise<BookCopyInfo[]> {
    const res = await axios.get(
      `${this.copyApi}/boek/${bookId}`,
      this.headers(),
    );
    return (res.data || []) as BookCopyInfo[];
  }

  async addCopy(
    bookId: number,
    condition?: "GOOD" | "MODERATE" | "BAD",
  ): Promise<void> {
    const body = condition ? { condition } : {};
    await axios.post(`${this.copyApi}/boek/${bookId}`, body, this.headers());
  }

  async updateCopyState(
    copyId: number,
    request: UpdateBookCopyStateRequest,
  ): Promise<void> {
    await axios.patch(`${this.copyApi}/${copyId}`, request, this.headers());
  }

  async deleteCopy(copyId: number): Promise<void> {
    await axios.delete(`${this.copyApi}/${copyId}`, this.headers());
  }
  async removeAvailableCopy(bookId: number): Promise<void> {
    const res = await axios.get(`${this.copyApi}/boek/${bookId}`);
    const copies: any[] = res.data;

    const available = copies.find((c) => c.status === "AVAILABLE");
    if (!available) throw new Error("Geen beschikbaar exemplaar");

    try {
      await axios.delete(`${this.copyApi}/${available.id}`, this.headers());
    } catch (err: any) {
      if (err?.response?.status === 409) {
        throw new Error(
          "Dit exemplaar heeft een uitleenhistoriek en kan niet verwijderd worden.",
        );
      }
      throw err;
    }
  }

  async updateLoanDueDate(loanId: number, newDueDate: string): Promise<void> {
    const token = this.authContext.getEffectiveBearerToken();
    await axios.patch(
      `/api/uitleningen/${loanId}/due-date`,
      { dueDate: newDueDate },
      {
        headers: { Authorization: `Bearer ${token}` },
      },
    );
  }

  async sendMessageToLibrarian(
    loanId: number,
    librarianSub: string,
    senderSub: string,
  ): Promise<void> {
    await axios.post(
      `${this.api}/${loanId}/bericht-bib`,
      {
        librarianSub,
        senderSub,
      },
      this.headers(),
    );
  }
}
