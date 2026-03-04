import { Injectable } from "@angular/core";
import axios from "axios";

@Injectable({ providedIn: "root" })
export class ItemService {
  baseUrl = "/api/items";
  boekenUrl = "/api/boeken";

  async getItems() {
    const res = await axios.get(this.baseUrl);
    return res.data;
  }

  async addItem(name: string) {
    const res = await axios.post(this.baseUrl, { name });
    return res.data;
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
    const res = await axios.post(this.boekenUrl, boek);
    return res.data;
  }
  async getBoeken() {
    const res = await axios.get(this.boekenUrl);
    return res.data
  }

}
