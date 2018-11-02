export class SearchResultItemModel {
  reference: string;
  title: string;
  passages: Passage[];
  score: number;
}

export class Passage {
  text: string;
  highlight: [number, number][];
}
