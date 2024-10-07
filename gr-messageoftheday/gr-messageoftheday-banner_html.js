/**
 * @license
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export const htmlTemplate = Polymer.html`
<style include="shared-styles">
  #container {
    background-color: #fcfad6;
    color: #000000;
    display: flex;
    height: fit-content;
    justify-content: space-between;
    padding: 1em;
  }
  #message {
    flex-grow: 1;
  }
  #dismissMessageBtn {
    color: #000000;
  }
</style>
<div id="container" hidden$="[[_hidden]]">
  <div id="message"></div>
  <gr-button id="dismissMessageBtn"
    link
    on-click="_handleDismissMessage">Dismiss</gr-button>
</div>
<gr-rest-api-interface id="restAPI"></gr-rest-api-interface>`;
