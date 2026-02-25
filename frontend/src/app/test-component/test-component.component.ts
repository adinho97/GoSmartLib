import { Component } from "@angular/core";

@Component({
  selector: "app-test-component",
  templateUrl: "./test-component.component.html",
  styleUrls: ["./test-component.component.css"],
})
export class TestComponentComponent {
  userRole: string;

  constructor() {
    this.userRole = localStorage.getItem("role") || "Unknown";
  }

  logOut(): void {
    localStorage.removeItem("role");
    window.location.href = "/login";
  }
}
