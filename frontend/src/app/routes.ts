import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { AuthGuard } from "./auth.guard";
import { AdminGuard } from "./admin.guard";
import { AdminSetupGuard } from "./admin-setup.guard";
import { LoginComponent } from "./login/login.component";
import { SuperAdminLoginComponent } from "./super-admin-login/super-admin-login.component";
import { AdminSetupComponent } from "./admin-setup/admin-setup.component";
import { AdminDashboardComponent } from "./admin-dashboard/admin-dashboard.component";
import { SuperAdminDashboardComponent } from "./super-admin-dashboard/super-admin-dashboard.component";
import { AdminChangePasswordComponent } from "./admin-change-password/admin-change-password.component";
import { AdminSchoolWizardComponent } from "./admin-school-wizard/admin-school-wizard.component";
import { AdminSchoolDetailComponent } from "./admin-school-detail/admin-school-detail.component";
import { DashboardComponent } from "./dashboard/dashboard.component";
import { AddBookComponent } from "./add-book-component/add-book-component";
import { AddIsbnComponent } from "./add-isbn/add-isbn.component";
import { AddBulkComponent } from "./add-bulk/add-bulk.component";
import { DetailComponent } from "./detail/detail.component";
import { BookListComponent } from "./book-list/book-list.component";
import { ProfileComponent } from "./profile/profile.component";
import { EditBookComponent } from "./edit-book/edit-book.component";
import { AddBarcodeComponent } from "./add-barcode/add-barcode.component";
import { GeneralAddComponent } from "./general-add/general-add.component";
import { LoanPageComponent } from "./loan-page/loan-page.component";
import { LoanHistoryCatalogComponent } from "./loan-history-catalog/loan-history-catalog.component";
import { LoanConditionOverviewComponent } from "./loan-condition-overview/loan-condition-overview.component";
import { LoanOverviewComponent } from "./loan-overview/loan-overview.component";
import { LeerlingInfoComponent } from "./leerling-info/leerling-info.component";
import { MapScreenComponent } from "./map-screen/map-screen.component";
import { MijnLijstenComponent } from "./mijn-lijsten/mijn-lijsten.component";
import { LeerkrachtDashboardComponent } from "./leerkracht-dashboard/leerkracht-dashboard.component";
import { LeeslijstCreateComponent } from "./leeslijst-create/leeslijst-create.component";
import { LeeslijstViewComponent } from "./leeslijst-view/leeslijst-view.component";
import { MijnTakenComponent } from "./mijn-taken/mijn-taken.component";
import { BoekTerugbrengenComponent } from "./boek-terugbrengen/boek-terugbrengen.component";
import { OntleningenVerlengenComponent } from "./ontleningen-verlengen/ontleningen-verlengen.component";
import { StatistiekenComponent } from "./statistieken/statistieken.component";
import { SchoolStatisticsComponent } from "./school-statistics/school-statistics.component";
import { LeaderboardComponent } from "./leaderboard/leaderboard.component";
import { AdminGenreComponent } from "./admin-genre/admin-genre.component";
import { TeacherPromotionComponent } from "./teacher-promotion/teacher-promotion.component";
import { SettingsComponent } from "./settings/settings.component";
import { ExtensionRequestsComponent } from "./extension-requests/extension-requests.component";

export const appRoutes: Routes = [
  // Super Admin Routes
  { path: "super-admin-login", component: SuperAdminLoginComponent },
  {
    path: "admin/setup",
    component: AdminSetupComponent,
    canActivate: [AdminSetupGuard],
  },
  {
    path: "admin/dashboard",
    component: SuperAdminDashboardComponent,
    canActivate: [AdminGuard],
  },
  {
    path: "admin/scholen",
    component: AdminDashboardComponent,
    canActivate: [AdminGuard],
  },
  {
    path: "admin/change-password",
    component: AdminChangePasswordComponent,
    canActivate: [AdminGuard],
  },
  {
    path: "admin/schools/new",
    component: AdminSchoolWizardComponent,
    canActivate: [AdminGuard],
  },
  {
    path: "admin/schools/:id",
    component: AdminSchoolDetailComponent,
    canActivate: [AdminGuard],
  },

  // Regular User Routes
  { path: "login", component: LoginComponent },
  { path: "auth/callback", component: LoginComponent },
  {
    path: "dashboard",
    component: DashboardComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerling", "leerkracht", "bibbeheerder"] },
  },
  {
    path: "leerkracht-dashboard",
    component: LeerkrachtDashboardComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerkracht", "bibbeheerder"] },
  },
  { path: "", component: LoginComponent },
  {
    path: "detail/:id",
    component: DetailComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerling", "leerkracht", "bibbeheerder"] },
  },
  {
    path: "add",
    component: AddBookComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "isbn",
    component: AddIsbnComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "admin/genres",
    component: AdminGenreComponent,
    canActivate: [AdminGuard],
  },
  {
    path: "add-general",
    component: GeneralAddComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "add-barcode",
    component: AddBarcodeComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "edit/:id",
    component: EditBookComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "isbn-bulk",
    component: AddBulkComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "books",
    component: BookListComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerling", "leerkracht", "bibbeheerder"] },
  },
  {
    path: "info",
    component: LeerlingInfoComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerling", "bibbeheerder"] },
  },
  {
    path: "uitleen",
    component: LoanPageComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "map",
    component: MapScreenComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerkracht", "bibbeheerder"] },
  },
  {
    path: "uitleen-catalogus",
    component: LoanHistoryCatalogComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerkracht", "bibbeheerder"] },
  },
  {
    path: "uitleen-conditie",
    component: LoanConditionOverviewComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "uitleen-overzicht",
    component: LoanOverviewComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "teacher-promotion",
    component: TeacherPromotionComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "mijn-lijsten",
    component: MijnLijstenComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerling", "leerkracht", "bibbeheerder"] },
  },
  {
    path: "leeslijst-create",
    component: LeeslijstCreateComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerkracht", "bibbeheerder"] },
  },
  {
    path: "leeslijst-edit/:id",
    component: LeeslijstCreateComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerkracht", "bibbeheerder"] },
  },
  {
    path: "leeslijst/:id",
    component: LeeslijstViewComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerling", "leerkracht", "bibbeheerder"] },
  },
  {
    path: "mijn-taken",
    component: MijnTakenComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerkracht", "bibbeheerder"] },
  },
  {
    path: "boek-terugbrengen",
    component: BoekTerugbrengenComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "ontleningen-verlengen",
    component: OntleningenVerlengenComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "statistieken",
    component: StatistiekenComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "statistieken/school",
    component: SchoolStatisticsComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "leaderboard",
    component: LeaderboardComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerling", "leerkracht", "bibbeheerder"] },
  },
  {
    path: "instellingen",
    component: SettingsComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] }
  },
  {
    path: "extension-requests",
    component: ExtensionRequestsComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] }
  }
];

@NgModule({
  imports: [RouterModule.forRoot(appRoutes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
