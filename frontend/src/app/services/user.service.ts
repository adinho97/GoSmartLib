import { Injectable } from "@angular/core";

export interface StudentWithKlas {
  sub: string;
  klas: string | null;
}

@Injectable({
  providedIn: "root",
})
export class UserService {
  constructor() {}

  async getAllStudentsWithKlas(): Promise<StudentWithKlas[]> {
    try {
      const res = await fetch(`/api/gebruikers/leerlingen-met-klas`);
      if (!res.ok) return [];
      const payload = await res.json();
      // Expected shape: [{ sub: string, klasName: string | null }, ...]
      return (payload || []).map((p: any) => ({
        sub: p.sub,
        klas: p.klasName ?? null,
      }));
    } catch (err) {
      console.error("getAllStudentsWithKlas failed:", err);
      return [];
    }
  }
}
