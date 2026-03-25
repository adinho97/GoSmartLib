import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FormsModule } from "@angular/forms";
import { AppRoutingModule } from "./app-routing.module";

import { AppComponent } from "./app.component";
import { LoginComponent } from "./login/login.component";
import { DashboardComponent } from "./dashboard/dashboard.component";
import { AddBookComponent } from "./add-book-component/add-book-component";
import { AddIsbnComponent } from "./add-isbn/add-isbn.component";
import { AddBulkComponent } from "./add-bulk/add-bulk.component";
import { AddBarcodeComponent } from "./add-barcode/add-barcode.component";
import { BookListComponent } from "./book-list/book-list.component";
import { provideHttpClient } from "@angular/common/http";
import { DetailComponent } from './detail/detail.component';
import { ProfileComponent } from './profile/profile.component';
import { GeneralAddComponent } from './general-add/general-add.component';
import { LoanPageComponent } from "./loan-page/loan-page.component";

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    DashboardComponent,
    AddBookComponent,
    AddIsbnComponent,
    AddBulkComponent,
    GeneralAddComponent,
    DetailComponent,
    BookListComponent,
    ProfileComponent,
    LoanPageComponent
  ],
  imports: [BrowserModule, FormsModule, AppRoutingModule, AddBarcodeComponent],
  providers: [provideHttpClient()],
  bootstrap: [AppComponent],
})
export class AppModule {}
