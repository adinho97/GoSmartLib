import { Injectable } from '@angular/core';
import axios from 'axios';

@Injectable({
  providedIn: 'root',
})
export class UserPreferencesService {
  private apiUrl = '/api/user/preferences';

  private getUserHeaders() {
    const userSub =
      localStorage.getItem('sub') || localStorage.getItem('userId') || '';
    return {
      headers: {
        'X-User-Sub': userSub,
      },
    };
  }

  async savePreference(key: string, value: boolean): Promise<void> {
    try {
      await axios.patch(
        this.apiUrl,
        { key, value },
        this.getUserHeaders()
      );
    } catch (error) {
      console.warn(`Failed to save preference ${key}:`, error);
      // Don't throw - let localStorage be the fallback cache
    }
  }

  async getPreferences(): Promise<Record<string, boolean>> {
    try {
      const res = await axios.get<Record<string, boolean>>(
        this.apiUrl,
        this.getUserHeaders()
      );
      return res.data || {};
    } catch (error) {
      console.warn('Failed to load user preferences from backend:', error);
      return {};
    }
  }
}
