export interface Book {
  id: number;
  titel: string;
  auteur: string;
  cover: string;
  beschrijving: string;
  genres: string[];
  uitgaveDatum: string;
  paginas: number | null;
  taal: string;
  uitgeverij: string;
  leesniveau?: string | null;
  isbn?: string;
  goNumber?: string;
  schoolId?: number;
  schoolNaam?: string;
  reviewCount?: number;
  averageRating?: number;
}
