import { Component } from "@angular/core";

@Component({
  selector: "app-mijn-taken",
  templateUrl: "./mijn-taken.component.html",
  styleUrls: ["./mijn-taken.component.css"],
  standalone: false,
})
export class MijnTakenComponent {
  get isLibrarian(): boolean {
    return localStorage.getItem("role") === "bibbeheerder";
  }
}
