import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { AuthGuard } from "./auth.guard";
import { LoginComponent } from "./login/login.component";
import { DashboardComponent } from "./dashboard/dashboard.component";
import { AddBookComponent } from "./add-book-component/add-book-component";
import { BookListComponent } from "./book-list/book-list.component";

export const appRoutes: Routes = [
  { path: "login", component: LoginComponent },
  {
    path: "dashboard",
    component: DashboardComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerling", "leerkracht", "bibbeheerder"] },
  },
  { path: "", redirectTo: "/dashboard", pathMatch: "full" },
  {
    path: "add",
    component: AddBookComponent,
    canActivate: [AuthGuard],
    data: { roles: ["bibbeheerder"] },
  },
  {
    path: "books",
    component: BookListComponent,
    canActivate: [AuthGuard],
    data: { roles: ["leerling", "leerkracht", "bibbeheerder"] },
  },
];

@NgModule({
  imports: [RouterModule.forRoot(appRoutes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
