/**
 * @license
 * Copyright (C) 2024 The Android Open Source Project
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
  <style include="gr-modal-styles gr-form-styles">
    iron-autogrow-textarea {
      font-family: var(--monospace-font-family);
      font-size: var(--font-size-mono);
      line-height: var(--line-height-mono);
      width: 50ch;
      height: 10ch;
      background: white; /* TODO: fix for dark mode */
    }
    h2 {
      font-family: var(--header-font-family);
      font-size: var(--font-size-h2);
      font-weight: var(--font-weight-h2);
      line-height: var(--line-height-h2);
    }
    fieldset {
      padding: 0;
    }
  /* TODO dont show this UI if user does not have the capability */
  </style>
  <h2 id="banner" class="">Banner</h2>
  <div class="gr-form-styles">
    <fieldset>
      <section>
        <span class="title">Message</span>
        <span class="value">
          <iron-autogrow-textarea class="textarea-container fit"
              on-keypress="_onKeyPressListener" class="text_area"
              autocomplete="off" bind-value="{{_message}}"/>
        </span>
      </section>
      <gr-button id="saveButton" link on-click="_saveMessage">SAVE MESSAGE</gr-button>
    </fieldset>
  </div>
`;
