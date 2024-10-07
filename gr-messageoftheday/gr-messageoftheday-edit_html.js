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
      background: var(--view-background-color);
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
  <template is="dom-if" if="[[_can_update]]">
    <div class="gr-form-styles">
      <h2 id="Banner">Banner</h2>
      <fieldset>
      <section>
        <span class="title">Expire After</span>
        <span class="value">
          <iron-input
            bind-value="{{_expire_after_value}}"
            placeholder="Enter Number">
            <input
              is="iron-input"
              placeholder="Enter Number"
              bind-value="{{_expire_after_value}}"
            />
          </iron-input>
          <gr-select bind-value="{{_expire_after_unit}}">
            <select>
              <option value="m">minutes</option>
              <option value="h">hours</option>
              <option value="d">days</option>
              <option value="w">weeks</option>
            </select>
          </gr-select>
        </span>
        </section>
        <section>
          <span class="title">Message</span>
          <span class="value">
            <iron-autogrow-textarea class="textarea-container fit"
                on-keypress="_onKeyPressListener" class="text_area"
                placeholder="Enter Message"
                autocomplete="off" bind-value="{{_message}}"/>
          </span>
        </section>
        <section>
          <span class="title">Preview</span>
          <span class="value">
            <div id="messagePreview"></div>
          </span>
        </section>
        <gr-button id="saveButton" on-click="_saveMessage"
          disabled="[[!_message]]">SAVE BANNER</gr-button>
      </fieldset>
    </div>
  </template>
`;
