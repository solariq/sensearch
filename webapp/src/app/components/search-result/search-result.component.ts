import {Component, Input, OnInit} from '@angular/core';
import {
  Passage,
  SearchResultItemModel,
  SynonymInfoModel,
  WordAndScore
} from "../../models/search-result-item.model";
import {SearchService} from "../../services/search.service";

@Component({
  selector: 'app-search-result',
  templateUrl: './search-result.component.html',
  styleUrls: ['./search-result.component.css']
})
export class SearchResultComponent implements OnInit {

  @Input() searchItem: SearchResultItemModel;
  @Input() query: string;
  @Input() debug: boolean;

  private synonyms: SynonymInfoModel[] = [];

  constructor(
    private searchService: SearchService,
  ) {
  }

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

  showSynonyms(uri: string) {
    this.searchService.getSynonyms$(this.query, uri).subscribe(synonyms => {
      this.synonyms = synonyms;
    })
  }


  sortedSynonyms(synonyms: WordAndScore[]) {
    return synonyms.sort((a, b) => {
      if (a.score < b.score) {
        return -1;
      }
      return a.score == b.score ? 0 : 1;
    })
  }
}
