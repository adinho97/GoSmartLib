import { Injectable } from "@angular/core";
import axios from "axios";

@Injectable({ providedIn: "root" })
export class ItemService {
  baseUrl = "http://localhost:8081/api/items";

  async getItems() {
    const res = await axios.get(this.baseUrl);
    return res.data;
  }

  async addItem(name: string) {
    const res = await axios.post(this.baseUrl, { name });
    return res.data;
  }
}
