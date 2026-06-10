import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SavedRanking } from '../models/saved-ranking.model';

@Injectable({ providedIn: 'root' })
export class SavedRankingService {
  private readonly apiUrl = `${environment.apiUrl}/saved-rankings`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<SavedRanking[]> {
    return this.http.get<SavedRanking[]>(this.apiUrl);
  }

  getById(id: number): Observable<SavedRanking> {
    return this.http.get<SavedRanking>(`${this.apiUrl}/${id}`);
  }

  create(savedRanking: SavedRanking): Observable<SavedRanking> {
    return this.http.post<SavedRanking>(this.apiUrl, savedRanking);
  }

  update(id: number, savedRanking: SavedRanking): Observable<SavedRanking> {
    return this.http.put<SavedRanking>(`${this.apiUrl}/${id}`, savedRanking);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
