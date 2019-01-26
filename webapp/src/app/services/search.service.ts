import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {environment} from "../../environments/environment";
import {SearchResultPageModel} from "../models/search-result-item.model";

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  constructor(private http: HttpClient) {
  }

  getSuggestions$(query: string): Observable<string[]> {
    return this.http.get(`${environment.backendUrl}/suggest`, {params: {query: query}}) as Observable<string[]>
  }

  getResults$(query: string, page: number, debug: boolean, metric: boolean): Observable<SearchResultPageModel> {
    let response = this.http.get(`${environment.backendUrl}/search`, {
      params: {
        query: query,
        page: page.toString(),
        debug: debug.toString(),
        metric: metric.toString(),
      }
    });
    return response as Observable<SearchResultPageModel>
  }
}
