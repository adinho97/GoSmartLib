import { LOCALE_ID, NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { FormsModule } from "@angular/forms";
import { AppRoutingModule } from "./app-routing.module";
import { provideHttpClient, withInterceptors } from "@angular/common/http";

import { registerLocaleData } from "@angular/common";
import localeNl from "@angular/common/locales/nl";
import { AppComponent } from "./app.component";
import { LoginComponent } from "./login/login.component";
import { SuperAdminLoginComponent } from "./super-admin-login/super-admin-login.component";
import { DashboardComponent } from "./dashboard/dashboard.component";
import { AddBookComponent } from "./add-book-component/add-book-component";
import { AddIsbnComponent } from "./add-isbn/add-isbn.component";
import { AddBulkComponent } from "./add-bulk/add-bulk.component";
import { AddBarcodeComponent } from "./add-barcode/add-barcode.component";
import { BookListComponent } from "./book-list/book-list.component";
import { DetailComponent } from "./detail/detail.component";
import { ProfileComponent } from "./profile/profile.component";
import { BadgeCollectionComponent } from "./profile/badge-collection/badge-collection.component";
import { GeneralAddComponent } from "./general-add/general-add.component";
import { LoanPageComponent } from "./loan-page/loan-page.component";
import { LoanHistoryCatalogComponent } from "./loan-history-catalog/loan-history-catalog.component";
import { LoanConditionOverviewComponent } from "./loan-condition-overview/loan-condition-overview.component";
import { BadgeToastComponent } from "./components/badge-toast/badge-toast.component";
import { RecommendationSectionComponent } from "./recommendation-section/recommendation-section.component";
import { RecommendationCardComponent } from "./recommendation-card/recommendation-card.component";
import { ActiveLoansComponent } from "./active-loans/active-loans.component";
import { LeerlingInfoComponent } from "./leerling-info/leerling-info.component";
import { UiToastComponent } from "./components/ui-toast/ui-toast.component";
import { BibFaqBeheerComponent } from "./bib-faq-beheer/bib-faq-beheer.component";

registerLocaleData(localeNl, "nl");

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    SuperAdminLoginComponent,
    DashboardComponent,
    AddBookComponent,
    AddIsbnComponent,
    AddBulkComponent,
    GeneralAddComponent,
    DetailComponent,
    BookListComponent,
    ProfileComponent,
    LoanPageComponent,
    LoanHistoryCatalogComponent,
    LoanConditionOverviewComponent,
    LeerlingInfoComponent,
    BibFaqBeheerComponent,
  ],
  imports: [
    BrowserModule,
    FormsModule,
    AppRoutingModule,
    AddBarcodeComponent,
    BadgeCollectionComponent,
    BadgeToastComponent,
    RecommendationSectionComponent,
    RecommendationCardComponent,
    ActiveLoansComponent,
    UiToastComponent,
  ],
  providers: [provideHttpClient(), { provide: LOCALE_ID, useValue: "nl" }],
  bootstrap: [AppComponent],
})
export class AppModule {}
