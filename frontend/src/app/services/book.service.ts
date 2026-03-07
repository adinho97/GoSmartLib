import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Book } from '../models/book';
import axios from "axios";

@Injectable({
  providedIn: 'root'
})
export class BookService {

  private apiUrl = '/api/boeken';

  constructor(private http: HttpClient) {}

  getAllBooks(): Observable<Book[]> {
    return this.http.get<Book[]>(this.apiUrl);
  }

  getBookById(id: number): Observable<Book> {
    return this.http.get<Book>(`${this.apiUrl}/${id}`);
  }

    async addBoek(boek: {
    titel: string;
    auteur: string;
    cover: string;
    beschrijving: string;
    genre: string;
    uitgaveDatum: string;
    paginas: number | null;
    taal: string;
    uitgeverij: string;
  }) {
    const res = await axios.post(this.apiUrl, boek);
    return res.data;
  }

  async getBoeken() {
    const res = await axios.get(this.apiUrl);
    return res.data;
  }

  async deleteBoek(id: number) {
    await axios.delete(`${this.apiUrl}/${id}`);
  }
}