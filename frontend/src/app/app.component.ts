import { Component, OnInit } from "@angular/core";
import { ItemService } from "./item.service";

@Component({
  selector: "app-root",
  templateUrl: "./app.component.html",
})
export class AppComponent implements OnInit {
  items: any[] = [];
  name: string = "";

  constructor(private itemService: ItemService) {}

  async ngOnInit() {
    this.items = await this.itemService.getItems();
  }

  async addItem() {
    if (!this.name) return;
    await this.itemService.addItem(this.name);
    this.name = "";
    this.items = await this.itemService.getItems();
  }
}
