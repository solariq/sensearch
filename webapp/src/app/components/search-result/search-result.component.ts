import {Component, Input, OnInit} from '@angular/core';
import {SearchResultItemModel} from "../../models/search-result-item.model";

@Component({
  selector: 'app-search-result',
  templateUrl: './search-result.component.html',
  styleUrls: ['./search-result.component.css']
})
export class SearchResultComponent implements OnInit {

  @Input() searchItem: SearchResultItemModel;

  constructor() { }

  ngOnInit() {
  }

}
