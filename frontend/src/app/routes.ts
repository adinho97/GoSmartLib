import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { AuthGuard } from "./auth.guard";
import { LoginComponent } from "./login/login.component";
import { DashboardComponent } from "./dashboard/dashboard.component";
import { AddBookComponent } from "./add-book-component/add-book-component";
import { AddIsbnComponent } from "./add-isbn/add-isbn.component";
import { DetailComponent } from "./detail/detail.component";
import { BookListComponent } from "./book-list/book-list.component";
import { ProfileComponent } from "./profile/profile.component";
import { EditBookComponent } from "./edit-book/edit-book.component";
import { LoanPageComponent } from "./loan-page/loan-page.component";

export const appRoutes: Routes = [
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
    path: "edit/:id",
    component: EditBookComponent,
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
    path: "profile",
    component: ProfileComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerling", "leerkracht", "bibbeheerder"] },
  },
  {
  path: 'uitleen',
  component: LoanPageComponent,
  canActivate: [AuthGuard],
  data: { roles: ['leerkracht', 'bibbeheerder'] },
},
];

@NgModule({
  imports: [RouterModule.forRoot(appRoutes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}