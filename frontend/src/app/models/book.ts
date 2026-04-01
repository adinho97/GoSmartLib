export interface Book {
  id: number;
  titel: string;
  auteur: string;
  cover: string;
  beschrijving: string;
  genre: string;
  uitgaveDatum: string;
  paginas: number;
  taal: string;
  uitgeverij: string;
  isbn?: string;
  schoolId?: number;
  schoolNaam?: string;
  reviewCount?: number;
  averageRating?: number;
}
