// src/app/services/loan.service.ts
import { Injectable } from "@angular/core";
import axios from "axios";

export interface Loan {
  id: number;
  copyId: number;
  bookId: number;
  bookTitel: string;
  bookCover: string;
  userSub: string;
  loanedAt: string;
  dueDate: string;
  returnedAt: string | null;
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

@Injectable({ providedIn: "root" })
export class LoanService {
  private api = "/api/uitleningen";
  private copyApi = "/api/exemplaren";

  private headers() {
    const userSub =
      localStorage.getItem("sub") || localStorage.getItem("userId") || "";
    return {
      headers: {
        "X-User-Role": localStorage.getItem("role") || "",
        "X-User-Sub": userSub,
      },
    };
  }

  async createLoan(
    bookId: number,
    userSub: string,
    dueDate: string,
  ): Promise<Loan> {
    const res = await axios.post(
      this.api,
      { bookId, userSub, dueDate },
      this.headers(),
    );
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

  async getMyLoanHistory(): Promise<Loan[]> {
    const res = await axios.get(`${this.api}/mijn/historiek`, this.headers());
    return res.data;
  }

  async getLoansForBook(bookId: number): Promise<Loan[]> {
    const res = await axios.get(`${this.api}/boek/${bookId}`, this.headers());
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

  async addCopy(bookId: number): Promise<void> {
    await axios.post(`${this.copyApi}/boek/${bookId}`, {}, this.headers());
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
    const token = localStorage.getItem("smartschoolToken");
    await axios.patch(
      `/api/uitleningen/${loanId}/due-date`,
      { dueDate: newDueDate },
      {
        headers: { Authorization: `Bearer ${token}` },
      },
    );
  }
}
