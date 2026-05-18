import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { firstValueFrom } from "rxjs";

export interface StudentWithKlas {
  sub: string;
  klas: string | null;
}

@Injectable({
  providedIn: "root",
})
export class UserService {
  constructor(private readonly http: HttpClient) {}

  async getAllStudentsWithKlas(): Promise<StudentWithKlas[]> {
    try {
      const payload = await firstValueFrom(
        this.http.get<any[]>(`/api/gebruikers/leerlingen-met-klas`),
      );
      return (payload || []).map((p) => ({
        sub: p.sub,
        klas: p.klasName ?? null,
      }));
    } catch (err) {
      console.error("getAllStudentsWithKlas failed:", err);
      return [];
    }
  }

  async getUserProfile(sub: string): Promise<any> {
    try {
      return await firstValueFrom(
        this.http.get<any>(`/api/users/${encodeURIComponent(sub)}/profile`),
      );
    } catch (err) {
      console.error(`getUserProfile failed for ${sub}:`, err);
      return null;
    }
  }
}
