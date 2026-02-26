import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FormsModule } from "@angular/forms";
import { RouterModule, Routes } from "@angular/router";

import { AppComponent } from "./app.component";
import { ItemsPageComponent } from "./items-page.component";
import { AddBookComponent } from "./add-book-component";

const routes: Routes = [
  { path: "", component: ItemsPageComponent },
  { path: "add", component: AddBookComponent },
];

@NgModule({
  declarations: [AppComponent, ItemsPageComponent, AddBookComponent],
  imports: [BrowserModule, FormsModule, RouterModule.forRoot(routes)],
  providers: [],
  bootstrap: [AppComponent],
})
export class AppModule {}
