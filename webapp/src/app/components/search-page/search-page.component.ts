import {Component, OnInit, ViewChild} from '@angular/core';
import {Observable} from "rxjs";
import {SearchService} from "../../services/search.service";
import {debounceTime, skip, switchMap, take} from "rxjs/operators";
import {FormControl} from "@angular/forms";
import {SearchResultPageModel} from "../../models/search-result-item.model";
import {MatAutocompleteTrigger} from "@angular/material";
import {ActivatedRoute, Router} from "@angular/router";

@Component({
  selector: 'app-search-page',
  templateUrl: './search-page.component.html',
  styleUrls: ['./search-page.component.css']
})
export class SearchPageComponent implements OnInit {
  private DEBOUNCE_TIME = 10;

  @ViewChild('autocompleteInput', { read: MatAutocompleteTrigger }) triggerAutocompleteInput: MatAutocompleteTrigger;

  suggestions: Observable<string[]>;

  searchResults: SearchResultPageModel;

  error: string;

  isSearchActive: boolean;
  autocompleteControl = new FormControl();

  constructor(
    private searchService: SearchService,
    private router: Router,
    private route: ActivatedRoute) { }

  ngOnInit() {
    this.suggestions = this.autocompleteControl.valueChanges.pipe(
      debounceTime(this.DEBOUNCE_TIME),
      switchMap(query => this.searchService.getSuggestions$(query))
    );

    this.route.queryParams.pipe(
      skip(1),
      take(1),
    ).subscribe(params => {
      if (params['search']) {
        this.autocompleteControl.setValue(params['search']);
        this.search();
      }
    });
  }

  search() {
    this.router.navigate(['.'], {queryParams: {search: this.autocompleteControl.value}});
    console.log(this.autocompleteControl.value);
    this.isSearchActive = true;
    this.triggerAutocompleteInput.closePanel();

    this.searchService.getResults$(this.autocompleteControl.value, 0).subscribe(
      results => {
        this.isSearchActive = false;
        this.error = undefined;
        this.searchResults = results;
      },
      error => {
        this.isSearchActive = false;
        this.error = error.statusText + "\n\n" + error.error;
        this.searchResults = undefined;
      });
  }
}
