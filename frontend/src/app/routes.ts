import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { AuthGuard } from "./auth.guard";
import { AdminGuard } from "./admin.guard";
import { AdminSetupGuard } from "./admin-setup.guard";
import { LoginComponent } from "./login/login.component";
import { SuperAdminLoginComponent } from "./super-admin-login/super-admin-login.component";
import { AdminSetupComponent } from "./admin-setup/admin-setup.component";
import { AdminDashboardComponent } from "./admin-dashboard/admin-dashboard.component";
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
import { SetupInviteComponent } from "./setup/setup-invite/setup-invite.component";
import { SelectBibbeheerderComponent } from "./setup/select-bibbeheerder/select-bibbeheerder.component";
import { LeerlingInfoComponent } from "./leerling-info/leerling-info.component";
import { BibFaqBeheerComponent } from "./bib-faq-beheer/bib-faq-beheer.component";
import { MapScreenComponent } from "./map-screen/map-screen.component";

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
    data: { roles: ["leerkracht", "bibbeheerder"] },
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
    path: "setup/invite/:token",
    component: SetupInviteComponent,
  },
  {
    path: "setup/select-teacher",
    component: SelectBibbeheerderComponent,
  },
  {
    path: "faq-beheer",
    component: BibFaqBeheerComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
];

@NgModule({
  imports: [RouterModule.forRoot(appRoutes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
