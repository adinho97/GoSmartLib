--- c:\Users\lucal\SofProj\gosmartlib\frontend\src\app\profile\profile.component.spec.ts
+++ c:\Users\lucal\SofProj\gosmartlib\frontend\src\app\profile\profile.component.spec.ts
@@ -1,13 +1,18 @@
 import { ComponentFixture, TestBed } from "@angular/core/testing";
 import { ProfileComponent } from "./profile.component";
 import { FormsModule } from "@angular/forms";
 import { Router } from "@angular/router";
 import { Location } from "@angular/common";
 import { By } from "@angular/platform-browser";
 import { BookService } from "../services/book.service";
 import { UserPreferencesService } from "../services/user-preferences.service";
+import { SmartschoolService } from "../services/smartschool.service";
+import { LoanService } from "../services/loan.service";
+import { ExperienceService } from "../services/experience.service";
+import { UiToastService } from "../services/ui-toast.service";
+import { HttpClientTestingModule } from "@angular/common/http/testing";
 import { of } from "rxjs";
 
 describe("ProfileComponent", () => {
   let component: ProfileComponent;
   let fixture: ComponentFixture<ProfileComponent>;
@@ -15,11 +20,10 @@
   let userPreferencesServiceSpy: jasmine.SpyObj<UserPreferencesService>;
 
   let routerSpy = jasmine.createSpyObj("Router", ["navigate"]);
   let locationSpy = jasmine.createSpyObj("Location", ["back"]);
-});
 
   beforeEach(async () => {
     userPreferencesServiceSpy = jasmine.createSpyObj("UserPreferencesService", [
       "savePreference",
       "getSnapshotForLegacyUse",
     ]);
@@ -27,19 +31,34 @@
     userPreferencesServiceSpy.getSnapshotForLegacyUse.and.returnValue({});
 
-    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", []);
+    bookServiceSpy = jasmine.createSpyObj<BookService>("BookService", [
+      "getUserWishlist",
+      "enrichBooksWithDetails",
+    ]);
+    bookServiceSpy.getUserWishlist.and.resolveTo([]);
+    bookServiceSpy.enrichBooksWithDetails.and.resolveTo([]);
+
+    const smartschoolServiceSpy = jasmine.createSpyObj("SmartschoolService", ["sendMessage"]);
+    const loanServiceSpy = jasmine.createSpyObj("LoanService", ["getMyLoanHistory", "getActiveLoans"]);
+    loanServiceSpy.getMyLoanHistory.and.resolveTo([]);
+    loanServiceSpy.getActiveLoans.and.resolveTo([]);
+    const experienceServiceSpy = jasmine.createSpyObj("ExperienceService", ["levelInfo$"]);
+    experienceServiceSpy.levelInfo$ = of({ level: 1 });
+    const uiToastServiceSpy = jasmine.createSpyObj("UiToastService", ["success", "error"]);
 
     await TestBed.configureTestingModule({
       declarations: [ProfileComponent],
-      imports: [FormsModule],
+      imports: [FormsModule, HttpClientTestingModule],
       providers: [
         { provide: Router, useValue: routerSpy },
         { provide: Location, useValue: locationSpy },
         { provide: BookService, useValue: bookServiceSpy },
-        {
-          provide: UserPreferencesService,
-          useValue: userPreferencesServiceSpy,
-        },
+        { provide: UserPreferencesService, useValue: userPreferencesServiceSpy },
+        { provide: SmartschoolService, useValue: smartschoolServiceSpy },
+        { provide: LoanService, useValue: loanServiceSpy },
+        { provide: ExperienceService, useValue: experienceServiceSpy },
+        { provide: UiToastService, useValue: uiToastServiceSpy },
       ],
     }).compileComponents();
 
     fixture = TestBed.createComponent(ProfileComponent);
@@ -95,10 +114,11 @@
   });
 
   it("should render borrowed books", () => {
+    component.borrowedBooks = [{ id: 1, title: "Test", cover: "" }];
     fixture.detectChanges();
 
     const books = fixture.debugElement.queryAll(By.css(".profile-book-card"));
     expect(books.length).toBeGreaterThan(0);
   });
 });
