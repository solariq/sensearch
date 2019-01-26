export class SearchResultPageModel {
  results: SearchResultItemModel[];
  googleResults: SearchResultItemModel[];
}

export class SearchResultItemModel {
  reference: string;
  title: string;
  passages: Passage[];
  score: number;
  debugInfo: DebugInfo;
}

export class Passage {
  text: string;
  highlights: [number, number][];
}

export class DebugInfo {
  rank: number;
  features: number[];
  featureIds: string[];
}
