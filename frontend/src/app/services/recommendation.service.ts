import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import axios from "axios";

export interface RecommendedBook {
  bookId: number;
  titel: string;
  auteur: string;
  genre: string;
  score: number;
  reason: string;
}

export interface GroupedRecommendations {
  [strategyName: string]: RecommendedBook[];
}

@Injectable({
  providedIn: "root",
})
export class RecommendationService {
  private apiUrl = "/api/aanbevelingen";

  constructor(private http: HttpClient) {}

  private getUserSub(): string {
    return localStorage.getItem("sub") || "";
  }

  private getRoleHeaders() {
    const role = localStorage.getItem("role") || "";
    const userName =
      localStorage.getItem("userName") ||
      localStorage.getItem("username") ||
      localStorage.getItem("name") ||
      "Gebruiker";
    return {
      headers: {
        "X-User-Role": role,
        "X-User-Name": userName,
        "X-User-Sub": this.getUserSub(),
      },
    };
  }

 
  async getGroupedRecommendations(
    limit: number = 10
  ): Promise<GroupedRecommendations> {
    try {
      const response = await axios.get<GroupedRecommendations>(
        `${this.apiUrl}/grouped?limit=${limit}`,
        this.getRoleHeaders()
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching grouped recommendations:", error);
      return {};
    }
  }

  async getRecommendations(limit: number = 10): Promise<RecommendedBook[]> {
    try {
      const response = await axios.get<RecommendedBook[]>(
        `${this.apiUrl}?limit=${limit}`,
        this.getRoleHeaders()
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching recommendations:", error);
      return [];
    }
  }

  async getRecommendationsByStrategy(
    strategyNames: string[],
    limit: number = 10
  ): Promise<RecommendedBook[]> {
    try {
      const strategiesParam = strategyNames.join(",");
      const response = await axios.get<RecommendedBook[]>(
        `${this.apiUrl}/by-strategy?strategies=${strategiesParam}&limit=${limit}`,
        this.getRoleHeaders()
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching strategy recommendations:", error);
      return [];
    }
  }
}