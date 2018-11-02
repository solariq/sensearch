import { Injectable } from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs";
import {environment} from "../../environments/environment";
import {SearchResultItemModel} from "../models/search-result-item.model";

@Injectable({
  providedIn: 'root'
})
export class SearchService {

  constructor(private http: HttpClient) { }

  getSuggestions$(query: string): Observable<string[]> {
    return this.http.get(`${environment.backendUrl}/suggest`, {params: {query: query}}) as Observable<string[]>
  }

  getResults$(query: string, page: number): Observable<SearchResultItemModel[]> {
    let response = this.http.get(`${environment.backendUrl}/search`, {params: {query: query, page: page.toString()}});
    return response as Observable<SearchResultItemModel[]>
  }
}
