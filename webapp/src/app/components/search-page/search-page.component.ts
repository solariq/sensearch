import {Component, OnInit, ViewChild} from '@angular/core';
import {Observable} from "rxjs";
import {SearchService} from "../../services/search.service";
import {debounceTime, switchMap} from "rxjs/operators";
import {FormControl} from "@angular/forms";
import {SearchResultItemModel} from "../../models/search-result-item.model";
import {MatAutocompleteTrigger} from "@angular/material";

@Component({
  selector: 'app-search-page',
  templateUrl: './search-page.component.html',
  styleUrls: ['./search-page.component.css']
})
export class SearchPageComponent implements OnInit {
  private DEBOUNCE_TIME = 10;

  @ViewChild('autocompleteInput', { read: MatAutocompleteTrigger }) triggerAutocompleteInput: MatAutocompleteTrigger;

  suggestions: Observable<string[]>;
  searchResults: SearchResultItemModel[];

  autocompleteControl = new FormControl();

  constructor(private searchService: SearchService) { }

  ngOnInit() {
    this.suggestions = this.autocompleteControl.valueChanges.pipe(
      debounceTime(this.DEBOUNCE_TIME),
      switchMap(query => this.searchService.getSuggestions$(query))
    )
  }

  search() {
    this.triggerAutocompleteInput.closePanel();
    this.searchService.getResults$(this.autocompleteControl.value, 0).subscribe(
      results => this.searchResults = results);

    this.triggerAutocompleteInput.closePanel();
  }
}
