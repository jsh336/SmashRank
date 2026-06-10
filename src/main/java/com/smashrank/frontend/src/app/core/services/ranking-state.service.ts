import { Injectable, signal } from '@angular/core';
import { RegionalRankingResult } from './region-ranking.service';

@Injectable({ providedIn: 'root' })
export class RankingStateService {
  private _result = signal<RegionalRankingResult | null>(null);
  private _provinceName = signal<string>('');
  private _dateRangeMonths = signal<number>(12);

  readonly result = this._result.asReadonly();
  readonly provinceName = this._provinceName.asReadonly();
  readonly dateRangeMonths = this._dateRangeMonths.asReadonly();

  setResult(result: RegionalRankingResult, province: string, months: number): void {
    this._result.set(result);
    this._provinceName.set(province);
    this._dateRangeMonths.set(months);
  }

  clear(): void {
    this._result.set(null);
    this._provinceName.set('');
  }

  hasResult(): boolean {
    return this._result() !== null;
  }
}
