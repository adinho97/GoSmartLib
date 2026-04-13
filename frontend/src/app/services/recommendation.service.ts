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
  cover?: string | null;
  taal?: string | null;
  paginas?: number | null;
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
    limit: number = 10,
    excludeRead: boolean = true
  ): Promise<GroupedRecommendations> {
    try {
      const response = await axios.get<GroupedRecommendations>(
        `${this.apiUrl}/grouped?limit=${limit}&excludeRead=${excludeRead}`,
        this.getRoleHeaders()
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching grouped recommendations:", error);
      return {};
    }
  }

  async getRecommendations(
    limit: number = 10,
    excludeRead: boolean = true
  ): Promise<RecommendedBook[]> {
    try {
      const response = await axios.get<RecommendedBook[]>(
        `${this.apiUrl}?limit=${limit}&excludeRead=${excludeRead}`,
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
    limit: number = 10,
    excludeRead: boolean = true
  ): Promise<RecommendedBook[]> {
    try {
      const strategiesParam = strategyNames.join(",");
      const response = await axios.get<RecommendedBook[]>(
        `${this.apiUrl}/by-strategy?strategies=${strategiesParam}&limit=${limit}&excludeRead=${excludeRead}`,
        this.getRoleHeaders()
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching strategy recommendations:", error);
      return [];
    }
  }

  async getTrending(
    limit: number = 10,
    excludeRead: boolean = true
  ): Promise<RecommendedBook[]> {
    const grouped = await this.getGroupedRecommendations(limit, excludeRead);
    return grouped["TrendingStrategy"] || [];
  }

  async getByGenre(
    limit: number = 10,
    excludeRead: boolean = true
  ): Promise<RecommendedBook[]> {
    const grouped = await this.getGroupedRecommendations(limit, excludeRead);
    return grouped["GenreBasedStrategy"] || [];
  }

  async getByAuthor(
    limit: number = 10,
    excludeRead: boolean = true
  ): Promise<RecommendedBook[]> {
    const grouped = await this.getGroupedRecommendations(limit, excludeRead);
    return grouped["AuthorBasedStrategy"] || [];
  }

  async getNewArrivals(limit: number = 10): Promise<RecommendedBook[]> {
    const grouped = await this.getGroupedRecommendations(limit, true);
    return grouped["NewArrivalsStrategy"] || [];
  }

  async getBlended(limit: number = 10, excludeRead: boolean = true): Promise<RecommendedBook[]> {
    return this.getRecommendations(limit, excludeRead);
  }
}