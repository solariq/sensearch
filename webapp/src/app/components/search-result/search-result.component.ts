import {Component, Input, OnInit} from '@angular/core';
import {Passage, SearchResultItemModel} from "../../models/search-result-item.model";

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

  selectText(passage: Passage): string {
    const selections = passage.highlights.slice().reverse();
    let text = passage.text;

    selections.forEach(selection => {
      text = text.slice(0, selection[1]) + "</b>" + text.slice(selection[1]);
      text = text.slice(0, selection[0]) + "<b>" + text.slice(selection[0]);
    });

    return text;
  }
}
