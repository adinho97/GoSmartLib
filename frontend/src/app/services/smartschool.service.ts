import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class SmartschoolService {
  private readonly apiUrl = '/api/smartschool/messages';

  constructor(private http: HttpClient) {}

  sendMessage(recipientId: string, subject: string, body: string): Observable<void> {
    const token = localStorage.getItem('smartschoolToken');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    const payload = { recipientId, subject, body };
    return this.http.post<void>(this.apiUrl, payload, { headers });
  }
}
