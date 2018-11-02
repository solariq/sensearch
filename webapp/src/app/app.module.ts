import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { SearchPageComponent } from './components/search-page/search-page.component';
import {SearchResultComponent} from "./components/search-result/search-result.component";
import {
  MatAutocompleteModule,
  MatButtonModule,
  MatFormFieldModule, MatIconModule,
  MatInputModule
} from "@angular/material";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {HttpClientModule} from "@angular/common/http";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";

@NgModule({
  bootstrap: [AppComponent],
  declarations: [
    AppComponent,
    SearchPageComponent,
    SearchResultComponent,
  ],
  imports: [
    BrowserAnimationsModule,
    BrowserModule,
    FormsModule,
    HttpClientModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    ReactiveFormsModule,
  ],
  providers: [],
})
export class AppModule { }
