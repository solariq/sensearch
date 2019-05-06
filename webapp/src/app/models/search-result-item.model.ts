export class SearchResultPageModel {
  query: string;
  results: SearchResultItemModel[];
  debugResults: SearchResultItemModel[];
}

export class SearchResultItemModel {
  reference: string;
  title: string;
  passages: Passage[];
  debugInfo: DebugInfo;
  pageId: number;
}

export class SynonymInfoModel {
  word: string;
  synonyms: WordAndScore[];
}

export class WordAndScore {
  word: string;
  score: number;
}

export class Passage {
  text: string;
  highlights: [number, number][];
}

export class DebugInfo {
  uri: string;

  rankScore: number;
  filterScore: number;

  rankPlace: number;
  filterPlace: number;

  rankFeatures: number[];
  rankFeatureIds: string[];

  filterFeatures: number[];
  filterFeatureIds: string[];
}
