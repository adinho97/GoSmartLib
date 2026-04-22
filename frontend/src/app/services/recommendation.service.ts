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

interface CacheEntry {
  data: GroupedRecommendations;
  timestamp: number;
}

@Injectable({
  providedIn: "root",
})
export class RecommendationService {
  private apiUrl = "/api/aanbevelingen";
  private readonly CACHE_TTL = 20 * 60 * 1000; //20min
  private cache: Map<string, CacheEntry> = new Map();

  constructor(private http: HttpClient) {}

  private getUserSub(): string {
    return localStorage.getItem("sub") || "";
  }

  private getRoleHeaders() {
    const role = localStorage.getItem("role") || "";
    const lastName = (localStorage.getItem("lastName") || "").trim();
    const storedFirstName = (localStorage.getItem("firstName") || "").trim();
    const nameCandidates = [
      localStorage.getItem("userName"),
      localStorage.getItem("fullname"),
      localStorage.getItem("name"),
    ];
    const inferredFirstName = nameCandidates
      .map((raw) => {
        const value = (raw || "").trim();
        if (!value || !lastName) {
          return "";
        }
        const parts = value.split(/\s+/).filter(Boolean);
        if (parts.length < 2) {
          return "";
        }
        if (parts[0].toLowerCase() === lastName.toLowerCase()) {
          return parts.slice(1).join(" ");
        }
        if (parts[parts.length - 1].toLowerCase() === lastName.toLowerCase()) {
          return parts.slice(0, -1).join(" ");
        }
        return "";
      })
      .find((value) => !!value);
    const firstName = storedFirstName || inferredFirstName || "";
    const composedName =
      firstName && lastName ? `${firstName} ${lastName}` : "";
    const userName =
      composedName ||
      localStorage.getItem("userName") ||
      localStorage.getItem("fullname") ||
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

  private getCacheKey(limit: number, excludeRead: boolean): string {
    return `grouped_${limit}_${excludeRead}`;
  }

  private isCacheValid(entry: CacheEntry | undefined): boolean {
    if (!entry) return false;
    const now = Date.now();
    return now - entry.timestamp < this.CACHE_TTL;
  }

  clearCache(): void {
    this.cache.clear();
  }

  async getGroupedRecommendations(
    limit: number = 10,
    excludeRead: boolean = true,
  ): Promise<GroupedRecommendations> {
    const cacheKey = this.getCacheKey(limit, excludeRead);
    const cachedEntry = this.cache.get(cacheKey);

    if (this.isCacheValid(cachedEntry)) {
      console.log(`Cache hit for ${cacheKey}`);
      return cachedEntry!.data;
    }

    try {
      const response = await axios.get<GroupedRecommendations>(
        `${this.apiUrl}/grouped?limit=${limit}&excludeRead=${excludeRead}`,
        this.getRoleHeaders(),
      );

      this.cache.set(cacheKey, {
        data: response.data,
        timestamp: Date.now(),
      });

      return response.data;
    } catch (error) {
      console.error("Error fetching grouped recommendations:", error);
      return {};
    }
  }

  async getRecommendations(
    limit: number = 10,
    excludeRead: boolean = true,
  ): Promise<RecommendedBook[]> {
    try {
      const response = await axios.get<RecommendedBook[]>(
        `${this.apiUrl}?limit=${limit}&excludeRead=${excludeRead}`,
        this.getRoleHeaders(),
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
    excludeRead: boolean = true,
  ): Promise<RecommendedBook[]> {
    try {
      const strategiesParam = strategyNames.join(",");
      const response = await axios.get<RecommendedBook[]>(
        `${this.apiUrl}/by-strategy?strategies=${strategiesParam}&limit=${limit}&excludeRead=${excludeRead}`,
        this.getRoleHeaders(),
      );
      return response.data;
    } catch (error) {
      console.error("Error fetching strategy recommendations:", error);
      return [];
    }
  }

  async getTrending(
    limit: number = 10,
    excludeRead: boolean = true,
  ): Promise<RecommendedBook[]> {
    const grouped = await this.getGroupedRecommendations(limit, excludeRead);
    return grouped["TrendingStrategy"] || [];
  }

  async getByGenre(
    limit: number = 10,
    excludeRead: boolean = true,
  ): Promise<RecommendedBook[]> {
    const grouped = await this.getGroupedRecommendations(limit, excludeRead);
    return grouped["GenreBasedStrategy"] || [];
  }

  async getByAuthor(
    limit: number = 10,
    excludeRead: boolean = true,
  ): Promise<RecommendedBook[]> {
    const grouped = await this.getGroupedRecommendations(limit, excludeRead);
    return grouped["AuthorBasedStrategy"] || [];
  }

  async getNewArrivals(
    limit: number = 10,
    excludeRead: boolean = true,
  ): Promise<RecommendedBook[]> {
    const grouped = await this.getGroupedRecommendations(limit, excludeRead);
    return grouped["NewArrivalsStrategy"] || [];
  }

  async getBlended(
    limit: number = 10,
    excludeRead: boolean = true,
  ): Promise<RecommendedBook[]> {
    return this.getRecommendations(limit, excludeRead);
  }
}
