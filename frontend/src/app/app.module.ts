import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FormsModule } from "@angular/forms";
import { RouterModule, Routes } from "@angular/router";

import { AppComponent } from "./app.component";
import { DashboardComponent } from './dashboard/dashboard.component';
import { ItemsPageComponent } from "./items-page.component";
import { AddBookComponent } from "./add-book-component";
import { provideHttpClient } from "@angular/common/http";


@NgModule({
  declarations: [AppComponent, DashboardComponent, ItemsPageComponent, AddBookComponent],
  imports: [BrowserModule, FormsModule],
  providers: [provideHttpClient()],
  bootstrap: [AppComponent],
})
export class AppModule {}
