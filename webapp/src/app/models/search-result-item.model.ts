export class SearchResultPageModel {
  query: string;
  results: SearchResultItemModel[];
  googleResults: SearchResultItemModel[];
}

export class SearchResultItemModel {
  reference: string;
  title: string;
  passages: Passage[];
  score: number;
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
  rank: number;
  features: number[];
  featureIds: string[];
}
